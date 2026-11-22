package br.com.redesurftank.havalshisuku.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import br.com.redesurftank.havalshisuku.managers.DashcamCatalog
import br.com.redesurftank.havalshisuku.managers.DashcamGpsTrack
import br.com.redesurftank.havalshisuku.managers.MapTiles
import br.com.redesurftank.havalshisuku.ui.components.ImpTokens
import br.com.redesurftank.havalshisuku.ui.components.impButtonColors
import br.com.redesurftank.havalshisuku.ui.theme.ImpulseTextSizes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

internal enum class TrailStatus { SAVED, ON_CARD, GONE }

private val OnCardColor = Color(0xFF4A9EFF)
private val SavedColor = Color(0xFF35B98F)
private val GoneColor = Color(0xFF6B7280)
private val MapShape = RoundedCornerShape(12.dp)
private val DayLabel = DateTimeFormatter.ofPattern("dd/MM")
private val TimeLabel = DateTimeFormatter.ofPattern("HH:mm:ss")

/** A clip window on the GPS (UTC) time axis. */
internal data class ClipWindow(val startUtc: Long, val endUtc: Long, val recording: DashcamCatalog.Recording?, val file: File?)

/** Camera clips and saved clips as UTC windows; the camera writes local time in its names. */
internal fun clipWindows(catalog: DashcamCatalog, saved: List<File>, zone: ZoneId): Pair<List<ClipWindow>, List<ClipWindow>> {
    val camera = catalog.recordings.mapNotNull { r ->
        val start = r.start ?: return@mapNotNull null
        val startUtc = start.atZone(zone).toInstant().toEpochMilli()
        val endUtc = r.end?.atZone(zone)?.toInstant()?.toEpochMilli()?.takeIf { it > startUtc }
            ?: (startUtc + DashcamGpsTrack.CLIP_MS)
        ClipWindow(startUtc, endUtc, r, null)
    }.sortedBy { it.startUtc }
    val local = saved.mapNotNull { f ->
        val start = DashcamGpsTrack.clipStartUtcMs(f.name, zone) ?: return@mapNotNull null
        ClipWindow(start, start + DashcamGpsTrack.CLIP_MS, null, f)
    }.sortedBy { it.startUtc }
    return camera to local
}

internal fun windowAt(windows: List<ClipWindow>, utc: Long): ClipWindow? =
    windows.lastOrNull { it.startUtc <= utc && utc < it.endUtc }

/**
 * Map of the car's GPS trail with the dashcam clips on it: blue where the clip is still on the
 * camera card, green where it is saved on the head unit, grey where the camera already recorded
 * over it. Tap the trail (or drag the time bar) to pick a moment, then watch or save that minute.
 */
