package br.com.redesurftank.havalshisuku.ambientlight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientLightAutomationRuleTest {

    @Test
    fun `round-trip preserva todas as regras e campos`() {
        val rules = AmbientLightAutomationRule.defaults()
        val json = AmbientLightAutomationRule.toJsonArray(rules)
        val parsed = AmbientLightAutomationRule.fromJsonArray(json)
        assertEquals(rules, parsed)
    }

    @Test
    fun `json nulo ou vazio retorna null (chamador usa defaults)`() {
        assertNull(AmbientLightAutomationRule.fromJsonArray(null))
        assertNull(AmbientLightAutomationRule.fromJsonArray(""))
        assertNull(AmbientLightAutomationRule.fromJsonArray("   "))
    }

    @Test
    fun `json corrompido nao lanca e retorna null`() {
        assertNull(AmbientLightAutomationRule.fromJsonArray("{nao-e-array"))
        assertNull(AmbientLightAutomationRule.fromJsonArray("[\"string-nao-objeto\"]"))
    }

    @Test
    fun `condicao desconhecida (nome ofuscado de build antigo) e descartada`() {
        // Simula o JSON que o Gson+R8 do build antigo salvava: nomes ofuscados tipo "e"/"f".
        val json = """[
            {"condition":"e","enabled":true,"r":255,"g":0,"b":0,"effect":"f","periodMs":500,"priority":20},
            {"condition":"BLIND_SPOT","enabled":true,"r":255,"g":0,"b":0,"effect":"BLINK","periodMs":500,"priority":20}
        ]"""
        val parsed = AmbientLightAutomationRule.fromJsonArray(json)!!
        assertEquals(1, parsed.size)
        assertEquals(AutomationCondition.BLIND_SPOT, parsed[0].condition)
    }

    @Test
    fun `todas as regras ilegiveis retorna null`() {
        val json = """[{"condition":"a"},{"condition":"b"}]"""
        assertNull(AmbientLightAutomationRule.fromJsonArray(json))
    }

    @Test
    fun `campos ausentes usam defaults e onlyWhenStopped so vale true pra ponto cego`() {
        val json = """[
            {"condition":"BLIND_SPOT"},
            {"condition":"NO_SEATBELT"}
        ]"""
        val parsed = AmbientLightAutomationRule.fromJsonArray(json)!!
        val blindSpot = parsed.first { it.condition == AutomationCondition.BLIND_SPOT }
        val seatbelt = parsed.first { it.condition == AutomationCondition.NO_SEATBELT }
        assertTrue(blindSpot.onlyWhenStopped)
        assertFalse(seatbelt.onlyWhenStopped)
        assertTrue(blindSpot.enabled)
        assertEquals(AlertEffect.BLINK, blindSpot.effect)
        assertEquals(1000, blindSpot.periodMs)
    }

    @Test
    fun `onlyWhenStopped explicito no json vence o default`() {
        val json = """[{"condition":"BLIND_SPOT","onlyWhenStopped":false}]"""
        val parsed = AmbientLightAutomationRule.fromJsonArray(json)!!
        assertFalse(parsed[0].onlyWhenStopped)
    }

    @Test
    fun `periodMs fora da faixa e coergido`() {
        val json = """[
            {"condition":"BLIND_SPOT","periodMs":50},
            {"condition":"NO_SEATBELT","periodMs":99999}
        ]"""
        val parsed = AmbientLightAutomationRule.fromJsonArray(json)!!
        assertEquals(AmbientLightAutomationRule.MIN_PERIOD_MS, parsed[0].periodMs)
        assertEquals(AmbientLightAutomationRule.MAX_PERIOD_MS, parsed[1].periodMs)
    }

    @Test
    fun `defaults tem ponto cego so parado e cinto sempre`() {
        val defaults = AmbientLightAutomationRule.defaults()
        assertTrue(defaults.first { it.condition == AutomationCondition.BLIND_SPOT }.onlyWhenStopped)
        assertFalse(defaults.first { it.condition == AutomationCondition.NO_SEATBELT }.onlyWhenStopped)
    }
}
