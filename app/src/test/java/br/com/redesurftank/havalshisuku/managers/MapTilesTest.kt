package br.com.redesurftank.havalshisuku.managers

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapTilesTest {
    @Test
    fun mercatorRoundTripsAndMatchesKnownTile() {
        // Goiânia area, zoom 16: the trail's tile numbers must match the OSM scheme.
        val lat = -16.883380
        val lon = -49.819595
        val z = 16.0
        val tx = (MapTiles.worldX(lon, z) / MapTiles.TILE).toInt()
        val ty = (MapTiles.worldY(lat, z) / MapTiles.TILE).toInt()
        assertEquals(23698, tx)
        assertEquals(35886, ty)
        assertEquals(lon, MapTiles.lonOf(MapTiles.worldX(lon, z), z), 1e-9)
        assertEquals(lat, MapTiles.latOf(MapTiles.worldY(lat, z), z), 1e-9)
        assertEquals(128.0, MapTiles.worldX(0.0, 0.0), 1e-9)
        assertEquals(128.0, MapTiles.worldY(0.0, 0.0), 1e-9)
    }

    @Test
    fun addressIsStreetNumberThenDistrictCity() {
        val reply = JSONObject(
            """{"display_name":"x","address":{"road":"Avenida Goiás","house_number":"100",
               "suburb":"Setor Central","city":"Goiânia","state":"Goiás"}}"""
        )
        assertEquals("Avenida Goiás, 100 · Setor Central, Goiânia", MapTiles.formatAddress(reply))
        assertEquals("Rodovia X · Anápolis",
            MapTiles.formatAddress(JSONObject("""{"address":{"road":"Rodovia X","town":"Anápolis"}}""")))
        assertEquals("Somewhere", MapTiles.formatAddress(JSONObject("""{"display_name":"Somewhere"}""")))
        assertNull(MapTiles.formatAddress(JSONObject("""{"address":{}}""")))
    }
}
