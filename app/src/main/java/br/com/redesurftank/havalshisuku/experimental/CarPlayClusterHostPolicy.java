package br.com.redesurftank.havalshisuku.experimental;

public final class CarPlayClusterHostPolicy {
    public static final long MAX_SNAPSHOT_AGE_MS = 1000;
    public static final long ACTIVE_GRACE_MAX_AGE_MS = 2000;
    public static final long MAX_READ_IN_FLIGHT_MS = 1000;
    public enum Reason {
        ALLOWED, SNAPSHOT_MISSING, TASK_OBSERVATION_UNKNOWN, SNAPSHOT_FUTURE, SNAPSHOT_STALE,
        WRONG_DISPLAY, EXPERIMENT_DISABLED, SLOT_NOT_OWNED, AA_NOT_KNOWN_INACTIVE,
        AA_SESSION_ACTIVE, AA_SURFACE_ATTACHED, CARPLAY_MAIN_UNCONFIRMED, CARPLAY_SECONDARY_PRESENT,
        SAFETY_UI_ACTIVE, OTHER_APP_ACTIVE, SLOT_OCCUPIED, SLOT_DETACHED, SLOT_NOT_ACCELERATED,
        SLOT_HIDDEN, SLOT_NOT_LAID_OUT, SLOT_SIZE_INVALID, SNAPSHOT_ERROR
    }

    public static final class Snapshot {
        public final long observedAtElapsedMs;
        public final int displayId;
        public final boolean explicitlyEnabled;
        public final boolean slotOwned;
        public final boolean androidAutoKnownInactive;
        public final boolean carPlayOnMain;
        public final boolean safetyUiClear;
        public final boolean otherAppsClear;
        public final boolean taskObservationKnown;
        public final boolean carPlayOnSecondary;
        public final boolean androidAutoRequested;
        public final long taskReadStartedAtElapsedMs;
        public final long taskReadCompletedAtElapsedMs;
        public final boolean taskReadInFlight;
        public final boolean lastTaskReadKnown;

        public Snapshot(long observedAtElapsedMs, int displayId, boolean explicitlyEnabled,
                        boolean slotOwned, boolean androidAutoKnownInactive, boolean carPlayOnMain,
                        boolean safetyUiClear, boolean otherAppsClear) {
            this(observedAtElapsedMs, displayId, explicitlyEnabled, slotOwned, androidAutoKnownInactive,
                carPlayOnMain, safetyUiClear, otherAppsClear, true, false, false,
                -1, -1, false, true);
        }

        public Snapshot(long observedAtElapsedMs, int displayId, boolean explicitlyEnabled,
                        boolean slotOwned, boolean androidAutoKnownInactive, boolean carPlayOnMain,
                        boolean safetyUiClear, boolean otherAppsClear, boolean taskObservationKnown,
                        boolean carPlayOnSecondary, boolean androidAutoRequested) {
            this(observedAtElapsedMs, displayId, explicitlyEnabled, slotOwned, androidAutoKnownInactive,
                carPlayOnMain, safetyUiClear, otherAppsClear, taskObservationKnown,
                carPlayOnSecondary, androidAutoRequested, -1, -1, false, true);
        }

        public Snapshot(long observedAtElapsedMs, int displayId, boolean explicitlyEnabled,
                        boolean slotOwned, boolean androidAutoKnownInactive, boolean carPlayOnMain,
                        boolean safetyUiClear, boolean otherAppsClear, boolean taskObservationKnown,
                        boolean carPlayOnSecondary, boolean androidAutoRequested,
                        long taskReadStartedAtElapsedMs, long taskReadCompletedAtElapsedMs,
                        boolean taskReadInFlight, boolean lastTaskReadKnown) {
            this.observedAtElapsedMs = observedAtElapsedMs;
            this.displayId = displayId;
            this.explicitlyEnabled = explicitlyEnabled;
            this.slotOwned = slotOwned;
            this.androidAutoKnownInactive = androidAutoKnownInactive;
            this.carPlayOnMain = carPlayOnMain;
            this.safetyUiClear = safetyUiClear;
            this.otherAppsClear = otherAppsClear;
            this.taskObservationKnown = taskObservationKnown;
            this.carPlayOnSecondary = carPlayOnSecondary;
            this.androidAutoRequested = androidAutoRequested;
            this.taskReadStartedAtElapsedMs = taskReadStartedAtElapsedMs;
            this.taskReadCompletedAtElapsedMs = taskReadCompletedAtElapsedMs;
            this.taskReadInFlight = taskReadInFlight;
            this.lastTaskReadKnown = lastTaskReadKnown;
        }
    }

