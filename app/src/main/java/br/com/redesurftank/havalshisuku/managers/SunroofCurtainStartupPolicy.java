package br.com.redesurftank.havalshisuku.managers;

/** Timing seguro da primeira avaliação automática da cortina após o boot. */
public final class SunroofCurtainStartupPolicy {
    private SunroofCurtainStartupPolicy() {}

    public static final long SETTLE_AFTER_READY_MS = 20_000L;
    public static final long READY_CHECK_DELAY_MS = 30_000L;
    public static final long ARM_FALLBACK_MS = 120_000L;

    public enum Trigger {
        DRIVING_READY_EVENT("driving_ready_event"),
        DRIVING_READY_POLL("driving_ready_poll"),
        FALLBACK_TIMEOUT("fallback_timeout");

        public final String traceName;

        Trigger(String traceName) {
            this.traceName = traceName;
        }
    }

    public static long settleMsFor(Trigger trigger, boolean readyAlreadyOnAtArm) {
        if (trigger == Trigger.DRIVING_READY_POLL && readyAlreadyOnAtArm) return 0L;
        return SETTLE_AFTER_READY_MS;
    }
}
