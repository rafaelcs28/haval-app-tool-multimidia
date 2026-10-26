package br.com.redesurftank.havalshisuku.bridge

import br.com.redesurftank.havalshisuku.managers.AndroidAutoTelemetryKeys

/** A theme-only view of directions; the source telemetry remains available to HUD and radar. */
object ThemeNavigationDirectionsPolicy {
    fun valueForTheme(
        key: String,
        rawValue: String,
        navigationShown: Boolean,
        inactiveDirections: String
    ): String =
        if (BridgeContractTranslator.translateThemeKeyToCanonical(key) == AndroidAutoTelemetryKeys.DIRECTIONS &&
            !navigationShown
        ) inactiveDirections else rawValue
}
