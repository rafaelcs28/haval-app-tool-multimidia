package br.com.redesurftank.havalshisuku.managers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterCardSyncPolicyTest {
    @Test
    fun recentSyntheticMainMenuNavigationIgnoresDivergentNativeAcEcho() {
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                3,
                616L,
                1027,
                616L,
                1
            )
        )
    }

    @Test
    fun recentSyntheticNavigationIgnoresMatchingNativeEcho() {
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                1,
                616L,
                1027,
                616L,
                1
            )
        )
    }

    @Test
    fun staleNativeZeroWithoutRecentInputIsIgnored() {
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                0,
                -1L,
                -1,
                -1L,
                -1
            )
        )
    }

    @Test
    fun nativeCardChangeWithoutSyntheticNavigationIsAccepted() {
        assertFalse(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                1,
                3,
                -1L,
                -1,
                -1L,
                -1
            )
        )
    }

    // Bug reportado (CarPlay no cluster): na A/C (card 3), o OEM empurra a volta pro menu (card 1)
    // a cada ~3s; sem input recente do usuário, isso deve ser IGNORADO (A/C fica fixa).
    @Test
    fun aircronCardStickyAgainstNativeRevertToMenuWithoutRecentInput() {
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                3,
                1,
                7408L,
                1027,
                7408L,
                3
            )
        )
    }

    // Mas se o usuário está navegando (toque LEFT/RIGHT recente), sair da A/C é HONRADO.
    @Test
    fun aircronCardLeavesOnRecentNavigationInput() {
        assertFalse(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                3,
                1,
                500L,
                1027,
                -1L,
                -1
            )
        )
    }
}
