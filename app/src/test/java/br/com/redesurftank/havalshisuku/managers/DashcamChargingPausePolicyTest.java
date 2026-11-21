package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DashcamChargingPausePolicyTest {
    @Test
    public void pausesOnlyWhileExternalChargingAndStopped() {
        assertEquals(DashcamChargingPausePolicy.Action.PAUSE,
                DashcamChargingPausePolicy.decide(true, "1", "0"));
        assertEquals(DashcamChargingPausePolicy.Action.PAUSE,
                DashcamChargingPausePolicy.decide(true, "1", "0.4"));
        assertEquals(DashcamChargingPausePolicy.Action.RESUME,
                DashcamChargingPausePolicy.decide(true, "1", "0.6"));
        assertEquals(DashcamChargingPausePolicy.Action.RESUME,
                DashcamChargingPausePolicy.decide(true, "0", "0"));
    }

    @Test
    public void disabledFeatureAlwaysRequestsOwnedPauseToResume() {
        assertEquals(DashcamChargingPausePolicy.Action.RESUME,
                DashcamChargingPausePolicy.decide(false, null, null));
    }

    @Test
    public void unknownTelemetryNeverChangesDashcamState() {
        assertEquals(DashcamChargingPausePolicy.Action.HOLD,
                DashcamChargingPausePolicy.decide(true, null, "0"));
        assertEquals(DashcamChargingPausePolicy.Action.HOLD,
                DashcamChargingPausePolicy.decide(true, "2", "0"));
        assertEquals(DashcamChargingPausePolicy.Action.HOLD,
                DashcamChargingPausePolicy.decide(true, "1", null));
        assertEquals(DashcamChargingPausePolicy.Action.HOLD,
                DashcamChargingPausePolicy.decide(true, "1", "-1"));
        assertEquals(DashcamChargingPausePolicy.Action.HOLD,
                DashcamChargingPausePolicy.decide(true, "1", "NaN"));
    }
}
