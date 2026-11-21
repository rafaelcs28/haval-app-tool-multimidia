package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DashcamSntpTest {
    private static byte[] reply(byte[] request, long receiveMs, long transmitMs, int stratum, int mode) {
        byte[] reply = new byte[DashcamSntp.PACKET_SIZE];
        reply[0] = (byte) ((0 << 6) | (4 << 3) | mode);
        reply[1] = (byte) stratum;
        System.arraycopy(request, 40, reply, 24, 8);
        DashcamSntp.writeTimestamp(reply, 32, receiveMs);
        DashcamSntp.writeTimestamp(reply, 40, transmitMs);
        return reply;
    }

    @Test
    public void timestampRoundTrip() {
        byte[] buffer = new byte[8];
        DashcamSntp.writeTimestamp(buffer, 0, 1_795_000_123_456L);
        Long read = DashcamSntp.readTimestamp(buffer, 0);
        assertNotNull(read);
        assertEquals(1_795_000_123_456d, (double) read, 1.0d);
    }

    @Test
    public void computesOffsetFromValidReply() {
        long sent = 1_795_000_000_000L;
        byte[] request = DashcamSntp.buildRequest(sent);
        // Server clock 3 s ahead, 100 ms each way.
        byte[] reply = reply(request, sent + 3_100L, sent + 3_100L, 2, 4);
        Long offset = DashcamSntp.offsetMs(reply, reply.length, request, sent, sent + 200L);
        assertNotNull(offset);
        assertEquals(3_000d, (double) offset, 2.0d);
    }

    @Test
    public void rejectsSpoofedUnsynchronizedOrSlowReplies() {
        long sent = 1_795_000_000_000L;
        byte[] request = DashcamSntp.buildRequest(sent);
        byte[] otherRequest = DashcamSntp.buildRequest(sent + 1);

        byte[] spoofed = reply(otherRequest, sent, sent, 2, 4);
        assertNull(DashcamSntp.offsetMs(spoofed, spoofed.length, request, sent, sent + 50));

        byte[] kissOfDeath = reply(request, sent, sent, 0, 4);
        assertNull(DashcamSntp.offsetMs(kissOfDeath, kissOfDeath.length, request, sent, sent + 50));

        byte[] clientMode = reply(request, sent, sent, 2, 3);
        assertNull(DashcamSntp.offsetMs(clientMode, clientMode.length, request, sent, sent + 50));

        byte[] good = reply(request, sent, sent, 2, 4);
        assertNull(DashcamSntp.offsetMs(good, good.length, request, sent, sent + 5_000));
        assertNull(DashcamSntp.offsetMs(good, 20, request, sent, sent + 50));
    }
}
