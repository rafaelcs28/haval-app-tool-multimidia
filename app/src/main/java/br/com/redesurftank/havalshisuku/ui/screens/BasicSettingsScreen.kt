package br.com.redesurftank.havalshisuku.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.ambientlight.AmbientLightService
import br.com.redesurftank.havalshisuku.managers.AutoBrightnessManager
import br.com.redesurftank.havalshisuku.managers.HotRouterManager
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceReminder
import br.com.redesurftank.havalshisuku.managers.ServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import br.com.redesurftank.havalshisuku.models.BottomBarState
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys
import br.com.redesurftank.havalshisuku.models.SteeringWheelClimateCommandType
import br.com.redesurftank.havalshisuku.models.SteeringWheelCustomActionType
import br.com.redesurftank.havalshisuku.ui.components.AppColors
import br.com.redesurftank.havalshisuku.ui.components.GroupedSettingsLayout
import br.com.redesurftank.havalshisuku.ui.components.SettingItem
import br.com.redesurftank.havalshisuku.ui.components.SettingsGroups

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicSettingsTab() {
        val context = LocalContext.current
        val prefs =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        var isAdvancedUse by remember {
                mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.ADVANCE_USE.key, false))
        }
        var selfInstallationCheck by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.SELF_INSTALLATION_INTEGRITY_CHECK.key,
                                false
                        )
                )
        }
        var bypassSelfInstallationCheck by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK.key,
                                false
                        )
                )
        }
        var disableMonitoring by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.DISABLE_MONITORING.key, false)
                )
        }
        var disableAvas by remember {
                mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.DISABLE_AVAS.key, false))
        }
        var disableAvmCarStopped by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.DISABLE_AVM_CAR_STOPPED.key, false)
                )
        }
        var enableSeatbeltVoice by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_SEATBELT_VOICE.key, true)
                )
        }
        var seatbeltMinVol by remember {
                mutableStateOf(
                        prefs.getInt(
                                SharedPreferencesKeys.SEATBELT_VOICE_MIN_VOLUME_PCT.key,
                                SeatbeltVoiceReminder.DEFAULT_MIN_VOLUME_PCT
                        ).toFloat()
                )
        }
        var closeWindowOnPowerOff by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.CLOSE_WINDOW_ON_POWER_OFF.key, false)
                )
        }
        var closeWindowOnFoldMirror by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.CLOSE_WINDOW_ON_FOLD_MIRROR.key,
                                false
                        )
                )
        }
        var closeSunroofOnPowerOff by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.CLOSE_SUNROOF_ON_POWER_OFF.key,
                                false
                        )
                )
        }
        var closeSunroofOnFoldMirror by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.CLOSE_SUNROOF_ON_FOLD_MIRROR.key,
                                false
                        )
                )
        }
        var disableBluetoothOnFoldMirror by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_FOLD_MIRROR.key,
                                false
                        )
                )
        }
        var disableHotspotOnFoldMirror by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.DISABLE_HOTSPOT_ON_FOLD_MIRROR.key,
                                false
                        )
                )
        }
        var closeSunroofSunShadeOnCloseSunroof by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF.key,
                                false
                        )
                )
        }
        var setStartupVolume by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.SET_STARTUP_VOLUME.key, false)
                )
        }
        var volume by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.STARTUP_VOLUME.key, 1))
        }
        var closeWindowsOnSpeed by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED.key, false)
                )
        }
        var closeSunroofOnSpeed by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED.key, false)
                )
        }
        var speedThreshold by remember {
                mutableFloatStateOf(prefs.getFloat(SharedPreferencesKeys.SPEED_THRESHOLD.key, 15f))
        }
        var closeSunroofSpeedThreshold by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.SUNROOF_SPEED_THRESHOLD.key, 15f)
                )
        }
        var enableMaxAcOnUnlock by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.key, false)
                )
        }
        var maxAcOnUnlockThreshold by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.MAX_AC_ON_UNLOCK_THRESHOLD.key, 34f)
                )
        }
        var maxAcTargetTemp by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.MAX_AC_TARGET_TEMP.key, 28f)
                )
        }
        var maxAcTimeout by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.MAX_AC_TIMEOUT.key, 0))
        }
        var enableAutoBrightness by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_AUTO_BRIGHTNESS.key, false)
                )
        }
        var autoBrightnessUseSun by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.AUTO_BRIGHTNESS_USE_SUN.key, false)
                )
        }
        var nightStartHour by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_START_HOUR.key, 20))
        }
        var nightStartMinute by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_START_MINUTE.key, 0))
        }
        var nightEndHour by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_END_HOUR.key, 6))
        }
        var nightEndMinute by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.NIGHT_END_MINUTE.key, 0))
        }
        var disableBluetoothOnPowerOff by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF.key,
                                false
                        )
                )
        }
        var disableHotspotOnPowerOff by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF.key,
                                false
                        )
                )
        }
        var ambientLightBleEnabled by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.AMBIENT_LIGHT_BLE_ENABLED.key, false)
                )
        }
        var nightBrightnessLevel by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.AUTO_BRIGHTNESS_LEVEL_NIGHT.key, 1)
                )
        }
        var dayBrightnessLevel by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.AUTO_BRIGHTNESS_LEVEL_DAY.key, 10)
                )
        }
        var enableSeatVentilationOnAcOn by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON.key,
                                false
                        )
                )
        }
        var enablePassengerSeatVentilationOnAcOn by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys
                                        .ENABLE_PASSENGER_SEAT_VENTILATION_ON_AC_ON
                                        .key,
                                false
                        )
                )
        }
        var passengerPresent by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.PASSENGER_PRESENT.key, false)
                )
        }
        var enableCustomSteeringWheelButtons by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS.key,
                                false
                        )
                )
        }
        var steeringWheelButton1Action by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION.key,
                                SteeringWheelCustomActionType.DEFAULT.key
                        )
                                ?: SteeringWheelCustomActionType.DEFAULT.key
                )
        }
        var steeringWheelButton2Action by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION.key,
                                SteeringWheelCustomActionType.DEFAULT.key
                        )
                                ?: SteeringWheelCustomActionType.DEFAULT.key
                )
        }
        var steeringWheelButton1Package by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1.key,
                                ""
                        )
                                ?: ""
                )
        }
        var steeringWheelButton2Package by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2.key,
                                ""
                        )
                                ?: ""
                )
        }
        var steeringWheelButton1ClimateCommand by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1.key,
                                SteeringWheelClimateCommandType.TOGGLE_AC.key
                        )
                                ?: SteeringWheelClimateCommandType.TOGGLE_AC.key
                )
        }
        var steeringWheelButton2ClimateCommand by remember {
                mutableStateOf(
                        prefs.getString(
                                SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2.key,
                                SteeringWheelClimateCommandType.TOGGLE_AC.key
                        )
                                ?: SteeringWheelClimateCommandType.TOGGLE_AC.key
                )
        }
        DisposableEffect(prefs) {
                val listener =
                        SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
                                when (key) {
                                        SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS
                                                .key ->
                                                enableCustomSteeringWheelButtons =
                                                        sharedPrefs.getBoolean(
                                                                SharedPreferencesKeys
                                                                        .ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS
                                                                        .key,
                                                                false
                                                        )
                                        SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION
                                                .key ->
                                                steeringWheelButton1Action =
                                                        sharedPrefs.getString(
                                                                SharedPreferencesKeys
                                                                        .STEERING_WHEEL_CUSTOM_BUTON_1_ACTION
                                                                        .key,
                                                                SteeringWheelCustomActionType.DEFAULT
                                                                        .key
                                                        )
                                                                ?: SteeringWheelCustomActionType
                                                                        .DEFAULT
                                                                        .key
                                        SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION
                                                .key ->
                                                steeringWheelButton2Action =
                                                        sharedPrefs.getString(
                                                                SharedPreferencesKeys
                                                                        .STEERING_WHEEL_CUSTOM_BUTON_2_ACTION
                                                                        .key,
                                                                SteeringWheelCustomActionType.DEFAULT
                                                                        .key
                                                        )
                                                                ?: SteeringWheelCustomActionType
                                                                        .DEFAULT
                                                                        .key
                                        SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1
                                                .key ->
                                                steeringWheelButton1Package =
                                                        sharedPrefs.getString(
                                                                SharedPreferencesKeys
                                                                        .STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1
                                                                        .key,
                                                                ""
                                                        )
                                                                ?: ""
                                        SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2
                                                .key ->
                                                steeringWheelButton2Package =
                                                        sharedPrefs.getString(
                                                                SharedPreferencesKeys
                                                                        .STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2
                                                                        .key,
                                                                ""
                                                        )
                                                                ?: ""
                                        SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1
                                                .key ->
                                                steeringWheelButton1ClimateCommand =
                                                        sharedPrefs.getString(
                                                                SharedPreferencesKeys
                                                                        .STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1
                                                                        .key,
                                                                SteeringWheelClimateCommandType
                                                                        .TOGGLE_AC
                                                                        .key
                                                        )
                                                                ?: SteeringWheelClimateCommandType
                                                                        .TOGGLE_AC
                                                                        .key
                                        SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2
                                                .key ->
                                                steeringWheelButton2ClimateCommand =
                                                        sharedPrefs.getString(
                                                                SharedPreferencesKeys
                                                                        .STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2
                                                                        .key,
                                                                SteeringWheelClimateCommandType
                                                                        .TOGGLE_AC
                                                                        .key
                                                        )
                                                                ?: SteeringWheelClimateCommandType
                                                                        .TOGGLE_AC
                                                                        .key
                                }
                        }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        var enablePersistentBottomBar by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.PERSISTENT_BOTTOM_BAR.key, false)
                )
        }
        var autoHideEnabled by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.BOTTOM_BAR_AUTO_HIDE.key, false)
                )
        }
        var barHiddenEnabled by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.BOTTOM_BAR_HIDDEN.key, false)
                )
        }
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }
        var enableSpeedAdjustment by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_SPEED_ADJUSTMENT.key, false)
                )
        }
        var speedAdjustmentOffset by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.SPEED_ADJUSTMENT_OFFSET.key, 0f)
                )
        }
        var hideClusterSpeedDuringProjection by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.HIDE_CLUSTER_SPEED_DURING_PROJECTION.key,
                                false
                        )
                )
        }

        var enableOpenSunroofCurtainOnStart by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.ENABLE_OPEN_SUNROOF_CURTAIN_ON_START.key,
                                false
                        )
                )
        }
        var curtainStartHour by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_START_HOUR.key, 18)
                )
        }
        var curtainStartMinute by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_START_MINUTE.key, 0)
                )
        }
        var curtainEndHour by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_END_HOUR.key, 9)
                )
        }
        var curtainEndMinute by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_END_MINUTE.key, 0)
                )
        }
        var openSunroofCurtainMaxTemp by remember {
                mutableFloatStateOf(
                        prefs.getFloat(SharedPreferencesKeys.OPEN_SUNROOF_CURTAIN_MAX_TEMP.key, -1f)
                )
        }
        var enablePersistHevSoc by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_PERSIST_HEV_SOC_TARGET.key, false)
                )
        }
        var hevSocTarget by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.HEV_SOC_TARGET_VALUE.key, 50)
                )
        }
        var enableAaClusterOffset by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_AA_CLUSTER_OFFSET.key, false)
                )
        }
        var aaClusterOffset by remember {
                mutableIntStateOf(
                        prefs.getInt(SharedPreferencesKeys.AA_CLUSTER_LEFT_OFFSET.key, 145)
                )
        }
        var enableHotRouter by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.ENABLE_HOT_ROUTER.key, false)
                )
        }
        var wifiPriorityEnabled by remember {
                mutableStateOf(
                        prefs.getBoolean(SharedPreferencesKeys.WIFI_PRIORITY_ENABLED.key, false)
                )
        }
        var dashboardAutoOpen by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.DASHBOARD_AUTO_OPEN_ON_PROJECTION.key,
                                false
                        )
                )
        }

        // ===== Controle de dados móveis do carro (master + regras) =====
        val mdm = br.com.redesurftank.havalshisuku.managers.MobileDataManager
        var mobileControlEnabled by remember { mutableStateOf(mdm.isControlEnabled()) }
        var mobileManualBlock by remember { mutableStateOf(mdm.isManualBlock()) }
        var mobileAutoblock by remember { mutableStateOf(mdm.isAutoblockEnabled()) }
        var mobileBlockOnWifi by remember { mutableStateOf(mdm.isBlockOnWifi()) }
        var mobileBlockOnProjection by remember { mutableStateOf(mdm.isBlockOnProjection()) }
        var mobileDataCycleDay by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.MOBILE_DATA_CYCLE_DAY.key, 1).coerceIn(1, 31))
        }
        var mobileDataAutoblockCapMb by remember {
                mutableIntStateOf(prefs.getInt(SharedPreferencesKeys.MOBILE_DATA_AUTOBLOCK_CAP_MB.key, 2048).coerceIn(512, 8192))
        }
        var mobileDataUsedMb by remember { mutableStateOf(0L) }
        var mobileBlockReason by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(mobileControlEnabled, mobileDataCycleDay) {
                while (true) {
                        val ctx = br.com.redesurftank.App.getContext()
                        mobileDataUsedMb = withContext(Dispatchers.IO) {
                                mdm.getMobileUsedMbThisCycle(ctx, System.currentTimeMillis())
                        }
                        mobileBlockReason = withContext(Dispatchers.IO) { mdm.blockReason(ctx) }
                        delay(10000)
                }
        }

        // A tela OBSERVA a SharedPreferences (fonte de verdade): um comando remoto (provider.call do
        // EcoTrip) grava a pref e este listener recarrega o estado, então os toggles/limite refletem a
        // mudança NA HORA, sem sair e voltar da tela. Sem isto, a UI mostrava o valor antigo enquanto a
        // lógica já obedecia o novo (a tela "mentia"). Todos os setters gravam pref -> este listener pega.
        val connScope = rememberCoroutineScope()
        DisposableEffect(prefs) {
                val listener =
                        SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                                when (key) {
                                        SharedPreferencesKeys.WIFI_PRIORITY_ENABLED.key ->
                                                wifiPriorityEnabled = sp.getBoolean(SharedPreferencesKeys.WIFI_PRIORITY_ENABLED.key, false)
                                        SharedPreferencesKeys.MOBILE_DATA_CONTROL_ENABLED.key ->
                                                mobileControlEnabled = mdm.isControlEnabled()
                                        SharedPreferencesKeys.BLOCK_CAR_MOBILE_DATA.key ->
                                                mobileManualBlock = mdm.isManualBlock()
                                        SharedPreferencesKeys.MOBILE_DATA_AUTOBLOCK.key ->
                                                mobileAutoblock = mdm.isAutoblockEnabled()
                                        SharedPreferencesKeys.MOBILE_DATA_BLOCK_ON_WIFI.key ->
                                                mobileBlockOnWifi = mdm.isBlockOnWifi()
                                        SharedPreferencesKeys.MOBILE_DATA_BLOCK_ON_PROJECTION.key ->
                                                mobileBlockOnProjection = mdm.isBlockOnProjection()
                                        SharedPreferencesKeys.MOBILE_DATA_CYCLE_DAY.key ->
                                                mobileDataCycleDay = sp.getInt(SharedPreferencesKeys.MOBILE_DATA_CYCLE_DAY.key, 1).coerceIn(1, 31)
                                        SharedPreferencesKeys.MOBILE_DATA_AUTOBLOCK_CAP_MB.key ->
                                                mobileDataAutoblockCapMb = sp.getInt(SharedPreferencesKeys.MOBILE_DATA_AUTOBLOCK_CAP_MB.key, 2048).coerceIn(512, 8192)
                                        else -> return@OnSharedPreferenceChangeListener
                                }
                                // uma regra mudou -> o cabeçalho "4G agora:" pode mudar: recomputa o motivo na hora
                                connScope.launch {
                                        val ctx = br.com.redesurftank.App.getContext()
                                        mobileBlockReason = withContext(Dispatchers.IO) { mdm.blockReason(ctx) }
                                }
                        }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

        var hideLeftNavPane by remember {
                mutableStateOf(prefs.getBoolean(SharedPreferencesKeys.HIDE_LEFT_NAV_PANE.key, false))
        }
        var autoMoveProjectionToCluster by remember {
                mutableStateOf(
                        prefs.getBoolean(
                                SharedPreferencesKeys.AUTO_MOVE_PROJECTION_TO_CLUSTER.key,
                                true
                        )
                )
        }
        var swipeUpCustomApp by remember {
                mutableStateOf(
                        prefs.getString(SharedPreferencesKeys.BOTTOM_BAR_SWIPE_UP_ACTION.key, null) ==
                                br.com.redesurftank.havalshisuku.models.BottomBarState.SwipeUpAction
                                        .CUSTOM_APP.key
                )
        }
        var swipeUpPackage by remember {
                mutableStateOf(
                        prefs.getString(SharedPreferencesKeys.BOTTOM_BAR_SWIPE_UP_PACKAGE.key, "")
                                ?: ""
                )
        }

        val settingsList = mutableListOf<SettingItem>()

        // Card MÃE — controle de dados móveis (master). As 4 regras só aparecem/valem com ele ligado.
        settingsList.add(
                SettingItem(
                        title = "Controle de dados móveis",
                        description =
                                "Liga o gerenciamento do 4G da multimídia. Com ele ligado, escolha abaixo QUANDO cortar o 4G. Desligado = 4G livre (nada é bloqueado).",
                        group = SettingsGroups.FEATURES,
                        checked = mobileControlEnabled,
                        onCheckedChange = {
                                mobileControlEnabled = it
                                mdm.setControlEnabled(it)
                        },
                        customContent = {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        val reason = mobileBlockReason
                                        Text(
                                                if (reason != null) "4G agora: BLOQUEADO ($reason)" else "4G agora: LIBERADO",
                                                color = if (reason != null) androidx.compose.ui.graphics.Color(0xFFE53935) else androidx.compose.ui.graphics.Color(0xFF34C759),
                                                fontSize = 15.sp
                                        )
                                        Text(
                                                String.format("Multimídia (esta tela): %.2f GB neste ciclo", mobileDataUsedMb / 1024f),
                                                color = AppColors.TextSecondary,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                        )

                                        // (a) manual
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                                Text("Bloquear manualmente agora", color = AppColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                                androidx.compose.material3.Switch(
                                                        checked = mobileManualBlock,
                                                        onCheckedChange = { mobileManualBlock = it; mdm.setManualBlock(it) }
                                                )
                                        }
                                        Text("Corta o 4G na hora, independente do resto.", color = AppColors.TextSecondary, fontSize = 12.sp)

                                        // (b) por consumo
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                                Text("Bloquear conforme o gasto", color = AppColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                                androidx.compose.material3.Switch(
                                                        checked = mobileAutoblock,
                                                        onCheckedChange = { mobileAutoblock = it; mdm.setAutoblockEnabled(it) }
                                                )
                                        }
                                        if (mobileAutoblock) {
                                                val usedGb = mobileDataUsedMb / 1024f
                                                val capGb = mobileDataAutoblockCapMb / 1024f
                                                val remainingGb = (capGb - usedGb).coerceAtLeast(0f)
                                                Text(
                                                        String.format("Bloquear ao passar de %.1f GB — faltam %.2f GB", capGb, remainingGb),
                                                        color = AppColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                                                )
                                                Slider(
                                                        value = (mobileDataAutoblockCapMb / 1024f).coerceIn(0.5f, 8f),
                                                        onValueChange = {
                                                                val gb = Math.round(it * 2f) / 2f
                                                                mobileDataAutoblockCapMb = (gb * 1024).toInt().coerceIn(512, 8192)
                                                        },
                                                        onValueChangeFinished = {
                                                                prefs.edit { putInt(SharedPreferencesKeys.MOBILE_DATA_AUTOBLOCK_CAP_MB.key, mobileDataAutoblockCapMb) }
                                                        },
                                                        valueRange = 0.5f..8f,
                                                        steps = 14
                                                )
                                                Text("Zera a contagem no dia $mobileDataCycleDay do mês", color = AppColors.TextSecondary, fontSize = 12.sp)
                                                Slider(
                                                        value = mobileDataCycleDay.toFloat(),
                                                        onValueChange = { mobileDataCycleDay = it.toInt().coerceIn(1, 31) },
                                                        onValueChangeFinished = {
                                                                prefs.edit { putInt(SharedPreferencesKeys.MOBILE_DATA_CYCLE_DAY.key, mobileDataCycleDay) }
                                                        },
                                                        valueRange = 1f..31f,
                                                        steps = 29
                                                )
                                        } else {
                                                Text("Bloqueia sozinho só quando a tela atinge o teto (GB) no ciclo.", color = AppColors.TextSecondary, fontSize = 12.sp)
                                        }

                                        // (c) no WiFi
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                                Text("Bloquear quando conectado ao WiFi", color = AppColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                                androidx.compose.material3.Switch(
                                                        checked = mobileBlockOnWifi,
                                                        onCheckedChange = { mobileBlockOnWifi = it; mdm.setBlockOnWifi(it) }
                                                )
                                        }
                                        Text("Com WiFi/Starlink no ar, desliga o 4G; volta quando o WiFi cai.", color = AppColors.TextSecondary, fontSize = 12.sp)

                                        // (d) no Android Auto/CarPlay
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                                Text("Bloquear no Android Auto/CarPlay", color = AppColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                                androidx.compose.material3.Switch(
                                                        checked = mobileBlockOnProjection,
                                                        onCheckedChange = { mobileBlockOnProjection = it; mdm.setBlockOnProjection(it) }
                                                )
                                        }
                                        Text("Enquanto o celular estiver projetando, o 4G do carro fica desligado.", color = AppColors.TextSecondary, fontSize = 12.sp)

                                        Text(
                                                "A fatura da linha é maior: inclui o TBOX (não passa pela tela e não é medido aqui).",
                                                color = AppColors.TextSecondary,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 12.dp)
                                        )
                                }
                        }
                )
        )

        // Card — prioridade de redes WiFi (troca automática pela preferida disponível)
        settingsList.add(
                SettingItem(
                        title = "Prioridade de redes WiFi",
                        description =
                                "Quando uma rede de prioridade MAIOR aparece no alcance, o carro pula sozinho pra ela (resolve o \"gruda no hotspot\"). Só sobe de prioridade, com histerese; cada troca pisca o WiFi ~10s. Precisa da localização ligada (pro carro enxergar as redes ao redor).",
                        group = SettingsGroups.FEATURES,
                        checked = wifiPriorityEnabled,
                        onCheckedChange = {
                                wifiPriorityEnabled = it
                                // setFeatureEnabled: persiste a pref + aplica + avisa o EcoTrip (mudança
                                // local também sincroniza o outro app, pra ele não forçar o valor velho).
                                br.com.redesurftank.havalshisuku.managers.WifiPriorityManager
                                        .getInstance()
                                        .setFeatureEnabled(it)
                        },
                        customContent =
                                if (wifiPriorityEnabled) {
                                        { WifiPriorityEditor(prefs) }
                                } else null
                )
        )

        // Dashboard: não subir sozinho por cima dos apps na projeção (default OFF)
        settingsList.add(
                SettingItem(
                        title = "Abrir o dashboard sozinho na projeção",
                        description =
                                "Quando o Android Auto/CarPlay vai pro cluster, reabre o dashboard na tela principal. Desligado (padrão): a tela principal fica no app que você deixou; o dashboard só abre pelo atalho do volante. Desligado resolve o dashboard \"insistindo\" por cima dos apps no boot.",
                        group = SettingsGroups.DISPLAY,
                        checked = dashboardAutoOpen,
                        onCheckedChange = {
                                dashboardAutoOpen = it
                                prefs.edit {
                                        putBoolean(
                                                SharedPreferencesKeys
                                                        .DASHBOARD_AUTO_OPEN_ON_PROJECTION
                                                        .key,
                                                it
                                        )
                                }
                        }
                )
        )

        // Ocultar o painel lateral esquerdo (navegação do sistema) — via immersive policy_control (netseek).
        settingsList.add(
                SettingItem(
                        title = "Ocultar painel lateral esquerdo",
                        description =
                                "Esconde a navegação do sistema (o painel à esquerda) via modo imersivo, liberando a largura da tela. Reversível.",
                        group = SettingsGroups.DISPLAY,
                        checked = hideLeftNavPane,
                        onCheckedChange = {
                                hideLeftNavPane = it
                                prefs.edit {
                                        putBoolean(SharedPreferencesKeys.HIDE_LEFT_NAV_PANE.key, it)
                                }
                                applyLeftNavPaneVisibility(it)
                        }
                )
        )

        // Mover a projeção (AA/CarPlay) pro cluster automaticamente (netseek autoMove).
        settingsList.add(
                SettingItem(
                        title = "Mover projeção pro cluster automaticamente",
                        description =
                                "Ligado (padrão): o Android Auto / CarPlay pode ir pro painel do motorista (cluster). Desligado: a projeção fica só na tela central.",
                        group = SettingsGroups.DISPLAY,
                        checked = autoMoveProjectionToCluster,
                        onCheckedChange = {
                                autoMoveProjectionToCluster = it
                                prefs.edit {
                                        putBoolean(
                                                SharedPreferencesKeys
                                                        .AUTO_MOVE_PROJECTION_TO_CLUSTER.key,
                                                it
                                        )
                                }
                        }
                )
        )

        // Swipe-up na barra: default abre o Dashboard; ligado abre um app escolhido (netseek swipe-up).
        settingsList.add(
                SettingItem(
                        title = "Swipe-up na barra abre um app",
                        description =
                                "Desligado: deslizar a barra pra cima abre o Dashboard (padrão). Ligado: abre o app que você escolher abaixo.",
                        group = SettingsGroups.DISPLAY,
                        checked = swipeUpCustomApp,
                        onCheckedChange = {
                                swipeUpCustomApp = it
                                val action =
                                        if (it)
                                                br.com.redesurftank.havalshisuku.models
                                                        .BottomBarState.SwipeUpAction.CUSTOM_APP
                                        else
                                                br.com.redesurftank.havalshisuku.models
                                                        .BottomBarState.SwipeUpAction.DASHBOARD
                                prefs.edit {
                                        putString(
                                                SharedPreferencesKeys
                                                        .BOTTOM_BAR_SWIPE_UP_ACTION.key,
                                                action.key
                                        )
                                }
                                br.com.redesurftank.havalshisuku.models.BottomBarState
                                        .swipeUpAction = action.key
                        },
                        customContent = {
                                if (swipeUpCustomApp) {
                                        AppSelectorField(
                                                packageName = swipeUpPackage,
                                                onPackageSelected = { pkg ->
                                                        swipeUpPackage = pkg
                                                        prefs.edit {
                                                                putString(
                                                                        SharedPreferencesKeys
                                                                                .BOTTOM_BAR_SWIPE_UP_PACKAGE
                                                                                .key,
                                                                        pkg
                                                                )
                                                        }
                                                        br.com.redesurftank.havalshisuku.models
                                                                .BottomBarState.swipeUpPackage = pkg
                                                },
                                                label = "App a abrir no swipe-up"
                                        )
                                }
                        }
                )
        )

        settingsList.add(
                SettingItem(
                        title = "Manter % de bateria no HEV Prioritário",
                        description = "Se o carro alterar sozinho o % a salvar, o app reaplica o valor que você escolheu. Só vale em HEV Prioritário.",
                        group = SettingsGroups.DRIVE,
                        checked = enablePersistHevSoc,
                        onCheckedChange = {
                                enablePersistHevSoc = it
                                prefs.edit {
                                        putBoolean(SharedPreferencesKeys.ENABLE_PERSIST_HEV_SOC_TARGET.key, it)
                                }
                                if (it) ServiceManager.getInstance().applyHevSocTargetIfActive("UI_TOGGLE")
                        },
                        customContent = {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        Text(
                                                "Salvar bateria em: $hevSocTarget%",
                                                color = AppColors.TextPrimary,
                                                fontSize = 14.sp
                                        )
                                        Slider(
                                                value = hevSocTarget.toFloat(),
                                                onValueChange = { hevSocTarget = it.toInt() },
                                                onValueChangeFinished = {
                                                        prefs.edit {
                                                                putInt(SharedPreferencesKeys.HEV_SOC_TARGET_VALUE.key, hevSocTarget)
                                                        }
                                                        ServiceManager.getInstance().applyHevSocTargetIfActive("UI_SLIDER")
                                                },
                                                valueRange = 20f..80f,
                                                steps = 11,
                                                colors = SliderDefaults.colors(
                                                        thumbColor = AppColors.Primary,
                                                        activeTrackColor = AppColors.Primary,
                                                        inactiveTrackColor = Color(0xFF2C3139)
                                                )
                                        )
                                }
                        }
                )
        )

        settingsList.add(
                SettingItem(
                        title = "Deslocar Android Auto no cluster",
                        description = "Move a projeção do AA para a direita no cluster (display 3), pra não sobrepor a barra. Começa desligado; ligue e ajuste o slider olhando a tela (aplica ao vivo).",
                        group = SettingsGroups.DRIVE,
                        checked = enableAaClusterOffset,
                        onCheckedChange = {
                                enableAaClusterOffset = it
                                prefs.edit {
                                        putBoolean(SharedPreferencesKeys.ENABLE_AA_CLUSTER_OFFSET.key, it)
                                }
                                br.com.redesurftank.havalshisuku.managers.DisplayAppLauncher.reapplyAndroidAutoClusterBounds()
                        },
                        customContent = {
                                if (enableAaClusterOffset) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                                Text(
                                                        "Deslocamento: $aaClusterOffset px",
                                                        color = AppColors.TextPrimary,
                                                        fontSize = 14.sp
                                                )
                                                Slider(
                                                        value = aaClusterOffset.toFloat(),
                                                        onValueChange = { aaClusterOffset = it.toInt() },
                                                        onValueChangeFinished = {
                                                                prefs.edit {
                                                                        putInt(SharedPreferencesKeys.AA_CLUSTER_LEFT_OFFSET.key, aaClusterOffset)
                                                                }
                                                                br.com.redesurftank.havalshisuku.managers.DisplayAppLauncher.reapplyAndroidAutoClusterBounds()
                                                        },
                                                        valueRange = 0f..400f,
                                                        colors = SliderDefaults.colors(
                                                                thumbColor = AppColors.Primary,
                                                                activeTrackColor = AppColors.Primary,
                                                                inactiveTrackColor = Color(0xFF2C3139)
                                                        )
                                                )
                                        }
                                }
                        }
                )
        )

        if (isAdvancedUse && !selfInstallationCheck) {
                settingsList.add(
                        SettingItem(
                                title = "Ignorar verificação de integridade",
                                description =
                                        SharedPreferencesKeys
                                                .BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK
                                                .description,
                                group = SettingsGroups.FEATURES,
                                checked = bypassSelfInstallationCheck,
                                onCheckedChange = {
                                        bypassSelfInstallationCheck = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .BYPASS_SELF_INSTALLATION_INTEGRITY_CHECK
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        )
                )
        }

        settingsList.addAll(
                listOfNotNull(
                        SettingItem(
                                title = "Fechar janela ao desligar o veículo",
                                description =
                                        "Fecha automaticamente as janelas quando o motor é desligado",
                                group = SettingsGroups.SHUTDOWN,
                                checked = closeWindowOnPowerOff,
                                onCheckedChange = {
                                        closeWindowOnPowerOff = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_WINDOW_ON_POWER_OFF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Fechar janela ao recolher retrovisores",
                                description =
                                        "Sincroniza fechamento das janelas com o recolhimento dos retrovisores",
                                group = SettingsGroups.SHUTDOWN,
                                checked = closeWindowOnFoldMirror,
                                onCheckedChange = {
                                        closeWindowOnFoldMirror = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_WINDOW_ON_FOLD_MIRROR
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Fechar teto solar ao desligar",
                                description =
                                        SharedPreferencesKeys.CLOSE_SUNROOF_ON_POWER_OFF
                                                .description,
                                group = SettingsGroups.SHUTDOWN,
                                checked = closeSunroofOnPowerOff,
                                onCheckedChange = {
                                        closeSunroofOnPowerOff = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_SUNROOF_ON_POWER_OFF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Fechar teto solar ao recolher retrovisores",
                                description =
                                        SharedPreferencesKeys.CLOSE_SUNROOF_ON_FOLD_MIRROR
                                                .description,
                                group = SettingsGroups.SHUTDOWN,
                                checked = closeSunroofOnFoldMirror,
                                onCheckedChange = {
                                        closeSunroofOnFoldMirror = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_SUNROOF_ON_FOLD_MIRROR
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Fechar cortina do teto solar",
                                description =
                                        SharedPreferencesKeys
                                                .CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF
                                                .description,
                                group = SettingsGroups.SHUTDOWN,
                                checked = closeSunroofSunShadeOnCloseSunroof,
                                onCheckedChange = {
                                        closeSunroofSunShadeOnCloseSunroof = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .CLOSE_SUNROOF_SUN_SHADE_ON_CLOSE_SUNROOF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Fechar janelas com velocidade",
                                description =
                                        SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED.description,
                                group = SettingsGroups.SPEED,
                                checked = closeWindowsOnSpeed,
                                onCheckedChange = {
                                        closeWindowsOnSpeed = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.CLOSE_WINDOWS_ON_SPEED
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = speedThreshold.toInt(),
                                sliderRange = 10..120,
                                sliderStep = 1,
                                onSliderChange = { newSpeed ->
                                        speedThreshold = newSpeed.toFloat()
                                        prefs.edit {
                                                putFloat(
                                                        SharedPreferencesKeys.SPEED_THRESHOLD.key,
                                                        newSpeed.toFloat()
                                                )
                                        }
                                },
                                sliderLabel = "Velocidade: $speedThreshold km/h"
                        ),
                        SettingItem(
                                title = "Fechar teto solar com velocidade",
                                description =
                                        SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED.description,
                                group = SettingsGroups.SPEED,
                                checked = closeSunroofOnSpeed,
                                onCheckedChange = {
                                        closeSunroofOnSpeed = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.CLOSE_SUNROOF_ON_SPEED
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = closeSunroofSpeedThreshold.toInt(),
                                sliderRange = 10..120,
                                sliderStep = 1,
                                onSliderChange = { newSpeed ->
                                        closeSunroofSpeedThreshold = newSpeed.toFloat()
                                        prefs.edit {
                                                putFloat(
                                                        SharedPreferencesKeys
                                                                .SUNROOF_SPEED_THRESHOLD
                                                                .key,
                                                        newSpeed.toFloat()
                                                )
                                        }
                                },
                                sliderLabel =
                                        "Velocidade: ${closeSunroofSpeedThreshold.toInt()} km/h"
                        ),
                        SettingItem(
                                title = "A/C no máximo ao ligar o carro",
                                description =
                                        SharedPreferencesKeys.ENABLE_MAX_AC_ON_UNLOCK.description,
                                group = SettingsGroups.CLIMATE,
                                checked = enableMaxAcOnUnlock,
                                onCheckedChange = {
                                        enableMaxAcOnUnlock = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_MAX_AC_ON_UNLOCK
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = maxAcOnUnlockThreshold.toInt(),
                                sliderRange = 20..38,
                                sliderStep = 1,
                                onSliderChange = { newTemp ->
                                        maxAcOnUnlockThreshold = newTemp.toFloat()
                                        prefs.edit {
                                                putFloat(
                                                        SharedPreferencesKeys
                                                                .MAX_AC_ON_UNLOCK_THRESHOLD
                                                                .key,
                                                        newTemp.toFloat()
                                                )
                                        }
                                },
                                sliderLabel =
                                        "Temperatura de disparo: ${maxAcOnUnlockThreshold.toInt()}°C",
                                customContent =
                                        if (enableMaxAcOnUnlock) {
                                                {
                                                        val timeOptions =
                                                                mapOf(
                                                                        0 to "Sem limite",
                                                                        1 to "1 minuto",
                                                                        3 to "3 minutos",
                                                                        5 to "5 minutos"
                                                                )
                                                        var expanded by remember {
                                                                mutableStateOf(false)
                                                        }

                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(16.dp)
                                                        ) {
                                                                Column {
                                                                        Text(
                                                                                text =
                                                                                        "Temperatura alvo: ${maxAcTargetTemp.toInt()}°C",
                                                                                fontSize = 14.sp,
                                                                                color = Color.White
                                                                        )
                                                                        Slider(
                                                                                value =
                                                                                        maxAcTargetTemp,
                                                                                onValueChange = {
                                                                                        newTemp ->
                                                                                        maxAcTargetTemp =
                                                                                                newTemp
                                                                                        prefs.edit {
                                                                                                putFloat(
                                                                                                        SharedPreferencesKeys
                                                                                                                .MAX_AC_TARGET_TEMP
                                                                                                                .key,
                                                                                                        newTemp
                                                                                                )
                                                                                        }
                                                                                },
                                                                                valueRange =
                                                                                        18f..34f,
                                                                                steps = 15,
                                                                                colors =
                                                                                        SliderDefaults
                                                                                                .colors(
                                                                                                        thumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        activeTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        inactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                ),
                                                                                                        activeTickColor =
                                                                                                                Color.Transparent,
                                                                                                        inactiveTickColor =
                                                                                                                Color.Transparent
                                                                                                )
                                                                        )
                                                                }
                                                                Column {
                                                                        Text(
                                                                                text =
                                                                                        SharedPreferencesKeys
                                                                                                .MAX_AC_TIMEOUT
                                                                                                .description,
                                                                                fontSize = 14.sp,
                                                                                color = Color.White
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Box {
                                                                                Text(
                                                                                        text =
                                                                                                timeOptions[
                                                                                                        maxAcTimeout]
                                                                                                        ?: "Sem limite",
                                                                                        color =
                                                                                                Color(
                                                                                                        0xFF4A9EFF
                                                                                                ),
                                                                                        fontSize =
                                                                                                16.sp,
                                                                                        modifier =
                                                                                                Modifier.background(
                                                                                                                Color(
                                                                                                                        0xFF2A2F37
                                                                                                                ),
                                                                                                                RoundedCornerShape(
                                                                                                                        4.dp
                                                                                                                )
                                                                                                        )
                                                                                                        .padding(
                                                                                                                horizontal =
                                                                                                                        12.dp,
                                                                                                                vertical =
                                                                                                                        8.dp
                                                                                                        )
                                                                                                        .clickable {
                                                                                                                expanded =
                                                                                                                        true
                                                                                                        }
                                                                                )
                                                                                DropdownMenu(
                                                                                        expanded =
                                                                                                expanded,
                                                                                        onDismissRequest = {
                                                                                                expanded =
                                                                                                        false
                                                                                        },
                                                                                        modifier =
                                                                                                Modifier.background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        )
                                                                                                )
                                                                                ) {
                                                                                        timeOptions
                                                                                                .forEach {
                                                                                                        (
                                                                                                                value,
                                                                                                                label)
                                                                                                        ->
                                                                                                        DropdownMenuItem(
                                                                                                                text = {
                                                                                                                        Text(
                                                                                                                                label,
                                                                                                                                color =
                                                                                                                                        Color.White
                                                                                                                        )
                                                                                                                },
                                                                                                                onClick = {
                                                                                                                        maxAcTimeout =
                                                                                                                                value
                                                                                                                        prefs
                                                                                                                                .edit {
                                                                                                                                        putInt(
                                                                                                                                                SharedPreferencesKeys
                                                                                                                                                        .MAX_AC_TIMEOUT
                                                                                                                                                        .key,
                                                                                                                                                value
                                                                                                                                        )
                                                                                                                                }
                                                                                                                        expanded =
                                                                                                                                false
                                                                                                                }
                                                                                                        )
                                                                                                }
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title =
                                        SharedPreferencesKeys.ENABLE_OPEN_SUNROOF_CURTAIN_ON_START
                                                .description,
                                description =
                                        "Abre automaticamente a cortina do teto solar ao ligar o veículo",
                                group = SettingsGroups.CLIMATE,
                                checked = enableOpenSunroofCurtainOnStart,
                                onCheckedChange = { checked ->
                                        enableOpenSunroofCurtainOnStart = checked
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_OPEN_SUNROOF_CURTAIN_ON_START
                                                                .key,
                                                        checked
                                                )
                                        }
                                },
                                customContent =
                                        if (enableOpenSunroofCurtainOnStart) {
                                                {
                                                        var showCurtainStartPicker by remember {
                                                                mutableStateOf(false)
                                                        }
                                                        var showCurtainEndPicker by remember {
                                                                mutableStateOf(false)
                                                        }
                                                        var expandedTemp by remember {
                                                                mutableStateOf(false)
                                                        }

                                                        val tempOptions =
                                                                mapOf(
                                                                        -1f to "Desabilitado",
                                                                        26f to "26°C",
                                                                        28f to "28°C",
                                                                        30f to "30°C",
                                                                        32f to "32°C",
                                                                        34f to "34°C",
                                                                        36f to "36°C"
                                                                )

                                                        if (showCurtainStartPicker) {
                                                                val timeSetListener =
                                                                        TimePickerDialog
                                                                                .OnTimeSetListener {
                                                                                        _,
                                                                                        hour,
                                                                                        minute ->
                                                                                        curtainStartHour =
                                                                                                hour
                                                                                        curtainStartMinute =
                                                                                                minute
                                                                                        prefs.edit {
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .OPEN_SUNROOF_CURTAIN_START_HOUR
                                                                                                                .key,
                                                                                                        hour
                                                                                                )
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .OPEN_SUNROOF_CURTAIN_START_MINUTE
                                                                                                                .key,
                                                                                                        minute
                                                                                                )
                                                                                        }
                                                                                        showCurtainStartPicker =
                                                                                                false
                                                                                }
                                                                TimePickerDialog(
                                                                                LocalContext
                                                                                        .current,
                                                                                timeSetListener,
                                                                                curtainStartHour,
                                                                                curtainStartMinute,
                                                                                true
                                                                        )
                                                                        .show()
                                                        }

                                                        if (showCurtainEndPicker) {
                                                                val timeSetListener =
                                                                        TimePickerDialog
                                                                                .OnTimeSetListener {
                                                                                        _,
                                                                                        hour,
                                                                                        minute ->
                                                                                        curtainEndHour =
                                                                                                hour
                                                                                        curtainEndMinute =
                                                                                                minute
                                                                                        prefs.edit {
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .OPEN_SUNROOF_CURTAIN_END_HOUR
                                                                                                                .key,
                                                                                                        hour
                                                                                                )
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .OPEN_SUNROOF_CURTAIN_END_MINUTE
                                                                                                                .key,
                                                                                                        minute
                                                                                                )
                                                                                        }
                                                                                        showCurtainEndPicker =
                                                                                                false
                                                                                }
                                                                TimePickerDialog(
                                                                                LocalContext
                                                                                        .current,
                                                                                timeSetListener,
                                                                                curtainEndHour,
                                                                                curtainEndMinute,
                                                                                true
                                                                        )
                                                                        .show()
                                                        }

                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceEvenly
                                                                ) {
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .clickable {
                                                                                                        showCurtainStartPicker =
                                                                                                                true
                                                                                                }
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Column(
                                                                                        horizontalAlignment =
                                                                                                Alignment
                                                                                                        .CenterHorizontally
                                                                                ) {
                                                                                        Text(
                                                                                                "Início",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        14.sp
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.height(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "${String.format("%02d", curtainStartHour)}:${String.format("%02d", curtainStartMinute)}",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        18.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Medium
                                                                                        )
                                                                                }
                                                                        }
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                12.dp
                                                                                        )
                                                                        )
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .clickable {
                                                                                                        showCurtainEndPicker =
                                                                                                                true
                                                                                                }
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Column(
                                                                                        horizontalAlignment =
                                                                                                Alignment
                                                                                                        .CenterHorizontally
                                                                                ) {
                                                                                        Text(
                                                                                                "Fim",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        14.sp
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.height(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "${String.format("%02d", curtainEndHour)}:${String.format("%02d", curtainEndMinute)}",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        18.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Medium
                                                                                        )
                                                                                }
                                                                        }
                                                                }

                                                                Row(
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Text(
                                                                                "Temp. Máxima:",
                                                                                color = Color.White,
                                                                                fontSize = 14.sp,
                                                                                modifier =
                                                                                        Modifier.padding(
                                                                                                end =
                                                                                                        8.dp
                                                                                        )
                                                                        )
                                                                        Box {
                                                                                Text(
                                                                                        text =
                                                                                                tempOptions[
                                                                                                        openSunroofCurtainMaxTemp]
                                                                                                        ?: "Desabilitado",
                                                                                        color =
                                                                                                Color(
                                                                                                        0xFF4A9EFF
                                                                                                ),
                                                                                        fontSize =
                                                                                                16.sp,
                                                                                        modifier =
                                                                                                Modifier.background(
                                                                                                                Color(
                                                                                                                        0xFF2A2F37
                                                                                                                ),
                                                                                                                RoundedCornerShape(
                                                                                                                        4.dp
                                                                                                                )
                                                                                                        )
                                                                                                        .padding(
                                                                                                                horizontal =
                                                                                                                        12.dp,
                                                                                                                vertical =
                                                                                                                        8.dp
                                                                                                        )
                                                                                                        .clickable {
                                                                                                                expandedTemp =
                                                                                                                        true
                                                                                                        }
                                                                                )
                                                                                DropdownMenu(
                                                                                        expanded =
                                                                                                expandedTemp,
                                                                                        onDismissRequest = {
                                                                                                expandedTemp =
                                                                                                        false
                                                                                        },
                                                                                        modifier =
                                                                                                Modifier.background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        )
                                                                                                )
                                                                                ) {
                                                                                        tempOptions
                                                                                                .forEach {
                                                                                                        (
                                                                                                                value,
                                                                                                                label)
                                                                                                        ->
                                                                                                        DropdownMenuItem(
                                                                                                                text = {
                                                                                                                        Text(
                                                                                                                                label,
                                                                                                                                color =
                                                                                                                                        Color.White
                                                                                                                        )
                                                                                                                },
                                                                                                                onClick = {
                                                                                                                        openSunroofCurtainMaxTemp =
                                                                                                                                value
                                                                                                                        prefs
                                                                                                                                .edit {
                                                                                                                                        putFloat(
                                                                                                                                                SharedPreferencesKeys
                                                                                                                                                        .OPEN_SUNROOF_CURTAIN_MAX_TEMP
                                                                                                                                                        .key,
                                                                                                                                                value
                                                                                                                                        )
                                                                                                                                }
                                                                                                                        expandedTemp =
                                                                                                                                false
                                                                                                                }
                                                                                                        )
                                                                                                }
                                                                                }
                                                                 }
                                                         }
                                                 }
                                                 }
                                         } else null
                         ),
                        SettingItem(
                                title = "Manter desativado monitoramento de distrações",
                                description = "Desabilita alertas de distração durante a condução",
                                group = SettingsGroups.SAFETY,
                                checked = disableMonitoring,
                                onCheckedChange = {
                                        disableMonitoring = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.DISABLE_MONITORING.key,
                                                        it
                                                )
                                        }
                                        ServiceManager.getInstance().setMonitoringEnabled(!it)
                                }
                        ),
                        SettingItem(
                                title = "Habilitar barra inferior de rápido acesso",
                                description =
                                        "Cria uma barra inferior fixa com atalhos para ar condicionado e outras funções",
                                group = SettingsGroups.FEATURES,
                                checked = enablePersistentBottomBar,
                                onCheckedChange = { checked ->
                                        if (checked && !Settings.canDrawOverlays(context)) {
                                                // Request overlay permission
                                                val intent =
                                                        Intent(
                                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                                Uri.parse(
                                                                        "package:${context.packageName}"
                                                                )
                                                        )
                                                context.startActivity(intent)
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "Por favor, habilite a permissão de sobreposição para a barra inferior",
                                                                android.widget.Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                return@SettingItem
                                        }

                                        enablePersistentBottomBar = checked
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.PERSISTENT_BOTTOM_BAR
                                                                .key,
                                                        checked
                                                )
                                        }
                                        val serviceIntent =
                                                Intent(
                                                        context,
                                                        br.com.redesurftank.havalshisuku.services
                                                                        .BottomBarService::class
                                                                .java
                                                )
                                        if (checked) {
                                                context.startService(serviceIntent)
                                                Thread {
                                                                br.com.redesurftank.havalshisuku
                                                                        .utils.ShizukuUtils
                                                                        .runCommandAndGetOutput(
                                                                                arrayOf(
                                                                                        "sh",
                                                                                        "-c",
                                                                                        "wm size reset"
                                                                                )
                                                                        )
                                                                val overscan =
                                                                        prefs.getInt(
                                                                                SharedPreferencesKeys
                                                                                        .PERSISTENT_BOTTOM_BAR_OVERSCAN
                                                                                        .key,
                                                                                20
                                                                        )
                                                                br.com.redesurftank.havalshisuku
                                                                        .utils.ShizukuUtils
                                                                        .runCommandAndGetOutput(
                                                                                arrayOf(
                                                                                        "wm",
                                                                                        "overscan",
                                                                                        "0,0,0,$overscan"
                                                                                )
                                                                        )
                                                                // v2.3: reflow Impulse-managed Display 0 apps to
                                                                // honor the freshly-applied overscan.
                                                                br.com.redesurftank.havalshisuku
                                                                        .managers.DisplayAppLauncher
                                                                        .reapplyDisplay0BoundsForOverscanAsync()
                                                        }
                                                        .start()
                                        } else {
                                                context.stopService(serviceIntent)
                                                Thread {
                                                                br.com.redesurftank.havalshisuku
                                                                        .utils.ShizukuUtils
                                                                        .runCommandAndGetOutput(
                                                                                arrayOf(
                                                                                        "wm",
                                                                                        "overscan",
                                                                                        "0,0,0,0"
                                                                                )
                                                                        )
                                                                // v2.3: restore full Display 0 bounds for managed
                                                                // apps when the bar is disabled.
                                                                br.com.redesurftank.havalshisuku
                                                                        .managers.DisplayAppLauncher
                                                                        .reapplyDisplay0BoundsForOverscanAsync()
                                                        }
                                                        .start()
                                        }
                                },
                                customContent =
                                        if (enablePersistentBottomBar) {
                                                {
                                                        Column(
                                                                modifier =
                                                                        Modifier.padding(top = 8.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF1D2430),
                                                                        thickness = 1.dp
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        12.dp
                                                                                )
                                                                )

                                                                // Auto-hide row
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        "Auto-ocultar barra",
                                                                                        color =
                                                                                                Color.White,
                                                                                        fontSize =
                                                                                                16.sp
                                                                                )
                                                                                Text(
                                                                                        "Esconde após 30s de inatividade",
                                                                                        color =
                                                                                                Color.Gray,
                                                                                        fontSize =
                                                                                                12.sp
                                                                                )
                                                                        }
                                                                        Switch(
                                                                                checked =
                                                                                        autoHideEnabled,
                                                                                onCheckedChange = {
                                                                                        autoHideEnabled =
                                                                                                it
                                                                                        prefs.edit()
                                                                                                .putBoolean(
                                                                                                        SharedPreferencesKeys
                                                                                                                .BOTTOM_BAR_AUTO_HIDE
                                                                                                                .key,
                                                                                                        it
                                                                                                )
                                                                                                .apply()
                                                                                        BottomBarState
                                                                                                .autoHideEnabled =
                                                                                                it
                                                                                },
                                                                                modifier =
                                                                                        Modifier.scale(
                                                                                                0.9f
                                                                                        ),
                                                                                colors =
                                                                                        SwitchDefaults
                                                                                                .colors(
                                                                                                        checkedThumbColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .TextPrimary,
                                                                                                        checkedTrackColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .Primary,
                                                                                                        uncheckedThumbColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .TextSecondary,
                                                                                                        uncheckedTrackColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .ButtonSecondary,
                                                                                                        uncheckedBorderColor =
                                                                                                                Color.Transparent,
                                                                                                        checkedBorderColor =
                                                                                                                Color.Transparent
                                                                                                )
                                                                        )
                                                                }

                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        12.dp
                                                                                )
                                                                )

                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Column(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        "Manter barra escondida",
                                                                                        color =
                                                                                                Color.White,
                                                                                        fontSize =
                                                                                                16.sp
                                                                                )
                                                                                Text(
                                                                                        "A barra some de vez; abra o dashboard pelo atalho do volante (\"Alternar dashboard Impulse\").",
                                                                                        color =
                                                                                                Color.Gray,
                                                                                        fontSize =
                                                                                                12.sp
                                                                                )
                                                                        }
                                                                        Switch(
                                                                                checked =
                                                                                        barHiddenEnabled,
                                                                                onCheckedChange = {
                                                                                        barHiddenEnabled =
                                                                                                it
                                                                                        prefs.edit()
                                                                                                .putBoolean(
                                                                                                        SharedPreferencesKeys
                                                                                                                .BOTTOM_BAR_HIDDEN
                                                                                                                .key,
                                                                                                        it
                                                                                                )
                                                                                                .apply()
                                                                                        BottomBarState
                                                                                                .barHidden =
                                                                                                it
                                                                                },
                                                                                modifier =
                                                                                        Modifier.scale(
                                                                                                0.9f
                                                                                        ),
                                                                                colors =
                                                                                        SwitchDefaults
                                                                                                .colors(
                                                                                                        checkedThumbColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .TextPrimary,
                                                                                                        checkedTrackColor =
                                                                                                                br.com
                                                                                                                        .redesurftank
                                                                                                                        .havalshisuku
                                                                                                                        .ui
                                                                                                                        .components
                                                                                                                        .AppColors
                                                                                                                        .Primary
                                                                                                )
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "Desativar AVAS",
                                description = "Sistema de alerta de veículo silencioso",
                                group = SettingsGroups.SAFETY,
                                checked = disableAvas,
                                onCheckedChange = {
                                        disableAvas = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.DISABLE_AVAS.key,
                                                        it
                                                )
                                        }
                                        ServiceManager.getInstance().setAvasEnabled(!it)
                                }
                        ),
                        SettingItem(
                                title = "Desativar câmera AVM quando parado",
                                description =
                                        "Desliga câmera de visão 360° quando o veículo está parado",
                                group = SettingsGroups.SAFETY,
                                checked = disableAvmCarStopped,
                                onCheckedChange = {
                                        disableAvmCarStopped = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_AVM_CAR_STOPPED
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Aviso de voz: cinto de segurança",
                                description =
                                        "Em movimento, fala QUAL assento está sem cinto (1x; repete só se prender " +
                                                "e soltar por 30s). 2+ juntos = frase única. Vozes trocáveis na pasta do " +
                                                "app: seatbelt_voice_seat0..4 e seatbelt_voice_multi (.mp3/.m4a).",
                                group = SettingsGroups.SAFETY,
                                checked = enableSeatbeltVoice,
                                onCheckedChange = {
                                        enableSeatbeltVoice = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_SEATBELT_VOICE
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                customContent = {
                                        Column {
                                                Text(
                                                        "Volume mínimo do aviso: ${seatbeltMinVol.toInt()}%",
                                                        color = AppColors.TextPrimary,
                                                        fontSize = 14.sp
                                                )
                                                Text(
                                                        "Força o volume de mídia pra esse mínimo enquanto o aviso toca (mesmo no mudo) e volta ao normal depois. 0% = respeita o volume atual.",
                                                        color = AppColors.TextSecondary,
                                                        fontSize = 11.sp
                                                )
                                                Slider(
                                                        value = seatbeltMinVol,
                                                        onValueChange = { seatbeltMinVol = it },
                                                        onValueChangeFinished = {
                                                                prefs.edit {
                                                                        putInt(
                                                                                SharedPreferencesKeys
                                                                                        .SEATBELT_VOICE_MIN_VOLUME_PCT
                                                                                        .key,
                                                                                seatbeltMinVol.toInt()
                                                                        )
                                                                }
                                                        },
                                                        valueRange = 0f..100f,
                                                        steps = 19
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Button(onClick = { SeatbeltVoiceReminder.playTest() }) {
                                                        Text("Testar voz (toca agora)")
                                                }
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Ligar ventilação do banco do motorista com A/C ligado",
                                description =
                                        SharedPreferencesKeys.ENABLE_SEAT_VENTILATION_ON_AC_ON
                                                .description,
                                group = SettingsGroups.CLIMATE,
                                checked = enableSeatVentilationOnAcOn,
                                onCheckedChange = {
                                        enableSeatVentilationOnAcOn = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_SEAT_VENTILATION_ON_AC_ON
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Ventilação do passageiro com A/C (presença automática)",
                                description =
                                        SharedPreferencesKeys.ENABLE_PASSENGER_SEAT_VENTILATION_ON_AC_ON
                                                .description,
                                group = SettingsGroups.CLIMATE,
                                checked = enablePassengerSeatVentilationOnAcOn,
                                onCheckedChange = {
                                        enablePassengerSeatVentilationOnAcOn = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_PASSENGER_SEAT_VENTILATION_ON_AC_ON
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                customContent =
                                        if (enablePassengerSeatVentilationOnAcOn) {
                                                {
                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )
                                                                Text(
                                                                        "Passageiro: " +
                                                                                if (passengerPresent)
                                                                                        "presente"
                                                                                else "ausente",
                                                                        color =
                                                                                if (passengerPresent)
                                                                                        Color(
                                                                                                0xFF4A9EFF
                                                                                        )
                                                                                else
                                                                                        AppColors.TextSecondary,
                                                                        fontSize = 15.sp,
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Text(
                                                                        "Detectada pelo sensor de cinto do passageiro + porta. Use o botão se a leitura ficar errada (ex.: bolsa pesada no banco, ou alguém que andou o tempo todo sem cinto).",
                                                                        color =
                                                                                AppColors.TextSecondary,
                                                                        fontSize = 12.sp
                                                                )
                                                                Button(
                                                                        onClick = {
                                                                                ServiceManager
                                                                                        .getInstance()
                                                                                        .togglePassengerPresent()
                                                                                passengerPresent =
                                                                                        !passengerPresent
                                                                        }
                                                                ) {
                                                                        Text(
                                                                                "Inverter presença"
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "Desligar bluetooth ao desligar",
                                description =
                                        SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_POWER_OFF
                                                .description,
                                group = SettingsGroups.SHUTDOWN,
                                checked = disableBluetoothOnPowerOff,
                                onCheckedChange = {
                                        disableBluetoothOnPowerOff = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_BLUETOOTH_ON_POWER_OFF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Desligar ponto de acesso ao desligar",
                                description =
                                        SharedPreferencesKeys.DISABLE_HOTSPOT_ON_POWER_OFF
                                                .description,
                                group = SettingsGroups.SHUTDOWN,
                                checked = disableHotspotOnPowerOff,
                                onCheckedChange = {
                                        disableHotspotOnPowerOff = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_HOTSPOT_ON_POWER_OFF
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Desativar Bluetooth ao recolher retrovisores",
                                description =
                                        SharedPreferencesKeys.DISABLE_BLUETOOTH_ON_FOLD_MIRROR
                                                .description,
                                group = SettingsGroups.SHUTDOWN,
                                checked = disableBluetoothOnFoldMirror,
                                onCheckedChange = {
                                        disableBluetoothOnFoldMirror = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_BLUETOOTH_ON_FOLD_MIRROR
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Desativar ponto de acesso ao recolher retrovisores",
                                description =
                                        SharedPreferencesKeys.DISABLE_HOTSPOT_ON_FOLD_MIRROR
                                                .description,
                                group = SettingsGroups.SHUTDOWN,
                                checked = disableHotspotOnFoldMirror,
                                onCheckedChange = {
                                        disableHotspotOnFoldMirror = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .DISABLE_HOTSPOT_ON_FOLD_MIRROR
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        ),
                        SettingItem(
                                title = "Ativar Ambient Light BLE",
                                description =
                                        "Exibe o recurso opcional para LEDs externos instalados pelo usuario",
                                group = SettingsGroups.FEATURES,
                                checked = ambientLightBleEnabled,
                                onCheckedChange = {
                                        ambientLightBleEnabled = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .AMBIENT_LIGHT_BLE_ENABLED
                                                                .key,
                                                        it
                                                )
                                        }
                                        if (it) {
                                                AmbientLightService.startIfEnabled(context)
                                        } else {
                                                AmbientLightService.stop(context)
                                        }
                                }
                        ),
                        SettingItem(
                                title = "HotRouter",
                                description = SharedPreferencesKeys.ENABLE_HOT_ROUTER.description,
                                group = SettingsGroups.FEATURES,
                                checked = enableHotRouter,
                                onCheckedChange = {
                                        enableHotRouter = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.ENABLE_HOT_ROUTER.key,
                                                        it
                                                )
                                        }
                                        HotRouterManager.getInstance().setEnabled(it)
                                },
                                customContent =
                                        if (enableHotRouter) {
                                                {
                                                        var statusMode by remember {
                                                                mutableStateOf(
                                                                        HotRouterManager.MODE_STARTING
                                                                )
                                                        }
                                                        var statusEpoch by remember {
                                                                mutableStateOf(0L)
                                                        }
                                                        // Hotspot do carro REALMENTE no ar (carrier do wlan2). O daemon reporta
                                                        // WLAN/4G mesmo com o hotspot desligado; sem isto o status dava "Ativo" à toa.
                                                        var hotspotOnAir by remember {
                                                                mutableStateOf(true)
                                                        }

                                                        LaunchedEffect(Unit) {
                                                                while (true) {
                                                                        val s =
                                                                                withContext(
                                                                                        Dispatchers.IO
                                                                                ) {
                                                                                        HotRouterManager
                                                                                                .getInstance()
                                                                                                .readStatusBlocking()
                                                                                }
                                                                        statusMode = s.mode
                                                                        statusEpoch = s.epochSeconds
                                                                        hotspotOnAir =
                                                                                withContext(Dispatchers.IO) {
                                                                                        try {
                                                                                                ServiceManager.getInstance().isHotspotOnAir()
                                                                                        } catch (t: Throwable) {
                                                                                                true
                                                                                        }
                                                                                }
                                                                        delay(3000)
                                                                }
                                                        }

                                                        val label =
                                                                when {
                                                                        statusMode == HotRouterManager.MODE_OFF ->
                                                                                "Desligado"
                                                                        statusMode == HotRouterManager.MODE_STARTING ->
                                                                                "Iniciando…"
                                                                        statusMode == HotRouterManager.MODE_ERROR ->
                                                                                "Erro"
                                                                        // Daemon rodando (WLAN/4G) mas hotspot do carro fora do ar:
                                                                        // não há o que rotear -> não é "Ativo".
                                                                        !hotspotOnAir ->
                                                                                "Ligado (sem hotspot)"
                                                                        statusMode == HotRouterManager.MODE_WLAN ->
                                                                                "Ativo (WLAN)"
                                                                        statusMode == HotRouterManager.MODE_4G ->
                                                                                "Ativo (4G)"
                                                                        else -> "—"
                                                                }

                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )
                                                                Text(
                                                                        text = "Status: $label",
                                                                        color = Color.White,
                                                                        fontSize = 16.sp
                                                                )
                                                                if (statusEpoch > 0L) {
                                                                        Text(
                                                                                text =
                                                                                        "atualizado ${formatHms(statusEpoch)}",
                                                                                color =
                                                                                        Color(
                                                                                                0xFFB0B8C4
                                                                                        ),
                                                                                fontSize = 12.sp
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "Habilitar botões personalizados no volante",
                                description =
                                        SharedPreferencesKeys.ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS
                                                .description,
                                group = SettingsGroups.FEATURES,
                                checked = enableCustomSteeringWheelButtons,
                                onCheckedChange = {
                                        enableCustomSteeringWheelButtons = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_STEERING_WHEEL_CUSTOM_BUTTONS
                                                                .key,
                                                        it
                                                )
                                        }
                                        ServiceManager.getInstance()
                                                .ensureSteeringWheelButtonIntegration()
                                },
                                customContent =
                                        if (enableCustomSteeringWheelButtons) {
                                                {
                                                        var steeringWheelButton1ActionDouble by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(
                                                                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION_DOUBLE.key,
                                                                                SteeringWheelCustomActionType.DEFAULT.key
                                                                        ) ?: SteeringWheelCustomActionType.DEFAULT.key
                                                                )
                                                        }
                                                        var steeringWheelButton2ActionDouble by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(
                                                                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION_DOUBLE.key,
                                                                                SteeringWheelCustomActionType.DEFAULT.key
                                                                        ) ?: SteeringWheelCustomActionType.DEFAULT.key
                                                                )
                                                        }
                                                        var steeringWheelButton1PackageDouble by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1_DOUBLE.key, "")
                                                                                ?: ""
                                                                )
                                                        }
                                                        var steeringWheelButton2PackageDouble by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2_DOUBLE.key, "")
                                                                                ?: ""
                                                                )
                                                        }

                                                        var steeringWheelButton1ActionLong by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(
                                                                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION_LONG.key,
                                                                                SteeringWheelCustomActionType.DEFAULT.key
                                                                        ) ?: SteeringWheelCustomActionType.DEFAULT.key
                                                                )
                                                        }
                                                        var steeringWheelButton2ActionLong by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(
                                                                                SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION_LONG.key,
                                                                                SteeringWheelCustomActionType.DEFAULT.key
                                                                        ) ?: SteeringWheelCustomActionType.DEFAULT.key
                                                                )
                                                        }
                                                        var steeringWheelButton1PackageLong by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1_LONG.key, "")
                                                                                ?: ""
                                                                )
                                                        }
                                                        var steeringWheelButton2PackageLong by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2_LONG.key, "")
                                                                                ?: ""
                                                                )
                                                        }
                                                        var steeringWheelButton1ClimateCommandDouble by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1_DOUBLE.key, "")
                                                                                ?: ""
                                                                )
                                                        }
                                                        var steeringWheelButton2ClimateCommandDouble by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2_DOUBLE.key, "")
                                                                                ?: ""
                                                                )
                                                        }
                                                        var steeringWheelButton1ClimateCommandLong by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1_LONG.key, "")
                                                                                ?: ""
                                                                )
                                                        }
                                                        var steeringWheelButton2ClimateCommandLong by remember {
                                                                mutableStateOf(
                                                                        prefs.getString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2_LONG.key, "")
                                                                                ?: ""
                                                                )
                                                        }

                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )

                                                                SteeringActionPicker(
                                                                        label = "Botão 1",
                                                                        actionKey = steeringWheelButton1Action,
                                                                        packageName = steeringWheelButton1Package,
                                                                        climateCommandKey = steeringWheelButton1ClimateCommand,
                                                                        onActionSelected = { newKey ->
                                                                                steeringWheelButton1Action = newKey
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION.key, newKey)
                                                                                }
                                                                                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                                                                        },
                                                                        onPackageChanged = { newPkg ->
                                                                                steeringWheelButton1Package = newPkg
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1.key, newPkg)
                                                                                }
                                                                        },
                                                                        onClimateCommandSelected = { command ->
                                                                                steeringWheelButton1ClimateCommand = command.key
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1.key, command.key)
                                                                                }
                                                                        }
                                                                )

                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )

                                                                SteeringActionPicker(
                                                                        label = "Botão 2",
                                                                        actionKey = steeringWheelButton2Action,
                                                                        packageName = steeringWheelButton2Package,
                                                                        climateCommandKey = steeringWheelButton2ClimateCommand,
                                                                        onActionSelected = { newKey ->
                                                                                steeringWheelButton2Action = newKey
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION.key, newKey)
                                                                                }
                                                                                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                                                                        },
                                                                        onPackageChanged = { newPkg ->
                                                                                steeringWheelButton2Package = newPkg
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2.key, newPkg)
                                                                                }
                                                                        },
                                                                        onClimateCommandSelected = { command ->
                                                                                steeringWheelButton2ClimateCommand = command.key
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2.key, command.key)
                                                                                }
                                                                        }
                                                                )

                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )
                                                                Text(
                                                                        "Toque duplo",
                                                                        color = Color.White,
                                                                        fontSize = 16.sp
                                                                )
                                                                SteeringActionPicker(
                                                                        label = "Botão 1 (toque duplo)",
                                                                        actionKey = steeringWheelButton1ActionDouble,
                                                                        packageName = steeringWheelButton1PackageDouble,
                                                                        climateCommandKey = steeringWheelButton1ClimateCommandDouble,
                                                                        onActionSelected = { newKey ->
                                                                                steeringWheelButton1ActionDouble = newKey
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION_DOUBLE.key, newKey)
                                                                                }
                                                                                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                                                                        },
                                                                        onPackageChanged = { newPkg ->
                                                                                steeringWheelButton1PackageDouble = newPkg
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1_DOUBLE.key, newPkg)
                                                                                }
                                                                        },
                                                                        onClimateCommandSelected = { command ->
                                                                                steeringWheelButton1ClimateCommandDouble = command.key
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1_DOUBLE.key, command.key)
                                                                                }
                                                                        }
                                                                )
                                                                SteeringActionPicker(
                                                                        label = "Botão 2 (toque duplo)",
                                                                        actionKey = steeringWheelButton2ActionDouble,
                                                                        packageName = steeringWheelButton2PackageDouble,
                                                                        climateCommandKey = steeringWheelButton2ClimateCommandDouble,
                                                                        onActionSelected = { newKey ->
                                                                                steeringWheelButton2ActionDouble = newKey
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION_DOUBLE.key, newKey)
                                                                                }
                                                                                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                                                                        },
                                                                        onPackageChanged = { newPkg ->
                                                                                steeringWheelButton2PackageDouble = newPkg
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2_DOUBLE.key, newPkg)
                                                                                }
                                                                        },
                                                                        onClimateCommandSelected = { command ->
                                                                                steeringWheelButton2ClimateCommandDouble = command.key
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2_DOUBLE.key, command.key)
                                                                                }
                                                                        }
                                                                )
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )
                                                                Text(
                                                                        "Toque longo",
                                                                        color = Color.White,
                                                                        fontSize = 16.sp
                                                                )
                                                                SteeringActionPicker(
                                                                        label = "Botão 1 (toque longo)",
                                                                        actionKey = steeringWheelButton1ActionLong,
                                                                        packageName = steeringWheelButton1PackageLong,
                                                                        climateCommandKey = steeringWheelButton1ClimateCommandLong,
                                                                        onActionSelected = { newKey ->
                                                                                steeringWheelButton1ActionLong = newKey
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_1_ACTION_LONG.key, newKey)
                                                                                }
                                                                                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                                                                        },
                                                                        onPackageChanged = { newPkg ->
                                                                                steeringWheelButton1PackageLong = newPkg
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_1_LONG.key, newPkg)
                                                                                }
                                                                        },
                                                                        onClimateCommandSelected = { command ->
                                                                                steeringWheelButton1ClimateCommandLong = command.key
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_1_LONG.key, command.key)
                                                                                }
                                                                        }
                                                                )
                                                                SteeringActionPicker(
                                                                        label = "Botão 2 (toque longo)",
                                                                        actionKey = steeringWheelButton2ActionLong,
                                                                        packageName = steeringWheelButton2PackageLong,
                                                                        climateCommandKey = steeringWheelButton2ClimateCommandLong,
                                                                        onActionSelected = { newKey ->
                                                                                steeringWheelButton2ActionLong = newKey
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CUSTOM_BUTON_2_ACTION_LONG.key, newKey)
                                                                                }
                                                                                ServiceManager.getInstance().ensureSteeringWheelButtonIntegration()
                                                                        },
                                                                        onPackageChanged = { newPkg ->
                                                                                steeringWheelButton2PackageLong = newPkg
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_OPEN_APP_PACKAGE_BUTTON_2_LONG.key, newPkg)
                                                                                }
                                                                        },
                                                                        onClimateCommandSelected = { command ->
                                                                                steeringWheelButton2ClimateCommandLong = command.key
                                                                                prefs.edit {
                                                                                        putString(SharedPreferencesKeys.STEERING_WHEEL_CLIMATE_COMMAND_BUTTON_2_LONG.key, command.key)
                                                                                }
                                                                        }
                                                                )
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "Ajustar brilho automaticamente",
                                description = "Ajusta o brilho da tela automaticamente",
                                group = SettingsGroups.DISPLAY,
                                checked = enableAutoBrightness,
                                onCheckedChange = {
                                        enableAutoBrightness = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.ENABLE_AUTO_BRIGHTNESS
                                                                .key,
                                                        it
                                                )
                                        }
                                        AutoBrightnessManager.getInstance().setEnabled(it)
                                },
                                customContent =
                                        if (enableAutoBrightness) {
                                                {
                                                        Column(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                HorizontalDivider(
                                                                        color = Color(0xFF3A3F47),
                                                                        thickness = 1.dp
                                                                )

                                                                // Toggle: transição por nascer/pôr do sol
                                                                Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                        Column(modifier = Modifier.weight(1f)) {
                                                                                Text(
                                                                                        "Transição por nascer/pôr do sol",
                                                                                        color = Color.White,
                                                                                        fontSize = 15.sp,
                                                                                        fontWeight = FontWeight.Medium
                                                                                )
                                                                                Text(
                                                                                        "Usa o GPS pra escurecer/clarear suave (1 nível a cada 5 min) no pôr/nascer do sol. Substitui os horários fixos.",
                                                                                        color = Color(0xFFB0B8C4),
                                                                                        fontSize = 12.sp
                                                                                )
                                                                        }
                                                                        Switch(
                                                                                checked = autoBrightnessUseSun,
                                                                                onCheckedChange = { on ->
                                                                                        autoBrightnessUseSun = on
                                                                                        prefs.edit {
                                                                                                putBoolean(
                                                                                                        SharedPreferencesKeys.AUTO_BRIGHTNESS_USE_SUN.key,
                                                                                                        on
                                                                                                )
                                                                                        }
                                                                                        AutoBrightnessManager.getInstance().updateSchedule()
                                                                                }
                                                                        )
                                                                }

                                                                if (autoBrightnessUseSun) {
                                                                        val sunInfo by produceState<AutoBrightnessManager.SunDisplayInfo?>(
                                                                                initialValue = null,
                                                                                autoBrightnessUseSun
                                                                        ) {
                                                                                // Fora da main; tenta de novo enquanto o GPS não tem fix
                                                                                // ou a cidade ainda é coordenada (geocode em background).
                                                                                var tries = 0
                                                                                while (tries < 15) {
                                                                                        val info = withContext(Dispatchers.IO) {
                                                                                                AutoBrightnessManager.getInstance().getSunInfoForDisplay()
                                                                                        }
                                                                                        value = info
                                                                                        if (info != null && info.cityResolved) break
                                                                                        tries++
                                                                                        delay(2000)
                                                                                }
                                                                        }
                                                                        Column {
                                                                                Text(
                                                                                        "Referência (GPS): ${sunInfo?.city ?: "obtendo localização..."}",
                                                                                        color = Color.White,
                                                                                        fontSize = 14.sp
                                                                                )
                                                                                sunInfo?.let {
                                                                                        Text(
                                                                                                "Nascer ${it.sunrise}  ·  Pôr ${it.sunset}",
                                                                                                color = Color(0xFFB0B8C4),
                                                                                                fontSize = 13.sp
                                                                                        )
                                                                                }
                                                                        }
                                                                }

                                                                if (!autoBrightnessUseSun)
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceEvenly
                                                                ) {
                                                                        // Início da noite
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .clickable {
                                                                                                        showStartPicker =
                                                                                                                true
                                                                                                }
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Column(
                                                                                        horizontalAlignment =
                                                                                                Alignment
                                                                                                        .CenterHorizontally
                                                                                ) {
                                                                                        Text(
                                                                                                "Início da noite",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        14.sp
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.height(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "${String.format("%02d", nightStartHour)}:${String.format("%02d", nightStartMinute)}",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        18.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Medium
                                                                                        )
                                                                                }
                                                                        }

                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                12.dp
                                                                                        )
                                                                        )

                                                                        // Fim da noite
                                                                        Box(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                                .clickable {
                                                                                                        showEndPicker =
                                                                                                                true
                                                                                                }
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF2A2F37
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                8.dp
                                                                                                        )
                                                                                                )
                                                                                                .padding(
                                                                                                        16.dp
                                                                                                ),
                                                                                contentAlignment =
                                                                                        Alignment
                                                                                                .Center
                                                                        ) {
                                                                                Column(
                                                                                        horizontalAlignment =
                                                                                                Alignment
                                                                                                        .CenterHorizontally
                                                                                ) {
                                                                                        Text(
                                                                                                "Fim da noite",
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        14.sp
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.height(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                "${String.format("%02d", nightEndHour)}:${String.format("%02d", nightEndMinute)}",
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFF4A9EFF
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        18.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Medium
                                                                                        )
                                                                                }
                                                                        }
                                                                }

                                                                // Slider para nível de brilho
                                                                // diurno
                                                                Column {
                                                                        Text(
                                                                                "Nível de brilho diurno: $dayBrightnessLevel",
                                                                                color = Color.White,
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Slider(
                                                                                value =
                                                                                        dayBrightnessLevel
                                                                                                .toFloat(),
                                                                                onValueChange = {
                                                                                        newValue ->
                                                                                        dayBrightnessLevel =
                                                                                                newValue.toInt()
                                                                                        prefs.edit {
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .AUTO_BRIGHTNESS_LEVEL_DAY
                                                                                                                .key,
                                                                                                        dayBrightnessLevel
                                                                                                )
                                                                                        }
                                                                                },
                                                                                valueRange =
                                                                                        1f..10f,
                                                                                steps = 9,
                                                                                colors =
                                                                                        SliderDefaults
                                                                                                .colors(
                                                                                                        thumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        activeTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        inactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                ),
                                                                                                        activeTickColor =
                                                                                                                Color.Transparent,
                                                                                                        inactiveTickColor =
                                                                                                                Color.Transparent,
                                                                                                        disabledThumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        disabledActiveTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        disabledInactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                )
                                                                                                )
                                                                        )
                                                                }

                                                                // Slider para nível de brilho
                                                                // noturno
                                                                Column {
                                                                        Text(
                                                                                "Nível de brilho noturno: $nightBrightnessLevel",
                                                                                color = Color.White,
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Slider(
                                                                                value =
                                                                                        nightBrightnessLevel
                                                                                                .toFloat(),
                                                                                onValueChange = {
                                                                                        newValue ->
                                                                                        nightBrightnessLevel =
                                                                                                newValue.toInt()
                                                                                        prefs.edit {
                                                                                                putInt(
                                                                                                        SharedPreferencesKeys
                                                                                                                .AUTO_BRIGHTNESS_LEVEL_NIGHT
                                                                                                                .key,
                                                                                                        nightBrightnessLevel
                                                                                                )
                                                                                        }
                                                                                },
                                                                                valueRange =
                                                                                        1f..10f,
                                                                                steps = 9,
                                                                                colors =
                                                                                        SliderDefaults
                                                                                                .colors(
                                                                                                        thumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        activeTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        inactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                ),
                                                                                                        activeTickColor =
                                                                                                                Color.Transparent,
                                                                                                        inactiveTickColor =
                                                                                                                Color.Transparent,
                                                                                                        disabledThumbColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        disabledActiveTrackColor =
                                                                                                                AppColors
                                                                                                                        .Primary,
                                                                                                        disabledInactiveTrackColor =
                                                                                                                Color(
                                                                                                                        0xFF2C3139
                                                                                                                )
                                                                                                )
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else null
                        ),
                        SettingItem(
                                title = "Definir volume inicial",
                                description = SharedPreferencesKeys.SET_STARTUP_VOLUME.description,
                                group = SettingsGroups.DISPLAY,
                                checked = setStartupVolume,
                                onCheckedChange = {
                                        setStartupVolume = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys.SET_STARTUP_VOLUME
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = volume,
                                sliderRange = 0..40,
                                onSliderChange = { newVolume ->
                                        volume = newVolume
                                        prefs.edit {
                                                putInt(
                                                        SharedPreferencesKeys.STARTUP_VOLUME.key,
                                                        newVolume
                                                )
                                        }
                                },
                                sliderLabel = "Volume: $volume"
                        ),
                        SettingItem(
                                title = "Ajuste de velocidade",
                                description =
                                        "Ajusta a velocidade exibida no painel (Virtual Cluster)",
                                group = SettingsGroups.DRIVE,
                                checked = enableSpeedAdjustment,
                                onCheckedChange = {
                                        enableSpeedAdjustment = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .ENABLE_SPEED_ADJUSTMENT
                                                                .key,
                                                        it
                                                )
                                        }
                                },
                                sliderValue = speedAdjustmentOffset.toInt(),
                                sliderRange = -50..50,
                                sliderStep = 1,
                                onSliderChange = { newValue ->
                                        speedAdjustmentOffset = newValue.toFloat()
                                        prefs.edit {
                                                putFloat(
                                                        SharedPreferencesKeys
                                                                .SPEED_ADJUSTMENT_OFFSET
                                                                .key,
                                                        newValue.toFloat()
                                                )
                                        }
                                },
                                sliderLabel =
                                        "Ajuste: ${if (speedAdjustmentOffset > 0) "+" else ""}${speedAdjustmentOffset.toInt()}%"
                        ),
                        SettingItem(
                                title = "Ocultar velocidade na projeção",
                                description =
                                        "Remove o número da velocidade e o card atrás dele no cluster enquanto o mapa do CarPlay/Android Auto está projetado",
                                group = SettingsGroups.DRIVE,
                                checked = hideClusterSpeedDuringProjection,
                                onCheckedChange = {
                                        hideClusterSpeedDuringProjection = it
                                        prefs.edit {
                                                putBoolean(
                                                        SharedPreferencesKeys
                                                                .HIDE_CLUSTER_SPEED_DURING_PROJECTION
                                                                .key,
                                                        it
                                                )
                                        }
                                }
                        )
                )
        )

        // Subaba "Performance" — último grupo (SettingsGroups.PERFORMANCE): debloat + DataTrack +
        // overlay CPU/RAM. Bloco mantido em PerformanceScreen.kt pra não inchar este arquivo.
        settingsList.addAll(performanceSettingItems(prefs))

        GroupedSettingsLayout(items = settingsList)

        if (showStartPicker) {
                LaunchedEffect(Unit) {
                        val dialog =
                                TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                                nightStartHour = h
                                                nightStartMinute = m
                                                prefs.edit {
                                                        putInt(
                                                                SharedPreferencesKeys
                                                                        .NIGHT_START_HOUR
                                                                        .key,
                                                                h
                                                        )
                                                        putInt(
                                                                SharedPreferencesKeys
                                                                        .NIGHT_START_MINUTE
                                                                        .key,
                                                                m
                                                        )
                                                }
                                                AutoBrightnessManager.getInstance().updateSchedule()
                                        },
                                        nightStartHour,
                                        nightStartMinute,
                                        true
                                )
                        dialog.setOnDismissListener { showStartPicker = false }
                        dialog.show()
                }
        }
        if (showEndPicker) {
                LaunchedEffect(Unit) {
                        val dialog =
                                TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                                nightEndHour = h
                                                nightEndMinute = m
                                                prefs.edit {
                                                        putInt(
                                                                SharedPreferencesKeys.NIGHT_END_HOUR
                                                                        .key,
                                                                h
                                                        )
                                                        putInt(
                                                                SharedPreferencesKeys
                                                                        .NIGHT_END_MINUTE
                                                                        .key,
                                                                m
                                                        )
                                                }
                                                AutoBrightnessManager.getInstance().updateSchedule()
                                        },
                                        nightEndHour,
                                        nightEndMinute,
                                        true
                                )
                        dialog.setOnDismissListener { showEndPicker = false }
                        dialog.show()
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SteeringActionPicker(
        label: String,
        actionKey: String,
        packageName: String,
        climateCommandKey: String,
        onActionSelected: (String) -> Unit,
        onPackageChanged: (String) -> Unit,
        onClimateCommandSelected: (SteeringWheelClimateCommandType) -> Unit,
) {
        var expanded by remember { mutableStateOf(false) }
        var climateCommandExpanded by remember { mutableStateOf(false) }
        Text(label, color = Color(0xFFB0B8C4), fontSize = 14.sp)
        ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
        ) {
                TextField(
                        value = SteeringWheelCustomActionType.entries
                                .find { it.key == actionKey }?.description ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Ação") },
                        trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                ) {
                        SteeringWheelCustomActionType.entries.forEach { type ->
                                DropdownMenuItem(
                                        text = { Text(type.description) },
                                        onClick = {
                                                onActionSelected(type.key)
                                                expanded = false
                                        }
                                )
                        }
                }
        }
        if (actionKey == SteeringWheelCustomActionType.OPEN_APP.key) {
                AppSelectorField(
                        packageName = packageName,
                        onPackageSelected = onPackageChanged
                )
        }
        if (actionKey == SteeringWheelCustomActionType.CLIMATE_COMMAND.key) {
                SteeringWheelClimateCommandDropdown(
                        selectedCommandKey = climateCommandKey,
                        expanded = climateCommandExpanded,
                        onExpandedChange = { climateCommandExpanded = it },
                        onCommandSelected = { command ->
                                onClimateCommandSelected(command)
                                climateCommandExpanded = false
                        }
                )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SteeringWheelClimateCommandDropdown(
        selectedCommandKey: String,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        onCommandSelected: (SteeringWheelClimateCommandType) -> Unit
) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
                TextField(
                        value =
                                SteeringWheelClimateCommandType.entries
                                        .find { it.key == selectedCommandKey }
                                        ?.description
                                        ?: SteeringWheelClimateCommandType.TOGGLE_AC.description,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Comando do ar-condicionado") },
                        trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) }
                ) {
                        SteeringWheelClimateCommandType.entries.forEach { command ->
                                DropdownMenuItem(
                                        text = { Text(command.description) },
                                        onClick = { onCommandSelected(command) }
                                )
                        }
                }
        }
}

