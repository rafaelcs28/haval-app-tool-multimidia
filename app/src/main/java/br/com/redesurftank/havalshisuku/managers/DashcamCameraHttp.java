package br.com.redesurftank.havalshisuku.managers;

import android.net.Network;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Minimal HTTP/1.1 GET over a raw socket bound to the camera Wi-Fi {@link Network}, for the
 * Novatek video stream and clip download. A raw socket is used on purpose: on the station network
 * the camera has a DHCP address (e.g. 10.x), and HttpURLConnection would refuse it under the app's
 * network security config, which only allows cleartext to the two camera AP addresses. The checks
 * HttpURLConnection gave us are kept here: IPv4 literal host only (no DNS), callers validate the
 * path, redirects are refused, a transfer coding (chunked) is refused, the head is bounded and the
 * body never reads past Content-Length.
 */
final class DashcamCameraHttp {
    static final int PORT = 80;

    private DashcamCameraHttp() {}

    /**
     * Opens {@code GET path} on {@code host}. {@code position}/{@code length} build a Range
     * ({@code length} -1 = to the end; 0/-1 = whole file). The caller closes the response.
     */
    static Response get(Network network, String host, String path, long position, long length,
            int connectTimeoutMs, int readTimeoutMs) throws IOException {
        if (network == null) throw new IOException("dashcam_wifi_unavailable");
        if (!DashcamCameraDiscovery.isIpv4Literal(host)) throw new IOException("dashcam_invalid_host");
        if (path == null || !path.startsWith("/") || path.indexOf(' ') >= 0 || path.indexOf('\r') >= 0
                || path.indexOf('\n') >= 0) {
            throw new IOException("dashcam_invalid_path");
        }
        Socket socket = network.getSocketFactory().createSocket();
        boolean handedOver = false;
        try {
            socket.connect(new InetSocketAddress(InetAddress.getByName(host), PORT), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);
            StringBuilder request = new StringBuilder()
                    .append("GET ").append(path).append(" HTTP/1.1\r\n")
                    .append("Host: ").append(host).append("\r\n")
                    .append("Accept-Encoding: identity\r\n")
                    .append("Connection: close\r\n");
            String range = DashcamHttpHead.rangeHeader(position, length);
            if (range != null) request.append("Range: ").append(range).append("\r\n");
            request.append("\r\n");
            OutputStream output = socket.getOutputStream();
            output.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
            output.flush();

            InputStream input = new BufferedInputStream(socket.getInputStream(), 64 * 1024);
            DashcamHttpHead head = readHead(input);
            if (head == null) throw new IOException("dashcam_malformed_http");
            if (head.isRedirect()) throw new IOException("dashcam_redirect_refused");
            if (head.hasTransferCoding()) throw new IOException("dashcam_transfer_coding");
            long contentLength = head.contentLength();
            if (contentLength == Long.MIN_VALUE) throw new IOException("dashcam_bad_content_length");
            Response response = new Response(socket, head, contentLength,
                    contentLength >= 0 ? new LimitedInputStream(input, contentLength) : input);
            handedOver = true;
            return response;
        } finally {
            if (!handedOver) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // already failing
                }
            }
        }
    }

    private static DashcamHttpHead readHead(InputStream input) throws IOException {
        byte[] buffer = new byte[DashcamHttpHead.MAX_HEAD_BYTES];
        int size = 0;
        while (size < buffer.length) {
            int b = input.read();
            if (b < 0) return null;
            buffer[size++] = (byte) b;
            if (size >= 4 && DashcamHttpHead.endsHead(buffer, size)) {
                return DashcamHttpHead.parse(new String(buffer, 0, size - 4, StandardCharsets.ISO_8859_1));
            }
        }
        return null; // head too large
    }

    static final class Response implements Closeable {
        private final Socket socket;
        final DashcamHttpHead head;
        /** -1 when the server sent no Content-Length (body ends when the socket closes). */
        final long contentLength;
        final InputStream body;

        Response(Socket socket, DashcamHttpHead head, long contentLength, InputStream body) {
            this.socket = socket;
            this.head = head;
            this.contentLength = contentLength;
            this.body = body;
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // closing a camera socket that already dropped
            }
        }
    }

    /** Returns end of stream after {@code remaining} bytes. */
    static final class LimitedInputStream extends InputStream {
        private final InputStream in;
        private long remaining;

        LimitedInputStream(InputStream in, long limit) {
            this.in = in;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = in.read();
            if (b >= 0) remaining--;
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) return -1;
            int read = in.read(buffer, offset, (int) Math.min(length, remaining));
            if (read > 0) remaining -= read;
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = in.skip(Math.min(n, remaining));
            if (skipped > 0) remaining -= skipped;
            return skipped;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(in.available(), remaining);
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
