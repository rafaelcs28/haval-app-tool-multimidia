package br.com.redesurftank.havalshisuku.managers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger;
import br.com.redesurftank.havalshisuku.models.CarConstants;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;

/**
 * Dashcam automation from the Dashcam screen: periodic check/resume, camera clock, the
 * "not recording" warning and the optional automatic move to the station network (off by
 * default). It also recovers a gallery pause whose resume was lost (process killed between stop
 * and start), even with every switch off.
 */
public final class DashcamAutomationManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "DashcamAutomation";
    static final long CHECK_INTERVAL_MS = 5L * 60 * 1000;
    private static final long FIRST_CHECK_DELAY_MS = 30_000L;
    private static final long PREF_CHANGE_DELAY_MS = 2_000L;
    private static final long CLOCK_SYNC_INTERVAL_MS = 6L * 60 * 60 * 1000;
    private static final long LOCK_WAIT_SECONDS = 20L;
    private static final String CHANNEL_ID = "dashcam_alerts";
    private static final int NOTIFICATION_ID = 0x0DA5C;
    private static final DateTimeFormatter CAMERA_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Context context;
    private final ServiceManager serviceManager;
    private final SharedPreferences preferences;
    private final SharedPreferences statePreferences;
    private final DashcamHisiliconClient client;
    private final DashcamNovatekClient novatek;
    /** Epoch ms (trusted UTC) of the last accepted camera clock set, either profile; 0 never. */
    public static final String KEY_CLOCK_SYNCED_AT = "clock_synced_at";
    private final DashcamTimeSource timeSource;
    private final Object lifecycleLock = new Object();
    private final Runnable tick = this::tick;
    /** Automatic move to the station network: its own 1-minute check (see maybeAutoStation). */
    private final Runnable stationTick = this::stationTick;
    /** Set by a move left in progress: the next station check runs soon instead of in a minute. */
    private volatile boolean continueSoon;

    private HandlerThread workerThread;
    private volatile Handler worker;
    private boolean started;
    /** elapsedRealtime of the last accepted clock set; 0 = not in this process. */
    private long lastClockSyncElapsed;
    /** One warning per "not recording" episode, cleared when recording is seen again. */
    private boolean warnedThisEpisode;
    /** Automatic "Rede da câmera": one attempt per run (car start), see DashcamStationPolicy. */
    /** Wall-clock ms of the last automatic move attempt (10 min guard across restarts). */
    static final String KEY_AUTO_STATION_LAST_ATTEMPT = "auto_station_last_attempt_ms";
    /** Wall-clock ms of the last manual "back to the camera Wi-Fi"; holds the automatic mode off. */
    public static final String KEY_AUTO_STATION_PAUSED_AT = "auto_station_paused_at_ms";
    /** Last automatic switch outcome, shown on the "Rede da câmera" card. */
    public static final String KEY_AUTO_STATION_LAST_RESULT_AT = "auto_station_last_result_at_ms";
    public static final String KEY_AUTO_STATION_LAST_RESULT_OK = "auto_station_last_result_ok";
    public static final String KEY_AUTO_STATION_LAST_RESULT_TEXT = "auto_station_last_result_text";

    public DashcamAutomationManager(Context context, ServiceManager serviceManager) {
        this.context = context.getApplicationContext();
        this.serviceManager = serviceManager;
        this.preferences = App.getDeviceProtectedContext()
                .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
        this.statePreferences = App.getDeviceProtectedContext()
                .getSharedPreferences(DashcamGalleryRepository.STATE_PREFS, Context.MODE_PRIVATE);
        this.client = new DashcamHisiliconClient(this.context);
        this.novatek = new DashcamNovatekClient(this.context);
        // 177: the owner asked for the camera clock sync to be on (it lines clips up with the GPS
        // trail). Turn it on once, even where it had been switched off; later choices stick.
        if (!statePreferences.getBoolean("migrated_clock_sync_on_177", false)) {
            preferences.edit().putBoolean(SharedPreferencesKeys.ENABLE_DASHCAM_CLOCK_SYNC.getKey(), true).apply();
            statePreferences.edit().putBoolean("migrated_clock_sync_on_177", true).apply();
        }
        this.timeSource = new DashcamTimeSource(this.context);
    }

    public void start() {
        synchronized (lifecycleLock) {
            if (started) return;
            workerThread = new HandlerThread("DashcamAutomation");
            workerThread.start();
            worker = new Handler(workerThread.getLooper());
            started = true;
        }
        preferences.registerOnSharedPreferenceChangeListener(this);
        worker.postDelayed(tick, FIRST_CHECK_DELAY_MS);
        worker.postDelayed(stationTick, FIRST_CHECK_DELAY_MS);
    }

    public void stop() {
        HandlerThread thread;
        Handler handler;
        synchronized (lifecycleLock) {
            if (!started) return;
            started = false;
            thread = workerThread;
            handler = worker;
            worker = null;
            workerThread = null;
        }
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (thread != null) thread.quitSafely();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!SharedPreferencesKeys.ENABLE_DASHCAM_WATCHDOG_RESUME.getKey().equals(key)
                && !SharedPreferencesKeys.ENABLE_DASHCAM_CLOCK_SYNC.getKey().equals(key)
                && !SharedPreferencesKeys.ENABLE_DASHCAM_FAILURE_WARNING.getKey().equals(key)
                && !SharedPreferencesKeys.ENABLE_DASHCAM_AUTO_STATION.getKey().equals(key)) {
            return;
        }
        Handler handler = worker;
        if (handler == null) return;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, PREF_CHANGE_DELAY_MS);
        if (SharedPreferencesKeys.ENABLE_DASHCAM_AUTO_STATION.getKey().equals(key)) {
            handler.removeCallbacks(stationTick);
            handler.postDelayed(stationTick, PREF_CHANGE_DELAY_MS);
        }
    }

    /** Every dashcam option is on until the user turns it off. */
    private boolean pref(SharedPreferencesKeys key) {
        return preferences.getBoolean(key.getKey(), true);
    }

    private void tick() {
        try {
            runCheck();
        } catch (RuntimeException e) {
            Log.w(TAG, "check failed", e);
            log("check", "exception=" + e.getClass().getSimpleName());
        } finally {
            Handler handler = worker;
            if (handler != null) {
                handler.removeCallbacks(tick);
                handler.postDelayed(tick, CHECK_INTERVAL_MS);
            }
        }
    }

    private void runCheck() {
        boolean resumeEnabled = pref(SharedPreferencesKeys.ENABLE_DASHCAM_WATCHDOG_RESUME);
        boolean clockEnabled = pref(SharedPreferencesKeys.ENABLE_DASHCAM_CLOCK_SYNC);
        boolean warningEnabled = pref(SharedPreferencesKeys.ENABLE_DASHCAM_FAILURE_WARNING);
        boolean galleryPauseOpen =
                statePreferences.getBoolean(DashcamGalleryRepository.KEY_PAUSED_BY_GALLERY, false);
        if (!resumeEnabled && !clockEnabled && !warningEnabled && !galleryPauseOpen) return;

        // NTP needs an internet network, which is usually not the camera Wi-Fi: sample first.
        if (clockEnabled) timeSource.refreshIfDue();
        if (!client.hasWifiNetwork()) return;
        // Resume and the HiSilicon state parser are HiSilicon-only; a Novatek camera gets the clock
        // (so its clip names line up with the GPS trail) and nothing else here.
        if (DashcamProfile.detect(context) == DashcamProfile.NOVATEK) {
            if (!clockEnabled) return;
            if (!lock()) {
                log("check", "camera_busy");
                return;
            }
            try {
                syncNovatekClockIfDue();
            } finally {
                DashcamHisiliconClient.CAMERA_LOCK.unlock();
            }
            return;
        }

        if (!lock()) {
            log("check", "camera_busy");
            return;
        }
        try {
            // Re-read under the lock: a gallery session may have finished while we waited.
            if (statePreferences.getBoolean(DashcamGalleryRepository.KEY_PAUSED_BY_GALLERY, false)) {
                recoverGalleryPause();
            }
            if (clockEnabled) syncClockIfDue();
            if (resumeEnabled || warningEnabled) checkRecording(resumeEnabled, warningEnabled);
        } finally {
            DashcamHisiliconClient.CAMERA_LOCK.unlock();
        }
    }

    /**
     * "Fazer isso sozinho a cada partida" (off by default), every minute: on the camera's own Wi-Fi
     * with the station SSID in range, move the camera; on the station network without the camera but
     * with the camera Wi-Fi in range, hop there and move it. Anywhere else nothing. A failed attempt
     * waits 5 min; a manual return holds it 30 min ({@link DashcamStationPolicy#decideAuto}).
     */
    private void maybeAutoStation() {
        if (!preferences.getBoolean(SharedPreferencesKeys.ENABLE_DASHCAM_AUTO_STATION.getKey(), false)) return;
        if (android.os.SystemClock.elapsedRealtime() < DashcamStationPolicy.AUTO_MIN_UPTIME_MS) return;
        String ssid = DashcamCameraHost.stationSsid();
        if (ssid == null) return;
        // Owner's rules: only on the camera's own Wi-Fi or on the station network; anywhere else, nothing.
        DashcamCameraHost.Place place = DashcamCameraHost.place(context);
        if (place != DashcamCameraHost.Place.CAMERA_AP && place != DashcamCameraHost.Place.STATION) return;
        long now = System.currentTimeMillis();
        long sinceAttempt = since(statePreferences.getLong(KEY_AUTO_STATION_LAST_ATTEMPT, 0L), now);
        long sinceReturn = since(statePreferences.getLong(KEY_AUTO_STATION_PAUSED_AT, 0L), now);
        DashcamNetworkManager network = DashcamNetworkManager.getInstance(context);
        if (network.isBusy()) return;
        // Cheap guards first (timers), then the camera probe / Wi-Fi scan.
        if (DashcamStationPolicy.decideAuto(true, DashcamCameraHost.Place.CAMERA_AP, true, false, false,
                sinceAttempt, sinceReturn) == DashcamStationPolicy.AutoAction.NONE) {
            return;
        }
        boolean stationVisible = false;
        boolean cameraFound = false;
        boolean apVisible = false;
        if (place == DashcamCameraHost.Place.CAMERA_AP) {
            stationVisible = network.isVisible(ssid, true);
        } else {
            if (!lock()) return;
            try {
                cameraFound = DashcamCameraHost.resolve(context, true, false, novatek::isNovatekAt) != null;
            } finally {
                DashcamHisiliconClient.CAMERA_LOCK.unlock();
            }
            String apSsid = DashcamCameraHost.cameraApSsid();
            if (!cameraFound && apSsid != null) apVisible = network.isVisible(apSsid, true);
        }
        DashcamStationPolicy.AutoAction action = DashcamStationPolicy.decideAuto(true, place,
                stationVisible, cameraFound, apVisible, sinceAttempt, sinceReturn);
        if (action == DashcamStationPolicy.AutoAction.NONE) return;
        log("auto_station", "action=" + action);
        DashcamNetworkManager.Result result = action == DashcamStationPolicy.AutoAction.MOVE_TO_STATION
                ? network.moveToStationBlocking("auto")
                : network.hopToApThenMoveBlocking();
        log("auto_station", "ok=" + result.ok + " code=" + result.code);
        statePreferences.edit()
                .putLong(KEY_AUTO_STATION_LAST_RESULT_AT, now)
                .putBoolean(KEY_AUTO_STATION_LAST_RESULT_OK, result.ok)
                .putString(KEY_AUTO_STATION_LAST_RESULT_TEXT, result.message)
                .apply();
        if (result.ok) {
            statePreferences.edit().remove(KEY_AUTO_STATION_LAST_ATTEMPT).apply();
        } else if (DashcamStationPolicy.IN_PROGRESS.equals(result.code)) {
            // Some steps went through before Android left the camera Wi-Fi: continue soon.
            statePreferences.edit().remove(KEY_AUTO_STATION_LAST_ATTEMPT).apply();
            Handler handler = worker;
            if (handler != null) {
                handler.removeCallbacks(stationTick);
                handler.postDelayed(stationTick, DashcamStationPolicy.IN_PROGRESS_RETRY_MS);
                continueSoon = true;
            }
        } else {
            statePreferences.edit().putLong(KEY_AUTO_STATION_LAST_ATTEMPT, now).apply();
        }
    }

    private static long since(long at, long now) {
        return at <= 0 ? -1L : now - at;
    }

    private void stationTick() {
        try {
            maybeAutoStation();
        } catch (RuntimeException e) {
            Log.w(TAG, "auto station failed", e);
            log("auto_station", "exception=" + e.getClass().getSimpleName());
        } finally {
            Handler handler = worker;
            if (handler != null && !continueSoon) {
                handler.removeCallbacks(stationTick);
                handler.postDelayed(stationTick, DashcamStationPolicy.AUTO_CHECK_INTERVAL_MS);
            }
            continueSoon = false;
        }
    }

    /** A gallery pause left open: resume it, or hand it to the charging pause if that applies. */
    private void recoverGalleryPause() {
        if (chargingWantsPause()) {
            DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> state =
                    client.getWorkState();
            if (!state.isSuccess()) {
                log("gallery_recovery", "status_failed=" + state.getError());
                return;
            }
            if (!Boolean.FALSE.equals(state.getValue().getRunning())) {
                // Already recording again: nothing to hand over, just drop the stale mark.
                statePreferences.edit()
                        .putBoolean(DashcamGalleryRepository.KEY_PAUSED_BY_GALLERY, false).commit();
                log("gallery_recovery", "already_running");
                return;
            }
            boolean handed = statePreferences.edit()
                    .putBoolean(DashcamGalleryRepository.KEY_PAUSED_BY_CHARGING, true)
                    .putBoolean(DashcamGalleryRepository.KEY_PAUSED_BY_GALLERY, false)
                    .commit();
            log("gallery_recovery", "handed_to_charging=" + handed);
            return;
        }
        Boolean resumed = DashcamGalleryRepository.getInstance(context).newSession().resumeOwnedPause();
        log("gallery_recovery", "confirmed=" + resumed);
    }

    private void syncClockIfDue() {
        long now = SystemClock.elapsedRealtime();
        if (lastClockSyncElapsed > 0 && now - lastClockSyncElapsed < CLOCK_SYNC_INTERVAL_MS) return;
        Long utc = timeSource.currentUtcMs();
        if (utc == null) {
            log("clock", "no_trusted_time");
            return;
        }
        String local = Instant.ofEpochMilli(utc).atZone(ZoneId.systemDefault()).format(CAMERA_TIME);
        DashcamHisiliconClient.Result<Boolean> result = client.setSystemTime(local);
        if (result.isSuccess()) {
            lastClockSyncElapsed = now;
            statePreferences.edit().putLong(KEY_CLOCK_SYNCED_AT, utc).apply();
            log("clock", "set source=" + timeSource.lastSource());
        } else {
            log("clock", "failed=" + result.getError());
        }
    }

    private void syncNovatekClockIfDue() {
        long now = SystemClock.elapsedRealtime();
        if (lastClockSyncElapsed > 0 && now - lastClockSyncElapsed < CLOCK_SYNC_INTERVAL_MS) return;
        Long utc = timeSource.currentUtcMs();
        if (utc == null) {
            log("clock", "novatek no_trusted_time");
            return;
        }
        // Two requests; aim the time at the second one (~0.5 s later), rounded to the second.
        java.time.LocalDateTime local = Instant.ofEpochMilli(utc + 500L).atZone(ZoneId.systemDefault())
                .toLocalDateTime().withNano(0);
        DashcamHisiliconClient.Result<Boolean> result = novatek.setClock(local);
        if (result.isSuccess()) {
            lastClockSyncElapsed = now;
            statePreferences.edit().putLong(KEY_CLOCK_SYNCED_AT, utc).apply();
            log("clock", "novatek set " + local + " source=" + timeSource.lastSource());
        } else {
            log("clock", "novatek failed=" + result.getError());
        }
    }

    private void checkRecording(boolean resumeEnabled, boolean warningEnabled) {
        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> status = client.getWorkState();
        if (!status.isSuccess()) {
            log("check", "status_failed=" + status.getError());
            return;
        }
        DashcamHisiliconProtocol.WorkState state = status.getValue();
        if (Boolean.TRUE.equals(state.getRunning())) {
            warnedThisEpisode = false;
            return;
        }
        boolean pausedByCharging =
                statePreferences.getBoolean(DashcamGalleryRepository.KEY_PAUSED_BY_CHARGING, false);
        DashcamWatchdogPolicy.Action action = DashcamWatchdogPolicy.decide(
                state.getRunning(), state.getEmergencyRecording(), pausedByCharging,
                chargingWantsPause(), resumeEnabled, warningEnabled);
        switch (action) {
            case RESUME:
                if (resume()) {
                    warnedThisEpisode = false;
                    log("check", "resumed");
                } else if (warningEnabled) {
                    warn("A dashcam parou de gravar e o Impulse não conseguiu retomar.");
                }
                break;
            case WARN:
                warn("A dashcam não está gravando.");
                break;
            case NONE:
            default:
                break;
        }
    }

    private boolean resume() {
        DashcamHisiliconClient.Result<Boolean> command = client.setRecording(true);
        if (!command.isSuccess()) {
            log("check", "resume_failed=" + command.getError());
            return false;
        }
        DashcamHisiliconClient.Result<DashcamHisiliconProtocol.WorkState> verified = client.getWorkState();
        boolean ok = verified.isSuccess() && Boolean.TRUE.equals(verified.getValue().getRunning());
        if (!ok) log("check", "resume_unverified");
        return ok;
    }

    /** True when a pause rule (charging and stopped, or not READY) currently asks for a pause. */
    private boolean chargingWantsPause() {
        boolean charging = pref(SharedPreferencesKeys.ENABLE_DASHCAM_PAUSE_WHILE_CHARGING);
        boolean ready = pref(SharedPreferencesKeys.ENABLE_DASHCAM_RECORD_ONLY_WHEN_READY);
        if (!charging && !ready) return false;
        return DashcamRecordingPolicy.decide(
                charging,
                serviceManager.getUpdatedData(CarConstants.CAR_EV_INFO_CHARGING_STATE.getValue()),
                serviceManager.getUpdatedData(CarConstants.CAR_BASIC_VEHICLE_SPEED.getValue()),
                ready,
                serviceManager.getUpdatedData(CarConstants.CAR_BASIC_DRIVING_READY_STATE.getValue()))
                == DashcamChargingPausePolicy.Action.PAUSE;
    }

    private void warn(String message) {
        if (warnedThisEpisode) return;
        warnedThisEpisode = true;
        log("warning", "shown");
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            } catch (RuntimeException ignored) {
                // Toast is best effort; the notification below is the durable signal.
            }
        });
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        try {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Avisos da dashcam", NotificationManager.IMPORTANCE_HIGH));
            Notification notification = new Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle("Dashcam")
                    .setContentText(message)
                    .setAutoCancel(true)
                    .build();
            manager.notify(NOTIFICATION_ID, notification);
        } catch (RuntimeException e) {
            Log.w(TAG, "notification failed", e);
        }
    }

    private boolean lock() {
        try {
            return DashcamHisiliconClient.CAMERA_LOCK.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void log(String phase, String detail) {
        // Sempre no logcat: o log persistente só grava com a captura de diagnóstico ligada.
        Log.i(TAG, "phase=" + phase + " " + detail);
        ClusterPersistentEventLogger.logText("dashcam_automation", "phase=" + phase + " " + detail);
    }
}
