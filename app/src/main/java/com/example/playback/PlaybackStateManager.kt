package com.example.playback

import com.example.data.model.AudioEffectConfig
import com.example.data.model.EqualizerPresets
import com.example.data.model.LyricLine
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.example.data.model.VisualizerStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VisualizerData(
    val rawWaveform: ByteArray = ByteArray(128),
    val rawFft: ByteArray = ByteArray(128),
    val normalizedFft: FloatArray = FloatArray(32) { 0f },
    val peakAmplitude: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisualizerData
        return normalizedFft.contentEquals(other.normalizedFft) && peakAmplitude == other.peakAmplitude
    }

    override fun hashCode(): Int {
        var result = normalizedFft.contentHashCode()
        result = 31 * result + peakAmplitude.hashCode()
        return result
    }
}

data class PlaybackUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffle: Boolean = false,
    val audioEffects: AudioEffectConfig = AudioEffectConfig(),
    val visualizerData: VisualizerData = VisualizerData(),
    val visualizerStyle: VisualizerStyle = VisualizerStyle.SPECTRUM_BARS,
    val currentLyrics: List<LyricLine> = emptyList(),
    val activeLyricIndex: Int = -1,
    val sleepTimerRemainingSec: Long = 0L,
    val sleepTimerFinishSong: Boolean = false,
    val isSleepTimerActive: Boolean = false
)

object PlaybackStateManager {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    // Original un-shuffled queue backup
    private var unShuffledQueue: List<Track> = emptyList()

    fun updateCurrentTrack(track: Track?, durationMs: Long) {
        _state.update {
            it.copy(
                currentTrack = track,
                durationMs = durationMs,
                currentPositionMs = 0L
            )
        }
    }

    fun updatePlayingState(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying) }
    }

    fun updatePosition(positionMs: Long) {
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun setQueue(queue: List<Track>, index: Int) {
        unShuffledQueue = queue
        val effectiveQueue = if (_state.value.isShuffle) {
            val current = queue.getOrNull(index)
            val rest = queue.filterIndexed { i, _ -> i != index }.shuffled()
            if (current != null) listOf(current) + rest else queue.shuffled()
        } else {
            queue
        }
        val effectiveIndex = if (_state.value.isShuffle && index in queue.indices) 0 else index
        _state.update {
            it.copy(
                queue = effectiveQueue,
                queueIndex = effectiveIndex,
                currentTrack = effectiveQueue.getOrNull(effectiveIndex)
            )
        }
    }

    fun setQueueIndex(index: Int) {
        val track = _state.value.queue.getOrNull(index)
        _state.update {
            it.copy(
                queueIndex = index,
                currentTrack = track ?: it.currentTrack
            )
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffle
        val currentTrack = _state.value.currentTrack
        val currentQueue = if (unShuffledQueue.isNotEmpty()) unShuffledQueue else _state.value.queue

        if (newShuffle) {
            val rest = currentQueue.filter { it.id != currentTrack?.id }.shuffled()
            val newQueue = if (currentTrack != null) listOf(currentTrack) + rest else currentQueue.shuffled()
            _state.update {
                it.copy(
                    isShuffle = true,
                    queue = newQueue,
                    queueIndex = if (currentTrack != null) 0 else 0
                )
            }
        } else {
            val newIndex = currentQueue.indexOfFirst { it.id == currentTrack?.id }.coerceAtLeast(0)
            _state.update {
                it.copy(
                    isShuffle = false,
                    queue = currentQueue,
                    queueIndex = newIndex
                )
            }
        }
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = next) }
    }

    fun addToQueue(track: Track) {
        _state.update {
            val newQueue = it.queue + track
            it.copy(queue = newQueue)
        }
    }

    fun playNext(track: Track) {
        _state.update {
            val nextIndex = (it.queueIndex + 1).coerceAtMost(it.queue.size)
            val newQueue = it.queue.toMutableList()
            newQueue.add(nextIndex, track)
            it.copy(queue = newQueue)
        }
    }

    fun removeFromQueue(index: Int) {
        _state.update {
            if (index !in it.queue.indices) return@update it
            val newQueue = it.queue.toMutableList()
            newQueue.removeAt(index)
            val newIndex = when {
                index < it.queueIndex -> it.queueIndex - 1
                index == it.queueIndex -> if (newQueue.isEmpty()) -1 else it.queueIndex.coerceAtMost(newQueue.lastIndex)
                else -> it.queueIndex
            }
            it.copy(
                queue = newQueue,
                queueIndex = newIndex,
                currentTrack = newQueue.getOrNull(newIndex)
            )
        }
    }

    fun updateAudioEffects(config: AudioEffectConfig) {
        _state.update { it.copy(audioEffects = config) }
    }

    fun updateVisualizer(data: VisualizerData) {
        _state.update { it.copy(visualizerData = data) }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        _state.update { it.copy(visualizerStyle = style) }
    }

    fun setLyrics(lyrics: List<LyricLine>) {
        _state.update { it.copy(currentLyrics = lyrics, activeLyricIndex = -1) }
    }

    fun setActiveLyricIndex(index: Int) {
        if (_state.value.activeLyricIndex != index) {
            _state.update { it.copy(activeLyricIndex = index) }
        }
    }

    fun updateSleepTimer(remainingSec: Long, finishSong: Boolean, isActive: Boolean) {
        _state.update {
            it.copy(
                sleepTimerRemainingSec = remainingSec,
                sleepTimerFinishSong = finishSong,
                isSleepTimerActive = isActive
            )
        }
    }
}
