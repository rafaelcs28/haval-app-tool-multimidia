package br.com.redesurftank.havalshisuku.ambientlight

/**
 * Motor de automação do Ambient Light: uma condição do carro -> cor/efeito na fita.
 * NOTA: o hardware do usuário (kit "18 em 1" master + 4 escravos) só faz a fita INTEIRA junto
 * (não há endereçamento por zona/porta — confirmado no app nativo). Então o alvo é sempre TODA a
 * fita. Por isso não há campo de zona aqui — só condição, cor e efeito.
 */
enum class AutomationCondition(val label: String) {
    NO_SEATBELT("Sem cinto (qualquer assento)"),
    BLIND_SPOT("Ponto cego (qualquer lado)"),
    DOOR_OPEN("Porta aberta (qualquer)"),
    REVERSE_GEAR("Marcha ré engatada");

    companion object {
        fun fromStored(value: String?): AutomationCondition? =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

enum class AlertEffect(val label: String) {
    BLINK("Piscar"),
    PULSE("Pulsar"),
    SOLID("Fixo");

    companion object {
        val DEFAULT = BLINK

        fun fromStored(value: String?): AlertEffect =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Uma regra de automação. Serializada como JSON (lista) nas prefs.
 * @param periodMs período do piscar/pulsar (ms). @param priority maior vence quando 2+ ativas.
 */
data class AmbientLightAutomationRule(
    val condition: AutomationCondition,
    val enabled: Boolean = true,
    val r: Int = 255,
    val g: Int = 0,
    val b: Int = 0,
    val effect: AlertEffect = AlertEffect.BLINK,
    val periodMs: Int = 1000,
    val priority: Int = 0
) {
    fun color(): LedColor = LedColor(r, g, b).coerce()

    companion object {
        const val MIN_PERIOD_MS = 200
        const val MAX_PERIOD_MS = 5000

        /** Regras de fábrica: os 2 casos pedidos pelo usuário + 2 extras desligados. */
        fun defaults(): List<AmbientLightAutomationRule> =
            listOf(
                // Ponto cego = mais urgente (2x/s = 500ms), vermelho.
                AmbientLightAutomationRule(
                    condition = AutomationCondition.BLIND_SPOT,
                    enabled = true, r = 255, g = 0, b = 0,
                    effect = AlertEffect.BLINK, periodMs = 500, priority = 20
                ),
                // Sem cinto = vermelho, 1x/s (1000ms).
                AmbientLightAutomationRule(
                    condition = AutomationCondition.NO_SEATBELT,
                    enabled = true, r = 255, g = 0, b = 0,
                    effect = AlertEffect.BLINK, periodMs = 1000, priority = 10
                ),
                // Porta aberta = âmbar, piscar 1s (desligado por padrão).
                AmbientLightAutomationRule(
                    condition = AutomationCondition.DOOR_OPEN,
                    enabled = false, r = 255, g = 120, b = 0,
                    effect = AlertEffect.BLINK, periodMs = 1000, priority = 5
                ),
                // Ré = branco fixo (desligado por padrão).
                AmbientLightAutomationRule(
                    condition = AutomationCondition.REVERSE_GEAR,
                    enabled = false, r = 255, g = 255, b = 255,
                    effect = AlertEffect.SOLID, periodMs = 1000, priority = 1
                )
            )
    }
}
