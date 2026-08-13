package com.encryxed.tally.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F2DA),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4B635B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE9DE),
    onSecondaryContainer = Color(0xFF072019),
    tertiary = Color(0xFF3F6375),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFDF9),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFBFDF9),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDBE5DF),
    onSurfaceVariant = Color(0xFF3F4945),
    outline = Color(0xFF6F7975),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD6BF),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFFA7F2DA),
    secondary = Color(0xFFB2CCC2),
    onSecondary = Color(0xFF1D352E),
    secondaryContainer = Color(0xFF344C44),
    onSecondaryContainer = Color(0xFFCDE9DE),
    tertiary = Color(0xFFA7CBE0),
    onTertiary = Color(0xFF0A3446),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE1E3E0),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C4),
    outline = Color(0xFF899390),
)

@Composable
fun TallyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            // enableEdgeToEdge() owns the system bar colours; all that's left
            // is making the icons legible against our background.
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
