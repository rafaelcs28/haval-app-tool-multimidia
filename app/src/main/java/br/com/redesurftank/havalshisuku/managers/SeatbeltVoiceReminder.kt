package br.com.redesurftank.havalshisuku.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.R
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger
import br.com.redesurftank.havalshisuku.listeners.IDataChanged
import br.com.redesurftank.havalshisuku.models.CarConstants
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Lógica PURA do aviso de voz do cinto (testável na JVM, sem Android).
 * Máquina de estados POR ASSENTO (0=motorista, 1=passageiro frente, 2=trás esq., 3=meio, 4=trás dir.):
 * - FRESH: nunca avisado neste ciclo de ignição. Solto + em movimento -> AVISA (frase do assento).
 * - ANNOUNCED: já avisado; segue solto -> NUNCA insiste (regra do usuário).
 * - ARMED: afivelou depois de avisado -> re-elegível.
 * - REARM_PENDING: soltou de novo; se ficar solto por 30s (e em movimento) -> AVISA de novo.
 *   Se re-afivelar dentro dos 30s -> volta a ARMED sem falar.
 * Ciclo de ignição (ENGINE_STATE off->on) zera tudo pra FRESH.
 */
internal object SeatbeltVoiceLogic {
    const val DEBOUNCE_MS = 3_000L
    const val REARM_GRACE_MS = 30_000L
    val KNOWN_SEATS = 0..4

    enum class SeatPhase { FRESH, ANNOUNCED, ARMED, REARM_PENDING }

    data class SeatState(val phase: SeatPhase, val sinceMs: Long = 0L)

    data class Decision(
        val announceSeats: List<Int>,
        val newState: Map<Int, SeatState>,
        // Menor prazo pendente (janela de 30s correndo) — o gerente agenda re-checagem.
        val recheckInMs: Long? = null
    )

    /** "(0,1,0,0,0)" / "{0,1,0,0,0}" -> índices com "1"; só assentos conhecidos (0..4). */
    fun parseUnbelted(raw: String?): Set<Int> =
        raw.orEmpty()
            .replace("(", "").replace(")", "")
            .replace("{", "").replace("}", "")
            .split(",")
            .mapIndexedNotNull { i, v -> if (v.trim() == "1" && i in KNOWN_SEATS) i else null }
            .toSet()

    /** Movendo = velocidade lida e > 0.5 km/h. Ilegível = parado (fail-closed: sem voz). */
    fun isMoving(rawSpeed: String?): Boolean =
        (rawSpeed?.trim()?.toFloatOrNull() ?: 0f) > 0.5f

    /** Mesmo conjunto off do TripConsistency (produção): -1/10/14/15 = desligado. */
    fun isEngineOff(rawEngineState: String?): Boolean =
        rawEngineState?.trim() in setOf("-1", "10", "14", "15")

    fun evaluate(
        state: Map<Int, SeatState>,
        unbelted: Set<Int>,
        moving: Boolean,
        nowMs: Long
    ): Decision {
        val newState = mutableMapOf<Int, SeatState>()
        val announce = mutableListOf<Int>()
        var recheck: Long? = null
        fun wantRecheck(inMs: Long) {
            recheck = minOf(recheck ?: Long.MAX_VALUE, inMs.coerceAtLeast(0))
        }

        for (seat in (state.keys + unbelted).sorted()) {
            val cur = state[seat] ?: SeatState(SeatPhase.FRESH)
            val next: SeatState =
                if (seat !in unbelted) {
                    when (cur.phase) {
                        // Afivelou depois de avisado (ou dentro da janela de 30s) -> re-elegível.
                        SeatPhase.ANNOUNCED, SeatPhase.REARM_PENDING -> SeatState(SeatPhase.ARMED, nowMs)
                        else -> cur
                    }
                } else {
                    when (cur.phase) {
                        SeatPhase.FRESH ->
                            if (moving) {
                                announce += seat
                                SeatState(SeatPhase.ANNOUNCED, nowMs)
                            } else {
                                cur // parado não fala; flip de velocidade reavalia
                            }
                        SeatPhase.ARMED -> {
                            // Soltou de novo: 30s de tolerância pra re-afivelar antes de falar.
                            wantRecheck(REARM_GRACE_MS)
                            SeatState(SeatPhase.REARM_PENDING, nowMs)
                        }
                        SeatPhase.REARM_PENDING -> {
                            val elapsed = nowMs - cur.sinceMs
                            if (elapsed >= REARM_GRACE_MS) {
                                if (moving) {
                                    announce += seat
                                    SeatState(SeatPhase.ANNOUNCED, nowMs)
                                } else {
                                    cur // janela vencida mas parado; fala quando andar
                                }
                            } else {
                                wantRecheck(REARM_GRACE_MS - elapsed)
                                cur
                            }
                        }
                        // Regra do usuário: quem ignorou o aviso não é mais incomodado.
                        SeatPhase.ANNOUNCED -> cur
                    }
                }
            if (next.phase != SeatPhase.FRESH) newState[seat] = next
        }
        return Decision(announce, newState, recheck)
    }
}

