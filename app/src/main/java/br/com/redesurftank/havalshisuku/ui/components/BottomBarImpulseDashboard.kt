package br.com.redesurftank.havalshisuku.ui.components

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.PopupProperties
import br.com.redesurftank.havalshisuku.R
import br.com.redesurftank.havalshisuku.managers.*
import br.com.redesurftank.havalshisuku.models.*
import br.com.redesurftank.havalshisuku.services.AlbumBackgroundService
import br.com.redesurftank.havalshisuku.services.BottomBarService
import br.com.redesurftank.havalshisuku.ui.theme.Michroma
import br.com.redesurftank.havalshisuku.utils.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.core.content.edit
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger

// NOTA (perf): NAO adicione aqui campo que nenhum painel desenhe, e principalmente nao adicione
// sinal RAPIDO (velocidade, corrente, RPM). Este snapshot e lido no escopo de
// ExpandedImpulseDashboard, entao toda escrita de qualquer campo recompoe o dashboard inteiro.
// Ja tivemos 7 campos (speed/gear/odometer/avgFuel/avgEnergy/batteryVoltage/batteryCurrent)
// alimentando dois paineis MORTOS -> a arvore recompunha a cada amostra de velocidade de graca.
private data class DashboardVehicleSnapshot(
        val driveMode: String,
        val powerModel: String,
        val powerReserve: String,
        val socTarget: String,
        val energyRecovery: String,
        val onePedalEnabled: String,
        val steeringMode: String,
        val driverTemp: String,
        val passTemp: String,
        val fanSpeed: String,
        val hvacPower: String,
        val blowerMode: String,
        val acSync: String,
        val acAuto: String,
        val acRecirc: String,
        val driverSeatVentilation: String,
        val passengerSeatVentilation: String,
        val seatVentilationMaxLevel: String,
        val insideTemp: String,
        val outsideTemp: String,
        val hotRouterMode: String,
        val hotRouterWifiName: String?,
        val batteryPercent: String,
        val fuelPercent: String,
        val batteryRange: String,
        val fuelRange: String,
        // Bateria 12V (AUXILIAR — nao confundir com a de tracao acima). Ambas sao observadas via
        // DEFAULT_KEYS, entao chegam por push e nao congelam.
        val batt12vPct: String,
        val batt12vVoltage: String,
        val volume: String,
        val readyState: String
)

@Composable
private fun rememberDashboardVehicleSnapshot(
        serviceManager: ServiceManager
): DashboardVehicleSnapshot {
        var driveMode by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE.getValue())
                                ?: "0"
                )
        }
        var powerModel by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_EV_SETTING_POWER_MODEL_CONFIG.getValue()
                        )
                                ?: "0"
                )
        }
        var powerReserve by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_EV_SETTING_POWER_RESERVE_CONFIG.getValue()
                        )
                                ?: "1"
                )
        }
        var socTarget by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_EV_SETTING_CHARGE_SOC_TARGET_CONFIG.getValue()
                        )
                                ?: "50"
                )
        }
        var energyRecovery by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_EV_SETTING_ENERGY_RECOVERY_LEVEL.getValue()
                        )
                                ?: "0"
                )
        }
        var onePedalEnabled by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_CONFIGURE_PEDAL_CONTROL_ENABLE.getValue()
                        )
                                ?: "0"
                )
        }
        var steeringMode by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_DRIVE_SETTING_STEERING_WHEEL_ASSIST_MODE
                                        .getValue()
                        )
                                ?: "0"
                )
        }
        var driverTemp by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue())
                                ?: "--"
                )
        }
        var passTemp by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue())
                                ?: "--"
                )
        }
        var fanSpeed by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_HVAC_FAN_SPEED.getValue()) ?: "0"
                )
        }
        var hvacPower by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_HVAC_POWER_MODE.getValue()) ?: "1"
                )
        }
        var blowerMode by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_HVAC_BLOWER_MODE.getValue()) ?: "0"
                )
        }
        var acSync by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_HVAC_SYNC_ENABLE.getValue()) ?: "0"
                )
        }
        var acAuto by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_HVAC_AUTO_ENABLE.getValue()) ?: "0"
                )
        }
        var acRecirc by remember {
                mutableStateOf(
                        if ((serviceManager.getData(CarConstants.CAR_HVAC_CYCLE_MODE.getValue())
                                                        ?: "0") == "0"
                        )
                                "1"
                        else "0"
                )
        }
        var driverSeatVentilation by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL
                                        .getValue()
                        )
                                ?: "0"
                )
        }
        var passengerSeatVentilation by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_COMFORT_SETTING_PASSENGER_SEAT_VENTILATION_LEVEL
                                        .getValue()
                        )
                                ?: "0"
                )
        }
        var seatVentilationMaxLevel by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_COMFORT_SETTING_SEAT_VENTILATION_MAX_LEVEL
                                        .getValue()
                        )
                                ?: "3"
                )
        }
        var insideTemp by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_BASIC_INSIDE_TEMP.getValue())
                                ?: "--"
                )
        }
        var outsideTemp by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_BASIC_OUTSIDE_TEMP.getValue())
                                ?: "--"
                )
        }
        // Status do HotRouter (roteamento Starlink) — não é um sinal CAN, então é lido via shell
        // (Shizuku) num poll periódico, fora da main thread. MODE_OFF = feature desligada -> chip some.
        var hotRouterMode by remember { mutableStateOf(HotRouterManager.MODE_OFF) }
        var hotRouterWifiName by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
                while (true) {
                        val mgr = HotRouterManager.getInstance()
                        val mode = withContext(Dispatchers.IO) { mgr.readStatusBlocking().mode }
                        hotRouterMode = mode
                        // SSID só é relevante (e só custa shell) quando está roteando pela WLAN.
                        hotRouterWifiName =
                                if (mode == HotRouterManager.MODE_WLAN) {
                                        withContext(Dispatchers.IO) {
                                                mgr.readRoutedWifiNameBlocking()
                                        }
                                } else null
                        delay(4000)
                }
        }
        var batteryPercent by remember {
                mutableStateOf(readDashboardBatteryPercent(serviceManager))
        }
        var fuelPercent by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_BASIC_REMAIN_FUEL_PERCENTAGE.getValue()
                        )
                                ?: "--"
                )
        }
        var batteryRange by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_EV_INFO_ELECTRIC_MODE_REMAIN_ODOMETER.getValue()
                        )
                                ?: "--"
                )
        }
        var fuelRange by remember {
                mutableStateOf(readDashboardFuelRange(serviceManager))
        }
        var batt12vPct by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_EV_INFO_BATTERY_POWER_PERCENTAGE.getValue()
                        )
                                ?: "--"
                )
        }
        var batt12vVoltage by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.CAR_BASIC_BATTERY_VOLTAGE.getValue()
                        )
                                ?: "--"
                )
        }
        var volume by remember {
                mutableStateOf(
                        serviceManager.getData(
                                CarConstants.SYS_SETTINGS_AUDIO_MEDIA_VOLUME.getValue()
                        )
                                ?: "0"
                )
        }
        var readyState by remember {
                mutableStateOf(
                        serviceManager.getData(CarConstants.CAR_BASIC_DRIVING_READY_STATE.getValue())
                                ?: "--"
                )
        }

        DisposableEffect(Unit) {
                val listener =
                        object : br.com.redesurftank.havalshisuku.listeners.IDataChanged {
                                override fun onDataChanged(key: String, value: String?) {
                                        if (value == null) return
                                        when (key) {
                                                // VEHICLE_SPEED e GEAR_STATUS NAO sao observados de
                                                // proposito: nada no dashboard os desenha, e a
                                                // velocidade chega em ritmo de direcao — observar
                                                // aqui recompunha a arvore inteira de graca.
                                                CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE
                                                        .getValue() -> driveMode = value
                                                CarConstants.CAR_EV_SETTING_POWER_MODEL_CONFIG
                                                        .getValue() -> powerModel = value
                                                CarConstants.CAR_EV_SETTING_POWER_RESERVE_CONFIG
                                                        .getValue() -> powerReserve = value
                                                CarConstants.CAR_EV_SETTING_CHARGE_SOC_TARGET_CONFIG
                                                        .getValue() -> socTarget = value
                                                CarConstants.CAR_EV_SETTING_ENERGY_RECOVERY_LEVEL
                                                        .getValue() -> energyRecovery = value
                                                CarConstants.CAR_CONFIGURE_PEDAL_CONTROL_ENABLE
                                                        .getValue() -> onePedalEnabled = value
                                                CarConstants
                                                        .CAR_DRIVE_SETTING_STEERING_WHEEL_ASSIST_MODE
                                                        .getValue() -> steeringMode = value
                                                CarConstants.CAR_HVAC_DRIVER_TEMPERATURE
                                                        .getValue() -> driverTemp = value
                                                CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue() ->
                                                        passTemp = value
                                                CarConstants.CAR_HVAC_FAN_SPEED.getValue() ->
                                                        fanSpeed = value
                                                CarConstants.CAR_HVAC_POWER_MODE.getValue() ->
                                                        hvacPower = value
                                                CarConstants.CAR_HVAC_BLOWER_MODE.getValue() ->
                                                        blowerMode = value
                                                CarConstants.CAR_HVAC_SYNC_ENABLE.getValue() ->
                                                        acSync = value
                                                CarConstants.CAR_HVAC_AUTO_ENABLE.getValue() ->
                                                        acAuto = value
                                                CarConstants.CAR_HVAC_CYCLE_MODE.getValue() ->
                                                        acRecirc = if (value == "0") "1" else "0"
                                                CarConstants
                                                        .CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL
                                                        .getValue() ->
                                                        driverSeatVentilation = value
                                                CarConstants
                                                        .CAR_COMFORT_SETTING_PASSENGER_SEAT_VENTILATION_LEVEL
                                                        .getValue() ->
                                                        passengerSeatVentilation = value
                                                CarConstants
                                                        .CAR_COMFORT_SETTING_SEAT_VENTILATION_MAX_LEVEL
                                                        .getValue() ->
                                                        seatVentilationMaxLevel = value
                                                CarConstants.CAR_BASIC_INSIDE_TEMP.getValue() ->
                                                        insideTemp = value
                                                CarConstants.CAR_BASIC_OUTSIDE_TEMP.getValue() ->
                                                        outsideTemp = value
                                                CarConstants
                                                        .CAR_EV_INFO_CUR_BATTERY_POWER_PERCENTAGE
                                                        .getValue(),
                                                CarConstants
                                                        .CAR_EV_INFO_CAR_EV_INFO_SOC_OF_BATTERY
                                                        .getValue(),
                                                CarConstants.CAR_EV_INFO_BATTERY_POWER_PERCENTAGE
                                                        .getValue() -> {
                                                        batteryPercent =
                                                                readDashboardBatteryPercent(
                                                                        serviceManager,
                                                                        key,
                                                                        value
                                                                )
                                                        // `car.ev_info.battery_charge_percentage` e a
                                                        // % da bateria 12V (auxiliar) — confirmado
                                                        // pelo bridge do proprio usuario (campo
                                                        // batt_12v_pct). Tratada AQUI dentro porque o
                                                        // `when` acima ja captura esta chave junto das
                                                        // de tracao (um branch separado seria
                                                        // inalcancavel). Ver a NOTA no card 12V sobre
                                                        // esta chave ainda servir de fallback da
                                                        // bateria de TRACAO — provavel bug preexistente.
                                                        if (
                                                                key ==
                                                                        CarConstants
                                                                                .CAR_EV_INFO_BATTERY_POWER_PERCENTAGE
                                                                                .getValue()
                                                        ) {
                                                                batt12vPct = value
                                                        }
                                                }
                                                CarConstants.CAR_BASIC_REMAIN_FUEL_PERCENTAGE
                                                        .getValue() -> fuelPercent = value
                                                CarConstants.CAR_BASIC_BATTERY_VOLTAGE.getValue() ->
                                                        batt12vVoltage = value
                                                CarConstants
                                                        .CAR_EV_INFO_ELECTRIC_MODE_REMAIN_ODOMETER
                                                        .getValue() -> batteryRange = value
                                                CarConstants
                                                        .CAR_EV_INFO_FUEL_MODE_REMAIN_ODOMETER
                                                        .getValue(),
                                                CarConstants.CAR_BASIC_REMAIN_ODOMETER.getValue() ->
                                                        fuelRange =
                                                                readDashboardFuelRange(
                                                                        serviceManager,
                                                                        key,
                                                                        value
                                                                )
                                                // TOTAL_ODOMETER / AVG_FUEL / AVG_ENERGY /
                                                // BATTERY_VOLTAGE / BATTERY_CURRENT idem: sem
                                                // consumidor na UI (voltagem/corrente mudam rapido).
                                                CarConstants.SYS_SETTINGS_AUDIO_MEDIA_VOLUME
                                                        .getValue() -> volume = value
                                                CarConstants.CAR_BASIC_DRIVING_READY_STATE
                                                        .getValue() -> readyState = value
                                        }
                                }
                        }
                serviceManager.addDataChangedListener(listener)
                onDispose { serviceManager.removeDataChangedListener(listener) }
        }

        return DashboardVehicleSnapshot(
                driveMode = driveMode,
                powerModel = powerModel,
                powerReserve = powerReserve,
                socTarget = socTarget,
                energyRecovery = energyRecovery,
                onePedalEnabled = onePedalEnabled,
                steeringMode = steeringMode,
                driverTemp = driverTemp,
                passTemp = passTemp,
                fanSpeed = fanSpeed,
                hvacPower = hvacPower,
                blowerMode = blowerMode,
                acSync = acSync,
                acAuto = acAuto,
                acRecirc = acRecirc,
                driverSeatVentilation = driverSeatVentilation,
                passengerSeatVentilation = passengerSeatVentilation,
                seatVentilationMaxLevel = seatVentilationMaxLevel,
                insideTemp = insideTemp,
                outsideTemp = outsideTemp,
                hotRouterMode = hotRouterMode,
                hotRouterWifiName = hotRouterWifiName,
                batteryPercent = batteryPercent,
                fuelPercent = fuelPercent,
                batteryRange = batteryRange,
                fuelRange = fuelRange,
                batt12vPct = batt12vPct,
                batt12vVoltage = batt12vVoltage,
                volume = volume,
                readyState = readyState
        )
}

