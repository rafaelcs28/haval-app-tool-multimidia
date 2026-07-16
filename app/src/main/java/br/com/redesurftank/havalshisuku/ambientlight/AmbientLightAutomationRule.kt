package br.com.redesurftank.havalshisuku.ambientlight

import org.json.JSONArray
import org.json.JSONObject

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
 * Uma regra de automação. Serializada como JSON (lista) nas prefs — via [toJsonArray]/[fromJsonArray]
 * (parse MANUAL com chaves explícitas: Gson reflexivo quebrava entre builds porque o R8 renomeia
 * campos/constantes, deixando enum nulo em campo não-nulo -> crash na UI e no motor).
 * @param periodMs período do piscar/pulsar (ms). @param priority maior vence quando 2+ ativas.
 * @param onlyWhenStopped só dispara com o carro parado (velocidade 0) — ex.: ponto cego como
 *  alerta de "não abra a porta", sem piscar a fita enquanto dirige.
 */
data class AmbientLightAutomationRule(
    val condition: AutomationCondition,
    val enabled: Boolean = true,
    val r: Int = 255,
    val g: Int = 0,
    val b: Int = 0,
    val effect: AlertEffect = AlertEffect.BLINK,
    val periodMs: Int = 1000,
    val priority: Int = 0,
    val onlyWhenStopped: Boolean = false
) {
    fun color(): LedColor = LedColor(r, g, b).coerce()

    companion object {
        const val MIN_PERIOD_MS = 200
        const val MAX_PERIOD_MS = 5000

        /** Regras de fábrica: os 2 casos pedidos pelo usuário + 2 extras desligados. */
        fun defaults(): List<AmbientLightAutomationRule> =
            listOf(
                // Ponto cego = mais urgente (2x/s = 500ms), vermelho. Só com o carro PARADO:
                // é o aviso de "vem carro, não abra a porta" — andando, o BSD do retrovisor basta.
                AmbientLightAutomationRule(
                    condition = AutomationCondition.BLIND_SPOT,
                    enabled = true, r = 255, g = 0, b = 0,
                    effect = AlertEffect.BLINK, periodMs = 500, priority = 20,
                    onlyWhenStopped = true
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

        /**
         * Desserializa a lista de regras. TOLERANTE por construção: entrada nula/vazia/corrompida
         * ou regra com condição desconhecida (ex.: nome ofuscado salvo por build antigo) -> descarta
         * a regra; se nada sobrar, retorna null (chamador cai nos defaults). NUNCA lança.
         */
        fun fromJsonArray(json: String?): List<AmbientLightAutomationRule>? {
            if (json.isNullOrBlank()) return null
            return runCatching {
                val arr = JSONArray(json)
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val cond = AutomationCondition.fromStored(o.optString("condition"))
                        ?: return@mapNotNull null
                    AmbientLightAutomationRule(
                        condition = cond,
                        enabled = o.optBoolean("enabled", true),
                        r = o.optInt("r", 255).coerceIn(0, 255),
                        g = o.optInt("g", 0).coerceIn(0, 255),
                        b = o.optInt("b", 0).coerceIn(0, 255),
                        effect = AlertEffect.fromStored(o.optString("effect")),
                        periodMs = o.optInt("periodMs", 1000).coerceIn(MIN_PERIOD_MS, MAX_PERIOD_MS),
                        priority = o.optInt("priority", 0),
                        // Regra antiga sem o campo: ponto cego herda o default seguro (só parado).
                        onlyWhenStopped =
                            o.optBoolean("onlyWhenStopped", cond == AutomationCondition.BLIND_SPOT)
                    )
                }.takeIf { it.isNotEmpty() }
            }.getOrNull()
        }

        fun toJsonArray(rules: List<AmbientLightAutomationRule>): String {
            val arr = JSONArray()
            rules.forEach { rule ->
                arr.put(
                    JSONObject()
                        .put("condition", rule.condition.name)
                        .put("enabled", rule.enabled)
                        .put("r", rule.r)
                        .put("g", rule.g)
                        .put("b", rule.b)
                        .put("effect", rule.effect.name)
                        .put("periodMs", rule.periodMs)
                        .put("priority", rule.priority)
                        .put("onlyWhenStopped", rule.onlyWhenStopped)
                )
            }
            return arr.toString()
        }
    }
}
