package br.com.redesurftank.havalshisuku.experimental

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

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

    private fun report(fields: Map<String, String>, read: String = "after",
                       iRc: Int = 0, pRc: Int = 0, sRc: Int = 0) =
        CarPlayPreflightFieldDiagnostic.report(
            attemptId = "a1b2c3d4-0000-0000-0000-000000000000",
            bootId = "b7013e51-250a-410c-8fed-4ff37e2cf163",
            revision = "74149-b1b3cd38152a-92d27fb67416",
            read = read, stage = "snapshot-after", fields = fields,
            clkTck = 100, readStartMs = 1000, readEndMs = 1120,
            injectorShaExit = iRc, policyShaExit = pRc, fingerprintExit = sRc,
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
        assertTrue(r.contains("\"shapeOk\":true"))
    }

    @Test fun read_exits_are_recorded() {
        val r = report(valid(), iRc = 0, pRc = 0, sRc = 0)
        assertTrue(r.contains("\"readExit\":{\"injectorSha\":0,\"policySha\":0,\"fingerprint\":0}"))
        val u = report(valid(), iRc = -1, pRc = -1, sRc = -1)
        assertTrue(u.contains("\"readExit\":{\"injectorSha\":-1,\"policySha\":-1,\"fingerprint\":-1}"))
        // a read failure masked by the pipeline: value absent but the original sha256sum exit != 0
        val f = valid().apply { remove("env_injector_sha") }
        val m = report(f, iRc = 1)
        assertEquals("injector_sha_absent", reason(m))
        assertTrue(m.contains("\"injectorSha\":1"))
    }

    @Test fun injector_absent_is_flagged_without_prefix() {
        val f = valid().apply { remove("env_injector_sha") }
        val r = report(f)
        assertEquals("injector_sha_absent", reason(r))
        assertTrue(r.contains("\"injector\":{\"present\":false"))
        assertTrue(r.contains("\"prefix\":null"))     // no prefix for absent
    }

    @Test fun injector_malformed_is_flagged_without_prefix() {
        val f = valid().apply { this["env_injector_sha"] = "not-a-hash" }
        val r = report(f)
        assertEquals("injector_sha_malformed", reason(r))
        assertTrue(r.contains("\"hex64\":false"))
        // malformed value must NOT leak even a prefix.
        assertTrue(r.contains("\"prefix\":null"))
        assertFalse(r.contains("not-a-ha"))
    }

    @Test fun injector_divergent_original_is_flagged_with_prefix() {
        val f = valid().apply { this["env_injector_sha"] = original }
        val r = report(f)
        assertEquals("injector_sha_divergent", reason(r))
        assertTrue(r.contains("\"equalsExpected\":false"))
        assertTrue(r.contains("\"prefix\":\"${original.take(8)}\""))  // divergent but valid-format: prefix ok
        assertFalse(r.contains(original))
        assertFalse(r.contains(patched))
    }

    @Test fun policy_absent_and_malformed() {
        assertEquals("policy_sha_absent", reason(report(valid().apply { remove("env_policy_sha") })))
        assertEquals("policy_sha_malformed", reason(report(valid().apply { this["env_policy_sha"] = "xyz" })))
    }

    @Test fun fingerprint_mirrors_validator_uid0_mode500() {
        // The exact confusion from the field: the stale injector had uid=1000.
        assertEquals("none", reason(report(valid())))  // :0:500 accepted
        val wrongUid = valid().apply { this["env_injector_fingerprint"] = "64784:274764:$size:1:1:1000:500" }
        assertEquals("fingerprint_shape_mismatch", reason(report(wrongUid)))
        val wrongMode = valid().apply { this["env_injector_fingerprint"] = "64784:274764:$size:1:1:0:777" }
        assertEquals("fingerprint_shape_mismatch", reason(report(wrongMode)))
        val wrongSize = valid().apply { this["env_injector_fingerprint"] = "64784:274764:999:1:1:0:500" }
        assertEquals("fingerprint_shape_mismatch", reason(report(wrongSize)))
    }

    @Test fun empty_map_never_throws_and_is_bounded() {
        val r = report(emptyMap())
        assertEquals("injector_sha_absent", reason(r))
        assertTrue(r.length <= CarPlayPreflightFieldDiagnostic.MAX_BYTES)
        assertFalse(r.contains("\n"))
        assertEquals(r.count { it == '{' }, r.count { it == '}' })
    }

    @Test fun never_leaks_raw_hash_values_even_when_valid() {
        val r = report(valid())
        assertFalse(r.contains(patched))
        assertFalse(r.contains(policy))
        assertTrue(r.contains("\"prefix\":\"${patched.take(8)}\""))
    }

    // ---- best-effort contract: the read is authoritative, the diagnostic is swallowed ----

    @Test fun captureBestEffort_returns_read_value_even_when_diagnostic_throws() {
        var diagRan = false
        val v = CarPlayPreflightFieldDiagnostic.captureBestEffort(
            read = { "VALUE" },
            diagnostic = { diagRan = true; throw RuntimeException("disk full") })
        assertEquals("VALUE", v)      // diagnostic failure never reaches the caller
        assertTrue(diagRan)
    }

    @Test fun captureBestEffort_propagates_read_exception_and_skips_diagnostic() {
        var diagRan = false
        try {
            CarPlayPreflightFieldDiagnostic.captureBestEffort<String>(
                read = { throw IllegalStateException("read failed") },
                diagnostic = { diagRan = true })
            fail("read exception must propagate")
        } catch (e: IllegalStateException) {
            assertEquals("read failed", e.message)
        }
        assertFalse(diagRan)          // gate-bearing read failure is never masked by the diagnostic
    }

    // ---- bounded atomic write ----

    @Test fun writeAtomicBounded_writes_full_content_and_separate_files() {
        val dir = Files.createTempDirectory("fieldcheck").toFile()
        val before = File(dir, "carplay-fieldcheck-before.json")
        val after = File(dir, "carplay-fieldcheck-after.json")
        CarPlayPreflightFieldDiagnostic.writeAtomicBounded(before, report(valid(), read = "before"))
        CarPlayPreflightFieldDiagnostic.writeAtomicBounded(after, report(valid(), read = "after"))
        assertTrue(before.readText().contains("\"read\":\"before\""))
        assertTrue(after.readText().contains("\"read\":\"after\""))
        // distinct attempts/reads never share a file.
        assertFalse(before.readText().contains("\"read\":\"after\""))
        assertFalse(File(dir, "carplay-fieldcheck-before.json.tmp").exists())
    }

    @Test fun writeAtomicBounded_rejects_oversize_instead_of_cutting_json() {
        val dir = Files.createTempDirectory("fieldcheck2").toFile()
        val f = File(dir, "big.json")
        try {
            CarPlayPreflightFieldDiagnostic.writeAtomicBounded(f, "x".repeat(10_000), maxBytes = 100)
            fail("over-limit content must be rejected, never truncated into invalid JSON")
        } catch (e: IllegalArgumentException) {
            assertFalse(f.exists())   // nothing written
            assertFalse(File(dir, "big.json.tmp").exists())   // no stale tmp
        }
    }

    @Test fun writeAtomicBounded_round_trips_utf8_and_leaves_no_tmp() {
        val dir = Files.createTempDirectory("fieldcheck4").toFile()
        val f = File(dir, "ok.json")
        val content = report(valid())
        CarPlayPreflightFieldDiagnostic.writeAtomicBounded(f, content)
        assertEquals(content, f.readText())
        assertFalse(File(dir, "ok.json.tmp").exists())
    }

    @Test fun writeAtomicBounded_preserves_previous_on_rename_failure() {
        val dir = Files.createTempDirectory("fieldcheck3").toFile()
        // Make the destination a NON-EMPTY directory so renameTo(file->dir) fails.
        val dest = File(dir, "dest.json").apply { mkdirs() }
        File(dest, "keep").writeText("previous")
        try {
            CarPlayPreflightFieldDiagnostic.writeAtomicBounded(dest, "{\"ok\":1}")
            fail("rename failure must surface (caller handles best-effort)")
        } catch (e: IllegalStateException) {
            // previous content preserved, stale tmp cleaned
            assertTrue(dest.isDirectory)
            assertTrue(File(dest, "keep").exists())
            assertFalse(File(dir, "dest.json.tmp").exists())
        }
    }
}
