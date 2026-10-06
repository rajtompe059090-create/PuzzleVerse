package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkGamingColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color(0xFF04101A),
    primaryContainer = ElectricBlueDark,
    onPrimaryContainer = Color.White,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = NeonPurpleDark,
    onSecondaryContainer = Color.White,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = DarkBgPrimary,
    onBackground = TextPrimary,
    surface = DarkBgSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkBgCard,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderSubtle,
    error = NeonCoral,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Always enforce premium gaming aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkGamingColorScheme,
        typography = Typography,
        content = content
    )
}
