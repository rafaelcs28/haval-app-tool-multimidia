package br.com.redesurftank.havalshisuku.managers;

import br.com.redesurftank.App;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger;

/**
 * Moves the Novatek camera (no buttons) between its own Wi-Fi and the station network configured
 * in "Rede da câmera", and the head unit along with it. Sequence verified on the car:
 * {@code 3032&str=SSID:PASS} → {@code 3033&par=1} → {@code 3018} (to station) and
 * {@code 3033&par=0} → {@code 3018} (back to its AP).
 *
 * <p>Camera commands run under {@link DashcamHisiliconClient#CAMERA_LOCK}; the Wi-Fi waits do not
 * hold it. Everything blocking runs on this manager's own thread, never on the main thread. Logs
 * carry phases and error codes only: no SSID, password or address.
 */
public final class DashcamNetworkManager {
    private static final String TAG = "DashcamNetwork";
    private static final long LOCK_WAIT_SECONDS = 20L;
    private static final long HEAD_UNIT_JOIN_TIMEOUT_MS = 45_000L;
    private static final long CAMERA_DISCOVERY_TIMEOUT_MS = 90_000L;
    private static final long AP_RETURN_TIMEOUT_MS = 75_000L;
    private static final long RETRY_ENABLE_MS = 10_000L;
    private static final long POLL_MS = 1_500L;
    private static final long SCAN_SETTLE_MS = 3_000L;
    private static final int HEARTBEAT_ATTEMPTS = 6;
    private static final long HEARTBEAT_RETRY_MS = 3_000L;

    private static volatile DashcamNetworkManager instance;

