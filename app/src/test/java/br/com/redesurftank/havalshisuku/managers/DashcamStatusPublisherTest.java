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
}
