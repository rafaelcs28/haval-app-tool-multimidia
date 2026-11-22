package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.models.CarConstants;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;

/**
 * Publishes the dashcam state on the public telemetry bus ({@code com.haval.vehicle.EVENT_CHANGED})
 * so the Haval 3D card shows it like any other vehicle value. Keys (all strings):
 *
 * <ul>
 *   <li>{@code app.dashcam.state}: recording | paused | paused_auto | emergency | other_mode |
 *       unreachable | no_wifi | unknown
 *   <li>{@code app.dashcam.pause_reason}: charging | ready | "" (with paused_auto)
 *   <li>{@code app.dashcam.profile}: novatek | hisilicon | none
 *   <li>{@code app.dashcam.free_mb}: last known free space on the camera card, or ""
 *   <li>{@code app.dashcam.saved_count}: clips saved on the head unit
 *   <li>{@code app.dashcam.saving}: "" | "NN" percent | "converting"
 *   <li>{@code app.dashcam.checked_at}: epoch ms of the last successful camera read ("0" never)
 * </ul>
 *
 * The camera is only reachable while the head unit is on its Wi-Fi; otherwise the state is
 * {@code no_wifi} and the last known free space and time stay published. Reads are read-only and
 * share {@link DashcamHisiliconClient#CAMERA_LOCK} through the gallery repository.
 */
public final class DashcamStatusPublisher {
    private static final String TAG = "DashcamStatus";
    private static final long CAMERA_POLL_MS = 30_000L;
    private static final long LOCAL_POLL_MS = 3_000L;
    private static final String KEY_LAST_FREE_MB = "status_last_free_mb";
    private static final String KEY_LAST_CHECKED_AT = "status_last_checked_at";

    public static final String STATE = "app.dashcam.state";
    public static final String PAUSE_REASON = "app.dashcam.pause_reason";
    public static final String PROFILE = "app.dashcam.profile";
    public static final String FREE_MB = "app.dashcam.free_mb";
    public static final String SAVED_COUNT = "app.dashcam.saved_count";
    public static final String SAVING = "app.dashcam.saving";
    public static final String CHECKED_AT = "app.dashcam.checked_at";

    private static volatile DashcamStatusPublisher instance;

    private final Context context;
    private final ServiceManager serviceManager;
    private final SharedPreferences statePreferences;
    private final SharedPreferences preferences;
    private final Map<String, String> published = new LinkedHashMap<>();
    private HandlerThread thread;
    private Handler handler;

    private final Runnable cameraPoll = new Runnable() {
        @Override
        public void run() {
            try {
                pollCamera();
            } catch (RuntimeException e) {
                Log.w(TAG, "camera poll failed", e);
            }
            Handler h = handler;
            if (h != null) h.postDelayed(this, CAMERA_POLL_MS);
        }
    };

    private final Runnable localPoll = new Runnable() {
        @Override
        public void run() {
            try {
                pollLocal();
            } catch (RuntimeException e) {
                Log.w(TAG, "local poll failed", e);
            }
            Handler h = handler;
            if (h != null) h.postDelayed(this, LOCAL_POLL_MS);
        }
    };

    private DashcamStatusPublisher(Context context, ServiceManager serviceManager) {
        this.context = context.getApplicationContext();
        this.serviceManager = serviceManager;
        this.statePreferences = App.getDeviceProtectedContext()
                .getSharedPreferences(DashcamGalleryRepository.STATE_PREFS, Context.MODE_PRIVATE);
        this.preferences = App.getDeviceProtectedContext()
                .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
    }

    public static synchronized DashcamStatusPublisher start(Context context, ServiceManager serviceManager) {
        if (instance == null) instance = new DashcamStatusPublisher(context, serviceManager);
        instance.startInternal();
        return instance;
    }

    public static synchronized void stop() {
        if (instance != null) instance.stopInternal();
    }

    /** Re-sends every key (the 3D viewer asks for a snapshot when it opens). */
    public static void dispatchSnapshot() {
        DashcamStatusPublisher local = instance;
        if (local == null) return;
        Map<String, String> copy;
        synchronized (local.published) {
            copy = new LinkedHashMap<>(local.published);
        }
        for (Map.Entry<String, String> entry : copy.entrySet()) {
            local.serviceManager.dispatchTelemetryOnly(entry.getKey(), entry.getValue());
        }
    }

    private synchronized void startInternal() {
        if (thread != null) return;
        thread = new HandlerThread("DashcamStatus");
        thread.start();
        handler = new Handler(thread.getLooper());
        // Last known values first, so the card is not empty until the first camera read.
        publish(FREE_MB, statePreferences.getString(KEY_LAST_FREE_MB, ""));
        publish(CHECKED_AT, String.valueOf(statePreferences.getLong(KEY_LAST_CHECKED_AT, 0L)));
        handler.post(localPoll);
        handler.postDelayed(cameraPoll, 5_000L);
    }

