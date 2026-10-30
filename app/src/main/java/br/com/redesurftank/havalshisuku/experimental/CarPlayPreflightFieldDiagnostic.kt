package br.com.redesurftank.havalshisuku.experimental

/**
 * Diagnostic-only, read-only. Produces a BOUNDED, explicit report of the preflight
 * environment-snapshot fields BEFORE they are parsed/validated, so a rejected
 * snapshot-after (which is never persisted today, because parseEnvironment runs
 * checkEnvironment inline) can be understood after the fact.
 *
 * Contract (reviewer constraints):
 *  - No raw stdout, no full field map, no secrets. Only explicit per-field facts:
 *    presence, length, format validity, equality vs expected, and an 8-char prefix
 *    purely for cross-read correlation (a sha256 prefix is not a secret).
 *  - Pure function: no Android, no I/O, no clock. Fully unit-testable.
 *  - Never throws on malformed/absent input (that is exactly what we must capture).
 *  - Output is a single bounded JSON line; the caller writes it best-effort and must
 *    not let a write failure hide the original exception or release any gate.
 */
internal object CarPlayPreflightFieldDiagnostic {

    private val HEX64 = Regex("[a-f0-9]{64}")

    /** Fields the environment snapshot is expected to emit. Checked explicitly, no raw values kept. */
    private val HASH_FIELDS = listOf("env_policy_sha", "env_injector_sha")
    private val SCALAR_FIELDS = listOf("env_boot", "env_system_pid", "env_system_start", "env_injector_fingerprint")

    /**
     * @param read "before" or "after".
     * @param fields the parsed key->value map from the snapshot (pre-parse, pre-validation).
     * @param clkTck Os.sysconf(_SC_CLK_TCK) as read by the caller (-1 if unavailable).
     * @param readStartMs/readEndMs monotonic bounds of the root read (elapsedRealtime).
     * @param rootExit exit code of the read command (0 on success; the caller only reaches
     *                 here on a successful read, so this is 0, but it is explicit for the record).
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
        rootExit: Int,
        expectedInjectorSha: String,
        injectorSize: Long,
    ): String {
        val fpRegex = Regex("[0-9]+:[0-9]+:$injectorSize:[0-9]+:[0-9]+:[0-9]+:[0-9]+")
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
        kv(sb, "rootExit", rootExit.toLong()); sb.append(',')
        kv(sb, "fieldCount", fields.size.toLong()); sb.append(',')

        // The field that drives L18: env_injector_sha must equal the PATCHED sha.
        val inj = fields["env_injector_sha"]
        sb.append("\"injector\":{")
        kvb(sb, "present", inj != null); sb.append(',')
        kv(sb, "length", (inj?.length ?: -1).toLong()); sb.append(',')
        kvb(sb, "hex64", inj != null && HEX64.matches(inj)); sb.append(',')
        kvb(sb, "equalsExpected", inj != null && inj == expectedInjectorSha); sb.append(',')
        qkv(sb, "prefix", prefix(inj)); sb.append(',')
        qkv(sb, "expectedPrefix", prefix(expectedInjectorSha))
        sb.append("},")

        // The other L18 condition: policy sha must be a valid sha256 (format only).
        val pol = fields["env_policy_sha"]
        sb.append("\"policy\":{")
        kvb(sb, "present", pol != null); sb.append(',')
        kv(sb, "length", (pol?.length ?: -1).toLong()); sb.append(',')
        kvb(sb, "hex64", pol != null && HEX64.matches(pol)); sb.append(',')
        qkv(sb, "prefix", prefix(pol))
        sb.append("},")

        // The immutable-executable guard: fingerprint must match the size-pinned shape.
        val fp = fields["env_injector_fingerprint"]
        sb.append("\"fingerprint\":{")
        kvb(sb, "present", fp != null); sb.append(',')
        kv(sb, "length", (fp?.length ?: -1).toLong()); sb.append(',')
        kvb(sb, "sizeShapeOk", fp != null && fpRegex.matches(fp))
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

        // One-line verdict for why checkEnvironment (L18/L19/L20) would reject this read.
        qkv(sb, "wouldRejectReason", rejectReason(inj, pol, fp, expectedInjectorSha, fpRegex))
        sb.append('}')
        return sb.toString()
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

    private fun prefix(v: String?): String = v?.take(8)?.filter { it in "0123456789abcdef" } ?: ""

    // Keep ASCII identifier chars only; strips quotes/backslashes/control chars so the
    // hand-built JSON stays valid without escaping. Covers uuids, revisions, "before"/"after"
    // and stage names like "snapshot-after".
    private fun safe(v: String, max: Int): String =
        v.take(max).filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "-._:" }

    private fun kv(sb: StringBuilder, k: String, v: Long) { sb.append('"').append(k).append("\":").append(v) }
    private fun kv(sb: StringBuilder, k: String, v: Int) = kv(sb, k, v.toLong())
    private fun kvb(sb: StringBuilder, k: String, v: Boolean) { sb.append('"').append(k).append("\":").append(v) }
    private fun qkv(sb: StringBuilder, k: String, v: String) { sb.append('"').append(k).append("\":\"").append(v).append('"') }
}
