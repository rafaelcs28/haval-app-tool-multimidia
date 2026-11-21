package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Which camera protocol the current Wi-Fi speaks, chosen by the Wi-Fi's own IPv4 subnet:
 * 192.168.1.x → Novatek CarDV at 192.168.1.254 (observed in the car, "CARDV-xxxx");
 * anything else → the HiSilicon profile at 192.168.0.1 (validated on another unit). A home router
 * on 192.168.0.x therefore stays HISILICON and fails its strict state parser, as before.
 */
public enum DashcamProfile {
    HISILICON,
    NOVATEK,
    NONE;

    public static DashcamProfile detect(Context context) {
        Network wifi = DashcamHisiliconClient.findCameraNetwork(context);
        if (wifi == null) return NONE;
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties properties = manager == null ? null : manager.getLinkProperties(wifi);
        if (properties != null) {
            for (LinkAddress address : properties.getLinkAddresses()) {
                if (isNovatekSubnet(address.getAddress())) return NOVATEK;
            }
        }
        return HISILICON;
    }

    static boolean isNovatekSubnet(InetAddress address) {
        if (!(address instanceof Inet4Address)) return false;
        byte[] b = address.getAddress();
        return (b[0] & 0xff) == 192 && (b[1] & 0xff) == 168 && (b[2] & 0xff) == 1;
    }
}
