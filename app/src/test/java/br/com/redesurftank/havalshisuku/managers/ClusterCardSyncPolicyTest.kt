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

    // Bug reportado (nosso carro, CarPlay projetando): na A/C (card 3), o OEM empurra a volta pro
    // menu (card 1) a cada ~3s; deve ser IGNORADO (A/C fica fixa) mesmo passada a janela.
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

    // Bug do usuário .73 (sem projeção): o OEM reverte a A/C pro menu em <2s — DENTRO da janela de
    // input (2,5s) mas depois da sintética (1,5s). Antes o fix por input honrava e a A/C sumia.
    // Agora tem que IGNORAR (A/C fica).
    @Test
    fun aircronCardStickyAgainstFastRevertToMenuWithinInputWindow() {
        assertTrue(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                3,
                1,
                1600L,
                1027,
                1600L,
                3
            )
        )
    }

    // Sair da A/C DE PROPÓSITO: nav sintética recente com alvo = menu -> HONRA (bloco sintético).
    @Test
    fun aircronLeavesToMenuOnDeliberateSyntheticNavigation() {
        assertFalse(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                3,
                1,
                600L,
                1027,
                600L,
                1
            )
        )
    }

    // Mudança nativa da A/C pra OUTRO card (não-menu) = navegação real -> HONRA.
    @Test
    fun aircronHonorsNativeChangeToAnotherCard() {
        assertFalse(
            ClusterCardSyncPolicy.shouldIgnoreNativeClusterCardChanged(
                3,
                4,
                7408L,
                1027,
                7408L,
                3
            )
        )
    }
}
