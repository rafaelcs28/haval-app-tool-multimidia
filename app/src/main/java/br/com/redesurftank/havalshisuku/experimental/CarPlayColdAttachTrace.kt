package br.com.redesurftank.havalshisuku.experimental

/**
 * Diagnostic-only, observability-ONLY trace of ONE cold/attach attempt. It records entry/exit of the
 * steps that ALREADY run inside attempt() — no new root.run, no re-reads, no gate change — so a boot
 * that fails the cold re-check (CarPlayBridgeBootstrapManager.kt check: current.cold == true) can be
 * understood after the fact.
 *
 * Contract (reviewer constraints, agreed with Mumu):
 *  - Never drives a decision and never touches a gate; the caller appends best-effort (runCatching),
 *    so a trace failure can never hide the original exception or release anything.
 *  - For the GATE property, records ONLY valid/invalid and equal/changed — NEVER the nonce value.
 *  - cold is carried as a tri-state ("true"/"false"/"null") plus the raw grep token ("yes"/"no"/other),
 *    so a null is not silently read as warm.
 *  - Pure: no Android, no clock, no org.json (the caller passes monotonic sinceEntryMs). The JSON is
 *    built by hand so the whole thing is JVM-unit-testable, exactly like CarPlayPreflightFieldDiagnostic.
 *  - Bounded by construction: at most MAX_STEPS steps (a runaway observe loop cannot grow memory) and
 *    the caller writes the document with a MAX_BYTES cap via CarPlayPreflightFieldDiagnostic.writeAtomicBounded.
 */
internal object CarPlayColdAttachTrace {
    const val MAX_STEPS = 64
    const val MAX_BYTES = 16384

    /** A single step. Field values are limited to String/Boolean/Int/Long/null; anything else is emitted as its toString(). */
    data class Step(val stage: String, val sinceEntryMs: Long, val fields: List<Pair<String, Any?>> = emptyList())

    fun coldTri(cold: Boolean?): String = when (cold) { true -> "true"; false -> "false"; else -> "null" }

    /** Bounded append; silently drops once MAX_STEPS is reached so a long poll loop cannot grow memory. */
    fun append(steps: MutableList<Step>, step: Step): Boolean {
        if (steps.size >= MAX_STEPS) return false
        steps.add(step)
        return true
    }

    /** Full bounded document (header fields + steps array) for the standalone trace file. */
    fun document(header: List<Pair<String, Any?>>, steps: List<Step>): String {
        val sb = StringBuilder(256)
        sb.append('{')
        for ((k, v) in header) { emit(sb, k, v); sb.append(',') }
        sb.append("\"steps\":[")
        steps.forEachIndexed { i, s -> if (i > 0) sb.append(','); stepJson(sb, s) }
        sb.append("]}")
        return sb.toString()
    }

    /**
     * Compact summary for the failure packet (a quick telnet pull): the cold tri-state and raw grep
     * token of the FAILING observation, derived from the last "precheck_cold" step (else "observe_loop").
     */
    fun summary(header: List<Pair<String, Any?>>, steps: List<Step>): String {
        val last = steps.lastOrNull { it.stage == "precheck_cold" }
            ?: steps.lastOrNull { it.stage == "observe_loop" }
        fun f(name: String): Any? = last?.fields?.firstOrNull { it.first == name }?.second
        val h = header.toMap()
        val sb = StringBuilder(160)
        sb.append('{')
        emit(sb, "action", h["action"]); sb.append(',')
        emit(sb, "ticket", h["ticket"]); sb.append(',')
        emit(sb, "beforeCold", h["beforeCold"]); sb.append(',')
        emit(sb, "currentCold", f("cold") ?: f("finalCold")); sb.append(',')
        emit(sb, "currentColdRaw", f("coldRaw") ?: f("finalColdRaw")); sb.append(',')
        emit(sb, "idMatchesOriginal", f("idMatchesOriginal")); sb.append(',')
        emit(sb, "stepCount", steps.size.toLong())
        sb.append('}')
        return sb.toString()
    }

    private fun stepJson(sb: StringBuilder, s: Step) {
        sb.append('{')
        emit(sb, "stage", s.stage); sb.append(',')
        emit(sb, "sinceEntryMs", s.sinceEntryMs)
        for ((k, v) in s.fields) { sb.append(','); emit(sb, k, v) }
        sb.append('}')
    }

    private fun emit(sb: StringBuilder, key: String, value: Any?) {
        sb.append('"').append(esc(key)).append("\":")
        when (value) {
            null -> sb.append("null")
            is Boolean -> sb.append(value)
            is Int -> sb.append(value)
            is Long -> sb.append(value)
            else -> sb.append('"').append(esc(value.toString())).append('"')
        }
    }

    private fun esc(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (c in s) when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
        }
        return sb.toString()
    }
}
