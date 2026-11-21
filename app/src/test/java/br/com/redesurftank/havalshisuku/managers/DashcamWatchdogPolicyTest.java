package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DashcamWatchdogPolicyTest {
    private static DashcamWatchdogPolicy.Action decide(Boolean running, Boolean emergency,
            boolean pausedByCharging, boolean chargingWantsPause, boolean resume, boolean warn) {
        return DashcamWatchdogPolicy.decide(running, emergency, pausedByCharging, chargingWantsPause,
                resume, warn);
    }

    @Test
    public void recordingOrUnknownNeedsNothing() {
        assertEquals(DashcamWatchdogPolicy.Action.NONE, decide(true, false, false, false, true, true));
        assertEquals(DashcamWatchdogPolicy.Action.NONE, decide(null, false, false, false, true, true));
        assertEquals(DashcamWatchdogPolicy.Action.NONE, decide(false, null, false, false, true, true));
    }

    @Test
    public void chargingPauseAndEmergencyAreRespected() {
        assertEquals(DashcamWatchdogPolicy.Action.NONE, decide(false, false, true, false, true, true));
        assertEquals(DashcamWatchdogPolicy.Action.NONE, decide(false, false, false, true, true, true));
        assertEquals(DashcamWatchdogPolicy.Action.NONE, decide(false, true, false, false, true, true));
    }

    @Test
    public void stoppedCameraIsResumedOrReported() {
        assertEquals(DashcamWatchdogPolicy.Action.RESUME, decide(false, false, false, false, true, false));
        assertEquals(DashcamWatchdogPolicy.Action.WARN, decide(false, false, false, false, false, true));
        assertEquals(DashcamWatchdogPolicy.Action.NONE, decide(false, false, false, false, false, false));
    }
}
