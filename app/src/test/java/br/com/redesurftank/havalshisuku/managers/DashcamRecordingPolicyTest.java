package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DashcamRecordingPolicyTest {
    private static final DashcamChargingPausePolicy.Action PAUSE = DashcamChargingPausePolicy.Action.PAUSE;
    private static final DashcamChargingPausePolicy.Action RESUME = DashcamChargingPausePolicy.Action.RESUME;
    private static final DashcamChargingPausePolicy.Action HOLD = DashcamChargingPausePolicy.Action.HOLD;

    @Test
    public void readyRulePausesOutsideReadyWhateverTheGear() {
        assertEquals(PAUSE, DashcamRecordingPolicy.decide(false, "0", "0", true, "0"));
        assertEquals(PAUSE, DashcamRecordingPolicy.decide(false, "0", "0", true, "2"));
        assertEquals(RESUME, DashcamRecordingPolicy.decide(false, "0", "0", true, "1"));
        assertEquals(RESUME, DashcamRecordingPolicy.decide(false, "0", "40", true, " 1 "));
    }

    @Test
    public void unknownReadyHoldsInsteadOfResuming() {
        assertEquals(HOLD, DashcamRecordingPolicy.decide(false, "0", "0", true, null));
        assertEquals(HOLD, DashcamRecordingPolicy.decide(false, "0", "0", true, "ready"));
    }

    @Test
    public void eitherRuleMayPauseAndOffMeansResume() {
        // Charging and stopped pauses even if READY.
        assertEquals(PAUSE, DashcamRecordingPolicy.decide(true, "1", "0", true, "1"));
        // Not READY pauses even when not charging.
        assertEquals(PAUSE, DashcamRecordingPolicy.decide(true, "0", "0", true, "0"));
        // A known pause beats the other rule's unknown telemetry.
        assertEquals(PAUSE, DashcamRecordingPolicy.decide(true, null, null, true, "0"));
        // Both off: resume (only an app-owned pause is ever resumed).
        assertEquals(RESUME, DashcamRecordingPolicy.decide(false, "1", "0", false, "0"));
        // Ready rule off keeps the charging behavior of 74162.
        assertEquals(HOLD, DashcamRecordingPolicy.decide(true, null, "0", false, "0"));
    }
}
