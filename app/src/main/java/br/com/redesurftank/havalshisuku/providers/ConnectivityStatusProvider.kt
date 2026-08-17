package br.com.redesurftank.havalshisuku.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.managers.ConnectivityStatusManager
import br.com.redesurftank.havalshisuku.managers.MobileDataManager
import br.com.redesurftank.havalshisuku.managers.ServiceManager
import br.com.redesurftank.havalshisuku.managers.WifiPriorityManager
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys
import br.com.redesurftank.havalshisuku.utils.HeadUnitResourceSampler
import br.com.redesurftank.havalshisuku.utils.ProcessUsageSampler

/**
 * Expõe o estado de conectividade + o CONTROLE COMPLETO (4G/dados + WiFi) pra outro app (EcoTrip)
 * LER via query() e COMANDAR via call(). Tudo roda no NOSSO processo.
 *
 * URI:  content://br.com.redesurftank.havalshisuku.status/connectivity
 * Proteção: guard por pacote (getCallingPackage) no query() E no call() — só EcoTrip e o próprio app.
 *
 * COMANDOS (call, method; extras: value:bool / mb:int / day:int / ssid+password:str). Cada um devolve
 * um Bundle com {ok:bool, [error:str]} + um SNAPSHOT do estado resultante (confirmação pro EcoTrip):
 *   4G/dados:  setMobileControl, setMobileBlock, setBlockOnWifi, setBlockOnProjection, setAutoblock,
 *              setDataLimit (extra mb:int ou gb:int), setCycleDay (extra day:int)
 *   WiFi:      scanWifi (só visíveis agora), addWifi (ssid+password), connectWifi (arg=ssid salvo),
 *              setWifiEnabled (value:bool — CUIDADO: off pode cortar o canal remoto), setWifiPriority,
 *              listSavedWifi
 *   Recursos:  resourceUsage — CPU% e RAM% do head unit + RSS do processo do app (MB), lidos do /proc.
 *              Resposta enxuta {ok, cpuPct, ramPct, appRamMb, sampledAtMs} (sem o snapshot de
 *              conectividade). extra intervalMs:long opcional (100..2000, default 500) = janela do %
 *              de CPU. BLOQUEIA ~intervalMs (2 leituras de /proc/stat). Amostra independente do overlay.
 *              processUsage — quebra POR PROCESSO (quem consome o que). Extras opcionais: topN:int
 *              (1..500, def 10), orderBy:str ("rss"|"cpu", def "rss"), intervalMs:long (100..2000,
 *              def 500), includeSystem:bool (def false = so apps). Resposta {ok, sampledAtMs,
 *              totalProcs, nCores, memKind="rss", cpuScale="system_total", processes:ArrayList<String>}
 *              com cada item um JSON {pkg,pid,rssMb,cpuPct,state,uid}. Le /proc CRU via Shizuku root
 *              (sem top/dumpsys). BLOQUEIA ~intervalMs. memoria = RSS (nao PSS); cpuPct = % do total.
 * Toda mudança dispara notifyChanged -> broadcast CONNECTIVITY_CHANGED pro EcoTrip re-ler.
 */
class ConnectivityStatusProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    private fun authorized(ctx: Context): Boolean {
        val caller = callingPackage
        return caller == null || caller == CONSUMER_PKG || caller == ctx.packageName
    }

    override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
    ): Cursor {
        val ctx = context!!
        if (!authorized(ctx)) return MatrixCursor(COLS, 0)
        val s = ConnectivityStatusManager.computeFresh(ctx)
        val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        val cur = MatrixCursor(COLS, 1)
        cur.addRow(arrayOf<Any?>(
                if (s.hotspotRouting) 1 else 0,
                s.routingMode,
                s.routingWifiName,
                if (s.mobileControlEnabled) 1 else 0,
                if (s.mobile4gOn) 1 else 0,
                s.mobileBlockReason,
                s.displayText,
                s.displayLevel,
                s.displayIcon,
                if (prefs.getBoolean(SharedPreferencesKeys.WIFI_PRIORITY_ENABLED.key, false)) 1 else 0,
                currentWifiSsid(ctx),
                if (MobileDataManager.isManualBlock()) 1 else 0,
                if (MobileDataManager.isBlockOnWifi()) 1 else 0,
                if (MobileDataManager.isAutoblockEnabled()) 1 else 0,
                MobileDataManager.getAutoblockCapMb(),
                MobileDataManager.getCycleDay(),
                MobileDataManager.getMobileUsedMbThisCycle(ctx, System.currentTimeMillis()).toInt()
        ))
        return cur
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = context!!
        val res = Bundle()
        if (!authorized(ctx)) {
            res.putBoolean("ok", false); res.putString("error", "unauthorized"); return res
        }
        fun boolArg() = extras?.getBoolean("value") ?: (arg == "true")
        try {
            when (method) {
                // ---------- 4G / dados móveis ----------
                "setMobileControl" -> { MobileDataManager.setControlEnabled(boolArg()); res.putBoolean("ok", true) }
                "setMobileBlock" -> {
                    val block = boolArg()
                    if (block) { MobileDataManager.setControlEnabled(true); MobileDataManager.setManualBlock(true) }
                    else MobileDataManager.setManualBlock(false)
                    res.putBoolean("ok", true)
                }
                "setBlockOnWifi" -> { MobileDataManager.setBlockOnWifi(boolArg()); res.putBoolean("ok", true) }
                "setBlockOnProjection" -> { MobileDataManager.setBlockOnProjection(boolArg()); res.putBoolean("ok", true) }
                "setAutoblock" -> { MobileDataManager.setAutoblockEnabled(boolArg()); res.putBoolean("ok", true) }
                "setDataLimit" -> {
                    val mb = when {
                        extras?.containsKey("mb") == true -> extras.getInt("mb")
                        extras?.containsKey("gb") == true -> extras.getInt("gb") * 1024
                        arg != null -> (arg.toDoubleOrNull()?.times(1024))?.toInt() ?: -1 // arg em GB
                        else -> -1
                    }
                    if (mb < 0) { res.putBoolean("ok", false); res.putString("error", "limite invalido (use mb:int ou gb:int)") }
                    else { MobileDataManager.setAutoblockCapMb(mb); res.putBoolean("ok", true) }
                }
                "setCycleDay" -> {
                    val day = extras?.getInt("day") ?: arg?.toIntOrNull() ?: -1
                    if (day < 1 || day > 31) { res.putBoolean("ok", false); res.putString("error", "dia invalido (1-31)") }
                    else { MobileDataManager.setCycleDay(day); res.putBoolean("ok", true) }
                }
                // ---------- WiFi ----------
                "setWifiPriority" -> { WifiPriorityManager.getInstance().setFeatureEnabled(boolArg()); res.putBoolean("ok", true) }
                "setWifiEnabled" -> {
                    val on = boolArg()
                    val ok = ServiceManager.getInstance().setCarWifiEnabled(on)
                    res.putBoolean("ok", ok)
                    if (!on) res.putString("warning", "WiFi desligado — se o carro so tem internet por WiFi, o canal remoto cai ate religar por 4G/no carro")
                }
                "scanWifi" -> {
                    res.putStringArrayList("networks", ArrayList(scanVisible(ctx))); res.putBoolean("ok", true)
                }
                "addWifi" -> {
                    val ssid = extras?.getString("ssid") ?: arg
                    val pw = extras?.getString("password")
                    val netId = if (ssid.isNullOrBlank()) -1 else ServiceManager.getInstance().addWifiNetwork(ssid, pw)
                    if (netId < 0) { res.putBoolean("ok", false); res.putString("error", "falha ao cadastrar '$ssid'") }
                    else {
                        res.putBoolean("ok", true); res.putInt("netId", netId)
                        try { ConnectivityStatusManager.notifyChanged(ctx) } catch (_: Throwable) {}
                    }
                }
                "connectWifi" -> {
                    val ssid = arg ?: extras?.getString("ssid")
                    val netId = if (ssid == null) -1 else netIdForSsid(ssid)
                    if (netId < 0) { res.putBoolean("ok", false); res.putString("error", "ssid nao salvo: $ssid") }
                    else {
                        Thread({
                            ServiceManager.getInstance().switchWifiToNetwork(netId)
                            try { ConnectivityStatusManager.notifyChanged(ctx) } catch (_: Throwable) {}
                        }, "ecotrip-wifi-switch").start()
                        res.putBoolean("ok", true); res.putInt("netId", netId)
                    }
                }
                "listSavedWifi" -> {
                    val ssids = ServiceManager.getInstance().listSavedWifi()
                            .mapNotNull { it.substringAfter('|').takeIf { s -> s.isNotBlank() } }.distinct()
                    res.putStringArrayList("ssids", ArrayList(ssids)); res.putBoolean("ok", true)
                }
                // ---------- Recursos (CPU/RAM) — leitura sob demanda pro EcoTrip salvar/analisar ----------
                "resourceUsage" -> {
                    val intervalMs = extras?.getLong("intervalMs")?.takeIf { it in 100L..2000L } ?: 500L
                    val snap = HeadUnitResourceSampler.sampleOnceBlocking(intervalMs)
                    res.putBoolean("ok", true)
                    snap.cpuPct?.let { res.putInt("cpuPct", it) }
                    snap.ramPct?.let { res.putInt("ramPct", it) }
                    snap.appRamMb?.let { res.putInt("appRamMb", it) }
                    res.putLong("sampledAtMs", System.currentTimeMillis())
                    return res // resposta enxuta: sem o snapshot de conectividade
                }
                // ---------- Recursos POR PROCESSO — "quem consome o que" no head unit ----------
                "processUsage" -> {
                    val topN = extras?.getInt("topN")?.takeIf { it in 1..500 } ?: 10
                    val orderBy = extras?.getString("orderBy")?.lowercase()
                            ?.takeIf { it == "cpu" || it == "rss" || it == "pss" } ?: "rss"
                    val intervalMs = extras?.getLong("intervalMs")?.takeIf { it in 100L..2000L } ?: 500L
                    val includeSystem = extras?.getBoolean("includeSystem") ?: false
                    val snap = ProcessUsageSampler.sample(topN, orderBy, intervalMs, includeSystem)
                    if (snap == null) {
                        res.putBoolean("ok", false)
                        res.putString("error", "shizuku indisponivel ou leitura de /proc falhou")
                    } else {
                        res.putBoolean("ok", true)
                        res.putLong("sampledAtMs", snap.sampledAtMs)
                        res.putInt("totalProcs", snap.totalProcs)
                        res.putInt("nCores", Runtime.getRuntime().availableProcessors())
                        res.putString("memKind", "rss") // devolvemos RSS, NAO PSS (ver ProcessUsageSampler)
                        res.putString("cpuScale", "system_total") // cpuPct = % do total (todos os nucleos)
                        res.putStringArrayList("processes", ArrayList(snap.processes.map { it.toJson() }))
                    }
                    return res // resposta enxuta: sem o snapshot de conectividade
                }
                // ---------- Canais virtuais (passa-a-diante do VirtualTelemetry: navegação do AA etc.) ----------
                // Genérico: o consumidor pede uma key app.* e recebe {ok, key, json, updatedAtMs}.
                // updatedAtMs = carimbo da última atualização do canal (crítico pra descartar dado velho).
                // "navDirections" é o atalho tipado equivalente a virtualValue(app.navigation.directions).
                "virtualValue", "navDirections" -> {
                    val key = if (method == "navDirections") "app.navigation.directions"
                              else (extras?.getString("key") ?: arg)?.trim().orEmpty()
                    if (!key.startsWith("app.")) {
                        res.putBoolean("ok", false)
                        res.putString("error", "key nao permitida (use app.*): '$key'")
                    } else {
                        val json = try {
                            br.com.redesurftank.havalshisuku.bridge.VirtualTelemetryManager.getVirtualValue(ctx, key)
                        } catch (_: Throwable) { "" }
                        val updatedAtMs =
                            if (key == "app.navigation.directions")
                                br.com.redesurftank.havalshisuku.bridge.AndroidAutoNavManager.getDirectionsUpdatedAtMs()
                            else System.currentTimeMillis()
                        res.putBoolean("ok", true)
                        res.putString("key", key)
                        res.putString("json", json)
                        res.putLong("updatedAtMs", updatedAtMs)
                    }
                    return res // resposta enxuta: sem snapshot de conectividade
                }
                else -> { res.putBoolean("ok", false); res.putString("error", "metodo desconhecido: $method") }
            }
        } catch (t: Throwable) {
            res.putBoolean("ok", false); res.putString("error", t.message ?: t.toString())
        }
        // SNAPSHOT do estado resultante em TODA resposta (confirmação pro EcoTrip)
        try { snapshot(res, ctx) } catch (_: Throwable) {}
        return res
    }

    /** Estado completo atual -> confirmação. O EcoTrip compara com o que pediu pra saber se "deu certo". */
    private fun snapshot(res: Bundle, ctx: Context) {
        res.putBoolean("mobileControlEnabled", MobileDataManager.isControlEnabled())
        res.putBoolean("mobileManualBlock", MobileDataManager.isManualBlock())
        res.putBoolean("mobileBlockOnWifi", MobileDataManager.isBlockOnWifi())
        res.putBoolean("mobileBlockOnProjection", MobileDataManager.isBlockOnProjection())
        res.putBoolean("mobileAutoblock", MobileDataManager.isAutoblockEnabled())
        res.putInt("mobileLimitMb", MobileDataManager.getAutoblockCapMb())
        res.putInt("mobileCycleDay", MobileDataManager.getCycleDay())
        val reason = MobileDataManager.blockReason(ctx)
        res.putString("mobileBlockReason", reason)
        res.putBoolean("mobileBlocked", reason != null)
        val prefs = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        res.putBoolean("wifiPriorityEnabled", prefs.getBoolean(SharedPreferencesKeys.WIFI_PRIORITY_ENABLED.key, false))
        res.putString("headUnitWifiSsid", currentWifiSsid(ctx))
        // Consumo da MULTIMÍDIA (esta tela) no ciclo atual, em MB. Fonte: NetworkStatsManager
        // (querySummaryForDevice TYPE_MOBILE) com fallback TrafficStats; NÃO inclui o TBOX (modem
        // separado, fora da interface móvel da multimídia). Zera na virada do mobileCycleDay.
        res.putInt("mobileUsedMb", MobileDataManager.getMobileUsedMbThisCycle(ctx, System.currentTimeMillis()).toInt())
    }

    /** Redes WiFi VISÍVEIS agora (último scan), como "SSID|nivel|seguraça(0/1)", melhor sinal primeiro. */
    private fun scanVisible(ctx: Context): List<String> {
        return try {
            val wm = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return emptyList()
            requestScanThrottled(wm)
            (wm.scanResults ?: emptyList<ScanResult>())
                    .filter { !it.SSID.isNullOrBlank() }
                    .groupBy { it.SSID }
                    .values
                    .map { grp -> grp.maxByOrNull { it.level }!! }
                    .sortedByDescending { it.level }
                    .map { "${it.SSID}|${it.level}|${if (isSecured(it)) 1 else 0}" }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    /**
     * O Android só aceita ~4 startScan por 2 minutos; além disso ele REJEITA e o framework
     * registra uma linha "WifiService: Failed to start scan" por chamada. Como este provider é
     * consultado de fora (EcoTrip), um consumidor em laço transformava cada consulta em uma
     * tentativa de scan — e um flood de ~130 linhas/s foi observado no carro, afogando o logcat
     * e queimando CPU no system_server (report 20260817-083131).
     *
     * Pedir mais que isto não traz resultado novo: `scanResults` devolve o último scan de
     * qualquer forma. Contamos as tentativas suprimidas para saber, no próximo relatório, se a
     * origem do flood somos nós ou outro app.
     */
    @Volatile private var lastScanRequestMs = 0L
    @Volatile private var suppressedScanRequests = 0
    private val SCAN_MIN_INTERVAL_MS = 30_000L

    private fun requestScanThrottled(wm: WifiManager) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastScanRequestMs < SCAN_MIN_INTERVAL_MS) {
            val n = ++suppressedScanRequests
            // Só marca em potências de 10: se aparecer, a taxa é anormal e a origem é nossa.
            if (n == 10 || n == 100 || n == 1000 || n == 10000) {
                br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger.log(
                        "wifi_scan_throttled",
                        mapOf("suppressed" to n, "windowMs" to SCAN_MIN_INTERVAL_MS)
                )
            }
            return
        }
        lastScanRequestMs = now
        suppressedScanRequests = 0
        try { wm.startScan() } catch (_: Throwable) {}
    }

    private fun isSecured(r: ScanResult): Boolean {
        val c = r.capabilities ?: ""
        return c.contains("WPA") || c.contains("WEP") || c.contains("PSK") || c.contains("EAP")
    }

    private fun netIdForSsid(ssid: String): Int {
        for (e in ServiceManager.getInstance().listSavedWifi()) {
            val bar = e.indexOf('|')
            if (bar > 0 && e.substring(bar + 1) == ssid) return e.substring(0, bar).toIntOrNull() ?: -1
        }
        return -1
    }

    private fun currentWifiSsid(ctx: Context): String? {
        return try {
            val wm = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            var s = wm?.connectionInfo?.ssid
            if (s != null && s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length - 1)
            if (s.isNullOrBlank() || s == "<unknown ssid>") null else s
        } catch (t: Throwable) {
            null
        }
    }

    override fun getType(uri: Uri): String =
            "vnd.android.cursor.item/vnd.br.com.redesurftank.havalshisuku.connectivity"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val CONSUMER_PKG = "br.com.redesurftank.ecotrip"
        private val COLS = arrayOf(
                "hotspotRouting", "routingMode", "routingWifiName",
                "mobileControlEnabled", "mobile4gOn", "mobileBlockReason",
                "displayText", "displayLevel", "displayIcon",
                "wifiPriorityEnabled", "headUnitWifiSsid",
                // Regras de corte + consumo do ciclo, pro EcoTrip LER por query() em vez de
                // martelar call() a cada 4s (handoff mobileUsedMb).
                "mobileManualBlock", "mobileBlockOnWifi", "mobileAutoblock",
                "mobileLimitMb", "mobileCycleDay", "mobileUsedMb"
        )
    }
}
