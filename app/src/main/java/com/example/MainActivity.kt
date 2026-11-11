package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemeGalleryScreen
import com.example.ui.theme.ResonixTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appPreferences by viewModel.appPreferences.collectAsStateWithLifecycle()
            val activeTheme by viewModel.activeTheme.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
            val tracks by viewModel.allTracks.collectAsStateWithLifecycle()
            val playlists by viewModel.playlists.collectAsStateWithLifecycle()
            val customEqPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()

            // Request runtime permissions
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val mediaGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    results[Manifest.permission.READ_MEDIA_AUDIO] == true
                } else {
                    results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }
                if (mediaGranted) {
                    viewModel.rescanMusic()
                }
            }

            LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    neededPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                    neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                neededPermissions.add(Manifest.permission.RECORD_AUDIO)

                permissionsLauncher.launch(neededPermissions.toTypedArray())
            }

            // System Back navigation handler
            BackHandler(enabled = currentScreen !is ScreenDestination.Library) {
                viewModel.navigateBack()
            }

            ResonixTheme(
                darkTheme = appPreferences.isDarkMode,
                primaryColor = activeTheme.primaryColor,
                secondaryColor = activeTheme.secondaryColor
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main Screen Routing
                        when (val screen = currentScreen) {
                            is ScreenDestination.Library -> {
                                LibraryScreen(
                                    tracks = tracks,
                                    playlists = playlists,
                                    activeTheme = activeTheme,
                                    currentlyPlayingTrackId = playbackState.currentTrack?.id,
                                    isPlaying = playbackState.isPlaying,
                                    onTrackClick = { track, queue ->
                                        viewModel.playTrack(track, queue)
                                    },
                                    onPlayAll = { trackList ->
                                        if (trackList.isNotEmpty()) {
                                            viewModel.playTrack(trackList.first(), trackList)
                                        }
                                    },
                                    onShuffleAll = { trackList ->
                                        if (trackList.isNotEmpty()) {
                                            val shuffled = trackList.shuffled()
                                            viewModel.playTrack(shuffled.first(), shuffled)
                                        }
                                    },
                                    onPlayNext = { trk -> viewModel.playNext(trk) },
                                    onAddToQueue = { trk -> viewModel.addToQueue(trk) },
                                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                    onSelectPlaylist = { pl ->
                                        viewModel.navigateTo(ScreenDestination.PlaylistDetail(pl))
                                    },
                                    onOpenThemeGallery = {
                                        viewModel.navigateTo(ScreenDestination.ThemeGalleryScreen)
                                    },
                                    onOpenSettings = {
                                        viewModel.navigateTo(ScreenDestination.Settings)
                                    },
                                    onOpenEqualizer = {
                                        viewModel.navigateTo(ScreenDestination.Equalizer)
                                    },
                                    onRescan = { viewModel.rescanMusic() },
                                    onSaveTags = { id, title, artist, album, genre ->
                                        viewModel.saveTags(id, title, artist, album, genre)
                                    },
                                    onAddToPlaylist = { plId, trkId ->
                                        viewModel.addTrackToPlaylist(plId, trkId)
                                    }
                                )
                            }

                            is ScreenDestination.NowPlaying -> {
                                NowPlayingScreen(
                                    state = playbackState,
                                    activeTheme = activeTheme,
                                    onTogglePlay = { viewModel.togglePlay() },
                                    onNext = { viewModel.next() },
                                    onPrev = { viewModel.prev() },
                                    onSeekTo = { pos -> viewModel.seekTo(pos) },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onCycleRepeat = { viewModel.cycleRepeat() },
                                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                    onOpenEqualizer = { viewModel.navigateTo(ScreenDestination.Equalizer) },
                                    onSetVisualizerStyle = { style -> viewModel.setVisualizerStyle(style) },
                                    onStartSleepTimer = { mins, finish -> viewModel.startSleepTimer(mins, finish) },
                                    onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                                    onSaveTags = { id, title, artist, album, genre ->
                                        viewModel.saveTags(id, title, artist, album, genre)
                                    },
                                    onQueueTrackClick = { idx -> viewModel.playQueueIndex(idx) },
                                    onRemoveFromQueue = { idx -> viewModel.removeFromQueue(idx) },
                                    onCollapse = { viewModel.navigateBack() }
                                )
                            }

                            is ScreenDestination.Equalizer -> {
                                EqualizerScreen(
                                    audioEffects = playbackState.audioEffects,
                                    customPresets = customEqPresets,
                                    activeTheme = activeTheme,
                                    onUpdateEffects = { cfg -> viewModel.updateAudioEffects(cfg) },
                                    onSaveCustomPreset = { name, gains, bass, virt ->
                                        viewModel.saveCustomEqPreset(name, gains, bass, virt)
                                    },
                                    onDeleteCustomPreset = { id -> viewModel.deleteCustomEqPreset(id) },
                                    onBack = { viewModel.navigateBack() }
                                )
                            }

                            is ScreenDestination.PlaylistDetail -> {
                                val customPlaylistTracks by androidx.compose.runtime.produceState(
                                    initialValue = emptyList<com.example.data.model.Track>(),
                                    key1 = screen.playlist.id,
                                    key2 = tracks
                                ) {
                                    if (!screen.playlist.isSmart && screen.playlist.id > 0) {
                                        viewModel.getPlaylistTracks(screen.playlist.id).collect {
                                            value = it
                                        }
                                    } else {
                                        value = emptyList()
                                    }
                                }

                                val playlistTracks = if (screen.playlist.isSmart) {
                                    when (screen.playlist.name) {
                                        "Favorites" -> tracks.filter { it.isFavorite }
                                        "Recently Played" -> tracks.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }
                                        "Recently Added" -> tracks.sortedByDescending { it.dateAdded }.take(50)
                                        else -> tracks
                                    }
                                } else if (screen.playlist.id < 0) {
                                    when {
                                        screen.playlist.name.startsWith("Album: ") -> tracks.filter { it.album == screen.playlist.name.removePrefix("Album: ") }
                                        screen.playlist.name.startsWith("Artist: ") -> tracks.filter { it.artist == screen.playlist.name.removePrefix("Artist: ") }
                                        screen.playlist.name.startsWith("Genre: ") -> tracks.filter { it.genre.equals(screen.playlist.name.removePrefix("Genre: "), ignoreCase = true) }
                                        screen.playlist.name.startsWith("Folder: ") -> tracks.filter { it.folderName == screen.playlist.name.removePrefix("Folder: ") }
                                        else -> tracks
                                    }
                                } else {
                                    customPlaylistTracks
                                }

                                PlaylistDetailScreen(
                                    playlist = screen.playlist,
                                    tracks = playlistTracks,
                                    activeTheme = activeTheme,
                                    currentlyPlayingTrackId = playbackState.currentTrack?.id,
                                    isPlaying = playbackState.isPlaying,
                                    onTrackClick = { trk, q -> viewModel.playTrack(trk, q) },
                                    onPlayAll = { list ->
                                        if (list.isNotEmpty()) viewModel.playTrack(list.first(), list)
                                    },
                                    onShuffleAll = { list ->
                                        if (list.isNotEmpty()) {
                                            val shuffled = list.shuffled()
                                            viewModel.playTrack(shuffled.first(), shuffled)
                                        }
                                    },
                                    onRemoveFromPlaylist = { plId, trkId ->
                                        viewModel.removeTrackFromPlaylist(plId, trkId)
                                    },
                                    onRenamePlaylist = { plId, newName ->
                                        viewModel.renamePlaylist(plId, newName)
                                    },
                                    onDeletePlaylist = { plId ->
                                        viewModel.deletePlaylist(plId)
                                    },
                                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                    onPlayNext = { trk -> viewModel.playNext(trk) },
                                    onAddToQueue = { trk -> viewModel.addToQueue(trk) },
                                    onEditTags = { trk ->
                                        viewModel.saveTags(trk.id, trk.title, trk.artist, trk.album, trk.genre)
                                    },
                                    onBack = { viewModel.navigateBack() }
                                )
                            }

                            is ScreenDestination.ThemeGalleryScreen -> {
                                ThemeGalleryScreen(
                                    currentTheme = activeTheme,
                                    onApplyTheme = { newTheme ->
                                        viewModel.applyTheme(newTheme)
                                    },
                                    onBack = { viewModel.navigateBack() }
                                )
                            }

                            is ScreenDestination.Settings -> {
                                SettingsScreen(
                                    activeTheme = activeTheme,
                                    isDarkMode = appPreferences.isDarkMode,
                                    onToggleDarkMode = { isDark -> viewModel.toggleDarkMode(isDark) },
                                    visualizerStyle = playbackState.visualizerStyle,
                                    onSelectVisualizerStyle = { style -> viewModel.setVisualizerStyle(style) },
                                    onOpenThemeGallery = {
                                        viewModel.navigateTo(ScreenDestination.ThemeGalleryScreen)
                                    },
                                    onRescanLibrary = { viewModel.rescanMusic() },
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                        }

                        // Floating MiniPlayer at bottom
                        AnimatedVisibility(
                            visible = playbackState.currentTrack != null && currentScreen !is ScreenDestination.NowPlaying,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            MiniPlayer(
                                currentTrack = playbackState.currentTrack,
                                isPlaying = playbackState.isPlaying,
                                currentPositionMs = playbackState.currentPositionMs,
                                durationMs = playbackState.durationMs,
                                onTogglePlay = { viewModel.togglePlay() },
                                onNext = { viewModel.next() },
                                onClick = { viewModel.navigateTo(ScreenDestination.NowPlaying) },
                                primaryColor = activeTheme.primaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}
