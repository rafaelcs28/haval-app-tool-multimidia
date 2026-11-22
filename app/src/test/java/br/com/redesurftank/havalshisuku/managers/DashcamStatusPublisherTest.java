package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DashcamStatusPublisherTest {
    private static DashcamGalleryRepository.StateSnapshot snap(
            DashcamGalleryRepository.StateKind kind, String error) {
        return new DashcamGalleryRepository.StateSnapshot(kind, null, error, 0L, false);
    }

    @Test
    public void mapsCameraStatesForThe3dCard() {
        assertEquals("recording", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.RECORDING, null), DashcamProfile.NOVATEK));
        assertEquals("paused_auto", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.PAUSED_BY_CHARGING, null), DashcamProfile.NOVATEK));
        assertEquals("paused", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.PAUSED_BY_GALLERY, null), DashcamProfile.HISILICON));
        assertEquals("emergency", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.EMERGENCY, null), DashcamProfile.HISILICON));
        assertEquals("no_wifi", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.WIFI_OFF, "wifi_unavailable"), DashcamProfile.NONE));
    }

    @Test
    public void otherNetworksAreNoWifiButASilentNovatekIsUnreachable() {
        assertEquals("unreachable", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.UNREACHABLE, "transport_x"), DashcamProfile.NOVATEK));
        assertEquals("no_wifi", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.UNREACHABLE, "transport_x"), DashcamProfile.HISILICON));
        assertEquals("no_wifi", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.UNKNOWN, "untrusted_status"), DashcamProfile.HISILICON));
        assertEquals("unknown", DashcamStatusPublisher.stateValue(
                snap(DashcamGalleryRepository.StateKind.UNKNOWN, "camera_busy"), DashcamProfile.NOVATEK));
        assertEquals("unknown", DashcamStatusPublisher.stateValue(null, DashcamProfile.NOVATEK));
    }

    @Test
    public void cardCapacityIsReadOnceAndThenEverySixHours() {
        long now = 100_000_000L;
        assertTrue(DashcamStatusPublisher.shouldRefreshTotal("", 0L, now));
        assertTrue(DashcamStatusPublisher.shouldRefreshTotal(null, now, now));
        assertFalse(DashcamStatusPublisher.shouldRefreshTotal("31000", now - 60_000L, now));
        assertTrue(DashcamStatusPublisher.shouldRefreshTotal(
                "31000", now - DashcamStatusPublisher.TOTAL_REFRESH_MS, now));
        // Clock moved backwards (camera/head unit time reset): re-read instead of waiting forever.
        assertTrue(DashcamStatusPublisher.shouldRefreshTotal("31000", now + 60_000L, now));
    }

    @Test
    public void wifiSsidIsPublishedWithoutQuotesOrPlaceholders() {
        assertEquals("ExampleNet", DashcamStatusPublisher.wifiSsidValue("\"ExampleNet\""));
        assertEquals("ExampleNet", DashcamStatusPublisher.wifiSsidValue("ExampleNet"));
        assertEquals("", DashcamStatusPublisher.wifiSsidValue("<unknown ssid>"));
        assertEquals("", DashcamStatusPublisher.wifiSsidValue("\"<unknown ssid>\""));
        assertEquals("", DashcamStatusPublisher.wifiSsidValue("\"\""));
        assertEquals("", DashcamStatusPublisher.wifiSsidValue(null));
    }

    @Test
    public void cameraApIsTheSubnetTheRememberedSsidOrAnAnsweringHisilicon() {
        assertTrue(DashcamStatusPublisher.onCameraAp(true, null, null, false));
        assertTrue(DashcamStatusPublisher.onCameraAp(false, "\"CARDV-0000\"", "CARDV-0000", false));
        assertTrue(DashcamStatusPublisher.onCameraAp(false, "HomeNet", null, true));
        assertFalse(DashcamStatusPublisher.onCameraAp(false, "HomeNet", "CARDV-0000", false));
        assertFalse(DashcamStatusPublisher.onCameraAp(false, null, null, false));
        assertFalse(DashcamStatusPublisher.onCameraAp(false, "<unknown ssid>", "<unknown ssid>", false));
    }

    @Test
    public void linkDerivation() {
        // No Wi-Fi wins over everything else.
        assertEquals("no_wifi", DashcamStatusPublisher.linkValue(false, "ExampleNet", true, "ExampleNet", true));
        // Camera's own Wi-Fi, whether or not the read worked.
        assertEquals("camera_ap", DashcamStatusPublisher.linkValue(true, "CARDV-0000", true, "ExampleNet", true));
        assertEquals("camera_ap", DashcamStatusPublisher.linkValue(true, "CARDV-0000", true, "ExampleNet", false));
        // Configured station network.
        assertEquals("same_network", DashcamStatusPublisher.linkValue(true, "\"ExampleNet\"", false, "ExampleNet", true));
        assertEquals("camera_missing", DashcamStatusPublisher.linkValue(true, "ExampleNet", false, "ExampleNet", false));
        // Any other Wi-Fi, also when no station network is configured.
        assertEquals("other_wifi", DashcamStatusPublisher.linkValue(true, "HomeNet", false, "ExampleNet", false));
        assertEquals("other_wifi", DashcamStatusPublisher.linkValue(true, "HomeNet", false, "ExampleNet", true));
        assertEquals("other_wifi", DashcamStatusPublisher.linkValue(true, "HomeNet", false, null, false));
        assertEquals("other_wifi", DashcamStatusPublisher.linkValue(true, "HomeNet", false, "  ", false));
        // SSID hidden by Android: a camera answering off its AP can only be on the station network.
        assertEquals("same_network", DashcamStatusPublisher.linkValue(true, "<unknown ssid>", false, "ExampleNet", true));
        assertEquals("other_wifi", DashcamStatusPublisher.linkValue(true, null, false, "ExampleNet", false));
        assertEquals("other_wifi", DashcamStatusPublisher.linkValue(true, null, false, null, true));
        // SSID match is exact.
        assertEquals("other_wifi", DashcamStatusPublisher.linkValue(true, "examplenet", false, "ExampleNet", true));
    }

    @Test
    public void cameraIpFollowsTheLink() {
        assertEquals("192.168.1.254", DashcamStatusPublisher.cameraIpValue("camera_ap", DashcamProfile.NOVATEK, null));
        assertEquals("192.168.0.1", DashcamStatusPublisher.cameraIpValue("camera_ap", DashcamProfile.HISILICON, null));
        assertEquals("10.0.0.23", DashcamStatusPublisher.cameraIpValue("same_network", DashcamProfile.NOVATEK, "10.0.0.23"));
        assertEquals("", DashcamStatusPublisher.cameraIpValue("same_network", DashcamProfile.NOVATEK, null));
        assertEquals("", DashcamStatusPublisher.cameraIpValue("same_network", DashcamProfile.NOVATEK, "8.8.8.8"));
        assertEquals("", DashcamStatusPublisher.cameraIpValue("camera_missing", DashcamProfile.NOVATEK, "10.0.0.23"));
        assertEquals("", DashcamStatusPublisher.cameraIpValue("other_wifi", DashcamProfile.NONE, null));
        assertEquals("", DashcamStatusPublisher.cameraIpValue("no_wifi", DashcamProfile.NONE, null));
    }
}
