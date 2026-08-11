package br.com.redesurftank.havalshisuku.bridge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.managers.DisplayAppLauncher
import br.com.redesurftank.havalshisuku.models.BottomBarState
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object VirtualTelemetryManager {
    private const val TAG = "VirtualTelemetry"

    fun drawableToBase64(drawable: Drawable?): String {
        if (drawable == null) return ""
        try {
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val bytes = outputStream.toByteArray()
            return "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting drawable to base64", e)
        }
        return ""
    }

    fun getLauncherAppsJson(context: Context): String {
        try {
            val configs = DisplayAppLauncher.getAllConfigs()
            val jsonArray = JSONArray()
            for ((index, config) in configs.withIndex()) {
                val pkg = config.packageName
                val info = DisplayAppLauncher.resolveAppInfo(context, pkg, config.customName)
                val appObj = JSONObject().apply {
                    put("packageName", pkg)
                    put("activityName", config.activityName ?: "")
                    put("displayName", info.label)
                    put("icon", drawableToBase64(info.icon))
                    put("displayId", config.displayId)
                    put("position", index)
                }
                jsonArray.put(appObj)
            }
            return jsonArray.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build launcher apps JSON", e)
        }
        return "[]"
    }

    fun getActiveAppPackage(displayId: Int): String {
        return DisplayAppLauncher.getTopPackageOnDisplay(displayId) ?: ""
    }

    fun getActiveAppLabel(context: Context, displayId: Int): String {
        val pkg = getActiveAppPackage(displayId)
        if (pkg.isEmpty()) return ""
        val info = DisplayAppLauncher.resolveAppInfo(context, pkg)
        return info.label
    }

    fun getActiveAppIconBase64(context: Context, displayId: Int): String {
        val pkg = getActiveAppPackage(displayId)
        if (pkg.isEmpty()) return ""
        val info = DisplayAppLauncher.resolveAppInfo(context, pkg)
        return drawableToBase64(info.icon)
    }

    // ---- Mídia tocando agora ----
    // Fonte central: BottomBarState (a MESMA que alimenta os canais app.media.* do tema). Leitura de
    // snapshot state, thread-safe. Metadados leves aqui; a capa (pesada) sai à parte em getAlbumArtBase64.
    fun getNowPlayingJson(): String {
        return try {
            val title = BottomBarState.mediaTitle?.takeIf { it.isNotBlank() } ?: return "{}"
            val o = JSONObject()
            o.put("isPlaying", BottomBarState.mediaIsPlaying)
            o.put("isMuted", BottomBarState.mediaIsMuted)
            o.put("title", title)
            BottomBarState.mediaArtist?.takeIf { it.isNotBlank() }?.let { o.put("artist", it) }
            BottomBarState.mediaAlbum?.takeIf { it.isNotBlank() }?.let { o.put("album", it) }
            BottomBarState.mediaPackageName?.takeIf { it.isNotBlank() }?.let { o.put("app", it) }
            if (BottomBarState.mediaDurationMs > 0L) o.put("durationMs", BottomBarState.mediaDurationMs)
            o.put("positionMs", BottomBarState.mediaElapsedMs)
            if (BottomBarState.mediaProgressUpdatedAtMs > 0L)
                o.put("positionUpdatedAtMs", BottomBarState.mediaProgressUpdatedAtMs)
            o.put("canSeek", BottomBarState.mediaCanSeek)
            o.put("hasAlbumArt", BottomBarState.mediaArtwork != null)
            o.toString()
        } catch (e: Exception) {
            Log.e(TAG, "getNowPlayingJson falhou", e)
            "{}"
        }
    }

    // Capa como data URI JPEG (menor que PNG; foto não precisa de alpha). "" quando não há arte.
    fun getAlbumArtBase64(): String {
        val bmp = BottomBarState.mediaArtwork ?: return ""
        return try {
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "getAlbumArtBase64 falhou", e)
            ""
        }
    }

    fun getVirtualValue(context: Context, key: String): String {
        return when (key) {
            "app.display.1.active_app" -> getActiveAppPackage(1)
            "app.display.1.active_app_label" -> getActiveAppLabel(context, 1)
            "app.display.1.active_app_icon" -> getActiveAppIconBase64(context, 1)
            "app.display.3.active_app" -> getActiveAppPackage(3)
            "app.display.3.active_app_label" -> getActiveAppLabel(context, 3)
            "app.display.3.active_app_icon" -> getActiveAppIconBase64(context, 3)
            "app.launcher.apps" -> getLauncherAppsJson(context)
            "app.navigation.directions" -> AndroidAutoNavManager.getDirectionsJson()
            "app.media.now_playing" -> getNowPlayingJson()
            "app.media.album_art" -> getAlbumArtBase64()
            else -> ""
        }
    }
}
