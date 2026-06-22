package com.example.carmedia // TODO: troque pelo package do seu app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/* =====================================================================================
 * PLAYER DE MÍDIA AUTOCONTIDO — extraído do haval-app-tool-multimidia
 *
 *  • Capa / título / artista / play-pause / faixa / seek  =  MediaController/MediaSession
 *    PADRÃO do Android (funciona em qualquer app, não depende do carro).
 *  • Volume  =  "pluggable". Por padrão usa AudioManager (STREAM_MUSIC). No head unit Haval,
 *    troque por gravar a chave do carro `sys.settings.audio.media_volume` (faixa 0..30):
 *        serviceCarro.getData("sys.settings.audio.media_volume")            // ler  (0..30)
 *        serviceCarro.updateData("sys.settings.audio.media_volume", valor)  // gravar
 *    (mute: sys.settings.audio.media_mute_state / sys.settings.audio.mute_adjust_action)
 *
 * REQUISITOS:
 *  1) Gradle:  implementation("androidx.compose.material:material-icons-extended")
 *  2) Registrar o MediaNotificationListenerService no AndroidManifest (ver rodapé).
 *  3) Conceder "Acesso a notificações" 1x (MediaControllerHelper.requestNotificationAccess).
 * ===================================================================================== */

// ---------- 1. Estado observável (Compose) ----------
class MediaPlaybackState {
    var title by mutableStateOf<String?>(null)
    var artist by mutableStateOf<String?>(null)
    var album by mutableStateOf<String?>(null)
    var artwork by mutableStateOf<Bitmap?>(null)
    var isPlaying by mutableStateOf(false)
    var durationMs by mutableLongStateOf(0L)
    var elapsedMs by mutableLongStateOf(0L)
    var positionUpdatedAtMs by mutableLongStateOf(0L)
    var canSeek by mutableStateOf(false)
    var packageName by mutableStateOf<String?>(null)
}

// ---------- 2. Motor: lê sessões ativas, extrai metadados e manda comandos ----------
class MediaControllerHelper(private val context: Context) {
    val state = MediaPlaybackState()

    private val main = Handler(Looper.getMainLooper())
    private var manager: MediaSessionManager? = null
    private var sessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private val callbacks = HashMap<MediaController, MediaController.Callback>()
    private val lock = Any()

    /** Chame em onCreate/onStart. */
    fun start(component: ComponentName = defaultListenerComponent(context)) {
        if (manager != null) return
        val mgr = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        manager = mgr
        val listenerComp = component.takeIf { isNotificationAccessGranted(context, it) }
        val l = MediaSessionManager.OnActiveSessionsChangedListener { updateControllers(it.orEmpty()) }
        sessionsListener = l
        try {
            updateControllers(mgr.getActiveSessions(listenerComp))
            mgr.addOnActiveSessionsChangedListener(l, listenerComp)
        } catch (e: SecurityException) {
            // Sem permissão de notification listener -> chame requestNotificationAccess()
        }
    }

