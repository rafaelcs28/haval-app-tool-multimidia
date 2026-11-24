package br.com.redesurftank.havalshisuku.managers

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.api.AaClusterProtocol
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger
import br.com.redesurftank.havalshisuku.models.CarConstants
import br.com.redesurftank.havalshisuku.projectors.AaClusterVideoHost
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Owns AA telemetry and demand for the independent D3 CLUSTER output.
 * The exact-profile Service registers CLUSTER before discovery with automatic
 * projection disabled. Readiness means a current authenticated rendered frame,
 * never a mounted APK, a local focus callback or merely an allocated Surface.
 */
object AndroidAutoClusterController {
    private const val TAG = "AaClusterCtrl"
    /** Guidance can drop for a moment while re-routing; don't tear the map down for that. */
    private const val NAVIGATION_HIDE_DEBOUNCE_MS = 5_000L

    private val sessionDebouncer = AndroidAutoSessionTelemetry.Debouncer()
    private val directionsPublisher = AndroidAutoNavigationTelemetry.Publisher()
    private val started = AtomicBoolean(false)
    private val clusterRequested = AtomicBoolean(false)
    private val navigationActive = AtomicBoolean(false)
    private val lastNavigationActiveAtMs = AtomicLong(0L)
    private val sessionStopSuppressed = AtomicBoolean(false)
    /** An app sent to D1/D3 gets the cluster; the map steps aside until it leaves. */
    private val secondaryAppActive = AtomicBoolean(false)
    private val demandEpoch = AtomicLong()
    private val surfaceAttached = AtomicBoolean(false)
    private var deliveredGeneration = -1L // main-thread Surface lifecycle token
    private var deliveredDemandEpoch = -1L
    private var liveSinceMs = 0L
    private var recoveryInFlight = false
    private var recoveryDemandEpoch = -1L
    private var decoderFailurePending = false
    private var recoveryCount = 0
    private var lastRecoveryAtMs = 0L
    private var notLiveSinceMs = 0L
    private var lastNotLiveKickAtMs = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client by lazy {
        AndroidAutoClusterClient(App.getContext()) { state, reason ->
            val live = state == AaClusterProtocol.LIVE && wantsOutput() &&
                AaClusterVideoHost.peekSurface()?.isValid == true
            val wasLive = surfaceAttached.getAndSet(live)
            decoderFailurePending = AndroidAutoClusterStreamHealth.nextDecoderFailurePending(
                decoderFailurePending, state, reason, wasLive
            )
            if (AndroidAutoClusterStreamHealth.shouldCancelRecovery(state, reason)) {
                recoveryInFlight = false
                recoveryDemandEpoch = -1L
            }
            if (live) {
                if (!wasLive) liveSinceMs = SystemClock.elapsedRealtime()
                recoveryInFlight = false
                recoveryDemandEpoch = -1L
            } else {
                liveSinceMs = 0L
            }
            AaClusterVideoHost.setLive(live)
            notifyHostProjectionFlag(live)
            Log.w(TAG, "CLUSTER state=$state live=$live reason=$reason")
            ClusterPersistentEventLogger.log(
                "aa_cluster_stream_state",
                mapOf(
                    "state" to state,
                    "live" to live,
                    "reason" to reason,
                    "requested" to wantsOutput(),
                    "surface_attached" to surfaceAttached.get(),
                    "recovery_in_flight" to recoveryInFlight,
                    "recovery_count" to recoveryCount
                )
            )
        }
    }
    private val sessionPollRunnable = object : Runnable {
        override fun run() {
            val status = DisplayAppLauncher.readAndroidAutoLinkStatusIfAlreadyBound("AA_SESSION_POLL")
            val dcmEvidenceRecent =
                DisplayAppLauncher.hasRecentAndroidAutoDcmProjectionActiveEvidenceForSession()
            val next = AndroidAutoSessionTelemetry.statusForPoll(
                linkStatus = status,
                dcmEvidenceRecent = dcmEvidenceRecent,
                sessionActive = isSessionActive()
            )
            if (next != null) {
                onLinkStatus(next, availabilityObserved = status != null || dcmEvidenceRecent)
            }
            checkClusterStreamHealth()
            mainHandler.postDelayed(this, 1_500L)
        }
    }

    @Volatile
    private var lastSessionValue: String = AndroidAutoTelemetryKeys.SESSION_STOPPED

