package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import br.com.redesurftank.havalshisuku.managers.DashcamCameraHost.Place;
import br.com.redesurftank.havalshisuku.managers.DashcamStationPolicy.AutoAction;
import org.junit.Test;

/** Placeholder network names and passwords only. */
public class DashcamStationPolicyTest {
    private static final long TEN_MIN = DashcamStationPolicy.AUTO_RETRY_WINDOW_MS;

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
    public void autoModeMovesFromApWhenStationVisible() {
        assertEquals(AutoAction.MOVE_TO_STATION,
                DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, true, false, false, false, -1));
        assertEquals(AutoAction.NONE,
                DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, false, false, false, false, -1));
        assertEquals("disabled by default means nothing", AutoAction.NONE,
                DashcamStationPolicy.decideAuto(false, Place.CAMERA_AP, true, false, false, false, -1));
    }

    @Test
    public void autoModeHopsBackWhenCameraReturnedToItsAp() {
        assertEquals(AutoAction.HOP_TO_AP_THEN_MOVE,
                DashcamStationPolicy.decideAuto(true, Place.STATION, false, true, false, false, -1));
        assertEquals("camera already here", AutoAction.NONE,
                DashcamStationPolicy.decideAuto(true, Place.STATION, false, true, true, false, -1));
        assertEquals("AP not in range", AutoAction.NONE,
                DashcamStationPolicy.decideAuto(true, Place.STATION, false, false, false, false, -1));
        assertEquals(AutoAction.NONE,
                DashcamStationPolicy.decideAuto(true, Place.OTHER, true, true, false, false, -1));
        assertEquals(AutoAction.NONE,
                DashcamStationPolicy.decideAuto(true, Place.NO_WIFI, true, true, false, false, -1));
    }

    @Test
    public void autoModeNeverLoops() {
        assertEquals("once per run", AutoAction.NONE,
                DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, true, false, false, true, -1));
        assertEquals("within 10 min", AutoAction.NONE,
                DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, true, false, false, false, TEN_MIN - 1));
        assertEquals(AutoAction.MOVE_TO_STATION,
                DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, true, false, false, false, TEN_MIN));
        assertEquals("clock went back: per-run guard still applies", AutoAction.MOVE_TO_STATION,
                DashcamStationPolicy.decideAuto(true, Place.CAMERA_AP, true, false, false, false, -5));
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
}
