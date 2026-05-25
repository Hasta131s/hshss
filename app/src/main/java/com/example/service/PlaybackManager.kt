package com.example.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.util.Log
import com.example.api.FlofysDownloader
import com.example.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class LoopMode {
    NONE,       // Play queue through once
    REPEAT_ONE, // Loop the current track indefinitely
    REPEAT_ALL  // Loop the entire queue infinitely
}

object PlaybackManager {
    private const val TAG = "PlaybackManager"
    
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val progressHandler = Handler(Looper.getMainLooper())

    // UI Observable States
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isPlayingBuffering = MutableStateFlow(false)
    val isPlayingBuffering = _isPlayingBuffering.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val playbackProgress = _playbackProgress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs = _durationMs.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<Track>>(emptyList())
    val playbackQueue = _playbackQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex = _currentQueueIndex.asStateFlow()

    private val _loopMode = MutableStateFlow(LoopMode.NONE)
    val loopMode = _loopMode.asStateFlow()

    private val _recommendedTracks = MutableStateFlow<List<Track>>(emptyList())
    val recommendedTracks = _recommendedTracks.asStateFlow()

    enum class LyricsState { IDLE, LOADING, SUCCESS, ERROR }

    private val _currentTrackLyrics = MutableStateFlow<String?>(null)
    val currentTrackLyrics = _currentTrackLyrics.asStateFlow()

    private val _lyricsState = MutableStateFlow(LyricsState.IDLE)
    val lyricsState = _lyricsState.asStateFlow()

    fun loadLyricsForCurrentTrack() {
        val track = _currentTrack.value ?: return
        _lyricsState.value = LyricsState.LOADING
        scope.launch {
            try {
                val lyrics = com.example.api.LyricsProvider.fetchLyrics(track.author, track.title)
                _currentTrackLyrics.value = lyrics
                _lyricsState.value = LyricsState.SUCCESS
            } catch (e: Exception) {
                _currentTrackLyrics.value = "Sözler bulunamadı."
                _lyricsState.value = LyricsState.ERROR
            }
        }
    }

    fun setRecommendedTracks(tracks: List<Track>) {
        _recommendedTracks.value = tracks
    }

    // Playlist/Track change callback for UI sync
    var onTrackChanged: ((Track) -> Unit)? = null

    init {
        setupProgressUpdater()
    }

    private fun setupProgressUpdater() {
        progressHandler.postDelayed(object : Runnable {
            override fun run() {
                val player = mediaPlayer
                if (player != null && _isPlaying.value && !_isPlayingBuffering.value) {
                    try {
                        val pos = player.currentPosition
                        val dur = player.duration
                        if (dur > 0) {
                            _currentPositionMs.value = pos
                            _durationMs.value = dur
                            _playbackProgress.value = pos.toFloat() / dur
                        }
                    } catch (e: Exception) {
                        // ignore safe
                    }
                }
                progressHandler.postDelayed(this, 100)
            }
        }, 100)
    }

