package br.com.redesurftank.havalshisuku.utils

import br.com.redesurftank.havalshisuku.projectors.ClusterPerfEventLogger
import org.json.JSONObject

/**
 * Amostra o consumo de CPU/RAM POR PROCESSO do head unit, sob demanda, pra outro app (EcoTrip)
 * salvar/analisar. Complementa o [HeadUnitResourceSampler] (que da so o agregado do sistema).
 *
 * COMO (e por que assim, num sistema ja saturado — load ~16):
 *  - UMA passada privilegiada por chamada via [ShizukuUtils.runCommandAndGetOutput] (root pelo Shizuku):
 *    um unico `sh -c` que le /proc CRU com glob (`cat`/`grep`), SEM `top`/`dumpsys` e SEM laco que
 *    forka um processo por pid. Sao ~5 processos toybox curtos por chamada, nao um-por-app.
 *  - hidepid: um app comum (uid 10052) NAO enxerga /proc de outros processos; so com root (Shizuku
 *    sobe como root no carro). Por isso a leitura roda no shell privilegiado, nao via File() nosso.
 *  - MEMORIA = RSS (campo 24 de /proc/<pid>/stat — sai de graca na MESMA leitura do CPU), NAO PSS.
 *    RSS superconta paginas compartilhadas (a soma passa da RAM fisica), mas preserva RANKING e
 *    TENDENCIA — que e o que interessa pra "quem cresce/quem e maior". PSS exigiria varrer smaps por
 *    pid (caro) ou dumpsys meminfo (pesado) — exatamente o custo que se quer evitar aqui.
 *  - CPU% = (utime+stime do processo) / (jiffies TOTAIS do sistema) — ou seja, % do TOTAL do sistema
 *    (somando todos os nucleos), MESMA escala do resourceUsage global. Some `nCores` pra converter em
 *    %-de-um-nucleo se precisar. Exige 2 leituras separadas por [intervalMs] => BLOQUEIA ~intervalMs.
 *
 * Chamar SEMPRE fora da main thread (ex.: thread de binder do ContentProvider).
 */
object ProcessUsageSampler {

    /** Um processo. cpuPct pode ser null (processo novo, sem 2 amostras, ou janela degenerada). */
    data class ProcInfo(
            val pid: Int,
            val name: String,
            val rssMb: Int,
            val cpuPct: Int?,
            val state: String, // fg / visible / service / cached / system / unknown
            val uid: Int
    ) {
        fun toJson(): String =
                JSONObject().apply {
                    put("pkg", name)
                    put("pid", pid)
                    put("rssMb", rssMb)
                    put("cpuPct", cpuPct ?: JSONObject.NULL)
                    put("state", state)
                    put("uid", uid)
                }.toString()
    }

    data class Snapshot(
            val processes: List<ProcInfo>, // ja filtrado, ordenado e cortado em topN
            val totalProcs: Int, // quantos passaram no filtro includeSystem, ANTES do corte topN
            val sampledAtMs: Long
    )

    private const val PAGE_KB = 4 // arm64 deste head unit: 4 KB/pagina
    private val NUL = 0.toChar() // separador do /proc/<pid>/cmdline

