package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * "Salvar com carimbo": makes {@code <clip>_carimbo.mp4} next to a saved clip, with the stamp
 * (car data, address, mini-map) burned in. The original stays untouched. Data comes from the
 * GPS trail and car signals of the clip's minute; map tiles and the address are fetched now if
 * there is internet, else the cached tiles are used and the address is left out.
 */
public final class DashcamStampJob {
    private static final String TAG = "DashcamStampJob";
    /** OSM zoom of the mini-map. */
    static final int MAP_ZOOM = 16;
    /** Overlay refresh: twice a second keeps the map arrow and the clock smooth enough. */
    private static final long OVERLAY_STEP_US = 500_000L;

    public interface Listener {
        void onProgress(String name, float fraction);

        void onDone(String name, File output);

        void onFailed(String name, String error);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "DashcamStamp"));
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean CANCEL = new AtomicBoolean(false);
    private static volatile String running = "";

    /** Name of the clip being stamped, or "". */
    public static String running() {
        return running;
    }

    public static void cancel() {
        CANCEL.set(true);
    }

    public static boolean isStamped(String name) {
        return name != null && name.contains("_carimbo.");
    }

    public static String stampedName(String clipName) {
        int dot = clipName.lastIndexOf('.');
        return (dot > 0 ? clipName.substring(0, dot) : clipName) + "_carimbo.mp4";
    }

    public static void start(Context context, File clip, Listener listener) {
        Context app = context.getApplicationContext();
        CANCEL.set(false);
        EXECUTOR.execute(() -> run(app, clip, listener));
    }

    private static void run(Context context, File clip, Listener listener) {
        String name = clip.getName();
        running = name;
        File out = new File(clip.getParentFile(), stampedName(name));
        File part = new File(clip.getParentFile(), out.getName() + ".part");
        try {
            Long start = DashcamGpsTrack.clipStartUtcMs(name, ZoneId.systemDefault());
            if (start == null) {
                post(() -> listener.onFailed(name, "nome_invalido"));
                return;
            }
            long from = start - 5_000L;
            long to = start + DashcamGpsTrack.CLIP_MS + 10_000L;
            List<DashcamGpsTrack.Fix> fixes = DashcamGpsTrack.getInstance(context).query(from, to);
            List<DashcamCarTrack.Sample> samples = DashcamCarTrack.getInstance(context).query(from, to);
            if (fixes.isEmpty() && samples.isEmpty()) {
                post(() -> listener.onFailed(name, "sem_dados"));
                return;
            }
            post(() -> listener.onProgress(name, 0f));
            // Address at the start, middle and end of the minute (Nominatim allows 1 request/s).
            List<long[]> addressTimes = new ArrayList<>();
            List<String> addressTexts = new ArrayList<>();
            for (long dt = 0; dt <= DashcamGpsTrack.CLIP_MS; dt += 30_000L) {
                DashcamGpsTrack.Fix f = DashcamGpsTrack.interpolate(fixes, start + dt);
                if (f == null) continue;
                String a = MapTiles.INSTANCE.address(context, f.lat, f.lon);
                if (a != null) {
                    addressTimes.add(new long[]{start + dt});
                    addressTexts.add(a);
                }
            }
            StampMap map = StampMap.build(context, fixes);
            DashcamStampPainter painter = new DashcamStampPainter();
            final long clipStart = start;
            final long[] lastBucket = {-1};
            // Two bitmaps used in turn: the encoder re-uploads whenever it gets a different object.
            final Bitmap[] buffers = new Bitmap[2];
            final int[] turn = {0};
            final Bitmap[] last = {null};
            DashcamStampEncoder.Overlay source = (ptsUs, width, height) -> {
                long bucket = ptsUs / OVERLAY_STEP_US;
                if (bucket == lastBucket[0] && last[0] != null) return last[0];
                lastBucket[0] = bucket;
                turn[0] ^= 1;
                Bitmap target = buffers[turn[0]];
                if (target == null || target.getWidth() != width || target.getHeight() != height) {
                    target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    buffers[turn[0]] = target;
                }
                target.eraseColor(0);
                long t = clipStart + ptsUs / 1000L;
                DashcamStampFrame frame = DashcamStampFrame.build(t, fixes, samples);
                String address = nearest(addressTimes, addressTexts, t);
                painter.paint(new Canvas(target), width, height, frame, address, map, fixes);
                last[0] = target;
                return target;
            };
            long t0 = System.currentTimeMillis();
            String error = DashcamStampEncoder.encode(clip, part, source, fraction -> {
                post(() -> listener.onProgress(name, fraction));
                return !CANCEL.get();
            });
            if (error != null) {
                part.delete();
                String e = error;
                post(() -> listener.onFailed(name, e));
                return;
            }
            if ((out.exists() && !out.delete()) || !part.renameTo(out)) {
                part.delete();
                post(() -> listener.onFailed(name, "rename_failed"));
                return;
            }
            Log.i(TAG, "stamped " + out.getName() + " ms=" + (System.currentTimeMillis() - t0) + " bytes=" + out.length());
            post(() -> listener.onDone(name, out));
        } catch (RuntimeException | OutOfMemoryError e) {
            Log.w(TAG, "stamp failed", e);
            part.delete();
            post(() -> listener.onFailed(name, "erro_" + e.getClass().getSimpleName()));
        } finally {
            running = "";
        }
    }

    private static String nearest(List<long[]> times, List<String> texts, long t) {
        String best = null;
        long bestDt = Long.MAX_VALUE;
        for (int i = 0; i < times.size(); i++) {
            long dt = Math.abs(times.get(i)[0] - t);
            if (dt < bestDt) {
                bestDt = dt;
                best = texts.get(i);
            }
        }
        return best;
    }

    private static void post(Runnable r) {
        MAIN.post(r);
    }

    /** Map tiles stitched around the clip's trail at {@link #MAP_ZOOM}; null when there is no trail. */
    public static final class StampMap {
        public final Bitmap bitmap;
        /** World pixel (at MAP_ZOOM) of the bitmap's top-left corner. */
        public final double originX;
        public final double originY;

        StampMap(Bitmap bitmap, double originX, double originY) {
            this.bitmap = bitmap;
            this.originX = originX;
            this.originY = originY;
        }

        public float x(double lon) {
            return (float) (MapTiles.worldX(lon, MAP_ZOOM) - originX);
        }

        public float y(double lat) {
            return (float) (MapTiles.worldY(lat, MAP_ZOOM) - originY);
        }

        static StampMap build(Context context, List<DashcamGpsTrack.Fix> fixes) {
            if (fixes.isEmpty()) return null;
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (DashcamGpsTrack.Fix f : fixes) {
                double x = MapTiles.worldX(f.lon, MAP_ZOOM);
                double y = MapTiles.worldY(f.lat, MAP_ZOOM);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
            // Margin so the car can sit in the middle of the mini-map at both ends of the minute.
            int margin = 320;
            int tx0 = (int) Math.floor((minX - margin) / MapTiles.TILE);
            int ty0 = (int) Math.floor((minY - margin) / MapTiles.TILE);
            int tx1 = (int) Math.floor((maxX + margin) / MapTiles.TILE);
            int ty1 = (int) Math.floor((maxY + margin) / MapTiles.TILE);
            // A minute at highway speed is ~2 km: at z16 that is ~9 tiles. Cap the bitmap anyway.
            if ((tx1 - tx0 + 1) * (ty1 - ty0 + 1) > 120) return null;
            Bitmap bitmap = Bitmap.createBitmap((tx1 - tx0 + 1) * MapTiles.TILE, (ty1 - ty0 + 1) * MapTiles.TILE,
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(0xFF2A2F36);
            boolean any = false;
            for (int tx = tx0; tx <= tx1; tx++) {
                for (int ty = ty0; ty <= ty1; ty++) {
                    Bitmap tile = MapTiles.INSTANCE.load(context, MAP_ZOOM, tx, ty, true);
                    if (tile == null) continue;
                    any = true;
                    int left = (tx - tx0) * MapTiles.TILE;
                    int top = (ty - ty0) * MapTiles.TILE;
                    canvas.drawBitmap(tile, null, new Rect(left, top, left + MapTiles.TILE, top + MapTiles.TILE), null);
                }
            }
            if (!any) Log.i(TAG, "mini-map without tiles (offline, nothing cached)");
            return new StampMap(bitmap, (double) tx0 * MapTiles.TILE, (double) ty0 * MapTiles.TILE);
        }
    }
}
