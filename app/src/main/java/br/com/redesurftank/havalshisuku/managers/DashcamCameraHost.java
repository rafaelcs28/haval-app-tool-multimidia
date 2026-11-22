package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

import java.net.Inet4Address;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;

/**
 * Where the Novatek camera is right now: {@link #AP_HOST} on its own Wi-Fi, or the address it got
 * by DHCP on the "station" network (the SSID configured in the "Rede da câmera" card). Every Novatek
 * request (XML commands, video stream, clip download) asks this resolver instead of using a fixed
 * address. HiSilicon is untouched: it stays at 192.168.0.1.
 *
 * <p>State kept in the device-protected {@link DashcamGalleryRepository#STATE_PREFS} (out of
 * backup): camera MAC and camera AP SSID (learned while on the camera AP) and the last station IP.
 */
public final class DashcamCameraHost {
    public static final String AP_HOST = DashcamNovatekProtocol.HOST;
    static final String KEY_CAMERA_MAC = "novatek_camera_mac";
    static final String KEY_STATION_HOST = "novatek_station_host";
    static final String KEY_CAMERA_AP_SSID = "novatek_camera_ap_ssid";
    /** A full sweep costs ~3 s on the camera lock; never more often than this unless forced. */
    static final long DISCOVERY_COOLDOWN_MS = 60_000L;
    private static final long REMEMBER_INTERVAL_MS = 10L * 60 * 1000;

    public enum Place { NO_WIFI, CAMERA_AP, STATION, OTHER }

    private static volatile String lastHost = AP_HOST;
    private static volatile long lastDiscoveryElapsed;
    private static volatile long lastRememberElapsed;

    private DashcamCameraHost() {}

    /** Snapshot of the Wi-Fi the camera traffic would use. */
    static final class Wifi {
        final Network network;
        /** Own IPv4 on that network, or null. */
        final String ownIp;
        /** Connected SSID without quotes, or null when unreadable. */
        final String ssid;
        final boolean onApSubnet;

        Wifi(Network network, String ownIp, String ssid, boolean onApSubnet) {
            this.network = network;
            this.ownIp = ownIp;
            this.ssid = ssid;
            this.onApSubnet = onApSubnet;
        }
    }

    // ----------------------------------------------------------------------------- pure helpers

    /** {@code "\"Name\""} → {@code Name}; the Android placeholders become null. */
    static String cleanSsid(String raw) {
        if (raw == null) return null;
        String ssid = raw.trim();
        if (ssid.length() >= 2 && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        if (ssid.isEmpty() || "<unknown ssid>".equals(ssid) || "0x".equals(ssid)) return null;
        return ssid;
    }

    /**
     * Which network the head unit is on. The SSID decides the station network; when Android hides
     * the SSID, a cached camera IP in our own /24 is taken as the station network instead.
     */
    static Place classify(boolean hasWifi, boolean onApSubnet, String currentSsid, String stationSsid,
            String cachedHost, String ownIp) {
        if (!hasWifi) return Place.NO_WIFI;
        if (onApSubnet) return Place.CAMERA_AP;
        if (stationSsid == null || stationSsid.isEmpty()) return Place.OTHER;
        if (currentSsid != null) return currentSsid.equals(stationSsid) ? Place.STATION : Place.OTHER;
        return cachedHost != null && DashcamCameraDiscovery.sameSlash24(cachedHost, ownIp)
                ? Place.STATION : Place.OTHER;
    }

    /** On the station network the profile is Novatek only once we know the camera (MAC or IP). */
    static boolean isNovatekStation(Place place, String cachedHost, String mac) {
        return place == Place.STATION && (cachedHost != null || mac != null);
    }

    /** Only the current camera host is allowed for Novatek paths; it must be a private IPv4 literal. */
    static boolean isAcceptableHost(String host) {
        return DashcamCameraDiscovery.isPrivateIpv4(host);
    }

    // ------------------------------------------------------------------------------- Android side

    static Wifi wifi(Context context) {
        Network network = DashcamHisiliconClient.findCameraNetwork(context);
        if (network == null) return null;
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties properties = manager == null ? null : manager.getLinkProperties(network);
        String ownIp = null;
        boolean onAp = false;
        if (properties != null) {
            for (LinkAddress address : properties.getLinkAddresses()) {
                if (!(address.getAddress() instanceof Inet4Address)) continue;
                if (DashcamProfile.isNovatekSubnet(address.getAddress())) onAp = true;
                if (ownIp == null) ownIp = address.getAddress().getHostAddress();
            }
        }
        return new Wifi(network, ownIp, currentSsid(context), onAp);
    }

    static String currentSsid(Context context) {
        try {
            WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi == null || wifi.getConnectionInfo() == null) return null;
            return cleanSsid(wifi.getConnectionInfo().getSSID());
        } catch (RuntimeException e) {
            return null; // missing permission or Wi-Fi service hiccup
        }
    }

    static SharedPreferences state() {
        return App.getDeviceProtectedContext()
                .getSharedPreferences(DashcamGalleryRepository.STATE_PREFS, Context.MODE_PRIVATE);
    }

    static SharedPreferences settings() {
        return App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
    }

    static String stationSsid() {
        String ssid = settings().getString(SharedPreferencesKeys.DASHCAM_STATION_SSID.getKey(), "");
        return ssid == null || ssid.trim().isEmpty() ? null : ssid;
    }

