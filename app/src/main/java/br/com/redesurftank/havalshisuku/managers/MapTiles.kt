package br.com.redesurftank.havalshisuku.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import android.util.LruCache
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

/**
 * OpenStreetMap raster tiles and reverse geocoding for the dashcam map, with a disk cache on the
 * head unit so the map also works on the camera Wi-Fi (no internet there). Follows the OSM tile and
 * Nominatim usage policies: identifying User-Agent, at most 2 parallel tile downloads, 1 geocoding
 * request per second, cached results, small prefetch only on an unmetered network.
 * Nothing about the user goes in the requests; only tile numbers and the tapped coordinate.
 */
object MapTiles {
    private const val TAG = "MapTiles"
    private const val TILE_URL = "https://tile.openstreetmap.org/%d/%d/%d.png"
    private const val NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&addressdetails=1&accept-language=pt-BR&lat=%.6f&lon=%.6f"
    private const val USER_AGENT = "ImpulseHeadUnit/1.0 (dashcam map)"
    const val TILE = 256
    const val MIN_ZOOM = 3
    const val MAX_ZOOM = 18
    private const val DIR = "map-tiles"
    private const val MAX_CACHE_BYTES = 200L * 1024 * 1024
    private const val TILE_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000
    private const val PREFETCH_MAX_TILES = 400

