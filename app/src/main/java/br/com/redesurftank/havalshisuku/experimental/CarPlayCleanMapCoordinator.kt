package br.com.redesurftank.havalshisuku.experimental

import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.widget.FrameLayout
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger
import br.com.redesurftank.havalshisuku.managers.AndroidAutoClusterController
import br.com.redesurftank.App
import java.util.Arrays

object CarPlayCleanMapCoordinator {
    private val main = Handler(Looper.getMainLooper())
    private data class Offer(val endpoint: IBinder, val token: ByteArray, val generation: Long, val offeredAtMs: Long)
    @Volatile private var enabled = false
    @Volatile private var leaseEpoch = 0L
    @Volatile private var requestEpoch = 0L
    @Volatile private var deadlineElapsedMs = 0L
    @Volatile var statusMessage: String = "Mapa experimental desligado"
        private set
    @Volatile private var taskObservation = CarPlayCleanMapTaskObservation.unknown()
    private data class ReadStatus(
        val startedAtMs: Long = -1,
        val completedAtMs: Long = -1,
        val inFlight: Boolean = false,
        val lastKnown: Boolean = false
    )
    private val readStatusLock = Any()
    private var readStatus = ReadStatus()
    private var slot: FrameLayout? = null
    private var snapshots: CarPlaySecondaryClusterHost.SnapshotProvider? = null
    private var offer: Offer? = null
    private var host: CarPlaySecondaryClusterHost? = null
    private var sampler: CarPlayCleanMapSampler? = null
    private var admission: CarPlayCleanMapAdmission? = null
    private var lastDecision: String? = null
    private var taskSampleCount = 0L
    private var lastTaskSampleState: String? = null
    private val stopAtDeadline = Runnable { disable("deadline") }
    private val pendingPoll = object : Runnable {
        override fun run() {
            if (!enabled) return
            reevaluate() // Only cached observations and View flags; never shells on main.
            if (enabled) main.postDelayed(this, 500)
        }
    }

    @JvmOverloads
    fun setExperimentEnabled(value: Boolean, deadlineElapsedMs: Long = SystemClock.elapsedRealtime() + 300_000L) {
        main.post {
            if (!value) { disable("disabled"); return@post }
            if (enabled) { reevaluate(); return@post } // Never renew the explicit test deadline.
            if (host != null) {
                statusMessage = "Aguardando o encerramento confirmado da saída anterior"
                return@post
            }
            val now = SystemClock.elapsedRealtime()
            if (deadlineElapsedMs <= now || deadlineElapsedMs - now > 300_000L) {
                statusMessage = "O prazo do teste terminou"
                return@post
            }
            enabled = true
            this.deadlineElapsedMs = deadlineElapsedMs
            leaseEpoch++
            val epoch = leaseEpoch
            admission = CarPlayCleanMapAdmission(deadlineElapsedMs)
            taskObservation = CarPlayCleanMapTaskObservation.unknown()
            synchronized(readStatusLock) { readStatus = ReadStatus() }
            lastDecision = null
            taskSampleCount = 0L
            lastTaskSampleState = null
            ClusterPersistentEventLogger.log("carplay_clean_map_gate", mapOf("enabled" to true))
            CarPlayPipelineTraceCollector.beginSession()
            sampler = CarPlayCleanMapSampler(
                deadlineElapsedMs,
                onReadStarted = { startedAt ->
                    if (enabled && leaseEpoch == epoch) synchronized(readStatusLock) {
                        val current = readStatus
                        if (startedAt > current.startedAtMs) {
                            readStatus = current.copy(startedAtMs = startedAt, inFlight = true)
                        }
                    }
                },
                publish = { sample -> main.post {
                    if (enabled && leaseEpoch == epoch) {
                        synchronized(readStatusLock) {
                            val current = readStatus
                            readStatus = current.copy(
                                completedAtMs = maxOf(current.completedAtMs, sample.completedAtElapsedMs),
                                inFlight = current.startedAtMs != sample.startedAtElapsedMs,
                                lastKnown = sample.observation.known)
                        }
                        if (sample.observation.known) taskObservation = sample.observation
                        taskSampleCount++
                        val sampleState = "${sample.observation.known}:" +
                            "${sample.observation.carPlayOnMain}:${sample.observation.carPlayOnSecondary}"
                        if (taskSampleCount == 1L || taskSampleCount % 20L == 0L ||
                            sample.durationMs > 300L || sampleState != lastTaskSampleState) {
                            ClusterPersistentEventLogger.log("carplay_clean_map_task_sample", mapOf(
                                "sample" to taskSampleCount,
                                "known" to sample.observation.known,
                                "duration_ms" to sample.durationMs,
                                "main" to sample.observation.carPlayOnMain,
                                "secondary" to sample.observation.carPlayOnSecondary))
                        }
                        lastTaskSampleState = sampleState
                        reevaluate()
                    }
                }}
            )
            main.postDelayed(stopAtDeadline, deadlineElapsedMs - now)
            main.postDelayed(pendingPoll, 500)
            reevaluate()
        }
    }

