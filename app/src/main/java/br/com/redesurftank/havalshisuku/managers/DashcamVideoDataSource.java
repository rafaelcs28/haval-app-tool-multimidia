package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.net.Network;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Streams one catalog video from the camera over the camera Wi-Fi (never VPN, never the default
 * route), with HTTP Range for seeking. Only validated catalog paths on the camera hosts are opened
 * (HiSilicon {@code 192.168.0.1/sd//<dir>/<name>.MP4}; Novatek {@code /CARDV/<dir>/<name>.TS} on
 * the camera's <em>current</em> host from {@link DashcamCameraHost}: 192.168.1.254 on its own
 * Wi-Fi or its DHCP address on the station network); redirects are refused.
 *
 * <p>Novatek goes through {@link DashcamCameraHttp} (raw socket): the app's network security config
 * only allows cleartext to the two camera AP addresses, and app-wide cleartext stays off. HiSilicon
 * keeps HttpURLConnection, as before.
 * Reads do not take the camera lock: the report shows downloads work while the camera records.
 */
@OptIn(markerClass = UnstableApi.class)
public final class DashcamVideoDataSource extends BaseDataSource {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private final Context context;
    @Nullable private DataSpec dataSpec;
    @Nullable private HttpURLConnection connection;
    @Nullable private DashcamCameraHttp.Response raw;
    @Nullable private InputStream input;
    private long bytesRemaining;
    private boolean opened;

    private DashcamVideoDataSource(Context context) {
        super(/* isNetwork= */ true);
        this.context = context.getApplicationContext();
    }

    /**
     * URI the player receives for a catalog recording. Novatek files point at the camera's last
     * known host, since the camera may have moved between the AP and the station network.
     */
    public static Uri uriFor(DashcamCatalog.Recording recording) {
        return Uri.parse("http://" + hostFor(recording) + recording.getHttpPath());
    }

    /** Host a recording is fetched from now (see {@link #uriFor}). */
    static String hostFor(DashcamCatalog.Recording recording) {
        return DashcamNovatekProtocol.isValidHttpPath(recording.getHttpPath())
                ? DashcamCameraHost.lastNovatekHost() : recording.getHost();
    }

    /**
     * HiSilicon: its fixed host and catalog path shape. Novatek: only {@code novatekHost} (the
     * camera's current host, a private IPv4 literal; null = camera not reachable) and its path shape.
     */
    static boolean isAllowed(String host, String encodedPath, @Nullable String novatekHost) {
        if (host == null || encodedPath == null || !encodedPath.startsWith("/")) return false;
        if (DashcamHisiliconClient.HOST.equals(host)) {
            return DashcamHisiliconProtocol.isValidCatalogPath(encodedPath.substring(1));
        }
        if (novatekHost != null && novatekHost.equals(host) && DashcamCameraHost.isAcceptableHost(host)) {
            return DashcamNovatekProtocol.isValidHttpPath(encodedPath);
        }
        return false;
    }

    /** Current Novatek host for {@link #isAllowed}; cheap (no discovery), never on the main thread. */
    @Nullable
    static String currentNovatekHost(Context context) {
        return DashcamCameraHost.resolve(context, false, false, null);
    }

    public static DataSource.Factory factory(Context context) {
        Context app = context.getApplicationContext();
        return () -> new DashcamVideoDataSource(app);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        this.dataSpec = dataSpec;
        Uri uri = dataSpec.uri;
        String encodedPath = uri.getEncodedPath();
        String host = uri.getHost();
        if (!"http".equals(uri.getScheme())
                || (uri.getPort() != -1 && uri.getPort() != DashcamHisiliconClient.PORT)
                || uri.getEncodedQuery() != null) {
            throw new IOException("dashcam_invalid_uri");
        }
        boolean novatek = DashcamNovatekProtocol.isValidHttpPath(encodedPath);
        String novatekHost = novatek ? currentNovatekHost(context) : null;
        if (!isAllowed(host, encodedPath, novatekHost)) throw new IOException("dashcam_invalid_uri");
        Network network = DashcamHisiliconClient.findCameraNetwork(context);
        if (network == null) throw new IOException("dashcam_wifi_unavailable");

        transferInitializing(dataSpec);
        long position = dataSpec.position;
        long length = dataSpec.length;
        int code;
        String contentRange;
        long contentLength;
        if (novatek) {
            DashcamCameraHttp.Response response;
            try {
                response = DashcamCameraHttp.get(network, host, encodedPath, position,
                        length == C.LENGTH_UNSET ? -1 : length, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            } catch (IOException e) {
                if (!"dashcam_redirect_refused".equals(e.getMessage())) DashcamCameraHost.reportFailure(host);
                throw e;
            }
            raw = response;
            code = response.head.statusCode;
            contentRange = response.head.header("Content-Range");
            contentLength = response.contentLength;
            input = response.body;
        } else {
            URL url = new URL("http", host, DashcamHisiliconClient.PORT, encodedPath);
            HttpURLConnection http = (HttpURLConnection) network.openConnection(url);
            connection = http;
            http.setInstanceFollowRedirects(false);
            http.setConnectTimeout(CONNECT_TIMEOUT_MS);
            http.setReadTimeout(READ_TIMEOUT_MS);
            http.setUseCaches(false);
            http.setRequestProperty("Accept-Encoding", "identity");
            String range = DashcamHttpHead.rangeHeader(position, length == C.LENGTH_UNSET ? -1 : length);
            if (range != null) http.setRequestProperty("Range", range);
            http.connect();
            code = http.getResponseCode();
            contentRange = http.getHeaderField("Content-Range");
            contentLength = http.getContentLengthLong();
        }

        long skip = 0;
        long available;
        if (code == HttpURLConnection.HTTP_PARTIAL) {
            long[] parsed = DashcamHttpHead.parseContentRange(contentRange);
            if (parsed == null || parsed[0] != position) {
                closeConnection();
                throw new IOException("dashcam_bad_content_range");
            }
            available = parsed[1] - position + 1;
        } else if (code == HttpURLConnection.HTTP_OK) {
            // Server ignored Range: skip up to the requested position ourselves.
            skip = position;
            long total = contentLength;
            available = total < 0 ? C.LENGTH_UNSET : total - position;
            if (total >= 0 && available < 0) {
                closeConnection();
                throw new IOException("dashcam_position_out_of_range");
            }
        } else {
            closeConnection();
            throw new IOException("dashcam_http_" + code);
        }

        if (input == null) input = connection.getInputStream();
        InputStream stream = input;
        while (skip > 0) {
            long skipped = stream.skip(skip);
            if (skipped <= 0) {
                if (stream.read() < 0) {
                    closeConnection();
                    throw new EOFException("dashcam_skip");
                }
                skipped = 1;
            }
            skip -= skipped;
        }

        if (length != C.LENGTH_UNSET) {
            bytesRemaining = available == C.LENGTH_UNSET ? length : Math.min(length, available);
        } else {
            bytesRemaining = available;
        }
        opened = true;
        transferStarted(dataSpec);
        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (length == 0) return 0;
        if (bytesRemaining == 0) return C.RESULT_END_OF_INPUT;
        InputStream stream = input;
        if (stream == null) throw new IOException("dashcam_not_open");
        int toRead = bytesRemaining == C.LENGTH_UNSET
                ? length : (int) Math.min(length, bytesRemaining);
        int read = stream.read(buffer, offset, toRead);
        if (read < 0) {
            if (bytesRemaining != C.LENGTH_UNSET) throw new EOFException("dashcam_truncated");
            return C.RESULT_END_OF_INPUT;
        }
        if (bytesRemaining != C.LENGTH_UNSET) bytesRemaining -= read;
        bytesTransferred(read);
        return read;
    }

    @Nullable
    @Override
    public Uri getUri() {
        return dataSpec == null ? null : dataSpec.uri;
    }

    @Override
    public void close() throws IOException {
        try {
            closeConnection();
        } finally {
            if (opened) {
                opened = false;
                transferEnded();
            }
            dataSpec = null;
        }
    }

    private void closeConnection() {
        InputStream stream = input;
        input = null;
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // closing a camera stream that already dropped
            }
        }
        HttpURLConnection http = connection;
        connection = null;
        if (http != null) http.disconnect();
        DashcamCameraHttp.Response socket = raw;
        raw = null;
        if (socket != null) socket.close();
    }
}
