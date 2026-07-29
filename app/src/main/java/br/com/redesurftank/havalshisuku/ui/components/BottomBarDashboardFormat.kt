package br.com.redesurftank.havalshisuku.ui.components

import kotlin.math.roundToInt

// Helpers PUROS do dashboard Impulse (formatadores, mapeadores de label, parsers de nível do
// banco), extraídos do BottomBarUI.kt (Lote E, Fase 1). SEM @Composable, SEM estado, SEM efeito
// colateral — só stdlib. São `internal` pra continuarem visíveis pro BottomBarUI (mesmo módulo).
// projectionLabel/shortProjectionLabel usam as consts BOTTOM_BAR_*_PACKAGE (agora `internal`, em
// BottomBarUI.kt). Move mecânico: comportamento idêntico ao original.

internal fun nextSeatVentilationLevel(currentLevel: String, maxLevel: String): String {
        val max = parseSeatVentilationMaxLevel(maxLevel).coerceAtMost(3)
        val current = parseSeatVentilationLevel(currentLevel, max.toString())
        return if (current >= max) "0" else (current + 1).toString()
}

internal fun parseSeatVentilationLevel(value: String, maxLevel: String): Int {
        val max = parseSeatVentilationMaxLevel(maxLevel)
        return value.toIntOrNull()?.coerceIn(0, max) ?: 0
}

internal fun parseSeatVentilationMaxLevel(value: String): Int {
        return value.toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(5) ?: 3
}

internal fun formatMediaTime(valueMs: Long, unknownWhenZero: Boolean = false): String {
        if (unknownWhenZero && valueMs <= 0L) return "--:--"
        val totalSeconds = (valueMs.coerceAtLeast(0L) / 1000L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}

internal fun formatDashboardClock(): String {
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
}

internal fun projectionLabel(packageName: String?): String {
        return when (packageName) {
                BOTTOM_BAR_CARPLAY_PACKAGE -> "Apple CarPlay ativo no cluster"
                BOTTOM_BAR_ANDROID_AUTO_PACKAGE -> "Android Auto ativo no cluster"
                null -> "Dashboard do display 0"
                else -> "Projeção ativa"
        }
}

internal fun shortProjectionLabel(packageName: String?): String? {
        return when (packageName) {
                BOTTOM_BAR_CARPLAY_PACKAGE -> "Apple CarPlay"
                BOTTOM_BAR_ANDROID_AUTO_PACKAGE -> "Android Auto"
                null -> null
                else -> "Projecao"
        }
}

internal fun driveModeLabel(value: String): String {
        return when (value) {
                "2" -> "Eco"
                "1" -> "Sport"
                "3" -> "Neve"
                "4" -> "Areia"
                "5" -> "Lama"
                else -> "Normal"
        }
}

internal fun powerModelLabel(
        value: String,
        reserve: String = "1",
        socTarget: String = "50"
): String {
        return when (value) {
                "1" -> "EV Prior."
                "3" -> "EV"
                else -> {
                        // HEV: mostra o sub-modo; se Prioritário, anexa o % de bateria alvo.
                        if (reserve.trim() == "2") {
                                val pct = socTarget.trim().toIntOrNull()?.coerceIn(20, 80) ?: 50
                                "HEV Prior. $pct%"
                        } else {
                                "HEV Intel."
                        }
                }
        }
}

internal fun regenLabel(value: String): String {
        return when (value) {
                "2" -> "Baixo"
                "1" -> "Alto"
                else -> "Normal"
        }
}

internal fun nextDashboardOption(currentValue: String, options: List<Pair<String, String>>): String {
        val currentIndex = options.indexOfFirst { it.first == currentValue }
        return options[(currentIndex + 1).coerceAtLeast(0) % options.size].first
}

internal fun nextDashboardOptionLabel(
        currentValue: String,
        options: List<Pair<String, String>>
): String {
        val nextValue = nextDashboardOption(currentValue, options)
        return options.firstOrNull { it.first == nextValue }?.second ?: nextValue
}

internal fun steeringModeLabel(value: String): String {
        return when (value) {
                "2" -> "Conforto"
                "1" -> "Sport"
                else -> "Normal"
        }
}

internal fun formatGear(value: String): String {
        return when (value.toIntOrNull()) {
                2 -> "D"
                3 -> "P"
                4 -> "R"
                else -> "N"
        }
}

internal fun formatSpeed(value: String): String {
        return value.toFloatOrNull()?.roundToInt()?.toString() ?: "--"
}

internal fun formatTemperature(value: String): String {
        val parsed = value.toFloatOrNull() ?: return "--"
        if (parsed <= -40f || parsed >= 85f || parsed == -1f || parsed == 255f) return "--"
        return String.format(java.util.Locale.US, "%.1f°C", parsed)
}

internal fun formatPercent(value: String): String {
        return value.toFloatOrNull()?.roundToInt()?.coerceIn(0, 100)?.let { "$it%" } ?: "--"
}

internal fun percentFraction(value: String): Float {
        return ((value.toFloatOrNull() ?: 0f) / 100f).coerceIn(0f, 1f)
}

internal fun formatDistance(value: String): String {
        return value.toFloatOrNull()?.roundToInt()?.let { "$it km" } ?: "--"
}

internal fun formatConsumption(value: String, suffix: String): String {
        val parsed = value.toFloatOrNull() ?: return "--"
        if (parsed <= 0f) return "--"
        return String.format(java.util.Locale.US, "%.1f %s", parsed, suffix)
}

internal fun calculateEvPowerKw(voltage: String, current: String): String {
        val volts = voltage.toFloatOrNull() ?: return "--"
        val amps = current.toFloatOrNull() ?: return "--"
        val kw = volts * amps / 1000f
        val label = if (kw < -0.5f) "REGEN" else "EV"
        val displayKw = if (kw < -0.5f) -kw else kw
        return String.format(java.util.Locale.US, "%s %.1f kW", label, displayKw)
}
