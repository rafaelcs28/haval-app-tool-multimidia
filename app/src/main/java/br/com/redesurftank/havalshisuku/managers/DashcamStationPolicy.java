package br.com.redesurftank.havalshisuku.managers;

import java.nio.charset.StandardCharsets;

/**
 * Pure rules for moving the Novatek camera between its own Wi-Fi (AP) and a shared "station"
 * network: input validation, the {@code 3032} command, the automatic-mode decision and the status
 * line. No Android types, so all of it is unit-tested.
 */
public final class DashcamStationPolicy {
    /** Novatek station-mode commands (verified on the car's TY670DLA335 firmware). */
    static final int CMD_SET_STATION = 3032;
    static final int CMD_WIFI_MODE = 3033;
    static final int CMD_WIFI_RESTART = 3018;
    /** Novatek doc: keep the passphrase within 26 characters. WPA2 needs at least 8. */
    static final int MAX_PASSWORD = 26;
    static final int MIN_PASSWORD = 8;
    static final int MAX_SSID_BYTES = 32;
    /** Automatic mode: at most one attempt per this window (and one per run/ignition). */
    /** After an automatic attempt that did not move the camera, wait this long before the next. */
    static final long AUTO_RETRY_WINDOW_MS = 5L * 60 * 1000;
    /** How often the automatic mode looks, only while the head unit is on the camera's own Wi-Fi. */
    static final long AUTO_CHECK_INTERVAL_MS = 60L * 1000;
    /** "Trazer câmera para o Wi-Fi próprio" holds the automatic mode off for this long. */
    static final long AUTO_PAUSE_AFTER_RETURN_MS = 30L * 60 * 1000;

    enum AutoAction { NONE, MOVE_TO_STATION, HOP_TO_AP_THEN_MOVE }

    private DashcamStationPolicy() {}

