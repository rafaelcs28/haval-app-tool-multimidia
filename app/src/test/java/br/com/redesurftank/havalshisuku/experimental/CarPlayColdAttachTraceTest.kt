package br.com.redesurftank.havalshisuku.experimental

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CarPlayColdAttachTraceTest {

    private val header = listOf<Pair<String, Any?>>(
        "action" to "COLD_ATTACH", "ticket" to "38002182-73aa-4047-920f-adbfcf69b269",
        "build" to "74149-abc", "clockBaseElapsedMs" to 1672L, "clockBaseWallMs" to 1793543816000L,
        "beforeId" to "b9f-3861-1672", "beforeCold" to "true")

    private fun steps(vararg s: CarPlayColdAttachTrace.Step) = s.toMutableList()

    @Test fun cold_tri_covers_all_three_states() {
        assertEquals("true", CarPlayColdAttachTrace.coldTri(true))
        assertEquals("false", CarPlayColdAttachTrace.coldTri(false))
        assertEquals("null", CarPlayColdAttachTrace.coldTri(null))
    }

    @Test fun append_is_bounded_so_a_runaway_loop_cannot_grow_memory() {
        val list = steps()
        repeat(CarPlayColdAttachTrace.MAX_STEPS) {
            assertTrue(CarPlayColdAttachTrace.append(list, CarPlayColdAttachTrace.Step("observe", it.toLong())))
        }
        assertEquals(CarPlayColdAttachTrace.MAX_STEPS, list.size)
        // past the cap: dropped, returns false, size unchanged
        assertFalse(CarPlayColdAttachTrace.append(list, CarPlayColdAttachTrace.Step("precheck_cold", 9999L)))
        assertEquals(CarPlayColdAttachTrace.MAX_STEPS, list.size)
    }

    @Test fun document_emits_header_then_steps_with_correct_json_types() {
        val list = steps(
            CarPlayColdAttachTrace.Step("entry", 0L, listOf("initialColdProofSet" to true)),
            CarPlayColdAttachTrace.Step("precheck_cold", 1420L, listOf(
                "currentId" to "b9f-3861-1672", "cold" to "false", "coldRaw" to "no",
                "idMatchesOriginal" to true, "initialColdProofSet" to true)))
        val doc = CarPlayColdAttachTrace.document(header, list)
        assertTrue(doc.startsWith("{"))
        assertTrue(doc.endsWith("}"))
        assertTrue(doc.contains("\"action\":\"COLD_ATTACH\""))
        assertTrue(doc.contains("\"clockBaseElapsedMs\":1672"))          // Long: unquoted
        assertTrue(doc.contains("\"steps\":["))
        assertTrue(doc.contains("\"stage\":\"entry\""))
        assertTrue(doc.contains("\"sinceEntryMs\":0"))
        assertTrue(doc.contains("\"initialColdProofSet\":true"))          // Boolean: unquoted
        assertTrue(doc.contains("\"cold\":\"false\""))                    // String tri-state: quoted
        assertTrue(doc.contains("\"coldRaw\":\"no\""))
    }

    @Test fun summary_reports_the_failing_observation_cold_and_raw_grep() {
        val list = steps(
            CarPlayColdAttachTrace.Step("entry", 0L),
            CarPlayColdAttachTrace.Step("observe_loop", 1300L, listOf(
                "observeCount" to 2, "finalCold" to "false", "finalColdRaw" to "no")),
            CarPlayColdAttachTrace.Step("precheck_cold", 1420L, listOf(
                "cold" to "false", "coldRaw" to "no", "idMatchesOriginal" to true)))
        val s = CarPlayColdAttachTrace.summary(header, list)
        assertTrue(s.contains("\"action\":\"COLD_ATTACH\""))
        assertTrue(s.contains("\"beforeCold\":\"true\""))
        assertTrue(s.contains("\"currentCold\":\"false\""))
        assertTrue(s.contains("\"currentColdRaw\":\"no\""))
        assertTrue(s.contains("\"idMatchesOriginal\":true"))
        assertTrue(s.contains("\"stepCount\":3"))
    }

    @Test fun summary_falls_back_to_observe_loop_when_precheck_absent() {
        val list = steps(
            CarPlayColdAttachTrace.Step("entry", 0L),
            CarPlayColdAttachTrace.Step("observe_loop", 1300L, listOf(
                "observeCount" to 3, "finalCold" to "null", "finalColdRaw" to null)))
        val s = CarPlayColdAttachTrace.summary(header, list)
        assertTrue(s.contains("\"currentCold\":\"null\""))      // tri-state string "null", quoted
        assertTrue(s.contains("\"currentColdRaw\":null"))        // genuine JSON null (grep token absent), unquoted
        assertTrue(s.contains("\"stepCount\":2"))
    }

    @Test fun summary_on_empty_steps_is_valid_json_with_nulls() {
        val s = CarPlayColdAttachTrace.summary(header, emptyList())
        assertTrue(s.contains("\"currentCold\":null"))
        assertTrue(s.contains("\"currentColdRaw\":null"))
        assertTrue(s.contains("\"idMatchesOriginal\":null"))
        assertTrue(s.contains("\"stepCount\":0"))
    }

    @Test fun document_over_the_byte_cap_is_rejected_and_the_previous_file_is_preserved() {
        // The cap must bound the ALREADY-escaped UTF-8 JSON (Mumu): an over-limit document is rejected,
        // never truncated into invalid JSON, and a stale previous file is left untouched (not half-written).
        val big = "x".repeat(CarPlayColdAttachTrace.MAX_BYTES)
        val doc = CarPlayColdAttachTrace.document(emptyList(),
            steps(CarPlayColdAttachTrace.Step("entry", 0L, listOf("blob" to big))))
        assertTrue(doc.toByteArray(Charsets.UTF_8).size > CarPlayColdAttachTrace.MAX_BYTES)
        val f = File.createTempFile("coldattach", ".json").apply { deleteOnExit(); writeText("PREVIOUS") }
        var rejected = false
        try {
            CarPlayPreflightFieldDiagnostic.writeAtomicBounded(f, doc, CarPlayColdAttachTrace.MAX_BYTES)
        } catch (e: IllegalArgumentException) { rejected = true }
        assertTrue("over-limit document must be rejected", rejected)
        assertEquals("previous file preserved untouched", "PREVIOUS", f.readText())
    }

    @Test fun strings_with_quotes_and_newlines_are_escaped() {
        val list = steps(CarPlayColdAttachTrace.Step("entry", 0L, listOf("note" to "a\"b\nc")))
        val doc = CarPlayColdAttachTrace.document(emptyList(), list)
        assertTrue(doc.contains("\"note\":\"a\\\"b\\nc\""))
    }
}