@Composable
fun ImpulseDashboardFullscreenContent() {
        ExpandedImpulseDashboard()
}

@Composable
internal fun ExpandedImpulseDashboard() {
        val serviceManager = ServiceManager.getInstance()
        val context = LocalContext.current
        val snapshot = rememberDashboardVehicleSnapshot(serviceManager)
        val entryProgress = remember { Animatable(0f) }
        var currentTime by remember { mutableStateOf(formatDashboardClock()) }
        val prefs =
                remember {
                        br.com.redesurftank.App.getDeviceProtectedContext()
                                .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
                }
        var dashboardCardOrder by remember {
                mutableStateOf(
                        normalizeDashboardCardOrder(
                                prefs.getString(SharedPreferencesKeys.DASHBOARD_CARD_ORDER.key, null)
                        )
                )
        }
        var layoutEditMode by remember { mutableStateOf(false) }
        var shortcutMenuExpanded by remember { mutableStateOf(false) }
        val albumBackground = rememberDashboardAlbumBackgroundState()
        var dashboardShortcutButton1Action by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION.key,
                                SteeringWheelCustomActionType.DEFAULT.key
                        )
                                ?: SteeringWheelCustomActionType.DEFAULT.key
                )
        }
        var dashboardShortcutButton2Action by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION.key,
                                SteeringWheelCustomActionType.DEFAULT.key
                        )
                                ?: SteeringWheelCustomActionType.DEFAULT.key
                )
        }

        fun updateDashboardCardOrder(nextOrder: List<DashboardCardId>) {
                val normalized = normalizeDashboardCardOrder(dashboardCardOrderToStorage(nextOrder))
                dashboardCardOrder = normalized
                prefs.edit {
                        putString(
                                SharedPreferencesKeys.DASHBOARD_CARD_ORDER.key,
                                dashboardCardOrderToStorage(normalized)
                        )
                }
        }

        fun setDashboardShortcutButton(button: Int) {
                val actions =
                        resolveImpulseDashboardShortcutActions(
                                currentButton1Action = dashboardShortcutButton1Action,
                                currentButton2Action = dashboardShortcutButton2Action,
                                selectedButton = button
                        )
                dashboardShortcutButton1Action = actions.button1Action
                dashboardShortcutButton2Action = actions.button2Action
                prefs.edit(commit = true) {
                        putBoolean(
                                SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS.key,
                                true
                        )
                        putString(
                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION.key,
                                actions.button1Action
                        )
                        putString(
                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION.key,
                                actions.button2Action
                        )
                }
                shortcutMenuExpanded = false
                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
        }

        LaunchedEffect(Unit) {
                entryProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                )
        }

        LaunchedEffect(Unit) {
                while (true) {
                        currentTime = formatDashboardClock()
                        delay(30000)
                }
        }

        val activeProjectionPackage =
                BottomBarState.activeClusterProjectionPackage.takeIf { it.isNotEmpty() }
        val effectivePackage =
                activeProjectionPackage
                        ?: BottomBarState.selectedPackage.takeIf { it.isNotEmpty() }
                        ?: getBottomBarAppConfigs().firstOrNull()?.packageName
        val effectiveConfig =
                remember(effectivePackage) {
                        getBottomBarAppConfigs().find { it.packageName == effectivePackage }
                }
        val appInfo =
                remember(effectivePackage, effectiveConfig?.customName) {
                        effectivePackage?.let {
                                DisplayAppLauncher.resolveAppInfo(
                                        context,
                                        it,
                                        effectiveConfig?.customName
                                )
                        }
                }

        fun collapseDashboard() {
                BottomBarState.isDashboardExpanded = false
                BottomBarState.isVisible = true
                BottomBarState.isMenuExpanded = false
                BottomBarState.isSettingsMenuExpanded = false
                BottomBarState.isOverrideMenuExpanded = false
                BottomBarState.activeSliderType = null
        }

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(
                                        Brush.linearGradient(
                                                colors =
                                                        listOf(
                                                                Color(0xFF05070A),
                                                                Color(0xFF0D1318),
                                                                Color(0xFF12120F)
                                                        )
                                        )
                                )
                                .pointerInput(Unit) {
                                        awaitEachGesture {
                                                awaitFirstDown(requireUnconsumed = false)
                                                var totalDragY = 0f
                                                var totalDragX = 0f
                                                do {
                                                        val event = awaitPointerEvent()
                                                        event.changes.forEach { change ->
                                                                val deltaX =
                                                                        change.position.x -
                                                                                change.previousPosition.x
                                                                val deltaY =
                                                                        change.position.y -
                                                                                change.previousPosition.y
                                                                totalDragX += deltaX
                                                                if (deltaY > 0f) {
                                                                        totalDragY += deltaY
                                                                        if (totalDragY >
                                                                                        DASHBOARD_COLLAPSE_CONSUME_DRAG_PX
                                                                                && isDashboardCollapseDragMostlyVertical(
                                                                                        totalDragY,
                                                                                        totalDragX
                                                                                )
                                                                        ) {
                                                                                change.consume()
                                                                        }
                                                                }
                                                        }
                                                } while (event.changes.any { it.pressed })

                                                if (shouldCollapseDashboardAfterDrag(totalDragY, totalDragX)) {
                                                        collapseDashboard()
                                                }
                                        }
                                }
        ) {
                DashboardAlbumDynamicBackground(
                        primaryState = albumBackground.primary,
                        secondaryState = albumBackground.secondary,
                        accentState = albumBackground.accent,
                        darkState = albumBackground.dark,
                        hasArtwork = albumBackground.hasArtwork,
                        modifier = Modifier.matchParentSize(),
                        cornerRadius = 0.dp,
                        artworkAlpha = 0.9f,
                        fallbackAlpha = 0.42f,
                        artworkScrimAlpha = 0.5f,
                        fallbackScrimAlpha = 0.66f
                )
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .graphicsLayer {
                                                alpha = 0.82f + (0.18f * entryProgress.value)
                                                translationY = (1f - entryProgress.value) * 180f
                                        }
                                        .padding(
                                                start = 18.dp,
                                                top = 8.dp,
                                                end = 18.dp,
                                                bottom = 18.dp
                                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        DashboardTopDragHandle(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        DashboardHeader(
                                time = currentTime,
                                snapshot = snapshot,
                                activeProjectionPackage = activeProjectionPackage,
                                layoutEditMode = layoutEditMode,
                                shortcutSelectedButton =
                                        resolveImpulseDashboardShortcutButton(
                                                dashboardShortcutButton1Action,
                                                dashboardShortcutButton2Action
                                        ),
                                shortcutMenuExpanded = shortcutMenuExpanded,
                                onToggleLayoutEditMode = { layoutEditMode = !layoutEditMode },
                                onShortcutExpandedChange = { shortcutMenuExpanded = it },
                                onShortcutButtonSelected = { setDashboardShortcutButton(it) },
                                onShowNativeMenu = { collapseDashboard() }
                        )
                        Row(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                dashboardCardOrder.forEachIndexed { index, cardId ->
                                        DashboardCardSlot(
                                                cardId = cardId,
                                                order = dashboardCardOrder,
                                                layoutEditMode = layoutEditMode,
                                                snapshot = snapshot,
                                                serviceManager = serviceManager,
                                                appLabel = appInfo?.label,
                                                appIcon = appInfo?.icon,
                                                activeProjectionPackage = activeProjectionPackage,
                                                albumBackground = albumBackground,
                                                modifier =
                                                        Modifier.weight(dashboardSlotWeight(index))
                                                                .fillMaxHeight(),
                                                onMoveCard = { movedCardId, direction ->
                                                        updateDashboardCardOrder(
                                                                moveDashboardCard(
                                                                        dashboardCardOrder,
                                                                        movedCardId,
                                                                        direction
                                                                )
                                                        )
                                                }
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun DashboardHeader(
        time: String,
        snapshot: DashboardVehicleSnapshot,
        activeProjectionPackage: String?,
        layoutEditMode: Boolean,
        shortcutSelectedButton: Int?,
        shortcutMenuExpanded: Boolean,
        onToggleLayoutEditMode: () -> Unit,
        onShortcutExpandedChange: (Boolean) -> Unit,
        onShortcutButtonSelected: (Int) -> Unit,
        onShowNativeMenu: () -> Unit
) {
        Row(
                modifier = Modifier.fillMaxWidth().height(62.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color(0xFF66E3FF),
                                modifier = Modifier.size(34.dp)
                        )
                        Column {
                                Text(
                                        text = "IMPULSE DRIVE",
                                        color = Color.White,
                                        fontFamily = DashboardReadableFont,
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.Bold
                                )
                                Text(
                                        text = projectionLabel(activeProjectionPackage),
                                        color = Color.White.copy(alpha = 0.62f),
                                        fontSize = 13.sp,
                                        fontFamily = DashboardReadableFont
                                )
                        }
                }
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        // Card ÚNICO de conectividade: roteamento do hotspot (Starlink/WiFi/4G) + estado
                        // do 4G, numa frase só. A lógica de texto/cor/ícone vem do ConnectivityStatusManager
                        // — a MESMA fonte que o EcoTrip puxa via ContentProvider (nada é recapturado lá).
                        val mcState = rememberMobileControlState()
                        val conn = br.com.redesurftank.havalshisuku.managers.ConnectivityStatusManager
                                .buildStatus(
                                        snapshot.hotRouterMode,
                                        snapshot.hotRouterWifiName ?: mcState.headUnitWifiName,
                                        mcState.controlEnabled,
                                        mcState.blockReason,
                                        mcState.hotspotActive,
                                        mcState.headUnitOnWifi
                                )
                        conn.displayText?.let { connText ->
                                DashboardStatusChip(
                                        icon =
                                                when (conn.displayIcon) {
                                                        "satellite" -> Icons.Default.SatelliteAlt
                                                        "wifi" -> Icons.Default.Wifi
                                                        "cell_off" -> Icons.Default.SignalCellularOff
                                                        "loader" -> Icons.Default.Sync
                                                        "alert" -> Icons.Default.WarningAmber
                                                        else -> Icons.Default.SignalCellularAlt
                                                },
                                        text = connText,
                                        accent =
                                                when (conn.displayLevel) {
                                                        "good" -> Color(0xFF78E08F)
                                                        "warn" -> Color(0xFFF0A93A)
                                                        "bad" -> Color(0xFFE24B4A)
                                                        else -> null
                                                }
                                )
                        }
                        // Uso de recursos do head unit (CPU / RAM total).
                        // GPU nao entra: o hypervisor nao expoe contador de GPU nesta VM convidada
                        // (ver HeadUnitResourceSampler).
                        rememberHeadUnitResourceText()?.let { resourceText ->
                                DashboardStatusChip(
                                        icon = Icons.Default.Memory,
                                        text = resourceText
                                )
                        }
                        DashboardStatusChip(
                                icon = Icons.Default.DeviceThermostat,
                                text = "Cabine ${formatTemperature(snapshot.insideTemp)}"
                        )
                        DashboardStatusChip(
                                icon = Icons.Default.WbSunny,
                                text = "Externa ${formatTemperature(snapshot.outsideTemp)}"
                        )
                        DashboardStatusChip(icon = Icons.Default.AccessTime, text = time)
                        DashboardHeaderControlButton(
                                icon = Icons.Default.Tune,
                                text = if (layoutEditMode) "Pronto" else "Layout",
                                active = layoutEditMode,
                                onClick = onToggleLayoutEditMode
                        )
                        DashboardShortcutSelectorButton(
                                selectedButton = shortcutSelectedButton,
                                expanded = shortcutMenuExpanded,
                                onExpandedChange = onShortcutExpandedChange,
                                onSelectButton = onShortcutButtonSelected,
                                modifier = Modifier.zIndex(6f)
                        )
                        DashboardNativeMenuButton(onClick = onShowNativeMenu)
                }
        }
}

private const val HEADER_RESOURCE_SAMPLE_INTERVAL_MS = 1_500L

/**
 * Texto do chip de recursos do head unit, amostrado FORA da main thread.
 *
 * Vive so enquanto o header esta em composicao (= barra estendida aberta), entao nao adiciona
 * nenhum custo de repouso ao app — foi exatamente esse tipo de poll sempre-ligado que o mapeamento
 * de performance mandou eliminar. Le apenas /proc (sem shell/processo).
 */
@Composable
private fun rememberHeadUnitResourceText(): String? {
        var text by remember { mutableStateOf<String?>(null) }
        DisposableEffect(Unit) {
                // Descarta a base do calculo de CPU ao sair: senao, ao reabrir o dashboard a primeira
                // leitura seria a media desde a ultima vez (minutos atras) em vez do instante atual.
                onDispose { HeadUnitResourceSampler.reset() }
        }
        LaunchedEffect(Unit) {
                // Primeira amostra so ESQUENTA a base (o % de CPU e uma variacao entre duas leituras);
                // por isso um intervalo curto antes da primeira exibicao, pra ja aparecer com CPU.
                withContext(Dispatchers.IO) { HeadUnitResourceSampler.sample() }
                delay(400L)
                while (true) {
                        val snapshot = withContext(Dispatchers.IO) { HeadUnitResourceSampler.sample() }
                        text =
                                buildString {
                                                snapshot.cpuPct?.let { append("CPU ").append(it).append('%') }
                                                snapshot.ramPct?.let {
                                                        if (isNotEmpty()) append("  ")
                                                        append("RAM ").append(it).append('%')
                                                }
                                        }
                                        .takeIf { it.isNotEmpty() }
                        delay(HEADER_RESOURCE_SAMPLE_INTERVAL_MS)
                }
        }
        return text
}

/**
 * Estado do controle de dados móveis (master + motivo do bloqueio) pro card unificado de conectividade.
 * Vive só enquanto o header está composto (barra aberta) — sem poll de repouso. Fora da main thread.
 * Devolve controle/motivo/hotspot + se a TELA está no WiFi (e o SSID); o ConnectivityStatusManager combina com o HotRouter.
 */
private data class MobileControlSnapshot(
        val controlEnabled: Boolean,
        val blockReason: String?,
        val hotspotActive: Boolean,
        val headUnitOnWifi: Boolean,
        val headUnitWifiName: String?
)

@Composable
private fun rememberMobileControlState(): MobileControlSnapshot {
        var state by remember { mutableStateOf(MobileControlSnapshot(false, null, false, false, null)) }
        LaunchedEffect(Unit) {
                while (true) {
                        state = withContext(Dispatchers.IO) {
                                val mdm = br.com.redesurftank.havalshisuku.managers.MobileDataManager
                                val ctx = br.com.redesurftank.App.getContext()
                                val onWifi = mdm.isWifiConnected(ctx)
                                // SSID da tela só quando ela está no WiFi (evita shell à toa no 4G).
                                val wifiName = if (onWifi) {
                                        try {
                                                br.com.redesurftank.havalshisuku.managers.HotRouterManager
                                                        .getInstance().readRoutedWifiNameBlocking()
                                        } catch (t: Throwable) { null }
                                } else null
                                MobileControlSnapshot(
                                        mdm.isControlEnabled(),
                                        mdm.blockReason(ctx),
                                        br.com.redesurftank.havalshisuku.managers.ConnectivityStatusManager.isHotspotActive(ctx),
                                        onWifi,
                                        wifiName
                                )
                        }
                        delay(HEADER_RESOURCE_SAMPLE_INTERVAL_MS)
                }
        }
        return state
}

@Composable
private fun DashboardHeaderControlButton(
        icon: ImageVector,
        text: String,
        active: Boolean,
        onClick: () -> Unit
) {
        val accent = if (active) Color(0xFF78E08F) else Color(0xFF66E3FF)
        Surface(
                onClick = onClick,
                modifier = Modifier.height(44.dp),
                color = accent.copy(alpha = if (active) 0.18f else 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = if (active) 0.48f else 0.30f))
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Icon(
                                icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                        )
                        Text(
                                text = text,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = DashboardReadableFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                        )
                }
        }
}

@Composable
private fun DashboardNativeMenuButton(onClick: () -> Unit) {
        Surface(
                onClick = onClick,
                modifier = Modifier.height(44.dp),
                color = Color(0xFF66E3FF).copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF66E3FF).copy(alpha = 0.34f))
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF66E3FF),
                                modifier = Modifier.size(20.dp)
                        )
                        Text(
                                text = "Menu nativo",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = DashboardReadableFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                        )
                }
        }
}

