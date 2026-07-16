package br.com.redesurftank.havalshisuku.ambientlight

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import br.com.redesurftank.havalshisuku.listeners.IDataChanged
import br.com.redesurftank.havalshisuku.managers.ServiceManager
import br.com.redesurftank.havalshisuku.models.CarConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class AmbientLightService : Service() {
    // Handler global: o LED é acessório — exceção não capturada em coroutine (BLE, parse, listener
    // de dados) derrubaria o APP INTEIRO (cluster, barra, tudo). Aqui ela vira só um log.
    private val serviceScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO +
                CoroutineExceptionHandler { _, e -> Log.e(TAG, "coroutine do ambient falhou", e) }
        )
    private lateinit var controller: AmbientLightBleController
    private lateinit var animationController: AmbientLightAnimationController
    private lateinit var musicVisualizerController: MusicVisualizerController
    private lateinit var albumWaveMusicController: AlbumWaveMusicController
    private var driveModeListenerRegistered = false
    private var currentDriveMode = DriveMode.UNKNOWN
    // Marca ~o boot: o serviço sobe cedo (ForegroundService). Usado pra adiar a conexão BLE do LED
    // durante a janela de boot, pra não competir com o Android Auto subindo (ver connectSavedDevice).
    private val serviceStartElapsedMs = SystemClock.elapsedRealtime()

    private var alertListenerRegistered = false
    private var currentAlertCondition: AutomationCondition? = null
    private var enabledAlertKeys: Set<String> = emptySet()
    // Serializa a avaliação de alertas: o listener dispara um coroutine por evento (Dispatchers.IO,
    // multi-thread) e o evaluateAlerts atravessa suspensões (connectAndWait). Sem isto, dois eventos
    // (ex.: abrir+fechar porta, ou BSD esq+dir no mesmo burst) corriam e podiam deixar o alerta travado.
    private val alertMutex = Mutex()

    private val driveModeListener =
        IDataChanged { key, value ->
            if (key == CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE.value) {
                handleDriveModeChanged(value)
            }
        }

    // Motor de automação: uma condição do carro (cinto/ponto cego/porta/ré) -> pisca a fita inteira.
    @Volatile
    private var lastVehicleStopped: Boolean? = null

    private val alertListener =
        IDataChanged { key, value ->
            if (key in enabledAlertKeys) {
                if (key == CarConstants.CAR_BASIC_VEHICLE_SPEED.value) {
                    // Velocidade muda o tempo todo andando: só reavalia quando o estado
                    // parado/andando FLIPA, ou quando há alerta ativo (pra poder desligá-lo).
                    val stopped = isVehicleStopped(value)
                    val flipped = stopped != lastVehicleStopped
                    lastVehicleStopped = stopped
                    if (!flipped && currentAlertCondition == null) return@IDataChanged
                }
                serviceScope.launch { evaluateAlerts() }
            }
        }

    override fun onCreate() {
        super.onCreate()
        controller = AmbientLightBleController.getInstance(this)
        animationController = AmbientLightAnimationController(controller, serviceScope)
        musicVisualizerController =
            MusicVisualizerController(
                context = this,
                controller = controller,
                scope = serviceScope,
                settingsProvider = { AmbientLightSettings.load() },
                baseColorProvider = { musicBaseColor() }
            )
        albumWaveMusicController =
            AlbumWaveMusicController(
                controller = controller,
                scope = serviceScope,
                settingsProvider = { AmbientLightSettings.load() },
                baseColorProvider = { musicBaseColor() }
            )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopAmbientLight()
            return START_NOT_STICKY
        }

        val settings = AmbientLightSettings.load()
        Log.i(TAG, "enabled=${settings.enabled}")
        if (!settings.enabled) {
            stopAmbientLight()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_CONNECT -> connectSavedDevice(settingsWithIntentDevice(intent, settings))
            ACTION_DISCONNECT -> disconnectLed()
            ACTION_SET_BRIGHTNESS -> handleSetBrightness(intent, settings)
            ACTION_TEST_COLOR -> handleTestColor(intent, settings)
            ACTION_SEND_HEX -> handleSendHex(intent, settings)
            ACTION_APPLY_DRIVE_MODE -> handleDriveModeChanged(intent?.getStringExtra(EXTRA_DRIVE_MODE))
            else -> {
                updateDriveModeListener(settings)
                updateAlertListener(settings)
                cacheCurrentDriveMode(settings)
                connectSavedDevice(settings, applyModeAfterConnect = true)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        unregisterDriveModeListener()
        unregisterAlertListener()
        stopMusicEffects()
        animationController.cancel()
        controller.disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectSavedDevice(
        settings: AmbientLightConfig,
        applyModeAfterConnect: Boolean = false
    ) {
        val address = settings.deviceAddress
        if (address.isNullOrBlank()) {
            Log.w(TAG, "connect ignored: no saved LED device")
            stopMusicEffects()
            controller.disconnect()
            return
        }
        serviceScope.launch {
            // No auto-start do boot, ESPERA o Android Auto assentar antes de mexer no BLE. Conectar o
            // LED cedo (junto com o boot do AA) satura o rádio BT compartilhado e deixa o AA lento / com
            // tela preta no cluster. Ações explícitas do usuário (CONNECT/TEST) não passam por aqui.
            if (applyModeAfterConnect) {
                awaitAndroidAutoBootSettle()
            }

            if (settings.musicAnimationEnabled) {
                animationController.cancel()
                startSelectedMusicEffect(settings)
            } else {
                stopMusicEffects()
            }

            val wasConnected = controller.isConnectedTo(address)
            if (connectAndWait(address, settings.autoReconnect)) {
                controller.setBrightness(settings.brightnessPercent, settings.output)
                if (!settings.musicAnimationEnabled) {
                    if (!wasConnected && settings.animationsEnabled) {
                        animationController.welcomeAnimation(currentDriveMode)
                    } else if (applyModeAfterConnect && settings.syncDriveMode) {
                        animationController.applyDriveMode(currentDriveMode)
                    } else if (settings.idleColorEnabled && currentAlertCondition == null) {
                        // Sem música/modo/alerta -> aplica a cor padrão de repouso. Cobre o caso
                        // "desliguei o efeito do álbum": a fita não fica presa no último frame.
                        animationController.cancel()
                        val idle = settings.idleColor
                        controller.setRgb(
                            idle.r, idle.g, idle.b,
                            settings.colorOrder, settings.bleColorOrder, settings.output
                        )
                    }
                }
                // Se uma condição de alerta já está ativa ao conectar, o alerta assume a fita.
                evaluateAlerts()
            }
        }
    }

    private suspend fun awaitAndroidAutoBootSettle() {
        // 1) Janela mínima de assentamento do boot — deixa o Android Auto subir e projetar primeiro.
        val bootRemaining = AMBIENT_BOOT_SETTLE_MS - (SystemClock.elapsedRealtime() - serviceStartElapsedMs)
        if (bootRemaining > 0) {
            Log.i(TAG, "ambient BLE connect deferred ${bootRemaining}ms (boot settle)")
            delay(bootRemaining)
        }
        // 2) Se o AA ainda está projetando/subindo, espera sair da janela crítica (com teto de segurança).
        val deferStart = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - deferStart < AMBIENT_AA_BUSY_DEFER_CAP_MS) {
            val aaBusy =
                runCatching {
                    br.com.redesurftank.havalshisuku.managers.DisplayAppLauncher
                        .hasAndroidAutoVisualTaskAnywhere()
                }.getOrDefault(false)
            if (!aaBusy) break
            Log.i(TAG, "ambient BLE connect deferred (Android Auto projecting)")
            delay(AMBIENT_AA_BUSY_POLL_MS)
        }
    }

    private fun disconnectLed() {
        serviceScope.launch {
            stopMusicEffects()
            animationController.cancel()
            controller.disconnect()
        }
    }

    private fun handleSetBrightness(intent: Intent?, settings: AmbientLightConfig) {
        val address = settings.deviceAddress
        if (address.isNullOrBlank()) {
            Log.w(TAG, "brightness ignored: no saved LED device")
            return
        }
        val percent = intent?.getIntExtra(EXTRA_BRIGHTNESS, settings.brightnessPercent) ?: settings.brightnessPercent
        serviceScope.launch {
            if (connectAndWait(address, settings.autoReconnect)) {
                controller.setBrightness(percent, settings.output)
            }
        }
    }

    private fun handleTestColor(intent: Intent?, settings: AmbientLightConfig) {
        val address = settings.deviceAddress
        if (address.isNullOrBlank()) {
            Log.w(TAG, "test ignored: no saved LED device")
            return
        }
        val r = intent?.getIntExtra(EXTRA_R, 0) ?: 0
        val g = intent?.getIntExtra(EXTRA_G, 0) ?: 0
        val b = intent?.getIntExtra(EXTRA_B, 0) ?: 0
        serviceScope.launch {
            stopMusicEffects()
            animationController.cancel()
            if (connectAndWait(address, settings.autoReconnect)) {
                controller.setRgb(r, g, b, settings.colorOrder, settings.bleColorOrder, settings.output)
            }
        }
    }

    private fun handleSendHex(intent: Intent?, settings: AmbientLightConfig) {
        val address = settings.deviceAddress
        val hex = intent?.getStringExtra(EXTRA_HEX)
        if (address.isNullOrBlank() || hex.isNullOrBlank()) {
            Log.w(TAG, "sendHex ignored: missing device or payload")
            return
        }
        serviceScope.launch {
            stopMusicEffects()
            animationController.cancel()
            if (connectAndWait(address, settings.autoReconnect)) {
                controller.sendHex(hex)
            }
        }
    }

    private suspend fun connectAndWait(address: String, autoReconnect: Boolean): Boolean {
        if (controller.isConnectedTo(address)) return true
        controller.connect(address, reconnect = autoReconnect)
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            controller.state
                .filter { it is AmbientLightConnectionState.Connected && it.address.equals(address, ignoreCase = true) }
                .first()
            true
        } ?: false
    }

    private fun updateDriveModeListener(settings: AmbientLightConfig) {
        if (settings.syncDriveMode && !driveModeListenerRegistered) {
            ServiceManager.getInstance().addDataChangedListener(driveModeListener)
            driveModeListenerRegistered = true
        } else if (!settings.syncDriveMode) {
            unregisterDriveModeListener()
        }
    }

    private fun unregisterDriveModeListener() {
        if (!driveModeListenerRegistered) return
        ServiceManager.getInstance().removeDataChangedListener(driveModeListener)
        driveModeListenerRegistered = false
    }

    private fun cacheCurrentDriveMode(settings: AmbientLightConfig) {
        if (!settings.syncDriveMode || settings.deviceAddress.isNullOrBlank()) return
        val raw = ServiceManager.getInstance().getData(CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE.value)
        currentDriveMode = AmbientLightDriveModeMapper.fromRaw(raw)
    }

    private fun handleDriveModeChanged(rawValue: String?) {
        val settings = AmbientLightSettings.load()
        if (!settings.enabled || !settings.syncDriveMode || settings.deviceAddress.isNullOrBlank()) {
            return
        }
        val mode = AmbientLightDriveModeMapper.fromRaw(rawValue)
        if (mode == currentDriveMode) return
        currentDriveMode = mode
        // Um alerta ativo tem prioridade sobre o modo de condução: só guarda o modo p/ restaurar depois.
        if (currentAlertCondition != null) return
        if (settings.musicAnimationEnabled) {
            Log.i(TAG, "drive mode changed: ${mode.name} music_effect_active")
            return
        }
        animationController.applyDriveMode(mode)
    }

    private fun stopAmbientLight() {
        unregisterDriveModeListener()
        unregisterAlertListener()
        stopMusicEffects()
        animationController.cancel()
        controller.disconnect()
        stopSelf()
    }

    private fun updateAlertListener(settings: AmbientLightConfig) {
        val speedKey = CarConstants.CAR_BASIC_VEHICLE_SPEED.value
        val hadSpeed = speedKey in enabledAlertKeys
        val enabledRules = settings.automationRules.filter { it.enabled }
        enabledAlertKeys =
            buildSet {
                enabledRules.forEach { addAll(conditionKeys(it.condition)) }
                // Regras "só parado" precisam reavaliar quando o carro anda/para.
                if (enabledRules.any { it.onlyWhenStopped }) {
                    add(speedKey)
                }
            }
        // Se a observação de velocidade ligou/desligou, o estado derivado do último evento
        // não é mais confiável — zera pra cair no fallback (cache) até o próximo evento.
        if (hadSpeed != (speedKey in enabledAlertKeys)) {
            lastVehicleStopped = null
        }
        if (enabledAlertKeys.isNotEmpty() && !alertListenerRegistered) {
            ServiceManager.getInstance().addDataChangedListener(alertListener)
            alertListenerRegistered = true
        } else if (enabledAlertKeys.isEmpty()) {
            unregisterAlertListener()
        }
    }

    private fun unregisterAlertListener() {
        if (!alertListenerRegistered) return
        ServiceManager.getInstance().removeDataChangedListener(alertListener)
        alertListenerRegistered = false
        // Limpa o alerta DENTRO do mutex: um evaluateAlerts em voo (suspenso até 7s no connect BLE)
        // não pode comitar um alerta DEPOIS deste teardown — a fita ficaria piscando pra sempre sem
        // listener pra desligar. Com o mutex + a re-validação pós-connect do evaluateAlerts, em
        // qualquer ordem o estado final fica limpo.
        serviceScope.launch {
            alertMutex.withLock {
                if (currentAlertCondition != null) {
                    currentAlertCondition = null
                    animationController.cancel()
                }
            }
        }
    }

    private fun conditionKeys(cond: AutomationCondition): List<String> =
        when (cond) {
            AutomationCondition.NO_SEATBELT -> listOf(CarConstants.CAR_BASIC_SEAT_BELT_WARNING.value)
            AutomationCondition.BLIND_SPOT ->
                listOf(
                    CarConstants.CAR_IPK_INFO_BSD_LCA_WARNING_REQLEFT.value,
                    CarConstants.CAR_IPK_INFO_BSD_LCA_WARNING_REQRIGHT.value
                )
            AutomationCondition.DOOR_OPEN -> listOf(CarConstants.CAR_BASIC_DOOR_STATUS.value)
            AutomationCondition.REVERSE_GEAR -> listOf(CarConstants.CAR_BASIC_GEAR_STATUS.value)
        }

    private fun isConditionActive(cond: AutomationCondition): Boolean {
        val sm = ServiceManager.getInstance()
        return when (cond) {
            AutomationCondition.NO_SEATBELT ->
                arrayHasActive(sm.getData(CarConstants.CAR_BASIC_SEAT_BELT_WARNING.value))
            AutomationCondition.BLIND_SPOT ->
                isWarnActive(sm.getData(CarConstants.CAR_IPK_INFO_BSD_LCA_WARNING_REQLEFT.value)) ||
                    isWarnActive(sm.getData(CarConstants.CAR_IPK_INFO_BSD_LCA_WARNING_REQRIGHT.value))
            AutomationCondition.DOOR_OPEN ->
                arrayHasActive(sm.getData(CarConstants.CAR_BASIC_DOOR_STATUS.value))
            AutomationCondition.REVERSE_GEAR ->
                sm.getData(CarConstants.CAR_BASIC_GEAR_STATUS.value)?.trim() == "4"
        }
    }

    private fun arrayHasActive(value: String?): Boolean =
        value?.replace("{", "")?.replace("}", "")?.split(",")?.any { it.trim() == "1" } == true

    private fun isWarnActive(value: String?): Boolean {
        val v = value?.trim() ?: return false
        return v.isNotEmpty() && v != "0" && !v.equals("false", true) && !v.equals("null", true)
    }

    // "Parado" = velocidade ~0 (tolerância pra ruído do sensor). Sem leitura confiável -> trata
    // como ANDANDO (fail-closed: melhor deixar de piscar do que piscar a fita dirigindo).
    private fun isVehicleStopped(raw: String?): Boolean {
        val speed = raw?.trim()?.toFloatOrNull() ?: return false
        return speed <= 0.5f
    }

    // FONTE FRESCA primeiro: o dataCache do ServiceManager só é atualizado DEPOIS do dispatch dos
    // listeners, então reler getData aqui devolvia a velocidade ANTIGA na própria avaliação que o
    // flip disparou (e o filtro de flip engole os eventos seguintes -> decisão errada ficava presa).
    // lastVehicleStopped é derivado do VALOR DO EVENTO no listener; cache é só fallback (boot/connect).
    private fun isVehicleStoppedNow(): Boolean =
        lastVehicleStopped
            ?: isVehicleStopped(
                ServiceManager.getInstance().getData(CarConstants.CAR_BASIC_VEHICLE_SPEED.value)
            )

    private suspend fun evaluateAlerts() {
        try {
            alertMutex.withLock {
                val settings = AmbientLightSettings.load()
                val address = settings.deviceAddress
                if (!settings.enabled || address.isNullOrBlank()) return@withLock
                val active =
                    settings.automationRules
                        .filter {
                            it.enabled &&
                                (!it.onlyWhenStopped || isVehicleStoppedNow()) &&
                                isConditionActive(it.condition)
                        }
                        .maxByOrNull { it.priority }
                if (active != null) {
                    if (currentAlertCondition != active.condition) {
                        // Conecta ANTES de comitar o estado: se a conexão falhar, currentAlertCondition
                        // fica intacto e o próximo evento re-tenta (não suprime o alerta permanentemente).
                        if (connectAndWait(address, settings.autoReconnect)) {
                            // RE-VALIDA depois da suspensão (o connect pode levar 7s): o usuário pode
                            // ter desligado a regra/módulo nesse meio-tempo pela UI (o unregister não
                            // espera este mutex pra remover o listener). Usa a regra FRESCA (cor/período
                            // também podem ter mudado).
                            val fresh = AmbientLightSettings.load()
                            val freshRule =
                                if (fresh.enabled && !fresh.deviceAddress.isNullOrBlank()) {
                                    fresh.automationRules.firstOrNull {
                                        it.condition == active.condition && it.enabled &&
                                            (!it.onlyWhenStopped || isVehicleStoppedNow()) &&
                                            isConditionActive(it.condition)
                                    }
                                } else {
                                    null
                                }
                            if (freshRule != null) {
                                stopMusicEffects()
                                animationController.startAlert(freshRule)
                                currentAlertCondition = freshRule.condition
                                Log.i(TAG, "alert ON: ${freshRule.condition.name} (${freshRule.effect.name})")
                            } else {
                                Log.i(TAG, "alert ABORTED: ${active.condition.name} (regra mudou durante o connect)")
                            }
                        }
                    }
                } else if (currentAlertCondition != null) {
                    Log.i(TAG, "alert OFF: ${currentAlertCondition?.name}")
                    currentAlertCondition = null
                    animationController.cancel()
                    restoreBaseAmbient(settings)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Nunca deixa o motor de alerta derrubar o app (ver handler do serviceScope).
            Log.e(TAG, "evaluateAlerts falhou", e)
        }
    }

    private fun restoreBaseAmbient(settings: AmbientLightConfig) {
        when {
            settings.musicAnimationEnabled -> startSelectedMusicEffect(settings)
            settings.syncDriveMode -> animationController.applyDriveMode(currentDriveMode)
            // Cor padrão de repouso escolhida pelo usuário (fixa) — prioridade sobre o fallback.
            settings.idleColorEnabled -> {
                val idle = settings.idleColor
                controller.setRgbAsync(
                    idle.r, idle.g, idle.b, settings.colorOrder, settings.bleColorOrder, settings.output
                )
            }
            else -> {
                val base = musicBaseColor()
                controller.setRgbAsync(
                    base.r, base.g, base.b, settings.colorOrder, settings.bleColorOrder, settings.output
                )
            }
        }
    }

    private fun startSelectedMusicEffect(settings: AmbientLightConfig) {
        when (settings.musicMode) {
            AmbientLightMusicMode.BASS -> {
                albumWaveMusicController.stop()
                musicVisualizerController.start()
            }
            AmbientLightMusicMode.ALBUM_WAVE -> {
                musicVisualizerController.stop()
                albumWaveMusicController.start()
            }
        }
    }

    private fun stopMusicEffects() {
        musicVisualizerController.stop()
        albumWaveMusicController.stop()
    }

    private fun settingsWithIntentDevice(intent: Intent?, settings: AmbientLightConfig): AmbientLightConfig {
        val address = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS)?.takeIf { it.isNotBlank() }
            ?: return settings
        val name = intent.getStringExtra(EXTRA_DEVICE_NAME)
        AmbientLightSettings.saveDevice(address, name)
        return AmbientLightSettings.load()
    }

    private fun musicBaseColor(): LedColor =
        if (currentDriveMode == DriveMode.UNKNOWN) {
            AmbientLightProtocol.SOFT_BLUE
        } else {
            AmbientLightDriveModeMapper.colorForMode(currentDriveMode)
        }

    companion object {
        private const val TAG = "AmbientLight"
        private const val CONNECT_TIMEOUT_MS = 7_000L
        // Adia a conexão BLE do LED no boot pra não competir com o Android Auto subindo.
        private const val AMBIENT_BOOT_SETTLE_MS = 40_000L
        private const val AMBIENT_AA_BUSY_DEFER_CAP_MS = 60_000L
        private const val AMBIENT_AA_BUSY_POLL_MS = 5_000L
        private const val ACTION_START = "br.com.redesurftank.havalshisuku.ambientlight.START"
        private const val ACTION_STOP = "br.com.redesurftank.havalshisuku.ambientlight.STOP"
        private const val ACTION_CONNECT = "br.com.redesurftank.havalshisuku.ambientlight.CONNECT"
        private const val ACTION_DISCONNECT = "br.com.redesurftank.havalshisuku.ambientlight.DISCONNECT"
        private const val ACTION_SET_BRIGHTNESS = "br.com.redesurftank.havalshisuku.ambientlight.SET_BRIGHTNESS"
        private const val ACTION_TEST_COLOR = "br.com.redesurftank.havalshisuku.ambientlight.TEST_COLOR"
        private const val ACTION_SEND_HEX = "br.com.redesurftank.havalshisuku.ambientlight.SEND_HEX"
        private const val ACTION_APPLY_DRIVE_MODE = "br.com.redesurftank.havalshisuku.ambientlight.APPLY_DRIVE_MODE"
        private const val EXTRA_R = "extra_r"
        private const val EXTRA_G = "extra_g"
        private const val EXTRA_B = "extra_b"
        private const val EXTRA_BRIGHTNESS = "extra_brightness"
        private const val EXTRA_HEX = "extra_hex"
        private const val EXTRA_DRIVE_MODE = "extra_drive_mode"
        private const val EXTRA_DEVICE_ADDRESS = "extra_device_address"
        private const val EXTRA_DEVICE_NAME = "extra_device_name"

        @JvmStatic
        fun createStartIntent(context: Context): Intent =
            Intent(context, AmbientLightService::class.java).setAction(ACTION_START)

        @JvmStatic
        fun createStopIntent(context: Context): Intent =
            Intent(context, AmbientLightService::class.java).setAction(ACTION_STOP)

        @JvmStatic
        fun createConnectIntent(
            context: Context,
            address: String? = null,
            name: String? = null
        ): Intent =
            Intent(context, AmbientLightService::class.java)
                .setAction(ACTION_CONNECT)
                .apply {
                    if (!address.isNullOrBlank()) {
                        putExtra(EXTRA_DEVICE_ADDRESS, address)
                        putExtra(EXTRA_DEVICE_NAME, name)
                    }
                }

        @JvmStatic
        fun createDisconnectIntent(context: Context): Intent =
            Intent(context, AmbientLightService::class.java).setAction(ACTION_DISCONNECT)

        @JvmStatic
        fun createBrightnessIntent(context: Context, percent: Int): Intent =
            Intent(context, AmbientLightService::class.java)
                .setAction(ACTION_SET_BRIGHTNESS)
                .putExtra(EXTRA_BRIGHTNESS, percent)

        @JvmStatic
        fun createTestColorIntent(context: Context, color: LedColor): Intent =
            Intent(context, AmbientLightService::class.java)
                .setAction(ACTION_TEST_COLOR)
                .putExtra(EXTRA_R, color.r)
                .putExtra(EXTRA_G, color.g)
                .putExtra(EXTRA_B, color.b)

        @JvmStatic
        fun createSendHexIntent(context: Context, hex: String): Intent =
            Intent(context, AmbientLightService::class.java)
                .setAction(ACTION_SEND_HEX)
                .putExtra(EXTRA_HEX, hex)

        @JvmStatic
        fun startIfEnabled(context: Context) {
            if (AmbientLightSettings.isEnabled()) {
                context.startService(createStartIntent(context))
            }
        }

        @JvmStatic
        fun stop(context: Context) {
            context.startService(createStopIntent(context))
        }
    }
}
