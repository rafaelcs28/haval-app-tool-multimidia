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
    const val DEFAULT_DUCK_MUSIC = true
    // Duck MANUAL: a música (STREAM_MUSIC) desce pra este % do máximo enquanto a fala toca; restaura depois.
    private const val DUCK_MUSIC_PCT = 20

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
                        // withBoostedVolume: (1) DUCK via audio focus transitório -> a música abaixa
                        // durante a fala e volta depois (sem se misturar); (2) volume mínimo garante
                        // audibilidade. Escape se a rádio do OEM não retomar: pref
                        // SEATBELT_VOICE_DUCK_MUSIC=false (volta ao mix puro por cima).
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

    // A fala toca em STREAM_ALARM (USAGE_ALARM), stream SEPARADO da música (STREAM_MUSIC). Assim o
    // duck manual (withBoostedVolume) ABAIXA a música SEM abaixar a fala — e SEM audio-focus, que
    // nesta pilha OEM PAUSA a mídia e não retoma. (USAGE_MEDIA colava a fala no mesmo stream da
    // música; NAVIGATION_GUIDANCE saía mudo no canal de TTS do OEM — por isso ALARM.)
    private val audioAttrs =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
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

    private fun duckMusicEnabled(): Boolean =
        App.getDeviceProtectedContext()
            .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
            .getBoolean(SharedPreferencesKeys.SEATBELT_VOICE_DUCK_MUSIC.key, DEFAULT_DUCK_MUSIC)

    // Prepara o áudio do alerta e RESTAURA tudo no finally:
    //  (1) DUCK MANUAL: abaixa a MÚSICA (STREAM_MUSIC) enquanto a fala toca — SEM audio-focus, que
    //      nesta pilha OEM PAUSA a mídia e não retoma. A fala toca em STREAM_ALARM (stream separado),
    //      então NÃO é abaixada junto. Desliga com o pref SEATBELT_VOICE_DUCK_MUSIC=false.
    //  (2) BOOST: garante um volume MÍNIMO no stream do alerta (STREAM_ALARM); pct=0 desliga o boost.
    //      Um cancelamento no meio da fala também devolve os dois volumes (finally).
    private suspend fun withBoostedVolume(block: suspend () -> Unit) {
        val am = runCatching { audioManager() }.getOrNull()
        var restoreMusicTo = -1
        var restoreAlarmTo = -1
        if (am != null) {
            // (1) DUCK MANUAL da música (sem foco -> não pausa a fonte).
            if (duckMusicEnabled()) {
                runCatching {
                    val musicMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val musicCur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val duckTo = Math.floor(DUCK_MUSIC_PCT / 100.0 * musicMax).toInt().coerceIn(0, musicCur)
                    if (duckTo < musicCur) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, duckTo, 0)
                        restoreMusicTo = musicCur
                        ClusterPersistentEventLogger.log(
                            DIAG_EVENT,
                            mapOf("duckMusic" to "$musicCur->$duckTo", "max" to musicMax)
                        )
                    }
                }.onFailure { Log.e(TAG, "duck manual da música falhou", it) }
            }
            // (2) BOOST — volume mínimo do stream do alerta (STREAM_ALARM, onde a fala toca).
            runCatching {
                val pct = minVolumePct()
                if (pct > 0) {
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val cur = am.getStreamVolume(AudioManager.STREAM_ALARM)
                    val target = Math.ceil(pct / 100.0 * max).toInt().coerceIn(1, max)
                    if (cur < target) {
                        am.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
                        restoreAlarmTo = cur
                        ClusterPersistentEventLogger.log(
                            DIAG_EVENT,
                            mapOf("alarmBoost" to "$cur->$target", "max" to max, "pct" to pct)
                        )
                    }
                }
            }.onFailure { Log.e(TAG, "boost do alerta falhou", it) }
        }
        try {
            block()
        } finally {
            if (am != null) {
                if (restoreMusicTo >= 0) runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, restoreMusicTo, 0) }
                if (restoreAlarmTo >= 0) runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, restoreAlarmTo, 0) }
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

    // ======================= VARIANTES ALEATORIAS DE VOZ =======================
    // Objetivo: NAO falar sempre a mesma frase. Cada "slot" (cada assento e o multi) pode ter N
    // audios; sorteia um, EVITANDO REPETIR O ULTIMO tocado daquele slot — sorteio puro repete e mata
    // a graca justamente na 2a vez, que e quando a pessoa presta atencao.
    //
    // De onde vem as variantes (as duas fontes entram no MESMO sorteio):
    //  - EXTERNAS, sem rebuild: qualquer arquivo na pasta de arquivos do app cujo nome comece com a
    //    base e termine em .mp3/.m4a. Ex.: seatbelt_voice_seat0.mp3, seatbelt_voice_seat0_vovo.mp3,
    //    seatbelt_voice_seat0_narrador.mp3. Basta jogar os arquivos la (o app le na proxima fala,
    //    sem restart).
    //  - EMBUTIDAS, sobrevivem a reinstalacao limpa: res/raw/<base>.mp3 e <base>_2 .. <base>_9.
    //
    // ATENCAO (armadilha real): as embutidas sao resolvidas por NOME (getIdentifier) e o build usa
    // isShrinkResources=true -> sem referencia estatica o encolhedor de recursos as REMOVE do APK,
    // em silencio. Por isso existe res/raw/keep.xml com tools:keep="@raw/seatbelt_voice_*".
    private const val MAX_EMBEDDED_VARIANTS = 9

    /** Uma opcao de audio sorteavel. [key] identifica pra nao repetir; [apply] aponta o player. */
    private class VoiceVariant(val key: String, val apply: (MediaPlayer) -> Unit)

    /** Ultima variante tocada por slot (base) — usada pra nao repetir em sequencia. */
    private val lastVariantKeyBySlot = HashMap<String, String>()

    /** Arquivos externos que pertencem a este slot. */
    private fun externalVariants(base: String): List<File> {
        val dir = App.getContext().getExternalFilesDir(null) ?: return emptyList()
        val files = dir.listFiles() ?: return emptyList()
        return files
            .filter { file ->
                if (!file.isFile || file.length() <= 0L) return@filter false
                val name = file.name
                val lower = name.lowercase()
                if (!lower.endsWith(".mp3") && !lower.endsWith(".m4a")) return@filter false
                if (!name.startsWith(base)) return@filter false
                // O que vem DEPOIS da base tem de ser a extensao ou "_sufixo" — assim "…seat0" nunca
                // engole um hipotetico "…seat01" nem o proprio "…seat0" casa com outro slot.
                val rest = name.removePrefix(base)
                rest.startsWith(".") || rest.startsWith("_")
            }
            .sortedBy { it.name } // ordem estavel: o sorteio e que varia, nao a lista
    }

    /** Recursos embutidos deste slot: <base>, <base>_2 .. <base>_9 (os que existirem). */
    private fun embeddedVariantResIds(base: String): List<Pair<String, Int>> {
        val ctx = App.getContext()
        val pkg = ctx.packageName
        val found = ArrayList<Pair<String, Int>>()
        val names = ArrayList<String>(MAX_EMBEDDED_VARIANTS)
        names.add(base)
        for (i in 2..MAX_EMBEDDED_VARIANTS) names.add("${base}_$i")
        for (name in names) {
            val id = ctx.resources.getIdentifier(name, "raw", pkg)
            if (id != 0) found.add(name to id)
        }
        return found
    }

    /**
     * Sorteia uma variante do slot, sem repetir a ultima. Externas + embutidas concorrem juntas.
     * Fallback: se nada for encontrado (ex.: encolhedor removeu os raws E nao ha arquivo externo),
     * cai no recurso passado em [fallbackResId] pra o aviso NUNCA ficar mudo.
     */
    @Synchronized
    private fun pickVariant(base: String, fallbackResId: Int): VoiceVariant {
        val options = ArrayList<VoiceVariant>()
        for (file in externalVariants(base)) {
            options.add(VoiceVariant("ext:${file.name}") { it.setDataSource(file.absolutePath) })
        }
        for ((name, resId) in embeddedVariantResIds(base)) {
            options.add(
                VoiceVariant("raw:$name") { player ->
                    App.getContext().resources.openRawResourceFd(resId).use { afd ->
                        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                }
            )
        }
        if (options.isEmpty()) {
            return VoiceVariant("raw:fallback") { player ->
                App.getContext().resources.openRawResourceFd(fallbackResId).use { afd ->
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            }
        }
        val last = lastVariantKeyBySlot[base]
        val pool = options.filter { it.key != last }.ifEmpty { options }
        val chosen = pool.random()
        lastVariantKeyBySlot[base] = chosen.key
        ClusterPersistentEventLogger.log(
            DIAG_EVENT,
            mapOf(
                "variant" to chosen.key,
                "slot" to base,
                "options" to options.size,
                "previous" to (last ?: "-")
            )
        )
        return chosen
    }

    private fun setSeatSource(player: MediaPlayer, seat: Int) {
        pickVariant("seatbelt_voice_seat$seat", rawResForSeat(seat)).apply(player)
    }

    private fun setMultiSource(player: MediaPlayer) {
        pickVariant("seatbelt_voice_multi", R.raw.seatbelt_voice_multi).apply(player)
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
