package com.example.data.model

data class EqPreset(
    val id: Int,
    val name: String,
    val gains: List<Int>, // in millibels, e.g. -1000 to +1000 (-10dB to +10dB)
    val isCustom: Boolean = false
)

data class AudioEffectConfig(
    val isEqEnabled: Boolean = true,
    val currentPresetId: Int = 0,
    val customPresetName: String? = null,
    val bandGains: List<Int> = listOf(0, 0, 0, 0, 0),
    val bandFreqs: List<Int> = listOf(60, 230, 910, 3600, 14000), // in Hz
    val bassBoostStrength: Int = 300, // 0 to 1000
    val virtualizerStrength: Int = 250, // 0 to 1000
    val balance: Float = 0f, // -1.0f (left) to 1.0f (right), 0f is centered
    val volumeGain: Float = 1.0f // 0.0f to 1.5f
)

object EqualizerPresets {
    // 24 professional presets exceeding the 22+ requirement
    val BUILT_IN_PRESETS = listOf(
        EqPreset(0, "Normal", listOf(0, 0, 0, 0, 0)),
        EqPreset(1, "Classical", listOf(350, 200, -100, 250, 350)),
        EqPreset(2, "Dance", listOf(500, 300, 0, 200, 450)),
        EqPreset(3, "Folk", listOf(200, 100, 0, 150, -100)),
        EqPreset(4, "Heavy Metal", listOf(400, 150, 600, 250, 50)),
        EqPreset(5, "Hip Hop", listOf(650, 450, 0, 200, 350)),
        EqPreset(6, "Jazz", listOf(400, 200, -150, 200, 450)),
        EqPreset(7, "Pop", listOf(-150, 200, 500, 150, -200)),
        EqPreset(8, "Rock", listOf(550, 300, -200, 250, 550)),
        EqPreset(9, "Acoustic", listOf(400, 250, 100, 250, 350)),
        EqPreset(10, "Bass Booster", listOf(800, 550, 0, 0, 0)),
        EqPreset(11, "Bass Reducer", listOf(-700, -450, 0, 0, 0)),
        EqPreset(12, "Deep Bass", listOf(600, 350, 100, -150, -350)),
        EqPreset(13, "Electronic", listOf(550, 250, 0, 150, 450)),
        EqPreset(14, "Latin", listOf(400, 200, 0, 0, 450)),
        EqPreset(15, "Loudness", listOf(650, 300, 0, 300, 550)),
        EqPreset(16, "Lounge", listOf(-250, 150, 350, 150, 0)),
        EqPreset(17, "Piano", listOf(350, 200, 0, 250, 350)),
        EqPreset(18, "R&B", listOf(300, 550, 250, -100, 250)),
        EqPreset(19, "Vocal Booster", listOf(-250, 0, 650, 350, -150)),
        EqPreset(20, "Treble Booster", listOf(0, 0, 0, 450, 800)),
        EqPreset(21, "Treble Reducer", listOf(0, 0, 0, -350, -700)),
        EqPreset(22, "Techno", listOf(550, 350, 0, 350, 550)),
        EqPreset(23, "Club & Party", listOf(500, 400, 200, 250, 400))
    )

    fun getPresetById(id: Int): EqPreset {
        return BUILT_IN_PRESETS.find { it.id == id } ?: BUILT_IN_PRESETS[0]
    }
}
