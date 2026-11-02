package br.com.redesurftank.havalshisuku.experimental

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import br.com.redesurftank.havalshisuku.managers.DisplayAppLauncher

/** ActivityManager reads stay off the UI thread and exist only for the explicit test. */
internal class CarPlayCleanMapSampler(
    private val deadlineElapsedMs: Long,
    private val onReadStarted: (Long) -> Unit,
    private val publish: (CarPlayCleanMapTaskRead) -> Unit
) : AutoCloseable {
    @Volatile private var closed = false
    private val poll = object : Runnable {
        override fun run() {
            if (closed || SystemClock.elapsedRealtime() >= deadlineElapsedMs) { close(); return }
            val startedAt = SystemClock.elapsedRealtime()
            onReadStarted(startedAt)
            val sample = runCatching { DisplayAppLauncher.readCarPlayCleanMapTasks(startedAt) }
                .getOrElse {
                    CarPlayCleanMapTaskRead(
                        CarPlayCleanMapTaskObservation.unknown(), startedAt, SystemClock.elapsedRealtime())
                }
            if (!closed && SystemClock.elapsedRealtime() < deadlineElapsedMs) publish(sample)
            if (!closed) {
                val nextStartAt = startedAt + SAMPLE_PERIOD_MS
                worker.postDelayed(this,
                    (nextStartAt - SystemClock.elapsedRealtime()).coerceAtLeast(MIN_READ_GAP_MS))
            }
        }
    }

    init { worker.post(poll) }

    override fun close() {
        closed = true
        worker.removeCallbacks(poll)
    }

    companion object {
        // A blocked AM read can never create one extra thread per later test.
        // All sessions share one serial daemon; close removes only its own work.
        private val thread = HandlerThread("carplay-clean-map-observe").apply { isDaemon = true; start() }
        private val worker = Handler(thread.looper)
        private const val SAMPLE_PERIOD_MS = 500L
        private const val MIN_READ_GAP_MS = 100L
    }
}
