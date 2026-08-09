package br.com.redesurftank.havalshisuku.diagnostics

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sobe um [ProblemReport] — a MESMA coleta do "Reportar problema" (relato + versão + log
 * persistente do cluster do dia + snapshot de logcat filtrado) — pro repositório PRIVADO do
 * usuário no GitHub, em vez de mandar pro dev. Assim os bugs marcados na viagem ficam no GitHub
 * e podem ser puxados/analisados depois SEM telnet.
 *
 * Auth: token do GitHub fornecido pelo USUÁRIO — lido de uma SharedPreference (preenchida num campo
 * do app) OU de [TOKEN_FILE]. O app NUNCA embute token. Recomenda-se um fine-grained escopado só no
 * repo [REPO], permissão Contents: write.
 */
object BugReportUploader {
    private const val TAG = "BugReportUploader"
    const val PREFS_NAME = "impulse_bug_reporter"
    const val TOKEN_PREF_KEY = "gh_token"
    private const val TOKEN_FILE = "/data/local/tmp/impulse-gh-token"
    const val REPO = "rafaelcs28/impulse-bug-reports"
    private const val MAX_BODY_CHARS = 900_000
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 30_000

    private val gson = Gson()

    sealed class Result {
        data class Success(val path: String, val htmlUrl: String?) : Result()
        object NoToken : Result()
        data class HttpError(val code: Int, val message: String) : Result()
        data class Failure(val message: String) : Result()
    }

    fun hasToken(context: Context): Boolean = !resolveToken(context).isNullOrEmpty()

    fun resolveToken(context: Context): String? {
        val pref = runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(TOKEN_PREF_KEY, null)
        }.getOrNull()?.trim()
        if (!pref.isNullOrEmpty()) return pref
        return runCatching { File(TOKEN_FILE).readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    /** Sobe o corpo Markdown em reports/bug-NNNN-<stamp>.md. Chamar FORA da main thread. */
    fun upload(context: Context, bodyMarkdown: String, bugNumber: Int, stamp: String): Result {
        val token = resolveToken(context) ?: return Result.NoToken
        val body =
            bodyMarkdown.let {
                if (it.length <= MAX_BODY_CHARS) it
                else "[relatorio truncado; ultimos $MAX_BODY_CHARS chars]\n" + it.takeLast(MAX_BODY_CHARS)
            }
        val fileName = "bug-%04d-%s.md".format(bugNumber, stamp)
        val path = "reports/$fileName"
        val url = "https://api.github.com/repos/$REPO/contents/$path"
        val contentB64 = Base64.encodeToString(body.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val payloadBytes =
            gson.toJson(mapOf("message" to "bug #$bugNumber @ $stamp", "content" to contentB64))
                .toByteArray(Charsets.UTF_8)

        var conn: HttpURLConnection? = null
        return try {
            conn =
                (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    useCaches = false
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("User-Agent", "ImpulseBugReporter")
                    setFixedLengthStreamingMode(payloadBytes.size)
                }
            conn.outputStream.use { it.write(payloadBytes) }
            val code = conn.responseCode
            val resp =
                runCatching {
                    (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()
                }.getOrDefault("")
            if (code in 200..299) {
                val htmlUrl =
                    runCatching {
                        JsonParser.parseString(resp).asJsonObject
                            .getAsJsonObject("content")
                            ?.get("html_url")
                            ?.asString
                    }.getOrNull()
                Log.w(TAG, "upload ok: $path")
                Result.Success(path, htmlUrl)
            } else {
                Log.w(TAG, "upload http $code: ${resp.take(200)}")
                Result.HttpError(code, resp.take(300))
            }
        } catch (t: Throwable) {
            Log.w(TAG, "upload falhou", t)
            Result.Failure(t.message ?: "falha de rede")
        } finally {
            conn?.disconnect()
        }
    }
}
