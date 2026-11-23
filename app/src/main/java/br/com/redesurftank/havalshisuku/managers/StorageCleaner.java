package br.com.redesurftank.havalshisuku.managers;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import br.com.redesurftank.havalshisuku.utils.ShizukuUtils;

/**
 * "Limpar armazenamento" (Recursos): finds large files the head unit accumulates (old APKs,
 * dashcam clips, debug logs) and deletes only what the user picks.
 *
 * <p>Only two roots are ever touched: shared storage ({@code /sdcard}) and the adb scratch folder
 * {@code /data/local/tmp}. Every path is checked against them before deletion, symlinks are not
 * followed and nothing outside them is listed or removed. Shared storage is scanned with plain
 * file APIs; {@code /data/local/tmp} (owned by shell/root) through Shizuku when it is available.
 */
public final class StorageCleaner {
    private static final String TAG = "StorageCleaner";
    public static final long MIN_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_ENTRIES = 300;
    private static final int MAX_DEPTH = 10;
    static final String SHARED_ROOT = "/storage/emulated/0";
    static final String TMP_ROOT = "/data/local/tmp";

    private StorageCleaner() {}

    public static final class Entry {
        public final String path;
        public final long size;
        public final long modifiedMs;
        public final boolean viaShizuku;

        Entry(String path, long size, long modifiedMs, boolean viaShizuku) {
            this.path = path;
            this.size = size;
            this.modifiedMs = modifiedMs;
            this.viaShizuku = viaShizuku;
        }

        public String getName() {
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(slash + 1) : path;
        }

        public String getFolder() {
            int slash = path.lastIndexOf('/');
            return slash > 0 ? path.substring(0, slash) : "/";
        }

        public String getCategory() {
            return category(getName());
        }
    }

    public static final class Result {
        public final int deleted;
        public final long freedBytes;
        public final List<String> failed;

        Result(int deleted, long freedBytes, List<String> failed) {
            this.deleted = deleted;
            this.freedBytes = freedBytes;
            this.failed = failed;
        }
    }

