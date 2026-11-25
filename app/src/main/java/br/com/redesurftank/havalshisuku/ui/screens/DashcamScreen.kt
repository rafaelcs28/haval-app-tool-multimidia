package br.com.redesurftank.havalshisuku.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.Stroke
import br.com.redesurftank.havalshisuku.managers.DashcamAutoStamp
import br.com.redesurftank.havalshisuku.managers.DashcamAutomationManager
import br.com.redesurftank.havalshisuku.managers.DashcamGpsTrack
import br.com.redesurftank.havalshisuku.managers.DashcamStampJob
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.managers.DashcamCatalog
import br.com.redesurftank.havalshisuku.managers.DashcamClipStore
import br.com.redesurftank.havalshisuku.managers.DashcamShareServer
import br.com.redesurftank.havalshisuku.managers.DashcamGalleryRepository
import br.com.redesurftank.havalshisuku.managers.DashcamNetworkManager
import br.com.redesurftank.havalshisuku.managers.DashcamStationPolicy
import br.com.redesurftank.havalshisuku.managers.DashcamProfile
import br.com.redesurftank.havalshisuku.managers.DashcamGalleryRepository.CatalogStatus
import br.com.redesurftank.havalshisuku.managers.DashcamGalleryRepository.StateKind
import br.com.redesurftank.havalshisuku.managers.DashcamVideoDataSource
import br.com.redesurftank.havalshisuku.managers.ServiceManager
import br.com.redesurftank.havalshisuku.models.CarConstants
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys
import br.com.redesurftank.havalshisuku.ui.components.ImpTokens
import br.com.redesurftank.havalshisuku.ui.components.impButtonColors
import br.com.redesurftank.havalshisuku.ui.theme.ImpulseTextSizes
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val PtBr = Locale("pt", "BR")
private val HourMinute = DateTimeFormatter.ofPattern("HH:mm")
private val DayMonth = DateTimeFormatter.ofPattern("d 'de' MMMM", PtBr)
private val MonthYear = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PtBr)
private val CardShape = RoundedCornerShape(16.dp)
private val RowShape = RoundedCornerShape(10.dp)
private val RecordingGreen = Color(0xFF34D399)
private val CardColor = Color(0xFF15171D)
private val CardBorder = Color(0xFF262A33)
private const val STATE_POLL_MS = 15_000L
private const val MAX_PLAYBACK_KMH = 3.0
/** Measured in the car (2026-11-21): 27.3 MB in 33 s over the camera Wi-Fi. */
private const val CAMERA_DOWNLOAD_BYTES_PER_SEC = 820_000.0

/** "~3 min" for a download of [bytes] over the camera Wi-Fi; empty when the size is unknown. */
private fun downloadEstimate(bytes: Long): String {
    if (bytes <= 0) return ""
    val seconds = bytes / CAMERA_DOWNLOAD_BYTES_PER_SEC
    return if (seconds < 60) "~${seconds.toInt().coerceAtLeast(1)} s" else "~${Math.ceil(seconds / 60).toInt()} min"
}

private fun clipsBytes(clips: List<DashcamCatalog.Clip>): Long =
    if (clips.any { it.recording.size <= 0 }) -1L else clips.sumOf { it.recording.size }

private fun withEstimate(label: String, bytes: Long): String {
    val estimate = downloadEstimate(bytes)
    return if (estimate.isEmpty()) label else "$label ($estimate)"
}

/** What the player shows: a clip on the camera or a copy saved on the head unit. */
private data class PlayerSource(
    val uri: Uri,
    val title: String,
    val subtitle: String,
    val recording: DashcamCatalog.Recording?,
    /** Where playback starts (a moment picked on the map); 0 = from the beginning. */
    val startMs: Long = 0L
)

private fun cameraSourceAt(r: DashcamCatalog.Recording, startMs: Long) = PlayerSource(
    DashcamVideoDataSource.uriFor(r),
    "${r.start.format(DayMonth)} · ${r.start.format(HourMinute)}",
    r.name,
    r,
    startMs.coerceAtLeast(0L)
)

private fun cameraSource(clip: DashcamCatalog.Clip): PlayerSource {
    val r = clip.recording
    return PlayerSource(
        DashcamVideoDataSource.uriFor(r),
        "${r.start.format(DayMonth)} · ${r.start.format(HourMinute)}–${clip.estimatedEnd.format(HourMinute)}",
        r.name,
        r
    )
}

private fun localSource(file: File, startMs: Long = 0L) =
    PlayerSource(Uri.fromFile(file), "Salvo no carro", file.name, null, startMs.coerceAtLeast(0L))

