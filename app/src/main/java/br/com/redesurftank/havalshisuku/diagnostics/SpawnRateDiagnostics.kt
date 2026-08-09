package br.com.redesurftank.havalshisuku.diagnostics

import android.content.Context
import br.com.redesurftank.havalshisuku.projectors.ClusterPerfEventLogger
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * INSTRUMENTACAO TEMPORARIA — mede a torneira de `am stack list` (Lote 4/5). Conta os spawns de shell
 * do Shizuku por categoria e, a cada 60s, grava um snapshot num arquivo proprio.
 *
 * POR QUE assim:
 *  - Log.* e removido pelo R8 em release/preview -> escreve em ARQUIVO
 *    (getExternalFilesDir/perf-diagnostics/perf-YYYYMMDD.log), independente do gate de diagnostico.
 *  - O sampler NAO pode contaminar a metrica: ele faz 1 spawn/min (le o system_server via root, que o
 *    app nao enxerga por hidepid) e marca esse comando com "PERFSELF" -> cai no contador perfSelf, NAO
 *    no amStack. Todo o resto (loadavg, meminfo, /proc/self) e leitura livre, sem spawn.
 *  - `recordSpawn` e chamado de dentro do ShizukuUtils (o funil do am stack list/dumpsys). So AtomicLong
 *    + alguns contains — barato o bastante pra rodar nos ~450 spawns/min.
 *
 * Ler no carro: cat .../files/perf-diagnostics/perf-YYYYMMDD.log (getExternalFilesDir do app)
 */
object SpawnRateDiagnostics {

    private val total = AtomicLong(0)
    private val amStack = AtomicLong(0)
    private val dumpsys = AtomicLong(0)
    private val amOther = AtomicLong(0)
    private val pm = AtomicLong(0)
    private val svc = AtomicLong(0)
    private val perfSelf = AtomicLong(0)
    private val other = AtomicLong(0)

    // Comandos distintos (normalizados) do minuto -> nomeia exatamente o que e o "other". Trocado por um
    // mapa novo a cada emissao (swap), entao cada linha TOP-CMD e o minuto corrente.
    @Volatile private var cmdCounts = ConcurrentHashMap<String, AtomicLong>()

    @Volatile private var started = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileDay = SimpleDateFormat("yyyyMMdd", Locale.US)

    /** Chamado de dentro do ShizukuUtils, uma vez por spawn. Classifica pelo comando. */
    @JvmStatic
    fun recordSpawn(command: Array<String>?) {
        total.incrementAndGet()
        val c = command?.joinToString(" ") ?: ""
        val bucket = when {
            c.contains("PERFSELF") -> perfSelf
            c.contains("am stack") -> amStack
            c.contains("dumpsys") -> dumpsys
            c.contains("pm ") -> pm
            c.contains("svc ") -> svc
            c.contains("am ") -> amOther
            else -> other
        }
        bucket.incrementAndGet()
        // Nomeia o comando (menos as nossas proprias leituras PERFSELF).
        if (!c.contains("PERFSELF")) {
            val k = normalizeCmd(command)
            val m = cmdCounts
            val e = m[k]
            if (e != null) e.incrementAndGet()
            else if (m.size < 400) m.computeIfAbsent(k) { AtomicLong(0) }.incrementAndGet()
        }
    }

    /** "sh -c 'am stack resize 5 0 0 800'" -> "am stack resize"; "pidof com.ts.carplay" fica igual. */
    private fun normalizeCmd(command: Array<String>?): String {
        if (command == null || command.isEmpty()) return "?"
        val inner =
                if (command.size >= 3 && command[0].endsWith("sh") && command[1] == "-c") command[2]
                else command.joinToString(" ")
        val seg = inner.substringBefore(";").substringBefore("&&").substringBefore("|")
                .substringBefore("2>").trim()
        val toks = seg.split(Regex("\\s+"))
                .filter { it.isNotEmpty() && !it.all { ch -> ch.isDigit() } && !it.startsWith("0x") }
        val key = toks.take(4).joinToString(" ")
        return (if (key.isEmpty()) seg else key).take(80)
    }

    @JvmStatic
    fun start(context: Context) {
        if (started) return
        started = true
        val appCtx = context.applicationContext
        scope.launch {
            writeLine(appCtx, "=== perf capture start (60s cadence) ===")
            var prev = counters()
            var prevAppBusy = readProcBusy("/proc/self/stat")
            var prevSysTotal = readSystemTotalJiffies()
            var prevSsBusy = -1L
            var lastSsPid = -1
            while (isActive) {
                delay(60_000L)
                val now = counters()
                val d = LongArray(now.size) { now[it] - prev[it] }
                prev = now

                val appBusy = readProcBusy("/proc/self/stat")
                val sysTotal = readSystemTotalJiffies()
                val appCpu = cpuPct(prevAppBusy, appBusy, prevSysTotal, sysTotal)

                val ss = readSystemServer() // 1 spawn (PERFSELF)
                val ssCpu = cpuPct(prevSsBusy, ss.busy, prevSysTotal, sysTotal)
                prevAppBusy = appBusy
                prevSsBusy = ss.busy
                prevSysTotal = sysTotal

                val restart = if (lastSsPid != -1 && ss.pid != -1 && ss.pid != lastSsPid)
                    "*SS_RESTARTED prev=$lastSsPid new=${ss.pid}* " else ""
                if (ss.pid != -1) lastSsPid = ss.pid

                val load = readLoad1()
                val sysRam = readSystemRamPct()
                val app = readSelfRssThreads()

                writeLine(appCtx, buildString {
                    append(restart)
                    append("SPAWNS/min total=").append(d[0])
                    append(" amStack=").append(d[1])
                    append(" dumpsys=").append(d[2])
                    append(" amOther=").append(d[3])
                    append(" pm=").append(d[4])
                    append(" svc=").append(d[5])
                    append(" other=").append(d[6])
                    append(" perfSelf=").append(d[7])
                    append(" (cumTotal=").append(now[0]).append(")")
                    append(" | load1=").append(load)
                    append(" sysRam=").append(sysRam).append("%")
                    append(" | APP rss=").append(app.first).append("MB thr=").append(app.second)
                    append(" cpu=").append(appCpu).append("%")
                    append(" | SS pid=").append(ss.pid).append(" rss=").append(ss.rssMb)
                    append("MB thr=").append(ss.threads).append(" cpu=").append(ssCpu).append("%")
                })

                // TOP comandos do minuto (nomeia o "other"): swap do mapa + dump dos 12 maiores.
                val cmdSnap = cmdCounts
                cmdCounts = ConcurrentHashMap()
                val topCmd = cmdSnap.entries.sortedByDescending { it.value.get() }.take(12)
                        .joinToString("  ") { "${it.key}=${it.value.get()}" }
                if (topCmd.isNotEmpty()) writeLine(appCtx, "TOP-CMD/min: $topCmd")
            }
        }
    }

