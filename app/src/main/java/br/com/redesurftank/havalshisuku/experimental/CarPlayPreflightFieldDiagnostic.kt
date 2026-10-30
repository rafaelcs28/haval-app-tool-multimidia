package br.com.redesurftank.havalshisuku.experimental

import java.io.File

/**
 * Diagnostic-only, read-only. Produces a BOUNDED, explicit report of the preflight
 * environment-snapshot fields BEFORE they are parsed/validated, so a rejected
 * snapshot-after (which is never persisted today, because parseEnvironment runs
 * checkEnvironment inline) can be understood after the fact.
 *
 * Contract (reviewer constraints):
 *  - No raw stdout, no full field map, no secrets. Only explicit per-field facts:
 *    presence, length, format validity, equality vs expected, and an 8-char prefix
 *    ONLY when the whole value is a valid sha256 (a prefix of a valid sha is not a secret;
 *    malformed values never emit a prefix).
 *  - The fingerprint check mirrors the real validator exactly (size pinned AND :0:500).
 *  - Pure functions: no Android, no clock. Fully unit-testable, including the best-effort
 *    contract and the bounded atomic write.
 *  - Never throws on malformed/absent input (that is exactly what we must capture).
 *  - Output is a single bounded JSON line; the caller writes it best-effort and must
 *    not let a write failure hide the original exception or release any gate.
 */
internal object CarPlayPreflightFieldDiagnostic {

    private val HEX64 = Regex("[a-f0-9]{64}")
    const val MAX_BYTES = 4096
    private val SCALAR_FIELDS = listOf("env_boot", "env_system_pid", "env_system_start", "env_injector_fingerprint")

    /**
     * Best-effort contract helper. The read result/exception is authoritative: a read
     * exception propagates (so no gate is released on a diagnostic path), and a diagnostic
     * failure is swallowed and never observed by the caller. Pure and unit-tested.
     */
    inline fun <T> captureBestEffort(read: () -> T, diagnostic: (T) -> Unit): T {
        val value = read()
        runCatching { diagnostic(value) }
        return value
    }

