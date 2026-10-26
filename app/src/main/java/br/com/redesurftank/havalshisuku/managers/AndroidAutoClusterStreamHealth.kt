package br.com.redesurftank.havalshisuku.managers

import br.com.redesurftank.havalshisuku.api.AaClusterProtocol

internal object AndroidAutoClusterStreamHealth {
    const val STALL_TIMEOUT_MS = 12_000L
    const val RECOVERY_BACKOFF_MS = 60_000L
    const val RECOVERY_IN_FLIGHT_TIMEOUT_MS = 15_000L
    const val HEALTHY_BUDGET_RESET_MS = 60_000L
    const val HEALTHY_FRAME_MAX_AGE_MS = 3_000L
    const val MAX_RECOVERIES_PER_BURST = 2
    const val MIN_MOVING_SPEED_KMH = 3f
    private const val DECODER_FAILURE = "CLUSTER decoder/config failed"
    private const val FAILED_REQUEST = "CLUSTER request failed"

    /** Only a known decoder failure after LIVE may recover; ownership errors stay terminal. */
    fun nextDecoderFailurePending(pending: Boolean, state: Int, reason: String, wasLive: Boolean): Boolean {
        if (state != AaClusterProtocol.FAILED) return false
        return when (reason) {
            DECODER_FAILURE -> pending || wasLive
            FAILED_REQUEST -> pending // Service repeats this for the same failed request.
            else -> false
        }
    }

    fun shouldCancelRecovery(state: Int, reason: String): Boolean =
        state == AaClusterProtocol.FAILED && reason != DECODER_FAILURE && reason != FAILED_REQUEST

    fun shouldRecoverDecoderFailure(
        pending: Boolean,
        outputRequested: Boolean,
        consumerAvailable: Boolean,
        nowMs: Long,
        lastRecoveryAtMs: Long,
        recoveryCount: Int
    ): Boolean = pending && outputRequested && consumerAvailable &&
        recoveryCount in 0 until MAX_RECOVERIES_PER_BURST && nowMs >= 0L &&
        (lastRecoveryAtMs == 0L || nowMs >= lastRecoveryAtMs &&
            nowMs - lastRecoveryAtMs >= RECOVERY_BACKOFF_MS)

    fun shouldRecover(
        nowMs: Long,
        liveSinceMs: Long,
        lastFrameAtMs: Long,
        lastRecoveryAtMs: Long,
        recoveryCount: Int,
        speedKmh: Float?
    ): Boolean {
        if (liveSinceMs <= 0L || nowMs < liveSinceMs) return false
        if (recoveryCount >= MAX_RECOVERIES_PER_BURST) return false
        if (lastRecoveryAtMs > 0L && nowMs - lastRecoveryAtMs < RECOVERY_BACKOFF_MS) return false
        if (speedKmh == null || !speedKmh.isFinite() || speedKmh < MIN_MOVING_SPEED_KMH) return false

        val lastActivityAtMs = maxOf(liveSinceMs, lastFrameAtMs)
        return nowMs - lastActivityAtMs >= STALL_TIMEOUT_MS
    }

    fun shouldResetRecoveryBudget(
        nowMs: Long,
        liveSinceMs: Long,
        lastFrameAtMs: Long,
        lastRecoveryAtMs: Long,
        recoveryCount: Int
    ): Boolean {
        if (recoveryCount <= 0 || lastRecoveryAtMs <= 0L) return false
        if (liveSinceMs <= lastRecoveryAtMs || lastFrameAtMs <= lastRecoveryAtMs) return false
        if (nowMs < lastRecoveryAtMs || nowMs - lastRecoveryAtMs < HEALTHY_BUDGET_RESET_MS) return false
        return nowMs >= lastFrameAtMs && nowMs - lastFrameAtMs <= HEALTHY_FRAME_MAX_AGE_MS
    }

    fun isRecoveryTimedOut(nowMs: Long, recoveryStartedAtMs: Long): Boolean =
        recoveryStartedAtMs > 0L && nowMs >= recoveryStartedAtMs &&
            nowMs - recoveryStartedAtMs >= RECOVERY_IN_FLIGHT_TIMEOUT_MS
}
