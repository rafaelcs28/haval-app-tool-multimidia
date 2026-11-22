package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * "Salvar no carro": copies camera clips to the head unit so they can be watched without the
 * camera and sent to a phone over a faster network ({@link DashcamShareServer}).
 *
 * <p>Downloads run one at a time over the camera Wi-Fi (same validated host/path rules as the
 * player), land in {@code <name>.part} and are renamed only after the byte count matches the
 * camera's Content-Length. A margin of free space is kept on the head unit.
 *
 * <p>Novatek clips are MPEG-TS, which phone galleries do not import. A saved {@code .TS} is
 * remuxed to {@code .mp4} (video track only, no re-encode, so no quality loss and a few seconds);
 * the user does not record audio. If the remux fails the {@code .TS} is kept.
 */
public final class DashcamClipStore {
    private static final String TAG = "DashcamClipStore";
    private static final String DIR = "Dashcam";
    /**
     * Android refuses installs (and starts failing) near its low-storage threshold (~500 MB on
     * this head unit's 10 GB /data), so saving never leaves less than 1 GB free.
     */
    private static final long FREE_SPACE_MARGIN = 1024L * 1024 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private static volatile DashcamClipStore instance;

    public interface Listener {
        /** Progress 0..1 (or -1 when the size is unknown) for the clip being saved. */
        void onProgress(String name, float progress);

        void onDone(String name, File file);

        void onFailed(String name, String error);
    }

    /** "" idle, "NN" percent while downloading, "converting" while remuxing (for the 3D card). */
    private static volatile String saving = "";

    public static String savingStatus() {
        return saving;
    }