    fun start() {
        if (!started.compareAndSet(false, true)) return
        Log.i(TAG, "Starting Android Auto cluster session monitor")
        publishSession(AndroidAutoTelemetryKeys.SESSION_STOPPED, force = true)
        publishDirections(AndroidAutoNavigationTelemetry.inactive(), SystemClock.elapsedRealtime(), force = true)
        mainHandler.removeCallbacks(sessionPollRunnable)
        mainHandler.post(sessionPollRunnable)
    }

    fun onLinkStatus(
        status: Int?,
        nowMs: Long = SystemClock.elapsedRealtime(),
        availabilityObserved: Boolean = status != null
    ) {
        val navigationEvidenceRecent =
            AndroidAutoSessionTelemetry.hasRecentNavigationEvidence(
                lastActiveAtMs = lastNavigationActiveAtMs.get(),
                nowMs = nowMs
            )
        val effectiveStatus = AndroidAutoSessionTelemetry.statusWithNavigationEvidence(
            linkStatus = status,
            navigationEvidenceRecent = navigationEvidenceRecent
        )
        if (availabilityObserved || navigationEvidenceRecent) {
            br.com.redesurftank.havalshisuku.navigation.CarPlayExperimentalHud
                .onAndroidAutoAvailability(AndroidAutoSessionTelemetry.isLinkActive(effectiveStatus), nowMs)
        }
        if (effectiveStatus != status && sessionStopSuppressed.compareAndSet(false, true)) {
            val navigationAgeMs = nowMs - lastNavigationActiveAtMs.get()
            Log.w(
                TAG,
                "Ignoring transient AA stopped status while navigation evidence is fresh " +
                    "status=$status navigationAgeMs=$navigationAgeMs"
            )
            ClusterPersistentEventLogger.log(
                "aa_cluster_session_stop_suppressed",
                mapOf("link_status" to status, "navigation_age_ms" to navigationAgeMs)
            )
        } else if (effectiveStatus == status) {
            sessionStopSuppressed.set(false)
        }
        val published = sessionDebouncer.onStatus(effectiveStatus, nowMs) ?: return
        publishSession(published, force = false)
    }

    fun currentSessionValue(): String = lastSessionValue

    fun isSessionActive(): Boolean {
        return lastSessionValue == AndroidAutoTelemetryKeys.SESSION_ACTIVE
    }

    fun isClusterRequested(): Boolean = clusterRequested.get()

    /**
     * The map shows only while the theme asked for it, AA is linked, Maps is guiding and no app
     * is on D1/D3. The map is drawn inside D3's Presentation, above anything on D1/D3, so it
     * would otherwise cover the app.
     */
    private fun wantsOutput(): Boolean =
        clusterRequested.get() && isSessionActive() && navigationActive.get() &&
            !secondaryAppActive.get() && !selfUpdateHold.get()

    private val selfUpdateHold = java.util.concurrent.atomic.AtomicBoolean(false)
    private val selfUpdateHoldExpired = Runnable {
        if (selfUpdateHold.getAndSet(false)) {
            ClusterPersistentEventLogger.log("aa_cluster_self_update_hold_expired", emptyMap<String, Any?>())
            demandEpoch.incrementAndGet()
            reconcileOutput("self_update_hold_expired")
        }
    }