    private val downloads = Semaphore(2)
    private val memory = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val failedAt = ConcurrentHashMap<String, Long>()
    private val prefetchExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "MapTilesPrefetch") }
    @Volatile private var lastGeocodeAt = 0L
    private val addresses = ConcurrentHashMap<String, String>()

    // ---- tile math (Web Mercator, 256 px tiles) ----

    @JvmStatic fun worldX(lon: Double, zoom: Double): Double = (lon + 180.0) / 360.0 * TILE * Math.pow(2.0, zoom)

    @JvmStatic fun worldY(lat: Double, zoom: Double): Double {
        val r = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        return (1.0 - ln(tan(r) + 1.0 / cos(r)) / PI) / 2.0 * TILE * Math.pow(2.0, zoom)
    }

    @JvmStatic fun lonOf(worldX: Double, zoom: Double): Double = worldX / (TILE * Math.pow(2.0, zoom)) * 360.0 - 180.0

    @JvmStatic fun latOf(worldY: Double, zoom: Double): Double {
        val n = PI - 2.0 * PI * worldY / (TILE * Math.pow(2.0, zoom))
        return Math.toDegrees(atan(sinh(n)))
    }

    fun key(z: Int, x: Int, y: Int) = "$z/$x/$y"

    /** In-memory tile, if any (no I/O): for drawing. */
    fun peek(z: Int, x: Int, y: Int): Bitmap? = memory.get(key(z, x, y))

    /**
     * Tile from memory, disk or network (in that order). Blocking: call off the main thread.
     * [allowNetwork] false keeps it to the cache (e.g. while rendering a stamp).
     */
    fun load(context: Context, z: Int, x: Int, y: Int, allowNetwork: Boolean = true): Bitmap? {
        val n = 1 shl z
        if (z < 0 || x < 0 || y < 0 || x >= n || y >= n) return null
        val k = key(z, x, y)
        memory.get(k)?.let { return it }
        val file = file(context, z, x, y)
        val fresh = file.isFile && System.currentTimeMillis() - file.lastModified() < TILE_MAX_AGE_MS
        if (file.isFile && (fresh || !allowNetwork)) {
            decode(file)?.let { memory.put(k, it); return it }
        }
        if (!allowNetwork) return null
        val failed = failedAt[k]
        if (failed != null && System.currentTimeMillis() - failed < 60_000L) return file.takeIf { it.isFile }?.let { decode(it) }
        val network = internetNetwork(context, unmeteredOnly = false)
        if (network == null) return file.takeIf { it.isFile }?.let { decode(it) }?.also { memory.put(k, it) }
        return if (download(network, String.format(Locale.ROOT, TILE_URL, z, x, y), file)) {
            failedAt.remove(k)
            decode(file)?.also { memory.put(k, it) }
        } else {
            failedAt[k] = System.currentTimeMillis()
            file.takeIf { it.isFile }?.let { decode(it) }?.also { memory.put(k, it) }
        }
    }

    /**
     * Caches the tiles along a trail (zooms 15 and 16, the trail's tile plus its neighbours) while an
     * unmetered network is available. Bounded per run; already cached tiles cost nothing.
     */
    @JvmStatic fun prefetchAsync(context: Context, fixes: List<DashcamGpsTrack.Fix>) {
        if (fixes.isEmpty()) return
        val app = context.applicationContext
        prefetchExecutor.execute {
            try {
                val network = internetNetwork(app, unmeteredOnly = true) ?: return@execute
                val wanted = LinkedHashSet<String>()
                for (z in intArrayOf(15, 16)) {
                    for (fix in fixes) {
                        val tx = floor(worldX(fix.lon, z.toDouble()) / TILE).toInt()
                        val ty = floor(worldY(fix.lat, z.toDouble()) / TILE).toInt()
                        for (dx in -1..1) for (dy in -1..1) wanted.add(key(z, tx + dx, ty + dy))
                    }
                }
                var fetched = 0
                for (k in wanted) {
                    if (fetched >= PREFETCH_MAX_TILES) break
                    val (z, x, y) = k.split("/").map { it.toInt() }
                    val file = file(app, z, x, y)
                    if (file.isFile && System.currentTimeMillis() - file.lastModified() < TILE_MAX_AGE_MS) continue
                    if (download(network, String.format(Locale.ROOT, TILE_URL, z, x, y), file)) fetched++
                }
                if (fetched > 0) {
                    Log.i(TAG, "prefetched $fetched tiles")
                    trim(app)
                }
            } catch (e: RuntimeException) {
                Log.w(TAG, "prefetch failed", e)
            }
        }
    }

    /**
     * Street address of a point ("Rua X, 123 · Bairro, Cidade"), or null. Cached by ~10 m cell.
     * Blocking; at most one Nominatim request per second.
     */
    fun address(context: Context, lat: Double, lon: Double): String? {
        val cell = String.format(Locale.ROOT, "%.4f,%.4f", lat, lon)
        addresses[cell]?.let { return it }
        val network = internetNetwork(context, unmeteredOnly = false) ?: return null
        synchronized(this) {
            val wait = 1_100L - (System.currentTimeMillis() - lastGeocodeAt)
            if (wait > 0) Thread.sleep(wait)
            lastGeocodeAt = System.currentTimeMillis()
        }
        return try {
            val conn = network.openConnection(URL(String.format(Locale.ROOT, NOMINATIM_URL, lat, lon))) as HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            try {
                if (conn.responseCode != 200) return null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                formatAddress(JSONObject(body))?.also { addresses[cell] = it }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "geocode failed: ${e.javaClass.simpleName}")
            null
        }
    }

    /** "Rua X, 123 · Bairro, Cidade" from a Nominatim reply; null when it has no usable part. */
    @JvmStatic fun formatAddress(reply: JSONObject): String? {
        val a = reply.optJSONObject("address") ?: return reply.optString("display_name").takeIf { it.isNotBlank() }
        val street = listOf("road", "pedestrian", "footway", "highway").map { a.optString(it) }.firstOrNull { it.isNotBlank() }
        val number = a.optString("house_number").takeIf { it.isNotBlank() }
        val district = listOf("suburb", "neighbourhood", "quarter").map { a.optString(it) }.firstOrNull { it.isNotBlank() }
        val city = listOf("city", "town", "village", "municipality").map { a.optString(it) }.firstOrNull { it.isNotBlank() }
        val first = listOfNotNull(street, number).joinToString(", ").takeIf { it.isNotBlank() }
        val second = listOfNotNull(district, city).joinToString(", ").takeIf { it.isNotBlank() }
        return listOfNotNull(first, second).joinToString(" · ").takeIf { it.isNotBlank() }
    }

    // ---- internals ----

    private fun file(context: Context, z: Int, x: Int, y: Int) = File(File(context.filesDir, DIR), "$z/$x/$y.png")

    private fun decode(file: File): Bitmap? = try {
        BitmapFactory.decodeFile(file.absolutePath)
    } catch (e: OutOfMemoryError) {
        null
    }

    private fun download(network: Network, url: String, target: File): Boolean {
        downloads.acquireUninterruptibly()
        try {
            val conn = network.openConnection(URL(url)) as HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            try {
                if (conn.responseCode != 200) return false
                target.parentFile?.mkdirs()
                val part = File(target.path + ".part")
                conn.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
                if (part.length() == 0L || !part.renameTo(target)) {
                    part.delete()
                    return false
                }
                return true
            } finally {
                conn.disconnect()
            }
        } catch (e: IOException) {
            return false
        } finally {
            downloads.release()
        }
    }

    /** A validated internet network that is not the camera Wi-Fi; null when offline. */
    private fun internetNetwork(context: Context, unmeteredOnly: Boolean): Network? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        var metered: Network? = null
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            ) continue
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            ) return network
            if (metered == null) metered = network
        }
        return if (unmeteredOnly) null else metered
    }

    private fun trim(context: Context) {
        val root = File(context.filesDir, DIR)
        val files = root.walkTopDown().filter { it.isFile }.toMutableList()
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        files.sortBy { it.lastModified() }
        for (f in files) {
            if (total <= MAX_CACHE_BYTES * 9 / 10) break
            total -= f.length()
            f.delete()
        }
    }
}
