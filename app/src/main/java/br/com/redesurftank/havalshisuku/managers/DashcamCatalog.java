package br.com.redesurftank.havalshisuku.managers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the camera's catalog paths into days, continuous ranges and files.
 *
 * <p>The camera only gives names such as {@code 2026_01_15_100600_00.MP4}. The start time comes from
 * the name and is the camera's local clock. The end of each clip is estimated from the cadence of
 * the files (or 3 minutes when there is no cadence), so the UI must label it as an estimate.
 */
public final class DashcamCatalog {
    static final Duration DEFAULT_CLIP = Duration.ofMinutes(3);
    private static final Duration MAX_CLIP = Duration.ofMinutes(10);
    private static final Pattern NAME = Pattern.compile(
            "^([0-9]{4})_([0-9]{2})_([0-9]{2})_([0-9]{2})([0-9]{2})([0-9]{2})_[0-9A-Za-z]{1,8}\\.(MP4|mp4)$");

    private final List<Recording> recordings;
    private final List<Recording> undated;
    private final Duration clip;
    private final TreeMap<LocalDate, List<Segment>> segmentsByDay;

    private DashcamCatalog(List<Recording> all) {
        List<Recording> dated = new ArrayList<>();
        List<Recording> unknown = new ArrayList<>();
        for (Recording recording : all) {
            if (recording == null) continue;
            if (recording.getStart() == null) unknown.add(recording);
            else dated.add(recording);
        }
        dated.sort(Comparator.comparing(Recording::getStart).thenComparing(Recording::getPath));
        this.recordings = Collections.unmodifiableList(dated);
        this.undated = Collections.unmodifiableList(unknown);
        this.clip = estimateClip(dated);
        this.segmentsByDay = buildSegments(dated, clip);
    }

    /** HiSilicon catalog paths ({@code sd//norm/…MP4}). */
    public static DashcamCatalog fromPaths(List<String> paths) {
        List<Recording> recordings = new ArrayList<>();
        if (paths != null) for (String path : paths) recordings.add(Recording.fromPath(path));
        return new DashcamCatalog(recordings);
    }

    /** Novatek {@code 3015} entries on the camera AP address. */
    public static DashcamCatalog fromNovatek(List<DashcamNovatekProtocol.Entry> entries) {
        return fromNovatek(entries, DashcamNovatekProtocol.HOST);
    }

    /**
     * Novatek {@code 3015} entries served from {@code host} (AP address or the station-network
     * address); their end time is the camera's own, not an estimate.
     */
    public static DashcamCatalog fromNovatek(List<DashcamNovatekProtocol.Entry> entries, String host) {
        String server = host == null ? DashcamNovatekProtocol.HOST : host;
        List<Recording> recordings = new ArrayList<>();
        if (entries != null) for (DashcamNovatekProtocol.Entry entry : entries) {
            recordings.add(Recording.fromNovatek(entry, server));
        }
        return new DashcamCatalog(recordings);
    }

    /** True when some shown end time is an estimate (the UI then says so). */
    public boolean hasEstimatedEnds() {
        for (List<Segment> segments : segmentsByDay.values()) {
            for (Segment segment : segments) if (segment.isEndEstimated()) return true;
        }
        return false;
    }

    public List<Recording> getRecordings() {
        return recordings;
    }

    /** Files whose name did not carry a valid date. They stay visible as "data desconhecida". */
    public List<Recording> getUndated() {
        return undated;
    }

    public Duration getEstimatedClipDuration() {
        return clip;
    }

    public boolean hasRecordingsOn(LocalDate day) {
        return segmentsByDay.containsKey(day);
    }

    public List<LocalDate> getDays() {
        return new ArrayList<>(segmentsByDay.keySet());
    }

    /** Continuous ranges of one day, newest first (as in the gallery list). */
    public List<Segment> getSegments(LocalDate day) {
        List<Segment> segments = segmentsByDay.get(day);
        if (segments == null) return Collections.emptyList();
        List<Segment> newestFirst = new ArrayList<>(segments);
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    /** Most recent day with recordings, or null. */
    public LocalDate getLatestDay() {
        if (segmentsByDay.isEmpty()) return null;
        return segmentsByDay.lastKey();
    }

    static Duration estimateClip(List<Recording> sorted) {
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            Recording previous = sorted.get(i - 1);
            Recording current = sorted.get(i);
            if (!previous.getDir().equals(current.getDir())) continue;
            long seconds = Duration.between(previous.getStart(), current.getStart()).getSeconds();
            if (seconds >= 30 && seconds <= MAX_CLIP.getSeconds()) gaps.add(seconds);
        }
        if (gaps.isEmpty()) return DEFAULT_CLIP;
        Collections.sort(gaps);
        return Duration.ofSeconds(gaps.get(gaps.size() / 2));
    }

    private static TreeMap<LocalDate, List<Segment>> buildSegments(List<Recording> sorted, Duration clip) {
        TreeMap<LocalDate, List<Segment>> byDay = new TreeMap<>();
        long joinLimit = clip.getSeconds() + clip.getSeconds() / 2;
        List<Recording> current = new ArrayList<>();
        for (Recording recording : sorted) {
            if (!current.isEmpty()) {
                Recording last = current.get(current.size() - 1);
                long gap = Duration.between(last.getStart(), recording.getStart()).getSeconds();
                boolean sameDay = last.getStart().toLocalDate().equals(recording.getStart().toLocalDate());
                if (!sameDay || gap > joinLimit) {
                    addSegment(byDay, current, clip);
                    current = new ArrayList<>();
                }
            }
            current.add(recording);
        }
        if (!current.isEmpty()) addSegment(byDay, current, clip);
        return byDay;
    }

