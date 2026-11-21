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

/** Minimal local-network client for the validated HiSilicon dashcam profile. */
final class DashcamHisiliconClient {
    private static final String HOST = "192.168.0.1";
    private static final int PORT = 80;
    private static final int TIMEOUT_MS = 3000;
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;
    private static final String PREFIX = "/cgi-bin/hisnet/";

    private final Context context;

    DashcamHisiliconClient(Context context) {
        this.context = context.getApplicationContext();
    }

    Result<DashcamHisiliconProtocol.WorkState> getWorkState() {
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

    Result<Boolean> setRecording(boolean recording) {
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

    private Result<DashcamHisiliconProtocol.HttpResponse> request(String relativePath) {
        Network wifi = findWifiNetwork();
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

    private Network findWifiNetwork() {
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

    static final class Result<T> {
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

        boolean isSuccess() {
            return error == null;
        }

        T getValue() {
            return value;
        }

        String getError() {
            return error;
        }
    }
}