/**
 * Aviso de voz PERSONALIZADO por assento quando alguém está sem cinto com o carro em movimento
 * (ex.: "Você aí no banco de trás, do lado do motorista: bota o cinto!"). Vozes embutidas
 * (res/raw/seatbelt_voice_seat0..4, voz "Grandma" pt-BR); troca sem rebuild colocando
 * `seatbelt_voice_seat<N>.mp3` (ou .m4a) em /sdcard/Android/data/<pkg>/files/.
 * Reset por ciclo de ignição via CAR_BASIC_ENGINE_STATE (a multimídia NÃO reinicia quando o
 * carro dorme — o app fica vivo, então o reset é pelo CAN).
 */
object SeatbeltVoiceReminder {
    private const val TAG = "SeatbeltVoice"
    private const val PLAY_TIMEOUT_MS = 15_000L
    private const val FOCUS_RETRY_MS = 15_000L
    private const val DIAG_EVENT = "seatbelt_voice"

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO +
                CoroutineExceptionHandler { _, e -> Log.e(TAG, "coroutine falhou", e) }
        )
    private val mutex = Mutex()
    private var state: Map<Int, SeatbeltVoiceLogic.SeatState> = emptyMap()
    private var pendingJob: Job? = null
    private var registered = false

    // Valores FRESCOS dos eventos (dataCache só atualiza depois do dispatch dos listeners).
    @Volatile private var lastBeltRaw: String? = null
    @Volatile private var lastBeltSet: Set<Int>? = null
    @Volatile private var lastSpeedRaw: String? = null
    @Volatile private var lastEngineOff: Boolean? = null

    private val listener =
        IDataChanged { key, value ->
            when (key) {
                CarConstants.CAR_BASIC_SEAT_BELT_WARNING.value -> {
                    lastBeltRaw = value
                    val set = SeatbeltVoiceLogic.parseUnbelted(value)
                    // Dedup: payload igual não re-agenda (flapping do CAN não pode estrangular o
                    // debounce cancel-replace pra sempre). Mudança real fica no diagnóstico.
                    if (set != lastBeltSet) {
                        lastBeltSet = set
                        ClusterPersistentEventLogger.log(
                            DIAG_EVENT,
                            mapOf("belt" to (value ?: "null"), "unbelted" to set.toString())
                        )
                        schedule()
                    }
                }
                CarConstants.CAR_BASIC_VEHICLE_SPEED.value -> {
                    val wasMoving = SeatbeltVoiceLogic.isMoving(lastSpeedRaw)
                    lastSpeedRaw = value
                    // Velocidade spamma; só interessa o flip parado->andando.
                    if (!wasMoving && SeatbeltVoiceLogic.isMoving(value)) schedule()
                }
                CarConstants.CAR_BASIC_ENGINE_STATE.value -> {
                    val off = SeatbeltVoiceLogic.isEngineOff(value)
                    val was = lastEngineOff
                    lastEngineOff = off
                    if (was == true && !off) {
                        scope.launch {
                            mutex.withLock { state = emptyMap() }
                            ClusterPersistentEventLogger.log(DIAG_EVENT, mapOf("reset" to "ignition"))
                            Log.i(TAG, "ignição ciclada; memória de avisos zerada")
                        }
                    }
                }
            }
        }

    @JvmStatic
    fun initialize() {
        if (registered) return
        ServiceManager.getInstance().addDataChangedListener(listener)
        registered = true
        Log.i(TAG, "registrado")
    }

    private fun isEnabled(): Boolean =
        App.getDeviceProtectedContext()
            .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
            .getBoolean(SharedPreferencesKeys.ENABLE_SEATBELT_VOICE.key, true)

    private fun beltRaw(): String? =
        lastBeltRaw ?: ServiceManager.getInstance().getData(CarConstants.CAR_BASIC_SEAT_BELT_WARNING.value)

    private fun speedRaw(): String? =
        lastSpeedRaw ?: ServiceManager.getInstance().getData(CarConstants.CAR_BASIC_VEHICLE_SPEED.value)

    // Debounce: espera 3s e reavalia com dados frescos — não fala em cima de quem já está puxando
    // o cinto na arrancada. Um job pendente por vez (cancel-replace). @Synchronized porque chega
    // de threads binder do listener E do próprio job (recheck) — sem isso o pendingJob raceia e
    // sobra job órfão furando o debounce.
    @Synchronized
    private fun schedule(extraDelayMs: Long = 0L) {
        if (!isEnabled()) return
        pendingJob?.cancel()
        pendingJob =
            scope.launch {
                delay(SeatbeltVoiceLogic.DEBOUNCE_MS + extraDelayMs)
                mutex.withLock {
                    val before = state
                    val unbelted = SeatbeltVoiceLogic.parseUnbelted(beltRaw())
                    val moving = SeatbeltVoiceLogic.isMoving(speedRaw())
                    val decision =
                        SeatbeltVoiceLogic.evaluate(before, unbelted, moving, System.currentTimeMillis())
                    if (decision.announceSeats.isEmpty()) {
                        state = decision.newState
                    } else {
                        val focusHold = acquireFocus()
                        if (focusHold == null) {
                            // Sem foco (ex.: ligação em andamento): NÃO fala por cima e NÃO gasta o
                            // aviso — reverte os assentos anunciáveis e tenta de novo em 15s.
                            val reverted = decision.newState.toMutableMap()
                            for (seat in decision.announceSeats) {
                                val old = before[seat]
                                if (old == null) reverted.remove(seat) else reverted[seat] = old
                            }
                            state = reverted
                            ClusterPersistentEventLogger.log(
                                DIAG_EVENT,
                                mapOf("focus" to "denied", "seats" to decision.announceSeats.toString())
                            )
                            schedule(extraDelayMs = FOCUS_RETRY_MS)
                        } else {
                            state = decision.newState
                            ClusterPersistentEventLogger.log(
                                DIAG_EVENT,
                                mapOf("announce" to decision.announceSeats.toString(), "moving" to moving)
                            )
                            try {
                                for (seat in decision.announceSeats) {
                                    playSeatAwait(seat)
                                }
                            } finally {
                                releaseFocus(focusHold)
                            }
                        }
                    }
                    decision.recheckInMs?.let { schedule(extraDelayMs = it) }
                }
            }
    }

    private data class FocusHold(val manager: AudioManager, val request: AudioFocusRequest)

    private val audioAttrs =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    private fun acquireFocus(): FocusHold? =
        try {
            val manager = App.getContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val request =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttrs)
                    .build()
            if (manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                FocusHold(manager, request)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "falha ao pedir audio focus", e)
            null
        }

    private fun releaseFocus(hold: FocusHold) {
        runCatching { hold.manager.abandonAudioFocusRequest(hold.request) }
    }

    private fun rawResForSeat(seat: Int): Int =
        when (seat) {
            0 -> R.raw.seatbelt_voice_seat0
            1 -> R.raw.seatbelt_voice_seat1
            2 -> R.raw.seatbelt_voice_seat2
            4 -> R.raw.seatbelt_voice_seat4
            else -> R.raw.seatbelt_voice_seat3 // 3 = meio; também fallback
        }

    // Toca a frase do assento e ESPERA terminar (fila sequencial quando há vários). Player e foco
    // nunca vazam: release no finally (a revisão pegou vazamento de foco em exceção do prepare —
    // mídia do carro ficava duckada pra sempre).
    private suspend fun playSeatAwait(seat: Int) {
        val context = App.getContext()
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(audioAttrs)
            val custom =
                listOf("seatbelt_voice_seat$seat.mp3", "seatbelt_voice_seat$seat.m4a")
                    .map { File(context.getExternalFilesDir(null), it) }
                    .firstOrNull { it.isFile && it.length() > 0 }
            if (custom != null) {
                player.setDataSource(custom.absolutePath)
            } else {
                context.resources.openRawResourceFd(rawResForSeat(seat)).use { afd ->
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            }
            player.prepare()
            val done = CompletableDeferred<Unit>()
            player.setOnCompletionListener { done.complete(Unit) }
            player.setOnErrorListener { _, _, _ ->
                done.complete(Unit)
                true
            }
            player.start()
            withTimeoutOrNull(PLAY_TIMEOUT_MS) { done.await() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Evento novo cancelou o job no meio da fala (ex.: afivelou): para limpo e propaga
            // pra reavaliação rodar — engolir cancelamento quebraria o cancel-replace do debounce.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "falha ao tocar aviso do assento $seat", e)
        } finally {
            runCatching { player.release() }
        }
    }
}
