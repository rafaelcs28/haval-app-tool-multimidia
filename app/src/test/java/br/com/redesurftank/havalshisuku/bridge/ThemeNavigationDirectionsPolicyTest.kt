package br.com.redesurftank.havalshisuku.bridge

import br.com.redesurftank.havalshisuku.managers.AndroidAutoNavigationTelemetry
import br.com.redesurftank.havalshisuku.managers.AndroidAutoTelemetryKeys
import br.com.redesurftank.havalshisuku.navigation.CarPlayHudPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeNavigationDirectionsPolicyTest {
    private val directions = AndroidAutoNavigationTelemetry.Directions(
        active = true, street = "Test road", distance = "150 m", distanceM = 150, turn = "TURN_RIGHT"
    )
    private val raw = directions.toJson()
    private val inactive = AndroidAutoNavigationTelemetry.inactive().toJson()

    private fun value(key: String, rawValue: String, shown: Boolean): String =
        ThemeNavigationDirectionsPolicy.valueForTheme(key, rawValue, shown, inactive)

    @Test fun identicalRawDirectionsReachTheThemeOnOffAndOnWhileDuplicatesStaySuppressed() {
        var previous: String? = null
        val delivered = mutableListOf<String>()
        for (shown in listOf(true, true, false, false, true, true)) {
            val next = value(AndroidAutoTelemetryKeys.DIRECTIONS, raw, shown)
            if (next != previous) delivered += next
            previous = next
        }
        assertEquals(listOf(raw, inactive, raw), delivered)
    }

    @Test fun changingHiddenDirectionsStaysInactiveAndEnablingRestoresTheLatestRawSnapshot() {
        val latest = directions.copy(distance = "90 m", distanceM = 90).toJson()
        assertEquals(inactive, value(AndroidAutoTelemetryKeys.DIRECTIONS, raw, false))
        assertEquals(inactive, value(AndroidAutoTelemetryKeys.DIRECTIONS, latest, false))
        assertEquals(latest, value(AndroidAutoTelemetryKeys.DIRECTIONS, latest, true))
    }

    @Test fun initialReadOrReloadHonorsCurrentVisibilityWithoutDependingOnPreviousPushes() {
        assertEquals(inactive, value(AndroidAutoTelemetryKeys.DIRECTIONS, raw, false))
        assertEquals(raw, value(AndroidAutoTelemetryKeys.DIRECTIONS, raw, true))
        assertEquals(inactive, value(AndroidAutoTelemetryKeys.DIRECTIONS, inactive, true))
    }

    @Test fun hidingThemeDirectionsDoesNotChangeTheHudArbiterOrSourcePayload() {
        val hud = CarPlayHudPolicy().apply { onAndroidAuto(directions) }
        assertEquals(inactive, value(AndroidAutoTelemetryKeys.DIRECTIONS, raw, false))
        assertEquals(directions, hud.snapshot(1_000))
        assertTrue(hud.snapshot(1_000).active)
        assertEquals(raw, hud.snapshot(1_000).toJson())
    }

    @Test fun unrelatedCanonicalAndLegacyKeysKeepTheirValuesAndAliasMappings() {
        for ((key, payload) in listOf(
            "app.navigation.radar" to "{\"active\":true}",
            AndroidAutoTelemetryKeys.SESSION to "active",
            "car.basic.speed" to "42", "car.basic.vehicle_speed" to "42",
            "car.basic.gear" to "P", "app.preferences.showNavigationCard" to "false"
        )) {
            assertEquals(payload, value(key, payload, false))
            assertEquals(payload, value(BridgeContractTranslator.translateThemeKeyToCanonical(key), payload, false))
        }
        assertEquals("car.basic.vehicle_speed", BridgeContractTranslator.translateThemeKeyToCanonical("car.basic.speed"))
        assertEquals("car.basic.speed", BridgeContractTranslator.translateCanonicalToThemeKey("car.basic.vehicle_speed"))
    }
}
