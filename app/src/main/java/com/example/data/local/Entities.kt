package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val trackId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class HistoryTrackEntity(
    @PrimaryKey val trackId: Long,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    indices = [Index(value = ["playlistId", "trackId"], unique = true)]
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val trackId: Long,
    val orderIndex: Int = 0
)

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val band0: Int,
    val band1: Int,
    val band2: Int,
    val band3: Int,
    val band4: Int,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0
)

@Entity(tableName = "track_tag_overrides")
data class TrackTagOverrideEntity(
    @PrimaryKey val trackId: Long,
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbum: String? = null,
    val customGenre: String? = null
)
