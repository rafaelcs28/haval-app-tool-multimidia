package br.com.redesurftank.havalshisuku.hud

import org.junit.Assert.*
import org.junit.Test

class RoundaboutTrackerTest {
    @Test fun switchingToAnOrdinaryManeuverCannotEnterTheRoundabout() {
        val tracker = RoundaboutTracker()
        tracker.onUpdate("roundabout", 40f)
        assertFalse(tracker.onUpdate(null, 100f))
        assertFalse(tracker.entered)
        assertNull(tracker.startM)
        assertEquals(0f, tracker.progress(50f), 0f)
    }

    @Test fun ordinaryManeuverClearsThePreviousApproachMinimum() {
        val tracker = RoundaboutTracker()
        tracker.onUpdate("first", 40f)
        tracker.onUpdate(null, null)
        assertFalse(tracker.onUpdate("second", 100f))
        assertFalse(tracker.entered)
        assertTrue(tracker.onUpdate("second", 140f))
    }

    @Test fun leavingAnEnteredRoundaboutClearsItsProgressImmediately() {
        val tracker = RoundaboutTracker()
        tracker.onUpdate("first", 40f)
        assertTrue(tracker.onUpdate("first", 100f))
        assertFalse(tracker.onUpdate(null, 160f))
        assertNull(tracker.startM)
        assertFalse(tracker.entered)
        assertFalse(tracker.onUpdate("second", 80f))
    }

    @Test fun renamedRoundaboutStillCarriesTheApproachMinimum() {
        val tracker = RoundaboutTracker()
        tracker.onUpdate("approach-road", 180f)
        tracker.onUpdate("approach-road", 40f)
        assertTrue(tracker.onUpdate("exit-road", 95f))
        assertEquals(0.5f, tracker.progress(47.5f), 0.001f)
    }

    @Test fun aStaleDistanceDoesNotCreateAnEntryDuringIdentityChange() {
        val tracker = RoundaboutTracker()
        tracker.onUpdate("first", 40f)
        assertFalse(tracker.onUpdate("second", null))
        assertFalse(tracker.onUpdate("second", 100f))
        assertFalse(tracker.entered)
    }

    @Test fun largeJumpsRemainANewApproachRatherThanAnExitLap() {
        val tracker = RoundaboutTracker()
        tracker.onUpdate("first", 40f)
        assertFalse(tracker.onUpdate("second", 300f))
        assertFalse(tracker.entered)
    }
}
