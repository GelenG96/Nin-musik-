package com.example.data.model

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dataPath: String,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val genre: String = "General",
    val dateAdded: Long = 0L,
    val folderName: String = "",
    val folderPath: String = "",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val lrcPath: String? = null
) {
    val durationFormatted: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val albumArtUri: Uri? = null
)

data class Artist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)

data class Folder(
    val name: String,
    val path: String,
    val trackCount: Int
)

data class Genre(
    val id: Long,
    val name: String,
    val trackCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isSmart: Boolean = false
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class VisualizerStyle(val displayName: String) {
    SPECTRUM_BARS("Spectrum Bars"),
    FLUID_WAVEFORM("Waveform"),
    RADIAL_SPECTRUM("Radial Pulse"),
    PARTICLE_BEAT("Particle Bass")
}

data class LyricLine(
    val timestampMs: Long,
    val text: String
)
