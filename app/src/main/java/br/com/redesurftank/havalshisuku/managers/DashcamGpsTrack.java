package br.com.redesurftank.havalshisuku.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;

/**
 * GPS trail of the car, so every dashcam clip can be placed on the map. Each fix is stored with
 * the time the GPS itself reports ({@link Location#getTime()}, satellite UTC), never the time it
 * reached us: a clip is matched by its camera start time, which the clock sync keeps on the same
 * trusted UTC. Everything stays on the head unit: hourly CSV files under the app's private storage,
 * kept for {@link #RETENTION_MS} and then deleted. Nothing is uploaded.
 */
public final class DashcamGpsTrack {
    private static final String TAG = "DashcamGps";
    static final String DIR = "dashcam-gps";
    static final long RETENTION_MS = 72L * 60 * 60 * 1000;
    private static final long INTERVAL_MS = 1_000L;
    /** Fixes worse than this are not worth placing a clip with. */
    private static final float MAX_ACCURACY_M = 200f;
    private static final int FLUSH_EVERY = 10;
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("yyyyMMddHH", Locale.ROOT);
    private static final DateTimeFormatter CLIP_NAME_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
    private static final Pattern CLIP_START = Pattern.compile("^([0-9]{14})_");
    /** A camera clip lasts one minute; a little margin either side for the first/last fix. */
    public static final long CLIP_MS = 60_000L;

    private static volatile DashcamGpsTrack instance;

    private final Context context;
    private final SharedPreferences preferences;
    private HandlerThread thread;
    private BufferedWriter writer;
    private String writerFile;
    private int pending;
    private long lastPruneMs;
    private long lastFixUtc;

    public static final class Fix {
        public final long utcMs;
        public final double lat;
        public final double lon;
        /** m/s, or -1 unknown. */
        public final float speed;
        /** Degrees, or -1 unknown. */
        public final float bearing;
        /** Metres, or -1 unknown. */
        public final float accuracy;

        public Fix(long utcMs, double lat, double lon, float speed, float bearing, float accuracy) {
            this.utcMs = utcMs;
            this.lat = lat;
            this.lon = lon;
            this.speed = speed;
            this.bearing = bearing;
            this.accuracy = accuracy;
        }

        public float getSpeedKmh() { return speed < 0 ? -1f : speed * 3.6f; }
    }

