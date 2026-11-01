package br.com.redesurftank.havalshisuku.experimental

import android.os.SystemClock
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils
import java.util.concurrent.Executors

internal object CarPlayPipelineTraceCollector {
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "CarPlayPipelineTrace").apply { isDaemon = true }
    }
    @Volatile private var lastCaptureAtMs = -1L
    private val seenLock = Any()
    private val seen = LinkedHashSet<CarPlayPipelineTraceLine>()

    fun beginSession() {
        synchronized(seenLock) { seen.clear() }
        lastCaptureAtMs = -1L
    }

    @Synchronized
    fun collect(reason: String, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && lastCaptureAtMs >= 0 && now - lastCaptureAtMs < MIN_CAPTURE_GAP_MS) return
        lastCaptureAtMs = now
        worker.execute {
            val raw = ShizukuUtils.runCommandAndGetOutput(arrayOf(
                "logcat", "-d", "-v", "brief", "ImpulseCPMap:W", "*:S"))
            val lines = CarPlayPipelineTraceParser.parse(raw).filter { line ->
                synchronized(seenLock) {
                    seen.size < MAX_SEEN_LINES && seen.add(line)
                }
            }
            ClusterPersistentEventLogger.log("carplay_clean_map_native_trace_capture",
                mapOf("reason" to reason, "lines" to lines.size))
            lines.forEach { line ->
                ClusterPersistentEventLogger.log("carplay_clean_map_native_trace", mapOf(
                    "reason" to reason,
                    "boot_ms" to line.bootMs,
                    "generation" to line.generation,
                    "stage" to line.event,
                    "status" to line.status,
                    "value" to line.value,
                    "aux" to line.auxiliary))
            }
        }
    }

    private const val MIN_CAPTURE_GAP_MS = 2_000L
    private const val MAX_SEEN_LINES = 8_192
}
