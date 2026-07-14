package br.com.redesurftank.havalshisuku.projectors

internal object ProjectionD3StateHoldPolicy {
    fun shouldHoldCarPlayInDash(
            lastHealthyD3AtMs: Long,
            nowMs: Long,
            @Suppress("UNUSED_PARAMETER") desiredCluster: Boolean,
            @Suppress("UNUSED_PARAMETER") preparingD3: Boolean,
            holdMs: Long
    ): Boolean {
        // Segura o estado "CarPlay no cluster" durante um falso-negativo transitório do D3 quando o
        // CarPlay esteve comprovadamente saudável no D3 nos últimos holdMs. A recência-de-saúde é o
        // sinal correto: se estava REALMENTE no D3 há pouco e some por um instante (churn de task /
        // recriação de serviço), é glitch, não desconexão — o próximo ciclo re-detecta e nada pisca.
        //
        // Antes exigia (desiredCluster || preparingD3) como PRÉ-condição. Bug capturado (2026-07-09):
        // o watchdog limpou o alvo de cluster como "stale" com o CarPlay AINDA projetando, zerando
        // desiredCluster; depois um gap de task derrubou o estado p/ false e o tema piscou
        // (velocímetro) por ~5s. Com projeção real recente, a recência sozinha basta — desconexão
        // de verdade persiste além de holdMs e aí sim o estado cai.
        if (lastHealthyD3AtMs <= 0L) return false

        val elapsedMs = nowMs - lastHealthyD3AtMs
        return elapsedMs in 0..holdMs
    }
}