    private CarPlayClusterHostPolicy() {
    }

    public static boolean allows(Snapshot snapshot, long nowElapsedMs) {
        return reason(snapshot, nowElapsedMs) == Reason.ALLOWED;
    }

    public static Reason reason(Snapshot snapshot, long nowElapsedMs) {
        return reason(snapshot, nowElapsedMs, false);
    }

    public static Reason reason(Snapshot snapshot, long nowElapsedMs, boolean allowActiveFreshnessGrace) {
        if (snapshot == null) return Reason.SNAPSHOT_MISSING;
        // These inputs are sampled synchronously by the display host and remain immediate even
        // while one slow ActivityManager read is tolerated for an already-active Surface.
        if (snapshot.displayId != 3) return Reason.WRONG_DISPLAY;
        if (!snapshot.explicitlyEnabled) return Reason.EXPERIMENT_DISABLED;
        if (!snapshot.slotOwned) return Reason.SLOT_NOT_OWNED;
        if (!snapshot.androidAutoKnownInactive) return Reason.AA_NOT_KNOWN_INACTIVE;
        if (!snapshot.safetyUiClear) return Reason.SAFETY_UI_ACTIVE;
        if (!snapshot.otherAppsClear) return Reason.OTHER_APP_ACTIVE;
        if (!snapshot.taskObservationKnown || snapshot.observedAtElapsedMs < 0) return Reason.TASK_OBSERVATION_UNKNOWN;
        if (nowElapsedMs < snapshot.observedAtElapsedMs) return Reason.SNAPSHOT_FUTURE;
        if (nowElapsedMs - snapshot.observedAtElapsedMs > MAX_SNAPSHOT_AGE_MS &&
                !(allowActiveFreshnessGrace && usesActiveFreshnessGrace(snapshot, nowElapsedMs))) {
            return Reason.SNAPSHOT_STALE;
        }
        // Task-derived negative evidence is never graceable. A newly completed known sample
        // replaces the prior observation before this method is called.
        if (snapshot.carPlayOnSecondary) return Reason.CARPLAY_SECONDARY_PRESENT;
        if (!snapshot.carPlayOnMain) return Reason.CARPLAY_MAIN_UNCONFIRMED;
        return Reason.ALLOWED;
    }

    public static boolean usesActiveFreshnessGrace(Snapshot snapshot, long nowElapsedMs) {
        if (snapshot == null || !snapshot.taskObservationKnown || snapshot.observedAtElapsedMs < 0 ||
                nowElapsedMs < snapshot.observedAtElapsedMs) return false;
        long age = nowElapsedMs - snapshot.observedAtElapsedMs;
        if (age <= MAX_SNAPSHOT_AGE_MS || age > ACTIVE_GRACE_MAX_AGE_MS) return false;
        if (snapshot.taskReadInFlight) {
            return snapshot.taskReadStartedAtElapsedMs >= snapshot.observedAtElapsedMs &&
                nowElapsedMs >= snapshot.taskReadStartedAtElapsedMs &&
                nowElapsedMs - snapshot.taskReadStartedAtElapsedMs <= MAX_READ_IN_FLIGHT_MS;
        }
        return !snapshot.lastTaskReadKnown &&
            snapshot.taskReadCompletedAtElapsedMs >= snapshot.observedAtElapsedMs &&
            nowElapsedMs >= snapshot.taskReadCompletedAtElapsedMs &&
            nowElapsedMs - snapshot.taskReadCompletedAtElapsedMs <= MAX_READ_IN_FLIGHT_MS;
    }
}
