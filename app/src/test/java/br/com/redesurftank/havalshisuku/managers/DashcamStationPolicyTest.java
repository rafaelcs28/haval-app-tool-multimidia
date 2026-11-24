package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import br.com.redesurftank.havalshisuku.managers.DashcamCameraHost.Place;
import br.com.redesurftank.havalshisuku.managers.DashcamStationPolicy.AutoAction;
import org.junit.Test;

/** Placeholder network names and passwords only. */
public class DashcamStationPolicyTest {

    @Test
    public void validatesInput() {
        assertNull(DashcamStationPolicy.validate("ExampleNet", "example123"));
        assertEquals("ssid_empty", DashcamStationPolicy.validate(" ", "example123"));
        assertEquals("ssid_empty", DashcamStationPolicy.validate(null, "example123"));
        assertEquals("ssid_colon", DashcamStationPolicy.validate("Bad:Net", "example123"));
        assertEquals("ssid_too_long", DashcamStationPolicy.validate("123456789012345678901234567890123", "example123"));
        assertEquals("ssid_invalid", DashcamStationPolicy.validate("Net\n", "example123"));
        assertEquals("password_short", DashcamStationPolicy.validate("ExampleNet", "short"));
        assertEquals("password_short", DashcamStationPolicy.validate("ExampleNet", null));
        assertNull(DashcamStationPolicy.validate("ExampleNet", "12345678901234567890123456"));
        assertEquals("password_long", DashcamStationPolicy.validate("ExampleNet", "123456789012345678901234567"));
        assertEquals("password_invalid", DashcamStationPolicy.validate("ExampleNet", "senhação123"));
        assertNotNull(DashcamStationPolicy.validationMessage("password_long"));
        assertNull(DashcamStationPolicy.validationMessage(null));
    }

    @Test
    public void percentEncodesEachHalf() {
        assertEquals("ExampleNet", DashcamStationPolicy.percentEncode("ExampleNet"));
        assertEquals("My%20Net%26Co", DashcamStationPolicy.percentEncode("My Net&Co"));
        assertEquals("a%2Bb%3Dc%3A", DashcamStationPolicy.percentEncode("a+b=c:"));
        assertEquals("Caf%C3%A9", DashcamStationPolicy.percentEncode("Café"));
        assertEquals("-._~", DashcamStationPolicy.percentEncode("-._~"));
    }

    @Test
    public void stationCommandKeepsLiteralSeparator() {
        assertEquals("3032&str=ExampleNet:example123", DashcamStationPolicy.stationCommand("ExampleNet", "example123"));
        assertEquals("3032&str=My%20Net:pa%26ss%23word", DashcamStationPolicy.stationCommand("My Net", "pa&ss#word"));
        assertNull(DashcamStationPolicy.stationCommand("Bad:Net", "example123"));
        assertNull(DashcamStationPolicy.stationCommand("ExampleNet", "short"));
        assertEquals("3033&par=1", DashcamStationPolicy.wifiModeCommand(true));
        assertEquals("3033&par=0", DashcamStationPolicy.wifiModeCommand(false));
    }




    @Test
    public void statusLine() {
        assertEquals("No Wi-Fi próprio (192.168.1.254)", DashcamStationPolicy.statusText(Place.CAMERA_AP, null, "CARDV-0000"));
        assertEquals("Na rede ExampleNet (10.20.30.78)",
                DashcamStationPolicy.statusText(Place.STATION, "10.20.30.78", "ExampleNet"));
        assertEquals("Na rede configurada (10.20.30.78)",
                DashcamStationPolicy.statusText(Place.STATION, "10.20.30.78", null));
        assertEquals("Não encontrada", DashcamStationPolicy.statusText(Place.STATION, null, "ExampleNet"));
        assertEquals("Não encontrada", DashcamStationPolicy.statusText(Place.OTHER, null, "Home"));
        assertEquals("Não encontrada", DashcamStationPolicy.statusText(Place.NO_WIFI, null, null));
    }

