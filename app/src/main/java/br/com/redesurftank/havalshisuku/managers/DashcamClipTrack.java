package br.com.redesurftank.havalshisuku.managers;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The GPS fixes and car signals of a saved clip's minute, kept next to the clip as
 * {@code <clip>.track}. The live trails are deleted after 72 h; with this file a clip saved today
 * can still be stamped next week. Plain text: a {@code #gps} section in {@link DashcamGpsTrack}'s
 * line format and a {@code #car} section in {@link DashcamCarTrack}'s (header plus lines).
 */
public final class DashcamClipTrack {
    private static final String TAG = "DashcamClipTrack";
    private static final String MAGIC = "#impulse-clip-track v1";

    private DashcamClipTrack() {}

    public static final class Data {
        public final List<DashcamGpsTrack.Fix> fixes;
        public final List<DashcamCarTrack.Sample> samples;

        Data(List<DashcamGpsTrack.Fix> fixes, List<DashcamCarTrack.Sample> samples) {
            this.fixes = fixes;
            this.samples = samples;
        }

        public boolean isEmpty() {
            return fixes.isEmpty() && samples.isEmpty();
        }
    }

    static String fileName(String clipName) {
        int dot = clipName.lastIndexOf('.');
        String base = dot > 0 ? clipName.substring(0, dot) : clipName;
        // A stamped copy shares its original's data.
        if (base.endsWith("_carimbo")) base = base.substring(0, base.length() - "_carimbo".length());
        return base + ".track";
    }

    public static File fileFor(File clip) {
        return new File(clip.getParentFile(), fileName(clip.getName()));
    }

    /** Window of a clip's data: the minute plus the same margins the stamp uses. */
    static long[] window(String clipName) {
        Long start = DashcamGpsTrack.clipStartUtcMs(clipName, java.time.ZoneId.systemDefault());
        if (start == null) return null;
        return new long[]{start - 5_000L, start + DashcamGpsTrack.CLIP_MS + 10_000L};
    }

    /** Copies the clip's minute from the live trails into {@code <clip>.track}; false when there is none. */
    public static boolean snapshot(android.content.Context context, File clip) {
        long[] w = window(clip.getName());
        if (w == null) return false;
        List<DashcamGpsTrack.Fix> fixes = DashcamGpsTrack.getInstance(context).query(w[0], w[1]);
        List<DashcamCarTrack.Sample> samples = DashcamCarTrack.getInstance(context).query(w[0], w[1]);
        if (fixes.isEmpty() && samples.isEmpty()) return false;
        File out = fileFor(clip);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            writer.write(format(fixes, samples));
            return true;
        } catch (IOException e) {
            Log.w(TAG, "snapshot failed for " + clip.getName(), e);
            return false;
        }
    }

    /** The data kept with a saved clip, or null when it has no {@code .track} file. */
    public static Data read(File clip) {
        File in = fileFor(clip);
        if (!in.isFile()) return null;
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
        } catch (IOException e) {
            Log.w(TAG, "read failed for " + clip.getName(), e);
            return null;
        }
        return parse(text.toString());
    }

    // ---- pure helpers (unit tested) ----

    static String format(List<DashcamGpsTrack.Fix> fixes, List<DashcamCarTrack.Sample> samples) {
        StringBuilder sb = new StringBuilder(MAGIC).append('\n').append("#gps\n");
        for (DashcamGpsTrack.Fix fix : fixes) sb.append(DashcamGpsTrack.formatLine(fix)).append('\n');
        String header = DashcamCarTrack.header();
        String[] columns = header.substring("#keys ".length()).split(",");
        sb.append("#car\n").append(header).append('\n');
        for (DashcamCarTrack.Sample sample : samples) {
            sb.append(sample.utcMs);
            for (String column : columns) sb.append('|').append(DashcamCarTrack.clean(sample.values.get(column)));
            sb.append('\n');
        }
        return sb.toString();
    }

    static Data parse(String text) {
        List<DashcamGpsTrack.Fix> fixes = new ArrayList<>();
        List<DashcamCarTrack.Sample> samples = new ArrayList<>();
        if (text == null || !text.startsWith(MAGIC)) return new Data(fixes, samples);
        String section = "";
        String[] columns = null;
        for (String line : text.split("\n")) {
            if (line.isEmpty() || line.equals(MAGIC)) continue;
            if (line.equals("#gps") || line.equals("#car")) {
                section = line;
                continue;
            }
            if (section.equals("#gps")) {
                DashcamGpsTrack.Fix fix = DashcamGpsTrack.parseLine(line);
                if (fix != null) fixes.add(fix);
            } else if (section.equals("#car")) {
                if (line.startsWith("#keys ")) {
                    columns = line.substring("#keys ".length()).split(",");
                    continue;
                }
                DashcamCarTrack.Sample sample = DashcamCarTrack.parseLine(line, columns);
                if (sample != null) samples.add(sample);
            }
        }
        return new Data(fixes, samples);
    }
}
