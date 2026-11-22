package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger;
import br.com.redesurftank.havalshisuku.listeners.IDataChanged;
import br.com.redesurftank.havalshisuku.models.CarConstants;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;

/**
 * Pauses the dashcam by the optional rules of {@link DashcamRecordingPolicy} (charging and stopped;
 * record only while READY) and resumes only a pause it made itself. Works on the HiSilicon profile
 * (validated) and on the Novatek profile ({@code 2001}, verified by the moving {@code 2016} counter).
 */
public final class DashcamChargingPauseManager
        implements IDataChanged, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "DashcamChargePause";
    private static final long EVALUATION_DELAY_MS = 250L;
    private static final long RETRY_DELAY_MS = 30_000L;
    private static final String SPEED_KEY = CarConstants.CAR_BASIC_VEHICLE_SPEED.getValue();
    private static final String CHARGING_KEY = CarConstants.CAR_EV_INFO_CHARGING_STATE.getValue();
    private static final String READY_KEY = CarConstants.CAR_BASIC_DRIVING_READY_STATE.getValue();

    private final ServiceManager serviceManager;
    private final Context context;
    private final SharedPreferences preferences;
    private final SharedPreferences statePreferences;
    private final DashcamHisiliconClient client;
    private final DashcamNovatekClient novatek;
    private final Object lifecycleLock = new Object();

    private HandlerThread workerThread;
    private Handler worker;
    private boolean started;
    private String speedRaw;
    private String chargingRaw;
    private String readyRaw;

    private final Runnable evaluation = this::evaluate;
    private final Runnable retry;

    private void refreshAndEvaluate() {
        speedRaw = serviceManager.getUpdatedData(SPEED_KEY);
        chargingRaw = serviceManager.getUpdatedData(CHARGING_KEY);
        readyRaw = serviceManager.getUpdatedData(READY_KEY);
        evaluate();
    }

    public DashcamChargingPauseManager(Context context, ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.context = context.getApplicationContext();
        this.retry = this::refreshAndEvaluate;
        this.preferences =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
        this.statePreferences =
                App.getDeviceProtectedContext()
                        .getSharedPreferences("dashcam_state", Context.MODE_PRIVATE);
        this.client = new DashcamHisiliconClient(context);
        this.novatek = new DashcamNovatekClient(context);
    }

    public void start() {
        synchronized (lifecycleLock) {
            if (started) return;
            workerThread = new HandlerThread("DashcamChargingPause");
            workerThread.start();
            worker = new Handler(workerThread.getLooper());
            started = true;
        }
        serviceManager.addDataChangedListener(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        worker.post(retry);
    }

    public void stop() {
        HandlerThread thread;
        synchronized (lifecycleLock) {
            if (!started) return;
            started = false;
            thread = workerThread;
        }
        serviceManager.removeDataChangedListener(this);
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        if (worker != null) worker.removeCallbacksAndMessages(null);
        if (thread != null) thread.quitSafely();
        worker = null;
        workerThread = null;
    }

    @Override
    public void onDataChanged(String key, String value) {
        Handler handler = worker;
        if (handler == null) return;
        if (SPEED_KEY.equals(key)) {
            handler.post(() -> {
                speedRaw = value;
                scheduleEvaluation();
            });
        } else if (CHARGING_KEY.equals(key)) {
            handler.post(() -> {
                chargingRaw = value;
                scheduleEvaluation();
            });
        } else if (READY_KEY.equals(key)) {
            handler.post(() -> {
                readyRaw = value;
                scheduleEvaluation();
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!SharedPreferencesKeys.ENABLE_DASHCAM_PAUSE_WHILE_CHARGING.getKey().equals(key)
                && !SharedPreferencesKeys.ENABLE_DASHCAM_RECORD_ONLY_WHEN_READY.getKey().equals(key)) {
            return;
        }
        Handler handler = worker;
        if (handler != null) handler.post(this::scheduleEvaluation);
    }

    private void scheduleEvaluation() {
        Handler handler = worker;
        if (handler == null) return;
        handler.removeCallbacks(evaluation);
        handler.removeCallbacks(retry);
        handler.postDelayed(evaluation, EVALUATION_DELAY_MS);
    }

    private void scheduleRetry() {
        Handler handler = worker;
        if (handler == null) return;
        handler.removeCallbacks(retry);
        handler.postDelayed(retry, RETRY_DELAY_MS);
    }

    private void evaluate() {
        DashcamChargingPausePolicy.Action action = DashcamRecordingPolicy.decide(
                preferences.getBoolean(SharedPreferencesKeys.ENABLE_DASHCAM_PAUSE_WHILE_CHARGING.getKey(), true),
                chargingRaw, speedRaw,
                preferences.getBoolean(SharedPreferencesKeys.ENABLE_DASHCAM_RECORD_ONLY_WHEN_READY.getKey(), true),
                readyRaw);
        if (action == DashcamChargingPausePolicy.Action.HOLD) {
            scheduleRetry();
            return;
        }
        boolean novatekProfile = DashcamProfile.detect(context) == DashcamProfile.NOVATEK;
        boolean completed;
        // Same lock as the gallery catalog and the automation: a pause/resume sequence never
        // interleaves with another camera writer in this process.
        DashcamHisiliconClient.CAMERA_LOCK.lock();
        try {
            switch (action) {
                case PAUSE:
                    completed = novatekProfile ? pauseNovatek() : pauseIfNeeded();
                    break;
                case RESUME:
                    completed = novatekProfile ? resumeNovatek() : resumeIfOwned();
                    break;
                case HOLD:
                default:
                    completed = false;
                    break;
            }
        } finally {
            DashcamHisiliconClient.CAMERA_LOCK.unlock();
        }
        if (!completed) scheduleRetry();
    }

    private boolean pauseIfNeeded() {
        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> result =
                client.getWorkState();
        if (!result.isSuccess()) return failed("pause_status", result.getError());

        DashcamHisiliconProtocol.WorkState state = result.getValue();
        boolean owned = isPauseOwned();
        if (Boolean.FALSE.equals(state.getRunning())) {
            log(owned ? "paused" : "manual_pause_preserved", "status_confirmed");
            return true;
        }
        if (!state.canPause()) {
            return failed("pause_blocked", "emergency_or_unknown");
        }

        if (!owned && !setPauseOwned(true)) {
            return failed("pause_ownership", "persist_failed");
        }
        DashcamHisiliconClient.Result<Boolean> command = client.setRecording(false);
        if (!command.isSuccess()) return failed("pause_command", command.getError());

        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> verified =
                client.getWorkState();
        if (!verified.isSuccess()) return failed("pause_verify", verified.getError());
        if (!Boolean.FALSE.equals(verified.getValue().getRunning())) {
            return failed("pause_verify", "still_running");
        }
        log("paused", "verified");
        return true;
    }

    private boolean resumeIfOwned() {
        if (!isPauseOwned()) return true;

        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> result =
                client.getWorkState();
        if (!result.isSuccess()) return failed("resume_status", result.getError());
        DashcamHisiliconProtocol.WorkState state = result.getValue();
        if (Boolean.TRUE.equals(state.getRunning())) {
            if (!setPauseOwned(false)) return failed("resume_ownership", "persist_failed");
            log("resumed", "already_running");
            return true;
        }
        if (!Boolean.FALSE.equals(state.getEmergencyRecording())) {
            return failed("resume_blocked", "emergency_or_unknown");
        }

        DashcamHisiliconClient.Result<Boolean> command = client.setRecording(true);
        if (!command.isSuccess()) return failed("resume_command", command.getError());
        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> verified =
                client.getWorkState();
        if (!verified.isSuccess()) return failed("resume_verify", verified.getError());
        if (!Boolean.TRUE.equals(verified.getValue().getRunning())) {
            return failed("resume_verify", "still_paused");
        }
        if (!setPauseOwned(false)) return failed("resume_ownership", "persist_failed");
        log("resumed", "verified");
        return true;
    }

    /** Novatek: no emergency flag exists; a stop found already in place is preserved as manual. */
    private boolean pauseNovatek() {
        DashcamHisiliconClient.Result<Boolean> recording = novatek.isRecording();
        if (!recording.isSuccess()) return failed("novatek_pause_status", recording.getError());
        boolean owned = isPauseOwned();
        if (!recording.getValue()) {
            log(owned ? "paused" : "manual_pause_preserved", "novatek_status_confirmed");
            return true;
        }
        if (!owned && !setPauseOwned(true)) return failed("novatek_pause_ownership", "persist_failed");
        DashcamHisiliconClient.Result<Boolean> command = novatek.setRecording(false);
        if (!command.isSuccess()) return failed("novatek_pause_command", command.getError());
        DashcamHisiliconClient.Result<Boolean> verified = novatek.isRecording();
        if (!verified.isSuccess()) return failed("novatek_pause_verify", verified.getError());
        if (verified.getValue()) return failed("novatek_pause_verify", "still_recording");
        log("paused", "novatek_verified");
        return true;
    }

    private boolean resumeNovatek() {
        if (!isPauseOwned()) return true;
        DashcamHisiliconClient.Result<Boolean> recording = novatek.isRecording();
        if (!recording.isSuccess()) return failed("novatek_resume_status", recording.getError());
        if (!recording.getValue()) {
            DashcamHisiliconClient.Result<Boolean> command = novatek.setRecording(true);
            if (!command.isSuccess()) return failed("novatek_resume_command", command.getError());
            DashcamHisiliconClient.Result<Boolean> verified = novatek.isRecording();
            if (!verified.isSuccess()) return failed("novatek_resume_verify", verified.getError());
            if (!verified.getValue()) return failed("novatek_resume_verify", "still_stopped");
        }
        if (!setPauseOwned(false)) return failed("novatek_resume_ownership", "persist_failed");
        log("resumed", "novatek_verified");
        return true;
    }

    private boolean isPauseOwned() {
        return statePreferences.getBoolean("pausedByApp", false);
    }

    private boolean setPauseOwned(boolean owned) {
        return statePreferences.edit().putBoolean("pausedByApp", owned).commit();
    }

    private boolean failed(String phase, String detail) {
        Log.w(TAG, phase + " failed: " + detail);
        log(phase, detail);
        return false;
    }

    private void log(String phase, String detail) {
        Log.i(TAG, "phase=" + phase + " detail=" + detail);
        ClusterPersistentEventLogger.logText(
                "dashcam_charging_pause",
                "phase=" + phase + " detail=" + detail);
    }
}
