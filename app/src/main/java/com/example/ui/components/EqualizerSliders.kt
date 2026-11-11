package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EqualizerSliders(
    bandGains: List<Int>, // in millibels, e.g. -1000 to +1000
    bandFreqs: List<Int>, // in Hz, e.g. [60, 230, 910, 3600, 14000]
    onBandGainChange: (index: Int, gainMb: Int) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        // dB scale guide
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "+10 dB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "0 dB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "-10 dB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vertical/Horizontal 5-band slider row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 5) {
                val gain = bandGains.getOrElse(i) { 0 }
                val label = labels.getOrElse(i) { "${bandFreqs.getOrElse(i) { 0 }} Hz" }
                val dbFormatted = String.format("%+.1f dB", gain / 100f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dbFormatted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Slider
                    Slider(
                        value = gain.toFloat(),
                        onValueChange = { newVal ->
                            if (isEnabled) {
                                onBandGainChange(i, newVal.toInt())
                            }
                        },
                        valueRange = -1000f..1000f,
                        enabled = isEnabled,
                        modifier = Modifier
                            .height(160.dp)
                            .testTag("eq_band_slider_$i"),
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = Color(0xFF334155),
                            disabledThumbColor = Color.Gray,
                            disabledActiveTrackColor = Color.DarkGray
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceAndVolumeSliders(
    balance: Float, // -1.0f (left) to 1.0f (right)
    volumeGain: Float, // 0.0f to 1.5f
    onBalanceChange: (Float) -> Unit,
    onVolumeGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        // Sound Balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sound Balance (L - R)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            val balanceText = when {
                balance < -0.05f -> "Left ${(balance * -100).toInt()}%"
                balance > 0.05f -> "Right ${(balance * 100).toInt()}%"
                else -> "Center"
            }
            Text(
                text = balanceText,
                fontSize = 12.sp,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = balance,
            onValueChange = onBalanceChange,
            valueRange = -1.0f..1.0f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("balance_slider"),
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = Color(0xFF334155)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pre-amp / Output Volume Gain
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Output Gain",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${(volumeGain * 100).toInt()}%",
                fontSize = 12.sp,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = volumeGain,
            onValueChange = onVolumeGainChange,
            valueRange = 0.0f..1.5f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("volume_gain_slider"),
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = Color(0xFF334155)
            )
        )
    }
}