    fun isExperimentEnabled(): Boolean = enabled && SystemClock.elapsedRealtime() < deadlineElapsedMs
    fun currentTaskObservation(): CarPlayCleanMapTaskObservation = taskObservation
    fun currentReadStatus(): LongArray = synchronized(readStatusLock) {
        longArrayOf(readStatus.startedAtMs, readStatus.completedAtMs,
            if (readStatus.inFlight) 1 else 0, if (readStatus.lastKnown) 1 else 0)
    }

    fun attachSlot(target: FrameLayout, provider: CarPlaySecondaryClusterHost.SnapshotProvider): AutoCloseable {
        check(Looper.myLooper() == Looper.getMainLooper())
        check(slot == null || slot === target) { "Only one CarPlay clean-map slot is allowed" }
        slot = target
        snapshots = provider
        reevaluate()
        return AutoCloseable {
            main.post {
                if (slot === target) {
                    retire("slot_detached")
                    slot = null
                    snapshots = null
                }
            }
        }
    }

    fun offerEndpoint(endpoint: IBinder, token: ByteArray, generation: Long) {
        require(token.size == 32 && generation > 0 && endpoint.isBinderAlive)
        val copy = token.clone()
        val epoch = leaseEpoch
        val request = requestEpoch
        val offeredAt = SystemClock.elapsedRealtime()
        main.post {
            val ledger = admission
            val accepted = enabled && leaseEpoch == epoch && requestEpoch == request && host == null &&
                ledger?.acceptsOffer(SystemClock.elapsedRealtime()) == true
            ClusterPersistentEventLogger.log("carplay_clean_map_endpoint",
                mapOf("generation" to generation, "accepted" to accepted))
            if (!accepted) { Arrays.fill(copy, 0); return@post }
            clearOffer()
            offer = Offer(endpoint, copy, generation, offeredAt)
            ledger?.offered()
            reevaluate()
        }
    }

    fun inputsChanged() { main.post { reevaluate() } }

