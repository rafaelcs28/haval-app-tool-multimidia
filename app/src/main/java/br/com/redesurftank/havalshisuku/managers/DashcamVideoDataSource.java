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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Streams one catalog video from the camera over the camera Wi-Fi (never VPN, never the default
 * route), with HTTP Range for seeking. Only validated catalog paths on the two camera hosts are
 * opened (HiSilicon {@code 192.168.0.1/sd//<dir>/<name>.MP4}, Novatek
 * {@code 192.168.1.254/CARDV/<dir>/<name>.TS}); redirects are refused.
 * Reads do not take the camera lock: the report shows downloads work while the camera records.
 */
@OptIn(markerClass = UnstableApi.class)
public final class DashcamVideoDataSource extends BaseDataSource {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final Pattern CONTENT_RANGE = Pattern.compile("^bytes ([0-9]+)-([0-9]+)/([0-9]+|\\*)$");

    private final Context context;
    @Nullable private DataSpec dataSpec;
    @Nullable private HttpURLConnection connection;
    @Nullable private InputStream input;
    private long bytesRemaining;
    private boolean opened;

    private DashcamVideoDataSource(Context context) {
        super(/* isNetwork= */ true);
        this.context = context.getApplicationContext();
    }

    /** URI the player receives for a catalog recording (HiSilicon or Novatek). */
    public static Uri uriFor(DashcamCatalog.Recording recording) {
        return Uri.parse("http://" + recording.getHost() + recording.getHttpPath());
    }

    /** Only the two camera hosts, each with its own validated path shape. */
    static boolean isAllowed(String host, String encodedPath) {
        if (encodedPath == null || !encodedPath.startsWith("/")) return false;
        if (DashcamHisiliconClient.HOST.equals(host)) {
            return DashcamHisiliconProtocol.isValidCatalogPath(encodedPath.substring(1));
        }
        if (DashcamNovatekProtocol.HOST.equals(host)) {
            return DashcamNovatekProtocol.isValidHttpPath(encodedPath);
        }
        return false;
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
                || uri.getEncodedQuery() != null
                || !isAllowed(host, encodedPath)) {
            throw new IOException("dashcam_invalid_uri");
        }
        Network network = DashcamHisiliconClient.findCameraNetwork(context);
        if (network == null) throw new IOException("dashcam_wifi_unavailable");

        transferInitializing(dataSpec);
        URL url = new URL("http", host, DashcamHisiliconClient.PORT, encodedPath);
        HttpURLConnection http = (HttpURLConnection) network.openConnection(url);
        connection = http;
        http.setInstanceFollowRedirects(false);
        http.setConnectTimeout(CONNECT_TIMEOUT_MS);
        http.setReadTimeout(READ_TIMEOUT_MS);
        http.setUseCaches(false);
        http.setRequestProperty("Accept-Encoding", "identity");
        long position = dataSpec.position;
        long length = dataSpec.length;
        if (position != 0 || length != C.LENGTH_UNSET) {
            String range = "bytes=" + position + "-";
            if (length != C.LENGTH_UNSET) range += (position + length - 1);
            http.setRequestProperty("Range", range);
        }
        http.connect();
        int code = http.getResponseCode();

        long skip = 0;
        long available;
        if (code == HttpURLConnection.HTTP_PARTIAL) {
            Matcher matcher = CONTENT_RANGE.matcher(String.valueOf(http.getHeaderField("Content-Range")));
            if (!matcher.matches() || Long.parseLong(matcher.group(1)) != position) {
                closeConnection();
                throw new IOException("dashcam_bad_content_range");
            }
            available = Long.parseLong(matcher.group(2)) - position + 1;
        } else if (code == HttpURLConnection.HTTP_OK) {
            // Server ignored Range: skip up to the requested position ourselves.
            skip = position;
            long total = http.getContentLengthLong();
            available = total < 0 ? C.LENGTH_UNSET : total - position;
            if (total >= 0 && available < 0) {
                closeConnection();
                throw new IOException("dashcam_position_out_of_range");
            }
        } else {
            closeConnection();
            throw new IOException("dashcam_http_" + code);
        }

        input = http.getInputStream();
        while (skip > 0) {
            long skipped = input.skip(skip);
            if (skipped <= 0) {
                if (input.read() < 0) {
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
    }
}
