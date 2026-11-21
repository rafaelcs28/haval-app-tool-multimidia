package br.com.redesurftank.havalshisuku.managers;

/**
 * Combines the two optional rules that may pause the dashcam:
 *
 * <ul>
 *   <li>charging and stopped ({@link DashcamChargingPausePolicy});
 *   <li>record only while the car is READY ({@code car.basic.driving_ready_state == 1}), whatever
 *       the gear: ignition without the brake ("meia fase") or a charging car is not READY.
 * </ul>
 *
 * Any rule that says PAUSE wins. A rule with unknown telemetry never resumes on its own: HOLD.
 * With both rules off (or both satisfied) the answer is RESUME, which only touches a pause the
 * app itself made.
 */
public final class DashcamRecordingPolicy {
    private DashcamRecordingPolicy() {}

    public static DashcamChargingPausePolicy.Action decide(
            boolean chargingRuleEnabled, String chargingState, String speedKmh,
            boolean readyRuleEnabled, String drivingReady) {
        DashcamChargingPausePolicy.Action charging =
                DashcamChargingPausePolicy.decide(chargingRuleEnabled, chargingState, speedKmh);
        DashcamChargingPausePolicy.Action ready = decideReady(readyRuleEnabled, drivingReady);
        if (charging == DashcamChargingPausePolicy.Action.PAUSE
                || ready == DashcamChargingPausePolicy.Action.PAUSE) {
            return DashcamChargingPausePolicy.Action.PAUSE;
        }
        if (charging == DashcamChargingPausePolicy.Action.HOLD
                || ready == DashcamChargingPausePolicy.Action.HOLD) {
            return DashcamChargingPausePolicy.Action.HOLD;
        }
        return DashcamChargingPausePolicy.Action.RESUME;
    }

    static DashcamChargingPausePolicy.Action decideReady(boolean enabled, String drivingReady) {
        if (!enabled) return DashcamChargingPausePolicy.Action.RESUME;
        String value = drivingReady == null ? "" : drivingReady.trim();
        if ("1".equals(value)) return DashcamChargingPausePolicy.Action.RESUME;
        if (value.matches("-?[0-9]{1,3}")) return DashcamChargingPausePolicy.Action.PAUSE;
        return DashcamChargingPausePolicy.Action.HOLD;
    }
}
