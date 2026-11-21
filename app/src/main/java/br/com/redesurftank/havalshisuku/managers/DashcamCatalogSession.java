package br.com.redesurftank.havalshisuku.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One catalog read: the tested firmware refuses to list while it records (-2222), so the gallery
 * may pause briefly, list with a short budget and resume in a finally block.
 *
 * <p>Rules: a pause that already existed (manual or charging) is preserved and never resumed here.
 * Emergency recording is never paused. The pause mark is persisted before {@code stop} so a crash
 * between the two calls is recovered later. A resume is only reported after {@code running=true}.
 * The caller must hold {@link DashcamHisiliconClient#CAMERA_LOCK}.
 */
final class DashcamCatalogSession {
    static final int PAGE_SIZE = 50;
    static final int MAX_PAGES_PER_DIR = 80;
    static final long LIST_BUDGET_MS = 6_000L;
    static final String[] DIRS = {"norm", "emr"};

    interface Camera {
        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> getWorkState();

        DashcamHisiliconClient.Result<Boolean> setRecording(boolean recording);

        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.FileListPage> getFileListPage(
                String dir, int start, int end);
    }

    /** Persistent "paused by the gallery" mark (device-protected, excluded from backup). */
    interface PauseMark {
        boolean isSet();

        boolean set(boolean owned);
    }

    interface Clock {
        long elapsedMs();
    }

    enum Status {
        OK,
        PARTIAL,
        NEEDS_PAUSE,
        FAILED
    }

    static final class Outcome {
        final Status status;
        final List<String> paths;
        final String error;
        final boolean pausedForCatalog;
        /** Null when nothing was paused here; otherwise whether running=true was observed. */
        final Boolean resumeConfirmed;
        final int rejectedEntries;

        Outcome(Status status, List<String> paths, String error, boolean pausedForCatalog,
                Boolean resumeConfirmed, int rejectedEntries) {
            this.status = status;
            this.paths = Collections.unmodifiableList(paths);
            this.error = error;
            this.pausedForCatalog = pausedForCatalog;
            this.resumeConfirmed = resumeConfirmed;
            this.rejectedEntries = rejectedEntries;
        }

        static Outcome failed(String error) {
            return new Outcome(Status.FAILED, Collections.<String>emptyList(), error, false, null, 0);
        }
    }

    private final Camera camera;
    private final PauseMark mark;
    private final Clock clock;

    DashcamCatalogSession(Camera camera, PauseMark mark, Clock clock) {
        this.camera = camera;
        this.mark = mark;
        this.clock = clock;
    }

    /**
     * @param allowPause the user confirmed the short pause. Without it a recording camera yields
     *     {@link Status#NEEDS_PAUSE} and nothing is sent.
     */
    Outcome run(boolean allowPause) {
        if (mark.isSet()) {
            // A previous gallery pause was not confirmed as resumed: verify that first, never stack
            // another pause on top of an unresolved one.
            Boolean resumed = resumeOwnedPause();
            if (!Boolean.TRUE.equals(resumed)) return Outcome.failed("resume_unconfirmed");
        }

        DashcamClientResult<DashcamHisiliconProtocol.WorkState> state = wrap(camera.getWorkState());
        if (!state.ok) return Outcome.failed("status_" + state.error);

        boolean pausedHere = false;
        if (Boolean.TRUE.equals(state.value.getRunning())) {
            if (!state.value.canPause()) return Outcome.failed("emergency_recording");
            if (!allowPause) {
                return new Outcome(Status.NEEDS_PAUSE, Collections.<String>emptyList(), null,
                        false, null, 0);
            }
            if (!mark.set(true)) return Outcome.failed("persist_failed");
            pausedHere = true;
        }

        Set<String> collected = new LinkedHashSet<>();
        int[] rejected = {0};
        String error = null;
        boolean partial = false;
        Boolean resumeConfirmed = null;
        try {
            if (pausedHere) {
                DashcamClientResult<Boolean> stop = wrap(camera.setRecording(false));
                if (!stop.ok) {
                    error = "pause_" + stop.error;
                } else {
                    DashcamClientResult<DashcamHisiliconProtocol.WorkState> verified =
                            wrap(camera.getWorkState());
                    if (!verified.ok) error = "pause_verify_" + verified.error;
                    else if (!Boolean.FALSE.equals(verified.value.getRunning())) {
                        error = "pause_verify_still_running";
                    }
                }
            }
            if (error == null) {
                long deadline = clock.elapsedMs() + LIST_BUDGET_MS;
                for (String dir : DIRS) {
                    String dirError = listDir(dir, deadline, collected, rejected);
                    if (dirError != null) {
                        partial = true;
                        if (error == null) error = dir + "_" + dirError;
                        if ("norm".equals(dir) && collected.isEmpty()) break;
                    }
                }
            }
        } finally {
            if (pausedHere) resumeConfirmed = resumeOwnedPause();
        }

        List<String> paths = new ArrayList<>(collected);
        if (error != null && paths.isEmpty()) {
            return new Outcome(Status.FAILED, paths, error, pausedHere, resumeConfirmed, rejected[0]);
        }
        return new Outcome(partial ? Status.PARTIAL : Status.OK, paths, error, pausedHere,
                resumeConfirmed, rejected[0]);
    }

    /** Resumes only a pause marked as the gallery's. Returns true once running=true is observed. */
    Boolean resumeOwnedPause() {
        if (!mark.isSet()) return Boolean.TRUE;
        DashcamClientResult<DashcamHisiliconProtocol.WorkState> state = wrap(camera.getWorkState());
        if (!state.ok) return Boolean.FALSE;
        if (Boolean.TRUE.equals(state.value.getRunning())) {
            return mark.set(false) ? Boolean.TRUE : Boolean.FALSE;
        }
        if (!Boolean.FALSE.equals(state.value.getEmergencyRecording())) return Boolean.FALSE;
        DashcamClientResult<Boolean> start = wrap(camera.setRecording(true));
        if (!start.ok) return Boolean.FALSE;
        DashcamClientResult<DashcamHisiliconProtocol.WorkState> verified = wrap(camera.getWorkState());
        if (!verified.ok || !Boolean.TRUE.equals(verified.value.getRunning())) return Boolean.FALSE;
        return mark.set(false) ? Boolean.TRUE : Boolean.FALSE;
    }

    private String listDir(String dir, long deadline, Set<String> out, int[] rejected) {
        String previousFirst = null;
        for (int page = 0; page < MAX_PAGES_PER_DIR; page++) {
            if (clock.elapsedMs() >= deadline) return "time_budget";
            int start = page * PAGE_SIZE;
            DashcamClientResult<DashcamHisiliconProtocol.FileListPage> result =
                    wrap(camera.getFileListPage(dir, start, start + PAGE_SIZE - 1));
            if (!result.ok) return result.error;
            List<String> pagePaths = result.value.getPaths();
            int entries = pagePaths.size() + result.value.getRejected();
            rejected[0] += result.value.getRejected();
            if (!pagePaths.isEmpty()) {
                if (pagePaths.get(0).equals(previousFirst)) return "repeated_page";
                previousFirst = pagePaths.get(0);
            }
            out.addAll(pagePaths);
            if (entries < PAGE_SIZE) return null;
        }
        return "page_limit";
    }

    private static <T> DashcamClientResult<T> wrap(DashcamHisiliconClient.Result<T> result) {
        if (result == null) return new DashcamClientResult<>(false, null, "null_result");
        return new DashcamClientResult<>(result.isSuccess(), result.getValue(), result.getError());
    }

    private static final class DashcamClientResult<T> {
        final boolean ok;
        final T value;
        final String error;

        DashcamClientResult(boolean ok, T value, String error) {
            this.ok = ok;
            this.value = value;
            this.error = error;
        }
    }
}