    /** Free bytes on the internal data partition (apps, shared storage and the tmp folder). */
    public static long freeBytes() {
        try {
            return new StatFs(Environment.getDataDirectory().getPath()).getAvailableBytes();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public static long totalBytes() {
        try {
            return new StatFs(Environment.getDataDirectory().getPath()).getTotalBytes();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** Large files in both roots, biggest first. Call off the main thread. */
    public static List<Entry> scan() {
        List<Entry> entries = new ArrayList<>();
        walk(new File(SHARED_ROOT), 0, entries);
        // The Download folder is listed whole (any size): files there pile up and other apps
        // rarely offer a way to remove them.
        walkAll(new File(SHARED_ROOT, "Download"), 0, entries);
        if (ShizukuUtils.isShizukuAvailable()) {
            String out = ShizukuUtils.runCommandAndGetOutput(new String[] {
                    "sh", "-c", "for f in " + TMP_ROOT + "/* " + TMP_ROOT + "/.[!.]*; do "
                            + "[ -f \"$f\" ] && [ ! -L \"$f\" ] && stat -c '%s|%Y|%n' \"$f\"; done"});
            entries.addAll(parseStatLines(out, MIN_SIZE_BYTES));
        }
        entries.sort((a, b) -> Long.compare(b.size, a.size));
        return entries.size() > MAX_ENTRIES ? new ArrayList<>(entries.subList(0, MAX_ENTRIES)) : entries;
    }

    private static void walk(File dir, int depth, List<Entry> out) {
        if (depth > MAX_DEPTH) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            try {
                // Skip symlinks: a link could point outside the allowed roots.
                if (!child.getCanonicalPath().equals(child.getAbsolutePath())) continue;
            } catch (Exception e) {
                continue;
            }
            if (child.isDirectory()) {
                walk(child, depth + 1, out);
            } else if (child.isFile() && child.length() >= MIN_SIZE_BYTES && !isInDownload(child)) {
                out.add(new Entry(child.getAbsolutePath(), child.length(), child.lastModified(), false));
            }
        }
    }

    private static boolean isInDownload(File file) {
        return file.getAbsolutePath().startsWith(SHARED_ROOT + "/Download/");
    }

    /** Every regular file under {@code dir}, whatever its size (used for Download). */
    private static void walkAll(File dir, int depth, List<Entry> out) {
        if (depth > MAX_DEPTH) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            try {
                if (!child.getCanonicalPath().equals(child.getAbsolutePath())) continue;
            } catch (Exception e) {
                continue;
            }
            if (child.isDirectory()) {
                walkAll(child, depth + 1, out);
            } else if (child.isFile()) {
                out.add(new Entry(child.getAbsolutePath(), child.length(), child.lastModified(), false));
            }
        }
    }

    /** {@code size|mtime_seconds|path} lines from {@code stat}; only allowed, large files. */
    static List<Entry> parseStatLines(String output, long minSize) {
        if (output == null || output.isEmpty()) return Collections.emptyList();
        List<Entry> entries = new ArrayList<>();
        for (String line : output.split("\n")) {
            String[] parts = line.trim().split("\\|", 3);
            if (parts.length != 3) continue;
            try {
                long size = Long.parseLong(parts[0]);
                long mtime = Long.parseLong(parts[1]) * 1000L;
                String path = parts[2];
                if (size >= minSize && isAllowedPath(path) && path.startsWith(TMP_ROOT + "/")) {
                    entries.add(new Entry(path, size, mtime, true));
                }
            } catch (NumberFormatException ignored) {
                // not a stat line
            }
        }
        return entries;
    }

    /** Deletes the chosen entries, re-checking each path; returns what was freed. */
    public static Result delete(List<Entry> chosen) {
        int deleted = 0;
        long freed = 0;
        List<String> failed = new ArrayList<>();
        for (Entry entry : chosen) {
            if (!isAllowedPath(entry.path)) {
                failed.add(entry.getName());
                continue;
            }
            boolean ok;
            if (entry.viaShizuku) {
                ok = deleteWithShizuku(entry.path);
            } else {
                File file = new File(entry.path);
                ok = file.delete() || (!file.exists());
                if (!ok && ShizukuUtils.isShizukuAvailable()) ok = deleteWithShizuku(entry.path);
            }
            if (ok) {
                deleted++;
                freed += entry.size;
            } else {
                failed.add(entry.getName());
            }
        }
        Log.i(TAG, "deleted=" + deleted + " freed=" + freed + " failed=" + failed.size());
        return new Result(deleted, freed, failed);
    }

    private static boolean deleteWithShizuku(String path) {
        ShizukuUtils.runCommandAndGetOutput(new String[] {"rm", "-f", "--", path});
        String check = ShizukuUtils.runCommandAndGetOutput(new String[] {
                "sh", "-c", "[ -e \"$1\" ] && echo present || echo gone", "sh", path});
        return check != null && check.trim().endsWith("gone");
    }

    /**
     * True only for a plain absolute path inside one of the two roots: no "..", no "//" tricks,
     * no control characters, and not the root folder itself.
     */
    static boolean isAllowedPath(String path) {
        if (path == null || path.isEmpty() || !path.startsWith("/")) return false;
        if (path.contains("/../") || path.endsWith("/..") || path.contains("/./") || path.contains("//")) {
            return false;
        }
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) < 0x20 || path.charAt(i) == 0x7f) return false;
        }
        String[] roots = {SHARED_ROOT, "/sdcard", TMP_ROOT};
        for (String root : roots) {
            if (path.startsWith(root + "/") && path.length() > root.length() + 1) return true;
        }
        return false;
    }

    static String category(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".apk")) return "Instalador (APK)";
        if (lower.endsWith(".ts") || lower.endsWith(".mp4") || lower.endsWith(".mov")
                || lower.endsWith(".mkv")) return "Vídeo";
        if (lower.endsWith(".log") || lower.endsWith(".txt") || lower.endsWith(".trace")) return "Log";
        if (lower.endsWith(".zip") || lower.endsWith(".tar") || lower.endsWith(".gz")) return "Compactado";
        return "Arquivo";
    }
}
