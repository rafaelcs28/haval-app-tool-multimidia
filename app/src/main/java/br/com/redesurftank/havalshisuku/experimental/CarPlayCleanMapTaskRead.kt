package br.com.redesurftank.havalshisuku.experimental

internal data class CarPlayCleanMapTaskRead(
    val observation: CarPlayCleanMapTaskObservation,
    val startedAtElapsedMs: Long,
    val completedAtElapsedMs: Long
) {
    val durationMs: Long get() = completedAtElapsedMs - startedAtElapsedMs
}
