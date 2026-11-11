package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Folder
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.TagEditorDialog
import com.example.ui.components.ThemeBackgroundScaffold
import com.example.ui.theme.VisualTheme

enum class SortOption(val displayName: String) {
    TITLE("Song Title (A-Z)"),
    ARTIST("Artist Name (A-Z)"),
    DATE_ADDED("Recently Added"),
    DURATION("Longest Duration")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    playlists: List<Playlist>,
    activeTheme: VisualTheme,
    currentlyPlayingTrackId: Long?,
    isPlaying: Boolean,
    onTrackClick: (track: Track, queue: List<Track>) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onOpenThemeGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRescan: () -> Unit,
    onSaveTags: (trackId: Long, title: String, artist: String, album: String, genre: String) -> Unit,
    onAddToPlaylist: (playlistId: Long, trackId: Long) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(SortOption.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    var editingTrack by remember { mutableStateOf<Track?>(null) }
    var showAddToPlaylistDialogForTrack by remember { mutableStateOf<Track?>(null) }

    val tabs = listOf("Home", "Tracks", "Playlists", "Albums", "Artists", "Folders")
    val primaryColor = activeTheme.primaryColor
    val secondaryColor = activeTheme.secondaryColor

    // Filtered & Sorted Tracks
    val filteredTracks by remember(tracks, searchQuery, currentSort) {
        derivedStateOf {
            val queried = if (searchQuery.isBlank()) {
                tracks
            } else {
                tracks.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
                }
            }

            when (currentSort) {
                SortOption.TITLE -> queried.sortedBy { it.title.lowercase() }
                SortOption.ARTIST -> queried.sortedBy { it.artist.lowercase() }
                SortOption.DATE_ADDED -> queried.sortedByDescending { it.dateAdded }
                SortOption.DURATION -> queried.sortedByDescending { it.durationMs }
            }
        }
    }

    val albums by remember(tracks) {
        derivedStateOf {
            tracks.groupBy { it.album }.map { (albumName, albumTracks) ->
                val first = albumTracks.first()
                Album(
                    id = first.id,
                    name = albumName,
                    artist = first.artist,
                    trackCount = albumTracks.size,
                    albumArtUri = first.albumArtUri
                )
            }.sortedBy { it.name.lowercase() }
        }
    }

    val artists by remember(tracks) {
        derivedStateOf {
            tracks.groupBy { it.artist }.map { (artistName, artistTracks) ->
                Artist(
                    id = artistTracks.first().id,
                    name = artistName,
                    trackCount = artistTracks.size,
                    albumCount = artistTracks.map { it.album }.distinct().size
                )
            }.sortedBy { it.name.lowercase() }
        }
    }

    val folders by remember(tracks) {
        derivedStateOf {
            tracks.groupBy { it.folderPath }.map { (path, folderTracks) ->
                Folder(
                    name = folderTracks.first().folderName,
                    path = path,
                    trackCount = folderTracks.size
                )
            }.sortedBy { it.name.lowercase() }
        }
    }

