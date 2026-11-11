package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ResonixTheme(
    darkTheme: Boolean = true,
    primaryColor: Color = Color(0xFF00E5FF),
    secondaryColor: Color = Color(0xFF38BDF8),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = primaryColor,
            background = Color(0xFF04060E),
            surface = Color(0xFF0C1322),
            surfaceVariant = Color(0xFF131D33),
            outline = Color.White.copy(alpha = 0.12f),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.7f)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = primaryColor,
            background = Color(0xFFF1F5F9),
            surface = Color.White,
            surfaceVariant = Color(0xFFE2E8F0),
            outline = Color.Black.copy(alpha = 0.1f),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