@Composable
fun DashcamTab() {
    val context = LocalContext.current
    val repository = remember { DashcamGalleryRepository.getInstance(context) }

    var state by remember { mutableStateOf<DashcamGalleryRepository.StateSnapshot?>(null) }
    var catalog by remember { mutableStateOf(DashcamCatalog.fromPaths(emptyList())) }
    var catalogFetchedAt by remember { mutableStateOf(0L) }
    var catalogPartial by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var askPause by remember { mutableStateOf(false) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var selectedSegment by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf<PlayerSource?>(null) }
    var autoRefreshed by remember { mutableStateOf(false) }
    val store = remember { DashcamClipStore.getInstance(context) }
    var savedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    // Background stamping: refresh the saved list while it works, so progress and the new copy show.
    var autoStampTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3_000L)
            if (DashcamAutoStamp.isActive() || autoStampTick > 0) {
                savedFiles = withContext(Dispatchers.IO) { store.list() }
                autoStampTick = if (DashcamAutoStamp.isActive()) 2 else autoStampTick - 1
            }
        }
    }
    val saveStatus = remember { mutableStateMapOf<String, String>() }
    val stampStatus = remember { mutableStateMapOf<String, String>() }
    var sharing by remember { mutableStateOf<File?>(null) }
    var showSaved by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }

    fun reloadSaved() {
        savedFiles = store.list()
    }

    fun saveClips(recordings: List<DashcamCatalog.Recording>) {
        val pending = recordings.filter { saveStatus[it.name]?.startsWith("salvando") != true }
        if (pending.isEmpty()) return
        pending.forEach { saveStatus[it.name] = "na fila" }
        store.save(pending, object : DashcamClipStore.Listener {
            override fun onProgress(name: String, progress: Float) {
                saveStatus[name] = when {
                    progress == DashcamClipStore.PROGRESS_CONVERTING -> "salvando · convertendo para MP4…"
                    progress >= 0 -> "salvando ${(progress * 100).toInt()}%"
                    else -> "salvando…"
                }
            }

            override fun onDone(name: String, file: File) {
                saveStatus[name] = "salvo no carro"
                message = "${file.name} salvo no carro. Veja em \"Salvos no carro\", no topo da tela."
                reloadSaved()
            }

            override fun onFailed(name: String, error: String) {
                saveStatus[name] = "falhou: " + saveErrorText(error)
            }
        })
    }

    fun applyCatalog(result: DashcamGalleryRepository.CatalogResult) {
        catalog = result.catalog
        catalogFetchedAt = result.catalogFetchedAtMs
        catalogPartial = result.catalogPartial
        val day = selectedDay?.takeIf { result.catalog.hasRecordingsOn(it) } ?: result.catalog.latestDay
        if (day != selectedDay) {
            selectedDay = day
            selectedSegment = 0
            if (day != null) month = YearMonth.from(day)
        }
    }

    fun refreshState() {
        repository.queryState { state = it }
    }

    fun handleCatalog(result: DashcamGalleryRepository.CatalogResult) {
        busy = false
        applyCatalog(result)
        message = catalogMessage(result)
        if (result.status == CatalogStatus.NEEDS_PAUSE) askPause = true
        refreshState()
    }

    fun refreshCatalog(allowPause: Boolean) {
        if (busy) return
        busy = true
        message = if (allowPause) "Pausando, lendo a lista e retomando…" else "Lendo a lista de vídeos…"
        repository.refreshCatalog(allowPause) { handleCatalog(it) }
    }

    // A Novatek entrega a lista gravando, sem pausa: carrega sozinha ao abrir a aba, uma vez.
    // A HiSilicon continua exigindo o toque e a confirmação da pausa.
    LaunchedEffect(state?.profile, state?.kind) {
        val current = state ?: return@LaunchedEffect
        if (!autoRefreshed && current.profile == DashcamProfile.NOVATEK
            && current.kind != StateKind.UNREACHABLE && current.kind != StateKind.WIFI_OFF) {
            autoRefreshed = true
            refreshCatalog(false)
        }
    }

    LaunchedEffect(Unit) {
        savedFiles = withContext(Dispatchers.IO) { store.list() }
        repository.loadCached { applyCatalog(it) }
        while (true) {
            refreshState()
            delay(STATE_POLL_MS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Dashcam",
                color = ImpTokens.TextPrimary,
                fontSize = ImpulseTextSizes.Title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (catalogFetchedAt > 0) {
                Text(
                    "Lista de ${formatFetched(catalogFetchedAt)}" + if (catalogPartial) " · parcial" else "",
                    color = ImpTokens.TextMuted,
                    fontSize = ImpulseTextSizes.BodyCompact
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ViewTab("Câmera", selected = !showSaved && !showMap) {
                showSaved = false
                showMap = false
            }
            ViewTab("Salvos no carro (${savedFiles.size})", selected = showSaved) {
                reloadSaved()
                showSaved = true
                showMap = false
            }
            ViewTab("Mapa", selected = showMap) {
                reloadSaved()
                showSaved = false
                showMap = true
            }
        }

        if (showMap) {
            message?.let { Notice(it) }
            DashcamMapCard(
                catalog = catalog,
                savedFiles = savedFiles,
                saveStatus = saveStatus,
                onPlayCamera = { r, at -> playing = cameraSourceAt(r, at) },
                onPlaySaved = { f, at -> playing = localSource(f, at) },
                onSave = { saveClips(it) }
            )
            Spacer(Modifier.height(8.dp))
            return@Column
        }

        if (showSaved) {
            message?.let { Notice(it) }
            SavedCard(
                files = savedFiles,
                freeBytes = store.directory().usableSpace,
                onPlay = { playing = localSource(it) },
                onShare = { sharing = it },
                onDelete = {
                    store.delete(it)
                    saveStatus.remove(it.name)
                    reloadSaved()
                },
                stampStatus = stampStatus,
                onStamp = { file ->
                    stampStatus[file.name] = "carimbando…"
                    DashcamStampJob.start(context, file, object : DashcamStampJob.Listener {
                        override fun onProgress(name: String, fraction: Float) {
                            stampStatus[name] = "carimbando ${(fraction * 100).toInt()}%"
                        }

                        override fun onDone(name: String, output: File) {
                            stampStatus[name] = "carimbo pronto: ${output.name}"
                            message = "${output.name} criado com o carimbo. O original continua salvo."
                            reloadSaved()
                        }

                        override fun onFailed(name: String, error: String) {
                            stampStatus[name] = "carimbo falhou: " + when (error) {
                                "sem_dados" -> "sem GPS e dados do carro para este horário"
                                "no_space" -> "sem espaço na central (libere em Recursos → Limpar armazenamento)"
                                "cancelled" -> "cancelado"
                                else -> error
                            }
                        }
                    })
                }
            )
            Spacer(Modifier.height(8.dp))
            return@Column
        }

        StateCard(
            state = state,
            onConnect = { openWifiSettings(context) },
            onQuery = { refreshState() },
            onRecover = {
                busy = true
                repository.resumeUnconfirmedPause {
                    busy = false
                    state = it
                    message = if (it.resumeUnconfirmed) "Retomada ainda não confirmada." else "Gravação retomada."
                }
            },
            busy = busy
        )

        message?.let { Notice(it) }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CalendarCard(
                modifier = Modifier.weight(1f),
                catalog = catalog,
                month = month,
                selectedDay = selectedDay,
                busy = busy,
                refreshEnabled = state?.resumeUnconfirmed != true,
                onMonth = { month = it },
                onToday = {
                    month = YearMonth.now()
                    selectedDay = LocalDate.now()
                    selectedSegment = 0
                },
                onDay = {
                    selectedDay = it
                    selectedSegment = 0
                },
                onRefresh = { refreshCatalog(false) }
            )
            DayCard(
                modifier = Modifier.weight(1f),
                catalog = catalog,
                day = selectedDay,
                selectedSegment = selectedSegment,
                onSegment = { selectedSegment = it },
                onClip = { playing = cameraSource(it) },
                saveStatus = saveStatus,
                onSaveSegment = { clips -> saveClips(clips.map { it.recording }) }
            )
        }

        CameraNetworkCard()
        AutomationCard()
        Spacer(Modifier.height(8.dp))
    }

    if (askPause) {
        AlertDialog(
            onDismissRequest = { askPause = false },
            title = { Text("Pausar a gravação por alguns segundos?") },
            text = {
                Text(
                    "Esta dashcam só entrega a lista de vídeos com a gravação pausada. " +
                        "O Impulse pausa, lê a lista e retoma a gravação em seguida (cerca de 2 segundos). " +
                        "Se a retomada não for confirmada, a tela avisa."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        askPause = false
                        refreshCatalog(true)
                    },
                    colors = impButtonColors()
                ) { Text("Pausar e atualizar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    askPause = false
                    message = "Lista não atualizada. Mostrando a última lista guardada."
                }) { Text("Cancelar") }
            }
        )
    }

    playing?.let { source ->
        PlayerDialog(
            source = source,
            saveStatus = source.recording?.let { saveStatus[it.name] },
            onSave = source.recording?.let { r -> { saveClips(listOf(r)) } },
            onClose = { playing = null }
        )
    }
    sharing?.let { file -> ShareDialog(file = file, onClose = { sharing = null }) }
}