@Composable
private fun DashboardShortcutSelectorButton(
        selectedButton: Int?,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        onSelectButton: (Int) -> Unit,
        modifier: Modifier = Modifier
) {
        val accent = if (selectedButton == null) Color(0xFFFF7A7A) else Color(0xFF66E3FF)
        Box(modifier = modifier) {
                Surface(
                        onClick = { onExpandedChange(!expanded) },
                        modifier =
                                if (selectedButton == null) Modifier.height(44.dp)
                                else Modifier.size(44.dp),
                        color = accent.copy(alpha = if (selectedButton == null) 0.16f else 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border =
                                BorderStroke(
                                        1.dp,
                                        accent.copy(alpha = if (selectedButton == null) 0.42f else 0.34f)
                                )
                ) {
                        Row(
                                modifier =
                                        if (selectedButton == null) {
                                                Modifier.padding(horizontal = 14.dp)
                                        } else {
                                                Modifier.fillMaxSize()
                                        },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                        ) {
                                if (selectedButton == null) {
                                        Text(
                                                text = "Definir atalho",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontFamily = DashboardReadableFont,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                        )
                                } else {
                                        Icon(
                                                dashboardShortcutButtonIcon(selectedButton),
                                                contentDescription = "Atalho no botão $selectedButton",
                                                tint = accent,
                                                modifier = Modifier.size(21.dp)
                                        )
                                }
                        }
                }
                DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) },
                        modifier = Modifier.background(Color(0xFF121A22)),
                        properties = PopupProperties(focusable = false)
                ) {
                        DashboardShortcutDropdownItem(
                                text = "Botão 1",
                                icon = Icons.Default.Add,
                                selected = selectedButton == 1,
                                onClick = { onSelectButton(1) }
                        )
                        DashboardShortcutDropdownItem(
                                text = "Botão 2",
                                icon = Icons.Default.Star,
                                selected = selectedButton == 2,
                                onClick = { onSelectButton(2) }
                        )
                }
        }
}

@Composable
private fun DashboardShortcutDropdownItem(
        text: String,
        icon: ImageVector,
        selected: Boolean,
        onClick: () -> Unit
) {
        DropdownMenuItem(
                text = {
                        Text(
                                text = text,
                                color = Color.White,
                                fontFamily = DashboardReadableFont,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                },
                leadingIcon = {
                        Icon(
                                icon,
                                contentDescription = null,
                                tint = if (selected) Color(0xFF66E3FF) else Color.White.copy(alpha = 0.76f)
                        )
                },
                onClick = onClick
        )
}

private fun dashboardShortcutButtonIcon(button: Int): ImageVector {
        return if (button == 1) Icons.Default.Add else Icons.Default.Star
}

@Composable
private fun DashboardTopDragHandle(modifier: Modifier = Modifier) {
        Box(
                modifier = modifier.width(148.dp).height(24.dp),
                contentAlignment = Alignment.Center
        ) {
                Box(
                        modifier =
                                Modifier.width(86.dp)
                                        .height(5.dp)
                                        .background(
                                                Color.White.copy(alpha = 0.42f),
                                                RoundedCornerShape(50)
                                        )
                )
        }
}

@Composable
private fun DashboardTopDragTouchTarget(modifier: Modifier = Modifier, onCollapse: () -> Unit) {
        Spacer(
                modifier =
                        modifier.fillMaxWidth()
                                .height(44.dp)
                                .zIndex(2f)
                                .pointerInput(onCollapse) {
                                        var totalDragY = 0f
                                        detectDragGestures(
                                                onDragStart = { totalDragY = 0f },
                                                onDragCancel = { totalDragY = 0f },
                                                onDragEnd = {
                                                        if (shouldCollapseDashboardAfterDrag(totalDragY)) {
                                                                onCollapse()
                                                        }
                                                        totalDragY = 0f
                                                }
                                        ) { change, dragAmount ->
                                                if (dragAmount.y > 0f) {
                                                        totalDragY += dragAmount.y
                                                        change.consume()
                                                }
                                        }
                                }
        )
}

@Composable
private fun DashboardCardSlot(
        cardId: DashboardCardId,
        order: List<DashboardCardId>,
        layoutEditMode: Boolean,
        snapshot: DashboardVehicleSnapshot,
        serviceManager: ServiceManager,
        appLabel: String?,
        appIcon: android.graphics.drawable.Drawable?,
        activeProjectionPackage: String?,
        albumBackground: DashboardAlbumBackgroundState,
        modifier: Modifier,
        onMoveCard: (DashboardCardId, Int) -> Unit
) {
        val controls: @Composable (() -> Unit)? =
                if (layoutEditMode) {
                        {
                                DashboardCardPositionControls(
                                        cardId = cardId,
                                        order = order,
                                        onMoveCard = onMoveCard
                                )
                        }
                } else {
                        null
                }

        val content: @Composable (Modifier) -> Unit = { contentModifier ->
                when (cardId) {
                        DashboardCardId.MEDIA ->
                                DashboardMediaPanel(
                                        appLabel = appLabel,
                                        appIcon = appIcon,
                                        activeProjectionPackage = activeProjectionPackage,
                                        albumBackground = albumBackground,
                                        volume = snapshot.volume,
                                        serviceManager = serviceManager,
                                        layoutControls = controls,
                                        modifier = contentModifier
                                )
                        DashboardCardId.DYNAMICS ->
                                DashboardSettingsPanel(
                                        snapshot = snapshot,
                                        serviceManager = serviceManager,
                                        albumBackground = albumBackground,
                                        layoutControls = controls,
                                        modifier = contentModifier
                                )
                        DashboardCardId.HVAC ->
                                DashboardHvacPanel(
                                        snapshot = snapshot,
                                        serviceManager = serviceManager,
                                        albumBackground = albumBackground,
                                        layoutControls = controls,
                                        modifier = contentModifier
                                )
                }
        }

        content(modifier)
}

@Composable
private fun DashboardCardPositionControls(
        cardId: DashboardCardId,
        order: List<DashboardCardId>,
        onMoveCard: (DashboardCardId, Int) -> Unit
) {
        val index = order.indexOf(cardId)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                DashboardMiniIconButton(
                        icon = Icons.Default.KeyboardArrowLeft,
                        enabled = index > 0,
                        contentDescription = "Mover card para esquerda"
                ) {
                        onMoveCard(cardId, -1)
                }
                DashboardMiniIconButton(
                        icon = Icons.Default.KeyboardArrowRight,
                        enabled = index in 0 until order.lastIndex,
                        contentDescription = "Mover card para direita"
                ) {
                        onMoveCard(cardId, 1)
                }
        }
}

@Composable
private fun DashboardMiniIconButton(
        icon: ImageVector,
        enabled: Boolean,
        contentDescription: String,
        onClick: () -> Unit
) {
        Surface(
                onClick = onClick,
                enabled = enabled,
                color = Color.White.copy(alpha = if (enabled) 0.10f else 0.04f),
                shape = RoundedCornerShape(8.dp),
                border =
                        BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = if (enabled) 0.18f else 0.06f)
                        ),
                modifier = Modifier.size(34.dp)
        ) {
                Box(contentAlignment = Alignment.Center) {
                        Icon(
                                icon,
                                contentDescription = contentDescription,
                                tint = Color.White.copy(alpha = if (enabled) 0.92f else 0.28f),
                                modifier = Modifier.size(22.dp)
                        )
                }
        }
}

