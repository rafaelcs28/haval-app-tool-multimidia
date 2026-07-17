package br.com.redesurftank.havalshisuku.managers

import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.REARM_GRACE_MS
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.SeatPhase
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.SeatState
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.evaluate
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.isEngineOff
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.isMoving
import br.com.redesurftank.havalshisuku.managers.SeatbeltVoiceLogic.parseUnbelted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeatbeltVoiceLogicTest {

    private val t0 = 1_000_000L

    @Test
    fun `parse do array de cintos so assentos conhecidos`() {
        assertEquals(setOf(1), parseUnbelted("(0,1,0,0,0)"))
        assertEquals(setOf(0, 4), parseUnbelted("{1,0,0,0,1}"))
        assertEquals(emptySet<Int>(), parseUnbelted("(0,0,0,0,0)"))
        assertEquals(emptySet<Int>(), parseUnbelted(null))
        assertEquals(emptySet<Int>(), parseUnbelted("lixo"))
        // índice além do 4 é ignorado
        assertEquals(setOf(2), parseUnbelted("(0,0,1,0,0,1,1)"))
    }

    @Test
    fun `movimento fail-closed`() {
        assertTrue(isMoving("12.5"))
        assertFalse(isMoving("0"))
        assertFalse(isMoving("0.3"))
        assertFalse(isMoving(null))
        assertFalse(isMoving("--"))
    }

    @Test
    fun `engine off usa o conjunto do TripConsistency`() {
        assertTrue(isEngineOff("-1"))
        assertTrue(isEngineOff("10"))
        assertTrue(isEngineOff("15"))
        assertFalse(isEngineOff("1"))
        assertFalse(isEngineOff(null))
    }

    @Test
    fun `regra 1 - arrancou com soltos avisa cada assento uma vez`() {
        val d = evaluate(emptyMap(), setOf(1, 2), moving = true, nowMs = t0)
        assertEquals(listOf(1, 2), d.announceSeats)
        assertEquals(SeatPhase.ANNOUNCED, d.newState[1]?.phase)
        assertEquals(SeatPhase.ANNOUNCED, d.newState[2]?.phase)
    }

    @Test
    fun `parado nao fala e continua elegivel pra quando andar`() {
        val d = evaluate(emptyMap(), setOf(1), moving = false, nowMs = t0)
        assertTrue(d.announceSeats.isEmpty())
        assertNull(d.newState[1]) // segue FRESH
        val d2 = evaluate(d.newState, setOf(1), moving = true, nowMs = t0 + 5_000)
        assertEquals(listOf(1), d2.announceSeats)
    }

    @Test
    fun `regra 3 - quem ignorou nao e mais incomodado`() {
        val s1 = evaluate(emptyMap(), setOf(1), true, t0).newState
        val d2 = evaluate(s1, setOf(1), true, t0 + 3_600_000)
        assertTrue(d2.announceSeats.isEmpty())
        assertNull(d2.recheckInMs)
    }

    @Test
    fun `regra 2 - prendeu soltou e ficou solto 30s avisa de novo`() {
        val s1 = evaluate(emptyMap(), setOf(1), true, t0).newState
        // prendeu
        val s2 = evaluate(s1, emptySet(), true, t0 + 10_000).newState
        assertEquals(SeatPhase.ARMED, s2[1]?.phase)
        // soltou de novo -> janela de 30s, sem falar ainda
        val d3 = evaluate(s2, setOf(1), true, t0 + 20_000)
        assertTrue(d3.announceSeats.isEmpty())
        assertEquals(REARM_GRACE_MS, d3.recheckInMs)
        assertEquals(SeatPhase.REARM_PENDING, d3.newState[1]?.phase)
        // 30s depois ainda solto -> fala
        val d4 = evaluate(d3.newState, setOf(1), true, t0 + 20_000 + REARM_GRACE_MS)
        assertEquals(listOf(1), d4.announceSeats)
        assertEquals(SeatPhase.ANNOUNCED, d4.newState[1]?.phase)
    }

    @Test
    fun `regra 2 - re-afivelou dentro dos 30s nao fala e continua armado`() {
        val s1 = evaluate(emptyMap(), setOf(1), true, t0).newState
        val s2 = evaluate(s1, emptySet(), true, t0 + 10_000).newState
        val s3 = evaluate(s2, setOf(1), true, t0 + 15_000).newState // soltou (pending)
        val d4 = evaluate(s3, emptySet(), true, t0 + 25_000) // prendeu aos 10s da janela
        assertTrue(d4.announceSeats.isEmpty())
        assertEquals(SeatPhase.ARMED, d4.newState[1]?.phase)
        assertNull(d4.recheckInMs)
    }

    @Test
    fun `janela parcial pede recheck do tempo restante`() {
        val s1 = evaluate(emptyMap(), setOf(1), true, t0).newState
        val s2 = evaluate(s1, emptySet(), true, t0 + 5_000).newState
        val d3 = evaluate(s2, setOf(1), true, t0 + 10_000) // abre a janela
        // re-checagem no meio da janela (ex.: outro evento) recalcula o restante
        val d4 = evaluate(d3.newState, setOf(1), true, t0 + 10_000 + 12_000)
        assertTrue(d4.announceSeats.isEmpty())
        assertEquals(REARM_GRACE_MS - 12_000, d4.recheckInMs)
    }

    @Test
    fun `janela vencida mas parado espera andar`() {
        val s1 = evaluate(emptyMap(), setOf(1), true, t0).newState
        val s2 = evaluate(s1, emptySet(), true, t0 + 5_000).newState
        val s3 = evaluate(s2, setOf(1), true, t0 + 10_000).newState
        // 30s+ depois, mas parado -> não fala, segue pendente
        val d4 = evaluate(s3, setOf(1), false, t0 + 10_000 + REARM_GRACE_MS + 5_000)
        assertTrue(d4.announceSeats.isEmpty())
        assertEquals(SeatPhase.REARM_PENDING, d4.newState[1]?.phase)
        // voltou a andar -> fala
        val d5 = evaluate(d4.newState, setOf(1), true, t0 + 10_000 + REARM_GRACE_MS + 10_000)
        assertEquals(listOf(1), d5.announceSeats)
    }

    @Test
    fun `assentos independentes - um anunciado nao trava o outro`() {
        val s1 = evaluate(emptyMap(), setOf(1), true, t0).newState
        // outro assento solta depois: fala só ele, na hora (sem cooldown global)
        val d2 = evaluate(s1, setOf(1, 3), true, t0 + 5_000)
        assertEquals(listOf(3), d2.announceSeats)
        assertEquals(SeatPhase.ANNOUNCED, d2.newState[1]?.phase)
    }

    @Test
    fun `reset de ignicao volta tudo pra fresh e re-avisa`() {
        val s1 = evaluate(emptyMap(), setOf(1), true, t0).newState
        assertTrue(s1.isNotEmpty())
        // manager zera o mapa no ciclo de ignição
        val d2 = evaluate(emptyMap(), setOf(1), true, t0 + 500_000)
        assertEquals(listOf(1), d2.announceSeats)
    }

    @Test
    fun `afivelado sem historico continua fora do mapa`() {
        val d = evaluate(emptyMap(), emptySet(), true, t0)
        assertTrue(d.announceSeats.isEmpty())
        assertTrue(d.newState.isEmpty())
        assertNull(d.recheckInMs)
    }

    @Test
    fun `recheck e o menor prazo entre assentos pendentes`() {
        // assento 1 pendente há 20s (faltam 10s), assento 2 acabou de soltar (faltam 30s)
        val state = mapOf(
            1 to SeatState(SeatPhase.REARM_PENDING, t0 - 20_000),
            2 to SeatState(SeatPhase.ARMED, t0 - 25_000)
        )
        val d = evaluate(state, setOf(1, 2), true, t0)
        assertNotNull(d.recheckInMs)
        assertEquals(10_000L, d.recheckInMs)
    }
}
