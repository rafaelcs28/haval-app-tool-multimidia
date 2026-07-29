package br.com.redesurftank.havalshisuku.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Paleta Impulse (Rodada 15) — mesmos valores do ImpTokens, aqui como colorScheme do
// Material 3 pra TODO componente sem cor explícita (AlertDialog, TextButton, dropdown,
// ProgressIndicator, Switch/Slider) herdar a marca em vez do roxo do template.
private val ImpulseDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A9EFF),                 // Accent
    onPrimary = Color(0xFF0A0A0C),               // texto sobre o acento (preto)
    primaryContainer = Color(0xFF14161C),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFF8A93A3),               // TextSecondary
    onSecondary = Color(0xFF0A0A0C),
    tertiary = Color(0xFF4A9EFF),
    onTertiary = Color(0xFF0A0A0C),
    background = Color(0xFF0A0A0C),              // Ground
    onBackground = Color(0xFFF5F5F5),           // TextPrimary
    surface = Color(0xFF12141A),                // Container
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF262A33),         // TrackOff (campos, divisórias)
    onSurfaceVariant = Color(0xFF8A93A3),       // labels/placeholder
    surfaceContainer = Color(0xFF12141A),
    surfaceContainerHigh = Color(0xFF14161C),   // container do AlertDialog
    surfaceContainerHighest = Color(0xFF1A1D24),
    error = Color(0xFFEF4444),                  // Danger
    onError = Color(0xFFF5F5F5),
    outline = Color(0xFF3A3F47),
    outlineVariant = Color(0xFF262A33)
)

@Composable
fun HavalShisukuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // App é dark-only e usa a paleta Impulse — ignora dynamicColor (Material You
    // sobrescreveria a marca) e o modo claro do sistema. Sempre o tema escuro da marca.
    MaterialTheme(
        colorScheme = ImpulseDarkColorScheme,
        typography = Typography,
        content = content
    )
}