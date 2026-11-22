package br.com.redesurftank.havalshisuku.managers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only parser for the Novatek CarDV profile observed in the car (Wi-Fi "CARDV-xxxx", HTTP
 * {@code /?custom=1&cmd=N} at 192.168.1.254, XML answers). Observed 2026-11-21: the file list
 * ({@code 3015}) and ranged downloads work while the camera records, so the gallery never pauses
 * this profile. The only write is {@link #CMD_RECORD}; callers verify its effect by sampling
 * {@link #CMD_RECORDING_SECONDS} twice ({@link #recordingFromSamples}).
 */
public final class DashcamNovatekProtocol {
    public static final String HOST = "192.168.1.254";
    public static final int CMD_HEARTBEAT = 3016;
    public static final int CMD_FILE_LIST = 3015;
    public static final int CMD_FREE_SPACE = 3017;
    public static final int CMD_MODE = 3037;
    public static final int CMD_RECORDING_SECONDS = 2016;
    /** Movie record on/off ({@code par=1} start, {@code par=0} stop). Standard Novatek CarDV. */
    public static final int CMD_RECORD = 2001;
    /** 3037 value for movie (recording) mode. */
    public static final int MODE_MOVIE = 1;

    private static final Pattern STATUS = Pattern.compile("<Status>(-?[0-9]{1,9})</Status>");
    private static final Pattern VALUE = Pattern.compile("<Value>([^<]{0,64})</Value>");
    private static final Pattern FILE = Pattern.compile("<File>(.*?)</File>", Pattern.DOTALL);
    private static final Pattern NAME = tag("NAME");
    private static final Pattern FPATH = tag("FPATH");
    private static final Pattern SIZE = tag("SIZE");
    private static final Pattern TIME = tag("TIME");
    private static final Pattern ATTR = tag("ATTR");
    /** {@code A:\CARDV\Movie\20261121152923_035707.TS} */
    private static final Pattern CAMERA_PATH = Pattern.compile(
            "^A:\\\\CARDV\\\\([A-Za-z]{1,16})\\\\([0-9A-Za-z_]{1,64}\\.(?:TS|ts|MP4|mp4|MOV|mov))$");
    private static final Pattern HTTP_PATH = Pattern.compile(
            "^/CARDV/[A-Za-z]{1,16}/[0-9A-Za-z_]{1,64}\\.(?:TS|ts|MP4|mp4|MOV|mov)$");
    private static final Pattern NAME_START = Pattern.compile("^([0-9]{14})_");
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final int MAX_FILES = 20_000;

    private DashcamNovatekProtocol() {}

    private static Pattern tag(String name) {
        return Pattern.compile("<" + name + ">([^<]{0,256})</" + name + ">");
    }

    /** {@code <Function>…<Status>s</Status>[<Value>v</Value>]</Function>}, or null. */
    public static Reply parseReply(String body) {
        if (body == null || !body.contains("<Function>")) return null;
        Matcher status = STATUS.matcher(body);
        if (!status.find()) return null;
        Matcher value = VALUE.matcher(body);
        try {
            return new Reply(Integer.parseInt(status.group(1)), value.find() ? value.group(1).trim() : null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** Entries of a {@code 3015} answer; null when the body is not a file list. */
    public static List<Entry> parseFileList(String body) {
        if (body == null || !body.contains("<LIST>")) return null;
        List<Entry> entries = new ArrayList<>();
        Matcher file = FILE.matcher(body);
        while (file.find()) {
            if (entries.size() >= MAX_FILES) break;
            String block = file.group(1);
            String path = find(FPATH, block);
            String name = find(NAME, block);
            Entry entry = entryFromCameraPath(path, name, find(SIZE, block), find(TIME, block), find(ATTR, block));
            if (entry != null) entries.add(entry);
        }
        return Collections.unmodifiableList(entries);
    }

    /** Builds a validated entry from the camera path and fields; null when anything is off. */
    static Entry entryFromCameraPath(String cameraPath, String name, String size, String time, String attr) {
        if (cameraPath == null) return null;
        Matcher matcher = CAMERA_PATH.matcher(cameraPath.trim());
        if (!matcher.matches()) return null;
        String fileName = matcher.group(2);
        if (name != null && !fileName.equals(name.trim())) return null;
        return entry(matcher.group(1), fileName, size, time, attr);
    }

    /** Builds a validated entry from directory and file name (cache reload path). */
    static Entry entry(String dir, String name, String size, String time, String attr) {
        if (dir == null || name == null) return null;
        String httpPath = "/CARDV/" + dir + "/" + name;
        if (!HTTP_PATH.matcher(httpPath).matches()) return null;
        long bytes = -1;
        if (size != null) {
            try {
                bytes = Long.parseLong(size.trim());
            } catch (NumberFormatException ignored) {
                bytes = -1;
            }
        }
        int attributes = 0;
        if (attr != null) {
            try {
                attributes = Integer.parseInt(attr.trim());
            } catch (NumberFormatException ignored) {
                attributes = 0;
            }
        }
        LocalDateTime start = parse(NAME_START, name, NAME_TIME);
        LocalDateTime end = time == null ? null : parseExact(time.trim(), FILE_TIME);
        if (start != null && end != null && (end.isBefore(start) || end.isAfter(start.plusMinutes(30)))) {
            end = null; // implausible: keep the start, estimate the end instead
        }
        // ATTR bit 0 = read-only (locked by an event); EMR/Event folders are emergency clips.
        boolean emergency = (attributes & 1) != 0
                || "EMR".equalsIgnoreCase(dir) || "Event".equalsIgnoreCase(dir) || "RO".equalsIgnoreCase(dir);
        return new Entry(dir, name, httpPath, bytes, start, end, emergency, time == null ? null : time.trim(),
                attributes);
    }

    /**
     * Recording state from two {@code 2016} samples taken ~1 s apart: the counter of the file being
     * written moves while it records (it also drops back when a new file starts). Equal samples,
     * zero or not, mean stopped. Null when a sample is missing.
     */
    public static Boolean recordingFromSamples(Long first, Long second) {
        if (first == null || second == null || first < 0 || second < 0) return null;
        return !first.equals(second);
    }

    public static boolean isValidHttpPath(String path) {
        return path != null && HTTP_PATH.matcher(path).matches();
    }

    private static LocalDateTime parse(Pattern pattern, String input, DateTimeFormatter format) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? parseExact(matcher.group(1), format) : null;
    }

    private static LocalDateTime parseExact(String text, DateTimeFormatter format) {
        try {
            return LocalDateTime.parse(text, format);
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private static String find(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static final class Reply {
        private final int status;
        private final String value;

        Reply(int status, String value) {
            this.status = status;
            this.value = value;
        }

        public int getStatus() {
            return status;
        }

        public String getValue() {
            return value;
        }

        public Long getLongValue() {
            if (value == null) return null;
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }

    /**
     * Card capacity as the camera can see it: free bytes (3017) plus every listed file (3015). The
     * firmware has no documented "total" command; the loop recorder keeps the card full of listed
     * clips, so this lands a little under the card's nominal size. Null without both inputs.
     */
    public static Long cardTotalBytes(Long freeBytes, List<Entry> entries) {
        if (freeBytes == null || freeBytes < 0 || entries == null) return null;
        long total = freeBytes;
        for (Entry entry : entries) {
            if (entry != null && entry.size > 0) total += entry.size;
        }
        return total;
    }

    public static final class Entry {
        final String dir;
        final String name;
        final String httpPath;
        final long size;
        final LocalDateTime start;
        final LocalDateTime end;
        final boolean emergency;
        final String timeText;
        final int attributes;

        Entry(String dir, String name, String httpPath, long size, LocalDateTime start, LocalDateTime end,
                boolean emergency, String timeText, int attributes) {
            this.dir = dir;
            this.name = name;
            this.httpPath = httpPath;
            this.size = size;
            this.start = start;
            this.end = end;
            this.emergency = emergency;
            this.timeText = timeText;
            this.attributes = attributes;
        }

        public String getHttpPath() {
            return httpPath;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public LocalDateTime getEnd() {
            return end;
        }
    }
}
