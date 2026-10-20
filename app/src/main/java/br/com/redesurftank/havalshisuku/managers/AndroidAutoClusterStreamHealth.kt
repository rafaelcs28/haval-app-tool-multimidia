package br.com.redesurftank.havalshisuku.managers

internal object AndroidAutoClusterStreamHealth {
    const val STALL_TIMEOUT_MS = 12_000L
    const val RECOVERY_BACKOFF_MS = 60_000L
    const val MAX_RECOVERIES_PER_SESSION = 2
    const val MIN_MOVING_SPEED_KMH = 3f

    fun shouldRecover(
        nowMs: Long,
        liveSinceMs: Long,
        lastFrameAtMs: Long,
        lastRecoveryAtMs: Long,
        recoveryCount: Int,
        speedKmh: Float?
    ): Boolean {
        if (liveSinceMs <= 0L || nowMs < liveSinceMs) return false
        if (recoveryCount >= MAX_RECOVERIES_PER_SESSION) return false
        if (lastRecoveryAtMs > 0L && nowMs - lastRecoveryAtMs < RECOVERY_BACKOFF_MS) return false
        if (speedKmh == null || !speedKmh.isFinite() || speedKmh < MIN_MOVING_SPEED_KMH) return false

        val lastActivityAtMs = maxOf(liveSinceMs, lastFrameAtMs)
        return nowMs - lastActivityAtMs >= STALL_TIMEOUT_MS
    }
}