private fun saveErrorText(error: String): String = when {
    error == "no_space" -> "sem espaço na central (libere em Recursos → Limpar armazenamento)"
    error == "wifi_unavailable" -> "Wi-Fi da dashcam não conectado"
    error == "cancelled" -> "cancelado"
    error == "truncated" -> "arquivo incompleto"
    error == "camera_not_found" -> "câmera não encontrada nesta rede"
    error.startsWith("transport_") -> "conexão com a câmera caiu"
    else -> error
}

private fun catalogMessage(result: DashcamGalleryRepository.CatalogResult): String? {
    val resume = when (result.resumeConfirmed) {
        false -> " Retomada não confirmada: confira a câmera."
        else -> ""
    }
    return when (result.status) {
        CatalogStatus.OK -> if (result.pausedForCatalog) "Lista atualizada. Gravação retomada." else "Lista atualizada."
        CatalogStatus.PARTIAL -> "Lista parcial (${errorText(result.error)})." + resume
        CatalogStatus.NEEDS_PAUSE -> null
        CatalogStatus.BUSY -> "A dashcam está ocupada com outra operação. Tente de novo."
        CatalogStatus.FAILED -> "Não foi possível ler a lista: ${errorText(result.error)}." + resume
    }
}

private fun errorText(error: String?): String = when {
    error == null -> "motivo desconhecido"
    error == "wifi_unavailable" -> "Wi-Fi da dashcam não conectado"
    error == "emergency_recording" -> "gravação de emergência em andamento"
    error == "resume_unconfirmed" -> "uma pausa anterior ainda não teve a retomada confirmada"
    error.contains("time_budget") -> "tempo limite da leitura"
    error.contains("busy_recording") -> "a câmera recusou a lista enquanto grava"
    error.contains("camera_not_found") -> "câmera não encontrada nesta rede"
    error.contains("transport_") -> "sem resposta da câmera"
    error.contains("untrusted_status") -> "modelo ou modo da câmera não reconhecido"
    else -> error
}

private fun formatFetched(ms: Long): String {
    val time = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ms), java.time.ZoneId.systemDefault())
    return time.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
}

private fun formatBytes(bytes: Long): String {
    val gb = bytes / 1_000_000_000.0
    return if (gb >= 1) String.format(PtBr, "%.1f GB", gb) else String.format(PtBr, "%d MB", bytes / 1_000_000)
}

private fun openWifiSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Abra o Wi-Fi da central e conecte à rede da dashcam.", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(CardColor)
            .border(1.dp, CardBorder, CardShape)
            .padding(20.dp)
    ) { content() }
}

@Composable
private fun Notice(text: String) {
    Text(
        text,
        color = ImpTokens.TextSecondary,
        fontSize = ImpulseTextSizes.BodyCompact,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(ImpTokens.Container)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun StateCard(
    state: DashcamGalleryRepository.StateSnapshot?,
    onConnect: () -> Unit,
    onQuery: () -> Unit,
    onRecover: () -> Unit,
    busy: Boolean
) {
    val (label, color) = stateLabel(state)
    SectionCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Último estado observado", color = ImpTokens.TextSecondary, fontSize = ImpulseTextSizes.BodyCompact)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(10.dp))
                    Text(label, color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Section, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                val observed = state?.let { " · consultado às " + formatFetched(it.observedAtMs).substringAfter(' ') } ?: ""
                Text(
                    "Cartão de memória: " + (state?.freeBytes?.let { formatBytes(it) + " livres" }
                        ?: "espaço livre indisponível neste modelo") + observed,
                    color = ImpTokens.TextMuted,
                    fontSize = ImpulseTextSizes.BodyCompact
                )
            }
            Button(onClick = onConnect, colors = impButtonColors()) { Text("Conectar à dashcam") }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onQuery, enabled = !busy) { Text("Consultar estado", color = ImpTokens.TextPrimary) }
        }
        if (state?.resumeUnconfirmed == true) {
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RowShape)
                    .background(ImpTokens.Attention.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    "Retomada não confirmada. A galeria pausou a gravação e não conseguiu confirmar a volta.",
                    color = ImpTokens.Attention,
                    fontSize = ImpulseTextSizes.BodyCompact,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onRecover, enabled = !busy, colors = impButtonColors(ImpTokens.Attention)) {
                    Text("Verificar e retomar")
                }
            }
        }
    }
}

