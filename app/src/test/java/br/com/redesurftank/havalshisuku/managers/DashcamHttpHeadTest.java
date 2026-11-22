package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class DashcamHttpHeadTest {
    @Test
    public void parsesPartialContentHead() {
        DashcamHttpHead head = DashcamHttpHead.parse("HTTP/1.1 206 Partial Content\r\n"
                + "Content-Type: video/MP2T\r\nContent-Length: 1000\r\n"
                + "Content-Range: bytes 500-1499/139540180\r\nConnection: close");
        assertNotNull(head);
        assertEquals(206, head.statusCode);
        assertEquals(1000L, head.contentLength());
        assertEquals("bytes 500-1499/139540180", head.header("content-range"));
        assertEquals("video/MP2T", head.header("CONTENT-TYPE"));
        assertFalse(head.isRedirect());
        assertFalse(head.hasTransferCoding());
    }

    @Test
    public void statusLineVariants() {
        assertEquals(200, DashcamHttpHead.parse("HTTP/1.0 200 OK").statusCode);
        assertEquals(200, DashcamHttpHead.parse("HTTP/1.1 200").statusCode);
        assertNull(DashcamHttpHead.parse("HTTP/2 200 OK"));
        assertNull(DashcamHttpHead.parse("ICY 200 OK"));
        assertNull(DashcamHttpHead.parse("HTTP/1.1 20 OK"));
        assertNull(DashcamHttpHead.parse(""));
        assertNull(DashcamHttpHead.parse(null));
    }

    @Test
    public void rejectsMalformedHeaders() {
        assertNull("no colon", DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nbroken"));
        assertNull("folded", DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nA: b\r\n c"));
        assertNull("empty name", DashcamHttpHead.parse("HTTP/1.1 200 OK\r\n: b"));
        assertNull("space in name", DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nBad Name: b"));
        assertNull("blank line inside", DashcamHttpHead.parse("HTTP/1.1 200 OK\r\n\r\nA: b"));
        assertNull("conflicting lengths",
                DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nContent-Length: 1\r\nContent-Length: 2"));
        assertNotNull("repeated identical length",
                DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nContent-Length: 2"));
    }

    @Test
    public void contentLengthValues() {
        assertEquals(-1L, DashcamHttpHead.parse("HTTP/1.1 200 OK").contentLength());
        assertEquals(0L, DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nContent-Length: 0").contentLength());
        assertEquals(Long.MIN_VALUE, DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nContent-Length: -5").contentLength());
        assertEquals(Long.MIN_VALUE, DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nContent-Length: 12a").contentLength());
        assertEquals(Long.MIN_VALUE,
                DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nContent-Length: 1234567890123456789").contentLength());
    }

    @Test
    public void redirectsAndCodings() {
        assertTrue(DashcamHttpHead.parse("HTTP/1.1 302 Found\r\nLocation: http://example.invalid/").isRedirect());
        assertTrue(DashcamHttpHead.parse("HTTP/1.1 301 Moved").isRedirect());
        assertFalse(DashcamHttpHead.parse("HTTP/1.1 404 Not Found").isRedirect());
        assertTrue(DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked").hasTransferCoding());
        assertFalse(DashcamHttpHead.parse("HTTP/1.1 200 OK\r\nTransfer-Encoding: identity").hasTransferCoding());
    }

    @Test
    public void contentRange() {
        assertArrayEquals(new long[] {0, 99, 1000}, DashcamHttpHead.parseContentRange("bytes 0-99/1000"));
        assertArrayEquals(new long[] {5, 9, -1}, DashcamHttpHead.parseContentRange("bytes 5-9/*"));
        assertNull(DashcamHttpHead.parseContentRange("bytes 9-5/1000"));
        assertNull(DashcamHttpHead.parseContentRange("bytes 0-1000/1000"));
        assertNull(DashcamHttpHead.parseContentRange("bytes */1000"));
        assertNull(DashcamHttpHead.parseContentRange("items 0-1/2"));
        assertNull(DashcamHttpHead.parseContentRange(null));
    }

    @Test
    public void rangeHeader() {
        assertNull(DashcamHttpHead.rangeHeader(0, -1));
        assertEquals("bytes=100-", DashcamHttpHead.rangeHeader(100, -1));
        assertEquals("bytes=0-99", DashcamHttpHead.rangeHeader(0, 100));
        assertEquals("bytes=100-199", DashcamHttpHead.rangeHeader(100, 100));
        assertNull(DashcamHttpHead.rangeHeader(-1, 10));
    }

    @Test
    public void endsHead() {
        byte[] bytes = "HTTP/1.1 200 OK\r\nA: b\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
        assertTrue(DashcamHttpHead.endsHead(bytes, bytes.length));
        assertFalse(DashcamHttpHead.endsHead(bytes, bytes.length - 1));
        assertFalse(DashcamHttpHead.endsHead(bytes, 3));
    }

    @Test
    public void limitedStreamStopsAtLength() throws Exception {
        java.io.InputStream source = new java.io.ByteArrayInputStream("abcdefgh".getBytes(StandardCharsets.ISO_8859_1));
        DashcamCameraHttp.LimitedInputStream limited = new DashcamCameraHttp.LimitedInputStream(source, 5);
        byte[] buffer = new byte[16];
        assertEquals(5, limited.read(buffer, 0, buffer.length));
        assertEquals(-1, limited.read(buffer, 0, buffer.length));
        assertEquals(-1, limited.read());
    }
}
