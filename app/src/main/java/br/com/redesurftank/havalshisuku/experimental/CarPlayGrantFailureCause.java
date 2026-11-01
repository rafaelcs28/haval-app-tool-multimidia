package br.com.redesurftank.havalshisuku.experimental;

import android.os.DeadObjectException;
import android.os.RemoteException;

/**
 * Diagnostic-only, pure classifier for the cause swallowed by the clean-map consumer's GRANT_FAILED
 * catch (CarPlaySecondarySurfaceConsumer). It maps a Throwable to a FIXED, bounded vocabulary of
 * category tokens — it NEVER emits a free getMessage() string (unknown messages collapse to a generic
 * category), so no secret, token or arbitrary text can leak into the diagnostic log.
 *
 * Contract (reviewer constraints, agreed with Mumu):
 *  - Allowed categories only; unknown type/message -> "unknown"/"*_other" with no extra text.
 *  - An activation code is appended ONLY when the message is exactly the known
 *    "Native runtime activation failed: <N>" format AND <N> parses to the validated contract range;
 *    it is never inferred from a generic GRANT_FAILED.
 *  - Pure: no Android framework calls beyond instanceof, no clock, no I/O; never throws (a classifier
 *    fault must never break the consumer's close/revoke cleanup). Fully JVM-unit-testable.
 */
public final class CarPlayGrantFailureCause {

    private CarPlayGrantFailureCause() {
    }

    private static final String ACTIVATION_PREFIX = "Native runtime activation failed: ";
    // The activation value is produced by our own native layer (CarPlaySurfaceEndpoint), not by a
    // remote peer; the range is a sanity bound so a malformed/implausible value never reaches the log.
    private static final int ACTIVATION_MIN = -999;
    private static final int ACTIVATION_MAX = 999;

    public static String describe(Throwable failure) {
        try {
            return classify(failure);
        } catch (Throwable classifierFault) {
            return "unknown"; // A diagnostic classifier must never surface its own fault.
        }
    }

    private static String classify(Throwable failure) {
        if (failure == null) return "none";
        // Transport/linkage: the message carries no useful, bounded category.
        if (failure instanceof DeadObjectException) return "remote_dead";
        if (failure instanceof RemoteException) return "remote";
        if (failure instanceof LinkageError) return "linkage";
        final String message = failure.getMessage();
        if (failure instanceof SecurityException) {
            if ("Unauthorized Surface transport".equals(message)) return "security_unauthorized";
            if ("Invalid session token or closed endpoint".equals(message)) return "security_token";
            return "security_other";
        }
        if (failure instanceof IllegalArgumentException) {
            if ("Invalid Surface grant".equals(message)) return "invalid_grant";
            if ("Trailing data".equals(message)) return "trailing_data";
            return "illegal_arg_other";
        }
        if (failure instanceof IllegalStateException) {
            if ("Native Surface import failed".equals(message)) return "native_import_failed";
            if ("Endpoint already consumed".equals(message)) return "endpoint_consumed";
            if ("Unknown generation".equals(message)) return "unknown_generation";
            Integer code = activationCode(message);
            if (code != null) return "native_activation_failed:" + code;
            return "illegal_state_other";
        }
        if (failure instanceof NullPointerException) return "npe";
        return "unknown";
    }

    /**
     * Returns the activation code only when {@code message} is exactly the known activation-failure
     * format and the trailing value parses to the validated contract range; otherwise null. Never
     * emits any other substring of the message.
     */
    static Integer activationCode(String message) {
        if (message == null || !message.startsWith(ACTIVATION_PREFIX)) return null;
        String tail = message.substring(ACTIVATION_PREFIX.length());
        if (tail.isEmpty() || tail.length() > 4) return null; // bounds the parse; "-999".length()==4
        int start = 0;
        if (tail.charAt(0) == '-') {
            if (tail.length() == 1) return null;
            start = 1;
        }
        for (int i = start; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (c < '0' || c > '9') return null;
        }
        try {
            int value = Integer.parseInt(tail);
            if (value < ACTIVATION_MIN || value > ACTIVATION_MAX) return null;
            return value;
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }
}
