package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/** Minimal local-network client for the validated HiSilicon dashcam profile. */
final class DashcamHisiliconClient implements DashcamCatalogSession.Camera {
    static final String HOST = "192.168.0.1";
    static final int PORT = 80;

    /**
     * Serializes every multi-step camera sequence in this process (charging pause, gallery catalog,
     * watchdog and clock). A pause/resume pair must never interleave with another writer.
     */
    static final ReentrantLock CAMERA_LOCK = new ReentrantLock(true);
    private static final int TIMEOUT_MS = 3000;
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;
    private static final String PREFIX = "/cgi-bin/hisnet/";

    private final Context context;

    DashcamHisiliconClient(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Any parseable state, including modes other than NORM_REC, for display only. Writers must use
     * {@link #getWorkState()}, which accepts only the validated profile.
     */
    Result<DashcamHisiliconProtocol.WorkState> readAnyWorkState() {
        Result<DashcamHisiliconProtocol.HttpResponse> response = request("getworkstate.cgi?");
        if (!response.isSuccess()) return Result.failure(response.error);
        if (!response.value.isOk()) return Result.failure("http_" + response.value.getStatusCode());
        DashcamHisiliconProtocol.WorkState state =
                DashcamHisiliconProtocol.parseWorkState(response.value.getBody());
        return state == null ? Result.failure("malformed_status") : Result.success(state);
    }

    @Override
    public Result<DashcamHisiliconProtocol.WorkState> getWorkState() {
        Result<DashcamHisiliconProtocol.HttpResponse> response = request("getworkstate.cgi?");
        if (!response.isSuccess()) return Result.failure(response.error);
        if (!response.value.isOk()) return Result.failure("http_" + response.value.getStatusCode());
        DashcamHisiliconProtocol.WorkState state =
                DashcamHisiliconProtocol.parseWorkState(response.value.getBody());
        if (state == null || !state.isTrustedNormalState()) {
            return Result.failure("untrusted_status");
        }
        return Result.success(state);
    }

    @Override
    public Result<Boolean> setRecording(boolean recording) {
        String command = recording ? "start" : "stop";
        Result<DashcamHisiliconProtocol.HttpResponse> response =
                request("workmodecmd.cgi?&-cmd=" + command);
        if (!response.isSuccess()) return Result.failure(response.error);
        if (!response.value.isOk()) return Result.failure("http_" + response.value.getStatusCode());
        if (!DashcamHisiliconProtocol.isSuccessfulCommandBody(response.value.getBody())) {
            return Result.failure("command_rejected");
        }
        return Result.success(Boolean.TRUE);
    }

    /** Directories announced by the camera; read-only. */
    Result<List<String>> getDirCapability() {
        Result<DashcamHisiliconProtocol.HttpResponse> response = request("getdircapability.cgi?");
        if (!response.isSuccess()) return Result.failure(response.error);
        if (!response.value.isOk()) return Result.failure("http_" + response.value.getStatusCode());
        List<String> dirs = DashcamHisiliconProtocol.parseCapability(response.value.getBody());
        return dirs == null ? Result.failure("malformed_capability") : Result.success(dirs);
    }

    /**
     * One catalog page, inclusive zero-based indexes. {@code busy_recording} is the -2222 answer
     * the tested firmware gives while it records.
     */
    @Override
    public Result<DashcamHisiliconProtocol.FileListPage> getFileListPage(String dir, int start, int end) {
        if (!"norm".equals(dir) && !"emr".equals(dir)) return Result.failure("invalid_dir");
        if (start < 0 || end < start) return Result.failure("invalid_range");
        Result<DashcamHisiliconProtocol.HttpResponse> response = request(
                "getdirfilelist.cgi?&-dir=" + dir + "&-start=" + start + "&-end=" + end);
        if (!response.isSuccess()) return Result.failure(response.error);
        if (!response.value.isOk()) return Result.failure("http_" + response.value.getStatusCode());
        String body = response.value.getBody();
        Integer code = DashcamHisiliconProtocol.parseFunctionResult(body);
        if (code != null) {
            return Result.failure(code == DashcamHisiliconProtocol.RESULT_BUSY_RECORDING
                    ? "busy_recording" : "function_result_" + code);
        }
        DashcamHisiliconProtocol.FileListPage page =
                DashcamHisiliconProtocol.parseFileListPage(body, dir);
        return page == null ? Result.failure("malformed_file_list") : Result.success(page);
    }

    /** Sets the camera clock; {@code localTime} is the 14-digit local time yyyyMMddHHmmss. */
    Result<Boolean> setSystemTime(String localTime) {
        if (localTime == null || !localTime.matches("[0-9]{14}")) return Result.failure("invalid_time");
        Result<DashcamHisiliconProtocol.HttpResponse> response =
                request("setsystime.cgi?&-time=" + localTime);
        if (!response.isSuccess()) return Result.failure(response.error);
        if (!response.value.isOk()) return Result.failure("http_" + response.value.getStatusCode());
        if (!DashcamHisiliconProtocol.isSuccessfulCommandBody(response.value.getBody())) {
            return Result.failure("command_rejected");
        }
        return Result.success(Boolean.TRUE);
    }

    boolean hasWifiNetwork() {
        return findCameraNetwork(context) != null;
    }

    private Result<DashcamHisiliconProtocol.HttpResponse> request(String relativePath) {
        Network wifi = findCameraNetwork(context);
        if (wifi == null) return Result.failure("wifi_unavailable");

        try (Socket socket = wifi.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            String request =
                    "GET " + PREFIX + relativePath + " HTTP/1.0\r\n"
                            + "Host: " + HOST + "\r\n"
                            + "Connection: close\r\n\r\n";
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes(StandardCharsets.ISO_8859_1));
            output.flush();

            byte[] bytes = readBounded(socket.getInputStream());
            if (bytes == null) return Result.failure("response_too_large");
            DashcamHisiliconProtocol.HttpResponse response =
                    DashcamHisiliconProtocol.parseHttpResponse(bytes);
            return response == null ? Result.failure("malformed_http") : Result.success(response);
        } catch (Exception e) {
            return Result.failure("transport_" + e.getClass().getSimpleName());
        }
    }

    /** Wi-Fi network (never VPN) used to reach the camera; null when Wi-Fi is not connected. */
    static Network findCameraNetwork(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return null;
        for (Network network : manager.getAllNetworks()) {
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
            if (capabilities != null
                    && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    && !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return network;
            }
        }
        return null;
    }

    private static byte[] readBounded(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > MAX_RESPONSE_BYTES) return null;
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    public static final class Result<T> {
        private final T value;
        private final String error;

        private Result(T value, String error) {
            this.value = value;
            this.error = error;
        }

        static <T> Result<T> success(T value) {
            return new Result<>(value, null);
        }

        static <T> Result<T> failure(String error) {
            return new Result<>(null, error);
        }

        public boolean isSuccess() {
            return error == null;
        }

        public T getValue() {
            return value;
        }

        public String getError() {
            return error;
        }
    }
}
