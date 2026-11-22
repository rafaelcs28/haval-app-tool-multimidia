package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.net.Network;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Read-only client for the Novatek CarDV profile ({@link DashcamNovatekProtocol}). Same transport
 * rules as the HiSilicon client: socket bound to the Android Wi-Fi network (never VPN), fixed host,
 * HTTP/1.0, bounded answers, no redirects. Only {@link #setRecording} changes the camera.
 */
final class DashcamNovatekClient {
    private static final int PORT = 80;
    private static final int TIMEOUT_MS = 3000;
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

    private DashcamHisiliconClient.Result<String> get(String cmd, int maxBytes, int readTimeoutMs) {
        Network wifi = DashcamHisiliconClient.findCameraNetwork(context);
        if (wifi == null) return DashcamHisiliconClient.Result.failure("wifi_unavailable");
        try (Socket socket = wifi.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress(DashcamNovatekProtocol.HOST, PORT), TIMEOUT_MS);
            socket.setSoTimeout(readTimeoutMs);
            String request = "GET /?custom=1&cmd=" + cmd + " HTTP/1.0\r\n"
                    + "Host: " + DashcamNovatekProtocol.HOST + "\r\n"
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
