package br.com.redesurftank.havalshisuku.managers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure parser for the head of an HTTP/1.x response read from a raw socket
 * ({@link DashcamCameraHttp}): status line, headers, Content-Length and Content-Range. Strict on
 * purpose: anything odd (duplicate conflicting lengths, folded headers, bad numbers) is rejected
 * instead of guessed, so a confused or hostile host on a shared network cannot steer the reader.
 */
final class DashcamHttpHead {
    /** Longest head accepted (status line + headers). */
    static final int MAX_HEAD_BYTES = 16 * 1024;
    private static final Pattern STATUS_LINE = Pattern.compile("^HTTP/1\\.[01] ([0-9]{3})(?: .*)?$");
    private static final Pattern CONTENT_RANGE =
            Pattern.compile("^bytes ([0-9]{1,18})-([0-9]{1,18})/([0-9]{1,18}|\\*)$");
    private static final Pattern DIGITS = Pattern.compile("^[0-9]{1,18}$");
    private static final Pattern TOKEN = Pattern.compile("^[!#$%&'*+.^_`|~0-9A-Za-z-]{1,64}$");

    final int statusCode;
    /** Header names in lower case; values trimmed. */
    final Map<String, String> headers;

    private DashcamHttpHead(int statusCode, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.headers = Collections.unmodifiableMap(headers);
    }

    /** Parses a head without its final empty line ({@code "HTTP/1.1 206 ...\r\nA: b"}); null if malformed. */
    static DashcamHttpHead parse(String head) {
        if (head == null || head.isEmpty() || head.length() > MAX_HEAD_BYTES) return null;
        String[] lines = head.split("\r\n", -1);
        Matcher status = STATUS_LINE.matcher(lines[0]);
        if (!status.matches()) return null;
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) return null; // the caller strips the terminating blank line
            if (line.charAt(0) == ' ' || line.charAt(0) == '\t') return null; // obsolete folding
            int colon = line.indexOf(':');
            if (colon <= 0) return null;
            String name = line.substring(0, colon);
            if (!TOKEN.matcher(name).matches()) return null;
            String key = name.toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            String previous = headers.get(key);
            if (previous != null) {
                if (("content-length".equals(key) || "content-range".equals(key)) && !previous.equals(value)) {
                    return null;
                }
                continue; // keep the first of other repeated headers
            }
            headers.put(key, value);
        }
        return new DashcamHttpHead(Integer.parseInt(status.group(1)), headers);
    }

    String header(String name) {
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    /** Content-Length, -1 when absent; {@link Long#MIN_VALUE} when present but invalid. */
    long contentLength() {
        String value = header("content-length");
        if (value == null) return -1L;
        if (!DIGITS.matcher(value).matches()) return Long.MIN_VALUE;
        return Long.parseLong(value);
    }

    /** True when the body uses a transfer coding other than identity (e.g. chunked). */
    boolean hasTransferCoding() {
        String value = header("transfer-encoding");
        return value != null && !"identity".equalsIgnoreCase(value);
    }

    boolean isRedirect() {
        return statusCode >= 300 && statusCode < 400;
    }

    /**
     * {@code bytes a-b/total} → {a, b, total} (total -1 for {@code *}); null when malformed or
     * inconsistent (b before a, or b past the total).
     */
    static long[] parseContentRange(String value) {
        if (value == null) return null;
        Matcher matcher = CONTENT_RANGE.matcher(value.trim());
        if (!matcher.matches()) return null;
        long first = Long.parseLong(matcher.group(1));
        long last = Long.parseLong(matcher.group(2));
        long total = "*".equals(matcher.group(3)) ? -1L : Long.parseLong(matcher.group(3));
        if (last < first) return null;
        if (total >= 0 && last >= total) return null;
        return new long[] {first, last, total};
    }

    /** {@code Range} value for a read at {@code position} of {@code length} bytes (-1 open); null for a plain GET. */
    static String rangeHeader(long position, long length) {
        if (position < 0) return null;
        if (position == 0 && length < 0) return null;
        String range = "bytes=" + position + "-";
        if (length > 0) range += (position + length - 1);
        return range;
    }

    /** True when {@code buffer[0..size)} ends with the {@code \r\n\r\n} that closes the head. */
    static boolean endsHead(byte[] buffer, int size) {
        return size >= 4 && buffer[size - 4] == '\r' && buffer[size - 3] == '\n'
                && buffer[size - 2] == '\r' && buffer[size - 1] == '\n';
    }
}