private fun stateLabel(state: DashcamGalleryRepository.StateSnapshot?): Pair<String, Color> = when (state?.kind) {
    null -> "Consultando…" to ImpTokens.ThumbOff
    StateKind.WIFI_OFF -> "Wi-Fi da dashcam não conectado" to ImpTokens.ThumbOff
    StateKind.UNREACHABLE -> "Dashcam sem resposta" to ImpTokens.Attention
    StateKind.RECORDING -> "Gravando" to RecordingGreen
    StateKind.EMERGENCY -> "Gravação de emergência" to ImpTokens.Danger
    StateKind.PAUSED -> "Pausada" to ImpTokens.Attention
    StateKind.PAUSED_BY_CHARGING -> "Pausada pela automação (recarga ou carro fora do READY)" to ImpTokens.Attention
    StateKind.PAUSED_BY_GALLERY -> "Pausada pela galeria" to ImpTokens.Attention
    StateKind.OTHER_MODE -> "Modo da câmera: ${state?.workMode ?: "desconhecido"}" to ImpTokens.Attention
    StateKind.UNKNOWN -> "Estado desconhecido" to ImpTokens.ThumbOff
}

@Composable
private fun CalendarCard(
    modifier: Modifier,
    catalog: DashcamCatalog,
    month: YearMonth,
    selectedDay: LocalDate?,
    busy: Boolean,
    refreshEnabled: Boolean,
    onMonth: (YearMonth) -> Unit,
    onToday: () -> Unit,
    onDay: (LocalDate) -> Unit,
    onRefresh: () -> Unit
) {
    val today = LocalDate.now()
    SectionCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Dias com gravações",
                color = ImpTokens.TextPrimary,
                fontSize = ImpulseTextSizes.Label,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onRefresh, enabled = !busy && refreshEnabled) {
                Text(if (busy) "Atualizando…" else "Atualizar galeria", color = ImpTokens.TextPrimary)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onMonth(month.minusMonths(1)) }) { Text("‹", color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Section) }
            Text(
                month.format(MonthYear).replaceFirstChar { it.titlecase(PtBr) },
                color = ImpTokens.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = ImpulseTextSizes.BodyCompact,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onMonth(month.plusMonths(1)) }) { Text("›", color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Section) }
            TextButton(onClick = onToday) { Text("Hoje", color = ImpTokens.Accent, fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach {
                Text(
                    it,
                    color = ImpTokens.TextMuted,
                    fontSize = ImpulseTextSizes.BodyCompact,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
            }
        }
        // Sunday-first grid: java.time DayOfWeek is Monday=1..Sunday=7.
        val leading = month.atDay(1).dayOfWeek.value % 7
        val cells = leading + month.lengthOfMonth()
        val weeks = (cells + 6) / 7
        for (week in 0 until weeks) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val dayNumber = week * 7 + column - leading + 1
                    Box(Modifier.weight(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (dayNumber in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                hasRecordings = catalog.hasRecordingsOn(date),
                                selected = date == selectedDay,
                                isToday = date == today,
                                onClick = { onDay(date) }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).border(1.5.dp, ImpTokens.Accent, CircleShape))
            Text("  Hoje", color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
            Spacer(Modifier.width(20.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(ImpTokens.Accent))
            Text("  Com gravações", color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
        }
        if (catalog.undated.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${catalog.undated.size} arquivo(s) com data desconhecida no nome.",
                color = ImpTokens.TextMuted,
                fontSize = ImpulseTextSizes.BodyCompact
            )
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, hasRecordings: Boolean, selected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (hasRecordings) ImpTokens.IndexActiveTint else Color.Transparent)
            .then(
                when {
                    selected -> Modifier.border(2.dp, ImpTokens.Accent, shape)
                    isToday -> Modifier.border(1.dp, ImpTokens.Accent.copy(alpha = 0.6f), shape)
                    else -> Modifier
                }
            )
            .clickable(enabled = hasRecordings, onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = if (hasRecordings) ImpTokens.TextPrimary else ImpTokens.TextMuted,
            fontWeight = if (hasRecordings) FontWeight.Bold else FontWeight.Normal,
            fontSize = ImpulseTextSizes.BodyCompact
        )
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (hasRecordings) ImpTokens.Accent else Color.Transparent)
        )
    }
}

