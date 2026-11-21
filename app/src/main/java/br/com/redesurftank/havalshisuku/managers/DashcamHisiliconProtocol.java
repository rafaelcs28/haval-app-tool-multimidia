package br.com.redesurftank.havalshisuku.managers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict parser for the small HTTP/HiSilicon responses used by the charging automation. */
public final class DashcamHisiliconProtocol {
    private static final Pattern STATUS_LINE =
            Pattern.compile("^HTTP/1\\.[01] ([0-9]{3})(?: [^\\r\\n]*)?\\r?$", Pattern.MULTILINE);
    private static final Pattern WORK_MODE = assignment("workmode");
    private static final Pattern RUNNING = assignment("running");
    private static final Pattern EMERGENCY = assignment("emrrecord");
    private static final Pattern FUNCTION_RESULT =
            Pattern.compile("^\\s*SvrFuncResult\\s*=\\s*\"(-?[0-9]{1,9})\"\\s*;?\\s*$");
    private static final Pattern CAPABILITY = assignment("capability");
    /** Catalog entries observed as {@code sd//norm/2026_01_15_100600_00.MP4}. */
    private static final Pattern CATALOG_PATH =
            Pattern.compile("^sd//(norm|emr)/([0-9A-Za-z_]{1,64})\\.(MP4|mp4)$");
    /** Result code the tested firmware returns for catalog calls while it records. */
    public static final int RESULT_BUSY_RECORDING = -2222;
    private static final int MAX_CATALOG_ENTRIES_PER_PAGE = 500;

    private DashcamHisiliconProtocol() {}

    private static Pattern assignment(String name) {
        return Pattern.compile("(?:^|\\n)\\s*var\\s+" + name + "\\s*=\\s*\"([^\"]*)\"\\s*;", Pattern.MULTILINE);
    }

    public static WorkState parseWorkState(String body) {
        if (body == null) return null;
        String workMode = find(WORK_MODE, body);
        String running = find(RUNNING, body);
        String emergency = find(EMERGENCY, body);
        if (workMode == null && running == null && emergency == null) return null;
        return new WorkState(workMode, parseBoolean(running), parseBoolean(emergency));
    }

    /** Returns the numeric {@code SvrFuncResult} when the whole body is that marker, else null. */
    public static Integer parseFunctionResult(String body) {
        if (body == null) return null;
        Matcher matcher = FUNCTION_RESULT.matcher(body.trim());
        if (!matcher.matches()) return null;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** Directory names announced by {@code getdircapability.cgi}, or null when absent. */
    public static List<String> parseCapability(String body) {
        if (body == null) return null;
        String raw = find(CAPABILITY, body);
        if (raw == null) return null;
        List<String> dirs = new ArrayList<>();
        for (String part : raw.split(",")) {
            String dir = part.trim();
            if (!dir.isEmpty() && dir.matches("[a-z]{1,16}") && !dirs.contains(dir)) dirs.add(dir);
        }
        return Collections.unmodifiableList(dirs);
    }

    /**
     * Parses one {@code getdirfilelist.cgi} page: paths separated by semicolons. Returns null for a
     * function-result marker, HTML or any body that is not a plain path list. Entries outside the
     * expected directory/name shape are dropped and counted, never turned into URLs.
     */
    public static FileListPage parseFileListPage(String body, String expectedDir) {
        if (body == null || expectedDir == null) return null;
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return new FileListPage(Collections.<String>emptyList(), 0);
        if (parseFunctionResult(trimmed) != null) return null;
        if (trimmed.indexOf('<') >= 0 || trimmed.indexOf('"') >= 0 || trimmed.indexOf('=') >= 0) {
            return null;
        }
        List<String> paths = new ArrayList<>();
        int rejected = 0;
        for (String part : trimmed.split(";")) {
            String entry = part.trim();
            if (entry.isEmpty()) continue;
            if (paths.size() + rejected >= MAX_CATALOG_ENTRIES_PER_PAGE) return null;
            Matcher matcher = CATALOG_PATH.matcher(entry);
            if (matcher.matches() && expectedDir.equals(matcher.group(1)) && !paths.contains(entry)) {
                paths.add(entry);
            } else {
                rejected++;
            }
        }
        return new FileListPage(Collections.unmodifiableList(paths), rejected);
    }

    /** True when {@code path} is a catalog path this client is allowed to request over HTTP. */
    public static boolean isValidCatalogPath(String path) {
        return path != null && CATALOG_PATH.matcher(path).matches();
    }

    /** HTTP path for a validated catalog entry; keeps the observed double slash. */
    public static String httpPathFor(String catalogPath) {
        if (!isValidCatalogPath(catalogPath)) throw new IllegalArgumentException("invalid path");
        return "/" + catalogPath;
    }

    public static boolean isSuccessfulCommandBody(String body) {
        return body != null && "Success".equals(body.trim());
    }

    public static HttpResponse parseHttpResponse(byte[] bytes) {
        if (bytes == null) return null;
        String response = new String(bytes, StandardCharsets.ISO_8859_1);
        int separator = response.indexOf("\r\n\r\n");
        if (separator < 0) return null;
        String headers = response.substring(0, separator);
        Matcher matcher = STATUS_LINE.matcher(headers);
        if (!matcher.find() || matcher.start() != 0) return null;
        try {
            return new HttpResponse(
                    Integer.parseInt(matcher.group(1)),
                    response.substring(separator + 4));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String find(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Boolean parseBoolean(String raw) {
        if ("true".equals(raw)) return Boolean.TRUE;
        if ("false".equals(raw)) return Boolean.FALSE;
        return null;
    }

    public static final class WorkState {
        private final String workMode;
        private final Boolean running;
        private final Boolean emergencyRecording;

        WorkState(String workMode, Boolean running, Boolean emergencyRecording) {
            this.workMode = workMode;
            this.running = running;
            this.emergencyRecording = emergencyRecording;
        }

        public String getWorkMode() {
            return workMode;
        }

        public Boolean getRunning() {
            return running;
        }

        public Boolean getEmergencyRecording() {
            return emergencyRecording;
        }

        public boolean isTrustedNormalState() {
            return "NORM_REC".equals(workMode) && running != null && emergencyRecording != null;
        }

        public boolean canPause() {
            return isTrustedNormalState()
                    && Boolean.TRUE.equals(running)
                    && Boolean.FALSE.equals(emergencyRecording);
        }
    }

    public static final class FileListPage {
        private final List<String> paths;
        private final int rejected;

        FileListPage(List<String> paths, int rejected) {
            this.paths = paths;
            this.rejected = rejected;
        }

        public List<String> getPaths() {
            return paths;
        }

        public int getRejected() {
            return rejected;
        }
    }

    public static final class HttpResponse {
        private final int statusCode;
        private final String body;

        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public boolean isOk() {
            return statusCode == 200;
        }
    }
}
