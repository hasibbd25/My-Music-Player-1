package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF050B14),
    primaryContainer = Color(0xFF0E2838),
    onPrimaryContainer = Color(0xFFB2EBF2),
    secondary = ElectricPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D144D),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = HotMagenta,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    surfaceContainer = SurfaceElevated
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
