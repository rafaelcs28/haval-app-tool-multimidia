package br.com.redesurftank.havalshisuku.managers;

import java.util.List;
import java.util.Locale;

/**
 * Everything the stamp shows at one instant of a clip, already interpreted. Built from the GPS
 * trail ({@link DashcamGpsTrack}) and the car signals ({@link DashcamCarTrack}); any value the car
 * did not report stays null and the stamp leaves it out.
 */
public final class DashcamStampFrame {
    public long utcMs;
    /** km/h: the car's speedometer when known, else the GPS. */
    public Float speedKmh;
    /** Longitudinal acceleration in g (negative = braking), from the speed change over ~2 s. */
    public Float accelG;
    public String gear;
    /** True = seat belt warning on (driver not buckled). */
    public Boolean beltWarning;
    public boolean left;
    public boolean right;
    public boolean hazard;
    public boolean lowBeam;
    public boolean highBeam;
    public Boolean handBrake;
    public Float steerDeg;
    public String driveMode;
    public Float soc;
    /** kW; negative = regeneration. */
    public Float powerKw;
    public Float insideC;
    public Float outsideC;
    public Float odometerKm;
    public Double lat;
    public Double lon;
    public Float bearing;
    public String compass;

    public static DashcamStampFrame build(long utcMs, List<DashcamGpsTrack.Fix> fixes, List<DashcamCarTrack.Sample> samples) {
        DashcamStampFrame f = new DashcamStampFrame();
        f.utcMs = utcMs;
        DashcamGpsTrack.Fix fix = DashcamGpsTrack.interpolate(fixes, utcMs);
        if (fix != null) {
            f.lat = fix.lat;
            f.lon = fix.lon;
            if (fix.bearing >= 0) {
                f.bearing = fix.bearing;
                f.compass = compass(fix.bearing);
            }
        }
        DashcamCarTrack.Sample s = samples == null ? null : DashcamCarTrack.at(samples, utcMs);
        Float carSpeed = s == null ? null : number(s.get("speed"));
        if (carSpeed != null && carSpeed >= 0 && carSpeed < 300) f.speedKmh = carSpeed;
        else if (fix != null && fix.speed >= 0) f.speedKmh = fix.speed * 3.6f;
        f.accelG = accel(utcMs, samples, fixes);
        if (s != null) {
            f.gear = gear(s.get("gear"));
            String belt = s.get("belt");
            f.beltWarning = belt.isEmpty() ? null : !"0".equals(belt);
            f.left = on(s.get("left"));
            f.right = on(s.get("right"));
            f.hazard = on(s.get("hazard"));
            f.lowBeam = on(s.get("low"));
            f.highBeam = on(s.get("high"));
            String hb = s.get("handbrake");
            f.handBrake = hb.isEmpty() ? null : on(hb);
            f.steerDeg = number(s.get("steer"));
            f.driveMode = driveMode(s.get("mode"));
            f.soc = range(number(s.get("soc")), 0f, 100f);
            f.powerKw = range(number(s.get("power")), -400f, 400f);
            f.insideC = range(number(s.get("tin")), -40f, 85f);
            f.outsideC = range(number(s.get("tout")), -40f, 85f);
            f.odometerKm = range(number(s.get("odo")), 0f, 2_000_000f);
        }
        return f;
    }

    /** Δv over the samples about 1 s either side, in g; null without two speeds. */
    static Float accel(long utcMs, List<DashcamCarTrack.Sample> samples, List<DashcamGpsTrack.Fix> fixes) {
        if (samples != null && !samples.isEmpty()) {
            DashcamCarTrack.Sample a = DashcamCarTrack.at(samples, utcMs - 1_000L);
            DashcamCarTrack.Sample b = DashcamCarTrack.at(samples, utcMs + 1_000L);
            if (a != null && b != null && b.utcMs > a.utcMs) {
                Float va = number(a.get("speed"));
                Float vb = number(b.get("speed"));
                if (va != null && vb != null) return toG(va, vb, b.utcMs - a.utcMs);
            }
        }
        DashcamGpsTrack.Fix a = DashcamGpsTrack.interpolate(fixes, utcMs - 1_000L);
        DashcamGpsTrack.Fix b = DashcamGpsTrack.interpolate(fixes, utcMs + 1_000L);
        if (a != null && b != null && a.speed >= 0 && b.speed >= 0 && b.utcMs > a.utcMs) {
            return toG(a.speed * 3.6f, b.speed * 3.6f, b.utcMs - a.utcMs);
        }
        return null;
    }

    static float toG(float kmhFrom, float kmhTo, long ms) {
        float g = (kmhTo - kmhFrom) / 3.6f / (ms / 1000f) / 9.80665f;
        return Math.max(-2f, Math.min(2f, g));
    }

    /** Same mapping the rest of Impulse uses (InstrumentProjector2.getGearLabel). */
    static String gear(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        switch (raw.trim()) {
            case "2": return "D";
            case "3": return "P";
            case "4": return "R";
            default: return "N";
        }
    }

    /** Same mapping as the bottom bar (BottomBarUI.driveModeLabel). */
    static String driveMode(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        switch (raw.trim()) {
            case "2": return "Eco";
            case "1": return "Sport";
            case "3": return "Neve";
            case "4": return "Areia";
            case "5": return "Lama";
            default: return "Normal";
        }
    }

    /** 8-point compass in Portuguese (L = leste, O = oeste). */
    static String compass(float bearing) {
        String[] names = {"N", "NE", "L", "SE", "S", "SO", "O", "NO"};
        int i = (int) Math.floor(((bearing % 360 + 360) % 360 + 22.5) / 45.0) % 8;
        return names[i];
    }

    static boolean on(String raw) {
        return raw != null && !raw.isEmpty() && !"0".equals(raw.trim()) && !"false".equalsIgnoreCase(raw.trim());
    }

    static Float number(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            float v = Float.parseFloat(raw.trim().replace(',', '.'));
            return Float.isNaN(v) || Float.isInfinite(v) ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Float range(Float v, float min, float max) {
        return v == null || v < min || v > max ? null : v;
    }

    /** "1,2" style decimal for the stamp. */
    public static String decimal(float v, int places) {
        return String.format(Locale.ROOT, "%." + places + "f", v).replace('.', ',');
    }
}