    private fun reevaluate() {
        if (!enabled) return
        val ledger = admission ?: return
        val target = slot
        val provider = snapshots
        val reason = if (target == null || provider == null) CarPlayClusterHostPolicy.Reason.SLOT_DETACHED
            else host?.eligibilityReason() ?: CarPlaySecondaryClusterHost.eligibilityReason(target, null, provider)
        recordDecision(reason)
        val active = host
        if (active != null) { active.onInputsChanged(); return }
        val pending = offer
        val now = SystemClock.elapsedRealtime()
        when (ledger.evaluate(now, reason == CarPlayClusterHostPolicy.Reason.ALLOWED,
            pending?.offeredAtMs ?: -1, pending?.endpoint?.isBinderAlive == true)) {
            CarPlayCleanMapAdmission.Action.STOP -> disable("deadline")
            CarPlayCleanMapAdmission.Action.REQUEST -> {
                ledger.requested(now)
                requestEpoch++
                statusMessage = "Aguardando a oferta autenticada do CarPlay"
                broadcast(ACTION_REQUEST)
                ClusterPersistentEventLogger.log("carplay_clean_map_request", mapOf("attempt" to ledger.requestCount()))
            }
            CarPlayCleanMapAdmission.Action.REVOKE -> {
                clearOffer()
                requestEpoch++
                ledger.revoked(now)
                broadcast(ACTION_REVOKE)
                statusMessage = "Aguardando nova oferta do mapa experimental"
                ClusterPersistentEventLogger.log("carplay_clean_map_offer_expired", mapOf("attempt" to ledger.requestCount()))
            }
            CarPlayCleanMapAdmission.Action.START -> if (target != null && provider != null && pending != null) {
                startHost(target, provider, pending, ledger)
            }
            CarPlayCleanMapAdmission.Action.EXHAUSTED -> {
                statusMessage = "Mapa indisponível neste teste; encerre e tente novamente mais tarde"
            }
            CarPlayCleanMapAdmission.Action.WAIT -> statusMessage = waitingMessage(reason)
            CarPlayCleanMapAdmission.Action.TERMINAL -> Unit
        }
    }

    private fun startHost(target: FrameLayout, provider: CarPlaySecondaryClusterHost.SnapshotProvider,
        pending: Offer, ledger: CarPlayCleanMapAdmission) {
        lateinit var next: CarPlaySecondaryClusterHost
        next = CarPlaySecondaryClusterHost(target, provider, object : CarPlaySecondaryClusterHost.Listener {
            override fun onReady(generation: Long, width: Int, height: Int) {
                statusMessage = "Saída experimental criada; confira a imagem no cluster"
                ClusterPersistentEventLogger.log("carplay_clean_map_surface_ready",
                    mapOf("generation" to generation, "width" to width, "height" to height))
                CarPlayPipelineTraceCollector.collect("surface_ready")
            }
            override fun onFramePresented(generation: Long, frameCount: Long) {
                ClusterPersistentEventLogger.log("carplay_clean_map_texture_updated",
                    mapOf("generation" to generation, "frames" to frameCount))
                if (frameCount == 1L) CarPlayPipelineTraceCollector.collect("first_texture_update")
            }
            override fun onFailure(failure: CarPlaySecondaryClusterHost.Failure,
                consumerFailure: CarPlaySecondarySurfaceConsumer.Failure?) {
                recordDecision(next.eligibilityReason())
                if (next.hasStartedConsumer()) {
                    ledger.consumerStarted()
                    statusMessage = "A saída experimental foi interrompida; sessão principal preservada"
                }
                ClusterPersistentEventLogger.log("carplay_clean_map_failure",
                    mapOf("failure" to failure.name, "consumer" to (consumerFailure?.name ?: "none"),
                        // diag4: bounded grant cause, attached ONLY to GRANT_FAILED (never a stale revoke cause)
                        "cause" to CarPlayGrantFailureCause.reportedGrantCause(consumerFailure?.name, next.consumerGrantCause())))
                CarPlayPipelineTraceCollector.collect("failure")
            }
            override fun onHidden() { ClusterPersistentEventLogger.log("carplay_clean_map_hidden") }
            override fun onRetired() {
                if (host === next) host = null
                if (next.hasStartedConsumer()) {
                    ledger.consumerStarted()
                    if (offer === pending) clearOffer()
                }
                ClusterPersistentEventLogger.log("carplay_clean_map_retired")
                CarPlayPipelineTraceCollector.collect("retired", force = true)
            }
        })
        host = next
        val started = next.start(pending.endpoint, pending.token, pending.generation)
        if (next.hasStartedConsumer()) {
            ledger.consumerStarted()
            if (offer === pending) clearOffer() // The consumer holds its own token copy.
        }
        if (!started && host === next && !next.hasStartedConsumer()) host = null
    }

