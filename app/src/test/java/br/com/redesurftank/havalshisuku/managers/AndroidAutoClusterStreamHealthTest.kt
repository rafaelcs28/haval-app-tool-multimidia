package br.com.redesurftank.havalshisuku.managers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutoClusterStreamHealthTest {
    @Test
    fun `recovers a stale live stream while the car is moving`() {
        assertTrue(
            AndroidAutoClusterStreamHealth.shouldRecover(
                nowMs = 20_000L,
                liveSinceMs = 1_000L,
                lastFrameAtMs = 7_000L,
                lastRecoveryAtMs = 0L,
                recoveryCount = 0,
                speedKmh = 25f
            )
        )
    }

    @Test
    fun `does not recover before the live grace period expires`() {
        assertFalse(
            AndroidAutoClusterStreamHealth.shouldRecover(
                nowMs = 12_999L,
                liveSinceMs = 1_000L,
                lastFrameAtMs = 0L,
                lastRecoveryAtMs = 0L,
                recoveryCount = 0,
                speedKmh = 25f
            )
        )
    }

    @Test
    fun `does not recover a static map while the car is parked`() {
        assertFalse(
            AndroidAutoClusterStreamHealth.shouldRecover(
                nowMs = 30_000L,
                liveSinceMs = 1_000L,
                lastFrameAtMs = 2_000L,
                lastRecoveryAtMs = 0L,
                recoveryCount = 0,
                speedKmh = 0f
            )
        )
    }

    @Test
    fun `fresh texture frames keep the stream healthy`() {
        assertFalse(
            AndroidAutoClusterStreamHealth.shouldRecover(
                nowMs = 30_000L,
                liveSinceMs = 1_000L,
                lastFrameAtMs = 29_000L,
                lastRecoveryAtMs = 0L,
                recoveryCount = 0,
                speedKmh = 40f
            )
        )
    }

    @Test
    fun `backoff and session limit prevent recovery loops`() {
        assertFalse(
            AndroidAutoClusterStreamHealth.shouldRecover(
                nowMs = 70_000L,
                liveSinceMs = 1_000L,
                lastFrameAtMs = 2_000L,
                lastRecoveryAtMs = 20_000L,
                recoveryCount = 1,
                speedKmh = 40f
            )
        )
        assertFalse(
            AndroidAutoClusterStreamHealth.shouldRecover(
                nowMs = 200_000L,
                liveSinceMs = 1_000L,
                lastFrameAtMs = 2_000L,
                lastRecoveryAtMs = 20_000L,
                recoveryCount = AndroidAutoClusterStreamHealth.MAX_RECOVERIES_PER_SESSION,
                speedKmh = 40f
            )
        )
    }
}
