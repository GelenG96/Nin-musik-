package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.CustomEqPresetEntity
import com.example.data.local.FavoriteTrackEntity
import com.example.data.local.HistoryTrackEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.ResonixDatabase
import com.example.data.local.TrackTagOverrideEntity
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.EqPreset
import com.example.data.model.Folder
import com.example.data.model.Genre
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.util.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {
    private val database = ResonixDatabase.getInstance(context)
    private val musicDao = database.musicDao()
    private val playlistDao = database.playlistDao()
    private val equalizerDao = database.equalizerDao()

    // Scans local MediaStore for all audio files
    suspend fun scanLocalTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Filter out ringtones, notifications, and audio shorter than 5 seconds
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown Title"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val durationMs = it.getLong(durationCol)
                    val dataPath = it.getString(dataCol) ?: ""
                    val albumId = it.getLong(albumIdCol)
                    val dateAdded = it.getLong(dateAddedCol)

                    val contentUri = ContentUris.withAppendedId(collectionUri, id)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    // Determine folder
                    val file = File(dataPath)
                    val folderName = file.parentFile?.name ?: "Music"
                    val folderPath = file.parentFile?.absolutePath ?: ""

                    // Check for existing LRC file
                    val lrcFile = LrcParser.findLrcFileForTrack(dataPath)

                    tracks.add(
                        Track(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            durationMs = durationMs,
                            dataPath = dataPath,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            genre = "Music",
                            dateAdded = dateAdded,
                            folderName = folderName,
                            folderPath = folderPath,
                            lrcPath = lrcFile?.absolutePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    // Reactive flow combining scanned tracks with DB favorites, history, and tag overrides
    fun getAugmentedTracksFlow(rawTracks: List<Track>): Flow<List<Track>> {
        return combine(
            musicDao.getAllFavoriteIds(),
            musicDao.getRecentHistory(),
            musicDao.getAllTagOverrides()
        ) { favIds, history, overrides ->
            val favSet = favIds.toSet()
            val historyMap = history.associateBy { it.trackId }
            val overrideMap = overrides.associateBy { it.trackId }

            rawTracks.map { track ->
                val isFav = favSet.contains(track.id)
                val hist = historyMap[track.id]
                val override = overrideMap[track.id]

                track.copy(
                    isFavorite = isFav,
                    playCount = hist?.playCount ?: 0,
                    lastPlayed = hist?.lastPlayedAt ?: 0L,
                    title = override?.customTitle ?: track.title,
                    artist = override?.customArtist ?: track.artist,
                    album = override?.customAlbum ?: track.album,
                    genre = override?.customGenre ?: track.genre
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    // Favorites
    suspend fun toggleFavorite(trackId: Long) = withContext(Dispatchers.IO) {
        if (musicDao.isFavorite(trackId)) {
            musicDao.deleteFavorite(trackId)
        } else {
            musicDao.insertFavorite(FavoriteTrackEntity(trackId))
        }
    }

    // History
    suspend fun recordTrackPlayed(trackId: Long) = withContext(Dispatchers.IO) {
        val existing = musicDao.getHistoryForTrack(trackId)
        val newCount = (existing?.playCount ?: 0) + 1
        musicDao.recordPlay(
            HistoryTrackEntity(
                trackId = trackId,
                lastPlayedAt = System.currentTimeMillis(),
                playCount = newCount
            )
        )
    }

    // Tag Editor
    suspend fun updateTrackTags(
        trackId: Long,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?
    ) = withContext(Dispatchers.IO) {
        musicDao.insertTagOverride(
            TrackTagOverrideEntity(
                trackId = trackId,
                customTitle = title?.trim()?.ifEmpty { null },
                customArtist = artist?.trim()?.ifEmpty { null },
                customAlbum = album?.trim()?.ifEmpty { null },
                customGenre = genre?.trim()?.ifEmpty { null }
            )
        )
    }

    // Playlists
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    trackCount = playlistDao.getSongCount(entity.id),
                    createdAt = entity.createdAt,
                    isSmart = false
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.createPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistSongs(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    fun getPlaylistTrackIds(playlistId: Long): Flow<List<Long>> {
        return playlistDao.getTrackIdsForPlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        val currentCount = playlistDao.getSongCount(playlistId)
        playlistDao.addTrackToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                trackId = trackId,
                orderIndex = currentCount
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    // Custom EQ Presets
    fun getCustomEqPresets(): Flow<List<EqPreset>> {
        return equalizerDao.getAllCustomPresets().map { list ->
            list.map {
                EqPreset(
                    id = it.id.toInt() + 1000,
                    name = it.name,
                    gains = listOf(it.band0, it.band1, it.band2, it.band3, it.band4),
                    isCustom = true
                )
            }
        }
    }

    suspend fun saveCustomPreset(name: String, gains: List<Int>, bass: Int, virtualizer: Int): Long = withContext(Dispatchers.IO) {
        equalizerDao.saveCustomPreset(
            CustomEqPresetEntity(
                name = name,
                band0 = gains.getOrElse(0) { 0 },
                band1 = gains.getOrElse(1) { 0 },
                band2 = gains.getOrElse(2) { 0 },
                band3 = gains.getOrElse(3) { 0 },
                band4 = gains.getOrElse(4) { 0 },
                bassBoost = bass,
                virtualizer = virtualizer
            )
        )
    }

    suspend fun deleteCustomPreset(presetId: Long) = withContext(Dispatchers.IO) {
        equalizerDao.deleteCustomPreset(presetId - 1000)
    }
}