@Composable
private fun DayCard(
    modifier: Modifier,
    catalog: DashcamCatalog,
    day: LocalDate?,
    selectedSegment: Int,
    onSegment: (Int) -> Unit,
    onClip: (DashcamCatalog.Clip) -> Unit,
    saveStatus: Map<String, String>,
    onSaveSegment: (List<DashcamCatalog.Clip>) -> Unit
) {
    SectionCard(modifier) {
        if (day == null) {
            Text("Galeria", color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Nenhuma lista guardada. Conecte a central ao Wi-Fi da dashcam e toque em Atualizar galeria.",
                color = ImpTokens.TextSecondary,
                fontSize = ImpulseTextSizes.BodyCompact
            )
            return@SectionCard
        }
        Text(day.format(DayMonth), color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        val segments = catalog.getSegments(day)
        if (segments.isEmpty()) {
            Text("Sem gravações neste dia na última lista.", color = ImpTokens.TextSecondary, fontSize = ImpulseTextSizes.BodyCompact)
            return@SectionCard
        }
        val current = segments.getOrElse(selectedSegment) { segments.first() }
        segments.forEachIndexed { index, segment ->
            val bytes = clipsBytes(segment.clips)
            ListRow(
                title = "${segment.start.format(HourMinute)}–${segment.estimatedEnd.format(HourMinute)}",
                subtitle = "${segment.clips.size} arquivo${if (segment.clips.size == 1) "" else "s"}" +
                    (if (bytes > 0) " · ${formatBytes(bytes)}" else "") +
                    if (segment.hasEmergency()) " · inclui emergência" else "",
                selected = segment === current,
                onClick = { onSegment(index) },
                menu = listOf(
                    "Mostrar arquivos" to { onSegment(index) },
                    withEstimate("Salvar faixa inteira no carro", bytes) to { onSaveSegment(segment.clips) }
                )
            )
            Spacer(Modifier.height(8.dp))
        }
        HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))
        Text("Arquivos da faixa selecionada", color = ImpTokens.TextPrimary, fontWeight = FontWeight.Bold, fontSize = ImpulseTextSizes.BodyCompact)
        Text(
            "Toque para assistir. Toque longo para salvar no carro.",
            color = ImpTokens.TextMuted,
            fontSize = ImpulseTextSizes.BodyCompact
        )
        Spacer(Modifier.height(10.dp))
        current.clips.forEachIndexed { index, clip ->
            val recording = clip.recording
            val rest = current.clips.drop(index)
            ListRow(
                title = "${recording.start.format(HourMinute)}–${clip.estimatedEnd.format(HourMinute)}" +
                    if (recording.isEmergency) "  · emergência" else "",
                subtitle = recording.name +
                    (if (recording.size > 0) " · ${formatBytes(recording.size)}" else "") +
                    (saveStatus[recording.name]?.let { " · $it" } ?: ""),
                selected = false,
                onClick = { onClip(clip) },
                menu = buildList {
                    add("Assistir" to { onClip(clip) })
                    add(withEstimate("Salvar este arquivo no carro", recording.size) to { onSaveSegment(listOf(clip)) })
                    if (rest.size > 1) {
                        add(withEstimate("Salvar daqui até o fim da faixa (${rest.size} arquivos)", clipsBytes(rest)) to {
                            onSaveSegment(rest)
                        })
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
        }
        if (catalog.hasEstimatedEnds()) {
            Text("Os horários de fim das gravações são estimados.", color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
        }
    }
}

/** Short tap = [onClick]; long press opens [menu] (label → action) when it is not empty. */
@kotlin.OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ListRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    menu: List<Pair<String, () -> Unit>> = emptyList()
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RowShape)
                .background(if (selected) ImpTokens.IndexActiveTint else Color.Transparent)
                .border(1.dp, if (selected) ImpTokens.Accent.copy(alpha = 0.5f) else CardBorder, RowShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (menu.isEmpty()) null else ({ menuOpen = true })
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.BodyCompact, fontWeight = FontWeight.Medium)
                Text(subtitle, color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
            }
            Text("›", color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Section)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            menu.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = ImpulseTextSizes.BodyCompact) },
                    onClick = {
                        menuOpen = false
                        action()
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraNetworkCard() {
    val context = LocalContext.current
    val prefs = remember {
        App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    }
    val manager = remember { DashcamNetworkManager.getInstance(context) }
    var ssid by remember { mutableStateOf(prefs.getString(SharedPreferencesKeys.DASHCAM_STATION_SSID.key, "") ?: "") }
    var password by remember {
        mutableStateOf(prefs.getString(SharedPreferencesKeys.DASHCAM_STATION_PASSWORD.key, "") ?: "")
    }
    var showPassword by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(manager.isBusy) }
    var result by remember { mutableStateOf<String?>(null) }
    val statePrefs = remember {
        App.getDeviceProtectedContext().getSharedPreferences(DashcamGalleryRepository.STATE_PREFS, Context.MODE_PRIVATE)
    }
    var lastAuto by remember { mutableStateOf<String?>(null) }

    fun refreshStatus() {
        manager.readStatus { status = it.text }
        val at = statePrefs.getLong(DashcamAutomationManager.KEY_AUTO_STATION_LAST_RESULT_AT, 0L)
        val text = statePrefs.getString(DashcamAutomationManager.KEY_AUTO_STATION_LAST_RESULT_TEXT, null)
        lastAuto = if (at <= 0L || text.isNullOrBlank()) null else {
            val time = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date(at))
            val ok = statePrefs.getBoolean(DashcamAutomationManager.KEY_AUTO_STATION_LAST_RESULT_OK, false)
            "Última troca automática ($time): " + (if (ok) "" else "falhou. ") + text
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshStatus()
            busy = manager.isBusy
            delay(STATE_POLL_MS)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ImpTokens.Accent,
        unfocusedBorderColor = CardBorder,
        focusedContainerColor = ImpTokens.Container,
        unfocusedContainerColor = ImpTokens.Container,
        focusedTextColor = ImpTokens.TextPrimary,
        unfocusedTextColor = ImpTokens.TextPrimary,
        focusedLabelColor = ImpTokens.Accent,
        unfocusedLabelColor = ImpTokens.TextMuted
    )

    SectionCard(Modifier.fillMaxWidth()) {
        Text("Rede da câmera", color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Leva a câmera (Novatek, sem botões) do Wi-Fi próprio para outra rede, como o roteador do celular. " +
                "A central entra junto e continua vendo os vídeos.",
            color = ImpTokens.TextMuted,
            fontSize = ImpulseTextSizes.BodyCompact
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ssid,
                onValueChange = {
                    ssid = it
                    prefs.edit { putString(SharedPreferencesKeys.DASHCAM_STATION_SSID.key, it) }
                },
                label = { Text("Nome da rede (SSID)") },
                singleLine = true,
                enabled = !busy,
                colors = fieldColors,
                shape = RowShape,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    prefs.edit { putString(SharedPreferencesKeys.DASHCAM_STATION_PASSWORD.key, it) }
                },
                label = { Text("Senha") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Ocultar" else "Mostrar", fontSize = ImpulseTextSizes.BodyCompact)
                    }
                },
                colors = fieldColors,
                shape = RowShape,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Onde está a câmera: " + (status ?: "verificando…"),
            color = ImpTokens.TextPrimary,
            fontSize = ImpulseTextSizes.BodyCompact
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val invalid = DashcamStationPolicy.validate(ssid, password)
                    if (invalid != null) {
                        result = DashcamStationPolicy.validationMessage(invalid)
                    } else {
                        busy = true
                        result = "Levando a câmera para a rede $ssid… (pode levar até 2 minutos)"
                        manager.moveToStation {
                            busy = false
                            result = it.message
                            refreshStatus()
                        }
                    }
                },
                enabled = !busy,
                colors = impButtonColors()
            ) { Text("Levar câmera para esta rede") }
            OutlinedButton(
                onClick = {
                    busy = true
                    result = "Trazendo a câmera para o Wi-Fi próprio… (pode levar até 2 minutos)"
                    manager.returnToAp {
                        busy = false
                        result = it.message
                        refreshStatus()
                    }
                },
                enabled = !busy
            ) { Text("Trazer câmera para o Wi-Fi próprio") }
        }
        result?.let {
            Spacer(Modifier.height(12.dp))
            Notice(it)
        }
        lastAuto?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Fazer isso sozinho",
                hint = "Espera ${DashcamStationPolicy.autoWaitMinutes()} min depois de ligar o carro, para a câmera " +
                    "terminar de iniciar, e então confere a cada minuto: com a central no Wi-Fi da câmera e a rede " +
                    "acima ao alcance, leva a câmera para ela; se a central entrou na rede antes da câmera, passa " +
                    "pelo Wi-Fi da câmera e a leva. A câmera volta ao Wi-Fi próprio quando o carro é desligado.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_AUTO_STATION,
                prefs = prefs,
                default = false
            )
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Atenção: na rede compartilhada a câmera não tem proteção por senha. Qualquer aparelho dessa rede " +
                "pode ver e apagar os vídeos e mudar a configuração dela. Use só uma rede sua, de confiança.",
            color = ImpTokens.Attention,
            fontSize = ImpulseTextSizes.BodyCompact
        )
    }
}

