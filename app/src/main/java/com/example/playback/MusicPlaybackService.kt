package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.MainActivity
import com.example.R
import com.example.data.model.AudioEffectConfig
import com.example.data.model.RepeatMode
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import com.example.util.LrcParser
import com.example.widget.ResonixWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot

class MusicPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "resonix_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PLAY_TRACK = "com.example.ACTION_PLAY_TRACK"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_TOGGLE_PLAY = "com.example.ACTION_TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_SEEK = "com.example.ACTION_SEEK"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_APPLY_EFFECTS = "com.example.ACTION_APPLY_EFFECTS"
        const val ACTION_START_SLEEP_TIMER = "com.example.ACTION_START_SLEEP_TIMER"
        const val ACTION_CANCEL_SLEEP_TIMER = "com.example.ACTION_CANCEL_SLEEP_TIMER"

        const val EXTRA_TRACK_ID = "EXTRA_TRACK_ID"
        const val EXTRA_POSITION_MS = "EXTRA_POSITION_MS"
        const val EXTRA_TIMER_MINUTES = "EXTRA_TIMER_MINUTES"
        const val EXTRA_TIMER_FINISH_SONG = "EXTRA_TIMER_FINISH_SONG"

        fun startService(context: Context, action: String) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentlyLoadedTrackId: Long? = null
    private lateinit var audioManager: AudioManager
    private lateinit var musicRepository: MusicRepository

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var visualizer: Visualizer? = null

    private var hasAudioFocus = false
    private var audioFocusRequest: AudioFocusRequest? = null

    // Becoming noisy receiver
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        musicRepository = MusicRepository(applicationContext)

        createNotificationChannel()
        initMediaSession()

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)

        // Observe effect changes to update DSP in real-time
        serviceScope.launch {
            PlaybackStateManager.state.collect { state ->
                applyAudioEffects(state.audioEffects)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Resonix Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing music playback controls and notifications"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "ResonixMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = play()
                override fun onPause() = pause()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = prev()
                override fun onSeekTo(pos: Long) = seekTo(pos)
                override fun onStop() = stopPlayback()
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_TRACK -> {
                val trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1L)
                val state = PlaybackStateManager.state.value
                val targetTrack = if (trackId != -1L) {
                    state.queue.find { it.id == trackId } ?: state.currentTrack
                } else {
                    state.currentTrack ?: state.queue.getOrNull(0)
                }
                if (targetTrack != null) {
                    playTrack(targetTrack)
                }
            }
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_TOGGLE_PLAY -> togglePlay()
            ACTION_NEXT -> next()
            ACTION_PREV -> prev()
            ACTION_SEEK -> {
                val pos = intent.getLongExtra(EXTRA_POSITION_MS, 0L)
                seekTo(pos)
            }
            ACTION_STOP -> stopPlayback()
            ACTION_APPLY_EFFECTS -> {
                applyAudioEffects(PlaybackStateManager.state.value.audioEffects)
            }
            ACTION_START_SLEEP_TIMER -> {
                val minutes = intent.getIntExtra(EXTRA_TIMER_MINUTES, 15)
                val finishSong = intent.getBooleanExtra(EXTRA_TIMER_FINISH_SONG, false)
                startSleepTimer(minutes, finishSong)
            }
            ACTION_CANCEL_SLEEP_TIMER -> {
                cancelSleepTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { focusChange -> handleAudioFocusChange(focusChange) }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
                hasAudioFocus = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                applyVolumeBalance(PlaybackStateManager.state.value.audioEffects)
                play()
            }
        }
    }

    fun playTrack(track: Track) {
        if (!requestAudioFocus()) return

        currentlyLoadedTrackId = track.id
        releasePlayer()

        try {
            val player = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, track.contentUri)
                setOnPreparedListener { mp ->
                    mp.start()
                    PlaybackStateManager.updateCurrentTrack(track, mp.duration.toLong())
                    PlaybackStateManager.updatePlayingState(true)
                    setupAudioEffects(mp.audioSessionId)
                    applyAudioEffects(PlaybackStateManager.state.value.audioEffects)
                    startProgressTracker()
                    startForeground(NOTIFICATION_ID, buildNotification(track, true))
                    ResonixWidgetProvider.updateAllWidgets(applicationContext)

                    // Record history in DB
                    serviceScope.launch {
                        musicRepository.recordTrackPlayed(track.id)
                    }

                    // Check for LRC lyrics
                    loadLyricsForTrack(track)
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    next()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
            next()
        }
    }

    private fun loadLyricsForTrack(track: Track) {
        serviceScope.launch(Dispatchers.IO) {
            var lrcFile = track.lrcPath?.let { File(it) }
            if (lrcFile == null || !lrcFile.exists()) {
                lrcFile = LrcParser.findLrcFileForTrack(track.dataPath)
            }
            if (lrcFile != null && lrcFile.exists()) {
                val content = lrcFile.readText()
                val parsed = LrcParser.parse(content)
                PlaybackStateManager.setLyrics(parsed)
            } else {
                PlaybackStateManager.setLyrics(emptyList())
            }
        }
    }

    private fun handleTrackCompletion() {
        val state = PlaybackStateManager.state.value

        // Check Sleep Timer "Finish current song"
        if (state.isSleepTimerActive && state.sleepTimerFinishSong && state.sleepTimerRemainingSec <= 0) {
            stopPlayback()
            PlaybackStateManager.updateSleepTimer(0, finishSong = false, isActive = false)
            return
        }

        when (state.repeatMode) {
            RepeatMode.ONE -> {
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()
                PlaybackStateManager.updatePlayingState(true)
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.lastIndex) {
                    next()
                } else {
                    stopPlayback()
                }
            }
        }
    }

    private fun play() {
        val state = PlaybackStateManager.state.value
        val targetTrack = state.currentTrack ?: state.queue.getOrNull(state.queueIndex) ?: state.queue.getOrNull(0)
        if (targetTrack == null) return

        if (mediaPlayer != null && currentlyLoadedTrackId == targetTrack.id) {
            if (requestAudioFocus()) {
                mediaPlayer?.start()
                PlaybackStateManager.updatePlayingState(true)
                startProgressTracker()
                startForeground(NOTIFICATION_ID, buildNotification(targetTrack, true))
                ResonixWidgetProvider.updateAllWidgets(applicationContext)
            }
        } else {
            playTrack(targetTrack)
        }
    }

    private fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        PlaybackStateManager.updatePlayingState(false)
        stopProgressTracker()
        PlaybackStateManager.state.value.currentTrack?.let {
            val notification = buildNotification(it, false)
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
        ResonixWidgetProvider.updateAllWidgets(applicationContext)
    }

    private fun togglePlay() {
        if (mediaPlayer?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    private fun next() {
        val state = PlaybackStateManager.state.value
        if (state.queue.isEmpty()) return
        val nextIndex = (state.queueIndex + 1) % state.queue.size
        val nextTrack = state.queue[nextIndex]
        PlaybackStateManager.setQueueIndex(nextIndex)
        playTrack(nextTrack)
    }

    private fun prev() {
        val state = PlaybackStateManager.state.value
        if (state.queue.isEmpty()) return

        // If played more than 3 seconds, replay from start
        val currentPos = mediaPlayer?.currentPosition ?: 0
        if (currentPos > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (state.queueIndex > 0) state.queueIndex - 1 else state.queue.lastIndex
        val prevTrack = state.queue[prevIndex]
        PlaybackStateManager.setQueueIndex(prevIndex)
        playTrack(prevTrack)
    }

    private fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        PlaybackStateManager.updatePosition(positionMs)
    }

    private fun stopPlayback() {
        currentlyLoadedTrackId = null
        releasePlayer()
        abandonAudioFocus()
        PlaybackStateManager.updatePlayingState(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        ResonixWidgetProvider.updateAllWidgets(applicationContext)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    val pos = mp.currentPosition.toLong()
                    PlaybackStateManager.updatePosition(pos)

                    // Update synced lyrics
                    val lyrics = PlaybackStateManager.state.value.currentLyrics
                    if (lyrics.isNotEmpty()) {
                        val activeIndex = LrcParser.findActiveLyricIndex(lyrics, pos)
                        PlaybackStateManager.setActiveLyricIndex(activeIndex)
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    // Audio Effects Setup
    private fun setupAudioEffects(sessionId: Int) {
        releaseAudioEffects()
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }

            // Initialize Visualizer with safety checks
            try {
                val captureRange = Visualizer.getCaptureSizeRange()
                val captureSize = captureRange[0].coerceAtLeast(128)
                visualizer = Visualizer(sessionId).apply {
                    setCaptureSize(captureSize)
                    setDataCaptureListener(
                        object : Visualizer.OnDataCaptureListener {
                            override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                                if (waveform != null) {
                                    processWaveform(waveform)
                                }
                            }

                            override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                                if (fft != null) {
                                    processFft(fft)
                                }
                            }
                        },
                        Visualizer.getMaxCaptureRate() / 2,
                        true,
                        true
                    )
                    enabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processWaveform(waveform: ByteArray) {
        var sum = 0f
        for (b in waveform) {
            val sample = (b.toInt() and 0xFF) - 128
            sum += abs(sample)
        }
        val peak = (sum / waveform.size) / 128f
        val currentVis = PlaybackStateManager.state.value.visualizerData
        PlaybackStateManager.updateVisualizer(
            currentVis.copy(rawWaveform = waveform, peakAmplitude = peak)
        )
    }

    private fun processFft(fft: ByteArray) {
        // Compute 32 normalized frequency bands from FFT pairs (real, imag)
        val bandCount = 32
        val normalized = FloatArray(bandCount)
        val step = (fft.size / 2) / bandCount

        for (i in 0 until bandCount) {
            var bandMag = 0f
            val startIdx = 2 + i * step * 2
            val endIdx = (startIdx + step * 2).coerceAtMost(fft.size)
            var count = 0
            for (j in startIdx until endIdx step 2) {
                if (j + 1 < fft.size) {
                    val r = fft[j].toFloat()
                    val im = fft[j + 1].toFloat()
                    bandMag += hypot(r, im)
                    count++
                }
            }
            if (count > 0) bandMag /= count
            normalized[i] = (bandMag / 64f).coerceIn(0f, 1f)
        }

        val currentVis = PlaybackStateManager.state.value.visualizerData
        PlaybackStateManager.updateVisualizer(
            currentVis.copy(rawFft = fft, normalizedFft = normalized)
        )
    }

    private fun applyAudioEffects(config: AudioEffectConfig) {
        try {
            // Equalizer
            equalizer?.let { eq ->
                eq.enabled = config.isEqEnabled
                if (config.isEqEnabled) {
                    val numBands = eq.numberOfBands.toInt()
                    for (i in 0 until numBands) {
                        val gain = config.bandGains.getOrElse(i) { 0 }
                        // Ensure gain is within device band range
                        val min = eq.bandLevelRange[0]
                        val max = eq.bandLevelRange[1]
                        val clamped = gain.toShort().coerceIn(min, max)
                        eq.setBandLevel(i.toShort(), clamped)
                    }
                }
            }

            // Bass Boost
            bassBoost?.let { bb ->
                bb.enabled = config.bassBoostStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(config.bassBoostStrength.toShort())
                }
            }

            // Virtualizer
            virtualizer?.let { virt ->
                virt.enabled = config.virtualizerStrength > 0
                if (virt.strengthSupported) {
                    virt.setStrength(config.virtualizerStrength.toShort())
                }
            }

            // Volume & Balance
            applyVolumeBalance(config)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyVolumeBalance(config: AudioEffectConfig) {
        val player = mediaPlayer ?: return
        val balance = config.balance.coerceIn(-1.0f, 1.0f)
        val gain = config.volumeGain.coerceIn(0.0f, 1.5f)

        val left = if (balance > 0f) (1.0f - balance) * gain else 1.0f * gain
        val right = if (balance < 0f) (1.0f + balance) * gain else 1.0f * gain

        player.setVolume(left.coerceIn(0f, 1f), right.coerceIn(0f, 1f))
    }

    private fun releaseAudioEffects() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null

            equalizer?.release()
            equalizer = null

            bassBoost?.release()
            bassBoost = null

            virtualizer?.release()
            virtualizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releasePlayer() {
        stopProgressTracker()
        releaseAudioEffects()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
    }

    private fun buildNotification(track: Track, isPlaying: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_PLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIconRes = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)

        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }

        return builder
            .setSmallIcon(R.drawable.ic_widget_play)
            .setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.album}")
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_widget_prev),
                    "Previous",
                    prevIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, playPauseIconRes),
                    if (isPlaying) "Pause" else "Play",
                    toggleIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_widget_next),
                    "Next",
                    nextIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    "Close",
                    stopIntent
                ).build()
            )
            .setStyle(mediaStyle)
            .build()
    }

    fun startSleepTimer(minutes: Int, finishSong: Boolean) {
        sleepTimerJob?.cancel()
        val totalSec = minutes * 60L
        PlaybackStateManager.updateSleepTimer(totalSec, finishSong, isActive = true)

        sleepTimerJob = serviceScope.launch {
            var remaining = totalSec
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                PlaybackStateManager.updateSleepTimer(remaining, finishSong, isActive = true)
            }
            if (!finishSong) {
                stopPlayback()
                PlaybackStateManager.updateSleepTimer(0, finishSong = false, isActive = false)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        PlaybackStateManager.updateSleepTimer(0, finishSong = false, isActive = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(noisyReceiver)
        releasePlayer()
        mediaSession?.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
