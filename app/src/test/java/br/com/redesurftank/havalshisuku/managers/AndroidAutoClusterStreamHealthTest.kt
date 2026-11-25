package br.com.redesurftank.havalshisuku.managers

import br.com.redesurftank.havalshisuku.api.AaClusterProtocol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutoClusterStreamHealthTest {
    @Test
    fun `decoder failure after live survives repeated failed request status`() {
        val pending = AndroidAutoClusterStreamHealth.nextDecoderFailurePending(
            false, AaClusterProtocol.FAILED, "CLUSTER decoder/config failed", true
        )
        assertTrue(pending)
        assertTrue(AndroidAutoClusterStreamHealth.nextDecoderFailurePending(
            pending, AaClusterProtocol.FAILED, "CLUSTER request failed", false
        ))
        assertFalse(AndroidAutoClusterStreamHealth.nextDecoderFailurePending(
            false, AaClusterProtocol.FAILED, "CLUSTER request failed", false
        ))
    }

    @Test
    fun `decoder failure before the first frame is recoverable, ownership failures are not`() {
        assertTrue(AndroidAutoClusterStreamHealth.nextDecoderFailurePending(
            false, AaClusterProtocol.FAILED, "CLUSTER decoder/config failed", false
        ))
        for (reason in listOf("Service died; consumer remains quarantined",
            "decoder retirement unconfirmed; consumer retained", "output transaction uncertain; consumer retained")) {
            assertFalse(AndroidAutoClusterStreamHealth.nextDecoderFailurePending(
                true, AaClusterProtocol.FAILED, reason, true
            ))
        }
    }

    @Test
    fun `ownership failures cancel in flight recovery but decoder and waiting states do not`() {
        for (reason in listOf("Service died; consumer remains quarantined",
            "decoder retirement unconfirmed; consumer retained", "output transaction uncertain; consumer retained",
            "unknown failure")) {
            assertTrue(AndroidAutoClusterStreamHealth.shouldCancelRecovery(AaClusterProtocol.FAILED, reason))
        }
        assertFalse(AndroidAutoClusterStreamHealth.shouldCancelRecovery(
            AaClusterProtocol.FAILED, "CLUSTER decoder/config failed"
        ))
        assertFalse(AndroidAutoClusterStreamHealth.shouldCancelRecovery(
            AaClusterProtocol.FAILED, "CLUSTER request failed"
        ))
        assertFalse(AndroidAutoClusterStreamHealth.shouldCancelRecovery(
            AaClusterProtocol.WAITING_SESSION, "previous output fully released"
        ))
    }

    @Test
    fun `new output states clear old decoder failure evidence`() {
        for (state in listOf(AaClusterProtocol.DISABLED, AaClusterProtocol.WAITING_SESSION, AaClusterProtocol.LIVE)) {
            assertFalse(AndroidAutoClusterStreamHealth.nextDecoderFailurePending(
                true, state, "CLUSTER decoder/config failed", true
            ))
        }
    }

    @Test
    fun `explicit decoder failure can recover without moving speed evidence`() {
        assertTrue(AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
            true, true, true, 20_000L, 0L, 0
        ))
        assertFalse(AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
            false, true, true, 20_000L, 0L, 0
        ))
        assertFalse(AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
            true, false, true, 20_000L, 0L, 0
        ))
        assertFalse(AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
            true, true, false, 20_000L, 0L, 0
        ))
    }

    @Test
    fun `decoder recovery shares bounded recovery budget and backoff`() {
        assertFalse(AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
            true, true, true, 79_999L, 20_000L, 1
        ))
        assertTrue(AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
            true, true, true, 80_000L, 20_000L, 1
        ))
        assertFalse(AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
            true, true, true, 80_000L, 20_000L, 2
        ))
        assertFalse(AndroidAutoClusterStreamHealth.shouldRecoverDecoderFailure(
            true, true, true, 19_999L, 20_000L, 1
        ))
    }

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
    fun `backoff and burst limit prevent recovery loops`() {
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
                recoveryCount = AndroidAutoClusterStreamHealth.MAX_RECOVERIES_PER_BURST,
                speedKmh = 40f
            )
        )
    }

    @Test
    fun `healthy frames rearm a previously exhausted recovery budget`() {
        assertTrue(
            AndroidAutoClusterStreamHealth.shouldResetRecoveryBudget(
                nowMs = 90_000L,
                liveSinceMs = 25_000L,
                lastFrameAtMs = 89_000L,
                lastRecoveryAtMs = 20_000L,
                recoveryCount = AndroidAutoClusterStreamHealth.MAX_RECOVERIES_PER_BURST
            )
        )
    }

    @Test
    fun `stale frames never rearm the recovery budget`() {
        assertFalse(
            AndroidAutoClusterStreamHealth.shouldResetRecoveryBudget(
                nowMs = 90_000L,
                liveSinceMs = 25_000L,
                lastFrameAtMs = 70_000L,
                lastRecoveryAtMs = 20_000L,
                recoveryCount = AndroidAutoClusterStreamHealth.MAX_RECOVERIES_PER_BURST
            )
        )
    }

    @Test
    fun `recovery in flight expires only after its bounded timeout`() {
        assertFalse(AndroidAutoClusterStreamHealth.isRecoveryTimedOut(34_999L, 20_000L))
        assertTrue(AndroidAutoClusterStreamHealth.isRecoveryTimedOut(35_000L, 20_000L))
    }

    @Test
    fun `exhausted recovery budget re-arms after the cooldown only`() {
        val max = AndroidAutoClusterStreamHealth.MAX_RECOVERIES_PER_BURST
        val cool = AndroidAutoClusterStreamHealth.EXHAUSTED_BUDGET_COOLDOWN_MS
        assertFalse(AndroidAutoClusterStreamHealth.shouldRearmExhaustedBudget(1_000L + cool - 1, 1_000L, max))
        assertTrue(AndroidAutoClusterStreamHealth.shouldRearmExhaustedBudget(1_000L + cool, 1_000L, max))
        assertFalse(AndroidAutoClusterStreamHealth.shouldRearmExhaustedBudget(1_000L + cool, 1_000L, max - 1))
        assertFalse(AndroidAutoClusterStreamHealth.shouldRearmExhaustedBudget(1_000L + cool, 0L, max))
    }

    @Test
    fun `watchdog re-requests a wanted map only after it stayed down long enough, once a minute`() {
        val after = AndroidAutoClusterStreamHealth.NOT_LIVE_KICK_AFTER_MS
        val every = AndroidAutoClusterStreamHealth.NOT_LIVE_KICK_INTERVAL_MS
        assertFalse(AndroidAutoClusterStreamHealth.shouldKickNotLive(1_000L + after - 1, 1_000L, 0L, false))
        assertTrue(AndroidAutoClusterStreamHealth.shouldKickNotLive(1_000L + after, 1_000L, 0L, false))
        assertFalse(AndroidAutoClusterStreamHealth.shouldKickNotLive(1_000L + after, 1_000L, 0L, true))
        assertFalse(AndroidAutoClusterStreamHealth.shouldKickNotLive(500_000L, 1_000L, 500_000L - every + 1, false))
        assertTrue(AndroidAutoClusterStreamHealth.shouldKickNotLive(500_000L, 1_000L, 500_000L - every, false))
        assertFalse(AndroidAutoClusterStreamHealth.shouldKickNotLive(500_000L, 0L, 0L, false))
    }

    @Test
    fun `a recovery whose request failed is retried after the short delay, not the full timeout`() {
        val quick = AndroidAutoClusterStreamHealth.FAILED_RECOVERY_RETRY_MS
        assertFalse(AndroidAutoClusterStreamHealth.isFailedRecoveryDue(10_000L, 0L))
        assertFalse(AndroidAutoClusterStreamHealth.isFailedRecoveryDue(10_000L + quick - 1, 10_000L))
        assertTrue(AndroidAutoClusterStreamHealth.isFailedRecoveryDue(10_000L + quick, 10_000L))
    }
}