@Composable
private fun AutomationCard() {
    val prefs = remember {
        App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    }
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Automação", color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Pausar dashcam durante recarga",
                hint = "Pausa só com o veículo carregando e parado; retoma ao fim da recarga ou ao andar.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_PAUSE_WHILE_CHARGING,
                prefs = prefs
            )
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Gravar só com o carro pronto (READY)",
                hint = "Pausa fora do READY, em qualquer marcha: ligado sem freio, recarga. Retoma no READY.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_RECORD_ONLY_WHEN_READY,
                prefs = prefs
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Verificar e retomar a gravação a cada 5 minutos",
                hint = "Retoma também pausas manuais. Não interfere na pausa da recarga nem na emergência.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_WATCHDOG_RESUME,
                prefs = prefs
            )
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Sincronizar o relógio da dashcam",
                hint = "Usa a hora de rede (NTP) ou do GPS. Necessário para casar o vídeo com o trajeto. Não corrige gravações antigas.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_CLOCK_SYNC,
                prefs = prefs
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Localizar os vídeos no mapa (GPS)",
                hint = "Guarda o trajeto do GPS da central por 72 h, só no carro. Precisa do relógio sincronizado.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_GPS_TRACK,
                prefs = prefs,
                default = true
            )
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Gerar o carimbo sozinho",
                hint = "Todo vídeo salvo ganha uma cópia com carimbo (velocidade, dados do carro, endereço e " +
                    "mapa), feita em segundo plano quando a central tiver internet. Fica em Salvos no carro.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_AUTO_STAMP,
                prefs = prefs,
                default = true
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Aviso de falha de gravação",
                hint = "Avisa uma vez quando a câmera é encontrada sem gravar.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_FAILURE_WARNING,
                prefs = prefs
            )
            AutomationSwitch(
                modifier = Modifier.weight(1f),
                title = "Permitir vídeo com o carro em movimento",
                hint = "Para o passageiro assistir. Desligado, o vídeo só toca com o carro parado.",
                key = SharedPreferencesKeys.ENABLE_DASHCAM_PLAYBACK_WHILE_MOVING,
                prefs = prefs,
                default = true
            )
        }
    }
}

@Composable
private fun AutomationSwitch(
    modifier: Modifier,
    title: String,
    hint: String,
    key: SharedPreferencesKeys,
    prefs: android.content.SharedPreferences,
    // Every dashcam option is on until the user turns it off.
    default: Boolean = true
) {
    var checked by remember { mutableStateOf(prefs.getBoolean(key.key, default)) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.BodyCompact)
            Text(hint, color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                prefs.edit { putBoolean(key.key, it) }
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = ImpTokens.Accent,
                checkedThumbColor = ImpTokens.OnAccent,
                uncheckedTrackColor = ImpTokens.TrackOff,
                uncheckedThumbColor = ImpTokens.ThumbOff
            )
        )
    }
}

/** Current speed in km/h from the vehicle service, or null when unknown. */
private fun currentSpeedKmh(): Double? = try {
    ServiceManager.getInstance()
        .getUpdatedData(CarConstants.CAR_BASIC_VEHICLE_SPEED.value)
        ?.trim()
        ?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }
} catch (e: RuntimeException) {
    null
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerDialog(
    source: PlayerSource,
    saveStatus: String?,
    onSave: (() -> Unit)?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        App.getDeviceProtectedContext().getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
    }
    var error by remember(source.uri) { mutableStateOf<String?>(null) }
    var blockReason by remember { mutableStateOf<String?>(null) }
    val exoPlayer = remember(source.uri) {
        val builder = ExoPlayer.Builder(context)
        // Camera clips stream over the camera Wi-Fi; saved copies play from local storage.
        if (source.recording != null) {
            builder.setMediaSourceFactory(DefaultMediaSourceFactory(DashcamVideoDataSource.factory(context)))
        }
        builder.build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                error = "Não foi possível reproduzir (${e.errorCodeName})." +
                    if (source.recording != null) " Confira o Wi-Fi da dashcam." else ""
            }
        }
        exoPlayer.addListener(listener)
        exoPlayer.setMediaItem(MediaItem.fromUri(source.uri), source.startMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Moving-car lock only when "Permitir vídeo com o carro em movimento" is off.
    LaunchedEffect(exoPlayer) {
        while (true) {
            val allowMoving = prefs.getBoolean(SharedPreferencesKeys.ENABLE_DASHCAM_PLAYBACK_WHILE_MOVING.key, true)
            val reason = if (allowMoving) null else {
                // getUpdatedData is a binder call: never on the main thread.
                val speed = withContext(Dispatchers.IO) { currentSpeedKmh() }
                when {
                    speed == null -> "Velocidade do veículo indisponível. Por segurança, a reprodução fica bloqueada."
                    speed > MAX_PLAYBACK_KMH -> "Reprodução disponível somente com o veículo parado."
                    else -> null
                }
            }
            if (reason != blockReason) {
                blockReason = reason
                exoPlayer.playWhenReady = reason == null
            }
            delay(1_000)
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(CardShape)
                .background(Color.Black)
                .border(BorderStroke(1.dp, CardBorder), CardShape)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(source.title, color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
                    Text(
                        source.subtitle + (saveStatus?.let { " · $it" } ?: ""),
                        color = ImpTokens.TextMuted,
                        fontSize = ImpulseTextSizes.BodyCompact
                    )
                }
                if (onSave != null) {
                    OutlinedButton(onClick = onSave, enabled = saveStatus?.startsWith("salvando") != true) {
                        Text("Salvar no carro", color = ImpTokens.TextPrimary)
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Button(onClick = onClose, colors = impButtonColors()) { Text("Fechar") }
            }
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Video and GPS trail side by side; the video keeps 16:9 and never outgrows the screen.
                val panelW = 300.dp
                val videoW0 = maxWidth - panelW - 12.dp
                val videoH = minOf(videoW0 * 9f / 16f, 540.dp)
                val videoW = videoH * 16f / 9f
                // Centred: the dialog is wider than video + panel on the 1920×720 head unit.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Box(Modifier.width(videoW).height(videoH), contentAlignment = Alignment.Center) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    player = exoPlayer
                                }
                            },
                            update = { it.player = exoPlayer },
                            modifier = Modifier.fillMaxSize()
                        )
                        blockReason?.let { reason ->
                            Box(
                                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    reason,
                                    color = ImpTokens.TextPrimary,
                                    fontSize = ImpulseTextSizes.Body,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    TrailPanel(
                        clipName = source.recording?.name ?: source.subtitle,
                        player = exoPlayer,
                        modifier = Modifier.width(panelW).height(videoH)
                    )
                }
            }
            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = ImpTokens.Danger, fontSize = ImpulseTextSizes.BodyCompact)
            }
        }
    }
}

