package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.net.Network;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Client for the Novatek CarDV profile ({@link DashcamNovatekProtocol}). Same transport rules as the
 * HiSilicon client: socket bound to the Android Wi-Fi network (never VPN), HTTP/1.0, bounded
 * answers, no redirects. The host comes from {@link DashcamCameraHost} (camera AP or station
 * network), always a private IPv4 literal. Writes: {@link #setRecording}, {@link #setClock} and the
 * Wi-Fi mode commands sent by {@link DashcamNetworkManager} through {@link #queryAt}.
 */
final class DashcamNovatekClient {
    private static final int PORT = 80;
    private static final int TIMEOUT_MS = 3000;
    private static final int PROBE_TIMEOUT_MS = 1500;
    private static final int LIST_TIMEOUT_MS = 10_000;
    private static final int MAX_REPLY_BYTES = 64 * 1024;
    /** 915 files took 211 KB; 8 MB covers far larger cards. */
    private static final int MAX_LIST_BYTES = 8 * 1024 * 1024;

    private final Context context;

    DashcamNovatekClient(Context context) {
        this.context = context.getApplicationContext();
    }

    private static final long SAMPLE_GAP_MS = 1200L;

    DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> query(int cmd) {
        return query(String.valueOf(cmd));
    }

    private DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> query(String cmdAndParams) {
        DashcamHisiliconClient.Result<String> body = get(cmdAndParams, MAX_REPLY_BYTES, TIMEOUT_MS);
        if (!body.isSuccess()) return DashcamHisiliconClient.Result.failure(body.getError());
        DashcamNovatekProtocol.Reply reply = DashcamNovatekProtocol.parseReply(body.getValue());
        if (reply == null) return DashcamHisiliconClient.Result.failure("malformed_reply");
        return DashcamHisiliconClient.Result.success(reply);
    }

    /** Two {@code 2016} samples ~1 s apart; true while the counter moves. */
    DashcamHisiliconClient.Result<Boolean> isRecording() {
        DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> first =
                query(DashcamNovatekProtocol.CMD_RECORDING_SECONDS);
        if (!first.isSuccess()) return DashcamHisiliconClient.Result.failure(first.getError());
        try {
            Thread.sleep(SAMPLE_GAP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DashcamHisiliconClient.Result.failure("interrupted");
        }
        DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> second =
                query(DashcamNovatekProtocol.CMD_RECORDING_SECONDS);
        if (!second.isSuccess()) return DashcamHisiliconClient.Result.failure(second.getError());
        Boolean recording = DashcamNovatekProtocol.recordingFromSamples(
                first.getValue().getLongValue(), second.getValue().getLongValue());
        return recording == null
                ? DashcamHisiliconClient.Result.failure("unknown_recording_state")
                : DashcamHisiliconClient.Result.success(recording);
    }

    /** {@code 2001&par=1|0}; Status 0 is accepted. Callers verify with {@link #isRecording}. */
    DashcamHisiliconClient.Result<Boolean> setRecording(boolean recording) {
        DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> reply =
                query(DashcamNovatekProtocol.CMD_RECORD + "&par=" + (recording ? 1 : 0));
        if (!reply.isSuccess()) return DashcamHisiliconClient.Result.failure(reply.getError());
        if (reply.getValue().getStatus() != 0) {
            return DashcamHisiliconClient.Result.failure("status_" + reply.getValue().getStatus());
        }
        return DashcamHisiliconClient.Result.success(Boolean.TRUE);
    }

    /**
     * Sets the camera clock (local time, which is what it writes in the clip names): date, then
     * time. Status 0 on both is accepted; the next clip's name is the real proof.
     */
    DashcamHisiliconClient.Result<Boolean> setClock(java.time.LocalDateTime local) {
        String date = local.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String time = local.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> d =
                query(DashcamNovatekProtocol.CMD_SET_DATE + "&str=" + date);
        if (!d.isSuccess()) return DashcamHisiliconClient.Result.failure(d.getError());
        if (d.getValue().getStatus() != 0) {
            return DashcamHisiliconClient.Result.failure("date_status_" + d.getValue().getStatus());
        }
        DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> t =
                query(DashcamNovatekProtocol.CMD_SET_TIME + "&str=" + time);
        if (!t.isSuccess()) return DashcamHisiliconClient.Result.failure(t.getError());
        if (t.getValue().getStatus() != 0) {
            return DashcamHisiliconClient.Result.failure("time_status_" + t.getValue().getStatus());
        }
        return DashcamHisiliconClient.Result.success(Boolean.TRUE);
    }

    DashcamHisiliconClient.Result<List<DashcamNovatekProtocol.Entry>> fileList() {
        DashcamHisiliconClient.Result<String> body =
                get(String.valueOf(DashcamNovatekProtocol.CMD_FILE_LIST), MAX_LIST_BYTES, LIST_TIMEOUT_MS);
        if (!body.isSuccess()) return DashcamHisiliconClient.Result.failure(body.getError());
        List<DashcamNovatekProtocol.Entry> entries = DashcamNovatekProtocol.parseFileList(body.getValue());
        if (entries == null) return DashcamHisiliconClient.Result.failure("malformed_file_list");
        return DashcamHisiliconClient.Result.success(entries);
    }

    /**
     * {@code cmd} on the current camera host ({@link DashcamCameraHost}): 192.168.1.254 on the camera
     * AP, the discovered DHCP address on the station network. A transport failure on the station
     * network drops the cached address so the next request rediscovers it.
     */
    private DashcamHisiliconClient.Result<String> get(String cmd, int maxBytes, int readTimeoutMs) {
        String host = DashcamCameraHost.resolve(context, true, false, this::isNovatekAt);
        if (host == null) return DashcamHisiliconClient.Result.failure("transport_camera_not_found");
        DashcamHisiliconClient.Result<String> result = getAt(host, cmd, maxBytes, TIMEOUT_MS, readTimeoutMs);
        if (!result.isSuccess() && isWrongHostError(result.getError())) {
            DashcamCameraHost.reportFailure(host);
        } else if (result.isSuccess() && DashcamCameraHost.AP_HOST.equals(host)) {
            DashcamCameraHost.rememberApIdentity(context, false);
        }
        return result;
    }

    /** Errors that mean "not the camera (any more)" at that address, so it is rediscovered. */
    static boolean isWrongHostError(String error) {
        return error != null && (error.startsWith("transport_") || error.startsWith("http_")
                || "malformed_http".equals(error));
    }

    /** One command on an explicit host (network moves and discovery). Status is not checked. */
    DashcamHisiliconClient.Result<DashcamNovatekProtocol.Reply> queryAt(String host, String cmdAndParams) {
        DashcamHisiliconClient.Result<String> body = getAt(host, cmdAndParams, MAX_REPLY_BYTES, TIMEOUT_MS, TIMEOUT_MS);
        if (!body.isSuccess()) return DashcamHisiliconClient.Result.failure(body.getError());
        DashcamNovatekProtocol.Reply reply = DashcamNovatekProtocol.parseReply(body.getValue());
        if (reply == null) return DashcamHisiliconClient.Result.failure("malformed_reply");
        return DashcamHisiliconClient.Result.success(reply);
    }

    /** Discovery probe: {@code 3016} answers with a Novatek XML reply. */
    boolean isNovatekAt(String host) {
        DashcamHisiliconClient.Result<String> body = getAt(host,
                String.valueOf(DashcamNovatekProtocol.CMD_HEARTBEAT), MAX_REPLY_BYTES, PROBE_TIMEOUT_MS, PROBE_TIMEOUT_MS);
        return body.isSuccess() && DashcamNovatekProtocol.parseReply(body.getValue()) != null;
    }

    private DashcamHisiliconClient.Result<String> getAt(String host, String cmd, int maxBytes,
            int connectTimeoutMs, int readTimeoutMs) {
        if (!DashcamCameraHost.isAcceptableHost(host)) return DashcamHisiliconClient.Result.failure("invalid_host");
        Network wifi = DashcamHisiliconClient.findCameraNetwork(context);
        if (wifi == null) return DashcamHisiliconClient.Result.failure("wifi_unavailable");
        try (Socket socket = wifi.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress(InetAddress.getByName(host), PORT), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);
            String request = "GET /?custom=1&cmd=" + cmd + " HTTP/1.0\r\n"
                    + "Host: " + host + "\r\n"
                    + "Connection: close\r\n\r\n";
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes(StandardCharsets.ISO_8859_1));
            output.flush();
            byte[] bytes = readBounded(socket.getInputStream(), maxBytes);
            if (bytes == null) return DashcamHisiliconClient.Result.failure("response_too_large");
            DashcamHisiliconProtocol.HttpResponse response = DashcamHisiliconProtocol.parseHttpResponse(bytes);
            if (response == null) return DashcamHisiliconClient.Result.failure("malformed_http");
            if (!response.isOk()) return DashcamHisiliconClient.Result.failure("http_" + response.getStatusCode());
            return DashcamHisiliconClient.Result.success(response.getBody());
        } catch (Exception e) {
            return DashcamHisiliconClient.Result.failure("transport_" + e.getClass().getSimpleName());
        }
    }

    private static byte[] readBounded(InputStream input, int maxBytes) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > maxBytes) return null;
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
