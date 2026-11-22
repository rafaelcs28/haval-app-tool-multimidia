package br.com.redesurftank.havalshisuku.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import br.com.redesurftank.havalshisuku.radar.RadarGeo
import br.com.redesurftank.havalshisuku.radar.RadarRepository
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Speed limit and speed cameras for the stamp.
 * - Camera ahead: Impulse's own radar base (offline), within [RADAR_AHEAD_M] and in front of the car.
 * - Road limit: OpenStreetMap `maxspeed` of the road under the car, asked once per clip to Overpass
 *   (needs internet when the stamp is made); without it, a camera's limit nearby is used.
 * - Over the limit: Brazilian tolerance (CTB/Contran): 7 km/h up to 100 km/h, 7 % above.
 */
object DashcamRoadInfo {
    private const val TAG = "DashcamRoad"
    private const val OVERPASS = "https://overpass-api.de/api/interpreter"
    private const val USER_AGENT = "ImpulseHeadUnit/1.0 (dashcam stamp)"
    const val RADAR_AHEAD_M = 800.0
    private const val RADAR_LIMIT_USE_M = 300.0

    data class Radar(val distanceM: Double, val limitKmh: Int?)

    /** Nearest camera in front of the car (heading ±35°, camera direction compatible), or null. */
    @JvmStatic
    fun radarAhead(context: Context, lat: Double, lon: Double, heading: Float?): Radar? {
        val index = RadarRepository.index ?: runCatching { RadarRepository.load(context) }.let { RadarRepository.index } ?: return null
        var best: Radar? = null
        index.forEachNear(lat, lon, RADAR_AHEAD_M) { _, cam, d ->
            if (heading != null && d > 30) {
                val toCam = RadarGeo.bearingDeg(lat, lon, cam.lat, cam.lon)
                if (RadarGeo.angleDiff(toCam, heading) > 35f) return@forEachNear
                val enforced = cam.bearing
                if (enforced != null && RadarGeo.angleDiff(enforced, heading) > 60f) return@forEachNear
            }
            if (best == null || d < best!!.distanceM) best = Radar(d, cam.limitKmh)
        }
        return best
    }

    /** Effective limit: the road's, else a camera's within 300 m. */
    @JvmStatic
    fun effectiveLimit(roadLimit: Int?, radar: Radar?): Int? =
        roadLimit ?: radar?.takeIf { it.distanceM <= RADAR_LIMIT_USE_M }?.limitKmh

    /** Over the limit with the CTB tolerance. */
    @JvmStatic
    fun isOverLimit(speedKmh: Float?, limitKmh: Int?): Boolean {
        if (speedKmh == null || limitKmh == null || limitKmh <= 0) return false
        val tolerance = if (limitKmh <= 100) 7f else limitKmh * 0.07f
        return speedKmh > limitKmh + tolerance
    }

    /** Road `maxspeed` around a point (Overpass), or null offline / unknown. Blocking. */
    @JvmStatic
    fun roadLimit(context: Context, lat: Double, lon: Double): Int? {
        val network = internet(context) ?: return null
        val query = String.format(
            Locale.ROOT,
            "[out:json][timeout:8];way(around:18,%.6f,%.6f)[highway][maxspeed];out tags 5;",
            lat, lon
        )
        return try {
            val conn = network.openConnection(URL(OVERPASS + "?data=" + URLEncoder.encode(query, "UTF-8"))) as HttpURLConnection
            conn.connectTimeout = 6_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            try {
                if (conn.responseCode != 200) return null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseMaxspeed(JSONObject(body))
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "overpass failed: ${e.javaClass.simpleName}")
            null
        }
    }

    /** Lowest numeric `maxspeed` (km/h) among the returned ways; "50 mph" converted; others ignored. */
    @JvmStatic
    fun parseMaxspeed(reply: JSONObject): Int? {
        val elements = reply.optJSONArray("elements") ?: return null
        var best: Int? = null
        for (i in 0 until elements.length()) {
            val raw = elements.optJSONObject(i)?.optJSONObject("tags")?.optString("maxspeed").orEmpty().trim()
            val m = Regex("^(\\d{1,3})\\s*(mph)?$").find(raw) ?: continue
            var v = m.groupValues[1].toInt()
            if (m.groupValues[2] == "mph") v = Math.round(v * 1.609f)
            if (v in 5..200 && (best == null || v < best!!)) best = v
        }
        return best
    }

    private fun internet(context: Context): android.net.Network? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        return cm.allNetworks.firstOrNull { n ->
            val caps = cm.getNetworkCapabilities(n) ?: return@firstOrNull false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }
    }
}
