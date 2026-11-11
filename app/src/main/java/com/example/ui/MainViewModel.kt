package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AudioEffectConfig
import com.example.data.model.EqPreset
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.data.model.VisualizerStyle
import com.example.data.repository.MusicRepository
import com.example.playback.MusicPlaybackService
import com.example.playback.PlaybackStateManager
import com.example.playback.PlaybackUiState
import com.example.ui.theme.ThemeGallery
import com.example.ui.theme.VisualTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Library : ScreenDestination()
    object NowPlaying : ScreenDestination()
    object Equalizer : ScreenDestination()
    data class PlaylistDetail(val playlist: Playlist) : ScreenDestination()
    object Settings : ScreenDestination()
    object ThemeGalleryScreen : ScreenDestination()
}

data class AppPreferences(
    val isDarkMode: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Library)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _rawTracks = MutableStateFlow<List<Track>>(emptyList())

    val tracks: StateFlow<List<Track>> = repository.getAugmentedTracksFlow(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _augmentedTracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks: StateFlow<List<Track>> = _augmentedTracks.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customEqPresets: StateFlow<List<EqPreset>> = repository.getCustomEqPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackUiState> = PlaybackStateManager.state

    private val _appPreferences = MutableStateFlow(AppPreferences())
    val appPreferences: StateFlow<AppPreferences> = _appPreferences.asStateFlow()

    // Persistent Visual Theme (wallpapers + accents)
    private val _activeTheme = MutableStateFlow(ThemeGallery.loadSavedTheme(application))
    val activeTheme: StateFlow<VisualTheme> = _activeTheme.asStateFlow()

    init {
        rescanMusic()
    }

    fun rescanMusic() {
        viewModelScope.launch {
            val scanned = repository.scanLocalTracks()
            _rawTracks.value = scanned
            repository.getAugmentedTracksFlow(scanned).collect { augmented ->
                _augmentedTracks.value = augmented
            }
        }
    }

    fun applyTheme(theme: VisualTheme) {
        _activeTheme.value = theme
        ThemeGallery.saveTheme(
            context = getApplication(),
            themeId = theme.id.name,
            customUri = theme.customUriString
        )
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        return when (_currentScreen.value) {
            ScreenDestination.Library -> false
            else -> {
                _currentScreen.value = ScreenDestination.Library
                true
            }
        }
    }

    // Playback Controls
    fun playTrack(track: Track, queue: List<Track>) {
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        PlaybackStateManager.setQueue(queue, index)
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PLAY
        }
        startPlaybackService(context, intent)
    }

    fun togglePlay() {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_TOGGLE_PLAY
        }
        startPlaybackService(context, intent)
    }

    fun next() {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_NEXT
        }
        startPlaybackService(context, intent)
    }

    fun prev() {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PREV
        }
        startPlaybackService(context, intent)
    }

    fun seekTo(positionMs: Long) {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SEEK
            putExtra(MusicPlaybackService.EXTRA_POSITION_MS, positionMs)
        }
        startPlaybackService(context, intent)
    }

    fun toggleShuffle() {
        PlaybackStateManager.toggleShuffle()
    }

    fun cycleRepeat() {
        PlaybackStateManager.cycleRepeatMode()
    }

    fun playNext(track: Track) {
        PlaybackStateManager.playNext(track)
    }

    fun addToQueue(track: Track) {
        PlaybackStateManager.addToQueue(track)
    }

    fun removeFromQueue(index: Int) {
        PlaybackStateManager.removeFromQueue(index)
    }

    fun playQueueIndex(index: Int) {
        val queue = PlaybackStateManager.state.value.queue
        val track = queue.getOrNull(index) ?: return
        playTrack(track, queue)
    }

    // Favorites
    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(trackId)
        }
    }

    // Equalizer & Audio Effects
    fun updateAudioEffects(config: AudioEffectConfig) {
        PlaybackStateManager.updateAudioEffects(config)
        val context = getApplication<Application>()
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_APPLY_EFFECTS
        }
        startPlaybackService(context, intent)
    }

    fun saveCustomEqPreset(name: String, gains: List<Int>, bass: Int, virt: Int) {
        viewModelScope.launch {
            repository.saveCustomPreset(name, gains, bass, virt)
        }
    }

    fun deleteCustomEqPreset(presetId: Long) {
        viewModelScope.launch {
            repository.deleteCustomPreset(presetId)
        }
    }

    // Visualizer
    fun setVisualizerStyle(style: VisualizerStyle) {
        PlaybackStateManager.setVisualizerStyle(style)
    }

    // Playlists
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    // Tag Editor
    fun saveTags(trackId: Long, title: String, artist: String, album: String, genre: String) {
        viewModelScope.launch {
            repository.updateTrackTags(trackId, title, artist, album, genre)
        }
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int, finishSong: Boolean) {
        val totalSec = minutes * 60L
        PlaybackStateManager.updateSleepTimer(totalSec, finishSong, isActive = true)
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, MusicPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun cancelSleepTimer() {
        PlaybackStateManager.updateSleepTimer(0, finishSong = false, isActive = false)
    }

    fun toggleDarkMode(isDark: Boolean) {
        _appPreferences.update { it.copy(isDarkMode = isDark) }
    }

    private fun startPlaybackService(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