// Oculta/mostra o painel lateral esquerdo (navegação do sistema) via immersive policy_control (Shizuku).
// Portado do fork netseek. Ligado -> "settings put global policy_control immersive.navigation=*";
// desligado -> apaga a policy. O valor fica em settings global (persiste entre reboots).
private fun applyLeftNavPaneVisibility(hidden: Boolean) {
        Thread {
                        try {
                                val command =
                                        if (hidden)
                                                arrayOf("settings", "put", "global", "policy_control", "immersive.navigation=*")
                                        else arrayOf("settings", "delete", "global", "policy_control")
                                val result =
                                        br.com.redesurftank.havalshisuku.utils.ShizukuUtils
                                                .runCommandAndGetOutput(command)
                                android.util.Log.w("BasicSettingsScreen", "[NAV_PANE] hidden=$hidden result=$result")
                        } catch (t: Throwable) {
                                android.util.Log.e("BasicSettingsScreen", "applyLeftNavPaneVisibility failed", t)
                        }
                }
                .start()
}

private fun formatHms(epochSeconds: Long): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(epochSeconds * 1000L))
}

private fun loadWifiPriority(prefs: SharedPreferences): List<String> {
        return try {
                val arr =
                        org.json.JSONArray(
                                prefs.getString(SharedPreferencesKeys.WIFI_PRIORITY_LIST.key, "[]")
                        )
                (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
        } catch (t: Throwable) {
                emptyList()
        }
}

@Composable
private fun WifiPriorityEditor(prefs: SharedPreferences) {
        var savedNets by remember { mutableStateOf<List<String>>(emptyList()) }
        var priority by remember { mutableStateOf(loadWifiPriority(prefs)) }

        LaunchedEffect(Unit) {
                savedNets =
                        withContext(Dispatchers.IO) {
                                br.com.redesurftank.havalshisuku.managers.ServiceManager.getInstance()
                                        .listSavedWifi()
                                        .map { it.substringAfter('|') }
                                        .filter { it.isNotBlank() }
                                        .distinct()
                        }
        }

        var diagTrigger by remember { mutableStateOf(0) }
        var diagReport by remember { mutableStateOf("") }
        var diagBusy by remember { mutableStateOf(false) }
        LaunchedEffect(diagTrigger) {
                if (diagTrigger > 0) {
                        diagBusy = true
                        diagReport = "Verificando…"
                        diagReport =
                                withContext(Dispatchers.IO) {
                                        try {
                                                br.com.redesurftank.havalshisuku.managers
                                                        .WifiPriorityManager.getInstance()
                                                        .forceCheckNow()
                                        } catch (t: Throwable) {
                                                "Erro no diagnóstico: ${t.message ?: t}"
                                        }
                                }
                        diagBusy = false
                }
        }

        fun persist(newList: List<String>) {
                priority = newList
                prefs.edit()
                        .putString(
                                SharedPreferencesKeys.WIFI_PRIORITY_LIST.key,
                                org.json.JSONArray(newList).toString()
                        )
                        .apply()
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Ordem de prioridade (1 = preferida):", color = Color.Gray, fontSize = 12.sp)
                if (priority.isEmpty()) {
                        Text(
                                "Nenhuma rede priorizada. Adicione abaixo (precisa de 2+ pra valer).",
                                color = Color.Gray,
                                fontSize = 12.sp
                        )
                }
                priority.forEachIndexed { idx, ssid ->
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Text(
                                        "${idx + 1}. $ssid",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                        onClick = {
                                                if (idx > 0)
                                                        persist(
                                                                priority.toMutableList().also {
                                                                        it.add(idx - 1, it.removeAt(idx))
                                                                }
                                                        )
                                        },
                                        enabled = idx > 0
                                ) { Text("↑", fontSize = 16.sp) }
                                TextButton(
                                        onClick = {
                                                if (idx < priority.size - 1)
                                                        persist(
                                                                priority.toMutableList().also {
                                                                        it.add(idx + 1, it.removeAt(idx))
                                                                }
                                                        )
                                        },
                                        enabled = idx < priority.size - 1
                                ) { Text("↓", fontSize = 16.sp) }
                                TextButton(onClick = { persist(priority.filterNot { it == ssid }) }) {
                                        Text("remover", fontSize = 11.sp, color = Color(0xFFE24B4A))
                                }
                        }
                }
                val available = savedNets.filter { it !in priority }
                if (available.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Adicionar rede salva:", color = Color.Gray, fontSize = 12.sp)
                        available.forEach { ssid ->
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Text(
                                                ssid,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { persist(priority + ssid) }) {
                                                Text("+ adicionar", fontSize = 12.sp)
                                        }
                                }
                        }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                        onClick = { if (!diagBusy) diagTrigger++ },
                        enabled = !diagBusy
                ) {
                        Text(
                                if (diagBusy) "Verificando…" else "Verificar agora",
                                fontSize = 13.sp
                        )
                }
                if (diagReport.isNotEmpty()) {
                        Text(diagReport, color = Color.Gray, fontSize = 11.sp)
                }
        }
}