/**
 * GPS trail of the clip, from the head unit's own GPS (satellite time, see DashcamGpsTrack). The
 * dot follows the playback position. No map tiles: the line, the speed and the coordinates, plus a
 * QR code that opens the point in the phone's map app.
 */
@Composable
private fun TrailPanel(clipName: String, player: ExoPlayer, modifier: Modifier) {
    val context = LocalContext.current
    var fixes by remember(clipName) { mutableStateOf<List<DashcamGpsTrack.Fix>?>(null) }
    LaunchedEffect(clipName) {
        fixes = withContext(Dispatchers.IO) { DashcamGpsTrack.getInstance(context).forClip(clipName) }
    }
    val startUtc = remember(clipName) { DashcamGpsTrack.clipStartUtcMs(clipName, java.time.ZoneId.systemDefault()) }
    var positionMs by remember { mutableStateOf(0L) }
    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            delay(250)
        }
    }
    val clockSyncedAt = remember {
        App.getDeviceProtectedContext()
            .getSharedPreferences(DashcamGalleryRepository.STATE_PREFS, Context.MODE_PRIVATE)
            .getLong(DashcamAutomationManager.KEY_CLOCK_SYNCED_AT, 0L)
    }
    var qrFor by remember { mutableStateOf<DashcamGpsTrack.Fix?>(null) }

    Column(
        modifier
            .clip(CardShape)
            .background(Color(0xFF111418))
            .border(BorderStroke(1.dp, CardBorder), CardShape)
            .padding(12.dp)
    ) {
        Text("Trajeto GPS", color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val list = fixes
        when {
            list == null -> Text("Carregando…", color = ImpTokens.TextMuted, fontSize = ImpulseTextSizes.BodyCompact)
            list.isEmpty() -> {
                Text("Sem trajeto GPS para este horário.", color = ImpTokens.TextSecondary, fontSize = ImpulseTextSizes.BodyCompact)
                Spacer(Modifier.height(6.dp))
                Text(
                    "O trajeto é guardado por 72 h a partir da ativação de \"Localizar os vídeos no mapa\" (Automação).",
                    color = ImpTokens.TextMuted,
                    fontSize = ImpulseTextSizes.BodyCompact
                )
            }
            else -> {
                val current = startUtc?.let { DashcamGpsTrack.interpolate(list, it + positionMs) }
                TrailCanvas(list, current, Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(8.dp))
                val shown = current ?: list.first()
                val kmh = shown.speedKmh
                Text(
                    if (kmh >= 0) "${kmh.roundToInt()} km/h" else "Velocidade indisponível",
                    color = ImpTokens.TextPrimary,
                    fontSize = ImpulseTextSizes.Body,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    String.format(java.util.Locale.ROOT, "%.5f, %.5f", shown.lat, shown.lon).replace('.', ',')
                        .replace(", ", "; "),
                    color = ImpTokens.TextSecondary,
                    fontSize = ImpulseTextSizes.BodyCompact
                )
                Text(
                    "GPS " + java.time.Instant.ofEpochMilli(shown.utcMs).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                    color = ImpTokens.TextMuted,
                    fontSize = ImpulseTextSizes.BodyCompact
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { qrFor = shown }) { Text("Abrir no celular", color = ImpTokens.TextPrimary) }
            }
        }
        if (clockSyncedAt == 0L) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Relógio da câmera ainda não sincronizado: a posição pode estar deslocada.",
                color = ImpTokens.Attention,
                fontSize = ImpulseTextSizes.BodyCompact
            )
        }
    }

    qrFor?.let { fix ->
        val url = String.format(java.util.Locale.ROOT, "https://www.google.com/maps?q=%.6f,%.6f", fix.lat, fix.lon)
        AlertDialog(
            onDismissRequest = { qrFor = null },
            title = { Text("Abrir no celular") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Leia o QR code com a câmera do celular para abrir este ponto no mapa.", fontSize = ImpulseTextSizes.BodyCompact)
                    Spacer(Modifier.height(12.dp))
                    QrCode(url, 240.dp)
                }
            },
            confirmButton = { Button(onClick = { qrFor = null }, colors = impButtonColors()) { Text("Fechar") } }
        )
    }
}

