package br.com.redesurftank.havalshisuku.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import br.com.redesurftank.App;
import br.com.redesurftank.havalshisuku.managers.DashcamStationPolicy;
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys;

/**
 * Device-only provisioning of the "Rede da câmera" station network, so a head unit can be
 * pre-filled over adb without shipping any network in the app. Callers are restricted by the
 * manifest's {@code android.permission.DUMP} (adb shell / system only; third-party apps cannot hold
 * it):
 *
 * <pre>
 * adb shell am broadcast -a br.com.redesurftank.havalshisuku.DASHCAM_STATION_CONFIG \
 *   -n br.com.redesurftank.havalshisuku/.broadcastReceivers.DashcamStationConfigReceiver \
 *   --es ssid NOME --es password SENHA --es auto 1
 * </pre>
 *
 * {@code --es command move} / {@code --es command return} start a camera move / return (in the
 * background; the result goes to the log and the card).
 *
 * {@code ssid} and {@code password} go together and are validated with
 * {@link DashcamStationPolicy#validate}; {@code auto} (boolean or "1"/"true"/"0"/"false") sets
 * "Fazer isso sozinho a cada partida". Values land in the same prefs the card reads. The password
 * (and the SSID) never reach the log or the broadcast result.
 */
public final class DashcamStationConfigReceiver extends BroadcastReceiver {
    private static final String TAG = "DashcamStationConfig";
    public static final String ACTION = "br.com.redesurftank.havalshisuku.DASHCAM_STATION_CONFIG";
    public static final String EXTRA_SSID = "ssid";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_AUTO = "auto";
    /** "move" (camera to the station network) or "return" (back to its own Wi-Fi); runs in background. */
    public static final String EXTRA_COMMAND = "command";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        try {
            Bundle extras = intent.getExtras();
            Object commandValue = extras == null ? null : extras.get(EXTRA_COMMAND);
            if (commandValue != null) {
                runCommand(context, String.valueOf(commandValue).trim());
                return;
            }
            Object ssidValue = extras == null ? null : extras.get(EXTRA_SSID);
            Object passwordValue = extras == null ? null : extras.get(EXTRA_PASSWORD);
            Object autoValue = extras == null ? null : extras.get(EXTRA_AUTO);
            boolean hasCredentials = ssidValue != null || passwordValue != null;
            Boolean auto = DashcamStationPolicy.parseAutoFlag(autoValue);
            if (autoValue != null && auto == null) {
                reply(false, "auto_invalid");
                return;
            }
            if (!hasCredentials && auto == null) {
                reply(false, "nothing_to_set");
                return;
            }
            SharedPreferences prefs = App.getDeviceProtectedContext()
                    .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (hasCredentials) {
                String ssid = ssidValue instanceof String ? (String) ssidValue : null;
                String password = passwordValue instanceof String ? (String) passwordValue : null;
                String invalid = DashcamStationPolicy.validate(ssid, password);
                if (invalid != null) {
                    Log.w(TAG, "rejected: " + invalid);
                    reply(false, invalid);
                    return;
                }
                editor.putString(SharedPreferencesKeys.DASHCAM_STATION_SSID.getKey(), ssid)
                        .putString(SharedPreferencesKeys.DASHCAM_STATION_PASSWORD.getKey(), password);
            }
            if (auto != null) {
                editor.putBoolean(SharedPreferencesKeys.ENABLE_DASHCAM_AUTO_STATION.getKey(), auto);
            }
            if (!editor.commit()) {
                reply(false, "write_failed");
                return;
            }
            String autoText = auto == null ? "unchanged" : String.valueOf(auto);
            Log.i(TAG, (hasCredentials ? "ssid set" : "ssid unchanged") + ", auto=" + autoText);
            reply(true, (hasCredentials ? "ssid set" : "ssid unchanged") + ", auto=" + autoText);
        } catch (RuntimeException e) {
            Log.e(TAG, "provisioning failed: " + e.getClass().getSimpleName());
            reply(false, "error");
        }
    }

    /**
     * Starts a move/return right away and answers "started"; the switch itself takes up to ~2 min
     * (camera Wi-Fi restart, head unit join, discovery), longer than a broadcast may wait. The
     * outcome is in the log (tag DashcamNetwork) and on the "Rede da câmera" card.
     */
    private void runCommand(Context context, String command) {
        br.com.redesurftank.havalshisuku.managers.DashcamNetworkManager network =
                br.com.redesurftank.havalshisuku.managers.DashcamNetworkManager.getInstance(context);
        if (network.isBusy()) {
            reply(false, "busy");
            return;
        }
        switch (command) {
            case "move":
                network.moveToStation(r -> Log.i(TAG, "adb move: ok=" + r.ok + " code=" + r.code));
                reply(true, "move started");
                return;
            case "return":
                network.returnToAp(r -> Log.i(TAG, "adb return: ok=" + r.ok + " code=" + r.code));
                reply(true, "return started");
                return;
            default:
                reply(false, "command_invalid");
        }
    }

    /** Shown by {@code am broadcast} as the result; never carries the SSID or the password. */
    private void reply(boolean ok, String detail) {
        if (!isOrderedBroadcast()) return;
        setResultCode(ok ? 1 : 0);
        setResultData((ok ? "ok: " : "error: ") + detail);
    }
}
