package br.com.redesurftank.havalshisuku.diagnostics

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Marcador manual de bug para viagens de validação. O usuário toca o botão vermelho no
 * cabeçalho do dashboard quando vê um problema. Isso:
 *  (1) escreve um marcador no logcat com uma tag distinta — que a [TripLogcatCapture] já grava
 *      em disco — então o marcador cai INLINE no trip.log no exato instante do problema;
 *  (2) anexa uma linha legível a um arquivo dedicado ([MARKS_FILE]), fácil de ler por telnet;
 *  (3) mostra um toast de confirmação.
 *
 * Assim, mesmo sem conexão ao vivo, dá pra achar depois o momento exato do bug no log da viagem.
 */
object BugMarker {
    private const val TAG = "IMPULSE_BUG_MARK"
    private const val DIR = "/data/local/tmp/impulse-trip"
    private const val MARKS_FILE = "$DIR/bug-marks.txt"

    private val counter = AtomicInteger(0)
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    @JvmStatic
    fun mark(context: Context) {
        val n = counter.incrementAndGet()
        val ts = fmt.format(Date())
        // (1) Marcador inline no logcat -> a TripLogcatCapture grava no trip.log no instante certo.
        Log.w(TAG, "===== BUG MARK #$n @ $ts (marcado pelo usuario) =====")
        // (2) Arquivo dedicado, persistente e compacto (sobrevive a reboot; fácil de ler por telnet).
        Thread({
            runCatching {
                ShizukuUtils.runCommandAndGetOutput(
                    arrayOf(
                        "sh", "-c",
                        "mkdir -p $DIR && echo \"#$n  $ts\" >> $MARKS_FILE"
                    )
                )
            }
        }, "bug-mark").start()
        // (3) Confirmação visual.
        Handler(Looper.getMainLooper()).post {
            runCatching {
                Toast.makeText(context, "🐞 Bug marcado #$n  •  $ts", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
