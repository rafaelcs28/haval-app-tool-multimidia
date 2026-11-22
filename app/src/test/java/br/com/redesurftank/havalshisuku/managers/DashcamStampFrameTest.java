package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class DashcamStampFrameTest {
    private static String[] cols() {
        return DashcamCarTrack.header().substring(6).split(",");
    }

    /** belt = cluster lamp value; the per-seat vector column is always "{1,0,0,0,0}" here. */
    private static DashcamCarTrack.Sample sample(long utc, String speed, String gear, String belt, String left) {
        // speed|gear|belt|left|right|hazard|low|high|handbrake|steer|mode|soc|power|tin|tout|odo
        String line = utc + "|" + speed + "|" + gear + "|" + "{1,0,0,0,0}|" + left + "|0|0|0|1|0|-12.5|2|72.0|-15.3|23|31|12345|-35.0|1450|{0,0,0,0,0}|1|" + belt;
        return DashcamCarTrack.parseLine(line, cols());
    }

    @Test
    public void carLinesParseByHeaderAndUnknownColumnsAreIgnored() {
        assertTrue(DashcamCarTrack.header().startsWith("#keys speed,gear,belt,"));
        DashcamCarTrack.Sample s = sample(1000L, "48.5", "2", "0", "1");
        assertNotNull(s);
        assertEquals("48.5", s.get("speed"));
        assertEquals("", s.get("missing"));
        assertNull(DashcamCarTrack.parseLine("x|1", cols()));
        assertNull(DashcamCarTrack.parseLine("1000|1", null));
        assertEquals("a/b", DashcamCarTrack.clean(" a|b "));
        List<DashcamCarTrack.Sample> list = Arrays.asList(sample(1000L, "1", "2", "0", "0"), sample(2000L, "2", "2", "0", "0"));
        assertEquals(1000L, DashcamCarTrack.at(list, 1500L).utcMs);
        assertNull(DashcamCarTrack.at(list, 500L));
        assertNull(DashcamCarTrack.at(list, 9000L));
    }

    @Test
    public void frameInterpretsSignals() {
        List<DashcamCarTrack.Sample> samples = new ArrayList<>();
        // 36 km/h -> 54 km/h over 2 s = 2.5 m/s^2 = 0.255 g
        long t0 = 1_795_361_560_000L;
        samples.add(sample(t0, "36", "2", "0", "0"));
        samples.add(sample(t0 + 1000L, "45", "2", "1", "1"));
        samples.add(sample(t0 + 2000L, "54", "2", "1", "1"));
        List<DashcamGpsTrack.Fix> fixes = Arrays.asList(
                new DashcamGpsTrack.Fix(t0, -16.0, -49.0, 10f, 80f, 3f),
                new DashcamGpsTrack.Fix(t0 + 2000L, -16.0, -48.9998, 15f, 80f, 3f));
        DashcamStampFrame f = DashcamStampFrame.build(t0 + 1000L, fixes, samples);
        assertEquals(45f, f.speedKmh, 1e-4);
        assertEquals(0.255f, f.accelG, 0.002f);
        assertEquals("D", f.gear);
        assertTrue(f.beltWarning);
        assertTrue(f.left);
        assertFalse(f.right);
        assertTrue(f.highBeam);
        assertEquals(-12.5f, f.steerDeg, 1e-4);
        assertEquals("Eco", f.driveMode);
        assertEquals(72f, f.soc, 1e-4);
        assertEquals(-15.3f, f.powerKw, 1e-4);
        assertEquals(23f, f.insideC, 1e-4);
        assertEquals(31f, f.outsideC, 1e-4);
        assertEquals("L", f.compass);
        assertEquals(-35f, f.outputPct, 1e-4);
        assertEquals(1450f, f.rpm, 1e-4);
        assertTrue(f.engineOn);
        assertFalse(f.doorOpen);
        assertNotNull(f.lat);
    }

    @Test
    public void gpsFillsInWithoutCarData() {
        List<DashcamGpsTrack.Fix> fixes = Arrays.asList(
                new DashcamGpsTrack.Fix(0L, -16.0, -49.0, 10f, 350f, 3f),
                new DashcamGpsTrack.Fix(2000L, -16.0, -49.0, 10f, 350f, 3f));
        DashcamStampFrame f = DashcamStampFrame.build(1000L, fixes, null);
        assertEquals(36f, f.speedKmh, 1e-3);
        assertEquals(0f, f.accelG, 1e-4);
        assertNull(f.gear);
        assertNull(f.beltWarning);
        assertEquals("N", f.compass);
    }

    @Test
    public void mappings() {
        assertEquals("P", DashcamStampFrame.gear("3"));
        assertEquals("R", DashcamStampFrame.gear("4"));
        assertEquals("N", DashcamStampFrame.gear("1"));
        assertNull(DashcamStampFrame.gear(""));
        assertEquals("Sport", DashcamStampFrame.driveMode("1"));
        assertEquals("Normal", DashcamStampFrame.driveMode("0"));
        assertEquals("NE", DashcamStampFrame.compass(45f));
        assertEquals("SO", DashcamStampFrame.compass(225f));
        assertEquals("NO", DashcamStampFrame.compass(-45f));
        assertEquals(-2f, DashcamStampFrame.toG(100f, 0f, 100L), 1e-6);
        assertEquals("1,25", DashcamStampFrame.decimal(1.25f, 2));
    }

    @Test
    public void painterHelpers() {
        assertEquals("ENE", DashcamStampPainter.compass16(73f));
        assertEquals("N", DashcamStampPainter.compass16(355f));
        assertEquals("SSO", DashcamStampPainter.compass16(200f));
        assertEquals("12.483", DashcamStampPainter.thousands(12483));
        assertEquals("1.450", DashcamStampPainter.thousands(1450));
        assertEquals("M9.70 4.20a2.30 2.30 0 1 0 4.60 0a2.30 2.30 0 1 0 -4.60 0z", DashcamStampPainter.circle(12f, 4.2f, 2.3f));
    }

    @Test
    public void beltFollowsTheClusterLampNotTheStuckVector() {
        String[] cols = DashcamCarTrack.header().substring(6).split(",");
        java.util.function.BiFunction<Long, String, DashcamCarTrack.Sample> lamp = (utc, v) ->
                DashcamCarTrack.parseLine(utc + "|50|2|{1,0,0,0,0}|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|3|" + v, cols);
        // Vector stuck at {1,0,0,0,0} with the lamp dark: no warning (seen for 20+ min in the car).
        assertFalse(DashcamStampFrame.beltWarning("{1,0,0,0,0}", Arrays.asList(lamp.apply(10_000L, "0")), 10_000L));
        // Lamp blinking: lit 1.5 s ago still counts.
        assertTrue(DashcamStampFrame.beltWarning("{1,0,0,0,0}",
                Arrays.asList(lamp.apply(8_500L, "1"), lamp.apply(9_500L, "0")), 10_000L));
        // Lamp lit but vector clear: no warning.
        assertFalse(DashcamStampFrame.beltWarning("{0,0,0,0,0}", Arrays.asList(lamp.apply(10_000L, "1")), 10_000L));
        // Lamp never reported: unknown.
        assertNull(DashcamStampFrame.beltWarning("{1,0,0,0,0}", Arrays.asList(lamp.apply(10_000L, "")), 10_000L));
        assertFalse(DashcamStampFrame.warningActive("{0,0,0,0,0}"));
        assertTrue(DashcamStampFrame.warningActive("{0,1,0,0,0}"));
    }
}