    private fun counters() = longArrayOf(
            total.get(), amStack.get(), dumpsys.get(), amOther.get(),
            pm.get(), svc.get(), other.get(), perfSelf.get()
    )

    private fun cpuPct(prevBusy: Long, busy: Long, prevTotal: Long?, total: Long?): Int {
        if (prevBusy < 0 || busy < 0 || prevTotal == null || total == null) return -1
        val td = total - prevTotal
        if (td <= 0L) return -1
        return ((busy - prevBusy).coerceAtLeast(0L).toDouble() / td.toDouble() * 100.0).toInt().coerceIn(0, 100)
    }

    /** utime+stime de um /proc/<pid>/stat (reusa o parser do cluster). -1 se falhar. */
    private fun readProcBusy(path: String): Long =
            runCatching { File(path).readText() }.getOrNull()
                    ?.let { ClusterPerfEventLogger.parseProcessStatLine(it) } ?: -1L

    private fun readSystemTotalJiffies(): Long? =
            runCatching { File("/proc/stat").useLines { it.firstOrNull { l -> l.startsWith("cpu ") } } }
                    .getOrNull()?.let { ClusterPerfEventLogger.parseProcStatLine(it)?.totalJiffies }

    private fun readLoad1(): String =
            runCatching { File("/proc/loadavg").readText().trim().substringBefore(' ') }.getOrDefault("?")

    private fun readSystemRamPct(): Int {
        return runCatching {
            var total = 0L; var avail = 0L
            File("/proc/meminfo").useLines { lines ->
                for (l in lines) {
                    when {
                        l.startsWith("MemTotal:") -> total = kb(l)
                        l.startsWith("MemAvailable:") -> avail = kb(l)
                    }
                    if (total > 0 && avail > 0) return@useLines
                }
            }
            if (total <= 0) -1 else ((total - avail).coerceAtLeast(0L).toDouble() / total.toDouble() * 100.0).toInt()
        }.getOrDefault(-1)
    }

    /** Pair(rssMb, threads) do nosso processo. */
    private fun readSelfRssThreads(): Pair<Int, Int> {
        var rss = -1; var thr = -1
        runCatching {
            File("/proc/self/status").useLines { lines ->
                for (l in lines) {
                    when {
                        l.startsWith("VmRSS:") -> rss = (kb(l) / 1024L).toInt()
                        l.startsWith("Threads:") -> thr = l.filter { it.isDigit() }.toIntOrNull() ?: -1
                    }
                }
            }
        }
        return rss to thr
    }

    private class SsInfo(val pid: Int, val rssMb: Int, val threads: Int, val busy: Long)

    /** system_server via root (1 spawn, marcado PERFSELF). pid re-resolvido a cada vez (detecta restart). */
    private fun readSystemServer(): SsInfo {
        val out = runCatching {
            ShizukuUtils.runCommandAndGetOutput(arrayOf(
                    "sh", "-c",
                    "echo PERFSELF; P=\$(pidof system_server); echo SSPID=\$P; " +
                            "grep -E 'VmRSS|Threads' /proc/\$P/status 2>/dev/null; " +
                            "echo SSSTAT; cat /proc/\$P/stat 2>/dev/null"
            ))
        }.getOrDefault("")
        if (out.isBlank()) return SsInfo(-1, -1, -1, -1L)
        var pid = -1; var rss = -1; var thr = -1; var busy = -1L
        for (l in out.split('\n')) {
            when {
                l.startsWith("SSPID=") -> pid = l.substringAfter('=').trim().toIntOrNull() ?: -1
                l.startsWith("VmRSS:") -> rss = (kb(l) / 1024L).toInt()
                l.startsWith("Threads:") -> thr = l.filter { it.isDigit() }.toIntOrNull() ?: -1
                l.isNotEmpty() && l[0].isDigit() && l.contains('(') ->
                    busy = ClusterPerfEventLogger.parseProcessStatLine(l) ?: -1L
            }
        }
        return SsInfo(pid, rss, thr, busy)
    }

    /** "VmRSS:   204464 kB" -> 204464 */
    private fun kb(line: String): Long =
            line.split(Regex("\\s+")).firstNotNullOfOrNull { it.toLongOrNull() } ?: 0L

    private fun writeLine(context: Context, msg: String) {
        runCatching {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            val dir = File(base, "perf-diagnostics").apply { if (!exists()) mkdirs() }
            val f = File(dir, "perf-" + fileDay.format(Date()) + ".log")
            f.appendText(stamp.format(Date()) + " | " + msg + "\n")
        }
    }
}
