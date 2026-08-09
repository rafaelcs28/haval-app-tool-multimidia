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
 * Marcador manual de bug (botão vermelho no cabeçalho do dashboard). Ao tocar:
 *  (1) marca o instante no logcat (tag [TAG]) e no arquivo local [MARKS_FILE];
 *  (2) em background, coleta MUITA informação — a base do "Reportar problema"
 *      ([ProblemReportBuilder]: relato + versão + log persistente do cluster + logcat filtrado)
 *      MAIS o logcat COMPLETO do sistema (root via Shizuku) — e SOBE pro repo privado do usuário
 *      no GitHub via [BugReportUploader]. NÃO passa pela função de report do dev;
 *  (3) toasts de confirmação.
 *
 * Depois, offline, o usuário avisa "bug #N, foi tal coisa" e o assistente puxa o relatório do
 * GitHub e analisa com calma.
 */
object BugMarker {
    private const val TAG = "IMPULSE_BUG_MARK"
    private const val DIR = "/data/local/tmp/impulse-trip"
    private const val MARKS_FILE = "$DIR/bug-marks.txt"
    private const val RAW_LOGCAT_LINES = "4000"

    private val counter = AtomicInteger(0)
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    private val stampFmt = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    @JvmStatic
    fun mark(context: Context) {
        val app = context.applicationContext
        val n = counter.incrementAndGet()
        val ts = fmt.format(Date())
        // (1) marcador imediato: logcat + arquivo local persistente
        Log.w(TAG, "===== BUG MARK #$n @ $ts (marcado pelo usuario) =====")
        toast(app, "🐞 Bug #$n marcado ($ts) — coletando…")

        // (2) coleta pesada + upload, fora da main thread
        Thread({
            runCatching {
                ShizukuUtils.runCommandAndGetOutput(
                    arrayOf("sh", "-c", "mkdir -p $DIR && echo \"#$n  $ts\" >> $MARKS_FILE")
                )
            }
            val result =
                runCatching { collectAndUpload(app, n) }
                    .getOrElse {
                        Log.w(TAG, "coleta/upload falhou", it)
                        BugReportUploader.Result.Failure(it.message ?: "erro")
                    }
            toast(app, describe(n, result))
        }, "bug-mark-$n").start()
    }

    private fun collectAndUpload(context: Context, n: Int): BugReportUploader.Result {
        val stamp = stampFmt.format(Date())
        // Coleta curada (MESMA base do "Reportar problema").
        val curated =
            runCatching {
                val input =
                    ProblemReportInput(
                        description = "Bug #$n marcado no dashboard Impulse (viagem)",
                        carPlayConnected = false,
                        androidAutoConnected = false,
                        mirroredOnD3 = false,
                        notMirroredOnD3 = false,
                        mapReturnedToNormal = false,
                        acReturnedToMainMenu = false
                    )
                ProblemReportBuilder.build(context, input).fullBody
            }.getOrElse { "## Coleta curada indisponivel: ${it.message}" }

        // Logcat COMPLETO do sistema (root via Shizuku) — a "muita informacao".
        val rawLogcat =
            runCatching {
                ShizukuUtils.runCommandAndGetOutput(
                    arrayOf(
                        "logcat", "-d", "-b", "main,system,crash",
                        "-v", "threadtime", "-t", RAW_LOGCAT_LINES
                    )
                )
            }.getOrDefault("").ifBlank { "(logcat completo indisponivel)" }

        val body = buildString {
            appendLine("# Bug #$n")
            appendLine()
            appendLine("- Marcado em: $stamp (hora do carro)")
            appendLine("- Origem: botao Bug do dashboard Impulse (upload automatico)")
            appendLine()
            appendLine("---")
            appendLine()
            append(curated)
            appendLine()
            appendLine()
            appendLine("## Logcat completo do sistema (root via Shizuku, ultimas $RAW_LOGCAT_LINES linhas)")
            appendLine()
            appendLine("```text")
            append(rawLogcat)
            appendLine()
            appendLine("```")
        }
        return BugReportUploader.upload(context, body, n, stamp)
    }

    private fun describe(n: Int, r: BugReportUploader.Result): String =
        when (r) {
            is BugReportUploader.Result.Success -> "☁️ Bug #$n enviado ao GitHub"
            is BugReportUploader.Result.NoToken -> "🐞 Bug #$n salvo no carro — falta o token do GitHub"
            is BugReportUploader.Result.HttpError -> "⚠️ Bug #$n: GitHub erro ${r.code}"
            is BugReportUploader.Result.Failure -> "⚠️ Bug #$n: upload falhou (salvo no carro)"
        }

    private fun toast(context: Context, msg: String) {
        Handler(Looper.getMainLooper()).post {
            runCatching { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        }
    }
}
