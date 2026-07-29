package br.com.redesurftank.havalshisuku.ui.components

import br.com.redesurftank.havalshisuku.managers.ServiceManager
import br.com.redesurftank.havalshisuku.models.CarConstants

// Leitura/seleção determinística de dados do veículo para o dashboard (Lote E, Fase 2).
// Extraído do BottomBarUI.kt: sem Compose, sem estado, sem mutação — recebe ServiceManager por
// parâmetro e apenas lê (getData) + seleciona/valida. Comportamento idêntico ao original.

internal fun readDashboardBatteryPercent(
        serviceManager: ServiceManager,
        overrideKey: String? = null,
        overrideValue: String? = null
): String {
        val currentKey = CarConstants.CAR_EV_INFO_CUR_BATTERY_POWER_PERCENTAGE.getValue()
        val socKey = CarConstants.CAR_EV_INFO_CAR_EV_INFO_SOC_OF_BATTERY.getValue()
        // REMOVIDO (2026-07-27): CAR_EV_INFO_BATTERY_POWER_PERCENTAGE
        // (`car.ev_info.battery_charge_percentage`) era a 3a candidata desta lista, mas ela e a
        // porcentagem da bateria 12V AUXILIAR, nao da de tracao — confirmado pelo bridge do proprio
        // usuario (mapeia essa chave para o campo `batt_12v_pct`) e agora exibida no bloco 12V do card
        // de dinamica. Como fallback aqui, se as duas chaves de tracao viessem invalidas, o anel
        // "Bateria" mostraria a carga da 12V (ex.: 84%) no lugar da carga real de tracao.
        val values =
                listOf(
                        valueForDashboardBatteryKey(serviceManager, currentKey, overrideKey, overrideValue),
                        valueForDashboardBatteryKey(serviceManager, socKey, overrideKey, overrideValue)
                )
        return selectDashboardBatteryPercent(values) ?: "--"
}

internal fun readDashboardFuelRange(
        serviceManager: ServiceManager,
        overrideKey: String? = null,
        overrideValue: String? = null
): String {
        val fuelModeKey = CarConstants.CAR_EV_INFO_FUEL_MODE_REMAIN_ODOMETER.getValue()
        val totalRemainKey = CarConstants.CAR_BASIC_REMAIN_ODOMETER.getValue()
        val values =
                listOf(
                        valueForDashboardFuelRangeKey(
                                serviceManager,
                                fuelModeKey,
                                overrideKey,
                                overrideValue
                        ),
                        valueForDashboardFuelRangeKey(
                                serviceManager,
                                totalRemainKey,
                                overrideKey,
                                overrideValue
                        )
                )
        return selectDashboardRange(values) ?: "--"
}

internal fun valueForDashboardBatteryKey(
        serviceManager: ServiceManager,
        key: String,
        overrideKey: String?,
        overrideValue: String?
): String? {
        return if (key == overrideKey) overrideValue else serviceManager.getData(key)
}

internal fun valueForDashboardFuelRangeKey(
        serviceManager: ServiceManager,
        key: String,
        overrideKey: String?,
        overrideValue: String?
): String? {
        return if (key == overrideKey) overrideValue else serviceManager.getData(key)
}

internal fun selectDashboardBatteryPercent(values: List<String?>): String? {
        val normalized = values.mapNotNull { it?.trim()?.takeIf { value -> value.isNotEmpty() } }
        return normalized.firstOrNull { isValidDashboardPercent(it, allowZero = false) }
                ?: normalized.firstOrNull { isValidDashboardPercent(it, allowZero = true) }
}

internal fun selectDashboardRange(values: List<String?>): String? {
        val normalized = values.mapNotNull { it?.trim()?.takeIf { value -> value.isNotEmpty() } }
        return normalized.firstOrNull { (it.toFloatOrNull() ?: -1f) > 0f }
                ?: normalized.firstOrNull { (it.toFloatOrNull() ?: -1f) >= 0f }
}

internal fun isValidDashboardPercent(value: String, allowZero: Boolean): Boolean {
        val parsed = value.toFloatOrNull() ?: return false
        return parsed in 0f..100f && (allowZero || parsed > 0f)
}
