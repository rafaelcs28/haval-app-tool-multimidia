package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class DashcamHisiliconProtocolTest {
    @Test
    public void parsesTrustedNormalRecordingState() {
        DashcamHisiliconProtocol.WorkState state = DashcamHisiliconProtocol.parseWorkState(
                "var workmode=\"NORM_REC\";\n"
                        + "var running=\"true\";\n"
                        + "var emrrecord=\"false\";\n");

        assertNotNull(state);
        assertEquals("NORM_REC", state.getWorkMode());
        assertEquals(Boolean.TRUE, state.getRunning());
        assertEquals(Boolean.FALSE, state.getEmergencyRecording());
        assertTrue(state.isTrustedNormalState());
        assertTrue(state.canPause());
    }

    @Test
    public void emergencyRecordingCannotBePaused() {
        DashcamHisiliconProtocol.WorkState state = DashcamHisiliconProtocol.parseWorkState(
                "var workmode=\"NORM_REC\";\n"
                        + "var running=\"true\";\n"
                        + "var emrrecord=\"true\";\n");

        assertNotNull(state);
        assertTrue(state.isTrustedNormalState());
        assertFalse(state.canPause());
    }

    @Test
    public void incompleteOrUnknownStatusIsNotTrusted() {
        assertNull(DashcamHisiliconProtocol.parseWorkState("SvrFuncResult=\"-2222\""));
        DashcamHisiliconProtocol.WorkState state = DashcamHisiliconProtocol.parseWorkState(
                "var workmode=\"PHOTO\";\n"
                        + "var running=\"true\";\n"
                        + "var emrrecord=\"false\";\n");
        assertNotNull(state);
        assertFalse(state.isTrustedNormalState());
    }

    @Test
    public void acceptsOnlyExactSuccessfulCommandBody() {
        assertTrue(DashcamHisiliconProtocol.isSuccessfulCommandBody(" Success\r\n"));
        assertFalse(DashcamHisiliconProtocol.isSuccessfulCommandBody("SvrFuncResult=\"-2222\""));
        assertFalse(DashcamHisiliconProtocol.isSuccessfulCommandBody("Success later"));
    }

    @Test
    public void parsesHttpStatusAndBodyWithoutFollowingRedirects() {
        DashcamHisiliconProtocol.HttpResponse response =
                DashcamHisiliconProtocol.parseHttpResponse(
                        ("HTTP/1.0 200 OK\r\nContent-Type: text/plain\r\n\r\nSuccess\r\n")
                                .getBytes(StandardCharsets.ISO_8859_1));
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals("Success\r\n", response.getBody());

        DashcamHisiliconProtocol.HttpResponse redirect =
                DashcamHisiliconProtocol.parseHttpResponse(
                        ("HTTP/1.0 302 Found\r\nLocation: http://example.invalid/\r\n\r\n")
                                .getBytes(StandardCharsets.ISO_8859_1));
        assertNotNull(redirect);
        assertEquals(302, redirect.getStatusCode());
        assertFalse(redirect.isOk());
    }
}