@Composable
private fun TrailCanvas(fixes: List<DashcamGpsTrack.Fix>, current: DashcamGpsTrack.Fix?, modifier: Modifier) {
    Canvas(modifier.clip(CardShape).background(Color(0xFF0A0C0F))) {
        val pad = 16f
        val midLat = fixes.sumOf { it.lat } / fixes.size
        val kx = Math.cos(Math.toRadians(midLat))
        val minX = fixes.minOf { it.lon * kx }
        val maxX = fixes.maxOf { it.lon * kx }
        val minY = fixes.minOf { it.lat }
        val maxY = fixes.maxOf { it.lat }
        // At least ~60 m across, so a parked car is a dot in the middle, not a zoomed-in jitter.
        val span = maxOf(maxX - minX, maxY - minY, 0.0006)
        val scale = minOf(size.width - 2 * pad, size.height - 2 * pad) / span
        val cx = (minX + maxX) / 2
        val cy = (minY + maxY) / 2
        fun point(f: DashcamGpsTrack.Fix) = Offset(
            (size.width / 2 + (f.lon * kx - cx) * scale).toFloat(),
            (size.height / 2 - (f.lat - cy) * scale).toFloat()
        )
        val path = androidx.compose.ui.graphics.Path()
        fixes.forEachIndexed { i, f ->
            val o = point(f)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(path, ImpTokens.Accent, style = Stroke(width = 4f))
        drawCircle(Color(0xFF9AA4AE), radius = 5f, center = point(fixes.first()))
        current?.let {
            val o = point(it)
            drawCircle(ImpTokens.Accent.copy(alpha = 0.3f), radius = 14f, center = o)
            drawCircle(Color.White, radius = 7f, center = o)
        }
    }
}

@Composable
private fun ViewTab(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, colors = impButtonColors()) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label, color = ImpTokens.TextPrimary) }
    }
}

@Composable
private fun SavedCard(
    files: List<File>,
    freeBytes: Long,
    onPlay: (File) -> Unit,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit,
    stampStatus: Map<String, String> = emptyMap(),
    onStamp: (File) -> Unit = {}
) {
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Salvos no carro", color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.Label, fontWeight = FontWeight.Bold)
        Text(
            "${files.size} vídeo${if (files.size == 1) "" else "s"} · ${formatBytes(files.sumOf { it.length() })} usados · " +
                "${formatBytes(freeBytes)} livres na central",
            color = ImpTokens.TextMuted,
            fontSize = ImpulseTextSizes.BodyCompact
        )
        Spacer(Modifier.height(6.dp))
        if (files.isEmpty()) {
            Text(
                "Nenhum vídeo salvo. Na galeria, toque longo num arquivo ou numa faixa para salvar no carro.",
                color = ImpTokens.TextSecondary,
                fontSize = ImpulseTextSizes.BodyCompact
            )
            return@SectionCard
        }
        Text(
            "\"Enviar ao celular\" mostra um QR code: conecte a central ao Wi-Fi de casa ou ao hotspot do celular e leia com a câmera do celular. Apague o que já enviou para liberar espaço na central.",
            color = ImpTokens.TextMuted,
            fontSize = ImpulseTextSizes.BodyCompact
        )
        Spacer(Modifier.height(10.dp))
        files.forEach { file ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RowShape)
                    .border(1.dp, CardBorder, RowShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(file.name, color = ImpTokens.TextPrimary, fontSize = ImpulseTextSizes.BodyCompact, fontWeight = FontWeight.Medium)
                    Text(
                        formatBytes(file.length()) +
                            ((stampStatus[file.name] ?: DashcamAutoStamp.statusFor(file.name))?.let { " · $it" } ?: ""),
                        color = ImpTokens.TextMuted,
                        fontSize = ImpulseTextSizes.BodyCompact
                    )
                }
                OutlinedButton(onClick = { onPlay(file) }) { Text("Assistir", color = ImpTokens.TextPrimary) }
                Spacer(Modifier.width(8.dp))
                if (!DashcamStampJob.isStamped(file.name) && file.name.endsWith(".mp4", ignoreCase = true)) {
                    val busy = stampStatus[file.name]?.startsWith("carimbando") == true
                    OutlinedButton(onClick = { onStamp(file) }, enabled = !busy) {
                        Text("Gerar com carimbo", color = ImpTokens.TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = { onShare(file) }, colors = impButtonColors()) { Text("Enviar ao celular") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onDelete(file) }) { Text("Apagar", color = ImpTokens.Danger) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareDialog(file: File, onClose: () -> Unit) {
    val context = LocalContext.current
    var link by remember(file) { mutableStateOf<DashcamShareServer.Link?>(null) }
    LaunchedEffect(file) {
        link = withContext(Dispatchers.IO) { DashcamShareServer.start(context, file) }
    }
    DisposableEffect(file) {
        onDispose { DashcamShareServer.stop() }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Enviar ao celular") },
        text = {
            val current = link
            when {
                current == null -> Text("Preparando o link…")
                current.error != null -> Text(
                    when (current.error) {
                        "on_camera_wifi" -> "A central está no Wi-Fi da dashcam. Conecte-a ao Wi-Fi de casa ou ao hotspot do celular e tente de novo."
                        "wifi_unavailable" -> "A central não está em nenhum Wi-Fi. Conecte-a ao Wi-Fi de casa ou ao hotspot do celular."
                        else -> "Não foi possível abrir o link (${current.error})."
                    }
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No celular, na mesma rede Wi-Fi da central, leia o QR code com a câmera. O download começa no navegador.",
                        fontSize = ImpulseTextSizes.BodyCompact
                    )
                    Spacer(Modifier.height(12.dp))
                    QrCode(current.url, 260.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(current.url, fontSize = ImpulseTextSizes.BodyCompact, textAlign = TextAlign.Center)
                    Text(
                        "${file.name} · ${formatBytes(file.length())} · link válido até " +
                            formatFetched(current.expiresAtMs).substringAfter(' '),
                        fontSize = ImpulseTextSizes.BodyCompact
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose, colors = impButtonColors()) { Text("Fechar") }
        }
    )
}

/** QR code drawn locally with ZXing: the link (with its access token) never leaves the device. */
@Composable
private fun QrCode(text: String, size: androidx.compose.ui.unit.Dp) {
    val matrix = remember(text) {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 2))
    }
    Canvas(Modifier.size(size).background(Color.White)) {
        val cell = this.size.width / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) {
                    drawRect(Color.Black, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}
