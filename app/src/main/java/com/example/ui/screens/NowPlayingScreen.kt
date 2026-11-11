package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.RepeatMode
import com.example.data.model.VisualizerStyle
import com.example.playback.PlaybackUiState
import com.example.ui.components.QueueBottomSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SyncedLyricsView
import com.example.ui.components.TagEditorDialog
import com.example.ui.components.ThemeBackgroundScaffold
import com.example.ui.components.VisualizerCanvas
import com.example.ui.theme.VisualTheme

enum class PlayerViewTab {
    ARTWORK,
    VISUALIZER,
    LYRICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlaybackUiState,
    activeTheme: VisualTheme,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onOpenEqualizer: () -> Unit,
    onSetVisualizerStyle: (VisualizerStyle) -> Unit,
    onStartSleepTimer: (minutes: Int, finishSong: Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSaveTags: (trackId: Long, title: String, artist: String, album: String, genre: String) -> Unit,
    onQueueTrackClick: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onCollapse: () -> Unit
) {
    val track = state.currentTrack ?: return

    var currentTab by remember { mutableStateOf(PlayerViewTab.ARTWORK) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showTagEditorDialog by remember { mutableStateOf(false) }

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isUserSeeking) seekPositionMs.toLong() else state.currentPositionMs
    val duration = state.durationMs.coerceAtLeast(1L)

    val primaryColor = activeTheme.primaryColor
    val secondaryColor = activeTheme.secondaryColor

    ThemeBackgroundScaffold(activeTheme = activeTheme, dimAmount = 0.72f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header (Collapse, Source Album info, Queue button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .testTag("collapse_now_playing_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = track.album,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }

                Row {
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (state.isSleepTimerActive) primaryColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                            .testTag("now_playing_sleep_timer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = if (state.isSleepTimerActive) primaryColor else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("now_playing_queue_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. View Tab Switcher (Artwork, Visualizer, Lyrics) in floating frosted capsule
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(
                    Triple(PlayerViewTab.ARTWORK, Icons.Default.MusicNote, "Artwork"),
                    Triple(PlayerViewTab.VISUALIZER, Icons.Default.GraphicEq, "Visualizer"),
                    Triple(PlayerViewTab.LYRICS, Icons.Default.Subtitles, "Lyrics")
                ).forEach { (tab, icon, label) ->
                    val isSelected = currentTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) primaryColor else Color.Transparent
                            )
                            .clickable { currentTab = tab }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // 3. Center Hero Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "centerStage"
                ) { targetTab ->
                    when (targetTab) {
                        PlayerViewTab.ARTWORK -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                // Ambient blurred glow behind artwork
                                AsyncImage(
                                    model = track.albumArtUri ?: R.drawable.ic_default_album,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize(0.95f)
                                        .blur(40.dp),
                                    contentScale = ContentScale.Crop
                                )

                                // Foreground Hero Artwork Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(28.dp))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                                        .shadow(32.dp, shape = RoundedCornerShape(28.dp), spotColor = primaryColor)
                                ) {
                                    AsyncImage(
                                        model = track.albumArtUri ?: R.drawable.ic_default_album,
                                        contentDescription = "Cover Art",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        PlayerViewTab.VISUALIZER -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Visualizer style chips
                                Row(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    VisualizerStyle.values().forEach { style ->
                                        val isStyleSelected = state.visualizerStyle == style
                                        Text(
                                            text = style.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isStyleSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isStyleSelected) primaryColor else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isStyleSelected) primaryColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)
                                                )
                                                .border(
                                                    width = if (isStyleSelected) 1.dp else 0.dp,
                                                    color = if (isStyleSelected) primaryColor.copy(alpha = 0.5f) else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { onSetVisualizerStyle(style) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                VisualizerCanvas(
                                    visualizerData = state.visualizerData,
                                    style = state.visualizerStyle,
                                    isPlaying = state.isPlaying,
                                    primaryColor = primaryColor,
                                    secondaryColor = secondaryColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        PlayerViewTab.LYRICS -> {
                            SyncedLyricsView(
                                lyrics = state.currentLyrics,
                                activeLyricIndex = state.activeLyricIndex,
                                onSeekToTimestamp = onSeekTo,
                                primaryColor = primaryColor
                            )
                        }
                    }
                }
            }

            // 4. Song Metadata & Quick Action Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = track.artist,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showTagEditorDialog = true },
                        modifier = Modifier.testTag("edit_tags_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Tags",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(track.id) },
                        modifier = Modifier.testTag("favorite_button")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) primaryColor else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Modern 2026 Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = {
                        isUserSeeking = true
                        seekPositionMs = it
                    },
                    onValueChangeFinished = {
                        isUserSeeking = false
                        onSeekTo(seekPositionMs.toLong())
                    },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_seekbar"),
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val posSec = (currentPosition / 1000).coerceAtLeast(0)
                    val durSec = (duration / 1000).coerceAtLeast(0)
                    val posFormatted = String.format("%02d:%02d", posSec / 60, posSec % 60)
                    val durFormatted = String.format("%02d:%02d", durSec / 60, durSec % 60)

                    Text(
                        text = posFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = durFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Minimalist 2026 Playback Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button with Active indicator pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.isShuffle) primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable(onClick = onToggleShuffle)
                        .padding(8.dp)
                        .testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffle) primaryColor else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Play / Pause Capsule Glow Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(20.dp, shape = CircleShape, spotColor = primaryColor)
                        .clip(CircleShape)
                        .background(primaryColor)
                        .clickable(onClick = onTogglePlay)
                        .testTag("main_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Repeat Mode Button with Status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.repeatMode != RepeatMode.OFF) primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable(onClick = onCycleRepeat)
                        .padding(8.dp)
                        .testTag("repeat_button")
                ) {
                    val icon = when (state.repeatMode) {
                        RepeatMode.OFF, RepeatMode.ALL -> Icons.Default.Repeat
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) primaryColor else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Bottom Floating Bar for Equalizer & Bass Booster
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenEqualizer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Professional Studio Equalizer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (state.audioEffects.isEqEnabled) "Bass Boost ${state.audioEffects.bassBoostStrength / 10}% • 3D Virtualizer" else "DSP Disabled",
                            fontSize = 11.sp,
                            color = if (state.audioEffects.isEqEnabled) primaryColor else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.audioEffects.isEqEnabled) primaryColor else Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.audioEffects.isEqEnabled) "ACTIVE" else "BYPASS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (state.audioEffects.isEqEnabled) Color.Black else Color.White
                    )
                }
            }
        }
    }

    // Sheets & Dialogs
    if (showQueueSheet) {
        QueueBottomSheet(
            queue = state.queue,
            queueIndex = state.queueIndex,
            onTrackClick = { idx ->
                onQueueTrackClick(idx)
                showQueueSheet = false
            },
            onRemoveFromQueue = onRemoveFromQueue,
            onDismiss = { showQueueSheet = false },
            primaryColor = primaryColor
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            isTimerActive = state.isSleepTimerActive,
            remainingSeconds = state.sleepTimerRemainingSec,
            onStartTimer = onStartSleepTimer,
            onCancelTimer = onCancelSleepTimer,
            onDismiss = { showSleepTimerDialog = false },
            primaryColor = primaryColor
        )
    }

    if (showTagEditorDialog) {
        TagEditorDialog(
            track = track,
            onSave = { newTitle, newArtist, newAlbum, newGenre ->
                onSaveTags(track.id, newTitle, newArtist, newAlbum, newGenre)
                showTagEditorDialog = false
            },
            onDismiss = { showTagEditorDialog = false },
            primaryColor = primaryColor
        )
    }
}
