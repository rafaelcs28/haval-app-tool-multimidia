package br.com.redesurftank.havalshisuku.experimental

import org.junit.Assert.*
import org.junit.Test

class CarPlayGrantFailureCauseTest {

    private fun d(t: Throwable?) = CarPlayGrantFailureCause.describe(t)

    @Test fun null_failure_is_none() {
        assertEquals("none", d(null))
    }

    @Test fun security_messages_map_to_fixed_categories() {
        assertEquals("security_unauthorized", d(SecurityException("Unauthorized Surface transport")))
        assertEquals("security_token", d(SecurityException("Invalid session token or closed endpoint")))
        assertEquals("security_other", d(SecurityException("something else entirely")))
    }

    @Test fun diag5_unauthorized_mask_decodes_which_conditions_tripped() {
        assertEquals("security_unauthorized:flags", d(SecurityException("Unauthorized Surface transport [v1 m=4]")))
        assertEquals("security_unauthorized:uid", d(SecurityException("Unauthorized Surface transport [v1 m=1]")))
        assertEquals("security_unauthorized:uid+flags", d(SecurityException("Unauthorized Surface transport [v1 m=5]")))
        assertEquals("security_unauthorized:uid+reply+flags+size",
            d(SecurityException("Unauthorized Surface transport [v1 m=15]")))
        assertEquals("flags", CarPlayGrantFailureCause.unauthorizedMaskLabel("Unauthorized Surface transport [v1 m=4]"))
        assertEquals("reply+size", CarPlayGrantFailureCause.unauthorizedMaskLabel("Unauthorized Surface transport [v1 m=10]"))
    }

    @Test fun diag5_mask_malformed_or_old_message_falls_back_to_generic() {
        assertEquals("security_unauthorized", d(SecurityException("Unauthorized Surface transport")))           // old generic
        assertEquals("security_unauthorized", d(SecurityException("Unauthorized Surface transport [v1 m=]")))    // empty
        assertEquals("security_unauthorized", d(SecurityException("Unauthorized Surface transport [v1 m=99]")))  // overflow range
        assertEquals("security_unauthorized", d(SecurityException("Unauthorized Surface transport [v1 m=0]")))   // no bits set
        assertEquals("security_unauthorized", d(SecurityException("Unauthorized Surface transport [v1 m=4] x"))) // trailing extra
        assertEquals("security_unauthorized", d(SecurityException("Unauthorized Surface transport [v2 m=4]")))   // wrong version
        assertNull(CarPlayGrantFailureCause.unauthorizedMaskLabel("Unauthorized Surface transport"))
        assertNull(CarPlayGrantFailureCause.unauthorizedMaskLabel(null))
    }

    @Test fun illegal_argument_categories() {
        assertEquals("invalid_grant", d(IllegalArgumentException("Invalid Surface grant")))
        assertEquals("trailing_data", d(IllegalArgumentException("Trailing data")))
        assertEquals("illegal_arg_other", d(IllegalArgumentException("x")))
    }

    @Test fun illegal_state_known_messages() {
        assertEquals("native_import_failed", d(IllegalStateException("Native Surface import failed")))
        assertEquals("endpoint_consumed", d(IllegalStateException("Endpoint already consumed")))
        // "Unknown generation" is the REVOKE_UNCONFIRMED cascade's source (old/unknown generation)
        assertEquals("unknown_generation", d(IllegalStateException("Unknown generation")))
    }

    @Test fun activation_code_only_from_exact_format_and_contract_range_0_to_10() {
        assertEquals("native_activation_failed:0", d(IllegalStateException("Native runtime activation failed: 0")))
        assertEquals("native_activation_failed:3", d(IllegalStateException("Native runtime activation failed: 3")))
        assertEquals("native_activation_failed:10", d(IllegalStateException("Native runtime activation failed: 10")))
        // out of contract range (0..10), negative, malformed, secret-ish -> category WITHOUT code, no leak
        assertEquals("illegal_state_other", d(IllegalStateException("Native runtime activation failed: 11")))
        assertEquals("illegal_state_other", d(IllegalStateException("Native runtime activation failed: -5")))
        assertEquals("illegal_state_other", d(IllegalStateException("Native runtime activation failed: 100000")))
        assertEquals("illegal_state_other", d(IllegalStateException("Native runtime activation failed: abc")))
        assertEquals("illegal_state_other", d(IllegalStateException("Native runtime activation failed: TOKENxyz")))
        assertEquals("illegal_state_other", d(IllegalStateException("Native runtime activation failed: ")))
        // activationCode() helper directly
        assertEquals(8, CarPlayGrantFailureCause.activationCode("Native runtime activation failed: 8"))
        assertNull(CarPlayGrantFailureCause.activationCode("Native runtime activation failed: 11"))
        assertNull(CarPlayGrantFailureCause.activationCode("Native runtime activation failed: -1"))
        assertNull(CarPlayGrantFailureCause.activationCode("totally different message"))
        assertNull(CarPlayGrantFailureCause.activationCode(null))
    }

    @Test fun cause_reported_only_for_grant_failed_never_cross_attributed_to_revoke() {
        assertEquals("native_import_failed",
            CarPlayGrantFailureCause.reportedGrantCause("GRANT_FAILED", "native_import_failed"))
        // the later REVOKE_UNCONFIRMED must NOT inherit the stale grant cause
        assertEquals("none",
            CarPlayGrantFailureCause.reportedGrantCause("REVOKE_UNCONFIRMED", "native_import_failed"))
        assertEquals("none", CarPlayGrantFailureCause.reportedGrantCause(null, "native_import_failed"))
        assertEquals("none", CarPlayGrantFailureCause.reportedGrantCause("GRANT_FAILED", null))
    }

    @Test fun large_or_secret_messages_never_leak_text() {
        val secret = "nonce=" + "A".repeat(5000) + " token=DEADBEEFCAFE"
        val out = d(IllegalStateException(secret))
        assertEquals("illegal_state_other", out)
        assertFalse(out.contains("DEADBEEF"))
        assertFalse(out.contains("nonce"))
        assertTrue(out.length < 40)
    }

    @Test fun type_fallbacks() {
        assertEquals("linkage", d(UnsatisfiedLinkError("libcarplay_video_bridge")))  // LinkageError subclass
        assertEquals("npe", d(NullPointerException("boom")))
        assertEquals("unknown", d(RuntimeException("whatever")))
    }

    @Test fun classifier_never_throws_even_when_getMessage_throws() {
        // A diagnostic classifier fault must never surface (it must not break the consumer's close/revoke).
        val hostile = object : RuntimeException() {
            override val message: String? get() = throw IllegalStateException("message access fails")
        }
        assertEquals("unknown", d(hostile))
    }
}
