package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
        return new File(directory(), recording.getName()).isFile();
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
            long needed = (length > 0 ? length : 200L * 1024 * 1024) + FREE_SPACE_MARGIN;
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
            post(() -> listener.onDone(name, target));
        } catch (Exception e) {
            fail(listener, name, part, "transport_" + e.getClass().getSimpleName());
        } finally {
            if (http != null) http.disconnect();
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
