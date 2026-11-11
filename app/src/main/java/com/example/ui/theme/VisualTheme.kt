package com.example.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.R

enum class ThemePresetId {
    AURORA,
    ABSTRACT_FLOW,
    DEEP_SPACE,
    NEON_ATMOSPHERE,
    NATURE_MIST,
    AMOLED_DARK,
    MIDNIGHT,
    LIQUID_GLASS,
    MINIMAL_ART,
    DYNAMIC_GRADIENT,
    CUSTOM
}

data class VisualTheme(
    val id: ThemePresetId,
    val name: String,
    val description: String,
    val drawableRes: Int? = null,
    val gradientColors: List<Color>? = null,
    val primaryColor: Color,
    val secondaryColor: Color,
    val customUriString: String? = null
)

object ThemeGallery {
    val PRESET_THEMES = listOf(
        VisualTheme(
            id = ThemePresetId.AURORA,
            name = "Aurora Nights",
            description = "Cinematic polar green and violet aurora borealis over dark waters",
            drawableRes = R.drawable.bg_aurora,
            primaryColor = Color(0xFF00FFC8),
            secondaryColor = Color(0xFFA855F7)
        ),
        VisualTheme(
            id = ThemePresetId.ABSTRACT_FLOW,
            name = "Abstract Flow",
            description = "3D luxury iridescent silk ribbons with dark liquid reflections",
            drawableRes = R.drawable.bg_abstract_flow,
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFFC084FC)
        ),
        VisualTheme(
            id = ThemePresetId.DEEP_SPACE,
            name = "Deep Space",
            description = "Cosmic nebula with starlight glow and infinite starfields",
            drawableRes = R.drawable.bg_deep_space,
            primaryColor = Color(0xFF38BDF8),
            secondaryColor = Color(0xFFE879F9)
        ),
        VisualTheme(
            id = ThemePresetId.NEON_ATMOSPHERE,
            name = "Neon Atmosphere",
            description = "Cyberpunk street reflections with electric cyan & laser magenta",
            drawableRes = R.drawable.bg_neon_atmosphere,
            primaryColor = Color(0xFF00F0FF),
            secondaryColor = Color(0xFFFF007F)
        ),
        VisualTheme(
            id = ThemePresetId.NATURE_MIST,
            name = "Nature Mist",
            description = "Atmospheric alpine pine forest enveloped in midnight blue fog",
            drawableRes = R.drawable.bg_nature_mist,
            primaryColor = Color(0xFF34D399),
            secondaryColor = Color(0xFF38BDF8)
        ),
        VisualTheme(
            id = ThemePresetId.AMOLED_DARK,
            name = "AMOLED Obsidian",
            description = "Pure black aesthetics with subtle luxury geometric carbon texture",
            drawableRes = R.drawable.bg_amoled_dark,
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF94A3B8)
        ),
        VisualTheme(
            id = ThemePresetId.MIDNIGHT,
            name = "Midnight Horizon",
            description = "Deep velvet indigo nightfall with luminous crescent moon & mist",
            drawableRes = R.drawable.bg_midnight,
            primaryColor = Color(0xFF60A5FA),
            secondaryColor = Color(0xFF818CF8)
        ),
        VisualTheme(
            id = ThemePresetId.LIQUID_GLASS,
            name = "Liquid Glass",
            description = "Translucent frosted refraction with prism cyan and deep violet flow",
            drawableRes = R.drawable.bg_liquid_glass,
            primaryColor = Color(0xFF22D3EE),
            secondaryColor = Color(0xFFA78BFA)
        ),
        VisualTheme(
            id = ThemePresetId.MINIMAL_ART,
            name = "Minimal Dark Art",
            description = "Refined monochromatic charcoal and dark mineral architecture",
            drawableRes = R.drawable.bg_minimal_art,
            primaryColor = Color(0xFFE2E8F0),
            secondaryColor = Color(0xFF94A3B8)
        ),
        VisualTheme(
            id = ThemePresetId.DYNAMIC_GRADIENT,
            name = "Dynamic Gradient",
            description = "Modern multi-stop chromatic wave of ultraviolet and electric cyan",
            drawableRes = R.drawable.bg_dynamic_gradient,
            primaryColor = Color(0xFFF43F5E),
            secondaryColor = Color(0xFF818CF8)
        )
    )

    fun getThemeById(idString: String, customUri: String? = null): VisualTheme {
        if (idString == ThemePresetId.CUSTOM.name && !customUri.isNullOrEmpty()) {
            return VisualTheme(
                id = ThemePresetId.CUSTOM,
                name = "Custom Wallpaper",
                description = "Custom image from device gallery",
                primaryColor = Color(0xFF00E5FF),
                secondaryColor = Color(0xFF38BDF8),
                customUriString = customUri
            )
        }
        return PRESET_THEMES.find { it.id.name == idString } ?: PRESET_THEMES[0]
    }

    fun saveTheme(context: Context, themeId: String, customUri: String? = null) {
        val prefs = context.getSharedPreferences("resonix_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("active_theme_id", themeId)
            putString("custom_wallpaper_uri", customUri)
            apply()
        }
    }

    fun loadSavedTheme(context: Context): VisualTheme {
        val prefs = context.getSharedPreferences("resonix_theme_prefs", Context.MODE_PRIVATE)
        val themeId = prefs.getString("active_theme_id", ThemePresetId.AURORA.name) ?: ThemePresetId.AURORA.name
        val customUri = prefs.getString("custom_wallpaper_uri", null)
        return getThemeById(themeId, customUri)
    }
}
