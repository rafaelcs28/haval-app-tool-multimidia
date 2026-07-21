package br.com.redesurftank.havalshisuku.managers

import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.REARM_FASTENED_MS
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.evaluate
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.isEngineOff
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.isMoving
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.parseUnbelted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeatbeltVoiceLogicTest {

    private val t0 = 1_000_000L
    private val off = emptySet<Int>()

    @Test
    fun `parse so assentos conhecidos`() {
        assertEquals(setOf(1), parseUnbelted("(0,1,0,0,0)"))
        assertEquals(setOf(0, 4), parseUnbelted("{1,0,0,0,1}"))
        assertEquals(off, parseUnbelted("(0,0,0,0,0)"))
        assertEquals(off, parseUnbelted(null))
        assertEquals(setOf(2), parseUnbelted("(0,0,1,0,0,1,1)")) // idx>4 ignorado
    }

    @Test
    fun `movimento fail-closed`() {
        assertTrue(isMoving("12.5"))
        assertFalse(isMoving("0"))
        assertFalse(isMoving("0.3"))
        assertFalse(isMoving(null))
    }

    @Test
    fun `engine off`() {
        assertTrue(isEngineOff("-1")); assertTrue(isEngineOff("15")); assertFalse(isEngineOff("1"))
    }

    @Test
    fun `primeira soltada andando avisa`() {
        val d = evaluate(emptyMap(), setOf(0), moving = true, nowMs = t0)
        assertEquals(listOf(0), d.announceSeats)
        assertTrue(d.newState[0]!!.warned)
    }

    @Test
    fun `seguir solto NAO repete`() {
        val s1 = evaluate(emptyMap(), setOf(0), true, t0).newState
        val d2 = evaluate(s1, setOf(0), true, t0 + 60_000)
        assertTrue(d2.announceSeats.isEmpty())
        assertTrue(d2.newState[0]!!.warned)
    }

    @Test
    fun `preso 3s e solta de novo avisa de novo`() {
        val s1 = evaluate(emptyMap(), setOf(0), true, t0).newState        // avisou
        val s2 = evaluate(s1, off, true, t0 + 5_000).newState             // prende (começa timer)
        val s3 = evaluate(s2, off, true, t0 + 5_000 + REARM_FASTENED_MS).newState // preso >=3s -> re-arma
        val d4 = evaluate(s3, setOf(0), true, t0 + 20_000)                // solta de novo
        assertEquals(listOf(0), d4.announceSeats)
    }

    @Test
    fun `preso menos de 3s e solta NAO reavisa (anti-flicker)`() {
        val s1 = evaluate(emptyMap(), setOf(0), true, t0).newState        // avisou
        val s2 = evaluate(s1, off, true, t0 + 1_000).newState             // prende 1s
        assertTrue(s2[0]!!.warned)                                        // ainda avisado (debounce)
        val d3 = evaluate(s2, setOf(0), true, t0 + 2_000)                 // solta em <3s
        assertTrue(d3.announceSeats.isEmpty())                           // não reavisa
    }

    @Test
    fun `preso agenda recheck ate completar o re-arm`() {
        val s1 = evaluate(emptyMap(), setOf(0), true, t0).newState
        // 1a avaliação com cinto preso: o timer de re-arm começa AGORA (elapsed 0) -> recheca no cheio.
        val d2 = evaluate(s1, off, true, t0 + 1_000)
        assertEquals(REARM_FASTENED_MS, d2.recheckInMs)
    }

    @Test
    fun `parado nao avisa e avisa quando andar`() {
        val d1 = evaluate(emptyMap(), setOf(0), moving = false, nowMs = t0)
        assertTrue(d1.announceSeats.isEmpty())
        assertNull(d1.newState[0])                                       // segue armado (default)
        val d2 = evaluate(d1.newState, setOf(0), moving = true, nowMs = t0 + 4_000)
        assertEquals(listOf(0), d2.announceSeats)
    }

    @Test
    fun `assentos independentes`() {
        val s1 = evaluate(emptyMap(), setOf(0), true, t0).newState       // avisa motorista
        val d2 = evaluate(s1, setOf(0, 1), true, t0 + 5_000)             // passageiro solta agora
        assertEquals(listOf(1), d2.announceSeats)                        // só o passageiro
        assertTrue(d2.newState[0]!!.warned)
    }

    @Test
    fun `varios juntos avisam todos de uma vez`() {
        val d = evaluate(emptyMap(), setOf(0, 1, 3), true, t0)
        assertEquals(listOf(0, 1, 3), d.announceSeats)
    }

    @Test
    fun `reset de ignicao (mapa vazio) volta a avisar`() {
        val s1 = evaluate(emptyMap(), setOf(0), true, t0).newState
        assertTrue(s1[0]!!.warned)
        val d2 = evaluate(emptyMap(), setOf(0), true, t0 + 500_000)      // manager zerou no ciclo
        assertEquals(listOf(0), d2.announceSeats)
    }

    @Test
    fun `preso re-armado apos recheck nao guarda estado`() {
        val s1 = evaluate(emptyMap(), setOf(0), true, t0).newState       // avisou
        val s2 = evaluate(s1, off, true, t0 + 1_000).newState            // prende (elapsed 0) -> guarda
        assertTrue(s2[0]!!.warned)
        val s3 = evaluate(s2, off, true, t0 + 1_000 + REARM_FASTENED_MS).newState // recheck: preso 3s -> re-arma
        assertTrue(s3.isEmpty())                                         // default, sem estado
    }
}
