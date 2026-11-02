package br.com.redesurftank.havalshisuku.experimental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarPlayClusterHostFreshnessTest {
    private fun snapshot(
        observedAt: Long = 1_000,
        main: Boolean = true,
        secondary: Boolean = false,
        safetyClear: Boolean = true,
        otherAppsClear: Boolean = true,
        readStartedAt: Long = 2_100,
        readCompletedAt: Long = -1,
        readInFlight: Boolean = true,
        lastReadKnown: Boolean = true
    ) = CarPlayClusterHostPolicy.Snapshot(
        observedAt, 3, true, true, true, main, safetyClear, otherAppsClear,
        true, secondary, false, readStartedAt, readCompletedAt, readInFlight, lastReadKnown)

    @Test fun admissionNeverBorrowsActiveFreshnessGrace() {
        val sample = snapshot()
        assertEquals(CarPlayClusterHostPolicy.Reason.SNAPSHOT_STALE,
            CarPlayClusterHostPolicy.reason(sample, 2_200))
        assertEquals(CarPlayClusterHostPolicy.Reason.ALLOWED,
            CarPlayClusterHostPolicy.reason(sample, 2_200, true))
        assertTrue(CarPlayClusterHostPolicy.usesActiveFreshnessGrace(sample, 2_200))
    }

    @Test fun graceIsBoundedByObservationAgeAndReadDuration() {
        assertEquals(CarPlayClusterHostPolicy.Reason.SNAPSHOT_STALE,
            CarPlayClusterHostPolicy.reason(snapshot(readStartedAt = 1_050), 2_060, true))
        assertEquals(CarPlayClusterHostPolicy.Reason.SNAPSHOT_STALE,
            CarPlayClusterHostPolicy.reason(snapshot(readStartedAt = 1_050,
                readCompletedAt = 1_050, readInFlight = false, lastReadKnown = false), 2_060, true))
        assertFalse(CarPlayClusterHostPolicy.usesActiveFreshnessGrace(snapshot(), 3_001))
    }

    @Test fun recentUnknownReadCanUseOnlyTheRemainingBoundedWindow() {
        val sample = snapshot(readStartedAt = 1_900, readCompletedAt = 2_100,
            readInFlight = false, lastReadKnown = false)
        assertEquals(CarPlayClusterHostPolicy.Reason.ALLOWED,
            CarPlayClusterHostPolicy.reason(sample, 2_200, true))
        assertEquals(CarPlayClusterHostPolicy.Reason.SNAPSHOT_STALE,
            CarPlayClusterHostPolicy.reason(sample, 3_101, true))
    }

    @Test fun liveSafetyAndFreshTaskDenialsRemainImmediate() {
        assertEquals(CarPlayClusterHostPolicy.Reason.SAFETY_UI_ACTIVE,
            CarPlayClusterHostPolicy.reason(snapshot(safetyClear = false), 2_200, true))
        assertEquals(CarPlayClusterHostPolicy.Reason.OTHER_APP_ACTIVE,
            CarPlayClusterHostPolicy.reason(snapshot(otherAppsClear = false), 2_200, true))
        assertEquals(CarPlayClusterHostPolicy.Reason.CARPLAY_SECONDARY_PRESENT,
            CarPlayClusterHostPolicy.reason(snapshot(observedAt = 2_150, secondary = true), 2_200, true))
        assertEquals(CarPlayClusterHostPolicy.Reason.CARPLAY_MAIN_UNCONFIRMED,
            CarPlayClusterHostPolicy.reason(snapshot(observedAt = 2_150, main = false), 2_200, true))
    }
}