@Composable
private fun DashboardSettingsPanel(
        snapshot: DashboardVehicleSnapshot,
        serviceManager: ServiceManager,
        albumBackground: DashboardAlbumBackgroundState? = null,
        layoutControls: @Composable (() -> Unit)? = null,
        modifier: Modifier
) {
        DashboardPanel(modifier = modifier, albumBackground = albumBackground) {
                val driveOptions = listOf("2" to "Eco", "0" to "Normal", "1" to "Sport")
                val powerOptions = listOf("0" to "HEV", "1" to "EV Prior.", "3" to "EV")
                val regenOptions = listOf("2" to "Baixo", "0" to "Normal", "1" to "Alto")
                val steeringOptions = listOf("2" to "Conforto", "0" to "Normal", "1" to "Sport")
                val context = LocalContext.current
                var showHevDialog by remember { mutableStateOf(false) }
                if (showHevDialog) {
                        HevModeDialog(
                                snapshot = snapshot,
                                serviceManager = serviceManager,
                                onDismiss = { showHevDialog = false }
                        )
                }

                Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                DashboardPanelTitle(Icons.Default.Tune, "Dinâmica")
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        layoutControls?.invoke()
                                        DashboardDynamicsReadyBadge(snapshot.readyState)
                                }
                        }
                        Row(
                                // 138 -> 120dp: a faixa 12V abaixo tirou altura de um painel de
                                // ALTURA FIXA, e os cards de baixo (Conducao/Energia/Regeneracao/
                                // Direcao) perderam a 3a linha de texto (cortada). Devolvendo 18dp
                                // aqui + 6dp do padding da faixa, eles voltam a caber.
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                DashboardCircularResourceGauge(
                                        label = "Bateria",
                                        value = formatPercent(snapshot.batteryPercent),
                                        fraction = percentFraction(snapshot.batteryPercent),
                                        detail = formatDistance(snapshot.batteryRange),
                                        accent = Color(0xFF78E08F),
                                        icon = Icons.Default.BatteryChargingFull,
                                        modifier = Modifier.weight(1f)
                                )
                                DashboardCircularResourceGauge(
                                        label = "Combustível",
                                        value = formatPercent(snapshot.fuelPercent),
                                        fraction = percentFraction(snapshot.fuelPercent),
                                        detail =
                                                formatDashboardFuelLitersAndRange(
                                                        snapshot.fuelPercent,
                                                        snapshot.fuelRange
                                                ),
                                        accent = Color(0xFFFFC857),
                                        icon = Icons.Default.LocalGasStation,
                                        modifier = Modifier.weight(1f)
                                )
                        }
                        // Bateria 12V numa FAIXA FINA abaixo (opcao C). A tentativa anterior era um
                        // bloco de 132dp DENTRO da fileira acima (opcao B): comprovadamente espremeu os
                        // dois aneis — no carro os rotulos truncaram ("Ba...", "Co...", "102 ...").
                        // Aqui os aneis voltam a largura cheia e a 12V ocupa ~34dp.
                        // SEM Spacer: este Column ja usa spacedBy(14.dp). Um Spacer(10) somava aos DOIS
                        // vaos do arranjo (14+10+14 = 38dp) e a faixa ficava "descolada" dos aneis,
                        // alem de empurrar os cards de baixo. Sem ele o vao volta a 14dp, no mesmo
                        // ritmo do resto do painel, e os cards de baixo sobem 24dp.
                        DashboardBattery12vStrip(
                                pct = snapshot.batt12vPct,
                                voltageRaw = snapshot.batt12vVoltage
                        )
                        Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        DashboardPremiumCycleControl(
                                                label = "Condução",
                                                value = driveModeLabel(snapshot.driveMode),
                                                nextValue =
                                                        nextDashboardOptionLabel(
                                                                snapshot.driveMode,
                                                                driveOptions
                                                        ),
                                                icon = Icons.Default.Speed,
                                                accent = Color(0xFF66E3FF),
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                serviceManager.updateDataOptimistic(
                                                        CarConstants
                                                                .CAR_DRIVE_SETTING_DRIVE_MODE
                                                                .getValue(),
                                                        nextDashboardOption(
                                                                snapshot.driveMode,
                                                                driveOptions
                                                        )
                                                )
                                        }
                                        DashboardPremiumCycleControl(
                                                label = "Energia",
                                                value = powerModelLabel(snapshot.powerModel, snapshot.powerReserve, snapshot.socTarget),
                                                nextValue =
                                                        nextDashboardOptionLabel(
                                                                snapshot.powerModel,
                                                                powerOptions
                                                        ),
                                                icon = Icons.Default.ElectricBolt,
                                                accent = Color(0xFF78E08F),
                                                modifier = Modifier.weight(1f),
                                                onLongClick = {
                                                        // Toque longo em HEV -> popup do sub-modo
                                                        // (Inteligente/Prioritário + % de bateria).
                                                        if (snapshot.powerModel.trim() == "0") {
                                                                showHevDialog = true
                                                        }
                                                }
                                        ) {
                                                serviceManager.updateDataOptimistic(
                                                        CarConstants
                                                                .CAR_EV_SETTING_POWER_MODEL_CONFIG
                                                                .getValue(),
                                                        nextDashboardOption(
                                                                snapshot.powerModel,
                                                                powerOptions
                                                        )
                                                )
                                        }
                                }
                                Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        val onePedalOn =
                                                snapshot.onePedalEnabled.trim() == "1"
                                        DashboardPremiumCycleControl(
                                                label = "Regeneração",
                                                value =
                                                        if (onePedalOn) "One Pedal"
                                                        else regenLabel(snapshot.energyRecovery),
                                                nextValue =
                                                        nextDashboardOptionLabel(
                                                                snapshot.energyRecovery,
                                                                regenOptions
                                                        ),
                                                icon = Icons.Default.Autorenew,
                                                accent = Color(0xFFFFC857),
                                                modifier = Modifier.weight(1f),
                                                hideNext = onePedalOn,
                                                hint =
                                                        if (onePedalOn)
                                                                "Segurar desativa One Pedal"
                                                        else "Segurar ativa One Pedal",
                                                onLongClick = {
                                                        // Toque longo -> liga/desliga o One Pedal.
                                                        // Independe do nível de regeneração (One
                                                        // Pedal é um "nível" à parte). Lê o estado
                                                        // fresco (igual à ação do volante) p/ evitar
                                                        // race do cache logo após o boot.
                                                        val current =
                                                                serviceManager.getUpdatedData(
                                                                        CarConstants
                                                                                .CAR_CONFIGURE_PEDAL_CONTROL_ENABLE
                                                                                .getValue()
                                                                )
                                                                        ?: snapshot.onePedalEnabled
                                                        val turningOn = current.trim() != "1"
                                                        serviceManager.updateDataOptimistic(
                                                                CarConstants
                                                                        .CAR_CONFIGURE_PEDAL_CONTROL_ENABLE
                                                                        .getValue(),
                                                                if (turningOn) "1" else "0"
                                                        )
                                                        Toast.makeText(
                                                                        context,
                                                                        if (turningOn)
                                                                                "One Pedal ativado"
                                                                        else "One Pedal desativado",
                                                                        Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                }
                                        ) {
                                                // Tap cicla a regeneração só com o One Pedal
                                                // desligado (ligado, o nível é indiferente).
                                                if (!onePedalOn) {
                                                        serviceManager.updateDataOptimistic(
                                                                CarConstants
                                                                        .CAR_EV_SETTING_ENERGY_RECOVERY_LEVEL
                                                                        .getValue(),
                                                                nextDashboardOption(
                                                                        snapshot.energyRecovery,
                                                                        regenOptions
                                                                )
                                                        )
                                                }
                                        }
                                        DashboardPremiumCycleControl(
                                                label = "Direção",
                                                value = steeringModeLabel(snapshot.steeringMode),
                                                nextValue =
                                                        nextDashboardOptionLabel(
                                                                snapshot.steeringMode,
                                                                steeringOptions
                                                        ),
                                                icon = DashboardSteeringWheelIcon,
                                                accent = Color(0xFFB7A6FF),
                                                modifier = Modifier.weight(1f)
                                        ) {
                                                serviceManager.updateDataOptimistic(
                                                        CarConstants
                                                                .CAR_DRIVE_SETTING_STEERING_WHEEL_ASSIST_MODE
                                                                .getValue(),
                                                        nextDashboardOption(
                                                                snapshot.steeringMode,
                                                                steeringOptions
                                                        )
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun DashboardMediaPanel(
        appLabel: String?,
        appIcon: android.graphics.drawable.Drawable?,
        activeProjectionPackage: String?,
        albumBackground: DashboardAlbumBackgroundState,
        volume: String,
        serviceManager: ServiceManager,
        layoutControls: @Composable (() -> Unit)? = null,
        modifier: Modifier
) {
        val context = LocalContext.current
        val mediaTitle = BottomBarState.mediaTitle?.takeIf { it.isNotBlank() }
        val mediaArtist = BottomBarState.mediaArtist?.takeIf { it.isNotBlank() }
        val mediaAlbum = BottomBarState.mediaAlbum?.takeIf { it.isNotBlank() }
        val mediaArtwork = BottomBarState.mediaArtwork
        val mediaPackageName = BottomBarState.mediaPackageName
        val isPlaying = BottomBarState.mediaIsPlaying
        val durationMs = BottomBarState.mediaDurationMs
        val elapsedMs = BottomBarState.mediaElapsedMs
        val progressUpdatedAtMs = BottomBarState.mediaProgressUpdatedAtMs
        val canSeek = BottomBarState.mediaCanSeek
        val displayedElapsedMs =
                rememberMediaElapsedMs(
                        elapsedMs = elapsedMs,
                        durationMs = durationMs,
                        progressUpdatedAtMs = progressUpdatedAtMs,
                        isPlaying = isPlaying
                )
        // States, NAO valores: ler aqui recomporia este card a cada frame do tween de 900ms.
        // As leituras acontecem no draw (drawBehind) e dentro do medidor de progresso.
        val dynamicPrimary = albumBackground.primary
        val dynamicSecondary = albumBackground.secondary
        val dynamicAccent = albumBackground.accent
        val dynamicDark = albumBackground.dark
        val title =
                mediaTitle
                        ?: appLabel
                        ?: shortProjectionLabel(activeProjectionPackage)
                        ?: "Audio"
        val mediaSubtitle =
                listOfNotNull(mediaArtist, mediaAlbum).distinct().joinToString(" • ")
                        .takeIf { it.isNotBlank() }
        val subtitle =
                mediaSubtitle
                        ?: when {
                                activeProjectionPackage != null -> "Projecao ativa no cluster"
                                mediaPackageName != null -> "Midia do sistema"
                                else -> "Sistema de audio"
                        }
        var visibleVolume by remember {
                mutableIntStateOf(
                        volume.toIntOrNull()
                                ?.coerceIn(DASHBOARD_MEDIA_VOLUME_MIN, DASHBOARD_MEDIA_VOLUME_MAX)
                                ?: DASHBOARD_MEDIA_VOLUME_MIN
                )
        }
        var pendingVolume by remember { mutableStateOf<Int?>(null) }
        var pendingVolumeAtMs by remember { mutableLongStateOf(0L) }

        LaunchedEffect(volume) {
                val remoteVolume =
                        volume.toIntOrNull()
                                ?.coerceIn(DASHBOARD_MEDIA_VOLUME_MIN, DASHBOARD_MEDIA_VOLUME_MAX)
                                ?: return@LaunchedEffect
                val pending = pendingVolume
                val pendingAgeMs = SystemClock.elapsedRealtime() - pendingVolumeAtMs
                if (pending == null || remoteVolume == pending || pendingAgeMs > 1800L) {
                        visibleVolume = remoteVolume
                        if (remoteVolume == pending || pendingAgeMs > 1800L) {
                                pendingVolume = null
                        }
                }
        }

        fun updateVolume(delta: Int) {
                if (delta == 0) return
                val serviceVolume =
                        serviceManager
                                .getData(CarConstants.SYS_SETTINGS_AUDIO_MEDIA_VOLUME.getValue())
                                ?.toIntOrNull()
                                ?.coerceIn(DASHBOARD_MEDIA_VOLUME_MIN, DASHBOARD_MEDIA_VOLUME_MAX)
                val base = pendingVolume ?: serviceVolume ?: visibleVolume
                val next = resolveDashboardMediaVolumeAfterDelta(base, delta)
                adjustDashboardSystemMediaVolume(context, delta)
                visibleVolume = next
                pendingVolume = next
                pendingVolumeAtMs = SystemClock.elapsedRealtime()
                serviceManager.updateData(
                        CarConstants.SYS_SETTINGS_AUDIO_MEDIA_VOLUME.getValue(),
                        next.toString()
                )
        }

        fun runMediaControl(action: () -> Unit) {
                BottomBarService.suppressDashboardControlFocusRestore("dashboard_media_control")
                action()
        }

        Box(
                modifier =
                        modifier.clip(RoundedCornerShape(8.dp))
                                .drawBehind {
                                        drawRect(
                                                Brush.linearGradient(
                                                        colors =
                                                                listOf(
                                                                        dynamicPrimary.value
                                                                                .copy(alpha = 0.38f),
                                                                        dynamicSecondary.value
                                                                                .copy(alpha = 0.28f),
                                                                        dynamicDark.value
                                                                                .copy(alpha = 0.98f)
                                                                ),
                                                        start = Offset.Zero,
                                                        end = Offset(900f, 620f)
                                                )
                                        )
                                }
                                .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.14f),
                                        RoundedCornerShape(8.dp)
                                )
        ) {
                DashboardAlbumDynamicBackground(
                        primaryState = dynamicPrimary,
                        secondaryState = dynamicSecondary,
                        accentState = dynamicAccent,
                        darkState = dynamicDark,
                        hasArtwork = mediaArtwork != null,
                        modifier = Modifier.matchParentSize()
                )
                Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                DashboardPanelTitle(Icons.Default.Album, "Midia")
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        layoutControls?.invoke()
                                        DashboardMediaBadge(
                                                isPlaying = isPlaying,
                                                hasMetadata = mediaTitle != null
                                        )
                                }
                        }
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.06f))
                                                .border(
                                                        1.dp,
                                                        Color.White.copy(alpha = 0.12f),
                                                        RoundedCornerShape(8.dp)
                                                )
                        ) {
                                if (mediaArtwork != null) {
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .drawBehind {
                                                                        drawRect(
                                                                                Brush.linearGradient(
                                                                                        colors =
                                                                                                listOf(
                                                                                                        dynamicPrimary.value,
                                                                                                        dynamicSecondary.value,
                                                                                                        dynamicDark.value
                                                                                                ),
                                                                                        start = Offset.Zero,
                                                                                        end = Offset(900f, 620f)
                                                                                )
                                                                        )
                                                                }
                                        )
                                        Image(
                                                // remember: asImageBitmap() envolve/copia o Bitmap a
                                                // cada recomposicao. Este card recompoe em rajada
                                                // (animacao da capa/troca de faixa), entao sem cache
                                                // isso e alocacao por frame.
                                                bitmap =
                                                        remember(mediaArtwork) {
                                                                mediaArtwork.asImageBitmap()
                                                        },
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                        )
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .background(
                                                                        Brush.verticalGradient(
                                                                                colors =
                                                                                        listOf(
                                                                                                Color.Transparent,
                                                                                                Color.Black.copy(alpha = 0.72f)
                                                                                        ),
                                                                                startY = 120f
                                                                        )
                                                                )
                                        )
                                } else {
                                        DashboardArtworkFallback(
                                                appIcon = appIcon,
                                                activeProjectionPackage = activeProjectionPackage,
                                                modifier = Modifier.fillMaxSize()
                                        )
                                }
                                Column(
                                        modifier =
                                                Modifier.align(Alignment.BottomStart)
                                                        .fillMaxWidth()
                                                        .padding(20.dp)
                                ) {
                                        Text(
                                                text = title,
                                                color = Color.White,
                                                fontSize = 31.sp,
                                                fontFamily = DashboardReadableFont,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                                text = subtitle,
                                                color = Color.White.copy(alpha = 0.72f),
                                                fontSize = 16.sp,
                                                fontFamily = DashboardReadableFont,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 6.dp)
                                        )
                                }
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                DashboardIconButton(
                                        Icons.Default.SkipPrevious,
                                        size = 66.dp,
                                        contentDescription = "Musica anterior"
                                ) {
                                        runMediaControl {
                                                BottomBarService.skipCurrentMediaPrevious()
                                        }
                                }
                                DashboardIconButton(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        size = 66.dp,
                                        contentDescription =
                                                if (isPlaying) "Pausar musica" else "Reproduzir musica"
                                ) {
                                        runMediaControl {
                                                BottomBarService.toggleCurrentMediaPlayback()
                                        }
                                }
                                DashboardMediaProgressMeter(
                                        elapsedMs = displayedElapsedMs,
                                        durationMs = durationMs,
                                        canSeek = canSeek,
                                        accentState = dynamicAccent,
                                        modifier = Modifier.weight(1f),
                                        onSeek = { positionMs ->
                                                runMediaControl {
                                                        BottomBarService.seekCurrentMediaTo(positionMs)
                                                }
                                        }
                                )
                                DashboardIconButton(
                                        Icons.Default.SkipNext,
                                        size = 66.dp,
                                        contentDescription = "Proxima musica"
                                ) {
                                        runMediaControl {
                                                BottomBarService.skipCurrentMediaNext()
                                        }
                                }
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                DashboardIconButton(
                                        Icons.Default.Remove,
                                        size = 70.dp,
                                        contentDescription = "Diminuir volume"
                                ) {
                                        runMediaControl { updateVolume(-1) }
                                }
                                DashboardLinearMeter(
                                        label = "Volume",
                                        value = visibleVolume.toString(),
                                        fraction =
                                                (visibleVolume / DASHBOARD_MEDIA_VOLUME_MAX.toFloat())
                                                        .coerceIn(0f, 1f),
                                        accent = Color(0xFF66E3FF),
                                        icon = Icons.Default.VolumeUp,
                                        modifier = Modifier.weight(1f)
                                )
                                DashboardIconButton(
                                        Icons.Default.Add,
                                        size = 70.dp,
                                        contentDescription = "Aumentar volume"
                                ) {
                                        runMediaControl { updateVolume(1) }
                                }
                        }
                }
        }
}