    /** Progress value meaning "converting to MP4" (download finished). */
    public static final float PROGRESS_CONVERTING = -2f;

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "DashcamClipStore");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    private DashcamClipStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public static DashcamClipStore getInstance(Context context) {
        DashcamClipStore local = instance;
        if (local == null) {
            synchronized (DashcamClipStore.class) {
                local = instance;
                if (local == null) {
                    local = new DashcamClipStore(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    public File directory() {
        File dir = context.getExternalFilesDir(DIR);
        if (dir == null) dir = new File(context.getFilesDir(), DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) Log.w(TAG, "cannot create " + dir);
        return dir;
    }

    /** Saved clips, newest name first; unfinished {@code .part} files are left out. */
    public List<File> list() {
        File[] files = directory().listFiles((dir, name) -> isClipName(name));
        if (files == null) return Collections.emptyList();
        List<File> sorted = new ArrayList<>(Arrays.asList(files));
        sorted.sort((a, b) -> b.getName().compareTo(a.getName()));
        return sorted;
    }

    public boolean isSaved(DashcamCatalog.Recording recording) {
        return new File(directory(), recording.getName()).isFile()
                || new File(directory(), mp4Name(recording.getName())).isFile();
    }

    static String mp4Name(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name) + ".mp4";
    }

    public boolean delete(File file) {
        return file != null && file.getParentFile() != null
                && file.getParentFile().equals(directory()) && file.delete();
    }

    public void cancel() {
        cancelRequested.set(true);
    }

    /** Queues the clips in order; each reports through the listener on the main thread. */
    public void save(List<DashcamCatalog.Recording> recordings, Listener listener) {
        cancelRequested.set(false);
        for (DashcamCatalog.Recording recording : recordings) {
            executor.execute(() -> {
                if (cancelRequested.get()) {
                    post(() -> listener.onFailed(recording.getName(), "cancelled"));
                    return;
                }
                download(recording, listener);
            });
        }
    }

    private void download(DashcamCatalog.Recording recording, Listener listener) {
        String name = recording.getName();
        File dir = directory();
        File target = new File(dir, name);
        File part = new File(dir, name + ".part");
        if (!isClipName(name)
                || !DashcamVideoDataSource.isAllowed(recording.getHost(), recording.getHttpPath())) {
            post(() -> listener.onFailed(name, "invalid_path"));
            return;
        }
        Network network = DashcamHisiliconClient.findCameraNetwork(context);
        if (network == null) {
            post(() -> listener.onFailed(name, "wifi_unavailable"));
            return;
        }
        HttpURLConnection http = null;
        try {
            URL url = new URL("http", recording.getHost(), DashcamHisiliconClient.PORT, recording.getHttpPath());
            http = (HttpURLConnection) network.openConnection(url);
            http.setInstanceFollowRedirects(false);
            http.setConnectTimeout(CONNECT_TIMEOUT_MS);
            http.setReadTimeout(READ_TIMEOUT_MS);
            http.setUseCaches(false);
            http.setRequestProperty("Accept-Encoding", "identity");
            int code = http.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                fail(listener, name, part, "http_" + code);
                return;
            }
            long length = http.getContentLengthLong();
            if (target.isFile() && length > 0 && target.length() == length) {
                post(() -> listener.onDone(name, target));
                return;
            }
            // A .TS needs room for itself and its MP4 copy until the remux finishes.
            long base = length > 0 ? length : 200L * 1024 * 1024;
            long needed = (isTs(name) ? base * 2 : base) + FREE_SPACE_MARGIN;
            if (dir.getUsableSpace() < needed) {
                fail(listener, name, part, "no_space");
                return;
            }
            long written = 0;
            long lastReport = 0;
            try (InputStream input = http.getInputStream();
                    FileOutputStream output = new FileOutputStream(part)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (cancelRequested.get()) {
                        fail(listener, name, part, "cancelled");
                        return;
                    }
                    output.write(buffer, 0, read);
                    written += read;
                    long now = System.currentTimeMillis();
                    if (now - lastReport > 400) {
                        lastReport = now;
                        float progress = length > 0 ? (float) written / length : -1f;
                        saving = progress >= 0 ? String.valueOf((int) (progress * 100)) : "0";
                        post(() -> listener.onProgress(name, progress));
                    }
                }
                output.getFD().sync();
            }
            if (length > 0 && written != length) {
                fail(listener, name, part, "truncated");
                return;
            }
            if (target.exists() && !target.delete()) {
                fail(listener, name, part, "replace_failed");
                return;
            }
            if (!part.renameTo(target)) {
                fail(listener, name, part, "rename_failed");
                return;
            }
            Log.i(TAG, "saved " + name + " bytes=" + written);
            if (isTs(name)) {
                saving = "converting";
                post(() -> listener.onProgress(name, PROGRESS_CONVERTING));
                File mp4 = convertToMp4(target);
                File result = mp4 != null ? mp4 : target;
                post(() -> listener.onDone(name, result));
            } else {
                post(() -> listener.onDone(name, target));
            }
        } catch (Exception e) {
            fail(listener, name, part, "transport_" + e.getClass().getSimpleName());
        } finally {
            saving = "";
            if (http != null) http.disconnect();
        }
    }

    private static boolean isTs(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".ts");
    }

    /** Remuxes [ts] to an .mp4 next to it; deletes the .TS on success. Null when it fails. */
    private File convertToMp4(File ts) {
        File mp4 = new File(ts.getParentFile(), mp4Name(ts.getName()));
        File part = new File(ts.getParentFile(), mp4.getName() + ".part");
        long start = System.currentTimeMillis();
        boolean ok = remuxVideoOnly(ts, part);
        if (ok && part.length() > 0 && (!mp4.exists() || mp4.delete()) && part.renameTo(mp4)) {
            if (!ts.delete()) Log.w(TAG, "cannot delete " + ts);
            Log.i(TAG, "mp4 " + mp4.getName() + " bytes=" + mp4.length()
                    + " ms=" + (System.currentTimeMillis() - start));
            return mp4;
        }
        if (part.exists() && !part.delete()) Log.w(TAG, "cannot delete " + part);
        Log.w(TAG, "mp4 conversion failed, keeping " + ts.getName());
        return null;
    }

    /** Copies the first video track of [input] into an MP4 container without re-encoding. */
    static boolean remuxVideoOnly(File input, File output) {
        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;
        boolean started = false;
        try {
            extractor.setDataSource(input.getAbsolutePath());
            int videoTrack = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat candidate = extractor.getTrackFormat(i);
                String mime = candidate.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    videoTrack = i;
                    format = candidate;
                    break;
                }
            }
            if (videoTrack < 0) return false;
            extractor.selectTrack(videoTrack);
            muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int outTrack = muxer.addTrack(format);
            muxer.start();
            started = true;
            int capacity = format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)
                    ? Math.max(format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE), 1024 * 1024)
                    : 8 * 1024 * 1024;
            ByteBuffer buffer = ByteBuffer.allocate(capacity);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long firstPts = -1;
            boolean sawKeyFrame = false;
            int samples = 0;
            while (true) {
                buffer.clear();
                int size = extractor.readSampleData(buffer, 0);
                if (size < 0) break;
                boolean key = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
                long pts = extractor.getSampleTime();
                // Start at the first key frame so the MP4 does not open on broken frames.
                if (!sawKeyFrame && !key) {
                    extractor.advance();
                    continue;
                }
                sawKeyFrame = true;
                if (firstPts < 0) firstPts = pts;
                info.offset = 0;
                info.size = size;
                info.presentationTimeUs = Math.max(0, pts - firstPts);
                info.flags = key ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
                muxer.writeSampleData(outTrack, buffer, info);
                samples++;
                extractor.advance();
            }
            return samples > 0;
        } catch (Exception e) {
            Log.w(TAG, "remux failed", e);
            return false;
        } finally {
            try {
                if (muxer != null) {
                    if (started) muxer.stop();
                    muxer.release();
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "muxer close failed", e);
            }
            extractor.release();
        }
    }

    private void fail(Listener listener, String name, File part, String error) {
        if (part.exists() && !part.delete()) Log.w(TAG, "cannot delete " + part);
        Log.w(TAG, "save " + name + " failed: " + error);
        post(() -> listener.onFailed(name, error));
    }

    private void post(Runnable runnable) {
        main.post(runnable);
    }

    static boolean isClipName(String name) {
        return name != null && name.matches("[0-9A-Za-z_]{1,64}\\.(?:TS|ts|MP4|mp4|MOV|mov)");
    }
}
