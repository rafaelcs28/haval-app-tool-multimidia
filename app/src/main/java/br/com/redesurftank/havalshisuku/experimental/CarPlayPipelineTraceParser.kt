package br.com.redesurftank.havalshisuku.experimental

internal data class CarPlayPipelineTraceLine(
    val bootMs: Long,
    val generation: Long,
    val event: String,
    val status: Int,
    val value: Long,
    val auxiliary: Long
)

internal object CarPlayPipelineTraceParser {
    private val MESSAGE = Regex(
        """boot_ms=([0-9]{1,16}) gen=([0-9]{1,20}) event=([a-z0-9_]{1,40}) status=(-?[0-9]{1,10}) value=([0-9]{1,20}) aux=([0-9]{1,20})""")

    fun parse(raw: String): List<CarPlayPipelineTraceLine> = raw.lineSequence()
        .mapNotNull { line ->
            val match = MESSAGE.find(line) ?: return@mapNotNull null
            if (match.value.length > 220) return@mapNotNull null
            runCatching {
                CarPlayPipelineTraceLine(
                    match.groupValues[1].toLong(),
                    match.groupValues[2].toLong(),
                    match.groupValues[3],
                    match.groupValues[4].toInt(),
                    match.groupValues[5].toLong(),
                    match.groupValues[6].toLong())
            }.getOrNull()
        }
        .take(MAX_LINES)
        .toList()

    private const val MAX_LINES = 4_096
}
