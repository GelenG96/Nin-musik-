package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Default Neon Cyan Theme
val ResonixCyan = Color(0xFF00E5FF)
val ResonixCyanLight = Color(0xFF38BDF8)
val ResonixCyanDark = Color(0xFF0284C7)

// Dynamic Accent Options
enum class AccentColorTheme(val displayName: String, val primaryColor: Color, val secondaryColor: Color) {
    CYAN("Neon Cyan", Color(0xFF00E5FF), Color(0xFF38BDF8)),
    VIOLET("Electric Violet", Color(0xFFA855F7), Color(0xFFC084FC)),
    AMBER("Solar Amber", Color(0xFFF59E0B), Color(0xFFFBBF24)),
    EMERALD("Emerald Glow", Color(0xFF10B981), Color(0xFF34D399)),
    CRIMSON("Hyper Crimson", Color(0xFFF43F5E), Color(0xFFFB7185))
}

// Dark Palette (Default)
val DarkBackground = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF131B2E)
val DarkCard = Color(0xFF1E293B)
val DarkCardBorder = Color(0xFF334155)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Light Palette
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFF1F5F9)
val LightCardBorder = Color(0xFFE2E8F0)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)
