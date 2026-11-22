package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class DashcamGpsTrackTest {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    public void linesRoundTripAndRejectGarbage() {
        DashcamGpsTrack.Fix fix = new DashcamGpsTrack.Fix(1795361560000L, -16.883380, -49.819595, 12.3f, 270f, 4f);
        String line = DashcamGpsTrack.formatLine(fix);
        assertEquals("1795361560000,-16.883380,-49.819595,12.3,270,4", line);
        DashcamGpsTrack.Fix back = DashcamGpsTrack.parseLine(line);
        assertNotNull(back);
        assertEquals(fix.utcMs, back.utcMs);
        assertEquals(fix.lat, back.lat, 1e-6);
        assertEquals(fix.lon, back.lon, 1e-6);
        assertNull(DashcamGpsTrack.parseLine("garbage"));
        assertNull(DashcamGpsTrack.parseLine("1,95.0,10.0,0,0,0"));
        assertNull(DashcamGpsTrack.parseLine(""));
    }

    @Test
    public void hourlyFilesAreUtc() {
        // 2026-11-22 15:32:40 UTC
        assertEquals("2026112215.csv", DashcamGpsTrack.fileName(1795361560000L));
        assertEquals(Long.valueOf(1795359600000L), DashcamGpsTrack.hourOf("2026112215.csv"));
        assertNull(DashcamGpsTrack.hourOf("notes.txt"));
        assertEquals(Arrays.asList("2026112214.csv", "2026112215.csv"),
                DashcamGpsTrack.fileNamesBetween(1795359600000L - 1000L, 1795361560000L));
    }

    @Test
    public void clipNameIsCameraLocalTime() {
        // 12:32:40 in Sao Paulo (UTC-3) = 15:32:40 UTC.
        assertEquals(Long.valueOf(1795361560000L),
                DashcamGpsTrack.clipStartUtcMs("20261122123240_035707.TS", SAO_PAULO));
        assertEquals(Long.valueOf(1795361560000L),
                DashcamGpsTrack.clipStartUtcMs("20261122123240_035707.mp4", SAO_PAULO));
        assertNull(DashcamGpsTrack.clipStartUtcMs("video.mp4", SAO_PAULO));
        assertNull(DashcamGpsTrack.clipStartUtcMs(null, SAO_PAULO));
    }

    @Test
    public void interpolatesBetweenFixesAndStaysInsideTheTrail() {
        List<DashcamGpsTrack.Fix> trail = Arrays.asList(
                new DashcamGpsTrack.Fix(1000L, -16.0, -49.0, 10f, 0f, 3f),
                new DashcamGpsTrack.Fix(2000L, -16.0010, -49.0020, 20f, 90f, 3f));
        DashcamGpsTrack.Fix mid = DashcamGpsTrack.interpolate(trail, 1500L);
        assertNotNull(mid);
        assertEquals(-16.0005, mid.lat, 1e-9);
        assertEquals(-49.0010, mid.lon, 1e-9);
        assertEquals(15f, mid.speed, 1e-4);
        assertEquals(trail.get(0), DashcamGpsTrack.interpolate(trail, 0L + 1000L));
        assertNull(DashcamGpsTrack.interpolate(trail, 1000L - 5000L));
        assertNull(DashcamGpsTrack.interpolate(trail, 2000L + 5000L));
        assertNull(DashcamGpsTrack.interpolate(null, 1500L));
    }

    @Test
    public void gpxCarriesGpsTimes() {
        String gpx = DashcamGpsTrack.gpx(Arrays.asList(
                new DashcamGpsTrack.Fix(1795361560000L, -16.88338, -49.819595, 5f, 0f, 3f)), "a<b>.mp4");
        assertTrue(gpx.contains("<trkpt lat=\"-16.883380\" lon=\"-49.819595\"><time>2026-11-22T15:32:40Z</time>"));
        assertTrue(gpx.contains("<name>a&lt;b&gt;.mp4</name>"));
        assertEquals("20261122123240_035707.gpx", DashcamClipStore.gpxName("20261122123240_035707.mp4"));
    }
}
