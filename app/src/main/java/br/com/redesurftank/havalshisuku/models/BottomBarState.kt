package br.com.redesurftank.havalshisuku.models

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

object BottomBarState {
    enum class SliderType {
        DRIVER_TEMP,
        PASS_TEMP,
        FAN,
        VOLUME
    }

    /** O que um swipe pra cima na barra faz. Valores = [SwipeUpAction.key]. */
    var swipeUpAction by mutableStateOf(SwipeUpAction.DASHBOARD.key)

    /** Pacote aberto quando [swipeUpAction] == [SwipeUpAction.CUSTOM_APP]. */
    var swipeUpPackage by mutableStateOf("")

    enum class SwipeUpAction(val key: String, val label: String) {
        DASHBOARD("dashboard", "Abrir o Dashboard (Impulse)"),
        HAVAL_HOME("haval_home", "Ir para a Home Haval"),
        APP_LAUNCHER("app_launcher", "Lista de apps Haval"),
        CUSTOM_APP("custom_app", "Abrir um app específico");

        companion object {
            fun fromKey(key: String?): SwipeUpAction =
                    entries.firstOrNull { it.key == key } ?: DASHBOARD

            const val HAVAL_HOME_PACKAGE = "com.beantechs.mediacenter"
            const val APP_LAUNCHER_PACKAGE = "com.beantechs.applist"
        }
    }

    var activeSliderType by mutableStateOf<SliderType?>(null)
    var sliderPositionX by mutableStateOf(0f)
    var sliderInteractionTrigger by mutableStateOf(0)
    var isSliderDragging by mutableStateOf(false)
    var isVisible by mutableStateOf(true)
    var isDashboardExpanded by mutableStateOf(false)
    /** Espelho OBSERVAVEL da pref ENABLE_RESOURCE_OVERLAY. Existe porque o snapshotFlow do
     *  BottomBarService so reage a estado do Compose — ler a SharedPreference direto la nao
     *  re-emitia, e o toggle so valeria depois de abrir/fechar a barra estendida. */
    var resourceOverlayEnabled by mutableStateOf(false)
    /** Espelhos OBSERVAVEIS do estilo/posicao do overlay de CPU/RAM: mudar no slider reposiciona na
     *  hora, sem precisar reabrir nada (o servico observa por snapshotFlow). */
    var resourceOverlayFontSp by mutableStateOf(14)
    var resourceOverlayCorner by mutableStateOf(3) // 0=sup.esq 1=sup.dir 2=inf.esq 3=inf.dir
    var resourceOverlayX by mutableStateOf(12)
    var resourceOverlayY by mutableStateOf(90)
    var isMenuExpanded by mutableStateOf(false)
    var isSettingsMenuExpanded by mutableStateOf(false)
    var isOverrideMenuExpanded by mutableStateOf(false)
    var selectedPackage by mutableStateOf("")
    var currentPackage by mutableStateOf("")
    var activeClusterProjectionPackage by mutableStateOf("")
    var mediaTitle by mutableStateOf<String?>(null)
    var mediaArtist by mutableStateOf<String?>(null)
    var mediaAlbum by mutableStateOf<String?>(null)
    var mediaPackageName by mutableStateOf<String?>(null)
    var mediaArtwork by mutableStateOf<Bitmap?>(null)
    var mediaIsPlaying by mutableStateOf(false)
    var mediaIsMuted by mutableStateOf(false)
    var mediaDurationMs by mutableLongStateOf(0L)
    var mediaElapsedMs by mutableLongStateOf(0L)
    var mediaProgressUpdatedAtMs by mutableLongStateOf(0L)
    var mediaCanSeek by mutableStateOf(false)
    var autoHideEnabled by mutableStateOf(false)
    /** Espelho OBSERVAVEL da pref BOTTOM_BAR_HIDDEN: quando true, a barra inferior NAO e desenhada
     *  (nem o overscan e aplicado) e o dashboard se abre so pelo atalho do volante. O servico segue
     *  vivo (atalho + monitores). Observado por snapshotFlow no BottomBarService p/ aplicar ao vivo. */
    var barHidden by mutableStateOf(false)
    var isFridaRunning by mutableStateOf(false)
    var isDeleteModeEnabled by mutableStateOf(false)
    val restoredApps = mutableStateListOf<String>()
}
