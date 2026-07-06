package com.nabil.aireels.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = PrimaryPurple,
    secondary = AccentPink,
    background = SurfaceDark,
    surface = SurfaceDark,
    onPrimary = OnSurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight
)

@Composable
fun AIReelsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
