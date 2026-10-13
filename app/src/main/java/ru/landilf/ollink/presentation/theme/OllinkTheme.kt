package ru.landilf.ollink.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF171717), onPrimary = Color.White,
    background = Color.White, onBackground = Color(0xFF171717),
    surface = Color.White, onSurface = Color(0xFF171717),
    surfaceContainer = Color(0xFFF4F4F4), surfaceContainerHigh = Color(0xFFF4F4F4),
    onSurfaceVariant = Color(0xFF707070), outlineVariant = Color(0xFFE5E5E5)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFECECEC), onPrimary = Color(0xFF212121),
    background = Color(0xFF212121), onBackground = Color(0xFFECECEC),
    surface = Color(0xFF212121), onSurface = Color(0xFFECECEC),
    surfaceContainer = Color(0xFF2F2F2F), surfaceContainerHigh = Color(0xFF2F2F2F),
    onSurfaceVariant = Color(0xFFAAAAAA), outlineVariant = Color(0xFF404040)
)

@Composable
fun OllinkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors) {
        // MaterialTheme does not assign the content color for a bare root layout.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
