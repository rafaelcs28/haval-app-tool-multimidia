package br.com.redesurftank.havalshisuku.managers;

/** What the periodic check does with one observed camera state. */
public final class DashcamWatchdogPolicy {
    public enum Action {
        /** Recording, or not recording for a reason the app must respect. */
        NONE,
        /** Not recording and the user enabled "verificar e retomar": send start and verify. */
        RESUME,
        /** Not recording, resume disabled: only warn (when the warning is enabled). */
        WARN
    }

    private DashcamWatchdogPolicy() {}

    /**
     * @param running camera running flag (null = unknown)
     * @param emergency emergency recording flag (null = unknown)
     * @param pausedByCharging the charging automation owns the current pause
     * @param chargingWantsPause the charging policy currently answers PAUSE
     */
    public static Action decide(Boolean running, Boolean emergency, boolean pausedByCharging,
            boolean chargingWantsPause, boolean resumeEnabled, boolean warningEnabled) {
        if (!Boolean.FALSE.equals(running)) return Action.NONE;
        if (!Boolean.FALSE.equals(emergency)) return Action.NONE;
        if (pausedByCharging || chargingWantsPause) return Action.NONE;
        if (resumeEnabled) return Action.RESUME;
        return warningEnabled ? Action.WARN : Action.NONE;
    }
}
