package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class DashcamClipTrackTest {
    @Test
    public void keptDataRoundTrips() {
        List<DashcamGpsTrack.Fix> fixes = Arrays.asList(
                new DashcamGpsTrack.Fix(1_795_000_000_000L, -16.686892, -49.264794, 12.5f, 90f, 4f),
                new DashcamGpsTrack.Fix(1_795_000_001_000L, -16.686900, -49.264700, 13.0f, 91f, 4f));
        Map<String, String> v = new HashMap<>();
        v.put("speed", "45");
        v.put("gear", "4");
        List<DashcamCarTrack.Sample> samples = new ArrayList<>();
        samples.add(new DashcamCarTrack.Sample(1_795_000_000_500L, v));

        DashcamClipTrack.Data back = DashcamClipTrack.parse(DashcamClipTrack.format(fixes, samples));

        assertEquals(2, back.fixes.size());
        assertEquals(-16.686892, back.fixes.get(0).lat, 1e-6);
        assertEquals(13.0f, back.fixes.get(1).speed, 0.05f);
        assertEquals(1, back.samples.size());
        assertEquals("45", back.samples.get(0).get("speed"));
        assertEquals("4", back.samples.get(0).get("gear"));
        assertEquals(1_795_000_000_500L, back.samples.get(0).utcMs);
    }

    @Test
    public void foreignTextGivesNoData() {
        assertTrue(DashcamClipTrack.parse("not a track").isEmpty());
        assertTrue(DashcamClipTrack.parse(null).isEmpty());
    }

    @Test
    public void stampedCopyAndTsShareTheOriginalsFile() {
        assertEquals("2026_1124_190312_001.track", DashcamClipTrack.fileName("2026_1124_190312_001.TS"));
        assertEquals("2026_1124_190312_001.track", DashcamClipTrack.fileName("2026_1124_190312_001.mp4"));
        assertEquals("2026_1124_190312_001.track", DashcamClipTrack.fileName("2026_1124_190312_001_carimbo.mp4"));
    }
}