    /** Chame em onDestroy/onStop. */
    fun stop() {
        synchronized(lock) {
            callbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }
            callbacks.clear()
        }
        sessionsListener?.let { manager?.removeOnActiveSessionsChangedListener(it) }
        sessionsListener = null
        manager = null
    }

    private fun updateControllers(controllers: List<MediaController>) {
        synchronized(lock) {
            callbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }
            callbacks.clear()
            controllers.forEach { c ->
                val cb = object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) = publishBest()
                    override fun onPlaybackStateChanged(s: PlaybackState?) = publishBest()
                    override fun onSessionDestroyed() = publishBest()
                }
                runCatching { c.registerCallback(cb); callbacks[c] = cb }
            }
        }
        publishBest()
    }

    /** Escolhe a melhor sessão (a que está tocando + com metadados) e publica no state. */
    private fun publishBest() {
        val controllers = synchronized(lock) { callbacks.keys.toList() }
        val selected = controllers
            .filterNot { it.packageName == "com.android.server.telecom" } // ignora telefonia
            .sortedWith(
                compareByDescending<MediaController> { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                    .thenByDescending { hasUsableMetadata(it.metadata) }
            )
            .firstOrNull { hasUsableMetadata(it.metadata) || it.playbackState != null }

        main.post {
            if (selected == null) { clear(); return@post }
            val m = selected.metadata
            val ps = selected.playbackState
            state.title = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: m?.description?.title?.toString()
            state.artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: m?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: m?.description?.subtitle?.toString()
            state.album = m?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            state.artwork = resolveArtwork(m)
            state.isPlaying = ps?.state == PlaybackState.STATE_PLAYING
            state.durationMs = (m?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)
            state.elapsedMs = (ps?.position ?: 0L).coerceAtLeast(0L)
            state.positionUpdatedAtMs = ps?.lastPositionUpdateTime?.takeIf { it > 0L } ?: SystemClock.elapsedRealtime()
            state.canSeek = state.durationMs > 0 && ((ps?.actions ?: 0L) and PlaybackState.ACTION_SEEK_TO) != 0L
            state.packageName = selected.packageName
        }
    }

    private fun clear() {
        state.title = null; state.artist = null; state.album = null; state.artwork = null
        state.isPlaying = false; state.durationMs = 0L; state.elapsedMs = 0L
        state.canSeek = false; state.packageName = null
    }

    // -------- comandos de transporte (transportControls do controller ativo) --------
    fun playPause() {
        val playing = state.isPlaying
        pick(if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY)?.let { c ->
            runCatching { if (playing) c.transportControls.pause() else c.transportControls.play() }
        }
    }
    fun next() { pick(PlaybackState.ACTION_SKIP_TO_NEXT)?.let { runCatching { it.transportControls.skipToNext() } } }
    fun previous() { pick(PlaybackState.ACTION_SKIP_TO_PREVIOUS)?.let { runCatching { it.transportControls.skipToPrevious() } } }
    fun seekTo(ms: Long) { pick(PlaybackState.ACTION_SEEK_TO)?.let { runCatching { it.transportControls.seekTo(ms.coerceAtLeast(0L)) } } }

    /** Prefere o controller do app que está tocando e que suporta a ação pedida. */
    private fun pick(action: Long): MediaController? {
        val controllers = synchronized(lock) { callbacks.keys.toList() }
        return controllers.firstOrNull { it.packageName == state.packageName && ((it.playbackState?.actions ?: 0L) and action) != 0L }
            ?: controllers.firstOrNull { ((it.playbackState?.actions ?: 0L) and action) != 0L }
    }

    private fun hasUsableMetadata(m: MediaMetadata?): Boolean {
        if (m == null) return false
        return !m.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() ||
               !m.getString(MediaMetadata.METADATA_KEY_ARTIST).isNullOrBlank()
    }

    private fun resolveArtwork(m: MediaMetadata?): Bitmap? = m?.let {
        it.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: it.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: it.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    }

    companion object {
        fun defaultListenerComponent(context: Context) =
            ComponentName(context, MediaNotificationListenerService::class.java)

        fun isNotificationAccessGranted(context: Context, component: ComponentName): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            return flat.split(":").any { ComponentName.unflattenFromString(it) == component }
        }
        fun requestNotificationAccess(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

// ---------- 3. NotificationListenerService (necessário p/ getActiveSessions) ----------
// Pode ficar vazio — o que importa é a permissão estar concedida.
class MediaNotificationListenerService : NotificationListenerService()

// ---------- 4. Volume via AudioManager (padrão; troque pela chave do carro no Haval) ----------
class SystemVolumeController(context: Context) {
    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val max: Int get() = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    fun get(): Int = am.getStreamVolume(AudioManager.STREAM_MUSIC)
    fun set(v: Int) = am.setStreamVolume(AudioManager.STREAM_MUSIC, v.coerceIn(0, max), 0)
}

// ---------- 5. Composable: card completo do player ----------
@Composable
fun MediaPlayerCard(
    helper: MediaControllerHelper,
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    volumeRange: IntRange = 0..30,
) {
    val s = helper.state
    val title = s.title
    val artist = s.artist
    val album = s.album
    val artwork = s.artwork
    val isPlaying = s.isPlaying
    val durationMs = s.durationMs
    val canSeek = s.canSeek
    val liveElapsed = rememberLiveElapsedMs(s.elapsedMs, durationMs, s.positionUpdatedAtMs, isPlaying)

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1F24))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Capa + título/artista
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2C3139))) {
                    if (artwork != null) {
                        Image(artwork.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.MusicNote, null,
                            Modifier.align(Alignment.Center).size(32.dp), tint = Color.White.copy(alpha = .5f))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(title ?: "Nada tocando", color = Color.White, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOfNotNull(artist, album).joinToString(" • ")
                    if (sub.isNotBlank()) Text(sub, color = Color.White.copy(alpha = .7f), fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Barra de progresso (arrastar = seek)
            if (durationMs > 0L) {
                var dragging by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(0f) }
                val pos = if (dragging) dragValue else liveElapsed.toFloat()
                Slider(
                    value = pos.coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { dragging = true; dragValue = it },
                    onValueChangeFinished = { helper.seekTo(dragValue.toLong()); dragging = false },
                    valueRange = 0f..durationMs.toFloat(),
                    enabled = canSeek,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(pos.toLong()), color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
                    Text(formatTime(durationMs), color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
                }
            }

            // Controles de transporte
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton({ helper.previous() }) {
                    Icon(Icons.Default.SkipPrevious, "Anterior", Modifier.size(36.dp), tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                FilledIconButton({ helper.playPause() }, Modifier.size(56.dp)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", Modifier.size(32.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton({ helper.next() }) {
                    Icon(Icons.Default.SkipNext, "Próxima", Modifier.size(36.dp), tint = Color.White)
                }
            }

            // Volume
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.VolumeDown, null, tint = Color.White.copy(alpha = .7f))
                IconButton({ onVolumeChange((volume - 1).coerceIn(volumeRange.first, volumeRange.last)) }) {
                    Icon(Icons.Default.Remove, "Menos", tint = Color.White)
                }
                Text("$volume", color = Color.White, modifier = Modifier.widthIn(min = 28.dp))
                IconButton({ onVolumeChange((volume + 1).coerceIn(volumeRange.first, volumeRange.last)) }) {
                    Icon(Icons.Default.Add, "Mais", tint = Color.White)
                }
                Icon(Icons.Default.VolumeUp, null, tint = Color.White.copy(alpha = .7f))
            }
        }
    }
}

