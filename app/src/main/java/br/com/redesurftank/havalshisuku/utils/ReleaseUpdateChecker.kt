package br.com.redesurftank.havalshisuku.utils

import android.util.Log
import br.com.redesurftank.havalshisuku.TAG
import br.com.redesurftank.havalshisuku.models.ReleaseInfo
import br.com.redesurftank.havalshisuku.models.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ReleaseUpdateChecker {
    // Manifesto estático no CDN de download do GitHub (NÃO é a api.github.com, que tem limite de
    // 60 req/h por IP e era esgotada pela varredura de temas -> "Buscar atualização" não achava).
    // Publicado numa tag fixa (app-latest) e reescrito a cada release nova. Mesmo caminho confiável
    // que o updater do viewer 3D já usa (releases/download/<tag>/<arquivo>). Ver memória
    // haval-ota-manifesto-cdn.
    private const val MANIFEST_URL =
            "https://github.com/rafaelcs28/haval-app-tool-multimidia/releases/download/app-latest/app-preview.json"

    suspend fun getAllReleaseInfo(): UpdateCheckResult {
        return withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                // rnd= evita cache de CDN/proxy servir um manifesto velho.
                conn = URL("$MANIFEST_URL?rnd=${System.currentTimeMillis()}")
                        .openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.instanceFollowRedirects = true

                if (conn.responseCode != 200) return@withContext UpdateCheckResult(null, null)

                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val o = JSONObject(body)
                val tag = o.optString("tag", o.optString("versionName", "")).trim()
                val apkUrl = o.optString("apkUrl", "").trim()
                if (tag.isEmpty() || apkUrl.isEmpty()) return@withContext UpdateCheckResult(null, null)

                val prerelease = o.optBoolean("prerelease", tag.contains("preview"))
                val notes = o.optString("notes", "").trim()
                val info = ReleaseInfo(tag, apkUrl, prerelease, notes)

                if (prerelease) UpdateCheckResult(null, info) else UpdateCheckResult(info, null)
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching update manifest", e)
                UpdateCheckResult(null, null)
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = versionParts(v1)
        val parts2 = versionParts(v2)
        val maxSize = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxSize) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a > b) return 1
            if (a < b) return -1
        }

        return 0
    }

    fun isNewer(candidateVersion: String, currentVersion: String): Boolean {
        return compareVersions(candidateVersion, currentVersion) > 0
    }

    private fun versionParts(version: String): List<Int> {
        val clean = version.removePrefix("v").removeSuffix("-preview")
        return clean.split(".")
                .filter { it.isNotBlank() }
                .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    }
}
