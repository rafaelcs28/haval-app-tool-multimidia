package br.com.redesurftank.havalshisuku.ambientlight

/**
 * Posição física de uma zona/canal DMX do Ambient Light (mapeada pelo usuário na tela, via o
 * identificador de canal). Cada canal DMX do controlador é uma dessas posições. Usado depois pelo
 * motor de automação (condição do carro -> zona -> cor/efeito).
 */
enum class ZonePosition(val label: String) {
    UNASSIGNED("— não atribuído"),
    FRONT_LEFT("Porta dianteira esquerda"),
    FRONT_RIGHT("Porta dianteira direita"),
    REAR_LEFT("Porta traseira esquerda"),
    REAR_RIGHT("Porta traseira direita"),
    FRONT_PANEL("Painel frontal"),
    CENTER_CONSOLE("Console central"),
    OTHER("Outra");

    companion object {
        val DEFAULT = UNASSIGNED

        fun fromStored(value: String?): ZonePosition =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}
