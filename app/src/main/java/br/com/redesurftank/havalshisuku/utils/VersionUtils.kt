package br.com.redesurftank.havalshisuku.utils

object VersionUtils {
    /**
     * Compara duas versoes no formato "a.b.c..." numericamente.
     *
     * - Componentes faltantes contam como 0, entao "67.7.0" e igual a "67.7"
     *   (evita "update fantasma" quando uma tag tem um zero a mais).
     * - O sufixo "-preview" e ignorado.
     * - Componentes nao-numericos contam como 0.
     * - null/vazio contam como versao "0".
     *
     * Retorna > 0 se v1 > v2, < 0 se v1 < v2, e 0 se forem equivalentes.
     */
    fun compareVersions(v1: String?, v2: String?): Int {
        val parts1 = parse(v1)
        val parts2 = parse(v2)
        val max = maxOf(parts1.size, parts2.size)
        for (i in 0 until max) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    private fun parse(v: String?): List<Int> {
        if (v.isNullOrBlank()) return emptyList()
        // Tolera tag com prefixo "v" (ex.: "v1.0.0.67.11") e sufixo "-preview".
        return v.trim().removePrefix("v").removeSuffix("-preview")
                .split(".")
                .map { it.trim().toIntOrNull() ?: 0 }
    }
}
