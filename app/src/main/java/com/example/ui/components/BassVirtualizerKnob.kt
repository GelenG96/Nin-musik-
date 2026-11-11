package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BassVirtualizerKnob(
    value: Int, // 0 to 1000
    onValueChange: (Int) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    testTag: String = "rotary_knob"
) {
    val haptic = LocalHapticFeedback.current
    val percentage = (value / 10f).toInt() // 0 to 100%

    // Knob sweeps from 135 degrees to 405 degrees (total sweep 270 degrees)
    val startAngle = 135f
    val sweepMax = 270f
    val currentSweep = (value / 1000f) * sweepMax

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .testTag(testTag)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging vertically or horizontally modifies value
                        val delta = -dragAmount.y + dragAmount.x
                        val deltaVal = (delta * 4).toInt()
                        val newVal = (value + deltaVal).coerceIn(0, 1000)
                        if (newVal != value) {
                            if ((newVal / 100) != (value / 100)) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onValueChange(newVal)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.width / 2) - 10.dp.toPx()

                // Background track arc
                drawArc(
                    color = Color(0xFF1E293B),
                    startAngle = startAngle,
                    sweepAngle = sweepMax,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active glow progress arc
                if (currentSweep > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryColor, secondaryColor, primaryColor),
                            center = center
                        ),
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Glow indicator dot at the tip of the sweep
                    val tipAngleRad = (startAngle + currentSweep) * (PI / 180f).toFloat()
                    val dotX = center.x + cos(tipAngleRad) * radius
                    val dotY = center.y + sin(tipAngleRad) * radius

                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }

                // Inner knob dial body
                drawCircle(
                    color = Color(0xFF131B2E),
                    radius = radius - 8.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = radius - 8.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Percentage readout in center
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percentage%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (value > 0) "ON" else "OFF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (value > 0) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
