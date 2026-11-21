package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class DashcamCatalogSessionTest {
    /** Fake camera following the observed firmware: lists only while paused. */
    private static final class FakeCamera implements DashcamCatalogSession.Camera {
        boolean running = true;
        boolean emergency = false;
        int files = 3;
        boolean failStart = false;
        boolean failStatusAfterStop = false;
        boolean repeatPages = false;
        final List<String> calls = new ArrayList<>();

        @Override
        public DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> getWorkState() {
            calls.add("state");
            if (failStatusAfterStop && !running) return DashcamHisiliconClient.Result.failure("transport_x");
            return DashcamHisiliconClient.Result.success(
                    new DashcamHisiliconProtocol.WorkState("NORM_REC", running, emergency));
        }

        @Override
        public DashcamHisiliconClient.Result<Boolean> setRecording(boolean recording) {
            calls.add(recording ? "start" : "stop");
            if (recording && failStart) return DashcamHisiliconClient.Result.failure("transport_x");
            running = recording;
            return DashcamHisiliconClient.Result.success(Boolean.TRUE);
        }

        @Override
        public DashcamHisiliconClient.Result<DashcamHisiliconProtocol.FileListPage> getFileListPage(
                String dir, int start, int end) {
            calls.add("list:" + dir + ":" + start);
            if (running) return DashcamHisiliconClient.Result.failure("busy_recording");
            List<String> paths = new ArrayList<>();
            if ("norm".equals(dir)) {
                int from = repeatPages ? 0 : start;
                for (int i = from; i <= end && i < files; i++) {
                    paths.add(String.format("sd//norm/2026_11_21_%06d_00.MP4", 80000 + i));
                }
            }
            return DashcamHisiliconClient.Result.success(
                    new DashcamHisiliconProtocol.FileListPage(paths, 0));
        }
    }

    private static final class Mark implements DashcamCatalogSession.PauseMark {
        boolean value;

        @Override
        public boolean isSet() {
            return value;
        }

        @Override
        public boolean set(boolean owned) {
            value = owned;
            return true;
        }
    }

    private static DashcamCatalogSession session(FakeCamera camera, Mark mark) {
        return new DashcamCatalogSession(camera, mark, () -> 0L);
    }

    @Test
    public void recordingCameraNeedsConfirmationAndReceivesNoCommand() {
        FakeCamera camera = new FakeCamera();
        DashcamCatalogSession.Outcome outcome = session(camera, new Mark()).run(false);

        assertEquals(DashcamCatalogSession.Status.NEEDS_PAUSE, outcome.status);
        assertFalse(camera.calls.contains("stop"));
        assertTrue(camera.running);
    }

    @Test
    public void confirmedPauseListsAndResumes() {
        FakeCamera camera = new FakeCamera();
        Mark mark = new Mark();
        DashcamCatalogSession.Outcome outcome = session(camera, mark).run(true);

        assertEquals(DashcamCatalogSession.Status.OK, outcome.status);
        assertEquals(3, outcome.paths.size());
        assertTrue(outcome.pausedForCatalog);
        assertEquals(Boolean.TRUE, outcome.resumeConfirmed);
        assertTrue(camera.running);
        assertFalse(mark.value);
        assertTrue(camera.calls.indexOf("stop") < camera.calls.indexOf("list:norm:0"));
        assertTrue(camera.calls.lastIndexOf("start") > camera.calls.indexOf("list:emr:0"));
    }

    @Test
    public void existingPauseIsPreservedAndNeverResumed() {
        FakeCamera camera = new FakeCamera();
        camera.running = false;
        DashcamCatalogSession.Outcome outcome = session(camera, new Mark()).run(false);

        assertEquals(DashcamCatalogSession.Status.OK, outcome.status);
        assertNull(outcome.resumeConfirmed);
        assertFalse(camera.calls.contains("start"));
        assertFalse(camera.running);
    }

    @Test
    public void emergencyRecordingIsNeverPaused() {
        FakeCamera camera = new FakeCamera();
        camera.emergency = true;
        DashcamCatalogSession.Outcome outcome = session(camera, new Mark()).run(true);

        assertEquals(DashcamCatalogSession.Status.FAILED, outcome.status);
        assertEquals("emergency_recording", outcome.error);
        assertFalse(camera.calls.contains("stop"));
    }

    @Test
    public void lostResumeKeepsTheMarkAndBlocksTheNextPause() {
        FakeCamera camera = new FakeCamera();
        camera.failStart = true;
        Mark mark = new Mark();
        DashcamCatalogSession.Outcome outcome = session(camera, mark).run(true);

        assertEquals(Boolean.FALSE, outcome.resumeConfirmed);
        assertTrue(mark.value);

        camera.calls.clear();
        DashcamCatalogSession.Outcome next = session(camera, mark).run(true);
        assertEquals(DashcamCatalogSession.Status.FAILED, next.status);
        assertEquals("resume_unconfirmed", next.error);
        assertFalse(camera.calls.contains("stop"));

        camera.failStart = false;
        assertEquals(Boolean.TRUE, session(camera, mark).resumeOwnedPause());
        assertFalse(mark.value);
        assertTrue(camera.running);
    }

    @Test
    public void unverifiedPauseStillAttemptsResumeAndSkipsListing() {
        FakeCamera camera = new FakeCamera();
        camera.failStatusAfterStop = true;
        Mark mark = new Mark();
        DashcamCatalogSession.Outcome outcome = session(camera, mark).run(true);

        assertEquals(DashcamCatalogSession.Status.FAILED, outcome.status);
        assertTrue(outcome.error.startsWith("pause_verify_"));
        assertFalse(camera.calls.contains("list:norm:0"));
        assertEquals(Boolean.FALSE, outcome.resumeConfirmed);
        assertTrue(mark.value);
    }

    @Test
    public void paginatesAndDetectsRepeatedPages() {
        FakeCamera camera = new FakeCamera();
        camera.files = 120;
        DashcamCatalogSession.Outcome outcome = session(camera, new Mark()).run(true);
        assertEquals(120, outcome.paths.size());
        assertTrue(camera.calls.contains("list:norm:100"));

        FakeCamera repeating = new FakeCamera();
        repeating.files = 120;
        repeating.repeatPages = true;
        DashcamCatalogSession.Outcome partial = session(repeating, new Mark()).run(true);
        assertEquals(DashcamCatalogSession.Status.PARTIAL, partial.status);
        assertEquals("norm_repeated_page", partial.error);
        assertEquals(50, partial.paths.size());
        assertEquals(Boolean.TRUE, partial.resumeConfirmed);
    }

    @Test
    public void timeBudgetStopsListingButStillResumes() {
        FakeCamera camera = new FakeCamera();
        long[] now = {0L};
        DashcamCatalogSession session = new DashcamCatalogSession(camera, new Mark(), () -> {
            now[0] += 4_000L;
            return now[0];
        });
        camera.files = 500;
        DashcamCatalogSession.Outcome outcome = session.run(true);

        assertEquals(DashcamCatalogSession.Status.PARTIAL, outcome.status);
        assertTrue(outcome.error.endsWith("time_budget"));
        assertEquals(Boolean.TRUE, outcome.resumeConfirmed);
        assertTrue(camera.running);
    }
}
