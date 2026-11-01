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
    // The native activation contract returns 0..10 (8 == NATIVE_INSTALLED success, which never throws
    // this exception). Negatives and larger values are not valid returns and must never be reported.
    private static final int ACTIVATION_MIN = 0;
    private static final int ACTIVATION_MAX = 10;
    // diag5: the endpoint's unauthorized-transport message; may carry a bounded reason mask "[v1 m=<1..15>]".
    private static final String UNAUTHORIZED = "Unauthorized Surface transport";
    private static final String UNAUTHORIZED_MASK_PREFIX = UNAUTHORIZED + " [v1 m=";

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
            if (message != null && message.startsWith(UNAUTHORIZED)) {
                String label = unauthorizedMaskLabel(message); // diag5: which of the 4 auth conditions tripped
                return label == null ? "security_unauthorized" : "security_unauthorized:" + label;
            }
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
        if (tail.isEmpty() || tail.length() > 2) return null; // "10".length()==2; no sign, bounds the parse
        for (int i = 0; i < tail.length(); i++) {
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

    /**
     * Parse the diag5 reason descriptor: EXACTLY {@code <UNAUTHORIZED> [v1 m=<mask>]} with mask in 1..15.
     * Returns a bounded label built from the set bits ("uid","reply","flags","size", joined with '+');
     * null for the old generic message or any malformed/overflowing form (strict: integral match, no extras).
     */
    static String unauthorizedMaskLabel(String message) {
        if (message == null || !message.startsWith(UNAUTHORIZED_MASK_PREFIX) || !message.endsWith("]")) return null;
        String tail = message.substring(UNAUTHORIZED_MASK_PREFIX.length(), message.length() - 1);
        if (tail.isEmpty() || tail.length() > 2) return null; // "15".length() == 2
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (c < '0' || c > '9') return null;
        }
        int mask;
        try {
            mask = Integer.parseInt(tail);
        } catch (NumberFormatException notANumber) {
            return null;
        }
        if (mask < 1 || mask > 15) return null;
        StringBuilder sb = new StringBuilder(20);
        if ((mask & 1) != 0) sb.append("uid");
        if ((mask & 2) != 0) { if (sb.length() > 0) sb.append('+'); sb.append("reply"); }
        if ((mask & 4) != 0) { if (sb.length() > 0) sb.append('+'); sb.append("flags"); }
        if ((mask & 8) != 0) { if (sb.length() > 0) sb.append('+'); sb.append("size"); }
        return sb.toString();
    }

    /**
     * The cause to report for a clean-map failure event: the classified grant cause ONLY for a
     * GRANT_FAILED consumer failure, otherwise "none". This stops a later REVOKE_UNCONFIRMED (or any
     * other) failure from inheriting the stale grant cause (the consumer keeps the last grant cause).
     */
    public static String reportedGrantCause(String consumerFailureName, String grantCause) {
        if (!"GRANT_FAILED".equals(consumerFailureName)) return "none";
        return grantCause == null ? "none" : grantCause;
    }
}
