package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.models.CarConstants;

/**
 * Car signals once per second, for the dashcam stamp (speed, gear, seat belt, indicators, lights,
 * steering, drive mode, battery, power, temperatures). Raw values as the car reports them; the
 * stamp interprets them. Same rules as {@link DashcamGpsTrack}: hourly files under the app's
 * private storage, 72 h, recorded only while "Localizar os vídeos no mapa" is on, nothing uploaded.
 * Each file starts with a {@code #keys} header, so adding a signal later never breaks old files.
 */
public final class DashcamCarTrack {
    private static final String TAG = "DashcamCar";
    static final String DIR = "dashcam-car";
    private static final long INTERVAL_MS = 1_000L;

    /** Short column name -> car property. Order is the file's column order. */
    static final String[][] SIGNALS = {
            {"speed", CarConstants.CAR_BASIC_VEHICLE_SPEED.getValue()},
            {"gear", CarConstants.CAR_BASIC_GEAR_STATUS.getValue()},
            {"belt", CarConstants.CAR_BASIC_SEAT_BELT_WARNING.getValue()},
            {"left", CarConstants.CAR_BASIC_LEFT_TURN_LIGHT_STATUS.getValue()},
            {"right", CarConstants.CAR_BASIC_RIGHT_TURN_LIGHT_STATUS.getValue()},
            {"hazard", CarConstants.CAR_BASIC_HAZARD_LIGHT_STATUS.getValue()},
            {"low", CarConstants.CAR_BASIC_LOW_BEAM_LIGHT_STATUS.getValue()},
            {"high", CarConstants.CAR_BASIC_HIGH_BEAM_LIGHT_STATUS.getValue()},
            {"handbrake", CarConstants.CAR_BASIC_HAND_BRAKE_STATUS.getValue()},
            {"steer", CarConstants.CAR_BASIC_STEERING_WHEEL_ANGLE.getValue()},
            {"mode", CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE.getValue()},
            {"soc", CarConstants.CAR_EV_INFO_CUR_BATTERY_POWER_PERCENTAGE.getValue()},
            {"power", CarConstants.CAR_EV_INFO_MOTOR_POWER.getValue()},
            {"tin", CarConstants.CAR_BASIC_INSIDE_TEMP.getValue()},
            {"tout", CarConstants.CAR_BASIC_OUTSIDE_TEMP.getValue()},
            {"odo", CarConstants.CAR_BASIC_TOTAL_ODOMETER.getValue()},
    };

    private static volatile DashcamCarTrack instance;

    private final Context context;
    private final SharedPreferences preferences;
    private HandlerThread thread;
    private Handler handler;
    private BufferedWriter writer;
    private String writerFile;
    private int pending;
    private long lastPruneMs;

    public static final class Sample {
        public final long utcMs;
        public final Map<String, String> values;

        Sample(long utcMs, Map<String, String> values) {
            this.utcMs = utcMs;
            this.values = values;
        }

