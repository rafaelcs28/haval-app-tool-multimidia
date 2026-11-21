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

    private DashcamCatalog(List<String> paths) {
        List<Recording> dated = new ArrayList<>();
        List<Recording> unknown = new ArrayList<>();
        for (String path : paths) {
            Recording recording = Recording.fromPath(path);
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

    public static DashcamCatalog fromPaths(List<String> paths) {
        return new DashcamCatalog(paths == null ? Collections.<String>emptyList() : paths);
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
            LocalDateTime estimatedEnd = recording.getStart().plus(clip);
            if (i + 1 < files.size()) {
                LocalDateTime next = files.get(i + 1).getStart();
                if (next.isBefore(estimatedEnd) && next.isAfter(recording.getStart())) estimatedEnd = next;
            }
            clips.add(new Clip(recording, estimatedEnd));
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
        private final String dir;
        private final String name;
        private final LocalDateTime start;

        private Recording(String path, String dir, String name, LocalDateTime start) {
            this.path = path;
            this.dir = dir;
            this.name = name;
            this.start = start;
        }

        static Recording fromPath(String path) {
            if (!DashcamHisiliconProtocol.isValidCatalogPath(path)) return null;
            int slash = path.lastIndexOf('/');
            String dir = path.substring("sd//".length(), slash);
            String name = path.substring(slash + 1);
            return new Recording(path, dir, name, parseStart(name));
        }

        public String getPath() {
            return path;
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

        public boolean isEmergency() {
            return "emr".equals(dir);
        }
    }

    public static final class Clip {
        private final Recording recording;
        private final LocalDateTime estimatedEnd;

        Clip(Recording recording, LocalDateTime estimatedEnd) {
            this.recording = recording;
            this.estimatedEnd = estimatedEnd;
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

        public boolean hasEmergency() {
            for (Clip clip : clips) if (clip.getRecording().isEmergency()) return true;
            return false;
        }
    }
}
