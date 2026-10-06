package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Premium Gaming Dark Palette
val DarkBgPrimary = Color(0xFF080C16)
val DarkBgSurface = Color(0xFF101726)
val DarkBgCard = Color(0xFF151E32)
val DarkBgElevated = Color(0xFF1B2640)

// Neon & Accent Colors
val ElectricBlue = Color(0xFF00E5FF)
val ElectricBlueDark = Color(0xFF0091EA)
val NeonPurple = Color(0xFFA855F7)
val NeonPurpleDark = Color(0xFF7E22CE)
val NeonPink = Color(0xFFFF2A85)
val NeonPinkDark = Color(0xFFE11D48)
val NeonEmerald = Color(0xFF10B981)
val NeonCoral = Color(0xFFEF4444)

// Reward Gold & Amber
val RewardGold = Color(0xFFFFB800)
val RewardGoldLight = Color(0xFFFFE066)
val RewardGoldDark = Color(0xFFD97706)

// Text Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Glass Borders & Overlays
val GlassBorderCyan = Color(0x4000E5FF)
val GlassBorderPurple = Color(0x40A855F7)
val GlassBorderGold = Color(0x40FFB800)
val GlassBorderSubtle = Color(0x1AFFFFFF)

// Gradients
val CyberGradient = Brush.horizontalGradient(
    colors = listOf(ElectricBlue, NeonPurple)
)

val RewardGoldGradient = Brush.horizontalGradient(
    colors = listOf(RewardGold, Color(0xFFFF7A00))
)

val CardGlowGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
)
