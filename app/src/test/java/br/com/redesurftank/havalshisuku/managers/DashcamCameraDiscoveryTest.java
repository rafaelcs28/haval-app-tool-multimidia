package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Fake MACs and addresses only. */
public class DashcamCameraDiscoveryTest {
    private static final String CAMERA_MAC = "02:11:22:33:44:55";
    private static final String ARP = "IP address       HW type     Flags       HW address            Mask     Device\n"
            + "10.20.30.1       0x1         0x2         02:aa:bb:cc:dd:01     *        wlan0\n"
            + "10.20.30.78      0x1         0x2         02:11:22:33:44:55     *        wlan0\n"
            + "10.20.30.90      0x1         0x0         00:00:00:00:00:00     *        wlan0\n"
            + "10.20.30.91      0x1         0x0         02:aa:bb:cc:dd:91     *        wlan0\n"
            + "192.168.1.254    0x1         0x2         02:11:22:33:44:55     *        wlan1\n"
            + "garbage line\n";

    @Test
    public void ipv4Literals() {
        assertTrue(DashcamCameraDiscovery.isIpv4Literal("10.0.0.1"));
        assertTrue(DashcamCameraDiscovery.isIpv4Literal("192.168.1.254"));
        assertFalse(DashcamCameraDiscovery.isIpv4Literal("256.1.1.1"));
        assertFalse(DashcamCameraDiscovery.isIpv4Literal("10.0.0"));
        assertFalse(DashcamCameraDiscovery.isIpv4Literal("camera.local"));
        assertFalse(DashcamCameraDiscovery.isIpv4Literal("10.0.0.1 "));
        assertFalse(DashcamCameraDiscovery.isIpv4Literal(null));
    }

    @Test
    public void privateRanges() {
        assertTrue(DashcamCameraDiscovery.isPrivateIpv4("10.20.30.78"));
        assertTrue(DashcamCameraDiscovery.isPrivateIpv4("172.16.0.5"));
        assertTrue(DashcamCameraDiscovery.isPrivateIpv4("172.31.255.1"));
        assertTrue(DashcamCameraDiscovery.isPrivateIpv4("192.168.1.254"));
        assertFalse(DashcamCameraDiscovery.isPrivateIpv4("172.32.0.1"));
        assertFalse(DashcamCameraDiscovery.isPrivateIpv4("8.8.8.8"));
        assertFalse(DashcamCameraDiscovery.isPrivateIpv4("example.com"));
    }

    @Test
    public void subnetMath() {
        assertTrue(DashcamCameraDiscovery.sameSlash24("10.20.30.1", "10.20.30.254"));
        assertFalse(DashcamCameraDiscovery.sameSlash24("10.20.30.1", "10.20.31.1"));
        assertFalse(DashcamCameraDiscovery.sameSlash24(null, "10.20.31.1"));
        List<String> hosts = DashcamCameraDiscovery.sweepHosts("10.20.30.42");
        assertEquals(253, hosts.size());
        assertEquals("10.20.30.1", hosts.get(0));
        assertEquals("10.20.30.254", hosts.get(hosts.size() - 1));
        assertFalse(hosts.contains("10.20.30.42"));
        assertFalse(hosts.contains("10.20.30.0"));
        assertFalse(hosts.contains("10.20.30.255"));
        assertTrue(DashcamCameraDiscovery.sweepHosts("nope").isEmpty());
    }

    @Test
    public void macNormalization() {
        assertEquals(CAMERA_MAC, DashcamCameraDiscovery.normalizeMac("02:11:22:33:44:55"));
        assertEquals(CAMERA_MAC, DashcamCameraDiscovery.normalizeMac(" 02-11-22-33-44-55 "));
        assertEquals("02:ab:cd:ef:00:11", DashcamCameraDiscovery.normalizeMac("02:AB:CD:EF:00:11"));
        assertNull(DashcamCameraDiscovery.normalizeMac("00:00:00:00:00:00"));
        assertNull(DashcamCameraDiscovery.normalizeMac("02:11:22:33:44"));
        assertNull(DashcamCameraDiscovery.normalizeMac(null));
    }

    @Test
    public void parsesOnlyCompleteArpEntries() {
        Map<String, String> table = DashcamCameraDiscovery.parseArp(ARP, null);
        assertEquals(3, table.size());
        assertEquals(CAMERA_MAC, table.get("10.20.30.78"));
        assertNull("incomplete entry", table.get("10.20.30.90"));
        assertNull("flag 0x0", table.get("10.20.30.91"));
        assertEquals(1, DashcamCameraDiscovery.parseArp(ARP, "wlan1").size());
        assertTrue(DashcamCameraDiscovery.parseArp(null, null).isEmpty());
    }

    @Test
    public void findsCameraByMacInOwnSubnet() {
        Map<String, String> table = DashcamCameraDiscovery.parseArp(ARP, null);
        assertEquals("10.20.30.78", DashcamCameraDiscovery.ipForMac(table, "02:11:22:33:44:55", "10.20.30.5"));
        assertEquals("192.168.1.254", DashcamCameraDiscovery.ipForMac(table, CAMERA_MAC, "192.168.1.33"));
        assertNull("other subnet", DashcamCameraDiscovery.ipForMac(table, CAMERA_MAC, "172.16.0.5"));
        assertNull(DashcamCameraDiscovery.ipForMac(table, "02:99:99:99:99:99", "10.20.30.5"));
        assertNull(DashcamCameraDiscovery.ipForMac(table, null, "10.20.30.5"));
        assertEquals(CAMERA_MAC, DashcamCameraDiscovery.macForIp(table, "192.168.1.254"));
    }
}
