package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Resonix", appName)
  }

  @Test
  fun `playback state manager properly updates queue and current track`() {
    val uri = android.net.Uri.parse("content://media/external/audio/media/1")
    val track1 = com.example.data.model.Track(1L, "Song One", "Artist A", "Album A", 180000L, "/path/1.mp3", uri)
    val track2 = com.example.data.model.Track(2L, "Song Two", "Artist B", "Album B", 200000L, "/path/2.mp3", uri)
    val queue = listOf(track1, track2)

    com.example.playback.PlaybackStateManager.setQueue(queue, 0)
    assertEquals(track1.id, com.example.playback.PlaybackStateManager.state.value.currentTrack?.id)
    assertEquals(0, com.example.playback.PlaybackStateManager.state.value.queueIndex)

    com.example.playback.PlaybackStateManager.setQueueIndex(1)
    assertEquals(track2.id, com.example.playback.PlaybackStateManager.state.value.currentTrack?.id)
    assertEquals(1, com.example.playback.PlaybackStateManager.state.value.queueIndex)
  }
}