    fun sample(topN: Int, orderBy: String, intervalMs: Long, includeSystem: Boolean): Snapshot? {
        if (!ShizukuUtils.isShizukuAvailable()) return null
        val ms = intervalMs.coerceIn(100L, 2000L)
        val secStr = "${ms / 1000}." + (ms % 1000).toString().padStart(3, '0')

        // Marcadores separam as secoes da saida. `2>/dev/null` engole ruido de pids que morreram no
        // meio do glob. grep -a: trata cmdline (nul-separado) como texto. grep -H: prefixa o caminho
        // (= como recuperamos o pid). oom_score_adj NAO tem newline -> `grep .` casa a linha unica.
        val script = buildString {
            append("echo @@S1@@;")
            append("cat /proc/stat /proc/[0-9]*/stat 2>/dev/null;")
            append("echo @@S2@@;")
            append("sleep ").append(secStr).append(';')
            append("cat /proc/stat /proc/[0-9]*/stat 2>/dev/null;")
            append("echo @@UID@@;")
            append("grep -aH '^Uid:' /proc/[0-9]*/status 2>/dev/null;")
            append("echo @@OOM@@;")
            append("grep -aH . /proc/[0-9]*/oom_score_adj 2>/dev/null;")
            append("echo @@CMD@@;")
            append("grep -aH . /proc/[0-9]*/cmdline 2>/dev/null;")
            append("echo @@END@@")
        }

        val out = ShizukuUtils.runCommandAndGetOutput(arrayOf("sh", "-c", script))
        return parseProcOutput(out, topN, orderBy, includeSystem, System.currentTimeMillis())
    }

    /** Parsing PURO da saida do shell — isolado do Shizuku pra dar pra testar sem carro. */
    internal fun parseProcOutput(
            out: String,
            topN: Int,
            orderBy: String,
            includeSystem: Boolean,
            sampledAtMs: Long
    ): Snapshot? {
        if (out.isBlank()) return null

        // Reparte por marcadores.
        val sec = HashMap<String, MutableList<String>>()
        var cur: MutableList<String>? = null
        for (line in out.split('\n')) {
            when (line.trim()) {
                "@@S1@@" -> { cur = ArrayList(); sec["s1"] = cur }
                "@@S2@@" -> { cur = ArrayList(); sec["s2"] = cur }
                "@@UID@@" -> { cur = ArrayList(); sec["uid"] = cur }
                "@@OOM@@" -> { cur = ArrayList(); sec["oom"] = cur }
                "@@CMD@@" -> { cur = ArrayList(); sec["cmd"] = cur }
                "@@END@@" -> cur = null
                else -> cur?.add(line)
            }
        }
        val s1 = sec["s1"] ?: return null
        val s2 = sec["s2"] ?: return null

        // CPU: jiffies totais do sistema (linha "cpu ") + busy por pid, nos dois instantes.
        val sysT0 = systemTotalJiffies(s1)
        val sysT1 = systemTotalJiffies(s2)
        val sysDelta = if (sysT0 != null && sysT1 != null) sysT1 - sysT0 else -1L

        val busy0 = HashMap<Int, Long>()
        for (l in s1) parsePidStat(l)?.let { busy0[it.pid] = it.busy }

        // Snapshot 2 e a "verdade" do conjunto de processos (nome/rss/pid vem daqui).
        val infos = LinkedHashMap<Int, MutableProc>()
        for (l in s2) {
            val p = parsePidStat(l) ?: continue
            val cpu: Int? =
                    if (sysDelta > 0L) {
                        val b0 = busy0[p.pid]
                        if (b0 == null) null
                        else ((p.busy - b0).coerceAtLeast(0L).toDouble() / sysDelta.toDouble() * 100.0)
                                .toInt().coerceIn(0, 100)
                    } else null
            infos[p.pid] = MutableProc(
                    pid = p.pid,
                    name = p.comm, // fallback; enriquecido por cmdline abaixo
                    rssMb = (p.rssPages * PAGE_KB / 1024L).coerceAtLeast(0L).toInt(),
                    cpuPct = cpu,
                    state = "unknown",
                    uid = -1
            )
        }

        // uid real (1o numero da linha Uid:) -> distingue app (uid%100000 in 10000..19999) de sistema.
        sec["uid"]?.forEach { l ->
            val pid = pidFromGrep(l, "/status") ?: return@forEach
            val v = l.substringAfter("Uid:", "").trim()
                    .split(Regex("\\s+")).firstNotNullOfOrNull { it.toIntOrNull() }
            if (v != null) infos[pid]?.uid = v
        }
        // estado por oom_score_adj.
        sec["oom"]?.forEach { l ->
            val pid = pidFromGrep(l, "/oom_score_adj") ?: return@forEach
            val adj = l.substringAfterLast(':').trim().toIntOrNull() ?: return@forEach
            infos[pid]?.state = stateFromOomAdj(adj)
        }
        // nome completo (cmdline e nul-separado: nul -> espaco, 1o token = processo/pacote).
        sec["cmd"]?.forEach { l ->
            val pid = pidFromGrep(l, "/cmdline") ?: return@forEach
            val raw = l.substringAfter("/cmdline:", "")
            val name = raw.replace(NUL, ' ').trim().substringBefore(' ').trim()
            if (name.isNotEmpty()) infos[pid]?.name = name
        }

        val filtered = infos.values.filter { includeSystem || isAppUid(it.uid) }
        val ordered =
                if (orderBy == "cpu")
                    filtered.sortedWith(compareByDescending<MutableProc> { it.cpuPct ?: -1 }.thenByDescending { it.rssMb })
                else // "rss" (default) — "pss" e aceito como alias (devolvemos RSS, ver memKind)
                    filtered.sortedWith(compareByDescending<MutableProc> { it.rssMb }.thenByDescending { it.cpuPct ?: -1 })

        val cut = if (topN in 1..500) topN else 10
        val top = ordered.take(cut).map {
            ProcInfo(it.pid, it.name, it.rssMb, it.cpuPct, it.state, it.uid)
        }
        return Snapshot(processes = top, totalProcs = filtered.size, sampledAtMs = sampledAtMs)
    }

