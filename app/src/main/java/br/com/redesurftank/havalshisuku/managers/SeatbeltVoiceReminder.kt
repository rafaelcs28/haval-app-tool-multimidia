package br.com.redesurftank.havalshisuku.managers

import android.content.Context
import android.media.AudioAttributes
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
    // Tempo que o cinto precisa ficar PRESO pra "re-armar" (voltar a poder avisar). Evita que um
    // flicker do sensor ou um reajuste rápido (solta->prende->solta em <3s) conte como novo evento.
    const val REARM_FASTENED_MS = 3_000L
    val KNOWN_SEATS = 0..4

    // Regra (escolha do usuário): avisa UMA vez a cada transição PRESO->SOLTO com o carro andando;
    // enquanto seguir solto, não repete; re-arma depois de ficar PRESO por REARM_FASTENED_MS.
    // warned = já avisamos neste episódio de "solto". fastenedSinceMs = desde quando está PRESO
    // (0 = solto). Ausente do mapa = default = armado e solto/desconhecido.
    data class SeatState(val warned: Boolean = false, val fastenedSinceMs: Long = 0L)

    data class Decision(
        val announceSeats: List<Int>,
        val newState: Map<Int, SeatState>,
        // Prazo pra re-checar e completar o re-arm (cinto preso mas ainda dentro do debounce).
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
            val cur = state[seat] ?: SeatState()
            if (seat in unbelted) {
                // Cinto SOLTO.
                if (!cur.warned && moving) {
                    announce += seat
                    newState[seat] = SeatState(warned = true, fastenedSinceMs = 0L)
                } else if (cur.warned) {
                    // Já avisado neste episódio -> não repete enquanto seguir solto.
                    newState[seat] = SeatState(warned = true, fastenedSinceMs = 0L)
                }
                // else (!warned && parado): default (armado) -> avisa quando começar a andar.
            } else {
                // Cinto PRESO. Re-arma (warned=false) após REARM_FASTENED_MS preso continuamente.
                val since = if (cur.fastenedSinceMs == 0L) nowMs else cur.fastenedSinceMs
                val elapsed = nowMs - since
                if (cur.warned && elapsed < REARM_FASTENED_MS) {
                    // Ainda no debounce: mantém "avisado" e re-checa pra completar o re-arm.
                    newState[seat] = SeatState(warned = true, fastenedSinceMs = since)
                    wantRecheck(REARM_FASTENED_MS - elapsed)
                }
                // else: re-armado (ou nunca avisado) -> default, não guarda.
            }
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
    const val DEFAULT_MIN_VOLUME_PCT = 60

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
        if (!isEnabled()) {
            ClusterPersistentEventLogger.log(DIAG_EVENT, mapOf("skip" to "disabled"))
            return
        }
        pendingJob?.cancel()
        pendingJob =
            scope.launch {
                delay(SeatbeltVoiceLogic.DEBOUNCE_MS + extraDelayMs)
                mutex.withLock {
                    val before = state
                    val rawSpeed = speedRaw()
                    val unbelted = SeatbeltVoiceLogic.parseUnbelted(beltRaw())
                    val moving = SeatbeltVoiceLogic.isMoving(rawSpeed)
                    val decision =
                        SeatbeltVoiceLogic.evaluate(before, unbelted, moving, System.currentTimeMillis())
                    if (decision.announceSeats.isEmpty()) {
                        // Diag do "não anunciou": mostra POR QUÊ (parado? sem cinto? já avisado?).
                        ClusterPersistentEventLogger.log(
                            DIAG_EVENT,
                            mapOf(
                                "eval" to "no_announce",
                                "moving" to moving,
                                "speed" to (rawSpeed ?: "null"),
                                "unbelted" to unbelted.toString(),
                                "state" to before.mapValues { "warned=${it.value.warned}" }.toString()
                            )
                        )
                        state = decision.newState
                    } else if (isInCall()) {
                        // Não fala por cima de uma ligação REAL. NÃO gasta o aviso: reverte os
                        // assentos anunciáveis e tenta de novo em 15s.
                        val reverted = decision.newState.toMutableMap()
                        for (seat in decision.announceSeats) {
                            val old = before[seat]
                            if (old == null) reverted.remove(seat) else reverted[seat] = old
                        }
                        state = reverted
                        ClusterPersistentEventLogger.log(
                            DIAG_EVENT,
                            mapOf("skip" to "in_call", "seats" to decision.announceSeats.toString())
                        )
                        schedule(extraDelayMs = FOCUS_RETRY_MS)
                    } else {
                        state = decision.newState
                        // 2+ soltos ao mesmo tempo -> uma frase genérica (multi); 1 só -> frase
                        // do assento. Ambas preferem o arquivo externo, senão a versão embutida.
                        val multi = decision.announceSeats.size >= 2
                        ClusterPersistentEventLogger.log(
                            DIAG_EVENT,
                            mapOf(
                                "announce" to decision.announceSeats.toString(),
                                "moving" to moving,
                                "mode" to if (multi) "multi" else "por_assento"
                            )
                        )
                        // NÃO pega audio focus: a rádio/mídia do OEM PAUSA no focus-loss e NÃO volta
                        // sozinha depois (o app da rádio não trata o refoco). Tocamos MIXADO por cima
                        // (o volume mínimo garante que dá pra ouvir), então a rádio nunca para.
                        withBoostedVolume {
                            if (multi) {
                                playAwait { setMultiSource(it) }
                            } else {
                                playAwait { setSeatSource(it, decision.announceSeats.first()) }
                            }
                        }
                    }
                    decision.recheckInMs?.let { schedule(extraDelayMs = it) }
                }
            }
    }

    // USAGE_MEDIA = canal de mídia (os alto-falantes principais, no volume de mídia). O
    // NAVIGATION_GUIDANCE anterior pode sair mudo neste head unit OEM (canal de TTS separado).
    // NÃO pedimos audio focus (mixa por cima da rádio/mídia sem pausá-la — a rádio do OEM não
    // volta sozinha após o focus-loss); o volume mínimo garante a audibilidade.
    private val audioAttrs =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    private fun audioManager(): AudioManager =
        App.getContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Só ligação REAL bloqueia o aviso (não queremos falar por cima de uma chamada).
    private fun isInCall(): Boolean =
        try {
            val m = audioManager().mode
            m == AudioManager.MODE_IN_CALL || m == AudioManager.MODE_IN_COMMUNICATION
        } catch (e: Exception) {
            false
        }

    private fun minVolumePct(): Int =
        App.getDeviceProtectedContext()
            .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
            .getInt(SharedPreferencesKeys.SEATBELT_VOICE_MIN_VOLUME_PCT.key, DEFAULT_MIN_VOLUME_PCT)
            .coerceIn(0, 100)

    // Garante um volume MÍNIMO no canal de mídia durante a fala (mesmo no mudo) e RESTAURA depois.
    // pct=0 desliga o boost (respeita o volume atual, inclusive mudo). Restaura no finally, então
    // um cancelamento no meio da fala também devolve o volume original.
    private suspend fun withBoostedVolume(block: suspend () -> Unit) {
        val am = runCatching { audioManager() }.getOrNull()
        var restoreTo = -1
        var restoreMuted = false
        if (am != null) {
            runCatching {
                val pct = minVolumePct()
                if (pct > 0) {
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    // No MUDO, getStreamVolume devolve o volume de ANTES do mudo (não 0), então
                    // checar só cur<target deixava passar o caso mudo. Checa isStreamMute explícito.
                    val muted = runCatching { am.isStreamMute(AudioManager.STREAM_MUSIC) }.getOrDefault(false)
                    val target = Math.ceil(pct / 100.0 * max).toInt().coerceIn(1, max)
                    if (muted || cur < target) {
                        // setStreamVolume com valor positivo já des-muta o canal.
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                        restoreTo = cur
                        restoreMuted = muted
                        ClusterPersistentEventLogger.log(
                            DIAG_EVENT,
                            mapOf("volBoost" to "$cur->$target", "max" to max, "pct" to pct, "wasMuted" to muted)
                        )
                    }
                }
            }.onFailure { Log.e(TAG, "boost de volume falhou", it) }
        }
        try {
            block()
        } finally {
            if (am != null && restoreTo >= 0) {
                runCatching {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, restoreTo, 0)
                    // Re-muta se estava mudo antes (o usuário deixou no mudo de propósito).
                    if (restoreMuted) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    }
                }
            }
        }
    }

    // Botão de teste (parado): toca a frase do motorista pelo MESMO caminho de áudio, ignorando
    // toda a lógica de cinto/velocidade/estado. Valida rota + volume sem precisar dirigir.
    @JvmStatic
    fun playTest() {
        scope.launch {
            ClusterPersistentEventLogger.log(DIAG_EVENT, mapOf("test" to "play", "inCall" to isInCall()))
            // Sem audio focus (mixa por cima; não pausa a rádio) + volume mínimo garantido.
            withBoostedVolume { playAwait { setSeatSource(it, 0) } }
        }
    }

    private fun rawResForSeat(seat: Int): Int =
        when (seat) {
            0 -> R.raw.seatbelt_voice_seat0
            1 -> R.raw.seatbelt_voice_seat1
            2 -> R.raw.seatbelt_voice_seat2
            4 -> R.raw.seatbelt_voice_seat4
            else -> R.raw.seatbelt_voice_seat3 // 3 = meio; também fallback
        }

    // Override externo (troca de voz sem rebuild): <base>.mp3 ou .m4a na pasta de arquivos do app.
    private fun resolveExternal(base: String): File? =
        listOf("$base.mp3", "$base.m4a")
            .map { File(App.getContext().getExternalFilesDir(null), it) }
            .firstOrNull { it.isFile && it.length() > 0 }

    private fun setSeatSource(player: MediaPlayer, seat: Int) {
        val custom = resolveExternal("seatbelt_voice_seat$seat")
        if (custom != null) {
            player.setDataSource(custom.absolutePath)
        } else {
            App.getContext().resources.openRawResourceFd(rawResForSeat(seat)).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
        }
    }

    private fun setMultiSource(player: MediaPlayer) {
        val custom = resolveExternal("seatbelt_voice_multi")
        if (custom != null) {
            player.setDataSource(custom.absolutePath)
        } else {
            App.getContext().resources.openRawResourceFd(R.raw.seatbelt_voice_multi).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
        }
    }

    // Toca UM áudio e ESPERA terminar (fila sequencial quando há vários). Player e foco nunca vazam:
    // release no finally (a revisão pegou vazamento de foco em exceção do prepare — mídia do carro
    // ficava duckada pra sempre). setSource pode lançar (arquivo ruim) — está dentro do try.
    private suspend fun playAwait(setSource: (MediaPlayer) -> Unit) {
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(audioAttrs)
            setSource(player)
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
            Log.e(TAG, "falha ao tocar aviso de cinto", e)
        } finally {
            runCatching { player.release() }
        }
    }
}