    private fun recordDecision(reason: CarPlayClusterHostPolicy.Reason) {
        val requested = AndroidAutoClusterController.isClusterRequested()
        val grace = host?.isUsingFreshnessGrace() == true
        val signature = "${reason.name}:$requested:$grace"
        if (signature == lastDecision) return
        lastDecision = signature
        ClusterPersistentEventLogger.log("carplay_clean_map_eligibility", mapOf(
            "reason" to reason.name, "aa_requested" to requested,
            "freshness_grace" to grace,
            "task_known" to taskObservation.known, "carplay_main" to taskObservation.carPlayOnMain,
            "carplay_secondary" to taskObservation.carPlayOnSecondary,
            "aa_session_active" to AndroidAutoClusterController.isSessionActive(),
            "aa_surface_attached" to AndroidAutoClusterController.isSurfaceAttached()))
    }

    private fun waitingMessage(reason: CarPlayClusterHostPolicy.Reason): String = when (reason) {
        CarPlayClusterHostPolicy.Reason.CARPLAY_MAIN_UNCONFIRMED ->
            "Abra o CarPlay na tela principal da central; o teste continua em segundo plano"
        CarPlayClusterHostPolicy.Reason.CARPLAY_SECONDARY_PRESENT -> "Aguardando o CarPlay somente na tela principal"
        CarPlayClusterHostPolicy.Reason.AA_SESSION_ACTIVE, CarPlayClusterHostPolicy.Reason.AA_SURFACE_ATTACHED ->
            "Aguardando o Android Auto encerrar; ele mantém prioridade"
        CarPlayClusterHostPolicy.Reason.AA_NOT_KNOWN_INACTIVE -> "Aguardando confirmação de Android Auto inativo"
        CarPlayClusterHostPolicy.Reason.SAFETY_UI_ACTIVE -> "Aguardando a tela nativa de segurança ficar livre"
        CarPlayClusterHostPolicy.Reason.OTHER_APP_ACTIVE -> "Aguardando o aplicativo do cluster encerrar"
        CarPlayClusterHostPolicy.Reason.ALLOWED -> "Aguardando a oferta autenticada do CarPlay"
        else -> "Aguardando confirmação das telas e do espaço do cluster"
    }

    private fun disable(reason: String) {
        val wasEnabled = enabled
        enabled = false
        deadlineElapsedMs = 0L
        leaseEpoch++
        requestEpoch++
        main.removeCallbacks(stopAtDeadline)
        main.removeCallbacks(pendingPoll)
        sampler?.close()
        sampler = null
        taskObservation = CarPlayCleanMapTaskObservation.unknown()
        synchronized(readStatusLock) { readStatus = ReadStatus() }
        if (wasEnabled) {
            broadcast(ACTION_REVOKE)
            ClusterPersistentEventLogger.log("carplay_clean_map_gate", mapOf("enabled" to false))
        }
        retire(reason)
        CarPlayPipelineTraceCollector.collect("test_end_$reason", force = true)
        statusMessage = if (reason == "deadline") "Teste encerrado após cinco minutos" else "Mapa experimental desligado"
    }

    private fun retire(reason: String) {
        ClusterPersistentEventLogger.log("carplay_clean_map_retire", mapOf("reason" to reason))
        host?.close()
        if (host == null) {
            clearOffer()
            val ledger = admission
            if (enabled && ledger != null && !ledger.hasStartedConsumer() && ledger.requestCount() > 0) {
                requestEpoch++
                ledger.revoked(SystemClock.elapsedRealtime())
                broadcast(ACTION_REVOKE)
            }
        }
    }
    private fun clearOffer() { offer?.let { Arrays.fill(it.token, 0) }; offer = null }
    private fun broadcast(action: String) {
        runCatching { App.getDeviceProtectedContext().sendBroadcast(Intent(action).setPackage(OEM_PACKAGE)) }
    }
    private const val OEM_PACKAGE = "com.ts.carplay"
    private const val ACTION_REQUEST = "br.com.redesurftank.havalshisuku.CARPLAY_CLEAN_MAP_REQUEST"
    private const val ACTION_REVOKE = "br.com.redesurftank.havalshisuku.CARPLAY_CLEAN_MAP_REVOKE"
}
