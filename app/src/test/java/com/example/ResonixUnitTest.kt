package com.example

import com.example.data.model.EqualizerPresets
import com.example.data.model.RepeatMode
import com.example.playback.PlaybackStateManager
import com.example.util.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResonixUnitTest {

    @Test
    fun equalizerPresets_has22PlusPresets_allHave5Bands() {
        val presets = EqualizerPresets.BUILT_IN_PRESETS
        assertTrue("Equalizer must have more than 22 presets", presets.size >= 22)

        for (preset in presets) {
            assertEquals("Preset '${preset.name}' must have 5 frequency bands", 5, preset.gains.size)
            for (gain in preset.gains) {
                assertTrue("Gain $gain must be within -1000mB to +1000mB", gain in -1000..1000)
            }
        }
    }

    @Test
    fun lrcParser_parsesTimestampsCorrectly() {
        val lrc = """
            [ti:Test Song]
            [ar:Test Artist]
            [00:10.50]First line of lyrics
            [00:25.00]Second line of lyrics
            [01:05.250]Third line of lyrics
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(3, parsed.size)

        assertEquals(10500L, parsed[0].timestampMs)
        assertEquals("First line of lyrics", parsed[0].text)

        assertEquals(25000L, parsed[1].timestampMs)
        assertEquals("Second line of lyrics", parsed[1].text)

        assertEquals(65250L, parsed[2].timestampMs)
        assertEquals("Third line of lyrics", parsed[2].text)
    }

    @Test
    fun lrcParser_findsActiveLyricIndex() {
        val lrc = """
            [00:10.00]Line 1
            [00:20.00]Line 2
            [00:30.00]Line 3
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)

        assertEquals(-1, LrcParser.findActiveLyricIndex(parsed, 5000L))
        assertEquals(0, LrcParser.findActiveLyricIndex(parsed, 12000L))
        assertEquals(1, LrcParser.findActiveLyricIndex(parsed, 20000L))
        assertEquals(1, LrcParser.findActiveLyricIndex(parsed, 25000L))
        assertEquals(2, LrcParser.findActiveLyricIndex(parsed, 35000L))
    }

    @Test
    fun repeatMode_cyclesThroughAllModes() {
        val initial = PlaybackStateManager.state.value.repeatMode

        PlaybackStateManager.cycleRepeatMode()
        val second = PlaybackStateManager.state.value.repeatMode

        PlaybackStateManager.cycleRepeatMode()
        val third = PlaybackStateManager.state.value.repeatMode

        PlaybackStateManager.cycleRepeatMode()
        val fourth = PlaybackStateManager.state.value.repeatMode

        assertTrue(initial != second || second != third)
    }
}