    /**
     * Bounded, atomic write (temp + rename) so a reader never sees a truncated JSON.
     * - Rejects an over-limit report by BYTE length instead of cutting it into invalid JSON
     *   (the report is bounded by construction; this is a guard, not a code path).
     * - On a rename failure the previous destination report is preserved untouched (the stale
     *   tmp is removed); it never falls back to a non-atomic overwrite that could truncate.
     * Throws on failure; the caller runs this best-effort so the gate is never affected.
     */
    fun writeAtomicBounded(file: File, content: String, maxBytes: Int = MAX_BYTES) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        require(bytes.size <= maxBytes) { "Diagnostic report exceeds ${maxBytes}B (${bytes.size})" }
        val tmp = File(file.parentFile, file.name + ".tmp")
        var renamed = false
        try {
            tmp.writeBytes(bytes)
            renamed = tmp.renameTo(file)
            if (!renamed) error("Atomic rename failed; previous report preserved")
        } finally {
            // Clean a partial/stale tmp whether writeBytes threw or the rename failed.
            if (!renamed) runCatching { tmp.delete() }
        }
    }

    /**
     * @param read "before" or "after".
     * @param fields the parsed key->value map from the snapshot (pre-parse, pre-validation).
     * @param clkTck Os.sysconf(_SC_CLK_TCK) as read by the caller (-1 if unavailable).
     * @param readStartMs/readEndMs monotonic bounds of the root read (elapsedRealtime).
     * @param injectorShaExit/policyShaExit/statExit per-command exit codes from a bounded
     *   diagnostic re-probe (sha256sum / sha256sum / stat), -1 if the probe was unavailable.
     *   These distinguish a read failure masked by the aggregate pipeline from a genuinely
     *   wrong value, WITHOUT affecting the original validator decision.
     */
    fun report(
        attemptId: String,
        bootId: String,
        revision: String,
        read: String,
        stage: String,
        fields: Map<String, String>,
        clkTck: Long,
        readStartMs: Long,
        readEndMs: Long,
        injectorShaExit: Int,
        policyShaExit: Int,
        fingerprintExit: Int,
        expectedInjectorSha: String,
        injectorSize: Long,
    ): String {
        val fpRegex = Regex("[0-9]+:[0-9]+:$injectorSize:[0-9]+:[0-9]+:0:500")
        val sb = StringBuilder(2048)
        sb.append('{')
        kv(sb, "schema", 1); sb.append(',')
        qkv(sb, "kind", "preflight-field-check"); sb.append(',')
        qkv(sb, "attemptId", safe(attemptId, 64)); sb.append(',')
        qkv(sb, "bootId", safe(bootId, 64)); sb.append(',')
        qkv(sb, "revision", safe(revision, 64)); sb.append(',')
        qkv(sb, "read", safe(read, 16)); sb.append(',')
        qkv(sb, "stage", safe(stage, 32)); sb.append(',')
        kv(sb, "clkTck", clkTck); sb.append(',')
        kv(sb, "readStartMs", readStartMs); sb.append(',')
        kv(sb, "readEndMs", readEndMs); sb.append(',')
        kv(sb, "readElapsedMs", (readEndMs - readStartMs).coerceAtLeast(-1)); sb.append(',')
        kv(sb, "fieldCount", fields.size.toLong()); sb.append(',')

        // Per-command exits captured inline by the ORIGINAL read (not a re-run, not the
        // aggregate shell exit): distinguishes a read failure masked by the pipeline from a
        // genuinely wrong value. -1 when the field was not present in the read.
        sb.append("\"readExit\":{")
        kv(sb, "injectorSha", injectorShaExit.toLong()); sb.append(',')
        kv(sb, "policySha", policyShaExit.toLong()); sb.append(',')
        kv(sb, "fingerprint", fingerprintExit.toLong())
        sb.append("},")

        // The field that drives L18: env_injector_sha must equal the PATCHED sha.
        val inj = fields["env_injector_sha"]
        sb.append("\"injector\":")
        hashBlock(sb, inj, equalsExpected = inj != null && inj == expectedInjectorSha)
        sb.append(',')
        qkv(sb, "expectedPrefix", prefix(expectedInjectorSha)); sb.append(',')

        // The other L18 condition: policy sha must be a valid sha256 (format only).
        val pol = fields["env_policy_sha"]
        sb.append("\"policy\":")
        hashBlock(sb, pol, equalsExpected = null)
        sb.append(',')

        // The immutable-executable guard (L19): fingerprint must match size AND :0:500.
        val fp = fields["env_injector_fingerprint"]
        sb.append("\"fingerprint\":{")
        kvb(sb, "present", fp != null); sb.append(',')
        kv(sb, "length", (fp?.length ?: -1).toLong()); sb.append(',')
        kvb(sb, "shapeOk", fp != null && fpRegex.matches(fp))
        sb.append("},")

        // Scalar identity fields: presence/length only (no values).
        sb.append("\"scalars\":{")
        for ((i, k) in SCALAR_FIELDS.withIndex()) {
            if (i > 0) sb.append(',')
            val v = fields[k]
            sb.append('"').append(k).append("\":{")
            kvb(sb, "present", v != null); sb.append(',')
            kv(sb, "length", (v?.length ?: -1).toLong())
            sb.append('}')
        }
        sb.append("},")

        // One-line verdict for why checkEnvironment (L18/L19) would reject this read.
        qkv(sb, "wouldRejectReason", rejectReason(inj, pol, fp, expectedInjectorSha, fpRegex))
        sb.append('}')
        return sb.toString()
    }

    /** present/length/hex64 always; prefix ONLY when the whole value is a valid sha256. */
    private fun hashBlock(sb: StringBuilder, v: String?, equalsExpected: Boolean?) {
        val valid = v != null && HEX64.matches(v)
        sb.append('{')
        kvb(sb, "present", v != null); sb.append(',')
        kv(sb, "length", (v?.length ?: -1).toLong()); sb.append(',')
        kvb(sb, "hex64", valid)
        if (equalsExpected != null) { sb.append(','); kvb(sb, "equalsExpected", equalsExpected) }
        sb.append(',')
        if (valid) qkv(sb, "prefix", v!!.take(8)) else nkv(sb, "prefix")
        sb.append('}')
    }

    private fun rejectReason(
        inj: String?, pol: String?, fp: String?, expected: String, fpRegex: Regex,
    ): String = when {
        inj == null -> "injector_sha_absent"
        !HEX64.matches(inj) -> "injector_sha_malformed"
        inj != expected -> "injector_sha_divergent"
        pol == null -> "policy_sha_absent"
        !HEX64.matches(pol) -> "policy_sha_malformed"
        fp == null -> "fingerprint_absent"
        !fpRegex.matches(fp) -> "fingerprint_shape_mismatch"
        else -> "none"
    }

    private fun prefix(v: String?): String = if (v != null && HEX64.matches(v)) v.take(8) else ""

    private fun safe(v: String, max: Int): String =
        v.take(max).filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "-._:" }

    private fun kv(sb: StringBuilder, k: String, v: Long) { sb.append('"').append(k).append("\":").append(v) }
    private fun kv(sb: StringBuilder, k: String, v: Int) = kv(sb, k, v.toLong())
    private fun kvb(sb: StringBuilder, k: String, v: Boolean) { sb.append('"').append(k).append("\":").append(v) }
    private fun qkv(sb: StringBuilder, k: String, v: String) { sb.append('"').append(k).append("\":\"").append(v).append('"') }
    private fun nkv(sb: StringBuilder, k: String) { sb.append('"').append(k).append("\":null") }
}
