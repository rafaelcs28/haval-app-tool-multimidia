package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import br.com.redesurftank.havalshisuku.managers.DashcamCameraHost.Place;
import java.util.Collections;
import org.junit.Test;

/** Placeholder network names and addresses only. */
public class DashcamCameraHostTest {
    @Test
    public void cleansSsid() {
        assertEquals("ExampleNet", DashcamCameraHost.cleanSsid("\"ExampleNet\""));
        assertEquals("ExampleNet", DashcamCameraHost.cleanSsid("ExampleNet"));
        assertNull(DashcamCameraHost.cleanSsid("<unknown ssid>"));
        assertNull(DashcamCameraHost.cleanSsid("\"\""));
        assertNull(DashcamCameraHost.cleanSsid(null));
    }

    @Test
    public void classifiesPlace() {
        assertEquals(Place.NO_WIFI, DashcamCameraHost.classify(false, false, null, "ExampleNet", null, null));
        assertEquals(Place.CAMERA_AP, DashcamCameraHost.classify(true, true, "CARDV-0000", "ExampleNet", null, "192.168.1.33"));
        assertEquals("AP wins even without station config", Place.CAMERA_AP,
                DashcamCameraHost.classify(true, true, null, null, null, "192.168.1.33"));
        assertEquals(Place.STATION, DashcamCameraHost.classify(true, false, "ExampleNet", "ExampleNet", null, "10.20.30.5"));
        assertEquals(Place.OTHER, DashcamCameraHost.classify(true, false, "HomeNet", "ExampleNet", "10.20.30.78", "10.20.30.5"));
        assertEquals("no station configured", Place.OTHER,
                DashcamCameraHost.classify(true, false, "ExampleNet", null, null, "10.20.30.5"));
        assertEquals("hidden SSID, cached camera in our /24", Place.STATION,
                DashcamCameraHost.classify(true, false, null, "ExampleNet", "10.20.30.78", "10.20.30.5"));
        assertEquals("hidden SSID, cached camera elsewhere", Place.OTHER,
                DashcamCameraHost.classify(true, false, null, "ExampleNet", "10.20.31.78", "10.20.30.5"));
    }

    @Test
    public void novatekOnStationNeedsAKnownCamera() {
        assertTrue(DashcamCameraHost.isNovatekStation(Place.STATION, "10.20.30.78", null));
        assertTrue(DashcamCameraHost.isNovatekStation(Place.STATION, null, "02:11:22:33:44:55"));
        assertFalse(DashcamCameraHost.isNovatekStation(Place.STATION, null, null));
        assertFalse(DashcamCameraHost.isNovatekStation(Place.OTHER, "10.20.30.78", "02:11:22:33:44:55"));
    }

    @Test
    public void onlyPrivateLiteralsAreCameraHosts() {
        assertTrue(DashcamCameraHost.isAcceptableHost("192.168.1.254"));
        assertTrue(DashcamCameraHost.isAcceptableHost("10.20.30.78"));
        assertFalse(DashcamCameraHost.isAcceptableHost("8.8.8.8"));
        assertFalse(DashcamCameraHost.isAcceptableHost("cardv.local"));
        assertFalse(DashcamCameraHost.isAcceptableHost(null));
    }

    @Test
    public void videoAllowListFollowsCurrentCameraHost() {
        String path = "/CARDV/Movie/20261115130916_034795.TS";
        assertTrue(DashcamVideoDataSource.isAllowed("192.168.1.254", path, "192.168.1.254"));
        assertTrue(DashcamVideoDataSource.isAllowed("10.20.30.78", path, "10.20.30.78"));
        assertFalse("stale host", DashcamVideoDataSource.isAllowed("192.168.1.254", path, "10.20.30.78"));
        assertFalse("camera not found", DashcamVideoDataSource.isAllowed("10.20.30.78", path, null));
        assertFalse("public host", DashcamVideoDataSource.isAllowed("8.8.8.8", path, "8.8.8.8"));
        assertFalse("bad path", DashcamVideoDataSource.isAllowed("10.20.30.78", "/CARDV/../etc/passwd", "10.20.30.78"));
        assertFalse("hisilicon path on novatek host",
                DashcamVideoDataSource.isAllowed("10.20.30.78", "/sd//norm/2026_01_15_100600_00.MP4", "10.20.30.78"));
        assertFalse("novatek path on hisilicon host",
                DashcamVideoDataSource.isAllowed("192.168.0.1", path, "192.168.1.254"));
    }

    @Test
    public void novatekCatalogCarriesItsHost() {
        DashcamNovatekProtocol.Entry entry = DashcamNovatekProtocol.entry("Movie", "20261115130916_034795.TS",
                "100", "2026/11/15 13:10:14", "32");
        DashcamCatalog catalog = DashcamCatalog.fromNovatek(Collections.singletonList(entry), "10.20.30.78");
        assertEquals("10.20.30.78", catalog.getRecordings().get(0).getHost());
        assertEquals("192.168.1.254",
                DashcamCatalog.fromNovatek(Collections.singletonList(entry)).getRecordings().get(0).getHost());
    }

    @Test
    public void wrongHostErrorsTriggerRediscovery() {
        assertTrue(DashcamNovatekClient.isWrongHostError("transport_SocketTimeoutException"));
        assertTrue(DashcamNovatekClient.isWrongHostError("http_404"));
        assertTrue(DashcamNovatekClient.isWrongHostError("malformed_http"));
        assertFalse(DashcamNovatekClient.isWrongHostError("malformed_reply"));
        assertFalse(DashcamNovatekClient.isWrongHostError(null));
    }
}
