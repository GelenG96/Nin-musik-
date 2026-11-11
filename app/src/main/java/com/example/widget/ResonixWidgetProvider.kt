package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.playback.MusicPlaybackService
import com.example.playback.PlaybackStateManager

class ResonixWidgetProvider : AppWidgetProvider() {

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ResonixWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, ResonixWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = PlaybackStateManager.state.value

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_music_player)

            // Open App on tap
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

            // Song Title and Artist
            val currentTrack = state.currentTrack
            if (currentTrack != null) {
                views.setTextViewText(R.id.widget_song_title, currentTrack.title)
                views.setTextViewText(R.id.widget_song_artist, "${currentTrack.artist} • ${currentTrack.album}")
            } else {
                views.setTextViewText(R.id.widget_song_title, "Resonix Player")
                views.setTextViewText(R.id.widget_song_artist, "Tap to select music")
            }

            // Play/Pause button
            val playPauseRes = if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            views.setImageViewResource(R.id.widget_btn_play, playPauseRes)

            // Controls PendingIntents
            val prevIntent = Intent(context, ResonixWidgetProvider::class.java).apply {
                action = "com.example.ACTION_PREV"
            }
            val prevPending = PendingIntent.getBroadcast(
                context, 10, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

            val toggleIntent = Intent(context, ResonixWidgetProvider::class.java).apply {
                action = "com.example.ACTION_TOGGLE_PLAY"
            }
            val togglePending = PendingIntent.getBroadcast(
                context, 11, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play, togglePending)

            val nextIntent = Intent(context, ResonixWidgetProvider::class.java).apply {
                action = "com.example.ACTION_NEXT"
            }
            val nextPending = PendingIntent.getBroadcast(
                context, 12, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            "com.example.ACTION_TOGGLE_PLAY" -> {
                MusicPlaybackService.startService(context, MusicPlaybackService.ACTION_TOGGLE_PLAY)
            }
            "com.example.ACTION_NEXT" -> {
                MusicPlaybackService.startService(context, MusicPlaybackService.ACTION_NEXT)
            }
            "com.example.ACTION_PREV" -> {
                MusicPlaybackService.startService(context, MusicPlaybackService.ACTION_PREV)
            }
        }
    }
}