    static String stationPassword() {
        return settings().getString(SharedPreferencesKeys.DASHCAM_STATION_PASSWORD.getKey(), "");
    }

    static String cameraMac() {
        return DashcamCameraDiscovery.normalizeMac(state().getString(KEY_CAMERA_MAC, null));
    }

    static String cameraApSsid() {
        return state().getString(KEY_CAMERA_AP_SSID, null);
    }

    /** Cached station IP, only when it is still in our /24. */
    static String cachedStationHost(String ownIp) {
        String host = state().getString(KEY_STATION_HOST, null);
        if (!isAcceptableHost(host) || !DashcamCameraDiscovery.sameSlash24(host, ownIp)) return null;
        return host;
    }

    static Place place(Context context) {
        return place(wifi(context));
    }

    static Place place(Wifi wifi) {
        if (wifi == null) return Place.NO_WIFI;
        String cached = state().getString(KEY_STATION_HOST, null);
        return classify(true, wifi.onApSubnet, wifi.ssid, stationSsid(), cached, wifi.ownIp);
    }

    /** Novatek profile on the station network (see {@link #isNovatekStation}). */
    static boolean isNovatekStation(Context context) {
        Wifi wifi = wifi(context);
        Place place = place(wifi);
        if (place != Place.STATION) return false;
        return isNovatekStation(place, cachedStationHost(wifi.ownIp), cameraMac());
    }

    /** Last host a Novatek request used (AP address until the camera is seen elsewhere). */
    public static String lastNovatekHost() {
        return lastHost;
    }

    /**
     * Current Novatek host, or null when the camera is not reachable from this Wi-Fi. Cheap unless
     * {@code allowDiscovery} is true and nothing is known (then it may sweep for ~3 s, at most once
     * per {@link #DISCOVERY_COOLDOWN_MS}, or always when {@code force}). Never on the main thread.
     */
    static String resolve(Context context, boolean allowDiscovery, boolean force,
            DashcamCameraDiscovery.Probe probe) {
        Wifi wifi = wifi(context);
        Place place = place(wifi);
        if (place == Place.CAMERA_AP) {
            lastHost = AP_HOST;
            return AP_HOST;
        }
        if (place != Place.STATION || wifi.ownIp == null) return null;
        String mac = cameraMac();
        String cached = cachedStationHost(wifi.ownIp);
        String viaArp = mac == null ? null : DashcamCameraDiscovery.lookup(mac, wifi.ownIp);
        if (viaArp != null) {
            if (!viaArp.equals(cached)) state().edit().putString(KEY_STATION_HOST, viaArp).apply();
            lastHost = viaArp;
            return viaArp;
        }
        if (cached != null) {
            lastHost = cached;
            return cached;
        }
        if (!allowDiscovery) return null;
        long now = SystemClock.elapsedRealtime();
        if (!force && lastDiscoveryElapsed > 0 && now - lastDiscoveryElapsed < DISCOVERY_COOLDOWN_MS) return null;
        lastDiscoveryElapsed = now;
        DashcamCameraDiscovery.Found found = DashcamCameraDiscovery.discover(wifi.network, wifi.ownIp, mac, probe);
        DashcamNetworkManager.log("discovery", found == null ? "not_found" : "found method=" + found.method);
        if (found == null || !isAcceptableHost(found.host)) return null;
        SharedPreferences.Editor editor = state().edit().putString(KEY_STATION_HOST, found.host);
        if (mac == null && found.mac != null) editor.putString(KEY_CAMERA_MAC, found.mac);
        editor.apply();
        lastHost = found.host;
        return found.host;
    }

    /** A request to {@code host} failed at transport level: drop the cached station IP. */
    static void reportFailure(String host) {
        if (host == null || AP_HOST.equals(host)) return;
        SharedPreferences prefs = state();
        if (host.equals(prefs.getString(KEY_STATION_HOST, null))) {
            prefs.edit().remove(KEY_STATION_HOST).apply();
        }
    }

    static void forgetStationHost() {
        state().edit().remove(KEY_STATION_HOST).apply();
    }

    /**
     * While on the camera AP, remember the camera MAC (ARP entry of {@link #AP_HOST}) and the AP
     * SSID, so the camera can be found on the station network and the head unit can come back.
     * Throttled unless {@code force}; cheap (reads one /proc file).
     */
    static void rememberApIdentity(Context context, boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (!force && lastRememberElapsed > 0 && now - lastRememberElapsed < REMEMBER_INTERVAL_MS) return;
        Wifi wifi = wifi(context);
        if (wifi == null || !wifi.onApSubnet) return;
        lastRememberElapsed = now;
        String mac = DashcamCameraDiscovery.macForIp(DashcamCameraDiscovery.arpTable(), AP_HOST);
        SharedPreferences prefs = state();
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;
        if (mac != null && !mac.equals(prefs.getString(KEY_CAMERA_MAC, null))) {
            editor.putString(KEY_CAMERA_MAC, mac);
            changed = true;
        }
        if (wifi.ssid != null && !wifi.ssid.equals(prefs.getString(KEY_CAMERA_AP_SSID, null))) {
            editor.putString(KEY_CAMERA_AP_SSID, wifi.ssid);
            changed = true;
        }
        if (changed) {
            editor.apply();
            DashcamNetworkManager.log("remember", "mac=" + (mac != null) + " ap_ssid=" + (wifi.ssid != null));
        }
    }
}
