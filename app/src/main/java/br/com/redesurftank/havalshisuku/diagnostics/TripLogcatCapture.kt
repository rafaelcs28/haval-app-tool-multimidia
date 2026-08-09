package br.com.redesurftank.havalshisuku.diagnostics

import android.util.Log
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils

/**
 * Captura de logcat pra VIAGENS de validação: grava o logcat inteiro (main+system+crash) em arquivo
 * ROTATIVO em /data/local/tmp/impulse-trip/, iniciado sozinho no boot do app. Assim o log da viagem
 * inteira fica no carro pra ler por telnet DEPOIS (o buffer em RAM do logcat só guarda os últimos
 * minutos; processos iniciados por telnet morrem no reboot — daí precisar nascer do app).
 *
 * Como: spawn via Shizuku (root) de um logcat destacado (setsid) com rotação 8MB x 6 arquivos
 * (~48MB teto, ~2-3h de viagem com projeção). Guard por pgrep pra não duplicar em restart do app.
 * O bootstrap do Shizuku é lento (telnetd do carro) -> retry por até ~10min em thread própria.
 *
 * Gate: só em build -preview (IMPULSE_REPORT_DIAGNOSTICS_ENABLED), igual ao
 * ClusterPersistentEventLogger — build estável não carrega isso.
 *
 * Ler no carro:  ls -la /data/local/tmp/impulse-trip/  +  os trip.log*
 */
object TripLogcatCapture {

    private const val TAG = "TripLogcatCapture"
    private const val DIR = "/data/local/tmp/impulse-trip"
    private const val MARKER = "impulse-trip/trip.log" // usado no pgrep -f (guard de duplicidade)

    @Volatile private var started = false

    @JvmStatic
    fun start() {
        if (started) return
        started = true
        Thread({
            try {
                // Espera o Shizuku subir (bootstrap por telnet demora; retry ~10min)
                var tries = 0
                while (tries < 60) {
                    try {
                        if (ShizukuUtils.isShizukuAvailable()) break
                    } catch (ignored: Throwable) {}
                    Thread.sleep(10_000L)
                    tries++
                }
                if (tries >= 60) {
                    Log.w(TAG, "Shizuku nao subiu em ~10min; captura de viagem nao iniciada")
                    return@Thread
                }
                // Guard: já existe um logcat nosso gravando? (sobrevive a restart do APP, morre no reboot)
                val running = runCatching {
                    ShizukuUtils.runCommandAndGetOutput(
                            arrayOf("sh", "-c", "pgrep -f '$MARKER' 2>/dev/null || true")
                    ).trim()
                }.getOrDefault("")
                if (running.isNotEmpty()) {
                    Log.w(TAG, "captura de viagem ja ativa (pid=$running)")
                    return@Thread
                }
                // Spawn destacado: sh retorna na hora, logcat segue gravando com rotacao.
                ShizukuUtils.runCommandAndGetOutput(arrayOf(
                        "sh", "-c",
                        "mkdir -p $DIR && " +
                                "setsid logcat -v time -b main,system,crash " +
                                "-f $DIR/trip.log -r 8192 -n 6 </dev/null >/dev/null 2>&1 &"
                ))
                Log.w(TAG, "captura de viagem iniciada em $DIR/trip.log (8MBx6)")
            } catch (t: Throwable) {
                Log.e(TAG, "falha ao iniciar captura de viagem", t)
            }
        }, "trip-logcat").start()
    }
}
