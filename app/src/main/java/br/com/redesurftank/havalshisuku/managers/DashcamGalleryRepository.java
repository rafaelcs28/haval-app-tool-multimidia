package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger;

/**
 * Entry point of the Dashcam screen: state, gallery catalog (with the confirmed short pause) and
 * the cached catalog shown while the camera records. Every camera call runs on one background
 * thread and holds {@link DashcamHisiliconClient#CAMERA_LOCK}; callbacks arrive on the main thread.
 */
public final class DashcamGalleryRepository {
    private static final String TAG = "DashcamGallery";
    private static final String CACHE_FILE = "dashcam_catalog.json";
    private static final long LOCK_WAIT_SECONDS = 20L;
    static final String STATE_PREFS = "dashcam_state";
    static final String KEY_PAUSED_BY_CHARGING = "pausedByApp";
    static final String KEY_PAUSED_BY_GALLERY = "pausedByGallery";

    private static volatile DashcamGalleryRepository instance;

    public enum StateKind {
        WIFI_OFF,
        UNREACHABLE,
        RECORDING,
        EMERGENCY,
        PAUSED,
        PAUSED_BY_CHARGING,
        PAUSED_BY_GALLERY,
        OTHER_MODE,
        UNKNOWN
    }

    public static final class StateSnapshot {
        public final StateKind kind;
        public final String workMode;
        public final String error;
        public final long observedAtMs;
        /** A gallery pause whose resume was never confirmed: block new pauses, offer recovery. */
        public final boolean resumeUnconfirmed;
        /** Camera protocol on the current Wi-Fi. */
        public final DashcamProfile profile;
        /** Free bytes on the memory card when the camera reports it (Novatek 3017), else null. */
        public final Long freeBytes;

        StateSnapshot(StateKind kind, String workMode, String error, long observedAtMs,
                boolean resumeUnconfirmed) {
            this(kind, workMode, error, observedAtMs, resumeUnconfirmed, DashcamProfile.HISILICON, null);
        }

        StateSnapshot(StateKind kind, String workMode, String error, long observedAtMs,
                boolean resumeUnconfirmed, DashcamProfile profile, Long freeBytes) {
            this.kind = kind;
            this.workMode = workMode;
            this.error = error;
            this.observedAtMs = observedAtMs;
            this.resumeUnconfirmed = resumeUnconfirmed;
            this.profile = profile;
            this.freeBytes = freeBytes;
        }
    }

    public enum CatalogStatus {
        OK,
        PARTIAL,
        NEEDS_PAUSE,
        FAILED,
        BUSY
    }

    public static final class CatalogResult {
        public final CatalogStatus status;
        /** Catalog to show: the fresh one, or the cached one when the read failed. */
        public final DashcamCatalog catalog;
        public final long catalogFetchedAtMs;
        public final boolean catalogPartial;
        public final String error;
        public final boolean pausedForCatalog;
        public final Boolean resumeConfirmed;

        CatalogResult(CatalogStatus status, DashcamCatalog catalog, long catalogFetchedAtMs,
                boolean catalogPartial, String error, boolean pausedForCatalog,
                Boolean resumeConfirmed) {
            this.status = status;
            this.catalog = catalog;
            this.catalogFetchedAtMs = catalogFetchedAtMs;
            this.catalogPartial = catalogPartial;
            this.error = error;
            this.pausedForCatalog = pausedForCatalog;
            this.resumeConfirmed = resumeConfirmed;
        }
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    private final Context context;
    private final DashcamHisiliconClient client;
    private final DashcamNovatekClient novatek;
    private final SharedPreferences statePreferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "DashcamGallery");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler main = new Handler(Looper.getMainLooper());

