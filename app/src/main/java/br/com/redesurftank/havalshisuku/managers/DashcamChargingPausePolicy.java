package br.com.redesurftank.havalshisuku.managers;

/** Decides whether charging automation may change the dashcam recording state. */
public final class DashcamChargingPausePolicy {
    private static final double STOPPED_MAX_KMH = 0.5d;

    public enum Action {
        HOLD,
        PAUSE,
        RESUME
    }

    private DashcamChargingPausePolicy() {}

    public static Action decide(boolean enabled, String chargingState, String speedKmh) {
        if (!enabled) return Action.RESUME;

        String charging = chargingState == null ? "" : chargingState.trim();
        if ("0".equals(charging)) return Action.RESUME;
        if (!"1".equals(charging)) return Action.HOLD;

        Double speed = parseSpeed(speedKmh);
        if (speed == null) return Action.HOLD;
        return speed <= STOPPED_MAX_KMH ? Action.PAUSE : Action.RESUME;
    }

    private static Double parseSpeed(String raw) {
        if (raw == null) return null;
        try {
            double value = Double.parseDouble(raw.trim());
            return Double.isFinite(value) && value >= 0d && value <= 300d ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
