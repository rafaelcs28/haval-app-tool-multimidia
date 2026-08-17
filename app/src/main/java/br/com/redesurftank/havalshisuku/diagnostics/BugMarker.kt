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
    private const val APP_LOGCAT_LINES = "1200"

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
        val rawLogcat = captureRootLogcat()
        // ...e o nosso proprio rastro, isolado por PID. O buffer cru e uma corrida: qualquer
        // app em laco (ja aconteceu: ~130 linhas/s de WifiService) empurra as nossas linhas
        // para fora e o relatorio chega cego. Filtrar por PID garante o rastro do app
        // independentemente do que mais esteja gritando no log.
        val ownPid = android.os.Process.myPid()
        val appLogcat = captureAppLogcat(ownPid)

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
            appendLine()
            appendLine("## Logcat SO DO APP (filtrado por PID $ownPid, ultimas $APP_LOGCAT_LINES linhas)")
            appendLine()
            appendLine("```text")
            append(appLogcat)
            appendLine()
            appendLine("```")
        }
        return BugReportUploader.upload(context, body, n, stamp)
    }

    /**
     * Logcat root via Shizuku com algumas tentativas. O Shizuku pode estar
     * transitoriamente fora do ar (comum em movimento) e [ShizukuUtils.runCommandAndGetOutput]
     * devolve "" nesse caso — SEM exceção. Antes, uma única falha virava "(indisponivel)"
     * sem retry e sem dizer o porquê, deixando o bug cego (ex.: report 20260813-184320).
     */
    /**
     * Rastro do PROPRIO app, isolado por PID. Sobrevive a qualquer flood de terceiros — o
     * buffer cru e disputado e um app em laco apaga as nossas linhas dele (ver [captureRootLogcat]).
     * Sem root da tambem certo: o processo sempre pode ler o proprio log.
     */
    private fun captureAppLogcat(pid: Int): String {
        val cmd = arrayOf("logcat", "-d", "-v", "threadtime", "-t", APP_LOGCAT_LINES, "--pid=$pid")
        // Via Shizuku primeiro (o buffer root ve tudo); cai para o processo local se indisponivel.
        runCatching { ShizukuUtils.runCommandAndGetOutput(cmd) }
            .getOrDefault("")
            .let { if (it.isNotBlank()) return it }
        return runCatching {
            val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().use { it.readText() }
            runCatching { p.destroy() }
            out
        }.getOrDefault("").ifBlank { "(logcat do app indisponivel)" }
    }

    private fun captureRootLogcat(): String {
        val cmd =
            arrayOf(
                "logcat", "-d", "-b", "main,system,crash",
                "-v", "threadtime", "-t", RAW_LOGCAT_LINES,
                // Silencia os floods que dominam o buffer, pra as 4000 linhas renderem MUITO
                // mais historico util (AA/VideoPlayer/projecao/erros):
                //  - Its_IntelligentVehicleControlService: spam de propriedade de CAN da OEM.
                //  - WifiService "Failed to start scan": ~130 linhas/s continuas no carro do
                //    usuario; sozinho reduzia as 4000 linhas a uma janela de 10s e apagava
                //    qualquer rastro do nosso app (report 20260817-083131).
                "Its_IntelligentVehicleControlService:S", "WifiService:S", "*:V"
            )
        repeat(4) { attempt ->
            val out = runCatching { ShizukuUtils.runCommandAndGetOutput(cmd) }.getOrDefault("")
            if (out.isNotBlank()) return out
            if (attempt < 3) runCatching { Thread.sleep(1500) }
        }
        // Falhou de novo: registra o PORQUE (Shizuku no ar?) pra proxima ser diagnosticavel.
        val shizukuUp = runCatching { ShizukuUtils.isShizukuAvailable() }.getOrDefault(false)
        return "(logcat completo indisponivel — shizukuPingBinder=$shizukuUp apos 4 tentativas)"
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
