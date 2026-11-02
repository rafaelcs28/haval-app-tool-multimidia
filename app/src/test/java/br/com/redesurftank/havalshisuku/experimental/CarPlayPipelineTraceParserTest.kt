package br.com.redesurftank.havalshisuku.experimental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarPlayPipelineTraceParserTest {
    @Test fun acceptsOnlyBoundedStructuredTraceLines() {
        val raw = """
            W/ImpulseCPMap(123): boot_ms=1200 gen=7 event=getter_runtime status=0 value=1 aux=1
            unrelated token=route secret=ignored
            W/ImpulseCPMap(123): boot_ms=1300 gen=7 event=codec_render status=0 value=2 aux=0
        """.trimIndent()
        val parsed = CarPlayPipelineTraceParser.parse(raw)
        assertEquals(2, parsed.size)
        assertEquals("getter_runtime", parsed[0].event)
        assertEquals(2L, parsed[1].value)
    }

    @Test fun rejectsOverflowAndFreeText() {
        val parsed = CarPlayPipelineTraceParser.parse(
            "boot_ms=1 gen=999999999999999999999999 event=x status=0 value=1 aux=0\n" +
                "boot_ms=1 gen=1 event=UPPER status=0 value=1 aux=0\n" +
                "free route text")
        assertTrue(parsed.isEmpty())
    }
}