    private final LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            try {
                record(location);
            } catch (RuntimeException e) {
                Log.w(TAG, "record failed", e);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    private DashcamGpsTrack(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = App.getDeviceProtectedContext()
                .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
    }

    public static DashcamGpsTrack getInstance(Context context) {
        DashcamGpsTrack local = instance;
        if (local == null) {
            synchronized (DashcamGpsTrack.class) {
                local = instance;
                if (local == null) {
                    local = new DashcamGpsTrack(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    public static boolean isEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(SharedPreferencesKeys.ENABLE_DASHCAM_GPS_TRACK.getKey(), true);
    }

    @SuppressLint("MissingPermission")
    public synchronized void start() {
        if (thread != null) return;
        thread = new HandlerThread("DashcamGps");
        thread.start();
        try {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (manager == null) throw new IllegalStateException("no LocationManager");
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_MS, 0f, listener,
                    thread.getLooper());
            Log.i(TAG, "started");
        } catch (RuntimeException e) {
            Log.w(TAG, "GPS unavailable", e);
        }
    }

    public synchronized void stop() {
        try {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (manager != null) manager.removeUpdates(listener);
        } catch (RuntimeException ignored) {
        }
        closeWriter();
        if (thread != null) thread.quitSafely();
        thread = null;
    }

    private synchronized void record(Location location) {
        if (!isEnabled(preferences)) {
            closeWriter();
            return;
        }
        long utc = location.getTime();
        if (utc <= 0 || utc == lastFixUtc) return;
        if (location.hasAccuracy() && location.getAccuracy() > MAX_ACCURACY_M) return;
        lastFixUtc = utc;
        Fix fix = new Fix(utc, location.getLatitude(), location.getLongitude(),
                location.hasSpeed() ? location.getSpeed() : -1f,
                location.hasBearing() ? location.getBearing() : -1f,
                location.hasAccuracy() ? location.getAccuracy() : -1f);
        String name = fileName(utc);
        try {
            if (!name.equals(writerFile)) {
                closeWriter();
                File dir = directory();
                writer = new BufferedWriter(new FileWriter(new File(dir, name), true));
                writerFile = name;
            }
            writer.write(formatLine(fix));
            writer.write('\n');
            if (++pending >= FLUSH_EVERY) flushWriter();
        } catch (IOException e) {
            Log.w(TAG, "write failed", e);
            closeWriter();
        }
        if (utc - lastPruneMs > 60L * 60 * 1000) {
            lastPruneMs = utc;
            prune(utc);
            // Map tiles along the last day's trail, only on an unmetered Wi-Fi (home).
            try {
                MapTiles.prefetchAsync(context, decimate(query(utc - 24L * 60 * 60 * 1000, utc), 40, 60_000L));
            } catch (RuntimeException e) {
                Log.w(TAG, "prefetch skipped", e);
            }
        }
    }

    /** Fixes with {@code fromUtc <= time <= toUtc}, oldest first. Safe from any thread. */
    public List<Fix> query(long fromUtc, long toUtc) {
        synchronized (this) {
            flushWriter();
        }
        List<Fix> out = new ArrayList<>();
        File dir = directory();
        for (String name : fileNamesBetween(fromUtc, toUtc)) {
            File file = new File(dir, name);
            if (!file.isFile()) continue;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Fix fix = parseLine(line);
                    if (fix != null && fix.utcMs >= fromUtc && fix.utcMs <= toUtc) out.add(fix);
                }
            } catch (IOException e) {
                Log.w(TAG, "read " + name + " failed", e);
            }
        }
        Collections.sort(out, (a, b) -> Long.compare(a.utcMs, b.utcMs));
        return out;
    }

    /** The trail of one camera clip (by its file name), or empty when there is none. */
    public List<Fix> forClip(String clipName) {
        Long start = clipStartUtcMs(clipName, ZoneId.systemDefault());
        if (start == null) return Collections.emptyList();
        return query(start - 2_000L, start + CLIP_MS + 2_000L);
    }

    private File directory() {
        File dir = new File(context.getFilesDir(), DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) Log.w(TAG, "cannot create " + dir);
        return dir;
    }

    private void prune(long nowUtc) {
        File[] files = directory().listFiles();
        if (files == null) return;
        for (File file : files) {
            Long hour = hourOf(file.getName());
            if (hour != null && hour + 60L * 60 * 1000 < nowUtc - RETENTION_MS && !file.delete()) {
                Log.w(TAG, "cannot delete " + file);
            }
        }
    }

    private void flushWriter() {
        if (writer == null) return;
        try {
            writer.flush();
            pending = 0;
        } catch (IOException e) {
            Log.w(TAG, "flush failed", e);
        }
    }

    private void closeWriter() {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException ignored) {
        }
        writer = null;
        writerFile = null;
        pending = 0;
    }

    // ---- pure helpers (unit tested) ----

    static String formatLine(Fix fix) {
        return fix.utcMs + "," + String.format(Locale.ROOT, "%.6f,%.6f,%.1f,%.0f,%.0f",
                fix.lat, fix.lon, fix.speed, fix.bearing, fix.accuracy);
    }

    static Fix parseLine(String line) {
        if (line == null) return null;
        String[] p = line.trim().split(",");
        if (p.length != 6) return null;
        try {
            long utc = Long.parseLong(p[0]);
            double lat = Double.parseDouble(p[1]);
            double lon = Double.parseDouble(p[2]);
            if (utc <= 0 || Math.abs(lat) > 90 || Math.abs(lon) > 180) return null;
            return new Fix(utc, lat, lon, Float.parseFloat(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** One file per UTC hour: {@code yyyyMMddHH.csv}. */
    static String fileName(long utcMs) {
        return Instant.ofEpochMilli(utcMs).atZone(ZoneOffset.UTC).format(HOUR) + ".csv";
    }

    static Long hourOf(String fileName) {
        if (fileName == null || !fileName.matches("[0-9]{10}\\.csv")) return null;
        try {
            return LocalDateTime.parse(fileName.substring(0, 10) + "0000", CLIP_NAME_TIME)
                    .toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static List<String> fileNamesBetween(long fromUtc, long toUtc) {
        List<String> names = new ArrayList<>();
        if (toUtc < fromUtc || toUtc - fromUtc > RETENTION_MS * 2) return names;
        long hour = 60L * 60 * 1000;
        for (long t = fromUtc - Math.floorMod(fromUtc, hour); t <= toUtc; t += hour) names.add(fileName(t));
        return names;
    }

    /**
     * Start of a camera clip from its name ({@code 20261121152923_035707.TS} or the saved
     * {@code .mp4}): the camera writes local time, read in {@code zone}. Null when not a clip name.
     */
    public static Long clipStartUtcMs(String clipName, ZoneId zone) {
        if (clipName == null) return null;
        Matcher m = CLIP_START.matcher(clipName);
        if (!m.find()) return null;
        try {
            return LocalDateTime.parse(m.group(1), CLIP_NAME_TIME).atZone(zone).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Position at {@code utcMs}, linearly between the two surrounding fixes; null outside the trail. */
    public static Fix interpolate(List<Fix> fixes, long utcMs) {
        if (fixes == null || fixes.isEmpty()) return null;
        Fix first = fixes.get(0);
        Fix last = fixes.get(fixes.size() - 1);
        if (utcMs <= first.utcMs) return utcMs >= first.utcMs - 3_000L ? first : null;
        if (utcMs >= last.utcMs) return utcMs <= last.utcMs + 3_000L ? last : null;
        for (int i = 1; i < fixes.size(); i++) {
            Fix b = fixes.get(i);
            if (b.utcMs < utcMs) continue;
            Fix a = fixes.get(i - 1);
            if (b.utcMs - a.utcMs > 10_000L) return utcMs - a.utcMs < b.utcMs - utcMs ? a : b;
            double f = (utcMs - a.utcMs) / (double) (b.utcMs - a.utcMs);
            float speed = a.speed < 0 || b.speed < 0 ? -1f : (float) (a.speed + (b.speed - a.speed) * f);
            return new Fix(utcMs, a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f, speed,
                    f < 0.5 ? a.bearing : b.bearing, Math.max(a.accuracy, b.accuracy));
        }
        return last;
    }

    /**
     * Thins a trail for drawing: keeps a fix when it is at least {@code minMeters} from the last kept
     * one or {@code maxGapMs} later, plus the last fix. Order is kept.
     */
    public static List<Fix> decimate(List<Fix> fixes, double minMeters, long maxGapMs) {
        List<Fix> out = new ArrayList<>();
        if (fixes == null || fixes.isEmpty()) return out;
        Fix kept = null;
        for (Fix fix : fixes) {
            if (kept == null || metres(kept, fix) >= minMeters || fix.utcMs - kept.utcMs >= maxGapMs) {
                out.add(fix);
                kept = fix;
            }
        }
        Fix last = fixes.get(fixes.size() - 1);
        if (out.get(out.size() - 1) != last) out.add(last);
        return out;
    }

    /** Equirectangular distance; plenty for metres-scale thinning. */
    public static double metres(Fix a, Fix b) {
        double k = Math.cos(Math.toRadians((a.lat + b.lat) / 2));
        double dx = Math.toRadians(b.lon - a.lon) * k;
        double dy = Math.toRadians(b.lat - a.lat);
        return Math.sqrt(dx * dx + dy * dy) * 6_371_000d;
    }

    /** GPX 1.1 track of the fixes (UTC times from the GPS), for the clip saved on the head unit. */
    public static String gpx(List<Fix> fixes, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<gpx version=\"1.1\" creator=\"Impulse\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
                .append("<trk><name>").append(escape(name)).append("</name><trkseg>\n");
        for (Fix fix : fixes) {
            sb.append(String.format(Locale.ROOT, "<trkpt lat=\"%.6f\" lon=\"%.6f\"><time>%s</time>",
                    fix.lat, fix.lon, Instant.ofEpochMilli(fix.utcMs).toString()));
            if (fix.speed >= 0) sb.append(String.format(Locale.ROOT, "<extensions><speed>%.1f</speed></extensions>", fix.speed));
            sb.append("</trkpt>\n");
        }
        sb.append("</trkseg></trk>\n</gpx>\n");
        return sb.toString();
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
