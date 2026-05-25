package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FlofysPlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "flofys_playback_channel"
        const val NOTIFICATION_ID = 4443

        const val ACTION_START = "com.example.flofys.action.START"
        const val ACTION_PLAY_PAUSE = "com.example.flofys.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.flofys.action.NEXT"
        const val ACTION_PREV = "com.example.flofys.action.PREV"
        const val ACTION_REWIND = "com.example.flofys.action.REWIND"
        const val ACTION_FORWARD = "com.example.flofys.action.FORWARD"
        const val ACTION_STOP = "com.example.flofys.action.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession = MediaSession(this, "FlofysSession").apply {
                isActive = true
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        PlaybackManager.resume(this@FlofysPlaybackService)
                    }

                    override fun onPause() {
                        PlaybackManager.pause()
                    }

                    override fun onSkipToNext() {
                        PlaybackManager.skipToNext(this@FlofysPlaybackService)
                    }

                    override fun onSkipToPrevious() {
                        PlaybackManager.skipToPrevious(this@FlofysPlaybackService)
                    }
                })
            }
        }

        // Observe player changes to update notification dynamically
        serviceScope.launch {
            PlaybackManager.isPlaying.collectLatest {
                updateNotification()
            }
        }
        serviceScope.launch {
            PlaybackManager.currentTrack.collectLatest {
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            ACTION_PLAY_PAUSE -> {
                PlaybackManager.togglePlayPause(this)
                updateNotification()
            }
            ACTION_NEXT -> {
                PlaybackManager.skipToNext(this)
                updateNotification()
            }
            ACTION_PREV -> {
                PlaybackManager.skipToPrevious(this)
                updateNotification()
            }
            ACTION_REWIND -> {
                PlaybackManager.rewind10s()
            }
            ACTION_FORWARD -> {
                PlaybackManager.forward10s()
            }
            ACTION_STOP -> {
                PlaybackManager.pause()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Flofys Müziği"
            val descriptionText = "Flofys arka planda müzik dinleme bildirim kontrolü"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val currentTrack = PlaybackManager.currentTrack.value
        val isPlaying = PlaybackManager.isPlaying.value

        val title = currentTrack?.title ?: "Müzik Dinle"
        val author = currentTrack?.author ?: "Flofys Müzik"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, flag)

        // Notification actions pending intents
        val playPauseIntent = Intent(this, FlofysPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val nextIntent = Intent(this, FlofysPlaybackService::class.java).apply { action = ACTION_NEXT }
        val prevIntent = Intent(this, FlofysPlaybackService::class.java).apply { action = ACTION_PREV }
        val stopIntent = Intent(this, FlofysPlaybackService::class.java).apply { action = ACTION_STOP }

        val playPausePI = PendingIntent.getService(this, 1, playPauseIntent, flag)
        val nextPI = PendingIntent.getService(this, 2, nextIntent, flag)
        val prevPI = PendingIntent.getService(this, 3, prevIntent, flag)
        val stopPI = PendingIntent.getService(this, 4, stopIntent, flag)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(author)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Custom style using Media metadata handles locks screens automatically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
            builder.setStyle(mediaStyle)
        }

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseLabel = if (isPlaying) "Duraklat" else "Oynat"

        builder.addAction(android.R.drawable.ic_media_previous, "Önceki", prevPI)
        builder.addAction(playPauseIcon, playPauseLabel, playPausePI)
        builder.addAction(android.R.drawable.ic_media_next, "Sonraki", nextPI)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Kapat", stopPI)

        return builder.build()
    }
}
