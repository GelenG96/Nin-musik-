package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteTrackEntity::class,
        HistoryTrackEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        CustomEqPresetEntity::class,
        TrackTagOverrideEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ResonixDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun equalizerDao(): EqualizerDao

    companion object {
        @Volatile
        private var INSTANCE: ResonixDatabase? = null

        fun getInstance(context: Context): ResonixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ResonixDatabase::class.java,
                    "resonix_music.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
