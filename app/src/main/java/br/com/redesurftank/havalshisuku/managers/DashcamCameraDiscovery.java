package br.com.redesurftank.havalshisuku.managers;

import android.net.Network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Finds the Novatek camera on a shared (station) network, where it gets a DHCP address.
 *
 * <ol>
 *   <li>Look its MAC (remembered while on the camera AP) up in {@code /proc/net/arp} (readable on
 *       the head unit's Android 9).</li>
 *   <li>Absent: a bounded concurrent TCP-80 connect sweep of the /24 (~3 s) fills the ARP table,
 *       then the table is read again.</li>
 *   <li>Still absent (MAC unknown or not answering ARP): probe {@code cmd=3016} on the hosts that
 *       had port 80 open.</li>
 * </ol>
 * Everything static and pure here is unit-tested; the I/O parts only connect to the /24 of the
 * Wi-Fi network they are given, always through that network's socket factory.
 */
final class DashcamCameraDiscovery {
    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}$");
    private static final Pattern MAC = Pattern.compile("^[0-9a-f]{2}(:[0-9a-f]{2}){5}$");
    private static final String ZERO_MAC = "00:00:00:00:00:00";
    static final String ARP_PATH = "/proc/net/arp";
    private static final int ARP_FLAG_COMPLETE = 0x2;
    private static final int SWEEP_THREADS = 64;
    private static final int SWEEP_CONNECT_TIMEOUT_MS = 700;
    static final long SWEEP_BUDGET_MS = 3_000L;
    /** At most this many port-80 hosts get a {@code 3016} probe. */
    private static final int MAX_PROBES = 16;

    /** True when {@code host} answers like a Novatek camera. */
    interface Probe {
        boolean isNovatek(String host);
    }

    static final class Found {
        final String host;
        /** "arp", "sweep_arp" or "probe". */
        final String method;
        /** MAC seen for the host in ARP after the probe, or null. */
        final String mac;

        Found(String host, String method, String mac) {
            this.host = host;
            this.method = method;
            this.mac = mac;
        }
    }

    private DashcamCameraDiscovery() {}

    // ----------------------------------------------------------------------------- pure helpers

    static boolean isIpv4Literal(String value) {
        return value != null && IPV4.matcher(value).matches();
    }

    static int[] octets(String ip) {
        if (!isIpv4Literal(ip)) return null;
        String[] parts = ip.split("\\.");
        int[] out = new int[4];
        for (int i = 0; i < 4; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }

    /** RFC 1918 address (10/8, 172.16/12, 192.168/16): the only kind a camera host may have. */
    static boolean isPrivateIpv4(String ip) {
        int[] o = octets(ip);
        if (o == null) return false;
        return o[0] == 10 || (o[0] == 172 && o[1] >= 16 && o[1] <= 31) || (o[0] == 192 && o[1] == 168);
    }

    static boolean sameSlash24(String a, String b) {
        int[] x = octets(a);
        int[] y = octets(b);
        return x != null && y != null && x[0] == y[0] && x[1] == y[1] && x[2] == y[2];
    }

    /** The other usable hosts (.1–.254) of {@code ownIp}'s /24, nearest to the low end first. */
    static List<String> sweepHosts(String ownIp) {
        int[] o = octets(ownIp);
        if (o == null) return Collections.emptyList();
        List<String> hosts = new ArrayList<>(253);
        String prefix = o[0] + "." + o[1] + "." + o[2] + ".";
        for (int i = 1; i <= 254; i++) {
            if (i != o[3]) hosts.add(prefix + i);
        }
        return hosts;
    }

    /** Lower-case {@code aa:bb:cc:dd:ee:ff}; null when not a usable MAC. */
    static String normalizeMac(String mac) {
        if (mac == null) return null;
        String value = mac.trim().toLowerCase(Locale.ROOT).replace('-', ':');
        if (!MAC.matcher(value).matches() || ZERO_MAC.equals(value)) return null;
        return value;
    }

    /**
     * IP → MAC from {@code /proc/net/arp}, only complete entries (flag 0x2) with a real MAC.
     * Optional {@code device} limits it to one interface (null = any).
     */
    static Map<String, String> parseArp(String content, String device) {
        Map<String, String> table = new LinkedHashMap<>();
        if (content == null) return table;
        String[] lines = content.split("\n");
        for (int i = 1; i < lines.length; i++) { // first line is the header
            String[] fields = lines[i].trim().split("\\s+");
            if (fields.length < 6) continue;
            String ip = fields[0];
            if (!isIpv4Literal(ip)) continue;
            int flags;
            try {
                flags = Integer.decode(fields[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            if ((flags & ARP_FLAG_COMPLETE) == 0) continue;
            String mac = normalizeMac(fields[3]);
            if (mac == null) continue;
            if (device != null && !device.equals(fields[5])) continue;
            table.put(ip, mac);
        }
        return table;
    }

    /** First IP with {@code mac} inside {@code ownIp}'s /24 (when given); null when absent. */
    static String ipForMac(Map<String, String> arp, String mac, String ownIp) {
        String wanted = normalizeMac(mac);
        if (wanted == null || arp == null) return null;
        for (Map.Entry<String, String> entry : arp.entrySet()) {
            if (!wanted.equals(entry.getValue())) continue;
            if (ownIp != null && !sameSlash24(entry.getKey(), ownIp)) continue;
            return entry.getKey();
        }
        return null;
    }

    static String macForIp(Map<String, String> arp, String ip) {
        return arp == null ? null : arp.get(ip);
    }

    // ---------------------------------------------------------------------------------------- I/O

    /** {@code /proc/net/arp}, or null when unreadable (newer Android blocks it). */
    static String readArp() {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(ARP_PATH))) {
            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null && lines++ < 1024) out.append(line).append('\n');
            return out.toString();
        } catch (Exception e) {
            return null;
        }
    }

    static Map<String, String> arpTable() {
        return parseArp(readArp(), null);
    }

    /** ARP only, no traffic: the camera's current IP in {@code ownIp}'s /24, or null. */
    static String lookup(String mac, String ownIp) {
        return ipForMac(arpTable(), mac, ownIp);
    }

    /**
     * Full discovery (ARP, sweep, ARP, probe). Blocking, a few seconds; never call it on the main
     * thread. {@code mac} may be null (then only the probe can find the camera).
     */
    static Found discover(Network network, String ownIp, String mac, Probe probe) {
        if (network == null || !isIpv4Literal(ownIp)) return null;
        String host = lookup(mac, ownIp);
        if (host != null) return new Found(host, "arp", normalizeMac(mac));

        List<String> open = sweep(network, sweepHosts(ownIp));

        host = lookup(mac, ownIp);
        if (host != null) return new Found(host, "sweep_arp", normalizeMac(mac));

        if (probe == null) return null;
        int probes = 0;
        for (String candidate : open) {
            if (probes++ >= MAX_PROBES) break;
            if (probe.isNovatek(candidate)) {
                return new Found(candidate, "probe", macForIp(arpTable(), candidate));
            }
        }
        return null;
    }

    /** Hosts with TCP 80 accepting, in sweep order; bounded by {@link #SWEEP_BUDGET_MS}. */
    private static List<String> sweep(Network network, List<String> hosts) {
        List<String> open = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(SWEEP_THREADS, runnable -> {
            Thread thread = new Thread(runnable, "DashcamSweep");
            thread.setDaemon(true);
            return thread;
        });
        long deadline = System.currentTimeMillis() + SWEEP_BUDGET_MS;
        try {
            for (String host : hosts) {
                pool.execute(() -> {
                    if (System.currentTimeMillis() >= deadline) return;
                    try (Socket socket = network.getSocketFactory().createSocket()) {
                        socket.connect(new InetSocketAddress(InetAddress.getByName(host), DashcamCameraHttp.PORT),
                                SWEEP_CONNECT_TIMEOUT_MS);
                        open.add(host);
                    } catch (Exception ignored) {
                        // closed, filtered or absent: the ARP request it caused is what matters
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(Math.max(0, deadline - System.currentTimeMillis()) + 200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        synchronized (open) {
            List<String> sorted = new ArrayList<>(open);
            sorted.sort((a, b) -> Integer.compare(octets(a)[3], octets(b)[3]));
            return sorted;
        }
    }
}
