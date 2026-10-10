package app.tgphotos.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.tgphotos.core.model.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFF6356D8), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E3FF), onPrimaryContainer = Color(0xFF211858),
    secondary = Color(0xFF526B62), secondaryContainer = Color(0xFFD6EBDF),
    background = Color(0xFFFAF8FF), surface = Color(0xFFFAF8FF),
    surfaceContainer = Color(0xFFF1EDF8), surfaceContainerLow = Color(0xFFF5F2FC),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFC8BEFF), onPrimary = Color(0xFF30246D),
    primaryContainer = Color(0xFF483A91), onPrimaryContainer = Color(0xFFEAE4FF),
    secondary = Color(0xFFAFCFC1), secondaryContainer = Color(0xFF344E43),
    background = Color(0xFF111119), surface = Color(0xFF111119),
    surfaceContainer = Color(0xFF22212D), surfaceContainerLow = Color(0xFF1B1A24),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TGPhotosTheme(mode: ThemeMode = ThemeMode.SYSTEM, dynamicColors: Boolean = true, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    val context = LocalContext.current
    val colors = if (dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) Dark else Light
    MaterialExpressiveTheme(colorScheme = colors,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content)
}