@Composable
internal fun DashcamMapCard(
    catalog: DashcamCatalog,
    savedFiles: List<File>,
    saveStatus: Map<String, String>,
    onPlayCamera: (DashcamCatalog.Recording, Long) -> Unit,
    onPlaySaved: (File, Long) -> Unit,
    onSave: (List<DashcamCatalog.Recording>) -> Unit
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    var trail by remember { mutableStateOf<List<DashcamGpsTrack.Fix>?>(null) }
    LaunchedEffect(Unit) {
        trail = withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            DashcamGpsTrack.getInstance(context).query(now - 72L * 60 * 60 * 1000, now + 60_000L)
        }
    }
    val all = trail
    if (all == null) {
        MapNote("Carregando o trajeto…")
        return
    }
    if (all.isEmpty()) {
        MapNote(
            "Ainda não há trajeto GPS guardado. Ele começa a ser registrado com \"Localizar os vídeos no mapa\" " +
                "ligado (Automação) e fica 72 h na central."
        )
        return
    }
    val days = remember(all) {
        all.map { Instant.ofEpochMilli(it.utcMs).atZone(zone).toLocalDate() }.distinct().sortedDescending()
    }
    var day by remember(days) { mutableStateOf(days.first()) }
    val dayFixes = remember(all, day) { all.filter { Instant.ofEpochMilli(it.utcMs).atZone(zone).toLocalDate() == day } }
    val drawFixes = remember(dayFixes) { DashcamGpsTrack.decimate(dayFixes, 8.0, 15_000L) }
    val (cameraWindows, savedWindows) = remember(catalog, savedFiles) { clipWindows(catalog, savedFiles, zone) }
    fun statusAt(utc: Long): TrailStatus = when {
        windowAt(savedWindows, utc) != null -> TrailStatus.SAVED
        windowAt(cameraWindows, utc) != null -> TrailStatus.ON_CARD
        else -> TrailStatus.GONE
    }
    var selectedUtc by remember(day) { mutableStateOf(dayFixes.last().utcMs) }
    val selected = remember(dayFixes, selectedUtc) {
        DashcamGpsTrack.interpolate(dayFixes, selectedUtc) ?: dayFixes.minByOrNull { kotlin.math.abs(it.utcMs - selectedUtc) }
    }
    // Map viewport: centre in zoom-0 world pixels, fractional zoom.
    val bounds = remember(drawFixes) { boundsOf(drawFixes) }
    var view by remember(day) { mutableStateOf<MapView?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            days.take(4).forEach { d ->
                val label = when (d) {
                    LocalDate.now(zone) -> "Hoje"
                    LocalDate.now(zone).minusDays(1) -> "Ontem"
                    else -> d.format(DayLabel)
                }
                if (d == day) Button(onClick = {}, colors = impButtonColors()) { Text(label) }
                else OutlinedButton(onClick = { day = d }) { Text(label, color = ImpTokens.TextPrimary) }
            }
            Spacer(Modifier.weight(1f))
            Legend(OnCardColor, "No cartão da câmera")
            Legend(SavedColor, "Salvo no carro")
            Legend(GoneColor, "Já gravado por cima")
        }
        Row(Modifier.fillMaxWidth().height(470.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrailMap(
                fixes = drawFixes,
                statusAt = ::statusAt,
                selected = selected,
                fit = bounds,
                view = view,
                onView = { view = it },
                onPick = { selectedUtc = it.utcMs },
                modifier = Modifier.weight(1f).fillMaxSize()
            )
            PointPanel(
                fix = selected,
                status = selected?.let { statusAt(it.utcMs) },
                camera = selected?.let { windowAt(cameraWindows, it.utcMs) },
                saved = selected?.let { windowAt(savedWindows, it.utcMs) },
                cameraWindows = cameraWindows,
                catalogEmpty = catalog.recordings.isEmpty(),
                saveStatus = saveStatus,
                onPlayCamera = onPlayCamera,
                onPlaySaved = onPlaySaved,
                onSave = onSave,
                modifier = Modifier.width(340.dp).fillMaxSize()
            )
        }
        // Time bar over the selected day: the point moves on the map as you drag.
        val first = dayFixes.first().utcMs
        val last = dayFixes.last().utcMs
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Instant.ofEpochMilli(first).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")),
                color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
            Slider(
                value = if (last > first) ((selectedUtc - first).toFloat() / (last - first)).coerceIn(0f, 1f) else 1f,
                onValueChange = { f ->
                    val target = first + ((last - first) * f).toLong()
                    // Snap to a real fix so the panel never shows a point between two drives.
                    selectedUtc = dayFixes.minByOrNull { kotlin.math.abs(it.utcMs - target) }?.utcMs ?: target
                    selected?.let { view = view?.centeredOn(it) }
                },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(thumbColor = ImpTokens.Accent, activeTrackColor = ImpTokens.Accent)
            )
            Text(Instant.ofEpochMilli(last).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")),
                color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
        }
    }
}

@Composable
private fun MapNote(text: String) {
    Text(
        text,
        color = ImpTokens.TextSecondary,
        fontSize = ImpulseTextSizes.Body,
        modifier = Modifier.fillMaxWidth().clip(MapShape).background(ImpTokens.Container).padding(20.dp)
    )
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
        Spacer(Modifier.width(10.dp))
    }
}

/** Viewport: centre in zoom-0 world pixels (0..256) and a fractional zoom. */
internal data class MapView(val cx: Double, val cy: Double, val zoom: Double) {
    fun centeredOn(fix: DashcamGpsTrack.Fix) = copy(cx = MapTiles.worldX(fix.lon, 0.0), cy = MapTiles.worldY(fix.lat, 0.0))
}

internal data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

