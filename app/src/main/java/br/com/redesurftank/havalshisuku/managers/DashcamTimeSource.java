package br.com.redesurftank.havalshisuku.managers;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Trusted UTC for the camera clock. The dashcam Wi-Fi has no internet, so an NTP sample is taken
 * whenever any validated internet network exists and kept against {@code elapsedRealtime}; while
 * the head unit sits on the camera Wi-Fi the cached sample still gives the current time. A recent
 * GPS fix is the fallback. The Android wall clock is never trusted on its own.
 */
final class DashcamTimeSource {
    private static final String[] NTP_HOSTS = {"a.st1.ntp.br", "time.google.com", "pool.ntp.org"};
    private static final int NTP_PORT = 123;
    private static final int NTP_TIMEOUT_MS = 2_000;
    private static final long SAMPLE_REFRESH_MS = 6L * 60 * 60 * 1000;
    private static final long SAMPLE_MAX_AGE_MS = 24L * 60 * 60 * 1000;
    private static final long MIN_ATTEMPT_INTERVAL_MS = 10L * 60 * 1000;
    private static final long GPS_MAX_AGE_MS = 120_000L;

    private final Context context;
    private long sampleUtcMs;
    private long sampleElapsedMs;
    private long lastAttemptElapsedMs = -MIN_ATTEMPT_INTERVAL_MS;
    private String lastSource = "none";

    DashcamTimeSource(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Opportunistic NTP refresh; call from a background thread. */
    synchronized void refreshIfDue() {
        long now = SystemClock.elapsedRealtime();
        boolean fresh = sampleElapsedMs > 0 && now - sampleElapsedMs < SAMPLE_REFRESH_MS;
        if (fresh || now - lastAttemptElapsedMs < MIN_ATTEMPT_INTERVAL_MS) return;
        lastAttemptElapsedMs = now;
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return;
        for (Network network : manager.getAllNetworks()) {
            NetworkCapabilities caps = manager.getNetworkCapabilities(network);
            if (caps == null
                    || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                continue;
            }
            for (String host : NTP_HOSTS) {
                if (query(network, host)) return;
            }
        }
    }

    private boolean query(Network network, String host) {
        try (DatagramSocket socket = new DatagramSocket()) {
            network.bindSocket(socket);
            socket.setSoTimeout(NTP_TIMEOUT_MS);
            InetAddress address = network.getByName(host);
            long sentWall = System.currentTimeMillis();
            long sentElapsed = SystemClock.elapsedRealtime();
            byte[] request = DashcamSntp.buildRequest(sentWall);
            socket.send(new DatagramPacket(request, request.length, address, NTP_PORT));
            byte[] reply = new byte[DashcamSntp.PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(reply, reply.length);
            socket.receive(packet);
            long receivedElapsed = SystemClock.elapsedRealtime();
            long receivedWall = sentWall + (receivedElapsed - sentElapsed);
            Long offset = DashcamSntp.offsetMs(reply, packet.getLength(), request, sentWall,
                    receivedWall);
            if (offset == null) return false;
            sampleUtcMs = receivedWall + offset;
            sampleElapsedMs = receivedElapsed;
            lastSource = "ntp";
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Current UTC from NTP (cached) or a recent GPS fix; null when neither is trustworthy. */
    synchronized Long currentUtcMs() {
        long now = SystemClock.elapsedRealtime();
        if (sampleElapsedMs > 0 && now - sampleElapsedMs < SAMPLE_MAX_AGE_MS) {
            lastSource = "ntp";
            return sampleUtcMs + (now - sampleElapsedMs);
        }
        Long gps = gpsUtcMs();
        if (gps != null) {
            lastSource = "gps";
            return gps;
        }
        lastSource = "none";
        return null;
    }

    synchronized String lastSource() {
        return lastSource;
    }

    private Long gpsUtcMs() {
        try {
            LocationManager manager =
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (manager == null) return null;
            Location fix = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (fix == null || fix.getTime() <= 0) return null;
            long ageMs = (SystemClock.elapsedRealtimeNanos() - fix.getElapsedRealtimeNanos())
                    / 1_000_000L;
            if (ageMs < 0 || ageMs > GPS_MAX_AGE_MS) return null;
            return fix.getTime() + ageMs;
        } catch (SecurityException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
