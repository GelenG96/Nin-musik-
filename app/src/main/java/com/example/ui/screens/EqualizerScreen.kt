package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioEffectConfig
import com.example.data.model.EqPreset
import com.example.data.model.EqualizerPresets
import com.example.ui.components.BalanceAndVolumeSliders
import com.example.ui.components.BassVirtualizerKnob
import com.example.ui.components.ThemeBackgroundScaffold
import com.example.ui.theme.VisualTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    audioEffects: AudioEffectConfig,
    customPresets: List<EqPreset>,
    activeTheme: VisualTheme,
    onUpdateEffects: (AudioEffectConfig) -> Unit,
    onSaveCustomPreset: (name: String, gains: List<Int>, bass: Int, virt: Int) -> Unit,
    onDeleteCustomPreset: (presetId: Long) -> Unit,
    onBack: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val allPresets = EqualizerPresets.BUILT_IN_PRESETS + customPresets
    val primaryColor = activeTheme.primaryColor
    val secondaryColor = activeTheme.secondaryColor

    ThemeBackgroundScaffold(activeTheme = activeTheme, dimAmount = 0.78f) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Pro Studio DSP",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Parametric Equalizer & Bass Engine",
                                fontSize = 11.sp,
                                color = primaryColor
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("eq_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Reset to flat
                        IconButton(
                            onClick = {
                                onUpdateEffects(
                                    audioEffects.copy(
                                        bandGains = listOf(0, 0, 0, 0, 0),
                                        currentPresetId = 0,
                                        customPresetName = null
                                    )
                                )
                            },
                            modifier = Modifier.testTag("reset_eq_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset EQ",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // Save custom preset
                        IconButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.testTag("save_custom_preset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Save Custom Preset",
                                tint = primaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                // 1. Master DSP Switch with Glass Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (audioEffects.isEqEnabled) primaryColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (audioEffects.isEqEnabled) primaryColor else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "DSP Processing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (audioEffects.isEqEnabled) "Hardware AudioFX Active" else "Bypass (Raw Audio)",
                                fontSize = 12.sp,
                                color = if (audioEffects.isEqEnabled) primaryColor else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Switch(
                        checked = audioEffects.isEqEnabled,
                        onCheckedChange = { isChecked ->
                            onUpdateEffects(audioEffects.copy(isEqEnabled = isChecked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryColor,
                            checkedTrackColor = primaryColor.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("master_eq_switch")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Visual Frequency Response Curve Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    FrequencyCurveCanvas(
                        bandGains = audioEffects.bandGains,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Presets Carousel (24 presets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sound Profiles (${allPresets.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (preset in allPresets) {
                        val isSelected = audioEffects.currentPresetId == preset.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) primaryColor else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onUpdateEffects(
                                        audioEffects.copy(
                                            currentPresetId = preset.id,
                                            bandGains = preset.gains,
                                            customPresetName = if (preset.isCustom) preset.name else null
                                        )
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = preset.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else Color.White
                                )

                                if (preset.isCustom) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete preset",
                                        tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onDeleteCustomPreset(preset.id.toLong()) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. 5-Band Equalizer Sliders in Frosted Glass Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 5) {
                            val gain = audioEffects.bandGains.getOrElse(i) { 0 }
                            val label = labels[i]
                            val dbFormatted = String.format("%+.1f", gain / 100f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${dbFormatted}dB",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (audioEffects.isEqEnabled) primaryColor else Color.White.copy(alpha = 0.4f)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Slider(
                                    value = gain.toFloat(),
                                    onValueChange = { newVal ->
                                        if (audioEffects.isEqEnabled) {
                                            val newGains = audioEffects.bandGains.toMutableList()
                                            newGains[i] = newVal.toInt()
                                            onUpdateEffects(
                                                audioEffects.copy(
                                                    bandGains = newGains,
                                                    currentPresetId = -1
                                                )
                                            )
                                        }
                                    },
                                    valueRange = -1000f..1000f,
                                    enabled = audioEffects.isEqEnabled,
                                    modifier = Modifier
                                        .height(160.dp)
                                        .testTag("eq_band_slider_$i"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = primaryColor,
                                        activeTrackColor = primaryColor,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Bass Booster & 3D Virtualizer Rotary Dials in Glass Housing
                Text(
                    text = "Acoustic Dynamics & Soundstage",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bass Booster Knob
                    BassVirtualizerKnob(
                        value = audioEffects.bassBoostStrength,
                        onValueChange = { newVal ->
                            onUpdateEffects(audioEffects.copy(bassBoostStrength = newVal))
                        },
                        title = "BASS BOOSTER",
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        testTag = "bass_booster_knob"
                    )

                    // 3D Virtualizer Knob
                    BassVirtualizerKnob(
                        value = audioEffects.virtualizerStrength,
                        onValueChange = { newVal ->
                            onUpdateEffects(audioEffects.copy(virtualizerStrength = newVal))
                        },
                        title = "3D VIRTUALIZER",
                        primaryColor = secondaryColor,
                        secondaryColor = primaryColor,
                        testTag = "virtualizer_knob"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6. Sound Balance & Output Volume Gain
                BalanceAndVolumeSliders(
                    balance = audioEffects.balance,
                    volumeGain = audioEffects.volumeGain,
                    onBalanceChange = { newBalance ->
                        onUpdateEffects(audioEffects.copy(balance = newBalance))
                    },
                    onVolumeGainChange = { newGain ->
                        onUpdateEffects(audioEffects.copy(volumeGain = newGain))
                    },
                    primaryColor = primaryColor
                )

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(text = "Save Custom Preset", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        text = "Name your tailored equalizer setting:",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            onSaveCustomPreset(
                                newPresetName.trim(),
                                audioEffects.bandGains,
                                audioEffects.bassBoostStrength,
                                audioEffects.virtualizerStrength
                            )
                            newPresetName = ""
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131A2A)
        )
    }
}

@Composable
private fun FrequencyCurveCanvas(
    bandGains: List<Int>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Grid lines (0 dB, +10 dB, -10 dB)
        drawLine(
            color = Color.White.copy(alpha = 0.1f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, height * 0.1f),
            end = Offset(width, height * 0.1f),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(0f, height * 0.9f),
            end = Offset(width, height * 0.9f),
            strokeWidth = 1f
        )

        val points = mutableListOf<Offset>()
        val count = bandGains.size
        for (i in 0 until count) {
            val gain = bandGains[i] // -1000 to +1000
            val x = (i + 0.5f) * (width / count)
            // gain 0 is centerY; +1000 is height * 0.15f; -1000 is height * 0.85f
            val y = centerY - (gain / 1000f) * (centerY * 0.75f)
            points.add(Offset(x, y))
        }

        // Draw smooth spline curve
        val path = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            path.moveTo(0f, centerY)
            fillPath.moveTo(0f, height)
            fillPath.lineTo(0f, centerY)

            for (i in points.indices) {
                val pt = points[i]
                if (i == 0) {
                    path.lineTo(pt.x, pt.y)
                    fillPath.lineTo(pt.x, pt.y)
                } else {
                    val prev = points[i - 1]
                    val cx = (prev.x + pt.x) / 2
                    path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                    fillPath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                }
            }

            path.lineTo(width, centerY)
            fillPath.lineTo(width, centerY)
            fillPath.lineTo(width, height)
            fillPath.close()

            // Area fill under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Neon stroke curve
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor, primaryColor)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw glowing points
            for (pt in points) {
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}
