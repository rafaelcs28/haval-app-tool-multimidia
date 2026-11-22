package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class DashcamGpsDecimateTest {
    @Test
    public void keepsMovesGapsAndTheLastFix() {
        List<DashcamGpsTrack.Fix> fixes = new ArrayList<>();
        // Parked for 10 s (same spot), then 1 fix per second ~11 m apart.
        for (int i = 0; i < 10; i++) fixes.add(new DashcamGpsTrack.Fix(1000L * i, -16.0, -49.0, 0f, 0f, 3f));
        for (int i = 1; i <= 5; i++) fixes.add(new DashcamGpsTrack.Fix(10_000L + 1000L * i, -16.0 + 0.0001 * i, -49.0, 11f, 0f, 3f));
        List<DashcamGpsTrack.Fix> out = DashcamGpsTrack.decimate(fixes, 8.0, 15_000L);
        // First parked fix, the 5 moving ones; the parked repeats are dropped.
        assertEquals(6, out.size());
        assertSame(fixes.get(fixes.size() - 1), out.get(out.size() - 1));
        assertEquals(11.1, DashcamGpsTrack.metres(fixes.get(0), fixes.get(10)), 0.2);
    }
}
