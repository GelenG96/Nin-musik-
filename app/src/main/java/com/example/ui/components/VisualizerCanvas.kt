package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.VisualizerStyle
import com.example.playback.VisualizerData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualizerCanvas(
    visualizerData: VisualizerData,
    style: VisualizerStyle,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    // Ambient breathing animation when audio is idle or silent
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (style) {
            VisualizerStyle.SPECTRUM_BARS -> {
                drawSpectrumBars(
                    visualizerData = visualizerData,
                    isPlaying = isPlaying,
                    ambientPhase = ambientPhase,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    canvasWidth = width,
                    canvasHeight = height
                )
            }
            VisualizerStyle.FLUID_WAVEFORM -> {
                drawFluidWaveform(
                    visualizerData = visualizerData,
                    isPlaying = isPlaying,
                    ambientPhase = ambientPhase,
                    primaryColor = primaryColor,
                    canvasWidth = width,
                    canvasHeight = height
                )
            }
            VisualizerStyle.RADIAL_SPECTRUM -> {
                drawRadialSpectrum(
                    visualizerData = visualizerData,
                    isPlaying = isPlaying,
                    ambientPhase = ambientPhase,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    canvasWidth = width,
                    canvasHeight = height
                )
            }
            VisualizerStyle.PARTICLE_BEAT -> {
                drawParticleBeat(
                    visualizerData = visualizerData,
                    isPlaying = isPlaying,
                    ambientPhase = ambientPhase,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    canvasWidth = width,
                    canvasHeight = height
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpectrumBars(
    visualizerData: VisualizerData,
    isPlaying: Boolean,
    ambientPhase: Float,
    primaryColor: Color,
    secondaryColor: Color,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val barCount = 28
    val spacing = 4f
    val totalSpacing = spacing * (barCount - 1)
    val barWidth = (canvasWidth - totalSpacing) / barCount
    val maxBarHeight = canvasHeight * 0.85f

    val gradient = Brush.verticalGradient(
        colors = listOf(primaryColor, secondaryColor, primaryColor.copy(alpha = 0.3f)),
        startY = 0f,
        endY = canvasHeight
    )

    for (i in 0 until barCount) {
        val fftVal = visualizerData.normalizedFft.getOrElse(i) { 0f }
        val ambientVal = if (!isPlaying || fftVal < 0.05f) {
            (sin(ambientPhase + i * 0.3f) * 0.15f + 0.2f).coerceIn(0.05f, 0.4f)
        } else {
            fftVal
        }

        val barH = (ambientVal * maxBarHeight).coerceIn(8f, maxBarHeight)
        val x = i * (barWidth + spacing)
        val y = canvasHeight - barH

        // Bar body
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(x, y),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )

        // Peak floating cap
        val peakY = (y - 6f).coerceAtLeast(0f)
        drawCircle(
            color = primaryColor,
            radius = barWidth / 3,
            center = Offset(x + barWidth / 2, peakY)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFluidWaveform(
    visualizerData: VisualizerData,
    isPlaying: Boolean,
    ambientPhase: Float,
    primaryColor: Color,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val centerY = canvasHeight / 2
    val waveform = visualizerData.rawWaveform
    val path = Path()

    val points = 64
    val step = canvasWidth / (points - 1)

    for (i in 0 until points) {
        val raw = if (waveform.isNotEmpty()) {
            val sampleIdx = (i * waveform.size / points).coerceIn(0, waveform.lastIndex)
            val sample = (waveform[sampleIdx].toInt() and 0xFF) - 128
            sample / 128f
        } else {
            0f
        }

        val amplitude = if (isPlaying && raw != 0f) {
            raw * (canvasHeight * 0.4f)
        } else {
            sin(ambientPhase + i * 0.25f) * (canvasHeight * 0.15f)
        }

        val x = i * step
        val y = centerY + amplitude

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    // Glow line
    drawPath(
        path = path,
        color = primaryColor.copy(alpha = 0.3f),
        style = Stroke(width = 8f, cap = StrokeCap.Round)
    )
    drawPath(
        path = path,
        color = primaryColor,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadialSpectrum(
    visualizerData: VisualizerData,
    isPlaying: Boolean,
    ambientPhase: Float,
    primaryColor: Color,
    secondaryColor: Color,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val center = Offset(canvasWidth / 2, canvasHeight / 2)
    val baseRadius = (canvasWidth.coerceAtMost(canvasHeight) * 0.25f)
    val rayCount = 36
    val angleStep = (2 * PI / rayCount).toFloat()

    for (i in 0 until rayCount) {
        val angle = i * angleStep
        val fftVal = visualizerData.normalizedFft.getOrElse(i % visualizerData.normalizedFft.size) { 0f }
        val rayLength = if (isPlaying && fftVal > 0.05f) {
            fftVal * 70f
        } else {
            (sin(ambientPhase * 2 + i * 0.4f) * 15f + 20f)
        }

        val startX = center.x + cos(angle) * baseRadius
        val startY = center.y + sin(angle) * baseRadius
        val endX = center.x + cos(angle) * (baseRadius + rayLength)
        val endY = center.y + sin(angle) * (baseRadius + rayLength)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(primaryColor, secondaryColor),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }

    // Pulsing inner circle
    val pulse = visualizerData.peakAmplitude * 20f
    drawCircle(
        color = primaryColor.copy(alpha = 0.15f),
        radius = baseRadius - 8f + pulse,
        center = center
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawParticleBeat(
    visualizerData: VisualizerData,
    isPlaying: Boolean,
    ambientPhase: Float,
    primaryColor: Color,
    secondaryColor: Color,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val center = Offset(canvasWidth / 2, canvasHeight / 2)
    val bassPeak = (visualizerData.normalizedFft.take(4).maxOrNull() ?: 0.1f)
    val pulseScale = if (isPlaying) bassPeak * 35f else 10f

    // Core pulsing bass circle
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.1f), Color.Transparent),
            center = center,
            radius = 60f + pulseScale * 1.5f
        ),
        radius = 60f + pulseScale * 1.5f,
        center = center
    )

    // Orbiting particle rings
    val particles = 24
    for (i in 0 until particles) {
        val angle = (i * (2 * PI / particles) + ambientPhase * 0.5f).toFloat()
        val orbitDist = 90f + (i % 3) * 35f + pulseScale
        val px = center.x + cos(angle) * orbitDist
        val py = center.y + sin(angle) * orbitDist

        val pRadius = 4f + (i % 4) * 2f + bassPeak * 4f
        val color = if (i % 2 == 0) primaryColor else secondaryColor
        drawCircle(
            color = color.copy(alpha = 0.8f),
            radius = pRadius,
            center = Offset(px, py)
        )
    }
}
