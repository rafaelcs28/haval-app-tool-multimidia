package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Enviar ao celular": serves ONE saved clip on the head unit's Wi-Fi address for a few minutes,
 * so a phone on the same network (home Wi-Fi or the phone's hotspot) downloads it from a QR code.
 *
 * <p>Only {@code GET /<random token>/<file name>} is answered; the token is 128 random bits and
 * never leaves the device except in the QR shown on screen. Single {@code Range} requests are
 * honored so phones can resume. The server stops by itself at expiry or on {@link #stop()}.
 * It refuses to start on the camera's own Wi-Fi, which a phone is not on.
 */
public final class DashcamShareServer {
    private static final String TAG = "DashcamShare";
    public static final long LIFETIME_MS = 15L * 60 * 1000;
    private static final int MAX_CONNECTIONS = 3;
    private static final Pattern RANGE = Pattern.compile("^bytes=([0-9]{1,15})-([0-9]{0,15})$");

    private static DashcamShareServer current;

    public static final class Link {
        public final String url;
        public final long expiresAtMs;
        public final String error;

        Link(String url, long expiresAtMs, String error) {
            this.url = url;
            this.expiresAtMs = expiresAtMs;
            this.error = error;
        }
    }

    private final File file;
    private final String token;
    private final ServerSocket server;
    private final long expiresAtMs;
    private final AtomicInteger active = new AtomicInteger();
    private volatile boolean running = true;

    private DashcamShareServer(File file, String token, ServerSocket server, long expiresAtMs) {
        this.file = file;
        this.token = token;
        this.server = server;
        this.expiresAtMs = expiresAtMs;
    }

    /** Starts sharing {@code file} (stopping any previous share). Call off the main thread. */
    public static synchronized Link start(Context context, File file) {
        stop();
        if (file == null || !file.isFile() || !DashcamClipStore.isClipName(file.getName())) {
            return new Link(null, 0, "invalid_file");
        }
        Inet4Address address = wifiAddress(context);
        if (address == null) return new Link(null, 0, "wifi_unavailable");
        if (DashcamProfile.isNovatekSubnet(address) || isHiSiliconSubnet(address, context)) {
            return new Link(null, 0, "on_camera_wifi");
        }
        try {
            ServerSocket server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(address, 0), 4);
            byte[] random = new byte[16];
            new SecureRandom().nextBytes(random);
            StringBuilder token = new StringBuilder();
            for (byte b : random) token.append(String.format("%02x", b));
            long expires = System.currentTimeMillis() + LIFETIME_MS;
            DashcamShareServer share = new DashcamShareServer(file, token.toString(), server, expires);
            current = share;
            Thread thread = new Thread(share::acceptLoop, "DashcamShare");
            thread.setDaemon(true);
            thread.start();
            String url = "http://" + address.getHostAddress() + ":" + server.getLocalPort()
                    + "/" + token + "/" + file.getName();
            Log.i(TAG, "sharing " + file.getName() + " until " + expires);
            return new Link(url, expires, null);
        } catch (Exception e) {
            return new Link(null, 0, "server_" + e.getClass().getSimpleName());
        }
    }

    public static synchronized void stop() {
        DashcamShareServer share = current;
        current = null;
        if (share != null) share.close();
    }

    private void close() {
        running = false;
        try {
            server.close();
        } catch (Exception ignored) {
            // already closed
        }
    }

    private void acceptLoop() {
        try {
            server.setSoTimeout(1000);
        } catch (Exception ignored) {
            // keep the default
        }
        while (running && System.currentTimeMillis() < expiresAtMs) {
            try {
                Socket socket = server.accept();
                if (active.incrementAndGet() > MAX_CONNECTIONS) {
                    active.decrementAndGet();
                    socket.close();
                    continue;
                }
                Thread worker = new Thread(() -> {
                    try {
                        serve(socket);
                    } finally {
                        active.decrementAndGet();
                    }
                }, "DashcamShareConn");
                worker.setDaemon(true);
                worker.start();
            } catch (java.net.SocketTimeoutException timeout) {
                // check expiry
            } catch (Exception e) {
                if (running) Log.w(TAG, "accept failed", e);
                break;
            }
        }
        synchronized (DashcamShareServer.class) {
            if (current == this) current = null;
        }
        close();
        Log.i(TAG, "share stopped");
    }

    private void serve(Socket socket) {
        try (Socket s = socket) {
            s.setSoTimeout(15_000);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
            String requestLine = reader.readLine();
            String range = null;
            String line;
            int headers = 0;
            while ((line = reader.readLine()) != null && !line.isEmpty() && headers++ < 64) {
                if (line.regionMatches(true, 0, "Range:", 0, 6)) range = line.substring(6).trim();
            }
            OutputStream out = s.getOutputStream();
            String expected = "/" + token + "/" + file.getName();
            if (requestLine == null) return;
            String[] parts = requestLine.split(" ");
            boolean head = parts.length >= 2 && "HEAD".equals(parts[0]);
            if (parts.length < 2 || !("GET".equals(parts[0]) || head) || !expected.equals(parts[1])) {
                write(out, "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
                return;
            }
            long total = file.length();
            long start = 0;
            long end = total - 1;
            boolean partial = false;
            if (range != null) {
                Matcher matcher = RANGE.matcher(range);
                if (matcher.matches()) {
                    start = Long.parseLong(matcher.group(1));
                    if (!matcher.group(2).isEmpty()) end = Math.min(end, Long.parseLong(matcher.group(2)));
                    if (start > end || start >= total) {
                        write(out, "HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */" + total
                                + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
                        return;
                    }
                    partial = true;
                }
            }
            long length = end - start + 1;
            String type = file.getName().toLowerCase().endsWith(".ts") ? "video/mp2t" : "video/mp4";
            StringBuilder header = new StringBuilder();
            header.append(partial ? "HTTP/1.1 206 Partial Content\r\n" : "HTTP/1.1 200 OK\r\n")
                    .append("Content-Type: ").append(type).append("\r\n")
                    .append("Content-Length: ").append(length).append("\r\n")
                    .append("Accept-Ranges: bytes\r\n")
                    .append("Content-Disposition: attachment; filename=\"").append(file.getName()).append("\"\r\n")
                    .append("Cache-Control: no-store\r\n")
                    .append("Connection: close\r\n");
            if (partial) header.append("Content-Range: bytes ").append(start).append('-').append(end)
                    .append('/').append(total).append("\r\n");
            header.append("\r\n");
            write(out, header.toString());
            if (head) return;
            try (FileInputStream input = new FileInputStream(file)) {
                long skipped = 0;
                while (skipped < start) {
                    long n = input.skip(start - skipped);
                    if (n <= 0) return;
                    skipped += n;
                }
                byte[] buffer = new byte[64 * 1024];
                long remaining = length;
                while (remaining > 0 && running) {
                    int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read < 0) break;
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
                out.flush();
            }
        } catch (Exception e) {
            Log.i(TAG, "connection ended: " + e.getClass().getSimpleName());
        }
    }

    private static void write(OutputStream out, String text) throws Exception {
        out.write(text.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }

    /** IPv4 address of the Wi-Fi network (never VPN), or null. */
    static Inet4Address wifiAddress(Context context) {
        Network wifi = DashcamHisiliconClient.findCameraNetwork(context);
        if (wifi == null) return null;
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties properties = manager == null ? null : manager.getLinkProperties(wifi);
        if (properties == null) return null;
        for (LinkAddress address : properties.getLinkAddresses()) {
            InetAddress inet = address.getAddress();
            if (inet instanceof Inet4Address && !inet.isLoopbackAddress()) return (Inet4Address) inet;
        }
        return null;
    }

    /**
     * The HiSilicon camera network is 192.168.0.x like many home routers; treat it as the camera
     * only when the camera answers there. Home networks fall through and sharing works.
     */
    private static boolean isHiSiliconSubnet(Inet4Address address, Context context) {
        byte[] b = address.getAddress();
        if ((b[0] & 0xff) != 192 || (b[1] & 0xff) != 168 || (b[2] & 0xff) != 0) return false;
        return new DashcamHisiliconClient(context).getWorkState().isSuccess();
    }
}
