package app.paprashare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B3A6B),
    onPrimary = Color.White,
    secondary = Color(0xFF2E5AA8),
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF141414),
    surface = Color.White,
    onSurface = Color(0xFF141414),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FB8E4),
    onPrimary = Color(0xFF0A1B38),
    secondary = Color(0xFFBFD0F0),
    onSecondary = Color(0xFF15264A),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E6E6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun PapraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}