@Composable
private fun rememberMediaElapsedMs(
        elapsedMs: Long,
        durationMs: Long,
        progressUpdatedAtMs: Long,
        isPlaying: Boolean
): Long {
        var nowMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(elapsedMs, durationMs, progressUpdatedAtMs, isPlaying) {
                nowMs = SystemClock.elapsedRealtime()
                while (isPlaying && durationMs > 0L) {
                        delay(1000)
                        nowMs = SystemClock.elapsedRealtime()
                }
        }
        val deltaMs =
                if (isPlaying && durationMs > 0L && progressUpdatedAtMs > 0L) {
                        (nowMs - progressUpdatedAtMs).coerceAtLeast(0L)
                } else {
                        0L
                }
        return if (durationMs > 0L) {
                (elapsedMs + deltaMs).coerceIn(0L, durationMs)
        } else {
                elapsedMs.coerceAtLeast(0L)
        }
}

// Guarda State<Color> em vez de Color DE PROPOSITO. As 4 cores vem de animateColorAsState (tween de
// 900ms): se o valor fosse lido aqui, a leitura seria atribuida ao escopo de quem chama
// rememberDashboardAlbumBackgroundState (= ExpandedImpulseDashboard, que nao gera restart group por
// retornar valor) e CADA FRAME da animacao recomporia o dashboard inteiro (~54 recomposicoes em
// 900ms, justo em cima do gesto de arraste e da animacao de entrada). Carregando o State, a leitura
// acontece onde a cor e realmente usada — no draw do Canvas, que so redesenha.
private data class DashboardAlbumBackgroundState(
        val primary: State<Color>,
        val secondary: State<Color>,
        val accent: State<Color>,
        val dark: State<Color>,
        val hasArtwork: Boolean
)

@Composable
private fun rememberDashboardAlbumBackgroundState(): DashboardAlbumBackgroundState {
        val mediaTitle = BottomBarState.mediaTitle?.takeIf { it.isNotBlank() }
        val mediaArtist = BottomBarState.mediaArtist?.takeIf { it.isNotBlank() }
        val mediaAlbum = BottomBarState.mediaAlbum?.takeIf { it.isNotBlank() }
        val mediaArtwork = BottomBarState.mediaArtwork
        val mediaPackageName = BottomBarState.mediaPackageName
        val artworkKey =
                remember(mediaPackageName, mediaTitle, mediaArtist, mediaAlbum, mediaArtwork) {
                        listOfNotNull(
                                        mediaPackageName,
                                        mediaTitle,
                                        mediaArtist,
                                        mediaAlbum,
                                        mediaArtwork?.width?.toString(),
                                        mediaArtwork?.height?.toString()
                                )
                                .joinToString("|")
                }
        var albumColors by remember { mutableStateOf(AlbumBackgroundService.fallbackColors) }
        LaunchedEffect(artworkKey, mediaArtwork) {
                albumColors =
                        withContext(Dispatchers.Default) {
                                AlbumBackgroundService.extractColors(mediaArtwork, artworkKey)
                        }
        }
        val dynamicPrimary =
                animateColorAsState(
                        targetValue = Color(albumColors.primary),
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "dashboardAlbumPrimary"
                )
        val dynamicSecondary =
                animateColorAsState(
                        targetValue = Color(albumColors.secondary),
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "dashboardAlbumSecondary"
                )
        val dynamicAccent =
                animateColorAsState(
                        targetValue = Color(albumColors.accent),
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "dashboardAlbumAccent"
                )
        val dynamicDark =
                animateColorAsState(
                        targetValue = Color(albumColors.dark),
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "dashboardAlbumDark"
                )
        return DashboardAlbumBackgroundState(
                primary = dynamicPrimary,
                secondary = dynamicSecondary,
                accent = dynamicAccent,
                dark = dynamicDark,
                hasArtwork = mediaArtwork != null
        )
}

@Composable
private fun DashboardAlbumDynamicBackground(
        primaryState: State<Color>,
        secondaryState: State<Color>,
        accentState: State<Color>,
        darkState: State<Color>,
        hasArtwork: Boolean,
        modifier: Modifier = Modifier,
        cornerRadius: Dp = 8.dp,
        artworkAlpha: Float = 1f,
        fallbackAlpha: Float = 0.56f,
        artworkScrimAlpha: Float = 0.42f,
        fallbackScrimAlpha: Float = 0.58f
) {
        Canvas(
                modifier =
                        modifier.fillMaxSize()
                                .clip(RoundedCornerShape(cornerRadius))
                                .alpha(if (hasArtwork) artworkAlpha else fallbackAlpha)
        ) {
                // Leitura no DRAW: invalida so o desenho, nao a composicao.
                val primary = primaryState.value
                val secondary = secondaryState.value
                val accent = accentState.value
                val dark = darkState.value
                drawRect(
                        brush =
                                Brush.linearGradient(
                                        colors =
                                                listOf(
                                                        primary.copy(alpha = 0.34f),
                                                        secondary.copy(alpha = 0.3f),
                                                        dark.copy(alpha = 0.72f)
                                                ),
                                        start = Offset.Zero,
                                        end = Offset(size.width, size.height)
                                )
                )
                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        primary.copy(alpha = 0.58f),
                                                        primary.copy(alpha = 0.08f),
                                                        Color.Transparent
                                                ),
                                        center = Offset(size.width * 0.22f, size.height * 0.18f),
                                        radius = size.maxDimension * 0.72f
                                ),
                        radius = size.maxDimension * 0.72f,
                        center = Offset(size.width * 0.22f, size.height * 0.18f)
                )
                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        secondary.copy(alpha = 0.48f),
                                                        secondary.copy(alpha = 0.08f),
                                                        Color.Transparent
                                                ),
                                        center = Offset(size.width * 0.82f, size.height * 0.72f),
                                        radius = size.maxDimension * 0.82f
                                ),
                        radius = size.maxDimension * 0.82f,
                        center = Offset(size.width * 0.82f, size.height * 0.72f)
                )
                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        accent.copy(alpha = 0.22f),
                                                        Color.Transparent
                                                ),
                                        center = Offset(size.width * 0.62f, size.height * 0.12f),
                                        radius = size.maxDimension * 0.48f
                                ),
                        radius = size.maxDimension * 0.48f,
                        center = Offset(size.width * 0.62f, size.height * 0.12f)
                )
                drawRect(Color.Black.copy(alpha = if (hasArtwork) artworkScrimAlpha else fallbackScrimAlpha))
        }
}