    private synchronized void stopInternal() {
        Handler h = handler;
        handler = null;
        if (h != null) h.removeCallbacksAndMessages(null);
        if (thread != null) thread.quitSafely();
        thread = null;
    }

    private void pollLocal() {
        DashcamClipStore store = DashcamClipStore.getInstance(context);
        publish(SAVED_COUNT, String.valueOf(store.list().size()));
        publish(SAVING, DashcamClipStore.savingStatus());
    }

    private void pollCamera() {
        DashcamProfile profile = DashcamProfile.detect(context);
        publish(PROFILE, profile == DashcamProfile.NOVATEK ? "novatek"
                : profile == DashcamProfile.HISILICON ? "hisilicon" : "none");
        DashcamGalleryRepository.StateSnapshot snapshot =
                DashcamGalleryRepository.getInstance(context).readState();
        String state = stateValue(snapshot, profile);
        publish(STATE, state);
        publish(PAUSE_REASON, "paused_auto".equals(state) ? pauseReason() : "");
        boolean read = !state.equals("no_wifi") && !state.equals("unreachable") && !state.equals("unknown");
        if (read) {
            long now = System.currentTimeMillis();
            String free = snapshot.freeBytes != null ? String.valueOf(snapshot.freeBytes / 1_000_000L) : "";
            statePreferences.edit()
                    .putLong(KEY_LAST_CHECKED_AT, now)
                    .putString(KEY_LAST_FREE_MB, free)
                    .apply();
            publish(CHECKED_AT, String.valueOf(now));
            publish(FREE_MB, free);
        }
    }

    static String stateValue(DashcamGalleryRepository.StateSnapshot snapshot, DashcamProfile profile) {
        if (snapshot == null || snapshot.kind == null) return "unknown";
        switch (snapshot.kind) {
            case WIFI_OFF:
                return "no_wifi";
            case UNREACHABLE:
                // On a 192.168.1.x Wi-Fi the camera should answer; elsewhere the head unit is most
                // likely just on another network (home, hotspot, Starlink).
                return profile == DashcamProfile.NOVATEK ? "unreachable" : "no_wifi";
            case RECORDING:
                return "recording";
            case EMERGENCY:
                return "emergency";
            case PAUSED:
            case PAUSED_BY_GALLERY:
                return "paused";
            case PAUSED_BY_CHARGING:
                return "paused_auto";
            case OTHER_MODE:
                return "other_mode";
            case UNKNOWN:
            default:
                // HiSilicon's strict parser fails on any non-camera Wi-Fi (e.g. a home router).
                return profile == DashcamProfile.HISILICON
                        && "untrusted_status".equals(snapshot.error) ? "no_wifi" : "unknown";
        }
    }

    /** Which automation rule is holding the pause right now. */
    private String pauseReason() {
        try {
            boolean chargingRule = preferences.getBoolean(
                    SharedPreferencesKeys.ENABLE_DASHCAM_PAUSE_WHILE_CHARGING.getKey(), false);
            String charging = serviceManager.getUpdatedData(CarConstants.CAR_EV_INFO_CHARGING_STATE.getValue());
            String speed = serviceManager.getUpdatedData(CarConstants.CAR_BASIC_VEHICLE_SPEED.getValue());
            if (DashcamChargingPausePolicy.decide(chargingRule, charging, speed)
                    == DashcamChargingPausePolicy.Action.PAUSE) {
                return "charging";
            }
            boolean readyRule = preferences.getBoolean(
                    SharedPreferencesKeys.ENABLE_DASHCAM_RECORD_ONLY_WHEN_READY.getKey(), false);
            String ready = serviceManager.getUpdatedData(CarConstants.CAR_BASIC_DRIVING_READY_STATE.getValue());
            if (DashcamRecordingPolicy.decideReady(readyRule, ready) == DashcamChargingPausePolicy.Action.PAUSE) {
                return "ready";
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "pause reason unavailable", e);
        }
        return "";
    }

    /** Sends a key only when its value changed. */
    private void publish(String key, String value) {
        String v = value == null ? "" : value;
        synchronized (published) {
            if (v.equals(published.get(key))) return;
            published.put(key, v);
        }
        try {
            serviceManager.dispatchTelemetryOnly(key, v);
        } catch (RuntimeException e) {
            Log.w(TAG, "dispatch " + key + " failed", e);
        }
    }
}