    /**
     * Before Impulse installs its own update: hand the cluster map back cleanly and wait for the
     * Android Auto side to confirm the codec released. When the update kills Impulse with the map
     * live, the AA codec errors with its Surface and its slot stays stuck until AA restarts (field
     * 2026-11-24). If the install does not happen, the map comes back after [SELF_UPDATE_HOLD_MS].
     * Call off the main thread. Returns true when nothing is left owned by the Service.
     */
    fun holdForSelfUpdate(maxWaitMs: Long = 4_000L): Boolean {
        selfUpdateHold.set(true)
        demandEpoch.incrementAndGet()
        mainHandler.post { reconcileOutput("self_update_hold") }
        mainHandler.removeCallbacks(selfUpdateHoldExpired)
        mainHandler.postDelayed(selfUpdateHoldExpired, SELF_UPDATE_HOLD_MS)
        val started = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - started < maxWaitMs) {
            if (!client.hasRemoteOutput()) {
                ClusterPersistentEventLogger.log("aa_cluster_self_update_released",
                    mapOf("wait_ms" to (SystemClock.elapsedRealtime() - started)))
                return true
            }
            try { Thread.sleep(100L) } catch (e: InterruptedException) { Thread.currentThread().interrupt(); break }
        }
        ClusterPersistentEventLogger.log("aa_cluster_self_update_release_timeout", mapOf("wait_ms" to maxWaitMs))
        return false
    }

    private const val SELF_UPDATE_HOLD_MS = 180_000L

    fun isSurfaceAttached(): Boolean = surfaceAttached.get()

    /**
     * Theme / external command. Does not persist across disconnects: a new
     * session starts over and the theme must ask again.
     */
    fun setClusterMapEnabled(enabled: Boolean, source: String) {
        Log.w(TAG, "setClusterMapEnabled enabled=$enabled source=$source session=$lastSessionValue")
        if (clusterRequested.getAndSet(enabled) != enabled) demandEpoch.incrementAndGet()
        if (!enabled) {
            detachSurface("disabled_by_$source")
            return
        }
        if (!isSessionActive()) {
            Log.i(TAG, "CLUSTER map requested with no AA session; will attach after session=active")
            return
        }
        attachIfSessionAllows(source)
    }

    /** D3 projector: whether any app is on D1 or D3 right now. Any thread. */
    fun setSecondaryDisplayAppActive(active: Boolean, source: String) {
        if (secondaryAppActive.getAndSet(active) == active) return
        Log.w(TAG, "CLUSTER secondary-display app active=$active source=$source")
        attachIfSessionAllows(if (active) "secondary_app_arrived" else "secondary_app_left")
    }

    fun onNavigationUpdate(update: AndroidAutoNavigationTelemetry.Directions, nowMs: Long = SystemClock.elapsedRealtime()) {
        onNavigationActive(update.active, nowMs)
        br.com.redesurftank.havalshisuku.navigation.CarPlayExperimentalHud.onAndroidAuto(update)
        publishDirections(update, nowMs, force = false)
        mainHandler.removeCallbacks(flushDirectionsRunnable)
        mainHandler.postDelayed(flushDirectionsRunnable, AndroidAutoNavigationTelemetry.THROTTLE_MS)
    }

    fun snapshotDirectionsJson(): String = directionsPublisher.lastJson()

    /** Whether the cluster theme gets the turn-by-turn ([SharedPreferencesKeys.CLUSTER_SHOW_NAVIGATION]). */
    fun navigationShownOnTheme(): Boolean =
        br.com.redesurftank.App.getDeviceProtectedContext()
            .getSharedPreferences("haval_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean(br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys.CLUSTER_SHOW_NAVIGATION.key, true)

    /** "No navigation", as a theme sees it while [navigationShownOnTheme] is off. */
    val INACTIVE_DIRECTIONS_JSON: String by lazy { AndroidAutoNavigationTelemetry.inactive().toJson() }

    /** Re-sends the current directions, so the theme follows a change of [navigationShownOnTheme] at once. */
    fun republishDirections() {
        ServiceManager.getInstance().dispatchTelemetryOnly(AndroidAutoTelemetryKeys.DIRECTIONS, snapshotDirectionsJson())
    }

    fun peekSurface(): Surface? = AaClusterVideoHost.peekSurface()

    private val navigationEndedRunnable = Runnable {
        if (navigationActive.getAndSet(false)) {
            Log.w(TAG, "CLUSTER navigation ended; hiding map")
            attachIfSessionAllows("navigation_ended")
        }
    }

    private fun onNavigationActive(active: Boolean, nowMs: Long) {
        if (active) {
            lastNavigationActiveAtMs.set(nowMs)
            mainHandler.removeCallbacks(navigationEndedRunnable)
            if (!navigationActive.getAndSet(true)) {
                Log.w(TAG, "CLUSTER navigation started")
                // A new trip is a fresh demand: forget an old failed request and the spent budget,
                // so the map is requested again instead of staying blank until a reboot.
                if (!surfaceAttached.get()) {
                    demandEpoch.incrementAndGet()
                    recoveryCount = 0
                    lastRecoveryAtMs = 0L
                    recoveryInFlight = false
                    recoveryDemandEpoch = -1L
                }
                if (clusterRequested.get()) attachIfSessionAllows("navigation_started")
            }
            if (AndroidAutoSessionTelemetry.shouldReviveSessionFromNavigation(
                    navigationActive = true,
                    sessionActive = isSessionActive()
                )
            ) {
                // Guidance only comes from a linked phone. The fresh evidence just recorded
                // makes onLinkStatus publish active; main thread, like the session poll.
                mainHandler.post {
                    if (!isSessionActive()) onLinkStatus(null)
                }
            }
        } else if (navigationActive.get()) {
            mainHandler.removeCallbacks(navigationEndedRunnable)
            mainHandler.postDelayed(navigationEndedRunnable, NAVIGATION_HIDE_DEBOUNCE_MS)
        }
    }

    private val flushDirectionsRunnable = Runnable {
        val json = directionsPublisher.flushPending(SystemClock.elapsedRealtime()) ?: return@Runnable
        ServiceManager.getInstance().dispatchTelemetryOnly(AndroidAutoTelemetryKeys.DIRECTIONS, json)
    }

    private fun publishSession(value: String, force: Boolean) {
        if (!force && value == lastSessionValue) return
        lastSessionValue = value
        br.com.redesurftank.havalshisuku.models.BottomBarState.publishAndroidAutoLinked(
            value == AndroidAutoTelemetryKeys.SESSION_ACTIVE
        )
        ServiceManager.getInstance().dispatchTelemetryOnly(AndroidAutoTelemetryKeys.SESSION, value)
        if (value == AndroidAutoTelemetryKeys.SESSION_STOPPED) {
            onSessionStopped()
        } else if (clusterRequested.get()) {
            attachIfSessionAllows("session_active")
        }
    }

    private fun onSessionStopped() {
        if (clusterRequested.getAndSet(false)) demandEpoch.incrementAndGet()
        mainHandler.removeCallbacks(navigationEndedRunnable)
        navigationActive.set(false)
        lastNavigationActiveAtMs.set(0L)
        sessionStopSuppressed.set(false)
        liveSinceMs = 0L
        recoveryInFlight = false
        recoveryDemandEpoch = -1L
        decoderFailurePending = false
        recoveryCount = 0
        lastRecoveryAtMs = 0L
        detachSurface("session_stopped")
        br.com.redesurftank.havalshisuku.navigation.CarPlayExperimentalHud.onAndroidAuto(AndroidAutoNavigationTelemetry.inactive())
        directionsPublisher.reset()
        ServiceManager.getInstance().dispatchTelemetryOnly(
            AndroidAutoTelemetryKeys.DIRECTIONS,
            AndroidAutoNavigationTelemetry.inactive().toJson()
        )
        sessionDebouncer.reset()
        lastSessionValue = AndroidAutoTelemetryKeys.SESSION_STOPPED
    }

    private fun publishDirections(
        update: AndroidAutoNavigationTelemetry.Directions,
        nowMs: Long,
        force: Boolean
    ) {
        val json = if (force) {
            directionsPublisher.reset()
            directionsPublisher.onUpdate(update, nowMs)
                ?: update.toJson()
        } else {
            directionsPublisher.onUpdate(update, nowMs)
        } ?: return
        ServiceManager.getInstance().dispatchTelemetryOnly(AndroidAutoTelemetryKeys.DIRECTIONS, json)
    }

    private fun attachIfSessionAllows(source: String) {
        mainHandler.post { reconcileOutput(source) }
    }

    private fun reconcileOutput(source: String) {
        // Re-read demand on the main thread: an older posted enable must not
        // recreate output after a rapid disable or session disconnect.
        if (!wantsOutput()) {
            surfaceAttached.set(false)
            notifyHostProjectionFlag(false)
            if (deliveredGeneration != -1L) client.setOutput(null)
            deliveredGeneration = -1L
            AaClusterVideoHost.hide()
            return
        }
        if (!AaClusterVideoHost.isShown()) {
            surfaceAttached.set(false)
            notifyHostProjectionFlag(false)
            if (!AaClusterVideoHost.show(App.getContext())) {
                Log.w(TAG, "CLUSTER awaits Presentation ($source)")
                return
            }
        }
        AaClusterVideoHost.peekOutput()?.let {
            onSurfaceAvailable(it, AaClusterVideoHost.surfaceGeneration())
        }
    }

    /** The client borrows the owned consumer until an authenticated terminal release. */
    internal fun onSurfaceAvailable(surface: ClusterSurfaceOutput, generation: Long) {
        val epoch = demandEpoch.get()
        if (wantsOutput() && surface.isAvailable && deliveredGeneration == generation && deliveredDemandEpoch == epoch) return
        surfaceAttached.set(false)
        notifyHostProjectionFlag(false)
        if (wantsOutput() && surface.isAvailable) {
            deliveredGeneration = generation
            deliveredDemandEpoch = epoch
            client.setOutput(surface)
        } else {
            deliveredGeneration = -1L
            client.setOutput(null)
        }
    }

    internal fun onSurfaceDestroyed() {
        deliveredGeneration = -1L
        surfaceAttached.set(false)
        notifyHostProjectionFlag(false)
        client.setOutput(null)
    }

    internal fun onHostAvailable() {
        mainHandler.post { reconcileOutput("presentation_available") }
    }

    private fun detachSurface(reason: String) {
        surfaceAttached.set(false)
        notifyHostProjectionFlag(false)
        mainHandler.post {
            reconcileOutput(reason)
        }
    }

    /**
     * Watchdog (owner's rule): a route is projected and the cluster map is on, but the map has not
     * been live for [AndroidAutoClusterStreamHealth.NOT_LIVE_KICK_AFTER_MS]: whatever made it drop,
     * request it again from scratch, at most once per minute.
     */
    private fun kickIfNotLiveTooLong(nowMs: Long) {
        if (notLiveSinceMs <= 0L) notLiveSinceMs = nowMs
        if (!AndroidAutoClusterStreamHealth.shouldKickNotLive(
                nowMs, notLiveSinceMs, lastNotLiveKickAtMs, recoveryInFlight)) return
        lastNotLiveKickAtMs = nowMs
        notLiveSinceMs = nowMs
        val epoch = demandEpoch.incrementAndGet()
        recoveryCount = 0
        lastRecoveryAtMs = 0L
        decoderFailurePending = false
        deliveredGeneration = -1L
        Log.w(TAG, "CLUSTER watchdog: route + cluster map on but not live; requesting it again")
        ClusterPersistentEventLogger.log("aa_cluster_watchdog_rerequest", mapOf("demand_epoch" to epoch))
        client.setOutput(null)
        mainHandler.postDelayed({
            if (demandEpoch.get() == epoch && wantsOutput()) reconcileOutput("not_live_watchdog")
        }, 250L)
    }

    private fun checkClusterStreamHealth() {
        val nowMs = SystemClock.elapsedRealtime()
        val lastFrameAtMs = AaClusterVideoHost.lastFrameElapsedRealtimeMs()
        if (recoveryInFlight && recoveryDemandEpoch != demandEpoch.get()) {
            recoveryInFlight = false
            recoveryDemandEpoch = -1L
        }
        if (AndroidAutoClusterStreamHealth.shouldResetRecoveryBudget(
                nowMs = nowMs,
                liveSinceMs = liveSinceMs,
                lastFrameAtMs = lastFrameAtMs,
                lastRecoveryAtMs = lastRecoveryAtMs,
                recoveryCount = recoveryCount
            )) {
            ClusterPersistentEventLogger.log(
                "aa_cluster_recovery_budget_rearmed",
                mapOf(
                    "previous_recovery_count" to recoveryCount,
                    "healthy_ms" to (nowMs - lastRecoveryAtMs),
                    "frame_age_ms" to (nowMs - lastFrameAtMs)
                )
            )
            recoveryCount = 0
            lastRecoveryAtMs = 0L
        }
        if (AndroidAutoClusterStreamHealth.shouldRearmExhaustedBudget(nowMs, lastRecoveryAtMs, recoveryCount)) {
            ClusterPersistentEventLogger.log(
                "aa_cluster_recovery_budget_cooldown_rearmed",
                mapOf("previous_recovery_count" to recoveryCount, "since_ms" to (nowMs - lastRecoveryAtMs))
            )
            recoveryCount = 0
            lastRecoveryAtMs = 0L
        }

        if (recoveryInFlight) {
            if (AndroidAutoClusterStreamHealth.isRecoveryTimedOut(nowMs, lastRecoveryAtMs)) {
                recoveryInFlight = false
                AaClusterVideoHost.setLive(false)
                notifyHostProjectionFlag(false)
                Log.w(TAG, "CLUSTER recovery timed out; reconciling requested output")
                ClusterPersistentEventLogger.log(
                    "aa_cluster_recovery_timeout",
                    mapOf(
                        "elapsed_ms" to (nowMs - lastRecoveryAtMs),
                        "recovery_count" to recoveryCount,
                        "requested" to wantsOutput()
                    )
                )
                if (wantsOutput() &&
                    recoveryCount < AndroidAutoClusterStreamHealth.MAX_RECOVERIES_PER_BURST
                ) {
                    recoveryCount++
                    lastRecoveryAtMs = nowMs
                    recoveryInFlight = true
                    recoveryDemandEpoch = demandEpoch.incrementAndGet()
                    deliveredGeneration = -1L
                    surfaceAttached.set(false)
                    client.setOutput(null)
                    mainHandler.postDelayed({ reconcileOutput("frame_stall_recovery_timeout") }, 250L)
                } else {
                    ClusterPersistentEventLogger.log(
                        "aa_cluster_recovery_burst_exhausted",
                        mapOf("recovery_count" to recoveryCount, "requested" to wantsOutput())
                    )
                }
            }
            return
        }
        if (decoderFailurePending) {
            val requested = wantsOutput()
            if (!requested) {
                decoderFailurePending = false
                return
            }
            val output = AaClusterVideoHost.peekOutput()
            if (!AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
                    pending = true,
                    outputRequested = requested,
                    consumerAvailable = output?.isAvailable == true,
                    nowMs = nowMs,
                    lastRecoveryAtMs = lastRecoveryAtMs,
                    recoveryCount = recoveryCount
                )) return
            decoderFailurePending = false
            recoveryInFlight = true
            recoveryCount++
            lastRecoveryAtMs = nowMs
            val epoch = demandEpoch.incrementAndGet()
            recoveryDemandEpoch = epoch
            deliveredGeneration = -1L
            ClusterPersistentEventLogger.log(
                "aa_cluster_decoder_failure_recovery",
                mapOf("recovery_count" to recoveryCount, "demand_epoch" to epoch)
            )
            // The client waits for authenticated RELEASED before exposing any new output.
            client.setOutput(null)
            mainHandler.postDelayed({
                if (recoveryDemandEpoch != epoch) return@postDelayed
                if (demandEpoch.get() == epoch && wantsOutput() && output?.isAvailable == true) {
                    reconcileOutput("decoder_failure_recovery")
                } else {
                    recoveryInFlight = false
                    recoveryDemandEpoch = -1L
                }
            }, 250L)
            return
        }
        if (!wantsOutput()) {
            notLiveSinceMs = 0L
            return
        }
        if (!surfaceAttached.get()) {
            kickIfNotLiveTooLong(nowMs)
            return
        }
        notLiveSinceMs = 0L

        val speedKmh = ServiceManager.getInstance()
            .getData(CarConstants.CAR_BASIC_VEHICLE_SPEED.value)
            ?.replace(',', '.')
            ?.toFloatOrNull()
        if (!AndroidAutoClusterStreamHealth.shouldRecover(
                nowMs = nowMs,
                liveSinceMs = liveSinceMs,
                lastFrameAtMs = lastFrameAtMs,
                lastRecoveryAtMs = lastRecoveryAtMs,
                recoveryCount = recoveryCount,
                speedKmh = speedKmh
            )) return

        val surface = AaClusterVideoHost.peekOutput() ?: return
        val frameAgeMs = nowMs - maxOf(liveSinceMs, lastFrameAtMs)
        recoveryInFlight = true
        recoveryCount++
        lastRecoveryAtMs = nowMs
        recoveryDemandEpoch = demandEpoch.incrementAndGet()
        deliveredGeneration = -1L
        surfaceAttached.set(false)
        AaClusterVideoHost.setLive(false)
        notifyHostProjectionFlag(false)
        Log.w(
            TAG,
            "CLUSTER frame stall ageMs=$frameAgeMs speedKmh=$speedKmh recovery=$recoveryCount; recycling output"
        )
        ClusterPersistentEventLogger.log(
            "aa_cluster_frame_stall_recovery",
            mapOf(
                "frame_age_ms" to frameAgeMs,
                "speed_kmh" to speedKmh,
                "recovery_count" to recoveryCount
            )
        )
        client.setOutput(null)
        mainHandler.postDelayed({
            if (wantsOutput() && surface.isAvailable) {
                reconcileOutput("frame_stall_recovery")
            } else {
                recoveryInFlight = false
            }
        }, 250L)
    }

    private fun notifyHostProjectionFlag(enabled: Boolean) {
        ServiceManager.getInstance().dispatchServiceManagerEvent(
            br.com.redesurftank.havalshisuku.models.ServiceManagerEventType.AA_CLUSTER_SURFACE,
            enabled
        )
    }

}