@Composable
private fun DashboardMediaProgressMeter(
        elapsedMs: Long,
        durationMs: Long,
        canSeek: Boolean,
        accentState: State<Color>,
        modifier: Modifier = Modifier,
        onSeek: (Long) -> Unit
) {
        // Leitura confinada AQUI: o tween da cor recompoe so este medidor, nao o card de midia
        // inteiro (que e onde estao a capa, os textos e os controles).
        val accent = accentState.value
        var trackWidthPx by remember { mutableFloatStateOf(1f) }
        var dragFraction by remember { mutableStateOf<Float?>(null) }
        val progressFraction =
                if (durationMs > 0L) {
                        elapsedMs.toFloat() / durationMs.toFloat()
                } else {
                        0f
                }
        val activeFraction = (dragFraction ?: progressFraction).coerceIn(0f, 1f)
        val trackModifier =
                Modifier.fillMaxWidth()
                        .height(14.dp)
                        .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(canSeek, durationMs, trackWidthPx) {
                                if (!canSeek || durationMs <= 0L) return@pointerInput

                                fun updateFraction(positionX: Float) {
                                        dragFraction = (positionX / trackWidthPx).coerceIn(0f, 1f)
                                }

                                awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        updateFraction(down.position.x)
                                        down.consume()

                                        var pressed: Boolean
                                        do {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull()
                                                if (change != null) {
                                                        updateFraction(change.position.x)
                                                        change.consume()
                                                }
                                                pressed = event.changes.any { it.pressed }
                                        } while (pressed)

                                        val target =
                                                ((dragFraction ?: activeFraction) * durationMs)
                                                        .roundToLong()
                                                        .coerceIn(0L, durationMs)
                                        dragFraction = null
                                        onSeek(target)
                                }
                        }

        Column(
                modifier =
                        modifier.fillMaxWidth()
                                .height(72.dp)
                                .background(Color.White.copy(alpha = 0.065f), RoundedCornerShape(8.dp))
                                .border(
                                        1.dp,
                                        accent.copy(alpha = if (durationMs > 0L) 0.24f else 0.1f),
                                        RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp)
                                )
                                Text(
                                        text = "Tempo",
                                        color = Color.White.copy(alpha = 0.68f),
                                        fontSize = 13.sp,
                                        fontFamily = DashboardReadableFont,
                                        fontWeight = FontWeight.Medium
                                )
                        }
                        Text(
                                text =
                                        "${formatMediaTime(elapsedMs)} / ${
                                                formatMediaTime(durationMs, unknownWhenZero = true)
                                        }",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = DashboardReadableFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                        )
                }
                Box(
                        modifier =
                                trackModifier.background(
                                        Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(50)
                                )
                ) {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth(activeFraction)
                                                .fillMaxHeight()
                                                .background(accent, RoundedCornerShape(50))
                        )
                        if (durationMs > 0L) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth(
                                                                activeFraction.coerceIn(0.01f, 1f)
                                                        )
                                                        .fillMaxHeight(),
                                        contentAlignment = Alignment.CenterEnd
                                ) {
                                        Box(
                                                modifier =
                                                        Modifier.size(if (canSeek) 20.dp else 14.dp)
                                                                .background(
                                                                        Color.White,
                                                                        CircleShape
                                                                )
                                                                .border(
                                                                        2.dp,
                                                                        accent.copy(alpha = 0.86f),
                                                                        CircleShape
                                                                )
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun DashboardMediaBadge(isPlaying: Boolean, hasMetadata: Boolean) {
        Row(
                modifier =
                        Modifier.background(
                                        if (isPlaying) Color(0xFF78E08F).copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                )
                                .border(
                                        1.dp,
                                        if (isPlaying) Color(0xFF78E08F).copy(alpha = 0.42f)
                                        else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
                Icon(
                        if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isPlaying) Color(0xFF78E08F) else Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(20.dp)
                )
                Text(
                        text =
                                when {
                                        isPlaying -> "PLAY"
                                        hasMetadata -> "MIDIA"
                                        else -> "AUDIO"
                                },
                        color = if (isPlaying) Color(0xFF78E08F) else Color.White.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        fontFamily = DashboardReadableFont,
                        fontWeight = FontWeight.Bold
                )
        }
}

@Composable
private fun DashboardArtworkFallback(
        appIcon: android.graphics.drawable.Drawable?,
        activeProjectionPackage: String?,
        modifier: Modifier = Modifier
) {
        Box(
                modifier =
                        modifier.background(
                                Brush.linearGradient(
                                        colors =
                                                listOf(
                                                        Color(0xFF1B2A31),
                                                        Color(0xFF11161A),
                                                        Color(0xFF2A2214)
                                                ),
                                        start = Offset.Zero,
                                        end = Offset(900f, 620f)
                                )
                        ),
                contentAlignment = Alignment.Center
        ) {
                Box(
                        modifier =
                                Modifier.size(168.dp)
                                        .background(
                                                Color.White.copy(alpha = 0.08f),
                                                RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                                1.dp,
                                                Color.White.copy(alpha = 0.16f),
                                                RoundedCornerShape(8.dp)
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                        if (appIcon != null) {
                                AsyncImage(
                                        model = appIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(104.dp)
                                )
                        } else {
                                Icon(
                                        if (activeProjectionPackage == BOTTOM_BAR_CARPLAY_PACKAGE)
                                                Icons.Default.DirectionsCar
                                        else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.78f),
                                        modifier = Modifier.size(82.dp)
                                )
                        }
                }
                Text(
                        text = shortProjectionLabel(activeProjectionPackage) ?: "IMPULSE AUDIO",
                        color = Color.White.copy(alpha = 0.1f),
                        fontSize = 34.sp,
                        fontFamily = DashboardReadableFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart).padding(18.dp),
                        maxLines = 1
                )
        }
}

@Composable
private fun DashboardQuickActionsPanel(
        context: Context,
        scope: CoroutineScope,
        effectivePackage: String?,
        modifier: Modifier
) {
        DashboardPanel(modifier = modifier) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        DashboardPanelTitle(Icons.Default.Apps, "Atalhos")
                        DashboardActionButton(
                                text = "Enviar ao cluster",
                                icon = Icons.Default.KeyboardArrowLeft,
                                enabled = effectivePackage != null
                        ) {
                                val pkg = effectivePackage ?: return@DashboardActionButton
                                scope.launch {
                                        DisplayAppLauncher.getOrCreateDefaultConfig(context, pkg)
                                                ?.let { DisplayAppLauncher.sendToDisplay(it) }
                                }
                        }
                        DashboardActionButton(
                                text = "Trazer para D0",
                                icon = Icons.Default.KeyboardArrowRight
                        ) {
                                scope.launch { DisplayAppLauncher.bringAllToMainDisplay() }
                        }
                        DashboardActionButton(
                                text = "Menu de apps",
                                icon = Icons.Default.GridView
                        ) {
                                BottomBarState.isDashboardExpanded = false
                                BottomBarState.isMenuExpanded = true
                        }
                        DashboardActionButton(
                                text = "Ocultar painel",
                                icon = Icons.Default.KeyboardDoubleArrowDown
                        ) {
                                BottomBarState.isDashboardExpanded = false
                                BottomBarState.isVisible = false
                        }
                }
        }
}

@Composable
private fun DashboardHvacPanel(
        snapshot: DashboardVehicleSnapshot,
        serviceManager: ServiceManager,
        albumBackground: DashboardAlbumBackgroundState? = null,
        layoutControls: @Composable (() -> Unit)? = null,
        modifier: Modifier
) {
        val hvacEnabled = snapshot.hvacPower == "1"
        var blowerMode by remember { mutableStateOf(snapshot.blowerMode) }
        var driverSeatVentilation by remember {
                mutableStateOf(snapshot.driverSeatVentilation)
        }
        var passengerSeatVentilation by remember {
                mutableStateOf(snapshot.passengerSeatVentilation)
        }

        LaunchedEffect(snapshot.blowerMode) { blowerMode = snapshot.blowerMode }
        LaunchedEffect(snapshot.driverSeatVentilation) {
                driverSeatVentilation = snapshot.driverSeatVentilation
        }
        LaunchedEffect(snapshot.passengerSeatVentilation) {
                passengerSeatVentilation = snapshot.passengerSeatVentilation
        }

        DashboardPanel(modifier = modifier, albumBackground = albumBackground) {
                Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                DashboardPanelTitle(Icons.Default.AcUnit, "Climatização")
                                layoutControls?.invoke()
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                DashboardTempAdjuster(
                                        label = "Motorista",
                                        temp = snapshot.driverTemp,
                                        enabled = hvacEnabled,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        updateTemperature(
                                                serviceManager,
                                                CarConstants.CAR_HVAC_DRIVER_TEMPERATURE,
                                                snapshot.driverTemp,
                                                it
                                        )
                                }
                                DashboardTempAdjuster(
                                        label = "Passageiro",
                                        temp = snapshot.passTemp,
                                        enabled = hvacEnabled,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        updateTemperature(
                                                serviceManager,
                                                CarConstants.CAR_HVAC_PASS_TEMPERATURE,
                                                snapshot.passTemp,
                                                it
                                        )
                                }
                        }
                        DashboardFanAdjuster(
                                speed = snapshot.fanSpeed,
                                enabled = true,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                val next =
                                        (snapshot.fanSpeed.toIntOrNull() ?: 0).plus(it).coerceIn(0, 7)
                                serviceManager.updateData(
                                        CarConstants.CAR_HVAC_FAN_SPEED.getValue(),
                                        next.toString()
                                )
                                if (next == 0 && snapshot.hvacPower == "1") {
                                        serviceManager.updateData(
                                                CarConstants.CAR_HVAC_POWER_MODE.getValue(),
                                                "0"
                                        )
                                } else if (next > 0 && snapshot.hvacPower == "0") {
                                        serviceManager.updateData(
                                                CarConstants.CAR_HVAC_POWER_MODE.getValue(),
                                                "1"
                                        )
                                }
                        }
                        DashboardAirflowModeSelector(
                                mode = blowerMode,
                                enabled = hvacEnabled,
                                modifier = Modifier.fillMaxWidth()
                        ) { nextMode ->
                                blowerMode = nextMode
                                serviceManager.updateData(
                                        CarConstants.CAR_HVAC_BLOWER_MODE.getValue(),
                                        nextMode
                                )
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                DashboardSeatVentilationButton(
                                        label = "Motorista",
                                        level = driverSeatVentilation,
                                        maxLevel = snapshot.seatVentilationMaxLevel,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        val nextLevel =
                                                nextSeatVentilationLevel(
                                                        driverSeatVentilation,
                                                        snapshot.seatVentilationMaxLevel
                                                )
                                        driverSeatVentilation = nextLevel
                                        updateSeatVentilationLevel(
                                                serviceManager,
                                                CarConstants
                                                        .CAR_COMFORT_SETTING_DRIVER_SEAT_VENTILATION_LEVEL,
                                                nextLevel
                                        )
                                }
                                DashboardSeatVentilationButton(
                                        label = "Passageiro",
                                        level = passengerSeatVentilation,
                                        maxLevel = snapshot.seatVentilationMaxLevel,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        val nextLevel =
                                                nextSeatVentilationLevel(
                                                        passengerSeatVentilation,
                                                        snapshot.seatVentilationMaxLevel
                                                )
                                        passengerSeatVentilation = nextLevel
                                        updateSeatVentilationLevel(
                                                serviceManager,
                                                CarConstants
                                                        .CAR_COMFORT_SETTING_PASSENGER_SEAT_VENTILATION_LEVEL,
                                                nextLevel
                                        )
                                }
                        }
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                DashboardToggleButton(
                                        label = "AC",
                                        icon = Icons.Default.PowerSettingsNew,
                                        active = hvacEnabled,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        serviceManager.updateData(
                                                CarConstants.CAR_HVAC_POWER_MODE.getValue(),
                                                if (hvacEnabled) "0" else "1"
                                        )
                                }
                                DashboardToggleButton(
                                        label = "Auto",
                                        icon = Icons.Default.AutoMode,
                                        active = snapshot.acAuto == "1",
                                        enabled = hvacEnabled,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        serviceManager.updateData(
                                                CarConstants.CAR_HVAC_AUTO_ENABLE.getValue(),
                                                if (snapshot.acAuto == "1") "0" else "1"
                                        )
                                }
                                DashboardToggleButton(
                                        label = "Sync",
                                        icon = Icons.Default.Sync,
                                        active = snapshot.acSync == "1",
                                        enabled = hvacEnabled,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        serviceManager.updateData(
                                                CarConstants.CAR_HVAC_SYNC_ENABLE.getValue(),
                                                if (snapshot.acSync == "1") "0" else "1"
                                        )
                                }
                                DashboardToggleButton(
                                        label = "Recirc",
                                        icon = Icons.Default.Autorenew,
                                        active = snapshot.acRecirc == "1",
                                        enabled = hvacEnabled,
                                        modifier = Modifier.weight(1f)
                                ) {
                                        val next = if (snapshot.acRecirc == "1") "0" else "1"
                                        val carValue = if (next == "0") "1" else "0"
                                        serviceManager.updateData(
                                                CarConstants.CAR_HVAC_CYCLE_MODE.getValue(),
                                                carValue
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun DashboardPanel(
        modifier: Modifier = Modifier,
        albumBackground: DashboardAlbumBackgroundState? = null,
        content: @Composable BoxScope.() -> Unit
) {
        val shape = RoundedCornerShape(8.dp)
        Box(
                modifier =
                        modifier.clip(shape)
                                .background(
                                        Brush.linearGradient(
                                                colors =
                                                        listOf(
                                                                Color(0xFF171D22).copy(alpha = 0.86f),
                                                                Color(0xFF0E1115).copy(alpha = 0.9f)
                                                        )
                                        ),
                                        shape
                                )
                                .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.12f),
                                        shape
                                )
        ) {
                if (albumBackground != null) {
                        DashboardAlbumDynamicBackground(
                                primaryState = albumBackground.primary,
                                secondaryState = albumBackground.secondary,
                                accentState = albumBackground.accent,
                                darkState = albumBackground.dark,
                                hasArtwork = albumBackground.hasArtwork,
                                modifier = Modifier.matchParentSize(),
                                cornerRadius = 8.dp,
                                artworkAlpha = 0.86f,
                                fallbackAlpha = 0.34f,
                                artworkScrimAlpha = 0.58f,
                                fallbackScrimAlpha = 0.72f
                        )
                }
                Box(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        content = content
                )
        }
}

@Composable
private fun DashboardPanelTitle(icon: ImageVector, title: String, compact: Boolean = false) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFF66E3FF),
                        modifier = Modifier.size(if (compact) 22.dp else 26.dp)
                )
                Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (compact) 15.sp else 18.sp,
                        fontFamily = DashboardReadableFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
        }
}

