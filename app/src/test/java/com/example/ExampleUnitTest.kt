package com.example

import com.example.data.model.AudioEffectConfig
import com.example.data.model.EqualizerPresets
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.example.playback.PlaybackStateManager
import com.example.ui.theme.ThemeGallery
import com.example.util.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `theme gallery contains all 10 curated presets with wallpaper assets`() {
        val themes = ThemeGallery.PRESET_THEMES
        assertEquals(10, themes.size)
        themes.forEach { theme ->
            assertNotNull("Theme ${theme.name} must have drawableRes", theme.drawableRes)
            assertTrue("Theme ${theme.name} drawableRes must be valid resource ID", theme.drawableRes!! > 0)
        }
    }

    @Test
    fun `equalizer presets exceeds 22 presets`() {
        val presets = EqualizerPresets.BUILT_IN_PRESETS
        assertTrue("Equalizer must provide at least 22 presets, found ${presets.size}", presets.size >= 22)
    }

    @Test
    fun `lrc parser accurately parses timestamps and synchronized lyrics`() {
        val lrcSample = """
            [00:01.50]Line 1 of song
            [00:05.00]Line 2 of song
            [00:10.25]Line 3 chorus
        """.trimIndent()

        val parsed = LrcParser.parse(lrcSample)
        assertEquals(3, parsed.size)
        assertEquals(1500L, parsed[0].timestampMs)
        assertEquals("Line 1 of song", parsed[0].text)
        assertEquals(5000L, parsed[1].timestampMs)
        assertEquals(10250L, parsed[2].timestampMs)

        // Test finding active index
        assertEquals(0, LrcParser.findActiveLyricIndex(parsed, 2000L))
        assertEquals(1, LrcParser.findActiveLyricIndex(parsed, 7000L))
        assertEquals(2, LrcParser.findActiveLyricIndex(parsed, 12000L))
    }
}

