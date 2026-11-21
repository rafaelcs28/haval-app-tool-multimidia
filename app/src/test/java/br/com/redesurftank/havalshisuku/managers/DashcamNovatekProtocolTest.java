package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Test;

/** Fixtures copied from the car's camera (Wi-Fi CARDV-3605) on 2026-11-21. */
public class DashcamNovatekProtocolTest {
    private static final String LIST = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<LIST>\n"
            + "<ALLFile><File>\n<NAME>20261115130916_034795.TS</NAME>\n"
            + "<FPATH>A:\\CARDV\\Movie\\20261115130916_034795.TS</FPATH>\n<SIZE>139540180</SIZE>\n"
            + "<TIMECODE>1567582535</TIMECODE>\n<TIME>2026/11/15 13:10:14</TIME>\n<ATTR>32</ATTR></File>\n</ALLFile>\n"
            + "<ALLFile><File>\n<NAME>20261115131016_034796.TS</NAME>\n"
            + "<FPATH>A:\\CARDV\\Movie\\20261115131016_034796.TS</FPATH>\n<SIZE>139520440</SIZE>\n"
            + "<TIMECODE>1567582567</TIMECODE>\n<TIME>2026/11/15 13:11:14</TIME>\n<ATTR>32</ATTR></File>\n</ALLFile>\n"
            + "<ALLFile><File>\n<NAME>20261115131116_034797.TS</NAME>\n"
            + "<FPATH>A:\\CARDV\\Movie\\20261115131116_034797.TS</FPATH>\n<SIZE>139553528</SIZE>\n"
            + "<TIMECODE>1567582599</TIMECODE>\n<TIME>2026/11/15 13:12:14</TIME>\n<ATTR>33</ATTR></File>\n</ALLFile>\n"
            + "<ALLFile><File>\n<NAME>../evil.TS</NAME>\n<FPATH>A:\\CARDV\\..\\evil.TS</FPATH>\n"
            + "<SIZE>1</SIZE>\n<TIME>2026/11/15 13:12:14</TIME>\n<ATTR>32</ATTR></File>\n</ALLFile>\n"
            + "</LIST>\n";

    @Test
    public void parsesObservedReplies() {
        DashcamNovatekProtocol.Reply mode = DashcamNovatekProtocol.parseReply(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<Function>\n<Cmd>3037</Cmd>\n<Status>0</Status>\n"
                        + "<Value>1</Value>\n</Function>");
        assertNotNull(mode);
        assertEquals(0, mode.getStatus());
        assertEquals(Long.valueOf(1), mode.getLongValue());

        DashcamNovatekProtocol.Reply free = DashcamNovatekProtocol.parseReply(
                "<Function>\n<Cmd>3017</Cmd>\n<Status>0</Status>\n<Value>1179516928</Value>\n</Function>");
        assertEquals(Long.valueOf(1179516928L), free.getLongValue());

        DashcamNovatekProtocol.Reply heartbeat = DashcamNovatekProtocol.parseReply(
                "<Function>\n<Cmd>3016</Cmd>\n<Status>0</Status>\n</Function>");
        assertNull(heartbeat.getValue());
        assertNull(DashcamNovatekProtocol.parseReply("<html>404</html>"));
    }

    @Test
    public void parsesFileListWithCameraEndTimesAndDropsBadPaths() {
        List<DashcamNovatekProtocol.Entry> entries = DashcamNovatekProtocol.parseFileList(LIST);

        assertNotNull(entries);
        assertEquals(3, entries.size());
        DashcamNovatekProtocol.Entry first = entries.get(0);
        assertEquals("/CARDV/Movie/20261115130916_034795.TS", first.getHttpPath());
        assertEquals(LocalDateTime.of(2026, 11, 15, 13, 9, 16), first.getStart());
        assertEquals(LocalDateTime.of(2026, 11, 15, 13, 10, 14), first.getEnd());
        assertEquals(139540180L, first.size);
        assertFalse(first.emergency);
        assertTrue("ATTR read-only bit marks a locked clip", entries.get(2).emergency);
        assertNull(DashcamNovatekProtocol.parseFileList("<Function><Status>-1</Status></Function>"));
    }

    @Test
    public void validatesHttpPathsAndRejectsImplausibleEnds() {
        assertTrue(DashcamNovatekProtocol.isValidHttpPath("/CARDV/Movie/20261121152923_035707.TS"));
        assertFalse(DashcamNovatekProtocol.isValidHttpPath("/CARDV/../etc/passwd"));
        assertFalse(DashcamNovatekProtocol.isValidHttpPath("/CARDV/Movie/x.TS?y=1"));

        DashcamNovatekProtocol.Entry entry = DashcamNovatekProtocol.entry(
                "Movie", "20261115130916_034795.TS", "10", "2026/11/16 13:10:14", "32");
        assertNotNull(entry);
        assertNull("end a day later is discarded", entry.getEnd());
    }

    @Test
    public void catalogUsesCameraEndTimes() {
        DashcamCatalog catalog = DashcamCatalog.fromNovatek(DashcamNovatekProtocol.parseFileList(LIST));
        LocalDate day = LocalDate.of(2026, 11, 15);

        assertTrue(catalog.hasRecordingsOn(day));
        List<DashcamCatalog.Segment> segments = catalog.getSegments(day);
        assertEquals(1, segments.size());
        DashcamCatalog.Segment segment = segments.get(0);
        assertEquals(3, segment.getClips().size());
        assertEquals(LocalDateTime.of(2026, 11, 15, 13, 12, 14), segment.getEstimatedEnd());
        assertFalse(segment.isEndEstimated());
        assertFalse(catalog.hasEstimatedEnds());
        DashcamCatalog.Recording recording = segment.getClips().get(0).getRecording();
        assertEquals("192.168.1.254", recording.getHost());
        assertEquals("/CARDV/Movie/20261115130916_034795.TS", recording.getHttpPath());
    }

    @Test
    public void novatekSubnetIsOnly192168One() throws Exception {
        assertTrue(DashcamProfile.isNovatekSubnet(InetAddress.getByAddress(new byte[] {(byte) 192, (byte) 168, 1, 33})));
        assertFalse(DashcamProfile.isNovatekSubnet(InetAddress.getByAddress(new byte[] {(byte) 192, (byte) 168, 0, 115})));
    }

    @Test
    public void recordingStateFromTwoCounterSamples() {
        assertEquals(Boolean.TRUE, DashcamNovatekProtocol.recordingFromSamples(6L, 7L));
        assertEquals("new file restarts the counter", Boolean.TRUE, DashcamNovatekProtocol.recordingFromSamples(59L, 0L));
        assertEquals(Boolean.FALSE, DashcamNovatekProtocol.recordingFromSamples(0L, 0L));
        assertEquals("frozen counter is stopped", Boolean.FALSE, DashcamNovatekProtocol.recordingFromSamples(19L, 19L));
        assertNull(DashcamNovatekProtocol.recordingFromSamples(null, 3L));
    }
}