@Composable
private fun DashboardStatusChip(icon: ImageVector, text: String, accent: Color? = null) {
        // accent != null realça o chip (mesmo formato das temperaturas, porém colorido) — usado, p.ex.,
        // em verde quando o HotRouter está roteando pela Starlink.
        val iconTint = accent ?: Color(0xFF66E3FF)
        val bg = accent?.copy(alpha = 0.15f) ?: Color.White.copy(alpha = 0.08f)
        val borderColor = accent?.copy(alpha = 0.40f) ?: Color.White.copy(alpha = 0.12f)
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                        Modifier.background(bg, RoundedCornerShape(8.dp))
                                .border(
                                        1.dp,
                                        borderColor,
                                        RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                Text(text = text, color = Color.White, fontSize = 14.sp, fontFamily = DashboardReadableFont)
        }
}

@Composable
private fun DashboardDynamicsReadyBadge(readyState: String) {
        val ready = readyState == "1" || readyState.equals("true", ignoreCase = true)
        val accent = if (ready) Color(0xFF78E08F) else Color.White.copy(alpha = 0.72f)
        Row(
                modifier =
                        Modifier.height(34.dp)
                                .background(accent.copy(alpha = if (ready) 0.15f else 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, accent.copy(alpha = if (ready) 0.38f else 0.14f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
                Box(
                        modifier = Modifier.size(8.dp).background(accent, CircleShape)
                )
                Text(
                        text = if (ready) "READY" else "STBY",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = DashboardReadableFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                )
        }
}

// ===================== Bateria 12V (auxiliar) =====================
// Faixas de tensao de uma bateria 12V de chumbo-acido. O que diz se ela esta boa e a TENSAO, nao a
// porcentagem que o carro estima — por isso a cor e o rotulo saem daqui, e a % e so complemento.
private const val BATT_12V_CHARGING_V = 13.2f // acima disso o DC-DC esta carregando
private const val BATT_12V_WEAK_V = 12.2f // abaixo disso ja e fraca
private const val BATT_12V_CRITICAL_V = 11.9f // abaixo disso a partida comeca a falhar

private class Batt12vState(val label: String, val accent: Color)

private fun batt12vState(voltage: Float?): Batt12vState =
        when {
            voltage == null -> Batt12vState("--", Color(0xFF8A93A3))
            voltage >= BATT_12V_CHARGING_V -> Batt12vState("CARREGANDO", Color(0xFF78E08F))
            voltage < BATT_12V_CRITICAL_V -> Batt12vState("CRITICA", Color(0xFFEF4444))
            voltage < BATT_12V_WEAK_V -> Batt12vState("FRACA", Color(0xFFFBBF24))
            else -> Batt12vState("NORMAL", Color(0xFF4A9EFF))
        }

// Faixa da barra = VOLTAGEM mapeada nesta janela, NAO a porcentagem. Motivo: no carro a %
// (car.ev_info.battery_charge_percentage) veio 0 com o motor ligado, entao uma barra baseada nela
// ficaria sempre vazia e mentiria sobre o estado. A tensao sempre existe e e o que de fato diz se a
// bateria esta boa.
private const val BATT_12V_BAR_MIN_V = 11.5f
private const val BATT_12V_BAR_MAX_V = 14.8f

/** Faixa fina abaixo dos aneis (opcao C). Nao rouba largura de bateria/combustivel. */
@Composable
private fun DashboardBattery12vStrip(pct: String, voltageRaw: String) {
        val voltage = voltageRaw.trim().replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }
        val state = batt12vState(voltage)
        val voltageText =
                voltage?.let { String.format(java.util.Locale.US, "%.2f", it).replace('.', ',') }
                        ?: "--"
        val fraction =
                voltage?.let {
                        ((it - BATT_12V_BAR_MIN_V) / (BATT_12V_BAR_MAX_V - BATT_12V_BAR_MIN_V))
                                .coerceIn(0f, 1f)
                }
                        ?: 0f
        // % so aparece se for um numero PLAUSIVEL (>0). Com 0/invalido, omitir e mais honesto que
        // exibir "0%" ao lado de uma bateria que esta carregando a 14V.
        val pctValue = pct.trim().toFloatOrNull()?.takeIf { it > 0f && it <= 100f }
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(8.dp))
                                .border(1.dp, state.accent.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text(
                        text = "Bateria 12 V",
                        color = state.accent,
                        fontSize = 11.sp,
                        fontFamily = DashboardReadableFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.em,
                        maxLines = 1
                )
                Box(
                        modifier =
                                Modifier.weight(1f)
                                        .height(7.dp)
                                        .background(
                                                Color.White.copy(alpha = 0.10f),
                                                RoundedCornerShape(99.dp)
                                        )
                ) {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth(fraction)
                                                .fillMaxHeight()
                                                .background(state.accent, RoundedCornerShape(99.dp))
                        )
                }
                if (pctValue != null) {
                        Text(
                                text = formatPercent(pct),
                                color = Color.White.copy(alpha = 0.62f),
                                fontSize = 12.sp,
                                fontFamily = DashboardReadableFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                        )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                                text = voltageText,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = DashboardReadableFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = "V",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                fontFamily = DashboardReadableFont,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                        )
                }
                Text(
                        text = state.label,
                        color = state.accent,
                        fontSize = 9.sp,
                        fontFamily = DashboardReadableFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.em,
                        maxLines = 1
                )
        }
}

@Composable
private fun DashboardCircularResourceGauge(
        label: String,
        value: String,
        fraction: Float,
        detail: String,
        accent: Color,
        icon: ImageVector,
        modifier: Modifier = Modifier
) {
        Row(
                modifier =
                        modifier.fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(8.dp))
                                .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
                Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 8.dp.toPx()
                                val diameter = size.minDimension - strokeWidth
                                val topLeft =
                                        Offset(
                                                (size.width - diameter) / 2f,
                                                (size.height - diameter) / 2f
                                        )
                                val arcSize = Size(diameter, diameter)
                                drawArc(
                                        color = Color.White.copy(alpha = 0.10f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                        color = accent,
                                        startAngle = -90f,
                                        sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                        text = value,
                                        color = Color.White,
                                        fontSize = 23.sp,
                                        fontFamily = DashboardReadableFont,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                )
                                Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = accent.copy(alpha = 0.88f),
                                        modifier = Modifier.size(18.dp)
                                )
                        }
                }
                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                        Text(
                                text = label,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontFamily = DashboardReadableFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        Text(
                                text = detail,
                                color = Color.White.copy(alpha = 0.62f),
                                fontSize = 13.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(4.dp)
                                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f))
                                                        .fillMaxHeight()
                                                        .background(accent.copy(alpha = 0.92f), RoundedCornerShape(4.dp))
                                )
                        }
                }
        }
}

@Composable
private fun DashboardCompactStatusTile(
        label: String,
        value: String,
        accent: Color,
        icon: ImageVector,
        modifier: Modifier = Modifier
) {
        Column(
                modifier =
                        modifier.height(74.dp)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                        Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.58f),
                                fontSize = 9.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
                Text(
                        text = value,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = DashboardReadableFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
        }
}

@Composable
private fun DashboardStatPill(
        label: String,
        value: String,
        icon: ImageVector,
        modifier: Modifier = Modifier,
        accent: Color = Color(0xFF66E3FF)
) {
        Row(
                modifier =
                        modifier.background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                Column {
                        Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.54f),
                                fontSize = 11.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1
                        )
                        Text(
                                text = value,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

@Composable
private fun DashboardReadinessStrip(readyState: String) {
        val ready = readyState == "1" || readyState.equals("true", ignoreCase = true)
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(46.dp)
                                .background(
                                        if (ready) Color(0xFF78E08F).copy(alpha = 0.13f)
                                        else Color.White.copy(alpha = 0.07f),
                                        RoundedCornerShape(8.dp)
                                )
                                .border(
                                        1.dp,
                                        if (ready) Color(0xFF78E08F).copy(alpha = 0.4f)
                                        else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(
                        text = if (ready) "READY" else "STANDBY",
                        color = if (ready) Color(0xFF78E08F) else Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontFamily = DashboardReadableFont,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = "Display 0",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = DashboardReadableFont
                )
        }
}

@Composable
private fun DashboardLinearMeter(
        label: String,
        value: String,
        fraction: Float,
        accent: Color,
        icon: ImageVector,
        modifier: Modifier = Modifier
) {
        Column(
                        modifier =
                                modifier.background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                                Text(
                                        text = label,
                                        color = Color.White.copy(alpha = 0.58f),
                                        fontSize = 12.sp,
                                        fontFamily = DashboardReadableFont
                                )
                        }
                        Text(text = value, color = Color.White, fontSize = 15.sp, fontFamily = DashboardReadableFont)
                }
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(9.dp)
                                        .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(4.dp))
                ) {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .background(accent, RoundedCornerShape(4.dp))
                        )
                }
        }
}