// Tempo decorrido "ao vivo" (extrapola a posição enquanto toca; resincroniza a cada update)
@Composable
private fun rememberLiveElapsedMs(elapsedMs: Long, durationMs: Long, updatedAtMs: Long, isPlaying: Boolean): Long {
    var now by remember { mutableLongStateOf(elapsedMs) }
    LaunchedEffect(elapsedMs, updatedAtMs, isPlaying, durationMs) {
        now = elapsedMs
        if (isPlaying && durationMs > 0L) {
            while (true) {
                delay(500)
                now = (now + 500).coerceAtMost(durationMs)
            }
        }
    }
    return now
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

/* =====================================================================================
 * AndroidManifest.xml — adicione dentro de <application>:
 *
 *   <service
 *       android:name=".MediaNotificationListenerService"
 *       android:exported="false"
 *       android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
 *       <intent-filter>
 *           <action android:name="android.service.notification.NotificationListenerService" />
 *       </intent-filter>
 *   </service>
 *
 * USO:
 *   val helper = remember { MediaControllerHelper(context) }
 *   DisposableEffect(Unit) {
 *       if (!MediaControllerHelper.isNotificationAccessGranted(context, MediaControllerHelper.defaultListenerComponent(context)))
 *           MediaControllerHelper.requestNotificationAccess(context)   // usuário concede 1x
 *       helper.start()
 *       onDispose { helper.stop() }
 *   }
 *   val volCtl = remember { SystemVolumeController(context) }
 *   var vol by remember { mutableIntStateOf(volCtl.get()) }
 *   MediaPlayerCard(helper, vol, onVolumeChange = { vol = it; volCtl.set(it) }, volumeRange = 0..volCtl.max)
 *
 *   // No Haval, no onVolumeChange use a chave do carro em vez do volCtl:
 *   //   serviceCarro.updateData("sys.settings.audio.media_volume", it.toString())
 * ===================================================================================== */