    ThemeBackgroundScaffold(activeTheme = activeTheme, dimAmount = 0.65f) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Minimalist 2026 Header
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search songs, artists, albums...", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (searchQuery.isEmpty()) isSearchActive = false else searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.White)
                                }
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(primaryColor, secondaryColor))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "RESONIX",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }, modifier = Modifier.testTag("open_search_button")) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                    }

                    // Theme Gallery shortcut
                    IconButton(onClick = onOpenThemeGallery, modifier = Modifier.testTag("open_theme_gallery_btn")) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme Gallery", tint = primaryColor)
                    }

                    // Equalizer shortcut
                    IconButton(onClick = onOpenEqualizer, modifier = Modifier.testTag("library_eq_button")) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Equalizer", tint = Color.White)
                    }

                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }, modifier = Modifier.testTag("sort_menu_button")) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            for (option in SortOption.entries) {
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        currentSort = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Settings
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("library_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Sleek Scrollable Tab Navigation Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = primaryColor,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, tabTitle ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = tabTitle,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Tab Switcher
            when (selectedTabIndex) {
                0 -> {
                    // 2026 CURATED BERANDA / HOME TAB
                    CuratedHomeTab(
                        tracks = tracks,
                        playlists = playlists,
                        albums = albums,
                        currentlyPlayingTrackId = currentlyPlayingTrackId,
                        isPlaying = isPlaying,
                        onTrackClick = { trk -> onTrackClick(trk, tracks) },
                        onPlayAll = { onPlayAll(tracks) },
                        onShuffleAll = { onShuffleAll(tracks) },
                        onToggleFavorite = onToggleFavorite,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onSelectPlaylist = onSelectPlaylist,
                        onRescan = onRescan,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                1 -> {
                    // TRACKS TAB
                    TracksTabContent(
                        tracks = filteredTracks,
                        currentlyPlayingTrackId = currentlyPlayingTrackId,
                        isPlaying = isPlaying,
                        onTrackClick = { track -> onTrackClick(track, filteredTracks) },
                        onPlayAll = { onPlayAll(filteredTracks) },
                        onShuffleAll = { onShuffleAll(filteredTracks) },
                        onToggleFavorite = onToggleFavorite,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = { showAddToPlaylistDialogForTrack = it },
                        onEditTags = { editingTrack = it },
                        onRescan = onRescan,
                        primaryColor = primaryColor
                    )
                }
                2 -> {
                    // PLAYLISTS TAB
                    PlaylistsTabContent(
                        tracks = tracks,
                        playlists = playlists,
                        onCreatePlaylist = { showCreatePlaylistDialog = true },
                        onSelectPlaylist = onSelectPlaylist,
                        onSelectSmartPlaylist = { title, playlistTracks ->
                            onSelectPlaylist(
                                Playlist(
                                    id = -1,
                                    name = title,
                                    trackCount = playlistTracks.size,
                                    isSmart = true
                                )
                            )
                        },
                        primaryColor = primaryColor
                    )
                }
                3 -> {
                    // ALBUMS TAB
                    AlbumsTabContent(
                        albums = albums,
                        onAlbumClick = { album ->
                            val albumTracks = tracks.filter { it.album == album.name }
                            onPlayAll(albumTracks)
                        },
                        primaryColor = primaryColor
                    )
                }
                4 -> {
                    // ARTISTS TAB
                    ArtistsTabContent(
                        artists = artists,
                        onArtistClick = { artist ->
                            val artistTracks = tracks.filter { it.artist == artist.name }
                            onPlayAll(artistTracks)
                        },
                        primaryColor = primaryColor
                    )
                }
                5 -> {
                    // FOLDERS TAB
                    FoldersTabContent(
                        folders = folders,
                        onFolderClick = { folder ->
                            val folderTracks = tracks.filter { it.folderPath == folder.path }
                            onPlayAll(folderTracks)
                        },
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131A2A)
        )
    }

    editingTrack?.let { trk ->
        TagEditorDialog(
            track = trk,
            onSave = { title, artist, album, genre ->
                onSaveTags(trk.id, title, artist, album, genre)
                editingTrack = null
            },
            onDismiss = { editingTrack = null },
            primaryColor = primaryColor
        )
    }

    showAddToPlaylistDialogForTrack?.let { trk ->
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialogForTrack = null },
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No custom playlists yet. Create one first!", color = Color.White.copy(alpha = 0.7f))
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(playlists) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddToPlaylist(pl.id, trk.id)
                                        showAddToPlaylistDialogForTrack = null
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = primaryColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(pl.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialogForTrack = null }) {
                    Text("Close", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF131A2A)
        )
    }
}

@Composable
private fun CuratedHomeTab(
    tracks: List<Track>,
    playlists: List<Playlist>,
    albums: List<Album>,
    currentlyPlayingTrackId: Long?,
    isPlaying: Boolean,
    onTrackClick: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onRescan: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color
) {
    val favorites = remember(tracks) { tracks.filter { it.isFavorite } }
    val recentlyPlayed = remember(tracks) { tracks.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed } }
    val recentlyAdded = remember(tracks) { tracks.sortedByDescending { it.dateAdded }.take(20) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Hero Music Collection Overview Glass Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.22f),
                                Color(0xFF131C31).copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hi-Fi Local Collection",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${tracks.size} Tracks • ${albums.size} Albums",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Row {
                            Button(
                                onClick = onPlayAll,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = CircleShape,
                                modifier = Modifier.size(46.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play all", tint = Color.Black, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onShuffleAll,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                shape = CircleShape,
                                modifier = Modifier.size(46.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle all", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2. Favorites Carousel (if any)
        if (favorites.isNotEmpty()) {
            item {
                SectionCarouselHeader(title = "Your Favorites", count = favorites.size)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favorites) { track ->
                        HorizontalTrackCard(
                            track = track,
                            isCurrent = track.id == currentlyPlayingTrackId,
                            isPlaying = isPlaying,
                            onClick = { onTrackClick(track) },
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }

        // 3. Recently Played Carousel (if any)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionCarouselHeader(title = "Recently Played", count = recentlyPlayed.size)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentlyPlayed) { track ->
                        HorizontalTrackCard(
                            track = track,
                            isCurrent = track.id == currentlyPlayingTrackId,
                            isPlaying = isPlaying,
                            onClick = { onTrackClick(track) },
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }

        // 4. Recently Added
        if (recentlyAdded.isNotEmpty()) {
            item {
                SectionCarouselHeader(title = "Recently Added", count = recentlyAdded.size)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentlyAdded) { track ->
                        HorizontalTrackCard(
                            track = track,
                            isCurrent = track.id == currentlyPlayingTrackId,
                            isPlaying = isPlaying,
                            onClick = { onTrackClick(track) },
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }

        // 5. Featured Albums
        if (albums.isNotEmpty()) {
            item {
                SectionCarouselHeader(title = "Albums", count = albums.size)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(albums.take(8)) { album ->
                        Column(
                            modifier = Modifier
                                .width(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .clickable {
                                    val albumTracks = tracks.filter { it.album == album.name }
                                    if (albumTracks.isNotEmpty()) onTrackClick(albumTracks.first())
                                }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = album.albumArtUri ?: R.drawable.ic_default_album,
                                    contentDescription = album.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = album.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = album.artist,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Bottom space for mini player
        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
private fun SectionCarouselHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun HorizontalTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    primaryColor: Color
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrent) primaryColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = 1.dp,
                color = if (isCurrent) primaryColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = track.albumArtUri ?: R.drawable.ic_default_album,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Playing",
                        tint = primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrent) primaryColor else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = track.artist,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TracksTabContent(
    tracks: List<Track>,
    currentlyPlayingTrackId: Long?,
    isPlaying: Boolean,
    onTrackClick: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onEditTags: (Track) -> Unit,
    onRescan: () -> Unit,
    primaryColor: Color
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = primaryColor.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Local Music Found",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ensure audio files are present on your device storage.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRescan,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Device Media", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tracks.size} TRACKS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )

                    Row {
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onShuffleAll,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shuffle", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            items(tracks) { track ->
                val isCurrent = track.id == currentlyPlayingTrackId

                ModernTrackCard(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying,
                    onClick = { onTrackClick(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onEditTags = { onEditTags(track) },
                    primaryColor = primaryColor
                )
            }

            item {
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
fun ModernTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditTags: () -> Unit,
    primaryColor: Color
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrent) primaryColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (isCurrent) primaryColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with playing wave overlay
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = track.albumArtUri ?: R.drawable.ic_default_album,
                contentDescription = "Track Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Playing",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isCurrent) primaryColor else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.durationFormatted}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Favorite Toggle
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (track.isFavorite) primaryColor else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 3-dot dropdown menu
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Play Next") },
                    onClick = {
                        onPlayNext()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Queue") },
                    onClick = {
                        onAddToQueue()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = {
                        onAddToPlaylist()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit Metadata Tags") },
                    onClick = {
                        onEditTags()
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    tracks: List<Track>,
    playlists: List<Playlist>,
    onCreatePlaylist: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onSelectSmartPlaylist: (title: String, tracks: List<Track>) -> Unit,
    primaryColor: Color
) {
    val favorites = remember(tracks) { tracks.filter { it.isFavorite } }
    val recentlyPlayed = remember(tracks) { tracks.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed } }
    val recentlyAdded = remember(tracks) { tracks.sortedByDescending { it.dateAdded }.take(50) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Smart Playlists
        item {
            Text(
                text = "AUTO SMART PLAYLISTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        }

        item {
            SmartPlaylistItem(
                title = "Favorites",
                subtitle = "${favorites.size} tracks",
                icon = Icons.Default.Favorite,
                tint = primaryColor,
                onClick = { onSelectSmartPlaylist("Favorites", favorites) }
            )
        }

        item {
            SmartPlaylistItem(
                title = "Recently Played",
                subtitle = "${recentlyPlayed.size} tracks",
                icon = Icons.Default.History,
                tint = Color(0xFF38BDF8),
                onClick = { onSelectSmartPlaylist("Recently Played", recentlyPlayed) }
            )
        }

        item {
            SmartPlaylistItem(
                title = "Recently Added",
                subtitle = "${recentlyAdded.size} tracks",
                icon = Icons.Default.MusicNote,
                tint = Color(0xFFA855F7),
                onClick = { onSelectSmartPlaylist("Recently Added", recentlyAdded) }
            )
        }

        // Custom User Playlists
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUSTOM PLAYLISTS (${playlists.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = onCreatePlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("create_playlist_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Playlist", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(playlists) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { onSelectPlaylist(playlist) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = playlist.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${playlist.trackCount} tracks",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
private fun SmartPlaylistItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AlbumsTabContent(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    primaryColor: Color
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(albums) { album ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                    .clickable { onAlbumClick(album) }
                    .padding(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    AsyncImage(
                        model = album.albumArtUri ?: R.drawable.ic_default_album,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = album.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.artist} • ${album.trackCount} tracks",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ArtistsTabContent(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    primaryColor: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(artists) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { onArtistClick(artist) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = artist.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${artist.trackCount} tracks • ${artist.albumCount} albums",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun FoldersTabContent(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    primaryColor: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(folders) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .clickable { onFolderClick(folder) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = primaryColor, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = folder.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${folder.trackCount} songs • ${folder.path}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