@Composable
private fun DashboardMetricTile(
        label: String,
        value: String,
        icon: ImageVector,
        modifier: Modifier = Modifier
) {
        Row(
                modifier =
                        modifier.background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF66E3FF), modifier = Modifier.size(19.dp))
                Column {
                        Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.52f),
                                fontSize = 9.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1
                        )
                        Text(
                                text = value,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

@Composable
private fun DashboardQuickCycleControl(
        label: String,
        value: String,
        icon: ImageVector,
        accent: Color,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
        Surface(
                onClick = onClick,
                modifier = modifier.fillMaxWidth(),
                color = accent.copy(alpha = 0.13f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
        ) {
                Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        Box(
                                modifier =
                                        Modifier.size(58.dp)
                                                .background(
                                                        accent.copy(alpha = 0.18f),
                                                        RoundedCornerShape(8.dp)
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(30.dp)
                                )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = label,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        fontFamily = DashboardReadableFont,
                                        maxLines = 1
                                )
                                Text(
                                        text = value,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontFamily = DashboardReadableFont,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                        }
                }
        }
}

@Composable
private fun HevModeDialog(
        snapshot: DashboardVehicleSnapshot,
        serviceManager: ServiceManager,
        onDismiss: () -> Unit
) {
        // Estado LOCAL do sub-modo p/ refletir o toque NA HORA (updateData não empurra de volta em
        // tempo real; antes só aparecia ao reabrir a barra). LaunchedEffect re-sincroniza se o valor
        // real do carro mudar por fora.
        var reserve by remember { mutableStateOf(snapshot.powerReserve) }
        LaunchedEffect(snapshot.powerReserve) { reserve = snapshot.powerReserve }
        val pct = snapshot.socTarget.trim().toIntOrNull()?.coerceIn(20, 80) ?: 50
        var dragging by remember { mutableStateOf(false) }
        var sliderPos by remember { mutableFloatStateOf(pct.toFloat()) }
        LaunchedEffect(pct) { if (!dragging) sliderPos = pct.toFloat() }
        AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color(0xFF161B24),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                confirmButton = {
                        TextButton(onClick = onDismiss) {
                                Text("Fechar", color = Color(0xFF78E08F))
                        }
                },
                title = { Text("Modo HEV") },
                text = {
                        Column {
                                SettingsCategoryRow(
                                        "Reserva de bateria",
                                        reserve,
                                        listOf("1" to "Inteligente", "2" to "Prioritário")
                                ) { newVal ->
                                        reserve = newVal // otimista: popup reflete o toque na hora
                                        // updateDataOptimistic ecoa pros listeners -> o CARD da barra
                                        // (que lê snapshot.powerReserve) também atualiza em tempo real.
                                        serviceManager.updateDataOptimistic(
                                                CarConstants.CAR_EV_SETTING_POWER_RESERVE_CONFIG
                                                        .getValue(),
                                                newVal
                                        )
                                }
                                if (reserve.trim() == "2") {
                                        Spacer(Modifier.height(14.dp))
                                        Text(
                                                text = "Bateria a manter: ${sliderPos.toInt()}%",
                                                style =
                                                        labelStyle.copy(
                                                                fontWeight = FontWeight.Bold
                                                        )
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Slider(
                                                value = sliderPos,
                                                onValueChange = { v ->
                                                        dragging = true
                                                        sliderPos = v
                                                },
                                                onValueChangeFinished = {
                                                        dragging = false
                                                        serviceManager.setHevSocTargetValue(
                                                                sliderPos.toInt()
                                                        )
                                                },
                                                valueRange = 20f..80f,
                                                steps = 11
                                        )
                                }
                        }
                }
        )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardPremiumCycleControl(
        label: String,
        value: String,
        nextValue: String,
        icon: ImageVector,
        accent: Color,
        modifier: Modifier = Modifier,
        hint: String? = null,
        hideNext: Boolean = false,
        onLongClick: (() -> Unit)? = null,
        onClick: () -> Unit
) {
        Surface(
                modifier =
                        modifier.fillMaxHeight()
                                .combinedClickable(
                                        onClick = onClick,
                                        onLongClick = onLongClick
                                ),
                color = Color.White.copy(alpha = 0.052f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
        ) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                Brush.linearGradient(
                                                        colors =
                                                                listOf(
                                                                        accent.copy(alpha = 0.13f),
                                                                        Color.Transparent,
                                                                        Color.Black.copy(alpha = 0.10f)
                                                                ),
                                                        start = Offset.Zero,
                                                        end = Offset(420f, 180f)
                                                )
                                        )
                                        .padding(14.dp)
                ) {
                        Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Box(
                                                modifier =
                                                        Modifier.size(42.dp)
                                                                .background(
                                                                        accent.copy(alpha = 0.18f),
                                                                        RoundedCornerShape(8.dp)
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Icon(
                                                        icon,
                                                        contentDescription = null,
                                                        tint = accent,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                        }
                                        if (hint != null) {
                                                Text(
                                                        text = hint,
                                                        color = Color.White.copy(alpha = 0.5f),
                                                        fontSize = 10.sp,
                                                        lineHeight = 12.sp,
                                                        fontFamily = DashboardReadableFont,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign =
                                                                androidx.compose.ui.text.style
                                                                        .TextAlign.End,
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .padding(start = 8.dp)
                                                )
                                        }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                                text = label,
                                                color = Color.White.copy(alpha = 0.62f),
                                                fontSize = 12.sp,
                                                fontFamily = DashboardReadableFont,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                                text = value,
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontFamily = DashboardReadableFont,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        if (!hideNext) {
                                                Text(
                                                        text = "Prox. $nextValue",
                                                        color = accent.copy(alpha = 0.86f),
                                                        fontSize = 11.sp,
                                                        fontFamily = DashboardReadableFont,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                        } else {
                                                // Placeholder invisível: mantém o valor na mesma
                                                // altura dos outros cards quando o One Pedal liga.
                                                Text(
                                                        text = " ",
                                                        fontSize = 11.sp,
                                                        fontFamily = DashboardReadableFont,
                                                        maxLines = 1
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun DashboardOptionGroup(
        label: String,
        currentValue: String,
        options: List<Pair<String, String>>,
        columns: Int,
        modifier: Modifier = Modifier,
        onSelect: (String) -> Unit
) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.58f),
                        fontSize = 10.sp,
                        fontFamily = DashboardReadableFont
                )
                options.chunked(columns).forEach { rowOptions ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowOptions.forEach { (value, text) ->
                                        val selected = currentValue == value
                                        Surface(
                                                onClick = { onSelect(value) },
                                                modifier = Modifier.weight(1f).height(34.dp),
                                                color =
                                                        if (selected)
                                                                Color(0xFF66E3FF).copy(alpha = 0.2f)
                                                        else Color.White.copy(alpha = 0.06f),
                                                shape = RoundedCornerShape(8.dp),
                                                border =
                                                        BorderStroke(
                                                                1.dp,
                                                                if (selected) Color(0xFF66E3FF)
                                                                else Color.White.copy(alpha = 0.08f)
                                                        )
                                        ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                                text = text,
                                                                color =
                                                                        if (selected)
                                                                                Color(0xFF66E3FF)
                                                                        else Color.White,
                                                                fontSize = 10.sp,
                                                                fontFamily = DashboardReadableFont,
                                                                textAlign = TextAlign.Center,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                        )
                                                }
                                        }
                                }
                                repeat(columns - rowOptions.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                }
                        }
                }
        }
}

@Composable
private fun DashboardIconButton(
        icon: ImageVector,
        size: Dp = 58.dp,
        contentDescription: String? = null,
        onClick: () -> Unit
) {
        Surface(
                onClick = onClick,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.size(size)
        ) {
                Box(contentAlignment = Alignment.Center) {
                        Icon(
                                icon,
                                contentDescription = contentDescription,
                                tint = Color.White,
                                modifier = Modifier.size((size.value * 0.48f).dp)
                        )
                }
        }
}

@Composable
private fun DashboardActionButton(
        text: String,
        icon: ImageVector,
        enabled: Boolean = true,
        modifier: Modifier = Modifier.fillMaxWidth(),
        height: Dp = 58.dp,
        onClick: () -> Unit
) {
        Surface(
                onClick = onClick,
                enabled = enabled,
                color =
                        if (enabled) Color.White.copy(alpha = 0.07f)
                        else Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(8.dp),
                border =
                        BorderStroke(
                                1.dp,
                                if (enabled) Color.White.copy(alpha = 0.12f)
                                else Color.White.copy(alpha = 0.05f)
                        ),
                modifier = modifier.height(height)
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Icon(
                                icon,
                                contentDescription = null,
                                tint = if (enabled) Color(0xFF66E3FF) else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                        )
                        Text(
                                text = text,
                                color = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
                                fontSize = 14.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
        }
}

@Composable
private fun DashboardTempAdjuster(
        label: String,
        temp: String,
        enabled: Boolean,
        modifier: Modifier = Modifier,
        onDelta: (Float) -> Unit
) {
        Row(
                modifier =
                        modifier.alpha(if (enabled) 1f else 0.45f)
                                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(8.dp))
                                .height(92.dp)
                                .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                DashboardIconButton(Icons.Default.Remove, size = 62.dp) {
                        if (enabled) onDelta(-0.5f)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                                text = label,
                                color = Color.White.copy(alpha = 0.58f),
                                fontSize = 12.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1
                        )
                        Text(
                                text = if (enabled) formatTemperature(temp) else "--",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1
                        )
                }
                DashboardIconButton(Icons.Default.Add, size = 62.dp) {
                        if (enabled) onDelta(0.5f)
                }
        }
}

@Composable
private fun DashboardFanAdjuster(
        speed: String,
        enabled: Boolean,
        modifier: Modifier = Modifier,
        onDelta: (Int) -> Unit
) {
        Row(
                modifier =
                        modifier.alpha(if (enabled) 1f else 0.45f)
                                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(8.dp))
                                .height(84.dp)
                                .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                DashboardIconButton(Icons.Default.Remove, size = 62.dp) { if (enabled) onDelta(-1) }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                                text = "Ventilação",
                                color = Color.White.copy(alpha = 0.58f),
                                fontSize = 12.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1
                        )
                        Text(
                                text = speed.toIntOrNull()?.toString() ?: "--",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1
                        )
                }
                DashboardIconButton(Icons.Default.Add, size = 62.dp) { if (enabled) onDelta(1) }
        }
}

private data class DashboardAirflowModeOption(
        val value: String,
        val label: String,
        val iconRes: Int
)

private val DashboardAirflowModeOptions =
        listOf(
                DashboardAirflowModeOption("2", "Pés", R.drawable.ic_hvac_blower_feet),
                DashboardAirflowModeOption("1", "Rosto/Pés", R.drawable.ic_hvac_blower_feet_and_face),
                DashboardAirflowModeOption("0", "Rosto", R.drawable.ic_hvac_blower_face),
                DashboardAirflowModeOption("3", "Vidro/Pés", R.drawable.ic_hvac_blower_feet_and_defrost)
        )

@Composable
private fun DashboardAirflowModeSelector(
        mode: String,
        enabled: Boolean,
        modifier: Modifier = Modifier,
        onSelect: (String) -> Unit
) {
        Row(
                modifier = modifier.alpha(if (enabled) 1f else 0.45f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                DashboardAirflowModeOptions.forEach { option ->
                        val active = mode == option.value
                        Surface(
                                onClick = { onSelect(option.value) },
                                enabled = enabled,
                                modifier = Modifier.weight(1f).height(82.dp),
                                color =
                                        if (active && enabled) Color(0xFF66E3FF).copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.055f),
                                shape = RoundedCornerShape(8.dp),
                                border =
                                        BorderStroke(
                                                1.dp,
                                                if (active && enabled)
                                                        Color(0xFF66E3FF).copy(alpha = 0.55f)
                                                else Color.White.copy(alpha = 0.08f)
                                        )
                        ) {
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                ) {
                                        Icon(
                                                painter = painterResource(option.iconRes),
                                                contentDescription = null,
                                                tint =
                                                        if (active && enabled) Color(0xFF66E3FF)
                                                        else Color.White.copy(
                                                                alpha = if (enabled) 0.82f else 0.35f
                                                        ),
                                                modifier = Modifier.size(46.dp)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                                text = option.label,
                                                color =
                                                        if (active && enabled) Color(0xFF66E3FF)
                                                        else Color.White.copy(
                                                                alpha = if (enabled) 0.76f else 0.35f
                                                        ),
                                                fontSize = 11.sp,
                                                fontFamily = DashboardReadableFont,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun DashboardSeatVentilationButton(
        label: String,
        level: String,
        maxLevel: String,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
        val parsedLevel = parseSeatVentilationLevel(level, maxLevel)
        val active = parsedLevel > 0
        Surface(
                onClick = onClick,
                modifier = modifier.height(82.dp),
                color =
                        if (active) Color(0xFF78E08F).copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.055f),
                shape = RoundedCornerShape(8.dp),
                border =
                        BorderStroke(
                                1.dp,
                                if (active) Color(0xFF78E08F).copy(alpha = 0.42f)
                                else Color.White.copy(alpha = 0.1f)
                        )
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Icon(
                                Icons.Default.EventSeat,
                                contentDescription = null,
                                tint =
                                        if (active) Color(0xFF78E08F)
                                        else Color.White.copy(alpha = 0.62f),
                                modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = label,
                                        color = Color.White.copy(alpha = 0.58f),
                                        fontSize = 13.sp,
                                        fontFamily = DashboardReadableFont,
                                        maxLines = 1
                                )
                                Text(
                                        text = if (active) "Nível $parsedLevel" else "Desligado",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontFamily = DashboardReadableFont,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                        }
                        DashboardSeatVentilationLevelIndicator(
                                level = parsedLevel,
                                maxLevel = parseSeatVentilationMaxLevel(maxLevel),
                                active = active
                        )
                }
        }
}

@Composable
private fun DashboardSeatVentilationLevelIndicator(level: Int, maxLevel: Int, active: Boolean) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                repeat(maxLevel.coerceIn(1, 3)) { index ->
                        val step = index + 1
                        val isFilled = active && step <= level
                        Box(
                                modifier =
                                        Modifier.width(5.dp)
                                                .height((10 + index * 5).dp)
                                                .background(
                                                        if (isFilled) Color(0xFF78E08F)
                                                        else Color.White.copy(alpha = 0.16f),
                                                        RoundedCornerShape(99.dp)
                                                )
                        )
                }
        }
}

@Composable
private fun DashboardToggleButton(
        label: String,
        icon: ImageVector,
        active: Boolean,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
        Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier.height(82.dp),
                color =
                        if (active && enabled) Color(0xFF66E3FF).copy(alpha = 0.16f)
                        else Color.White.copy(alpha = 0.055f),
                shape = RoundedCornerShape(8.dp),
                border =
                        BorderStroke(
                                1.dp,
                                if (active && enabled) Color(0xFF66E3FF).copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.08f)
                        )
        ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        Icon(
                                icon,
                                contentDescription = null,
                                tint =
                                        if (active && enabled) Color(0xFF66E3FF)
                                        else Color.White.copy(alpha = if (enabled) 0.82f else 0.35f),
                                modifier = Modifier.size(31.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                                text = label,
                                color =
                                        if (active && enabled) Color(0xFF66E3FF)
                                        else Color.White.copy(alpha = if (enabled) 0.76f else 0.35f),
                                fontSize = 12.sp,
                                fontFamily = DashboardReadableFont,
                                maxLines = 1
                        )
                }
        }
}

@Composable
private fun DashboardTinyReadout(label: String, value: String, modifier: Modifier = Modifier) {
        Row(
                modifier =
                        modifier.fillMaxWidth()
                                .height(40.dp)
                                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 10.sp,
                        fontFamily = DashboardReadableFont
                )
                Text(text = value, color = Color.White, fontSize = 12.sp, fontFamily = DashboardReadableFont)
        }
}

private fun updateTemperature(
        serviceManager: ServiceManager,
        key: CarConstants,
        currentValue: String,
        delta: Float
) {
        val current = currentValue.toFloatOrNull() ?: 22.0f
        val next = (current + delta).coerceIn(16.0f, 32.0f)
        val nextValue = String.format(java.util.Locale.US, "%.1f", next)
        serviceManager.updateDataOptimistic(key.getValue(), nextValue)
}

internal fun resolveDashboardMediaVolumeAfterDelta(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(DASHBOARD_MEDIA_VOLUME_MIN, DASHBOARD_MEDIA_VOLUME_MAX)
}

private fun adjustDashboardSystemMediaVolume(context: Context, delta: Int) {
        val direction =
                if (delta > 0) {
                        AudioManager.ADJUST_RAISE
                } else {
                        AudioManager.ADJUST_LOWER
                }
        try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        } catch (e: Exception) {
                Log.w(BOTTOM_BAR_TAG, "Unable to adjust system media volume", e)
        }
}

private fun updateSeatVentilationLevel(
        serviceManager: ServiceManager,
        key: CarConstants,
        nextLevel: String
) {
        serviceManager.updateDataOptimistic(key.getValue(), nextLevel)
}