    private final Context context;
    private final DashcamNovatekClient novatek;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "DashcamNetwork");
        thread.setDaemon(true);
        return thread;
    });
    /** Status reads must not queue behind a two-minute move. */
    private final ExecutorService statusExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "DashcamNetworkStatus");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler main = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T value);
    }

    /** Outcome of a move; {@code message} is shown to the user as is (pt-BR). */
    public static final class Result {
        public final boolean ok;
        public final String code;
        public final String message;

        Result(boolean ok, String code, String message) {
            this.ok = ok;
            this.code = code;
            this.message = message;
        }

        static Result ok(String message) {
            return new Result(true, "ok", message);
        }

        static Result fail(String code, String message) {
            return new Result(false, code, message);
        }
    }

    /** Where the camera is, for the status line. */
    public static final class Status {
        public final DashcamCameraHost.Place place;
        public final String host;
        public final String text;

        Status(DashcamCameraHost.Place place, String host, String text) {
            this.place = place;
            this.host = host;
            this.text = text;
        }
    }

    private DashcamNetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.novatek = new DashcamNovatekClient(this.context);
    }

    public static DashcamNetworkManager getInstance(Context context) {
        DashcamNetworkManager local = instance;
        if (local == null) {
            synchronized (DashcamNetworkManager.class) {
                local = instance;
                if (local == null) {
                    local = new DashcamNetworkManager(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    public boolean isBusy() {
        return busy.get();
    }

    // ------------------------------------------------------------------------------ async (UI)

    public void moveToStation(Callback<Result> callback) {
        executor.execute(() -> deliver(callback, moveToStationBlocking("ui")));
    }

    public void returnToAp(Callback<Result> callback) {
        executor.execute(() -> deliver(callback, returnToApBlocking("ui")));
    }

    public void readStatus(Callback<Status> callback) {
        statusExecutor.execute(() -> deliver(callback, readStatusBlocking()));
    }

    private <T> void deliver(Callback<T> callback, T value) {
        main.post(() -> callback.onResult(value));
    }

    // -------------------------------------------------------------------------------- blocking

    /** Cheap: no discovery sweep and no camera traffic. */
    Status readStatusBlocking() {
        DashcamCameraHost.Wifi wifi = DashcamCameraHost.wifi(context);
        DashcamCameraHost.Place place = DashcamCameraHost.place(wifi);
        if (place == DashcamCameraHost.Place.CAMERA_AP) DashcamCameraHost.rememberApIdentity(context, false);
        String host = place == DashcamCameraHost.Place.CAMERA_AP ? DashcamCameraHost.AP_HOST
                : place == DashcamCameraHost.Place.STATION ? DashcamCameraHost.resolve(context, false, false, null)
                : null;
        String ssid = wifi == null ? null : wifi.ssid;
        return new Status(place, host, DashcamStationPolicy.statusText(place, host, ssid));
    }

    Result moveToStationBlocking(String trigger) {
        requireWorkerThread();
        if (!busy.compareAndSet(false, true)) return Result.fail("busy", "Já existe uma troca de rede em andamento.");
        try {
            return moveToStationLocked(trigger);
        } finally {
            busy.set(false);
        }
    }

    /**
     * Automatic mode only: the head unit is on the station network, the camera did not answer there
     * and its own AP is in range (the car started and the head unit joined the station network
     * before the camera was moved). Hop to the camera AP, then move the camera again.
     */
    Result hopToApThenMoveBlocking() {
        requireWorkerThread();
        if (!busy.compareAndSet(false, true)) return Result.fail("busy", "Já existe uma troca de rede em andamento.");
        try {
            String apSsid = DashcamCameraHost.cameraApSsid();
            if (apSsid == null) return Result.fail("ap_unknown", "Wi-Fi próprio da câmera ainda não conhecido.");
            log("hop", "connect_ap");
            if (!joinHeadUnit(apSsid, null, AP_RETURN_TIMEOUT_MS, this::cameraAnswersOnAp)) {
                log("hop", "failed=ap_join");
                return Result.fail("ap_join", "A central não voltou ao Wi-Fi da câmera.");
            }
            return moveToStationLocked("auto_hop");
        } finally {
            busy.set(false);
        }
    }

    Result returnToApBlocking(String trigger) {
        requireWorkerThread();
        if (!busy.compareAndSet(false, true)) return Result.fail("busy", "Já existe uma troca de rede em andamento.");
        try {
            Result result = returnToApLocked(trigger);
            // A manual "back to the camera Wi-Fi" must not be undone a minute later by the automatic mode.
            if (!"auto".equals(trigger)) {
                App.getDeviceProtectedContext()
                        .getSharedPreferences(DashcamGalleryRepository.STATE_PREFS, Context.MODE_PRIVATE)
                        .edit().putLong(DashcamAutomationManager.KEY_AUTO_STATION_PAUSED_AT, System.currentTimeMillis())
                        .apply();
            }
            return result;
        } finally {
            busy.set(false);
        }
    }



    private Result moveToStationLocked(String trigger) {
        log("to_station", "start trigger=" + trigger);
        DashcamCameraHost.Wifi wifi = DashcamCameraHost.wifi(context);
        if (wifi == null) return fail("to_station", "wifi_unavailable", "A central está sem Wi-Fi.");
        if (!wifi.onApSubnet) {
            return fail("to_station", "not_on_camera_ap", "Conecte a central ao Wi-Fi da câmera primeiro.");
        }
        if (DashcamProfile.detect(context) != DashcamProfile.NOVATEK) {
            return fail("to_station", "not_novatek", "Disponível só para a câmera Novatek (CARDV).");
        }
        String ssid = DashcamCameraHost.stationSsid();
        String password = DashcamCameraHost.stationPassword();
        String invalid = DashcamStationPolicy.validate(ssid, password);
        if (invalid != null) return fail("to_station", invalid, DashcamStationPolicy.validationMessage(invalid));
        if (ssid.equals(wifi.ssid)) return fail("to_station", "same_network", "Essa é a rede da própria câmera.");
        if (!isVisible(ssid, true)) {
            return fail("to_station", "station_not_visible", "A rede " + ssid + " não está ao alcance da central.");
        }

        if (!lock()) return fail("to_station", "camera_busy", "A câmera está ocupada. Tente de novo.");
        try {
            // Right after the head unit joins the AP (or the camera boots) its HTTP server can take
            // a few seconds to answer: retry the heartbeat before giving up.
            DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> heartbeat = null;
            for (int attempt = 1; attempt <= HEARTBEAT_ATTEMPTS; attempt++) {
                heartbeat = novatek.queryAt(DashcamCameraHost.AP_HOST,
                        String.valueOf(DashcamNovatekProtocol.CMD_HEARTBEAT));
                if (heartbeat.isSuccess()) break;
                log("to_station", "heartbeat_retry=" + attempt + " error=" + heartbeat.getError());
                if (attempt < HEARTBEAT_ATTEMPTS) sleep(HEARTBEAT_RETRY_MS);
            }
            if (!heartbeat.isSuccess()) {
                return fail("to_station", "heartbeat_" + heartbeat.getError(), "A câmera não respondeu.");
            }
            // The MAC is what finds the camera on the other network: learn it now.
            DashcamCameraHost.rememberApIdentity(context, true);
            String error = send(DashcamStationPolicy.stationCommand(ssid, password), "3032", false);
            if (error == null) error = send(DashcamStationPolicy.wifiModeCommand(true), "3033", false);
            if (error != null) {
                return fail("to_station", error, "A câmera recusou a configuração (" + error + ").");
            }
            error = send(String.valueOf(DashcamStationPolicy.CMD_WIFI_RESTART), "3018", true);
            if (error != null) return fail("to_station", error, "A câmera recusou reiniciar o Wi-Fi (" + error + ").");
        } finally {
            DashcamHisiliconClient.CAMERA_LOCK.unlock();
        }
        DashcamCameraHost.forgetStationHost();

        log("to_station", "camera_sent join_head_unit");
        if (!joinHeadUnit(ssid, password, HEAD_UNIT_JOIN_TIMEOUT_MS, () -> isOn(ssid))) {
            return fail("to_station", "head_unit_join",
                    "A câmera saiu do Wi-Fi próprio, mas a central não conectou à rede " + ssid
                            + ". Conecte a central a essa rede pelo Wi-Fi.");
        }
        String host = discoverOnStation(CAMERA_DISCOVERY_TIMEOUT_MS);
        if (host == null) {
            return fail("to_station", "camera_not_found",
                    "A câmera saiu do Wi-Fi próprio, mas não apareceu na rede " + ssid + ". Confira se a rede "
                            + "aceita novos aparelhos (no iPhone, deixe a tela Acesso Pessoal aberta). Desligar e "
                            + "ligar o carro devolve a câmera ao Wi-Fi próprio.");
        }
        log("to_station", "done");
        return Result.ok("Câmera na rede " + ssid + " (" + host + ").");
    }

    private Result returnToApLocked(String trigger) {
        log("to_ap", "start trigger=" + trigger);
        DashcamCameraHost.Place place = DashcamCameraHost.place(context);
        if (place == DashcamCameraHost.Place.CAMERA_AP) {
            return Result.ok("A câmera já está no Wi-Fi próprio.");
        }
        if (place != DashcamCameraHost.Place.STATION) {
            return fail("to_ap", "not_on_station", "A central não está na rede configurada para a câmera.");
        }
        if (!lock()) return fail("to_ap", "camera_busy", "A câmera está ocupada. Tente de novo.");
        try {
            String host = DashcamCameraHost.resolve(context, true, true, novatek::isNovatekAt);
            if (host == null) return fail("to_ap", "camera_not_found", "A câmera não foi encontrada nesta rede.");
            String error = sendAt(host, DashcamStationPolicy.wifiModeCommand(false), "3033", false);
            if (error != null) return fail("to_ap", error, "A câmera recusou voltar (" + error + ").");
            error = sendAt(host, String.valueOf(DashcamStationPolicy.CMD_WIFI_RESTART), "3018", true);
            if (error != null) return fail("to_ap", error, "A câmera recusou reiniciar o Wi-Fi (" + error + ").");
        } finally {
            DashcamHisiliconClient.CAMERA_LOCK.unlock();
        }
        DashcamCameraHost.forgetStationHost();

        String apSsid = DashcamCameraHost.cameraApSsid();
        if (apSsid == null) {
            log("to_ap", "camera_sent ap_unknown");
            return Result.ok("A câmera está voltando ao Wi-Fi próprio. Conecte a central a ele pelo Wi-Fi.");
        }
        log("to_ap", "camera_sent join_head_unit");
        if (!joinHeadUnit(apSsid, null, AP_RETURN_TIMEOUT_MS, this::cameraAnswersOnAp)) {
            return fail("to_ap", "head_unit_join",
                    "A câmera foi enviada de volta, mas a central não reconectou ao Wi-Fi " + apSsid + ".");
        }
        log("to_ap", "done");
        return Result.ok("Câmera de volta ao Wi-Fi próprio (" + DashcamCameraHost.AP_HOST + ").");
    }

    /** Sends one command to the AP address; null on Status 0. */
    private String send(String command, String label, boolean transportIsOk) {
        return sendAt(DashcamCameraHost.AP_HOST, command, label, transportIsOk);
    }

    /**
     * Null on Status 0. {@code transportIsOk}: for {@code 3018}, the camera may drop the
     * connection while restarting Wi-Fi before answering; that counts as sent.
     */
    private String sendAt(String host, String command, String label, boolean transportIsOk) {
        if (command == null) return label + "_invalid";
        DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> reply = novatek.queryAt(host, command);
        if (!reply.isSuccess()) {
            if (transportIsOk && reply.getError().startsWith("transport_")) {
                log("command", label + " unconfirmed=" + reply.getError());
                return null;
            }
            log("command", label + " failed=" + reply.getError());
            return label + "_" + reply.getError();
        }
        int status = reply.getValue().getStatus();
        log("command", label + " status=" + status);
        return status == 0 ? null : label + "_status_" + status;
    }

    /** Polls discovery (forced, under the lock per attempt) until the camera answers 3016. */
    private String discoverOnStation(long timeoutMs) {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        int attempts = 0;
        while (SystemClock.elapsedRealtime() < deadline) {
            attempts++;
            if (lock()) {
                try {
                    String host = DashcamCameraHost.resolve(context, true, true, novatek::isNovatekAt);
                    if (host != null && novatek.isNovatekAt(host)) {
                        log("discovery", "station_found attempts=" + attempts);
                        return host;
                    }
                    if (host != null) DashcamCameraHost.reportFailure(host);
                } finally {
                    DashcamHisiliconClient.CAMERA_LOCK.unlock();
                }
            }
            if (!sleep(5_000L)) return null;
        }
        log("discovery", "station_timeout attempts=" + attempts);
        return null;
    }

    private boolean cameraAnswersOnAp() {
        DashcamCameraHost.Wifi wifi = DashcamCameraHost.wifi(context);
        return wifi != null && wifi.onApSubnet && novatek.isNovatekAt(DashcamCameraHost.AP_HOST);
    }

    private boolean isOn(String ssid) {
        DashcamCameraHost.Wifi wifi = DashcamCameraHost.wifi(context);
        return wifi != null && wifi.ownIp != null && ssid.equals(wifi.ssid);
    }

    // ------------------------------------------------------------------------- head-unit Wi-Fi

    private WifiManager wifiManager() {
        return (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    /** True when {@code ssid} is in the scan results (optionally after a fresh scan). */
    boolean isVisible(String ssid, boolean freshScan) {
        WifiManager wifi = wifiManager();
        if (wifi == null || ssid == null) return false;
        try {
            if (freshScan) {
                // Android 9 throttles foreground scans (4 per 2 min); a refused scan keeps old results.
                wifi.startScan();
                sleep(SCAN_SETTLE_MS);
            }
            List<ScanResult> results = wifi.getScanResults();
            if (results == null) return false;
            for (ScanResult result : results) {
                if (ssid.equals(result.SSID)) return true;
            }
        } catch (RuntimeException e) {
            log("scan", "failed=" + e.getClass().getSimpleName());
        }
        return false;
    }

    /**
     * Connects the head unit to {@code ssid} (target SDK 28 API): reuses a saved configuration for
     * it, or adds a WPA2 one when {@code password} is given. Retries the enable while waiting for
     * {@code joined}.
     */
    private boolean joinHeadUnit(String ssid, String password, long timeoutMs, BooleanSupplier joined) {
        WifiManager wifi = wifiManager();
        if (wifi == null) return false;
        int netId;
        try {
            netId = networkIdFor(wifi, ssid, password);
        } catch (RuntimeException e) {
            log("join", "config_failed=" + e.getClass().getSimpleName());
            return false;
        }
        if (netId < 0) {
            log("join", "no_config");
            return false;
        }
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        long nextEnable = 0;
        while (SystemClock.elapsedRealtime() < deadline) {
            long now = SystemClock.elapsedRealtime();
            if (now >= nextEnable) {
                try {
                    boolean enabled = wifi.enableNetwork(netId, true);
                    if (!enabled) log("join", "enable_refused");
                } catch (RuntimeException e) {
                    log("join", "enable_failed=" + e.getClass().getSimpleName());
                }
                nextEnable = now + RETRY_ENABLE_MS;
            }
            if (!sleep(POLL_MS)) return false;
            if (joined.getAsBoolean()) {
                log("join", "ok");
                return true;
            }
        }
        log("join", "timeout");
        return false;
    }

    @SuppressWarnings("deprecation")
    private static int networkIdFor(WifiManager wifi, String ssid, String password) {
        WifiConfiguration existing = null;
        List<WifiConfiguration> configured = wifi.getConfiguredNetworks();
        if (configured != null) {
            for (WifiConfiguration config : configured) {
                if (ssid.equals(DashcamCameraHost.cleanSsid(config.SSID))) {
                    existing = config;
                    break;
                }
            }
        }
        if (password == null) return existing == null ? -1 : existing.networkId;
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        config.preSharedKey = "\"" + password + "\"";
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        if (existing != null) {
            // Ours can be updated with the current password; a config saved by Settings cannot
            // (Android refuses), and then the saved one is used as is.
            config.networkId = existing.networkId;
            int updated = wifi.updateNetwork(config);
            return updated >= 0 ? updated : existing.networkId;
        }
        return wifi.addNetwork(config);
    }

    // ---------------------------------------------------------------------------------- helpers

    private Result fail(String phase, String code, String message) {
        log(phase, "failed=" + code);
        return Result.fail(code, message);
    }

    private static boolean lock() {
        try {
            return DashcamHisiliconClient.CAMERA_LOCK.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void requireWorkerThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("camera network moves never run on the main thread");
        }
    }

    static void log(String phase, String detail) {
        // Sempre no logcat; sem SSID, senha nem endereços.
        Log.i(TAG, "phase=" + phase + " " + detail);
        ClusterPersistentEventLogger.logText("dashcam_network", "phase=" + phase + " " + detail);
    }
}