    private class MutableProc(
            val pid: Int,
            var name: String,
            var rssMb: Int,
            var cpuPct: Int?,
            var state: String,
            var uid: Int
    )

    private class PidStat(val pid: Int, val comm: String, val busy: Long, val rssPages: Long)

    /**
     * /proc/<pid>/stat: `PID (comm) STATE ppid ... utime(14) stime(15) ... rss(24) ...`. comm pode ter
     * espacos/parenteses -> pega entre o 1o '(' e o ULTIMO ')'. Depois do ')', os campos sao 3,4,5...
     * (state no indice 0), entao utime=idx11, stime=idx12, rss=idx21.
     */
    private fun parsePidStat(line: String): PidStat? {
        if (line.isEmpty() || !line[0].isDigit()) return null
        val open = line.indexOf('(')
        val close = line.lastIndexOf(')')
        if (open <= 0 || close <= open) return null
        val pid = line.substring(0, open).trim().toIntOrNull() ?: return null
        val comm = line.substring(open + 1, close)
        val rest = line.substring(close + 1).trim().split(Regex("\\s+"))
        if (rest.size < 22) return null
        val utime = rest[11].toLongOrNull() ?: return null
        val stime = rest[12].toLongOrNull() ?: return null
        val rss = rest[21].toLongOrNull() ?: 0L
        return PidStat(pid, comm, utime + stime, rss)
    }

    /** Linha "cpu  u n s idle ..." do /proc/stat (reusa o parser ja testado do cluster). */
    private fun systemTotalJiffies(lines: List<String>): Long? {
        val cpu = lines.firstOrNull { it.startsWith("cpu ") } ?: return null
        return ClusterPerfEventLogger.parseProcStatLine(cpu)?.totalJiffies
    }

    /** "/proc/1234/<suffix>:..." -> 1234. */
    private fun pidFromGrep(line: String, suffix: String): Int? {
        if (!line.startsWith("/proc/")) return null
        val end = line.indexOf(suffix)
        if (end <= 6) return null
        return line.substring(6, end).toIntOrNull()
    }

    private fun isAppUid(uid: Int): Boolean {
        if (uid < 0) return false
        val appId = uid % 100000
        return appId in 10000..19999
    }

    private fun stateFromOomAdj(adj: Int): String = when {
        adj < 0 -> "system" // persistent / nativo
        adj == 0 -> "fg"
        adj < 200 -> "visible"
        adj < 900 -> "service"
        else -> "cached"
    }
}
