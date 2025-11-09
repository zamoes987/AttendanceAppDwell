package com.attendancetracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark color scheme for the Attendance Tracker app.
 *
 * Uses Material 3 dark theme colors with custom primary/secondary colors.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = TextOnPrimary,

    secondary = Secondary,
    onSecondary = TextOnPrimary,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = TextOnPrimary,

    background = BackgroundDark,
    onBackground = Color(0xFFE0E0E0),

    surface = SurfaceDark,
    onSurface = Color(0xFFE0E0E0),

    error = ErrorRed,
    onError = TextOnPrimary
)

/**
 * Light color scheme for the Attendance Tracker app.
 *
 * Uses Material 3 light theme colors with custom primary/secondary colors.
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextOnPrimary,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF001D36),

    secondary = Secondary,
    onSecondary = TextOnPrimary,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF003300),

    background = BackgroundLight,
    onBackground = TextPrimary,

    surface = SurfaceLight,
    onSurface = TextPrimary,

    error = ErrorRed,
    onError = TextOnPrimary
)

/**
 * Main theme composable for the Attendance Tracker app.
 *
 * Applies Material 3 theming with custom colors and handles
 * system bar appearance based on dark/light theme.
 *
 * @param darkTheme Whether to use dark theme (defaults to system setting)
 * @param content The composable content to wrap with the theme
 */
@Composable
fun AttendanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
