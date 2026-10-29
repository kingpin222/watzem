package com.watzem.nativeapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CinemaAmber,
    onPrimary = CinemaBackground,
    primaryContainer = CinemaAmberDark,
    onPrimaryContainer = TextPrimary,
    secondary = CinemaAmberLight,
    onSecondary = CinemaBackground,
    background = CinemaBackground,
    onBackground = TextPrimary,
    surface = CinemaSurface,
    onSurface = TextPrimary,
    surfaceVariant = CinemaCard,
    onSurfaceVariant = TextSecondary,
    outline = CinemaCardBorder,
    error = CinemaRed,
    onError = TextPrimary
)

@Composable
fun WatzemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Watzem is designed as an immersive cinema dark theme
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

