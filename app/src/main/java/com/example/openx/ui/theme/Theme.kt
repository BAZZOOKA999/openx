package com.example.openx.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OpenXColorScheme = darkColorScheme(
    primary = OpenXRed,
    onPrimary = OpenXTextPrimary,
    primaryContainer = OpenXRedContainer,
    onPrimaryContainer = OpenXRedLight,
    secondary = OpenXRedLight,
    onSecondary = OpenXBackground,
    secondaryContainer = OpenXSurfaceVariant,
    onSecondaryContainer = OpenXTextPrimary,
    tertiary = OpenXInfo,
    onTertiary = OpenXBackground,
    tertiaryContainer = OpenXInfoContainer,
    onTertiaryContainer = OpenXInfo,
    background = OpenXBackground,
    onBackground = OpenXTextPrimary,
    surface = OpenXSurface,
    onSurface = OpenXTextPrimary,
    surfaceVariant = OpenXSurfaceVariant,
    onSurfaceVariant = OpenXTextSecondary,
    outline = OpenXSurfaceBorder,
    outlineVariant = OpenXSurfaceBorder,
    error = OpenXError,
    onError = OpenXTextPrimary,
    errorContainer = OpenXErrorContainer,
    onErrorContainer = OpenXTextPrimary
)

@Composable
fun OpenXTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = OpenXBackground.toArgb()
                window.navigationBarColor = OpenXBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = OpenXColorScheme,
        typography = Typography,
        content = content
    )
}