    /** Null when usable; otherwise an error code for {@link #validationMessage}. */
    public static String validate(String ssid, String password) {
        if (ssid == null || ssid.trim().isEmpty()) return "ssid_empty";
        if (ssid.indexOf(':') >= 0) return "ssid_colon";
        if (ssid.getBytes(StandardCharsets.UTF_8).length > MAX_SSID_BYTES) return "ssid_too_long";
        for (int i = 0; i < ssid.length(); i++) {
            if (Character.isISOControl(ssid.charAt(i))) return "ssid_invalid";
        }
        if (password == null || password.length() < MIN_PASSWORD) return "password_short";
        if (password.length() > MAX_PASSWORD) return "password_long";
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c < 0x20 || c > 0x7e) return "password_invalid";
        }
        return null;
    }

    /**
     * The {@code auto} extra of the device-only provisioning broadcast: a boolean ({@code --ez}) or
     * a string ({@code --es}) "1"/"true"/"0"/"false" (case-insensitive). Null when absent or not
     * understood, so the stored switch is left alone.
     */
    public static Boolean parseAutoFlag(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (!(value instanceof String)) return null;
        String v = ((String) value).trim().toLowerCase(java.util.Locale.ROOT);
        if (v.equals("1") || v.equals("true")) return Boolean.TRUE;
        if (v.equals("0") || v.equals("false")) return Boolean.FALSE;
        return null;
    }

    public static String validationMessage(String code) {
        if (code == null) return null;
        switch (code) {
            case "ssid_empty":
                return "Informe o nome da rede.";
            case "ssid_colon":
                return "O nome da rede não pode ter \":\" (a câmera usa esse caractere como separador).";
            case "ssid_too_long":
                return "O nome da rede passa de 32 bytes.";
            case "ssid_invalid":
                return "O nome da rede tem caracteres inválidos.";
            case "password_short":
                return "A senha precisa ter pelo menos 8 caracteres.";
            case "password_long":
                return "A câmera aceita senha de até 26 caracteres.";
            case "password_invalid":
                return "Use só letras, números e símbolos comuns na senha.";
            default:
                return "Configuração inválida.";
        }
    }

    /** RFC 3986 percent-encoding (UTF-8); only unreserved characters stay literal. */
    static String percentEncode(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~') {
                out.append((char) c);
            } else {
                out.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(c & 0xf, 16)));
            }
        }
        return out.toString();
    }

    /**
     * {@code 3032&str=SSID:PASSWORD} with each half percent-encoded and the separator kept literal,
     * so a plain alphanumeric SSID/password goes out exactly as verified on the car. Null when the
     * input does not validate.
     */
    static String stationCommand(String ssid, String password) {
        if (validate(ssid, password) != null) return null;
        return CMD_SET_STATION + "&str=" + percentEncode(ssid) + ":" + percentEncode(password);
    }

    /** {@code 3033&par=1} station, {@code par=0} back to the camera's own AP. */
    /** Pause between 3032, 3033 and 3018: back to back the camera joins but never answers. */
    static final long STEP_GAP_MS = 3_000L;
    /** A remembered step older than this is redone (the camera may have restarted meanwhile). */
    static final long STEP_VALID_MS = 10L * 60 * 1000;
    /** Result code of a move that sent some steps and continues on the next camera Wi-Fi visit. */
    static final String IN_PROGRESS = "in_progress";
    /** How soon the automatic mode continues a move left in progress. */
    static final long IN_PROGRESS_RETRY_MS = 5_000L;

    static boolean stepStillValid(long doneAtMs, long nowMs) {
        return doneAtMs > 0 && nowMs >= doneAtMs && nowMs - doneAtMs < STEP_VALID_MS;
    }

    static String wifiModeCommand(boolean station) {
        return CMD_WIFI_MODE + "&par=" + (station ? 1 : 0);
    }

    /**
     * Automatic mode, checked every {@link #AUTO_CHECK_INTERVAL_MS}. Two cases only (owner's rules):
     * <ul>
     *   <li>head unit on the camera's own Wi-Fi and the station SSID in range: move the camera;</li>
     *   <li>head unit on the station network, the camera not answering there and the camera's own
     *       Wi-Fi in range (the head unit joined the station before the camera was moved): hop to the
     *       camera Wi-Fi and move it.</li>
     * </ul>
     * Anywhere else nothing happens. A failed attempt waits {@link #AUTO_RETRY_WINDOW_MS}; a manual
     * "back to the camera Wi-Fi" holds it off for {@link #AUTO_PAUSE_AFTER_RETURN_MS}.
     *
     * @param msSinceLastAttempt -1 when none (a negative value from a clock change also counts as none)
     * @param msSinceManualReturn -1 when none
     */
    static AutoAction decideAuto(boolean enabled, DashcamCameraHost.Place place, boolean stationVisible,
            boolean cameraFoundOnStation, boolean apVisible, long msSinceLastAttempt, long msSinceManualReturn) {
        if (!enabled) return AutoAction.NONE;
        if (msSinceLastAttempt >= 0 && msSinceLastAttempt < AUTO_RETRY_WINDOW_MS) return AutoAction.NONE;
        if (msSinceManualReturn >= 0 && msSinceManualReturn < AUTO_PAUSE_AFTER_RETURN_MS) return AutoAction.NONE;
        if (place == DashcamCameraHost.Place.CAMERA_AP && stationVisible) return AutoAction.MOVE_TO_STATION;
        if (place == DashcamCameraHost.Place.STATION && !cameraFoundOnStation && apVisible) {
            return AutoAction.HOP_TO_AP_THEN_MOVE;
        }
        return AutoAction.NONE;
    }

    /** Status line of the "Rede da câmera" card. */
    static String statusText(DashcamCameraHost.Place place, String host, String ssid) {
        if (place == DashcamCameraHost.Place.CAMERA_AP) return "No Wi-Fi próprio (" + DashcamCameraHost.AP_HOST + ")";
        if (place == DashcamCameraHost.Place.STATION && host != null) {
            return "Na rede " + (ssid == null ? "configurada" : ssid) + " (" + host + ")";
        }
        return "Não encontrada";
    }
}
