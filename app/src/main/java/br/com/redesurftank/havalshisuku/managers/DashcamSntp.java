package br.com.redesurftank.havalshisuku.managers;

/**
 * Minimal SNTP (RFC 4330) packet handling. Pure Java so the validation is covered by JVM tests;
 * the socket lives in {@link DashcamTimeSource}.
 */
final class DashcamSntp {
    static final int PACKET_SIZE = 48;
    /** Seconds between 1900-01-01 (NTP epoch) and 1970-01-01 (Unix epoch). */
    static final long NTP_UNIX_OFFSET_SECONDS = 2_208_988_800L;
    private static final long MAX_ROUND_TRIP_MS = 2_000L;

    private DashcamSntp() {}

    /** Client request: LI=0, VN=4, Mode=3, transmit timestamp = local time. */
    static byte[] buildRequest(long localNowMs) {
        byte[] packet = new byte[PACKET_SIZE];
        packet[0] = 0x23;
        writeTimestamp(packet, 40, localNowMs);
        return packet;
    }

    /**
     * Validates a server reply and returns the clock offset (server - local) in milliseconds,
     * or null when the reply must be ignored.
     *
     * @param request the exact request sent; the reply must echo its transmit timestamp
     * @param sentLocalMs local time when the request was sent (t1)
     * @param receivedLocalMs local time when the reply arrived (t4)
     */
    static Long offsetMs(byte[] reply, int length, byte[] request, long sentLocalMs,
            long receivedLocalMs) {
        if (reply == null || request == null || length < PACKET_SIZE) return null;
        int leap = (reply[0] >> 6) & 0x3;
        int version = (reply[0] >> 3) & 0x7;
        int mode = reply[0] & 0x7;
        int stratum = reply[1] & 0xff;
        if (leap == 3 || version < 3 || version > 4 || (mode != 4 && mode != 5)) return null;
        if (stratum < 1 || stratum > 15) return null;
        for (int i = 0; i < 8; i++) {
            if (reply[24 + i] != request[40 + i]) return null; // originate must echo our transmit
        }
        long roundTrip = receivedLocalMs - sentLocalMs;
        if (roundTrip < 0 || roundTrip > MAX_ROUND_TRIP_MS) return null;
        Long receive = readTimestamp(reply, 32);
        Long transmit = readTimestamp(reply, 40);
        if (receive == null || transmit == null) return null;
        return ((receive - sentLocalMs) + (transmit - receivedLocalMs)) / 2;
    }

    static void writeTimestamp(byte[] buffer, int offset, long unixMs) {
        long seconds = unixMs / 1000L + NTP_UNIX_OFFSET_SECONDS;
        long fraction = ((unixMs % 1000L) * 0x100000000L) / 1000L;
        for (int i = 3; i >= 0; i--) {
            buffer[offset + i] = (byte) (seconds & 0xff);
            seconds >>= 8;
        }
        for (int i = 7; i >= 4; i--) {
            buffer[offset + i] = (byte) (fraction & 0xff);
            fraction >>= 8;
        }
    }

    /** NTP timestamp as Unix milliseconds; null for the all-zero "unknown" value. */
    static Long readTimestamp(byte[] buffer, int offset) {
        long seconds = 0;
        long fraction = 0;
        for (int i = 0; i < 4; i++) seconds = (seconds << 8) | (buffer[offset + i] & 0xffL);
        for (int i = 4; i < 8; i++) fraction = (fraction << 8) | (buffer[offset + i] & 0xffL);
        if (seconds == 0 && fraction == 0) return null;
        return (seconds - NTP_UNIX_OFFSET_SECONDS) * 1000L + (fraction * 1000L) / 0x100000000L;
    }
}
