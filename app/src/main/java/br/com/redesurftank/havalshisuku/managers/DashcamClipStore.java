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
    /**
     * Free space left after a save. Android refuses app updates (OTA) below ~500 MB, so keep a
     * little above that; 1 GB refused clips with 1.1 GB free (2026-11-22).
     */
    static final long FREE_SPACE_MARGIN = 600L * 1024 * 1024;
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
        boolean deleted = file != null && file.getParentFile() != null
                && file.getParentFile().equals(directory()) && file.delete();
        if (deleted) {
            // Sidecars are shared by the original and its stamped copy: remove them with the last one.
            boolean lastOfPair = !hasPartner(file);
            if (lastOfPair) {
                File gpx = new File(file.getParentFile(), gpxName(file.getName()));
                if (gpx.isFile() && !gpx.delete()) Log.w(TAG, "cannot delete " + gpx);
                File ev = new File(file.getParentFile(), eventName(file.getName()));
                if (ev.isFile() && !ev.delete()) Log.w(TAG, "cannot delete " + ev);
            }
            File sha = new File(file.getParentFile(), file.getName() + ".sha256");
            if (sha.isFile() && !sha.delete()) Log.w(TAG, "cannot delete " + sha);
            if (lastOfPair) {
                File track = DashcamClipTrack.fileFor(file);
                if (track.isFile() && !track.delete()) Log.w(TAG, "cannot delete " + track);
            }
        }
        return deleted;
    }

    /** The other half of an original / stamped pair is still saved. */
    static boolean hasPartner(File file) {
        File dir = file.getParentFile();
        String name = file.getName();
        if (isStampedName(name)) {
            String base = name.replace("_carimbo.mp4", "");
            return new File(dir, base + ".mp4").exists() || new File(dir, base + ".TS").exists()
                    || new File(dir, base + ".ts").exists();
        }
        int dot = name.lastIndexOf('.');
        return new File(dir, (dot > 0 ? name.substring(0, dot) : name) + "_carimbo.mp4").exists();
    }

    /** Same rule as DashcamStampJob.isStamped, without loading that class (it needs a Looper). */
    static boolean isStampedName(String name) {
        return name != null && name.contains("_carimbo.");
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
        boolean novatek = DashcamNovatekProtocol.isValidHttpPath(recording.getHttpPath());
        // Novatek: the camera's current host (AP or station network), not the one it was listed on.
        String host = novatek ? DashcamVideoDataSource.currentNovatekHost(context) : recording.getHost();
        if (novatek && host == null) {
            post(() -> listener.onFailed(name, "camera_not_found"));
            return;
        }
        if (!isClipName(name)
                || !DashcamVideoDataSource.isAllowed(host, recording.getHttpPath(), novatek ? host : null)) {
            post(() -> listener.onFailed(name, "invalid_path"));
            return;
        }
        Network network = DashcamHisiliconClient.findCameraNetwork(context);
        if (network == null) {
            post(() -> listener.onFailed(name, "wifi_unavailable"));
            return;
        }
        HttpURLConnection http = null;
        DashcamCameraHttp.Response raw = null;
        try {
            int code;
            long length;
            if (novatek) {
                // Raw socket: a station-network camera (10.x…) is outside the cleartext allow-list.
                try {
                    raw = DashcamCameraHttp.get(network, host, recording.getHttpPath(), 0, -1,
                            CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                } catch (java.io.IOException e) {
                    if (!"dashcam_redirect_refused".equals(e.getMessage())) DashcamCameraHost.reportFailure(host);
                    throw e;
                }
                code = raw.head.statusCode;
                length = raw.contentLength;
            } else {
                URL url = new URL("http", host, DashcamHisiliconClient.PORT, recording.getHttpPath());
                http = (HttpURLConnection) network.openConnection(url);
                http.setInstanceFollowRedirects(false);
                http.setConnectTimeout(CONNECT_TIMEOUT_MS);
                http.setReadTimeout(READ_TIMEOUT_MS);
                http.setUseCaches(false);
                http.setRequestProperty("Accept-Encoding", "identity");
                code = http.getResponseCode();
                length = code == HttpURLConnection.HTTP_OK ? http.getContentLengthLong() : -1;
            }
            if (code != HttpURLConnection.HTTP_OK) {
                fail(listener, name, part, "http_" + code);
                return;
            }
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
            try (InputStream input = raw != null ? raw.body : http.getInputStream();
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
            if (recording.isEmergency()) markEvent(target);
            List<DashcamGpsTrack.Fix> trail = trailFor(name);
            writeGpx(target, trail);
            // The minute's GPS and car data travel with the clip, so it can be stamped after 72 h.
            DashcamClipTrack.snapshot(context, target);
            if (isTs(name)) {
                saving = "converting";
                post(() -> listener.onProgress(name, PROGRESS_CONVERTING));
                File mp4 = convertToMp4(target, trail.isEmpty() ? null : trail.get(0));
                File result = mp4 != null ? mp4 : target;
                DashcamAutoStamp.enqueue(context, result);
                post(() -> listener.onDone(name, result));
            } else {
                DashcamAutoStamp.enqueue(context, target);
                post(() -> listener.onDone(name, target));
            }
        } catch (Exception e) {
            fail(listener, name, part, "transport_" + e.getClass().getSimpleName());
        } finally {
            saving = "";
            if (http != null) http.disconnect();
            if (raw != null) raw.close();
        }
    }

    private static boolean isTs(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".ts");
    }

    /** Remuxes [ts] to an .mp4 next to it; deletes the .TS on success. Null when it fails. */
    private File convertToMp4(File ts, DashcamGpsTrack.Fix location) {
        File mp4 = new File(ts.getParentFile(), mp4Name(ts.getName()));
        File part = new File(ts.getParentFile(), mp4.getName() + ".part");
        long start = System.currentTimeMillis();
        boolean ok = remuxVideoOnly(ts, part, location);
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
    /** The clip's GPS trail, when recorded (empty otherwise). */
    private List<DashcamGpsTrack.Fix> trailFor(String clipName) {
        try {
            return DashcamGpsTrack.getInstance(context).forClip(clipName);
        } catch (RuntimeException e) {
            Log.w(TAG, "trail unavailable for " + clipName, e);
            return Collections.emptyList();
        }
    }

    /** {@code <clip>.event}: the camera flagged it (emergency / G-sensor); the stamp shows a badge. */
    static String eventName(String clipName) {
        String base = clipName.replace("_carimbo.", ".");
        int dot = base.lastIndexOf('.');
        return (dot > 0 ? base.substring(0, dot) : base) + ".event";
    }

    private void markEvent(File clip) {
        try {
            if (!new File(clip.getParentFile(), eventName(clip.getName())).createNewFile()) Log.i(TAG, "event mark exists");
        } catch (java.io.IOException e) {
            Log.w(TAG, "event mark failed", e);
        }
    }

    public static boolean isEvent(File clip) {
        return new File(clip.getParentFile(), eventName(clip.getName())).isFile();
    }

    static String gpxName(String clipName) {
        // A stamped copy shares its original's trail (the original may have been removed).
        String base = clipName.replace("_carimbo.", ".");
        int dot = base.lastIndexOf('.');
        return (dot > 0 ? base.substring(0, dot) : base) + ".gpx";
    }

    /**
     * Owner (2026-11-25): once the stamped copy is ready, keep only it. Removes the original clip and
     * its hash file; the trail, event mark and kept data stay, shared with the stamped copy.
     */
    public static boolean removeOriginalAfterStamp(File original, File stamped) {
        if (original == null || stamped == null || !stamped.isFile() || stamped.length() <= 0
                || isStampedName(original.getName())) return false;
        boolean removed = !original.exists() || original.delete();
        File sha = new File(original.getParentFile(), original.getName() + ".sha256");
        if (sha.isFile() && !sha.delete()) Log.w(TAG, "cannot delete " + sha);
        return removed;
    }

    /** {@code <clip>.gpx} next to the saved clip (same base name for .TS and .mp4). */
    private void writeGpx(File clip, List<DashcamGpsTrack.Fix> trail) {
        if (trail == null || trail.isEmpty()) return;
        File gpx = new File(clip.getParentFile(), gpxName(clip.getName()));
        try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(gpx),
                java.nio.charset.StandardCharsets.UTF_8)) {
            w.write(DashcamGpsTrack.gpx(trail, clip.getName()));
        } catch (java.io.IOException e) {
            Log.w(TAG, "gpx write failed for " + clip.getName(), e);
        }
    }

    /** Saved trail of a saved clip ({@code <clip>.gpx}), read back as fixes; empty when absent. */
    public File gpxFor(File clip) {
        File gpx = new File(clip.getParentFile(), gpxName(clip.getName()));
        return gpx.isFile() ? gpx : null;
    }

    static boolean remuxVideoOnly(File input, File output) {
        return remuxVideoOnly(input, output, null);
    }

    /**
     * Copies the first video track of [input] into an MP4 container without re-encoding. With a
     * [location], the MP4 carries it (ISO 6709), so the phone's gallery shows the clip on the map.
     */
    static boolean remuxVideoOnly(File input, File output, DashcamGpsTrack.Fix location) {
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
            if (location != null) muxer.setLocation((float) location.lat, (float) location.lon);
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
