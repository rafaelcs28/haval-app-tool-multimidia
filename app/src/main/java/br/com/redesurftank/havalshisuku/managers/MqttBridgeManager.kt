package br.com.redesurftank.havalshisuku.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger
import br.com.redesurftank.havalshisuku.models.CarConstants
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys
import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Ponte MQTT → Home Assistant pela WiFi do head unit.
 *
 * Fase 1 (somente leitura): publica HA MQTT Discovery e os valores do carro lidos
 * via [ServiceManager.getUpdatedData]. Não escreve nada no carro nesta fase.
 *
 * Roda dentro do processo do ForegroundService (que mantém o app vivo) — não precisa
 * de um Service Android próprio. Toda E/S é em background; nada bloqueia a UI/composição.
 */
object MqttBridgeManager {
    private const val TAG = "MqttBridge"

    private const val DEFAULT_PORT_PLAIN = 1883
    private const val DEFAULT_PORT_TLS = 8883
    private const val DEFAULT_POLL_SEC = 20
    private const val DEFAULT_DEVICE_NAME = "Haval H6 — Local (WiFi)"
    private const val DEFAULT_DISCOVERY_PREFIX = "homeassistant"

    private val gson = Gson()

    // Executor serial só para conectar/desconectar/reconfigurar sem corrida.
    private val lifecycleExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "mqtt-bridge-lifecycle").apply { isDaemon = true }
    }
    private var pollExecutor: ScheduledExecutorService? = null
    private var pollFuture: ScheduledFuture<*>? = null

    @Volatile private var client: MqttAsyncClient? = null
    @Volatile private var appContext: Context? = null
    @Volatile private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var wifiOnly: Boolean = true
    @Volatile private var nodeId: String = "haval_h6_local"
    @Volatile private var stateBase: String = "haval/haval_h6_local"
    @Volatile private var availabilityTopic: String = "haval/haval_h6_local/availability"
    @Volatile private var discoveryPrefix: String = DEFAULT_DISCOVERY_PREFIX
    @Volatile private var deviceName: String = DEFAULT_DEVICE_NAME

    /** Último valor publicado por objectId, para só publicar quando muda. */
    private val lastPublished = ConcurrentHashMap<String, String>()

    // ----------------------------------------------------------------------------------
    // Tabela de sensores (Fase 1). component = "sensor" | "binary_sensor".
    // transform converte o valor cru do carro -> estado HA (binários: "ON"/"OFF").
    // Onde a semântica ainda é incerta, publicamos o valor CRU como sensor para calibrar.
    // ----------------------------------------------------------------------------------
    private data class SensorDef(
        val objectId: String,
        val name: String,
        val key: String,
        val component: String = "sensor",
        val deviceClass: String? = null,
        val unit: String? = null,
        val icon: String? = null,
        val stateClass: String? = null,
        val transform: ((String) -> String?)? = null
    )

    private val isOne: (String) -> String? = { v -> if (v.trim() == "1") "ON" else "OFF" }
    private val gtZero: (String) -> String? = { v -> if ((v.trim().toDoubleOrNull() ?: 0.0) > 0.0) "ON" else "OFF" }

    private fun sensorDefs(): List<SensorDef> = listOf(
        // --- Bateria / energia ---
        SensorDef("soc", "Estado de Carga (SoC)", CarConstants.CAR_EV_INFO_CAR_EV_INFO_SOC_OF_BATTERY.value, deviceClass = "battery", unit = "%", stateClass = "measurement"),
        SensorDef("battery_12v", "Estado de Carga 12V", CarConstants.CAR_BASIC_BATTERY_POWER_LEVEL.value, unit = "%", icon = "mdi:car-battery", stateClass = "measurement"),
        SensorDef("range_ev", "Autonomia EV", CarConstants.CAR_EV_INFO_ELECTRIC_MODE_REMAIN_ODOMETER.value, unit = "km", icon = "mdi:map-marker-distance"),
        SensorDef("range_fuel", "Autonomia Combustão", CarConstants.CAR_EV_INFO_FUEL_MODE_REMAIN_ODOMETER.value, unit = "km", icon = "mdi:map-marker-distance"),
        SensorDef("fuel_level", "Nível de Combustível", CarConstants.CAR_BASIC_REMAIN_FUEL_PERCENTAGE.value, unit = "%", icon = "mdi:gas-station", stateClass = "measurement"),
        SensorDef("odometer", "Quilometragem Total", CarConstants.CAR_BASIC_TOTAL_ODOMETER.value, unit = "km", icon = "mdi:counter", stateClass = "total_increasing"),
        // --- Carga (estado cru — calibrar na Fase 1b) ---
        SensorDef("charging_state", "Estado da Carga", CarConstants.CAR_EV_INFO_CHARGING_STATE.value, icon = "mdi:ev-station"),
        SensorDef("charge_remaining", "Tempo de Carga", CarConstants.CAR_EV_INFO_CHARGE_REMAINING_TIME.value, unit = "min", icon = "mdi:timer-outline"),
        SensorDef("charge_gun", "Cabo Conectado", CarConstants.CAR_EV_INFO_CHARGING_GUN_CONN_STATE.value, component = "binary_sensor", deviceClass = "plug", transform = isOne),
        // --- Temperaturas (valor cru — calibrar escala depois) ---
        SensorDef("inside_temp", "Temperatura da Cabine", CarConstants.CAR_BASIC_INSIDE_TEMP.value, deviceClass = "temperature", unit = "°C", stateClass = "measurement"),
        SensorDef("outside_temp", "Temperatura Externa", CarConstants.CAR_BASIC_OUTSIDE_TEMP.value, deviceClass = "temperature", unit = "°C", stateClass = "measurement"),
        // --- Motor / ignição ---
        SensorDef("engine_state", "Estado do Motor", CarConstants.CAR_BASIC_ENGINE_STATE.value, icon = "mdi:engine"),
        SensorDef("gear", "Marcha", CarConstants.CAR_BASIC_GEAR_STATUS.value, icon = "mdi:car-shift-pattern"),
        SensorDef("drive_mode", "Modo de Condução", CarConstants.CAR_EV_SETTING_POWER_MODEL_CONFIG.value, icon = "mdi:car-cog"),
        // --- Climatização / conforto (binários conhecidos do AcControlScreen) ---
        SensorDef("ac_state", "Estado do Ar Condicionado", CarConstants.CAR_HVAC_POWER_MODE.value, component = "binary_sensor", icon = "mdi:air-conditioner", transform = isOne),
        SensorDef("defrost_front", "Desembaçador Dianteiro", CarConstants.CAR_HVAC_FRONT_DEFROST_ENABLE.value, component = "binary_sensor", icon = "mdi:car-defrost-front", transform = isOne),
        SensorDef("defrost_rear", "Desembaçador Traseiro", CarConstants.CAR_HVAC_REAR_DEFROST_ENABLE.value, component = "binary_sensor", icon = "mdi:car-defrost-rear", transform = isOne),
        SensorDef("seat_heat_driver", "Aquecimento Banco Motorista", CarConstants.CAR_COMFORT_SETTING_DRIVER_SEAT_HEATING_LEVEL.value, component = "binary_sensor", icon = "mdi:car-seat-heater", transform = gtZero),
        SensorDef("seat_heat_pass", "Aquecimento Banco Passageiro", CarConstants.CAR_COMFORT_SETTING_PASSENGER_SEAT_HEATING_LEVEL.value, component = "binary_sensor", icon = "mdi:car-seat-heater", transform = gtZero),
        SensorDef("air_purifier", "Purificador de Ar", CarConstants.CAR_HVAC_ANION_ENABLE.value, component = "binary_sensor", icon = "mdi:air-purifier", transform = isOne),
        SensorDef("sunroof", "Posição do Teto Solar", CarConstants.CAR_BASIC_SUNROOF_STATUS.value, icon = "mdi:car-select"),
        // --- Trava (estado cru — confirmar polaridade na Fase 1b antes de virar lock) ---
        SensorDef("lock_raw", "Estado da Trava (cru)", CarConstants.CAR_BASIC_DOOR_LOCK_STATUS.value, icon = "mdi:lock"),
        // --- Compostos: publicar CRU p/ decodificar por posição depois ---
        SensorDef("doors_raw", "Portas (cru)", CarConstants.CAR_BASIC_DOOR_STATUS.value, icon = "mdi:car-door"),
        SensorDef("windows_raw", "Vidros (cru)", CarConstants.CAR_BASIC_WINDOW_STATUS.value, icon = "mdi:car-door"),
        SensorDef("tpms_raw", "Pneus / TPMS (cru)", CarConstants.CAR_BASIC_TPMS_STATUS.value, icon = "mdi:car-tire-alert")
    )

    // ----------------------------------------------------------------------------------
    // Ciclo de vida
    // ----------------------------------------------------------------------------------

    /** Chamado pelo ForegroundService quando os serviços do carro estão prontos. */
    @JvmStatic
    fun startIfEnabled(context: Context) {
        appContext = context.applicationContext
        lifecycleExecutor.execute { reevaluate() }
    }

    /** Reaplica a config (chamado pela tela de ajustes ao salvar). */
    @JvmStatic
    fun notifyConfigChanged(context: Context) {
        appContext = context.applicationContext
        lifecycleExecutor.execute {
            disconnectInternal("config-changed")
            // pequeno respiro para o broker liberar o clientId antigo
            try { Thread.sleep(400) } catch (ignored: InterruptedException) {}
            reevaluate()
        }
    }

    @JvmStatic
    fun stop() {
        lifecycleExecutor.execute { teardown("stop") }
    }

    /**
     * Decide o estado certo da ponte conforme as configs + a rede atual. Idempotente:
     * roda na inicialização, ao salvar a config, e a cada mudança de rede. Sempre no
     * lifecycleExecutor (serial), então não há corrida entre conectar/desconectar.
     */
    private fun reevaluate() {
        try {
            val prefs = prefs()
            if (!prefs.getBoolean(SharedPreferencesKeys.ENABLE_MQTT_BRIDGE.key, false)) {
                teardown("disabled")
                return
            }
            val host = prefs.getString(SharedPreferencesKeys.MQTT_BROKER_HOST.key, "")?.trim().orEmpty()
            if (host.isEmpty()) {
                Log.w(TAG, "MQTT bridge enabled but broker host is empty; not connecting.")
                teardown("no-host")
                return
            }
            wifiOnly = prefs.getBoolean(SharedPreferencesKeys.MQTT_WIFI_ONLY.key, true)
            if (wifiOnly) registerNetworkMonitor() else unregisterNetworkMonitor()

            val networkOk = !wifiOnly || isWifiActive()
            if (networkOk) {
                if (client == null) connectInternal(host)
            } else if (client != null) {
                Log.w(TAG, "Não está na WiFi — pausando a ponte (o 4G/gwmbrasil cobre).")
                ClusterPersistentEventLogger.logText("mqtt_paused_no_wifi", "wifiOnly=true")
                disconnectInternal("no-wifi")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "reevaluate failed: ${t.message}", t)
            ClusterPersistentEventLogger.logText("mqtt_reevaluate_failed", t.message ?: "unknown")
        }
    }

    private fun teardown(reason: String) {
        unregisterNetworkMonitor()
        disconnectInternal(reason)
    }

    private fun onNetworkChanged() {
        lifecycleExecutor.execute { reevaluate() }
    }

    /** True se a rede ativa do head unit é WiFi (para nunca publicar via 4G). */
    private fun isWifiActive(): Boolean {
        val ctx = appContext ?: return false
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun registerNetworkMonitor() {
        if (networkCallback != null) return
        val ctx = appContext ?: return
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onNetworkChanged()
            override fun onLost(network: Network) = onNetworkChanged()
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = onNetworkChanged()
        }
        networkCallback = cb
        try {
            cm.registerDefaultNetworkCallback(cb)
            Log.w(TAG, "Modo só-WiFi: monitorando mudanças de rede.")
        } catch (t: Throwable) {
            networkCallback = null
            Log.e(TAG, "registerDefaultNetworkCallback failed: ${t.message}", t)
        }
    }

    private fun unregisterNetworkMonitor() {
        val cb = networkCallback ?: return
        networkCallback = null
        try {
            val cm = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.unregisterNetworkCallback(cb)
        } catch (ignored: Throwable) {}
    }

    private fun connectInternal(host: String) {
        if (client != null) {
            Log.d(TAG, "Client already present; skip connect.")
            return
        }
        val prefs = prefs()
        val useTls = prefs.getBoolean(SharedPreferencesKeys.MQTT_USE_TLS.key, false)
        val port = prefs.getString(SharedPreferencesKeys.MQTT_BROKER_PORT.key, "")?.trim()
            ?.toIntOrNull() ?: if (useTls) DEFAULT_PORT_TLS else DEFAULT_PORT_PLAIN
        val user = prefs.getString(SharedPreferencesKeys.MQTT_USERNAME.key, "")?.trim().orEmpty()
        val pass = prefs.getString(SharedPreferencesKeys.MQTT_PASSWORD.key, "").orEmpty()
        discoveryPrefix = prefs.getString(SharedPreferencesKeys.MQTT_DISCOVERY_PREFIX.key, DEFAULT_DISCOVERY_PREFIX)
            ?.trim()?.ifEmpty { DEFAULT_DISCOVERY_PREFIX } ?: DEFAULT_DISCOVERY_PREFIX
        deviceName = prefs.getString(SharedPreferencesKeys.MQTT_DEVICE_NAME.key, DEFAULT_DEVICE_NAME)
            ?.trim()?.ifEmpty { DEFAULT_DEVICE_NAME } ?: DEFAULT_DEVICE_NAME

        nodeId = sanitizeId(deviceName).ifEmpty { "haval_h6_local" }
        stateBase = "haval/$nodeId"
        availabilityTopic = "$stateBase/availability"

        val scheme = if (useTls) "ssl" else "tcp"
        val uri = "$scheme://$host:$port"
        val clientId = "havalshisuku-$nodeId"

        val opts = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true
            connectionTimeout = 15
            keepAliveInterval = 45
            if (user.isNotEmpty()) {
                userName = user
                password = pass.toCharArray()
            }
            // Last-Will: se a ponte cair, o HA marca as entidades como indisponíveis.
            setWill(availabilityTopic, "offline".toByteArray(), 1, true)
        }

        Log.w(TAG, "Connecting to $uri as $clientId ...")
        ClusterPersistentEventLogger.logText("mqtt_connecting", "uri=$uri tls=$useTls user=${user.isNotEmpty()}")

        val c = MqttAsyncClient(uri, clientId, MemoryPersistence())
        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.w(TAG, "MQTT connected (reconnect=$reconnect) to $serverURI")
                ClusterPersistentEventLogger.logText("mqtt_connected", "reconnect=$reconnect")
                onConnected()
            }

            override fun connectionLost(cause: Throwable?) {
                Log.e(TAG, "MQTT connection lost: ${cause?.message}")
                ClusterPersistentEventLogger.logText("mqtt_conn_lost", cause?.message ?: "unknown")
                // automaticReconnect cuida do retorno; pausamos o polling até reconectar.
                stopPolling()
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                // Fase 1 é somente leitura — comandos chegam nas próximas fases.
            }
        })
        client = c
        try {
            c.connect(opts)
        } catch (t: Throwable) {
            Log.e(TAG, "MQTT connect threw: ${t.message}", t)
            ClusterPersistentEventLogger.logText("mqtt_connect_threw", t.message ?: "unknown")
        }
    }

    private fun onConnected() {
        try {
            lastPublished.clear()
            publishDiscovery()
            publish(availabilityTopic, "online", retained = true)
            startPolling()
        } catch (t: Throwable) {
            Log.e(TAG, "onConnected failed: ${t.message}", t)
        }
    }

    private fun disconnectInternal(reason: String) {
        stopPolling()
        val c = client ?: return
        client = null
        try {
            if (c.isConnected) {
                try { c.publish(availabilityTopic, MqttMessage("offline".toByteArray()).apply { qos = 1; isRetained = true }) } catch (ignored: Throwable) {}
                c.disconnectForcibly(500, 500)
            }
            c.close()
        } catch (t: Throwable) {
            Log.w(TAG, "disconnect ($reason) error: ${t.message}")
        }
        Log.w(TAG, "MQTT disconnected ($reason).")
    }

    // ----------------------------------------------------------------------------------
    // Discovery + polling
    // ----------------------------------------------------------------------------------

    private fun deviceBlock(): Map<String, Any> = linkedMapOf(
        "identifiers" to listOf(nodeId),
        "name" to deviceName,
        "manufacturer" to "GWM",
        "model" to "HAVAL H6 PHEV"
    )

    private fun publishDiscovery() {
        val dev = deviceBlock()
        for (s in sensorDefs()) {
            val cfg = linkedMapOf<String, Any?>(
                "name" to s.name,
                "unique_id" to "${nodeId}_${s.objectId}",
                "object_id" to "${nodeId}_${s.objectId}",
                "state_topic" to "$stateBase/${s.objectId}/state",
                "availability_topic" to availabilityTopic,
                "device" to dev
            )
            s.deviceClass?.let { cfg["device_class"] = it }
            s.unit?.let { cfg["unit_of_measurement"] = it }
            s.icon?.let { cfg["icon"] = it }
            s.stateClass?.let { cfg["state_class"] = it }
            val topic = "$discoveryPrefix/${s.component}/$nodeId/${s.objectId}/config"
            publish(topic, gson.toJson(cfg), retained = true)
        }
        Log.w(TAG, "Published HA discovery for ${sensorDefs().size} entities under '$nodeId'.")
        ClusterPersistentEventLogger.logText("mqtt_discovery", "entities=${sensorDefs().size} node=$nodeId")
    }

    private fun startPolling() {
        stopPolling()
        val interval = (prefs().getString(SharedPreferencesKeys.MQTT_POLL_INTERVAL_SEC.key, "")?.trim()
            ?.toIntOrNull() ?: DEFAULT_POLL_SEC).coerceIn(5, 600)
        val exec = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "mqtt-bridge-poll").apply { isDaemon = true }
        }
        pollExecutor = exec
        pollFuture = exec.scheduleWithFixedDelay({ safePollOnce() }, 0, interval.toLong(), TimeUnit.SECONDS)
        Log.w(TAG, "MQTT polling started (every ${interval}s).")
    }

    private fun stopPolling() {
        pollFuture?.cancel(false)
        pollFuture = null
        pollExecutor?.shutdownNow()
        pollExecutor = null
    }

    private fun safePollOnce() {
        try {
            val c = client ?: return
            if (!c.isConnected) return
            val sm = ServiceManager.getInstance()
            for (s in sensorDefs()) {
                val raw = try { sm.getUpdatedData(s.key) } catch (t: Throwable) { null } ?: continue
                val state = try { s.transform?.invoke(raw) ?: raw } catch (t: Throwable) { raw } ?: continue
                if (lastPublished[s.objectId] == state) continue
                lastPublished[s.objectId] = state
                publish("$stateBase/${s.objectId}/state", state, retained = true)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "poll error: ${t.message}", t)
        }
    }

    private fun publish(topic: String, payload: String, retained: Boolean) {
        val c = client ?: return
        try {
            if (!c.isConnected) return
            c.publish(topic, MqttMessage(payload.toByteArray()).apply { qos = 0; isRetained = retained })
        } catch (t: Throwable) {
            Log.w(TAG, "publish to $topic failed: ${t.message}")
        }
    }

    // ----------------------------------------------------------------------------------
    private fun prefs() = App.getDeviceProtectedContext()
        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)

    private fun sanitizeId(name: String): String =
        name.lowercase()
            .map { if (it.isLetterOrDigit()) it else '_' }
            .joinToString("")
            .replace(Regex("_+"), "_")
            .trim('_')
}
