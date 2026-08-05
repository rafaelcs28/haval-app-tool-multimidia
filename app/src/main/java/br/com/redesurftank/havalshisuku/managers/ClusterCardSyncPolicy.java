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

        // O card de A/C (aircon) é uma seleção deliberada do usuário. O cluster do OEM reverte pro
        // card default (menu) sozinho — em <2s sem projeção, ou repetidamente (~3s) com CarPlay
        // projetando. O ramo "nextCard != 0 -> honra" abaixo aceitava essa volta pro menu (card 1)
        // assim que a janela sintética de 1,5s expirava, fazendo a A/C sumir. Aqui: estando na A/C,
        // IGNORA a reversão espúria pro menu (card 0/1) SEMPRE. Sair da A/C de propósito é via
        // navegação (LEFT/RIGHT), que dispara nav sintética pro alvo -> honrada pelo bloco sintético
        // acima (nextCard == lastSyntheticTarget). Mudança nativa pra OUTRO card (navegação real do
        // OEM) segue honrada.
        if (previousCard == AIRCON_CARD && (nextCard == 0 || nextCard == MAIN_MENU_CARD)) {
            return true;
        }

        // O card 1 (menu) é o card DEFAULT do cluster do OEM — é onde projetamos o menu. Quando o
        // mapa/nav fica ocioso, o OEM volta pro default sozinho (native_cluster_card_changed 0->1) e
        // a persistência abaixo então PRENDE o menu sobre o mapa (o menu "insiste em voltar" — bug
        // confirmado no log persistente: subida espontânea com sinceInputMs de minutos, seguida de
        // dezenas de "from=1 to=0" ignorados). O menu só deve subir por AÇÃO DO USUÁRIO: sem toque
        // LEFT/RIGHT recente (nem nav sintética recente, já tratada acima), IGNORA a subida
        // espontânea pro menu. Assim o mapa continua vencendo; o menu que o usuário abre segue
        // honrado (pela janela sintética/de input).
        if (nextCard == MAIN_MENU_CARD
                && !isRecentClusterCardNavigationInput(lastInputKeyCode, sinceInputMs)) {
            return true;
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