    @Synchronized
    private fun getOrCreateMediaPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    _isPlaying.value = false
                    _isPlayingBuffering.value = false
                    false
                }
            }
        }
        return mediaPlayer!!
    }

    private fun handleTrackCompletion() {
        val mode = _loopMode.value
        val list = _playbackQueue.value
        val index = _currentQueueIndex.value

        if (mode == LoopMode.REPEAT_ONE) {
            // Re-play the current track
            _currentTrack.value?.let { playCurrentTrack(it) }
        } else {
            if (list.isNotEmpty() && index != -1) {
                val nextIndex = index + 1
                if (nextIndex < list.size) {
                    _currentQueueIndex.value = nextIndex
                    val nextTrack = list[nextIndex]
                    _currentTrack.value = nextTrack
                    playCurrentTrack(nextTrack)
                } else {
                    if (mode == LoopMode.REPEAT_ALL) {
                        _currentQueueIndex.value = 0
                        val firstTrack = list[0]
                        _currentTrack.value = firstTrack
                        playCurrentTrack(firstTrack)
                    } else {
                        val recs = _recommendedTracks.value
                        if (recs.isNotEmpty()) {
                            _playbackQueue.value = recs
                            _currentQueueIndex.value = 0
                            val nextTrack = recs[0]
                            _currentTrack.value = nextTrack
                            playCurrentTrack(nextTrack)
                        } else {
                            // End of playback
                            _isPlaying.value = false
                            _playbackProgress.value = 0f
                            _currentPositionMs.value = 0
                        }
                    }
                }
            } else {
                val recs = _recommendedTracks.value
                if (recs.isNotEmpty()) {
                    _playbackQueue.value = recs
                    _currentQueueIndex.value = 0
                    val nextTrack = recs[0]
                    _currentTrack.value = nextTrack
                    playCurrentTrack(nextTrack)
                } else {
                    _isPlaying.value = false
                    _playbackProgress.value = 0f
                    _currentPositionMs.value = 0
                }
            }
        }
    }

    fun playAll(tracks: List<Track>, startIndex: Int, context: Context) {
        if (tracks.isEmpty()) return
        _playbackQueue.value = tracks
        val resolvedIndex = if (startIndex in tracks.indices) startIndex else 0
        _currentQueueIndex.value = resolvedIndex
        val track = tracks[resolvedIndex]
        _currentTrack.value = track
        
        playCurrentTrack(track)
        startPlaybackService(context)
    }

    fun togglePlayPause(context: Context) {
        val mPlayer = mediaPlayer
        if (mPlayer == null) {
            _currentTrack.value?.let { playCurrentTrack(it) }
            return
        }

        if (_isPlaying.value) {
            pause()
        } else {
            resume(context)
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
        } catch (e: Exception) {
            // quiet safe
        }
    }

    fun resume(context: Context) {
        try {
            getOrCreateMediaPlayer().start()
            _isPlaying.value = true
            startPlaybackService(context)
        } catch (e: Exception) {
            // try to play current track
            _currentTrack.value?.let { playCurrentTrack(it) }
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            // safe quiet
        }
    }

    fun rewind10s() {
        try {
            mediaPlayer?.let {
                val target = (it.currentPosition - 10000).coerceAtLeast(0)
                it.seekTo(target)
                _currentPositionMs.value = target
            }
        } catch (e: Exception) {
            // safe quiet
        }
    }

    fun forward10s() {
        try {
            mediaPlayer?.let {
                val target = (it.currentPosition + 10000).coerceAtMost(it.duration)
                it.seekTo(target)
                _currentPositionMs.value = target
            }
        } catch (e: Exception) {
            // safe quiet
        }
    }

    fun skipToNext(context: Context) {
        val list = _playbackQueue.value
        val index = _currentQueueIndex.value
        if (list.isNotEmpty() && index != -1) {
            val nextIndex = index + 1
            if (nextIndex < list.size) {
                _currentQueueIndex.value = nextIndex
                val nextTrack = list[nextIndex]
                _currentTrack.value = nextTrack
                playCurrentTrack(nextTrack)
                startPlaybackService(context)
            } else if (_loopMode.value == LoopMode.REPEAT_ALL) {
                _currentQueueIndex.value = 0
                val firstTrack = list[0]
                _currentTrack.value = firstTrack
                playCurrentTrack(firstTrack)
                startPlaybackService(context)
            } else {
                val recs = _recommendedTracks.value
                if (recs.isNotEmpty()) {
                    _playbackQueue.value = recs
                    _currentQueueIndex.value = 0
                    val nextTrack = recs[0]
                    _currentTrack.value = nextTrack
                    playCurrentTrack(nextTrack)
                    startPlaybackService(context)
                }
            }
        } else {
            val recs = _recommendedTracks.value
            if (recs.isNotEmpty()) {
                _playbackQueue.value = recs
                _currentQueueIndex.value = 0
                val nextTrack = recs[0]
                _currentTrack.value = nextTrack
                playCurrentTrack(nextTrack)
                startPlaybackService(context)
            }
        }
    }

    fun skipToPrevious(context: Context) {
        val list = _playbackQueue.value
        val index = _currentQueueIndex.value
        if (list.isNotEmpty() && index != -1) {
            val prevIndex = index - 1
            if (prevIndex >= 0) {
                _currentQueueIndex.value = prevIndex
                val prevTrack = list[prevIndex]
                _currentTrack.value = prevTrack
                playCurrentTrack(prevTrack)
                startPlaybackService(context)
            } else if (_loopMode.value == LoopMode.REPEAT_ALL) {
                _currentQueueIndex.value = list.size - 1
                val lastTrack = list[list.size - 1]
                _currentTrack.value = lastTrack
                playCurrentTrack(lastTrack)
                startPlaybackService(context)
            }
        }
    }

    fun setLoopMode(mode: LoopMode) {
        _loopMode.value = mode
    }

    fun playTrackAlone(track: Track, context: Context) {
        // Creates a virtual queue containing only this track
        _playbackQueue.value = listOf(track)
        _currentQueueIndex.value = 0
        _currentTrack.value = track
        playCurrentTrack(track)
        startPlaybackService(context)
    }

    private fun playCurrentTrack(track: Track) {
        onTrackChanged?.invoke(track)
        _currentTrackLyrics.value = null
        _lyricsState.value = LyricsState.LOADING
        scope.launch {
            try {
                val lyrics = com.example.api.LyricsProvider.fetchLyrics(track.author, track.title)
                _currentTrackLyrics.value = lyrics
                _lyricsState.value = LyricsState.SUCCESS
            } catch (e: Exception) {
                _currentTrackLyrics.value = "Sözler bulunamadı."
                _lyricsState.value = LyricsState.ERROR
            }
        }
        scope.launch {
            _isPlaying.value = false
            _isPlayingBuffering.value = true
            _playbackProgress.value = 0f
            _currentPositionMs.value = 0
            _durationMs.value = 100 // default dummy duration

            try {
                val dataSource = if (track.isDownloaded && track.localFilePath != null) {
                    val file = File(track.localFilePath)
                    if (file.exists()) {
                        track.localFilePath
                    } else {
                        null
                    }
                } else {
                    null
                }

                val finalUrl = dataSource ?: withContext(Dispatchers.IO) {
                    FlofysDownloader.getMp3DownloadLink(track.id)
                }

                if (finalUrl == null) {
                    _isPlayingBuffering.value = false
                    return@launch
                }

                val player = getOrCreateMediaPlayer()
                player.reset()
                player.setDataSource(finalUrl)
                
                player.setOnPreparedListener { mp ->
                    _isPlayingBuffering.value = false
                    mp.start()
                    _isPlaying.value = true
                    _durationMs.value = mp.duration
                }
                
                player.prepareAsync()
            } catch (e: Exception) {
                _isPlayingBuffering.value = false
                _isPlaying.value = false
            }
        }
    }

    private fun startPlaybackService(context: Context) {
        try {
            val intent = Intent(context, FlofysPlaybackService::class.java).apply {
                action = FlofysPlaybackService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback service: ${e.message}")
        }
    }

    fun cleanUp() {
        try {
            progressHandler.removeCallbacksAndMessages(null)
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // safe quiet
        }
    }
}
