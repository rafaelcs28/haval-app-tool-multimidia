package br.com.redesurftank.havalshisuku.managers;

public final class ClusterCardSyncPolicy {
    private static final int MAIN_MENU_CARD = 1;
    private static final int AIRCON_CARD = 3;
    private static final int CLUSTER_KEY_LEFT = 1026;
    private static final int CLUSTER_KEY_RIGHT = 1027;
    private static final long NATIVE_CLUSTER_CARD_INPUT_WINDOW_MS = 2500L;
    private static final long SYNTHETIC_CLUSTER_CARD_ECHO_WINDOW_MS = 1500L;

    private ClusterCardSyncPolicy() {
    }

    public static boolean shouldIgnoreNativeClusterCardChanged(
            int previousCard,
            int nextCard,
            long sinceInputMs,
            int lastInputKeyCode,
            long sinceSyntheticMs,
            int lastSyntheticTarget
    ) {
        if (previousCard == nextCard) return true;

        if (isRecentSyntheticClusterCardNavigation(sinceSyntheticMs, lastSyntheticTarget)) {
            return nextCard != lastSyntheticTarget;
        }

        // O card de A/C (aircon) é uma seleção deliberada do usuário. Com CarPlay/AA projetando, o
        // cluster do OEM fica empurrando reversão pro card default (ex.: from=3_to=1) a cada ~3s;
        // sem esta trava, a A/C some sozinha depois de ~4s (passada a janela sintética de 1,5s), pois
        // o ramo "nextCard != 0 -> honra" abaixo aceitava a volta pro menu. Mantém a A/C fixa,
        // honrando a saída dela SÓ quando o usuário está de fato navegando (toque LEFT/RIGHT recente).
        if (previousCard == AIRCON_CARD) {
            return !isRecentClusterCardNavigationInput(lastInputKeyCode, sinceInputMs);
        }

        if (nextCard != 0) return false;
        if (previousCard != MAIN_MENU_CARD) return false;
        return !isRecentClusterCardNavigationInput(lastInputKeyCode, sinceInputMs);
    }

    private static boolean isRecentSyntheticClusterCardNavigation(long sinceSyntheticMs, int lastSyntheticTarget) {
        return lastSyntheticTarget >= 0
                && sinceSyntheticMs >= 0L
                && sinceSyntheticMs <= SYNTHETIC_CLUSTER_CARD_ECHO_WINDOW_MS;
    }

    private static boolean isRecentClusterCardNavigationInput(int lastInputKeyCode, long sinceInputMs) {
        return (lastInputKeyCode == CLUSTER_KEY_LEFT || lastInputKeyCode == CLUSTER_KEY_RIGHT)
                && sinceInputMs >= 0L
                && sinceInputMs <= NATIVE_CLUSTER_CARD_INPUT_WINDOW_MS;
    }
}