private fun boundsOf(fixes: List<DashcamGpsTrack.Fix>): Bounds? {
    if (fixes.isEmpty()) return null
    val xs = fixes.map { MapTiles.worldX(it.lon, 0.0) }
    val ys = fixes.map { MapTiles.worldY(it.lat, 0.0) }
    return Bounds(xs.min(), ys.min(), xs.max(), ys.max())
}

private fun fitView(b: Bounds, w: Float, h: Float): MapView {
    val spanX = max(b.maxX - b.minX, 1e-6)
    val spanY = max(b.maxY - b.minY, 1e-6)
    val zoom = min(ln((w * 0.8) / spanX) / ln(2.0), ln((h * 0.8) / spanY) / ln(2.0))
        .coerceIn(MapTiles.MIN_ZOOM.toDouble(), 17.0)
    return MapView((b.minX + b.maxX) / 2, (b.minY + b.maxY) / 2, zoom)
}

@Composable
private fun TrailMap(
    fixes: List<DashcamGpsTrack.Fix>,
    statusAt: (Long) -> TrailStatus,
    selected: DashcamGpsTrack.Fix?,
    fit: Bounds?,
    view: MapView?,
    onView: (MapView) -> Unit,
    onPick: (DashcamGpsTrack.Fix) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val inFlight = remember { HashSet<String>() }
    val statuses = remember(fixes) { fixes.map { statusAt(it.utcMs) } }
    val density = LocalDensity.current

    BoxWithConstraints(modifier.clip(MapShape).background(Color(0xFF1B1F24)).border(1.dp, Color(0xFF262A33), MapShape)) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val v = view ?: fit?.let { fitView(it, w, h) } ?: MapView(128.0, 128.0, 3.0)
        val current = androidx.compose.runtime.rememberUpdatedState(v)
        val z = floor(v.zoom).toInt().coerceIn(MapTiles.MIN_ZOOM, MapTiles.MAX_ZOOM)
        val scale = 2.0.pow(v.zoom - z)
        val n = 1 shl z
        val cxz = v.cx * (1 shl z)
        val cyz = v.cy * (1 shl z)
        val half = MapTiles.TILE * scale
        val tx0 = floor((cxz - w / 2 / scale) / MapTiles.TILE).toInt()
        val tx1 = floor((cxz + w / 2 / scale) / MapTiles.TILE).toInt()
        val ty0 = floor((cyz - h / 2 / scale) / MapTiles.TILE).toInt().coerceAtLeast(0)
        val ty1 = floor((cyz + h / 2 / scale) / MapTiles.TILE).toInt().coerceAtMost(n - 1)
        val visible = buildList { for (tx in tx0..tx1) for (ty in ty0..ty1) if (tx in 0 until n) add(MapTiles.key(z, tx, ty)) }
        LaunchedEffect(visible) {
            if (tiles.size > 160) tiles.keys.filter { it !in visible }.forEach { tiles.remove(it) }
            for (k in visible) {
                if (tiles.containsKey(k) || !inFlight.add(k)) continue
                val (tz, tx, ty) = k.split("/").map { it.toInt() }
                val cached = MapTiles.peek(tz, tx, ty)
                if (cached != null) {
                    tiles[k] = cached.asImageBitmap()
                    inFlight.remove(k)
                    continue
                }
                scope.launch {
                    val bmp = withContext(Dispatchers.IO) { MapTiles.load(context, tz, tx, ty) }
                    if (bmp != null) tiles[k] = bmp.asImageBitmap()
                    inFlight.remove(k)
                }
            }
        }
        fun screenOf(f: DashcamGpsTrack.Fix): Offset {
            val k = 2.0.pow(v.zoom)
            return Offset(
                ((MapTiles.worldX(f.lon, 0.0) - v.cx) * k + w / 2).toFloat(),
                ((MapTiles.worldY(f.lat, 0.0) - v.cy) * k + h / 2).toFloat()
            )
        }
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        val cur = current.value
                        val k = 2.0.pow(cur.zoom)
                        var cx = cur.cx - pan.x / k
                        var cy = cur.cy - pan.y / k
                        val newZoom = (cur.zoom + ln(zoomChange.toDouble()) / ln(2.0))
                            .coerceIn(MapTiles.MIN_ZOOM.toDouble(), MapTiles.MAX_ZOOM.toDouble())
                        if (newZoom != cur.zoom) {
                            val k2 = 2.0.pow(newZoom)
                            val px = cx + (centroid.x - size.width / 2) / k
                            val py = cy + (centroid.y - size.height / 2) / k
                            cx = px - (centroid.x - size.width / 2) / k2
                            cy = py - (centroid.y - size.height / 2) / k2
                        }
                        onView(MapView(cx, cy, newZoom))
                    }
                }
                .pointerInput(fixes) {
                    detectTapGestures { tap ->
                        val cur = current.value
                        val k = 2.0.pow(cur.zoom)
                        val best = fixes.minByOrNull { f ->
                            val dx = (MapTiles.worldX(f.lon, 0.0) - cur.cx) * k + size.width / 2 - tap.x
                            val dy = (MapTiles.worldY(f.lat, 0.0) - cur.cy) * k + size.height / 2 - tap.y
                            dx * dx + dy * dy
                        }
                        if (best != null) {
                            val dx = (MapTiles.worldX(best.lon, 0.0) - cur.cx) * k + size.width / 2 - tap.x
                            val dy = (MapTiles.worldY(best.lat, 0.0) - cur.cy) * k + size.height / 2 - tap.y
                            if (dx * dx + dy * dy < 48.0 * 48.0) onPick(best)
                        }
                    }
                }
        ) {
            for (tx in tx0..tx1) for (ty in ty0..ty1) {
                if (tx !in 0 until n) continue
                val img = tiles[MapTiles.key(z, tx, ty)] ?: continue
                val x = ((tx * MapTiles.TILE - cxz) * scale + w / 2)
                val y = ((ty * MapTiles.TILE - cyz) * scale + h / 2)
                drawImage(
                    img,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(img.width, img.height),
                    dstOffset = IntOffset(floor(x).toInt(), floor(y).toInt()),
                    dstSize = IntSize(ceil(half).toInt() + 1, ceil(half).toInt() + 1)
                )
            }
            // Trail in runs of one status, so each colour is one path. A gap of more than 2 min is a
            // stop (car off): the line is not joined across it.
            var path: Path? = null
            var runStatus: TrailStatus? = null
            fun flush() {
                val pth = path ?: return
                val color = when (runStatus) {
                    TrailStatus.SAVED -> SavedColor
                    TrailStatus.ON_CARD -> OnCardColor
                    else -> GoneColor
                }
                drawPath(pth, Color.Black.copy(alpha = 0.35f), style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(pth, color, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                path = null
            }
            for (k in 1 until fixes.size) {
                if (fixes[k].utcMs - fixes[k - 1].utcMs > 120_000L) {
                    flush()
                    continue
                }
                val st = statuses[k - 1]
                if (path == null || st != runStatus) {
                    flush()
                    val p0 = screenOf(fixes[k - 1])
                    path = Path().apply { moveTo(p0.x, p0.y) }
                    runStatus = st
                }
                val p = screenOf(fixes[k])
                path?.lineTo(p.x, p.y)
            }
            flush()
            selected?.let {
                val o = screenOf(it)
                drawCircle(Color.Black.copy(alpha = 0.35f), radius = 17f, center = o)
                drawCircle(Color.White, radius = 13f, center = o)
                drawCircle(ImpTokens.Accent, radius = 8f, center = o)
            }
        }
        Text(
            "© OpenStreetMap",
            color = Color(0xFF333333),
            fontSize = ImpulseTextSizes.BodyCompact,
            modifier = Modifier.align(Alignment.BottomEnd).background(Color.White.copy(alpha = 0.75f)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Column(Modifier.align(Alignment.TopEnd).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MapButton("+") { onView(v.copy(zoom = min(v.zoom + 1, MapTiles.MAX_ZOOM.toDouble()))) }
            MapButton("−") { onView(v.copy(zoom = max(v.zoom - 1, MapTiles.MIN_ZOOM.toDouble()))) }
            MapButton("◎") { fit?.let { onView(fitView(it, w, h)) } }
        }
    }
}

@Composable
private fun MapButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color(0xE6111418)).border(1.dp, Color(0xFF3A404A), CircleShape)
            .pointerInput(onClick) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PointPanel(
    fix: DashcamGpsTrack.Fix?,
    status: TrailStatus?,
    camera: ClipWindow?,
    saved: ClipWindow?,
    cameraWindows: List<ClipWindow>,
    catalogEmpty: Boolean,
    saveStatus: Map<String, String>,
    onPlayCamera: (DashcamCatalog.Recording, Long) -> Unit,
    onPlaySaved: (File, Long) -> Unit,
    onSave: (List<DashcamCatalog.Recording>) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    var address by remember(fix?.let { "%.4f,%.4f".format(java.util.Locale.ROOT, it.lat, it.lon) }) { mutableStateOf<String?>(null) }
    LaunchedEffect(fix?.let { "%.4f,%.4f".format(java.util.Locale.ROOT, it.lat, it.lon) }) {
        val f = fix ?: return@LaunchedEffect
        address = withContext(Dispatchers.IO) { MapTiles.address(context, f.lat, f.lon) } ?: ""
    }
    Column(
        modifier.clip(MapShape).background(ImpTokens.Container).border(1.dp, Color(0xFF262A33), MapShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (fix == null) {
            Text("Toque no trajeto para escolher um momento.", color = ImpTokens.TextSecondary, fontSize = ImpulseTextSizes.Body)
            return@Column
        }
        val local = Instant.ofEpochMilli(fix.utcMs).atZone(zone)
        Text(local.format(DayLabel) + " · " + local.format(TimeLabel), color = ImpTokens.TextPrimary,
            fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
        val kmh = fix.speedKmh
        Text(if (kmh >= 0) "${kmh.roundToInt()} km/h" else "Velocidade indisponível", color = ImpTokens.TextSecondary,
            fontSize = ImpulseTextSizes.Body)
        Text(
            when (address) {
                null -> "Buscando o endereço…"
                "" -> "Endereço indisponível sem internet"
                else -> address!!
            },
            color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Body
        )
        Text("%.5f, %.5f".format(java.util.Locale.ROOT, fix.lat, fix.lon), color = ImpTokens.TextMuted,
            fontSize = ImpulseTextSizes.BodyCompact)
        Spacer(Modifier.height(4.dp))
        val (dot, line) = when (status) {
            TrailStatus.SAVED -> SavedColor to "Vídeo salvo no carro"
            TrailStatus.ON_CARD -> OnCardColor to "Vídeo no cartão da câmera"
            else -> GoneColor to if (catalogEmpty) "Lista da câmera não carregada" else "A câmera já gravou por cima"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
            Text(line, color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Body)
        }
        if (catalogEmpty) {
            Text("Conecte a central ao Wi-Fi da câmera para ver quais trechos ainda têm vídeo.",
                color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
        }
        Spacer(Modifier.weight(1f))
        saved?.file?.let { f ->
            Button(onClick = { onPlaySaved(f, fix.utcMs - saved.startUtc) }, colors = impButtonColors(),
                modifier = Modifier.fillMaxWidth()) { Text("Assistir (salvo no carro)") }
        }
        camera?.recording?.let { r ->
            val state = saveStatus[r.name]
            if (saved == null) {
                Button(onClick = { onPlayCamera(r, fix.utcMs - camera.startUtc) }, colors = impButtonColors(),
                    modifier = Modifier.fillMaxWidth()) { Text("Assistir este momento") }
            }
            val idx = cameraWindows.indexOf(camera)
            val prev = cameraWindows.getOrNull(idx - 1)?.takeIf { camera.startUtc - it.endUtc < 120_000L }
            val next = cameraWindows.getOrNull(idx + 1)?.takeIf { it.startUtc - camera.endUtc < 120_000L }
            OutlinedButton(onClick = { onSave(listOf(r)) }, modifier = Modifier.fillMaxWidth(),
                enabled = state?.startsWith("salvando") != true) {
                Text("Salvar este minuto", color = ImpTokens.TextPrimary)
            }
            if (prev != null || next != null) {
                OutlinedButton(
                    onClick = { onSave(listOfNotNull(prev?.recording, r, next?.recording)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salvar com o minuto antes e depois", color = ImpTokens.TextPrimary) }
            }
            state?.let { Text(it, color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact) }
        }
    }
}