        public String get(String name) {
            String v = values.get(name);
            return v == null ? "" : v;
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                sample();
            } catch (RuntimeException e) {
                Log.w(TAG, "sample failed", e);
            }
            Handler h = handler;
            if (h != null) h.postDelayed(this, INTERVAL_MS);
        }
    };

    private DashcamCarTrack(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
    }

    public static DashcamCarTrack getInstance(Context context) {
        DashcamCarTrack local = instance;
        if (local == null) {
            synchronized (DashcamCarTrack.class) {
                local = instance;
                if (local == null) {
                    local = new DashcamCarTrack(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    public synchronized void start() {
        if (thread != null) return;
        thread = new HandlerThread("DashcamCar");
        thread.start();
        handler = new Handler(thread.getLooper());
        handler.post(tick);
    }

    public synchronized void stop() {
        Handler h = handler;
        handler = null;
        if (h != null) h.removeCallbacksAndMessages(null);
        closeWriter();
        if (thread != null) thread.quitSafely();
        thread = null;
    }

    private synchronized void sample() {
        if (!DashcamGpsTrack.isEnabled(preferences)) {
            closeWriter();
            return;
        }
        ServiceManager sm = ServiceManager.getInstance();
        if (sm == null) return;
        StringBuilder line = new StringBuilder();
        long now = System.currentTimeMillis();
        line.append(now);
        boolean any = false;
        for (String[] signal : SIGNALS) {
            String value;
            try {
                value = sm.getUpdatedData(signal[1]);
            } catch (RuntimeException e) {
                value = null;
            }
            String clean = clean(value);
            if (!clean.isEmpty()) any = true;
            line.append('|').append(clean);
        }
        if (!any) return;
        String name = DashcamGpsTrack.fileName(now);
        try {
            if (!name.equals(writerFile)) {
                closeWriter();
                File file = new File(directory(), name);
                boolean fresh = !file.exists() || file.length() == 0;
                writer = new BufferedWriter(new FileWriter(file, true));
                writerFile = name;
                if (fresh) {
                    writer.write(header());
                    writer.write('\n');
                }
            }
            writer.write(line.toString());
            writer.write('\n');
            if (++pending >= 10) flushWriter();
        } catch (IOException e) {
            Log.w(TAG, "write failed", e);
            closeWriter();
        }
        if (now - lastPruneMs > 60L * 60 * 1000) {
            lastPruneMs = now;
            prune(now);
        }
    }

    /** Samples with {@code fromUtc <= time <= toUtc}, oldest first. Safe from any thread. */
    public List<Sample> query(long fromUtc, long toUtc) {
        synchronized (this) {
            flushWriter();
        }
        List<Sample> out = new ArrayList<>();
        for (String name : DashcamGpsTrack.fileNamesBetween(fromUtc, toUtc)) {
            File file = new File(directory(), name);
            if (!file.isFile()) continue;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String[] columns = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#keys ")) {
                        columns = line.substring(6).split(",");
                        continue;
                    }
                    Sample s = parseLine(line, columns);
                    if (s != null && s.utcMs >= fromUtc && s.utcMs <= toUtc) out.add(s);
                }
            } catch (IOException e) {
                Log.w(TAG, "read " + name + " failed", e);
            }
        }
        Collections.sort(out, (a, b) -> Long.compare(a.utcMs, b.utcMs));
        return out;
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
            Long hour = DashcamGpsTrack.hourOf(file.getName());
            if (hour != null && hour + 60L * 60 * 1000 < nowUtc - DashcamGpsTrack.RETENTION_MS && !file.delete()) {
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

    static String header() {
        StringBuilder sb = new StringBuilder("#keys ");
        for (int i = 0; i < SIGNALS.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(SIGNALS[i][0]);
        }
        return sb.toString();
    }

    static String clean(String value) {
        if (value == null) return "";
        String v = value.trim().replace('|', '/').replace('\n', ' ').replace('\r', ' ');
        return v.length() > 24 ? v.substring(0, 24) : v;
    }

    static Sample parseLine(String line, String[] columns) {
        if (line == null || line.isEmpty() || columns == null) return null;
        String[] p = line.split("\\|", -1);
        if (p.length < 2) return null;
        long utc;
        try {
            utc = Long.parseLong(p[0]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (utc <= 0) return null;
        Map<String, String> values = new HashMap<>();
        for (int i = 1; i < p.length && i - 1 < columns.length; i++) values.put(columns[i - 1], p[i]);
        return new Sample(utc, values);
    }

    /** The sample at or just before {@code utcMs} (within 3 s), or null. */
    public static Sample at(List<Sample> samples, long utcMs) {
        Sample best = null;
        for (Sample s : samples) {
            if (s.utcMs > utcMs) break;
            best = s;
        }
        return best != null && utcMs - best.utcMs <= 3_000L ? best : null;
    }
}
