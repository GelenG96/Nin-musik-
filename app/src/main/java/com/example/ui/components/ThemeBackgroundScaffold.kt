package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.ui.theme.ThemePresetId
import com.example.ui.theme.VisualTheme

@Composable
fun ThemeBackgroundScaffold(
    activeTheme: VisualTheme,
    modifier: Modifier = Modifier,
    dimAmount: Float = 0.65f,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Wallpaper layer
        when {
            activeTheme.id == ThemePresetId.CUSTOM && activeTheme.customUriString != null -> {
                AsyncImage(
                    model = Uri.parse(activeTheme.customUriString),
                    contentDescription = "Custom Wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            activeTheme.drawableRes != null -> {
                Image(
                    painter = painterResource(id = activeTheme.drawableRes),
                    contentDescription = activeTheme.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            activeTheme.gradientColors != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = activeTheme.gradientColors
                            )
                        )
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF060913))
                )
            }
        }

        // 2. Frosted glass scrim & dark gradient overlay for optimal 2026 text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF04060E).copy(alpha = dimAmount * 0.7f),
                            Color(0xFF04060E).copy(alpha = dimAmount * 0.9f),
                            Color(0xFF020408).copy(alpha = (dimAmount + 0.2f).coerceAtMost(0.96f))
                        )
                    )
                )
        )

        // 3. Screen content
        content()
    }
}
