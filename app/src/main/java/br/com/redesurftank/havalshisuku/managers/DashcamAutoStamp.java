package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;

/**
 * Background stamping (owner, 2026-11-25): every clip saved from the dashcam is queued, and when
 * the head unit has internet (map tiles, address, road limit) Impulse makes {@code <clip>_carimbo.mp4}
 * next to it, ready to send to the phone. The queue is kept in preferences, so a clip saved just
 * before the car is switched off is stamped on the next drive. Without internet for
 * {@link #OFFLINE_GIVE_UP_MS} it stamps anyway with the cached map and no address.
 */
public final class DashcamAutoStamp {
    private static final String TAG = "DashcamAutoStamp";
    private static final String KEY_QUEUE = "auto_stamp_queue";
    private static final String KEY_WAITING_SINCE = "auto_stamp_waiting_since_ms";
    static final long RETRY_MS = 60_000L;
    static final long OFFLINE_GIVE_UP_MS = 60L * 60 * 1000;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, String> STATUS = new ConcurrentHashMap<>();
    private static volatile boolean busy;
    private static volatile Context appContext;

    private DashcamAutoStamp() {}

    public static boolean enabled() {
        return App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
                .getBoolean(SharedPreferencesKeys.ENABLE_DASHCAM_AUTO_STAMP.getKey(), true);
    }

    /** Text for the saved-clip row ("carimbo na fila", "carimbando 40%"…), or null. */
    public static String statusFor(String clipName) {
        return STATUS.get(clipName);
    }

    public static boolean isActive() {
        return busy || !queue().isEmpty();
    }

    /** A clip was saved: queue it (the .mp4 the gallery shows). */
    public static void enqueue(Context context, File clip) {
        if (clip == null || !enabled() || DashcamStampJob.isStamped(clip.getName())
                || !clip.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".mp4")) return;
        appContext = context.getApplicationContext();
        Set<String> q = new HashSet<>(queue());
        q.add(clip.getAbsolutePath());
        state().edit().putStringSet(KEY_QUEUE, q).apply();
        STATUS.put(clip.getName(), "carimbo na fila");
        log("queued", clip.getName());
        MAIN.post(DashcamAutoStamp::kick);
    }

    /** App start: continue a queue left by a previous drive. */
    public static void resume(Context context) {
        appContext = context.getApplicationContext();
        for (String path : queue()) STATUS.put(new File(path).getName(), "carimbo na fila");
        MAIN.postDelayed(DashcamAutoStamp::kick, 30_000L);
    }

    private static void kick() {
        Context context = appContext;
        if (context == null || busy) return;
        Set<String> q = queue();
        if (q.isEmpty()) return;
        if (!DashcamStampJob.running().isEmpty()) { // a manual stamp is running
            MAIN.postDelayed(DashcamAutoStamp::kick, RETRY_MS);
            return;
        }
        String path = q.iterator().next();
        File clip = new File(path);
        File stamped = new File(clip.getParentFile(), DashcamStampJob.stampedName(clip.getName()));
        if (!clip.isFile() || stamped.isFile()) {
            remove(path);
            STATUS.remove(clip.getName());
            MAIN.post(DashcamAutoStamp::kick);
            return;
        }
        long now = System.currentTimeMillis();
        if (!hasInternet(context)) {
            long since = state().getLong(KEY_WAITING_SINCE, 0L);
            if (since <= 0) state().edit().putLong(KEY_WAITING_SINCE, now).apply();
            if (since <= 0 || now - since < OFFLINE_GIVE_UP_MS) {
                STATUS.put(clip.getName(), "carimbo na fila · esperando internet");
                MAIN.postDelayed(DashcamAutoStamp::kick, RETRY_MS);
                return;
            }
            log("offline_stamp", clip.getName());
        }
        state().edit().remove(KEY_WAITING_SINCE).apply();
        busy = true;
        STATUS.put(clip.getName(), "carimbando…");
        log("start", clip.getName());
        DashcamStampJob.start(context, clip, new DashcamStampJob.Listener() {
            @Override public void onProgress(String name, float fraction) {
                STATUS.put(name, "carimbando " + (int) (fraction * 100) + "%");
            }

            @Override public void onDone(String name, File output) {
                busy = false;
                remove(path);
                STATUS.put(name, "carimbo pronto: " + output.getName());
                log("done", name);
                MAIN.post(DashcamAutoStamp::kick);
            }

            @Override public void onFailed(String name, String error) {
                busy = false;
                log("failed", name + " " + error);
                if ("cancelled".equals(error)) {
                    STATUS.put(name, "carimbo na fila");
                    MAIN.postDelayed(DashcamAutoStamp::kick, RETRY_MS);
                    return;
                }
                remove(path);
                STATUS.put(name, "carimbo falhou: " + ("sem_dados".equals(error)
                        ? "sem GPS e dados do carro para este horário"
                        : ("no_space".equals(error) ? "sem espaço na central" : error)));
                MAIN.post(DashcamAutoStamp::kick);
            }
        });
    }

    private static boolean hasInternet(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            for (Network network : cm.getAllNetworks()) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return true;
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "connectivity check failed", e);
        }
        return false;
    }

    private static Set<String> queue() {
        return new HashSet<>(state().getStringSet(KEY_QUEUE, new HashSet<>()));
    }

    private static void remove(String path) {
        Set<String> q = queue();
        q.remove(path);
        state().edit().putStringSet(KEY_QUEUE, q).apply();
    }

    private static SharedPreferences state() {
        return App.getDeviceProtectedContext()
                .getSharedPreferences(DashcamGalleryRepository.STATE_PREFS, Context.MODE_PRIVATE);
    }

    private static void log(String phase, String detail) {
        Log.i(TAG, phase + " " + detail);
        ClusterPersistentEventLogger.logText("dashcam_auto_stamp", "phase=" + phase + " " + detail);
    }
}