    @Test
    public void provisioningAutoFlagAcceptsBooleansAndCommonStrings() {
        assertEquals(Boolean.TRUE, DashcamStationPolicy.parseAutoFlag(Boolean.TRUE));
        assertEquals(Boolean.FALSE, DashcamStationPolicy.parseAutoFlag(Boolean.FALSE));
        assertEquals(Boolean.TRUE, DashcamStationPolicy.parseAutoFlag("1"));
        assertEquals(Boolean.TRUE, DashcamStationPolicy.parseAutoFlag(" TRUE "));
        assertEquals(Boolean.FALSE, DashcamStationPolicy.parseAutoFlag("0"));
        assertEquals(Boolean.FALSE, DashcamStationPolicy.parseAutoFlag("false"));
        assertNull(DashcamStationPolicy.parseAutoFlag("yes"));
        assertNull(DashcamStationPolicy.parseAutoFlag(1));
        assertNull(DashcamStationPolicy.parseAutoFlag(null));
    }

    @Test
    public void autoMovesFromTheCameraWifiWithTheStationInRange() {
        assertEquals(AutoAction.MOVE_TO_STATION, DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, true, false, false, -1, -1));
        assertEquals(AutoAction.NONE, DashcamStationPolicy.decideAuto(false, Place.CAMERA_AP, true, false, false, -1, -1));
        assertEquals(AutoAction.NONE, DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, false, false, false, -1, -1));
    }

    @Test
    public void autoHopsBackOnlyWhenTheCameraIsMissingAndItsWifiIsInRange() {
        assertEquals(AutoAction.HOP_TO_AP_THEN_MOVE, DashcamStationPolicy.decideAuto(true, Place.STATION, false, false, true, -1, -1));
        // Camera already on the station network: nothing to do.
        assertEquals(AutoAction.NONE, DashcamStationPolicy.decideAuto(true, Place.STATION, false, true, true, -1, -1));
        // Camera Wi-Fi not in range: nothing.
        assertEquals(AutoAction.NONE, DashcamStationPolicy.decideAuto(true, Place.STATION, false, false, false, -1, -1));
        // Other networks / no Wi-Fi: never.
        assertEquals(AutoAction.NONE, DashcamStationPolicy.decideAuto(true, Place.OTHER, true, false, true, -1, -1));
        assertEquals(AutoAction.NONE, DashcamStationPolicy.decideAuto(true, Place.NO_WIFI, true, false, true, -1, -1));
    }

    @Test
    public void autoWaitsAfterAFailureAndAfterAManualReturn() {
        long retry = DashcamStationPolicy.AUTO_RETRY_WINDOW_MS;
        long pause = DashcamStationPolicy.AUTO_PAUSE_AFTER_RETURN_MS;
        assertEquals(AutoAction.NONE, DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, true, false, false, retry - 1, -1));
        assertEquals(AutoAction.MOVE_TO_STATION, DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, true, false, false, retry, -1));
        assertEquals(AutoAction.NONE, DashcamStationPolicy.decideAuto(true, Place.STATION, false, false, true, -1, pause - 1));
        assertEquals(AutoAction.HOP_TO_AP_THEN_MOVE, DashcamStationPolicy.decideAuto(true, Place.STATION, false, false, true, -1, pause));
        assertEquals(60_000L, DashcamStationPolicy.AUTO_CHECK_INTERVAL_MS);
        assertEquals(5L * 60 * 1000, retry);
    }

    @Test
    public void rememberedStepIsReusedOnlyWhileFresh() {
        long now = 1_000_000_000L;
        assertTrue(DashcamStationPolicy.stepStillValid(now - 1_000L, now));
        assertTrue(DashcamStationPolicy.stepStillValid(now - DashcamStationPolicy.STEP_VALID_MS + 1, now));
        assertFalse(DashcamStationPolicy.stepStillValid(now - DashcamStationPolicy.STEP_VALID_MS, now));
        assertFalse(DashcamStationPolicy.stepStillValid(0L, now));
        assertFalse(DashcamStationPolicy.stepStillValid(now + 5_000L, now));
    }
}
