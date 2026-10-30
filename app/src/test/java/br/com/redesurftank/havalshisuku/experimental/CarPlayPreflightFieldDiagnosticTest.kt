package br.com.redesurftank.havalshisuku.experimental

import org.junit.Assert.*
import org.junit.Test

class CarPlayPreflightFieldDiagnosticTest {
    private val patched = CarPlayResidentProof.PATCHED_INJECTOR_SHA
    private val original = CarPlayResidentProof.ORIGINAL_INJECTOR_SHA
    private val size = CarPlayResidentProof.INJECTOR_SIZE
    private val policy = "58d3ec6d8a097b1e1f351e8050c76322f10dc1504900fb96d5ba9c168588c1ae"
    private val fingerprint = "64784:274764:$size:1793205072:1793205072:0:500"

    private fun valid(): MutableMap<String, String> = mutableMapOf(
        "env_boot" to "b7013e51-250a-410c-8fed-4ff37e2cf163",
        "env_system_pid" to "1858",
        "env_system_start" to "493",
        "env_policy_sha" to policy,
        "env_injector_sha" to patched,
        "env_injector_fingerprint" to fingerprint,
    )

    private fun report(fields: Map<String, String>, read: String = "after") =
        CarPlayPreflightFieldDiagnostic.report(
            attemptId = "a1b2c3d4-0000-0000-0000-000000000000",
            bootId = "b7013e51-250a-410c-8fed-4ff37e2cf163",
            revision = "74149-b1b3cd38152a-92d27fb67416",
            read = read, stage = "snapshot-after", fields = fields,
            clkTck = 100, readStartMs = 1000, readEndMs = 1120, rootExit = 0,
            expectedInjectorSha = patched, injectorSize = size,
        )

    private fun reason(r: String) = Regex("\"wouldRejectReason\":\"([^\"]*)\"").find(r)!!.groupValues[1]

    @Test fun valid_read_rejects_nothing() {
        val r = report(valid())
        assertEquals("none", reason(r))
        assertTrue(r.contains("\"equalsExpected\":true"))
        assertTrue(r.contains("\"read\":\"after\""))
        assertTrue(r.contains("\"clkTck\":100"))
        assertTrue(r.contains("\"readElapsedMs\":120"))
    }

    @Test fun injector_absent_is_flagged() {
        val f = valid().apply { remove("env_injector_sha") }
        val r = report(f)
        assertEquals("injector_sha_absent", reason(r))
        assertTrue(r.contains("\"injector\":{\"present\":false"))
    }

    @Test fun injector_malformed_is_flagged() {
        val f = valid().apply { this["env_injector_sha"] = "not-a-hash" }
        val r = report(f)
        assertEquals("injector_sha_malformed", reason(r))
        assertTrue(r.contains("\"hex64\":false"))
    }

    @Test fun injector_divergent_original_is_flagged() {
        // The exact confusion seen in the field: a valid-format sha that is the ORIGINAL, not PATCHED.
        val f = valid().apply { this["env_injector_sha"] = original }
        val r = report(f)
        assertEquals("injector_sha_divergent", reason(r))
        assertTrue(r.contains("\"equalsExpected\":false"))
        // prefix present for correlation, but the full value must NOT leak.
        assertTrue(r.contains("\"prefix\":\"${original.take(8)}\""))
        assertFalse(r.contains(original))
        assertFalse(r.contains(patched))
    }

    @Test fun policy_absent_is_flagged() {
        val f = valid().apply { remove("env_policy_sha") }
        assertEquals("policy_sha_absent", reason(report(f)))
    }

    @Test fun policy_malformed_is_flagged() {
        val f = valid().apply { this["env_policy_sha"] = "xyz" }
        assertEquals("policy_sha_malformed", reason(report(f)))
    }

    @Test fun fingerprint_shape_mismatch_is_flagged() {
        val f = valid().apply { this["env_injector_fingerprint"] = "64784:274764:999:1:1:0:500" }
        assertEquals("fingerprint_shape_mismatch", reason(report(f)))
    }

    @Test fun empty_map_never_throws_and_is_bounded() {
        val r = report(emptyMap())
        assertEquals("injector_sha_absent", reason(r))
        assertTrue(r.length <= 4096)
        assertFalse(r.contains("\n"))
        assertEquals(r.count { it == '{' }, r.count { it == '}' })
    }

    @Test fun never_leaks_raw_hash_values_even_when_valid() {
        val r = report(valid())
        assertFalse(r.contains(patched))     // only the 8-char prefix is emitted
        assertFalse(r.contains(policy))
        assertTrue(r.contains("\"prefix\":\"${patched.take(8)}\""))
    }
}