    private static void addSegment(
            TreeMap<LocalDate, List<Segment>> byDay, List<Recording> files, Duration clip) {
        List<Clip> clips = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            Recording recording = files.get(i);
            if (recording.getEnd() != null) {
                clips.add(new Clip(recording, recording.getEnd(), false));
                continue;
            }
            LocalDateTime estimatedEnd = recording.getStart().plus(clip);
            if (i + 1 < files.size()) {
                LocalDateTime next = files.get(i + 1).getStart();
                if (next.isBefore(estimatedEnd) && next.isAfter(recording.getStart())) estimatedEnd = next;
            }
            clips.add(new Clip(recording, estimatedEnd, true));
        }
        LocalDate day = files.get(0).getStart().toLocalDate();
        Segment segment = new Segment(
                files.get(0).getStart(), clips.get(clips.size() - 1).getEstimatedEnd(), clips);
        byDay.computeIfAbsent(day, ignored -> new ArrayList<>()).add(segment);
    }

    /** Start time encoded in a camera file name, or null when it is not a valid date. */
    static LocalDateTime parseStart(String name) {
        if (name == null) return null;
        Matcher matcher = NAME.matcher(name);
        if (!matcher.matches()) return null;
        try {
            return LocalDateTime.of(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(4)),
                    Integer.parseInt(matcher.group(5)),
                    Integer.parseInt(matcher.group(6)));
        } catch (RuntimeException invalidDate) {
            return null;
        }
    }

    public static final class Recording {
        private final String path;
        private final String host;
        private final String httpPath;
        private final String dir;
        private final String name;
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final boolean emergency;
        private final long size;

        private Recording(String path, String host, String httpPath, String dir, String name,
                LocalDateTime start, LocalDateTime end, boolean emergency, long size) {
            this.path = path;
            this.host = host;
            this.httpPath = httpPath;
            this.dir = dir;
            this.name = name;
            this.start = start;
            this.end = end;
            this.emergency = emergency;
            this.size = size;
        }

        static Recording fromPath(String path) {
            if (!DashcamHisiliconProtocol.isValidCatalogPath(path)) return null;
            int slash = path.lastIndexOf('/');
            String dir = path.substring("sd//".length(), slash);
            String name = path.substring(slash + 1);
            return new Recording(path, DashcamHisiliconClient.HOST, DashcamHisiliconProtocol.httpPathFor(path),
                    dir, name, parseStart(name), null, "emr".equals(dir), -1L);
        }

        static Recording fromNovatek(DashcamNovatekProtocol.Entry entry, String host) {
            if (entry == null) return null;
            return new Recording(entry.httpPath, host, entry.httpPath, entry.dir,
                    entry.name, entry.start, entry.end, entry.emergency, entry.size);
        }

        /** Unique key of the file in its catalog. */
        public String getPath() {
            return path;
        }

        /**
         * Camera host the file was listed from. Novatek files are fetched from the camera's
         * current host ({@link DashcamVideoDataSource#uriFor}), since the camera can move networks.
         */
        public String getHost() {
            return host;
        }

        /** HTTP path on {@link #getHost()}; already validated for its profile. */
        public String getHttpPath() {
            return httpPath;
        }

        public String getDir() {
            return dir;
        }

        public String getName() {
            return name;
        }

        /** Local camera time; null when the name has no valid date. */
        public LocalDateTime getStart() {
            return start;
        }

        /** End time reported by the camera, or null when only an estimate exists. */
        public LocalDateTime getEnd() {
            return end;
        }

        public boolean isEmergency() {
            return emergency;
        }

        /** Bytes reported by the camera, or -1 when unknown (HiSilicon list has no size). */
        public long getSize() {
            return size;
        }
    }

    public static final class Clip {
        private final Recording recording;
        private final LocalDateTime estimatedEnd;
        private final boolean endEstimated;

        Clip(Recording recording, LocalDateTime estimatedEnd, boolean endEstimated) {
            this.recording = recording;
            this.estimatedEnd = estimatedEnd;
            this.endEstimated = endEstimated;
        }

        /** False when the end came from the camera. */
        public boolean isEndEstimated() {
            return endEstimated;
        }

        public Recording getRecording() {
            return recording;
        }

        public LocalDateTime getEstimatedEnd() {
            return estimatedEnd;
        }
    }

    public static final class Segment {
        private final LocalDateTime start;
        private final LocalDateTime estimatedEnd;
        private final List<Clip> clips;

        Segment(LocalDateTime start, LocalDateTime estimatedEnd, List<Clip> clips) {
            this.start = start;
            this.estimatedEnd = estimatedEnd;
            this.clips = Collections.unmodifiableList(clips);
        }

        public LocalDateTime getStart() {
            return start;
        }

        public LocalDateTime getEstimatedEnd() {
            return estimatedEnd;
        }

        public List<Clip> getClips() {
            return clips;
        }

        public boolean isEndEstimated() {
            for (Clip clip : clips) if (clip.isEndEstimated()) return true;
            return false;
        }

        public boolean hasEmergency() {
            for (Clip clip : clips) if (clip.getRecording().isEmergency()) return true;
            return false;
        }
    }
}
