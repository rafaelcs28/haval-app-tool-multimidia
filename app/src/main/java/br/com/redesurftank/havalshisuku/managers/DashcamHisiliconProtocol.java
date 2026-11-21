package br.com.redesurftank.havalshisuku.managers;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict parser for the small HTTP/HiSilicon responses used by the charging automation. */
public final class DashcamHisiliconProtocol {
    private static final Pattern STATUS_LINE =
            Pattern.compile("^HTTP/1\\.[01] ([0-9]{3})(?: [^\\r\\n]*)?\\r?$", Pattern.MULTILINE);
    private static final Pattern WORK_MODE = assignment("workmode");
    private static final Pattern RUNNING = assignment("running");
    private static final Pattern EMERGENCY = assignment("emrrecord");

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