    private DashcamGalleryRepository(Context context) {
        this.context = context.getApplicationContext();
        this.client = new DashcamHisiliconClient(this.context);
        this.novatek = new DashcamNovatekClient(this.context);
        this.statePreferences = App.getDeviceProtectedContext()
                .getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE);
    }

    public static DashcamGalleryRepository getInstance(Context context) {
        DashcamGalleryRepository local = instance;
        if (local == null) {
            synchronized (DashcamGalleryRepository.class) {
                local = instance;
                if (local == null) {
                    local = new DashcamGalleryRepository(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    public boolean isGalleryPauseUnconfirmed() {
        return statePreferences.getBoolean(KEY_PAUSED_BY_GALLERY, false);
    }

    /** Reads the state without changing the camera. */
    public void queryState(Callback<StateSnapshot> callback) {
        executor.execute(() -> deliver(callback, readState()));
    }

    StateSnapshot readState() {
        boolean unconfirmed = isGalleryPauseUnconfirmed();
        long now = System.currentTimeMillis();
        if (!client.hasWifiNetwork()) {
            return new StateSnapshot(StateKind.WIFI_OFF, null, "wifi_unavailable", now, unconfirmed);
        }
        if (DashcamProfile.detect(context) == DashcamProfile.NOVATEK) return readNovatekState(now, unconfirmed);
        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> result;
        if (!lock()) {
            return new StateSnapshot(StateKind.UNKNOWN, null, "camera_busy", now, unconfirmed);
        }
        try {
            result = client.readAnyWorkState();
        } finally {
            DashcamHisiliconClient.CAMERA_LOCK.unlock();
        }
        if (!result.isSuccess()) {
            StateKind kind = result.getError().startsWith("transport_")
                    ? StateKind.UNREACHABLE : StateKind.UNKNOWN;
            return new StateSnapshot(kind, null, result.getError(), now, unconfirmed);
        }
        DashcamHisiliconProtocol.WorkState state = result.getValue();
        return new StateSnapshot(classify(state), state.getWorkMode(), null, now, unconfirmed);
    }

    /**
     * Novatek: 3037 = mode (1 = movie), 2016 sampled twice (the counter moves while it records),
     * 3017 = free bytes. Read-only; nothing here changes the camera.
     */
    private StateSnapshot readNovatekState(long now, boolean unconfirmed) {
        if (!lock()) {
            return new StateSnapshot(StateKind.UNKNOWN, null, "camera_busy", now, unconfirmed,
                    DashcamProfile.NOVATEK, null);
        }
        DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> mode;
        DashcamHisiliconClient.Result<Boolean> recording = null;
        DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> free = null;
        try {
            mode = novatek.query(DashcamNovatekProtocol.CMD_MODE);
            if (mode.isSuccess()) {
                recording = novatek.isRecording();
                free = novatek.query(DashcamNovatekProtocol.CMD_FREE_SPACE);
            }
        } finally {
            DashcamHisiliconClient.CAMERA_LOCK.unlock();
        }
        if (!mode.isSuccess()) {
            StateKind kind = mode.getError().startsWith("transport_") ? StateKind.UNREACHABLE : StateKind.UNKNOWN;
            return new StateSnapshot(kind, null, mode.getError(), now, unconfirmed, DashcamProfile.NOVATEK, null);
        }
        Long freeBytes = free != null && free.isSuccess() && free.getValue().getStatus() == 0
                ? free.getValue().getLongValue() : null;
        Long modeValue = mode.getValue().getLongValue();
        StateKind kind;
        if (modeValue == null || mode.getValue().getStatus() != 0) {
            kind = StateKind.UNKNOWN;
        } else if (modeValue != DashcamNovatekProtocol.MODE_MOVIE) {
            kind = StateKind.OTHER_MODE;
        } else {
            if (recording == null || !recording.isSuccess()) {
                kind = StateKind.UNKNOWN;
            } else if (recording.getValue()) {
                kind = StateKind.RECORDING;
            } else {
                kind = statePreferences.getBoolean(KEY_PAUSED_BY_CHARGING, false)
                        ? StateKind.PAUSED_BY_CHARGING : StateKind.PAUSED;
            }
        }
        return new StateSnapshot(kind, modeValue == null ? null : "modo " + modeValue, null, now, unconfirmed,
                DashcamProfile.NOVATEK, freeBytes);
    }

    StateKind classify(DashcamHisiliconProtocol.WorkState state) {
        if (Boolean.TRUE.equals(state.getEmergencyRecording())) return StateKind.EMERGENCY;
        if (!"NORM_REC".equals(state.getWorkMode())) return StateKind.OTHER_MODE;
        if (Boolean.TRUE.equals(state.getRunning())) return StateKind.RECORDING;
        if (Boolean.FALSE.equals(state.getRunning())) {
            if (statePreferences.getBoolean(KEY_PAUSED_BY_GALLERY, false)) {
                return StateKind.PAUSED_BY_GALLERY;
            }
            if (statePreferences.getBoolean(KEY_PAUSED_BY_CHARGING, false)) {
                return StateKind.PAUSED_BY_CHARGING;
            }
            return StateKind.PAUSED;
        }
        return StateKind.UNKNOWN;
    }

    /** Last catalog saved on this device, without touching the camera. */
    public void loadCached(Callback<CatalogResult> callback) {
        executor.execute(() -> {
            Cached cached = readCache();
            deliver(callback, new CatalogResult(CatalogStatus.OK, cached.catalog, cached.fetchedAtMs,
                    cached.partial, null, false, null));
        });
    }

    /**
     * Reads the catalog. With {@code allowPause=false} a recording camera answers NEEDS_PAUSE and
     * nothing is sent; with true, the camera is paused, listed and resumed.
     */
    public void refreshCatalog(boolean allowPause, Callback<CatalogResult> callback) {
        executor.execute(() -> deliver(callback, refreshBlocking(allowPause)));
    }

    private CatalogResult refreshBlocking(boolean allowPause) {
        Cached cached = readCache();
        if (!client.hasWifiNetwork()) {
            return fromCache(cached, CatalogStatus.FAILED, "wifi_unavailable", false, null);
        }
        if (DashcamProfile.detect(context) == DashcamProfile.NOVATEK) return refreshNovatek(cached);
        if (!lock()) return fromCache(cached, CatalogStatus.BUSY, "camera_busy", false, null);
        DashcamCatalogSession.Outcome outcome;
        try {
            outcome = newSession().run(allowPause);
        } finally {
            DashcamHisiliconClient.CAMERA_LOCK.unlock();
        }
        log("catalog", "status=" + outcome.status
                + " files=" + outcome.paths.size()
                + " rejected=" + outcome.rejectedEntries
                + " paused=" + outcome.pausedForCatalog
                + " resumed=" + outcome.resumeConfirmed
                + (outcome.error == null ? "" : " error=" + outcome.error));
        switch (outcome.status) {
            case NEEDS_PAUSE:
                return fromCache(cached, CatalogStatus.NEEDS_PAUSE, null, false, null);
            case FAILED:
                return fromCache(cached, CatalogStatus.FAILED, outcome.error,
                        outcome.pausedForCatalog, outcome.resumeConfirmed);
            case PARTIAL:
            case OK:
            default:
                boolean partial = outcome.status == DashcamCatalogSession.Status.PARTIAL;
                long now = System.currentTimeMillis();
                writeCache(outcome.paths, now, partial);
                return new CatalogResult(
                        partial ? CatalogStatus.PARTIAL : CatalogStatus.OK,
                        DashcamCatalog.fromPaths(outcome.paths), now, partial, outcome.error,
                        outcome.pausedForCatalog, outcome.resumeConfirmed);
        }
    }

    /** Novatek lists while recording (observed 2026-11-21): no pause, no confirmation. */
    private CatalogResult refreshNovatek(Cached cached) {
        if (!lock()) return fromCache(cached, CatalogStatus.BUSY, "camera_busy", false, null);
        DashcamHisiliconClient.Result<List<DashcamNovatekProtocol.Entry>> list;
        try {
            list = novatek.fileList();
        } finally {
            DashcamHisiliconClient.CAMERA_LOCK.unlock();
        }
        if (!list.isSuccess()) {
            log("catalog", "profile=novatek status=FAILED error=" + list.getError());
            return fromCache(cached, CatalogStatus.FAILED, list.getError(), false, null);
        }
        List<DashcamNovatekProtocol.Entry> entries = list.getValue();
        long now = System.currentTimeMillis();
        writeNovatekCache(entries, now);
        log("catalog", "profile=novatek status=OK files=" + entries.size());
        return new CatalogResult(CatalogStatus.OK, DashcamCatalog.fromNovatek(entries), now, false, null,
                false, null);
    }

    /** Retries the resume of a gallery pause that was not confirmed. */
    public void resumeUnconfirmedPause(Callback<StateSnapshot> callback) {
        executor.execute(() -> {
            if (lock()) {
                try {
                    Boolean resumed = newSession().resumeOwnedPause();
                    log("gallery_resume_retry", "confirmed=" + resumed);
                } finally {
                    DashcamHisiliconClient.CAMERA_LOCK.unlock();
                }
            }
            deliver(callback, readState());
        });
    }

    DashcamCatalogSession newSession() {
        return new DashcamCatalogSession(
                client,
                new DashcamCatalogSession.PauseMark() {
                    @Override
                    public boolean isSet() {
                        return statePreferences.getBoolean(KEY_PAUSED_BY_GALLERY, false);
                    }

                    @Override
                    public boolean set(boolean owned) {
                        return statePreferences.edit()
                                .putBoolean(KEY_PAUSED_BY_GALLERY, owned).commit();
                    }
                },
                SystemClock::elapsedRealtime);
    }

    private boolean lock() {
        try {
            return DashcamHisiliconClient.CAMERA_LOCK.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private CatalogResult fromCache(Cached cached, CatalogStatus status, String error,
            boolean paused, Boolean resumed) {
        return new CatalogResult(status, cached.catalog, cached.fetchedAtMs, cached.partial, error,
                paused, resumed);
    }

    private static final class Cached {
        final DashcamCatalog catalog;
        final long fetchedAtMs;
        final boolean partial;

        Cached(DashcamCatalog catalog, long fetchedAtMs, boolean partial) {
            this.catalog = catalog;
            this.fetchedAtMs = fetchedAtMs;
            this.partial = partial;
        }
    }

    private File cacheFile() {
        return new File(context.getFilesDir(), CACHE_FILE);
    }

    private Cached readCache() {
        File file = cacheFile();
        if (!file.isFile() || file.length() > 8L * 1024 * 1024) {
            return new Cached(DashcamCatalog.fromPaths(Collections.<String>emptyList()), 0L, false);
        }
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(text);
            if ("novatek".equals(json.optString("profile"))) {
                JSONArray items = json.optJSONArray("entries");
                List<DashcamNovatekProtocol.Entry> entries = new ArrayList<>();
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        DashcamNovatekProtocol.Entry entry = DashcamNovatekProtocol.entry(
                                item.optString("dir", null), item.optString("name", null),
                                item.optString("size", null), item.optString("time", null),
                                item.optString("attr", null));
                        if (entry != null) entries.add(entry);
                    }
                }
                return new Cached(DashcamCatalog.fromNovatek(entries), json.optLong("fetchedAtMs", 0L), false);
            }
            JSONArray array = json.optJSONArray("paths");
            List<String> paths = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    String path = array.optString(i, null);
                    if (DashcamHisiliconProtocol.isValidCatalogPath(path)) paths.add(path);
                }
            }
            return new Cached(DashcamCatalog.fromPaths(paths), json.optLong("fetchedAtMs", 0L),
                    json.optBoolean("partial", false));
        } catch (Exception e) {
            Log.w(TAG, "catalog cache unreadable", e);
            return new Cached(DashcamCatalog.fromPaths(Collections.<String>emptyList()), 0L, false);
        }
    }

    private void writeNovatekCache(List<DashcamNovatekProtocol.Entry> entries, long fetchedAtMs) {
        try {
            JSONArray items = new JSONArray();
            for (DashcamNovatekProtocol.Entry entry : entries) {
                JSONObject item = new JSONObject();
                item.put("dir", entry.dir);
                item.put("name", entry.name);
                item.put("size", String.valueOf(entry.size));
                if (entry.timeText != null) item.put("time", entry.timeText);
                item.put("attr", String.valueOf(entry.attributes));
                items.put(item);
            }
            JSONObject json = new JSONObject();
            json.put("profile", "novatek");
            json.put("fetchedAtMs", fetchedAtMs);
            json.put("entries", items);
            writeCacheJson(json);
        } catch (Exception e) {
            Log.w(TAG, "catalog cache write failed", e);
        }
    }

    private void writeCache(List<String> paths, long fetchedAtMs, boolean partial) {
        try {
            JSONObject json = new JSONObject();
            json.put("fetchedAtMs", fetchedAtMs);
            json.put("partial", partial);
            json.put("paths", new JSONArray(paths));
            writeCacheJson(json);
        } catch (Exception e) {
            Log.w(TAG, "catalog cache write failed", e);
        }
    }

    private void writeCacheJson(JSONObject json) throws Exception {
        File target = cacheFile();
        File temp = new File(target.getParentFile(), CACHE_FILE + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(json.toString().getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        if (!temp.renameTo(target)) Log.w(TAG, "catalog cache rename failed");
    }

    private <T> void deliver(Callback<T> callback, T result) {
        if (callback != null) main.post(() -> callback.onResult(result));
    }

    private static void log(String phase, String detail) {
        // Sempre no logcat: o log persistente só grava com a captura de diagnóstico ligada.
        Log.i(TAG, "phase=" + phase + " " + detail);
        ClusterPersistentEventLogger.logText("dashcam_gallery", "phase=" + phase + " " + detail);
    }
}
