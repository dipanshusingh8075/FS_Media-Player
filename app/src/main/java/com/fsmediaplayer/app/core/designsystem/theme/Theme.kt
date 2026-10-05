package com.fsmediaplayer.app.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FSDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBackground,
    primaryContainer = SurfaceContainerHigh,
    onPrimaryContainer = ElectricCyan,
    secondary = CyberEmerald,
    onSecondary = ObsidianBackground,
    secondaryContainer = SurfaceContainer,
    onSecondaryContainer = CyberEmerald,
    tertiary = ElectricViolet,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = TextPrimary
)

@Composable
fun FSMediaPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = FSDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = ObsidianBackground.toArgb()
                it.navigationBarColor = ObsidianBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FSTypography,
        content = content
    )
}
