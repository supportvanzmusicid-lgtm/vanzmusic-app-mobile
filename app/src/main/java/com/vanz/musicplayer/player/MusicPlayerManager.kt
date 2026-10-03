package com.vanz.musicplayer.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.vanz.musicplayer.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppPlaybackState {
    IDLE,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

class MusicPlayerManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "MusicPlayerManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                shuffleModeEnabled = false
                volume = 1.0f
                addListener(playerListener)
            }
    }

    private val _playbackState = MutableStateFlow(AppPlaybackState.IDLE)
    val playbackState: StateFlow<AppPlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var onTrackChangedListener: ((Track?) -> Unit)? = null

    fun setOnTrackChangedListener(listener: (Track?) -> Unit) {
        onTrackChangedListener = listener
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_IDLE -> {
                    _playbackState.value = AppPlaybackState.IDLE
                    stopProgressTicker()
                }
                Player.STATE_BUFFERING -> {
                    _playbackState.value = AppPlaybackState.BUFFERING
                }
                Player.STATE_READY -> {
                    val isPlaying = exoPlayer.isPlaying
                    _playbackState.value = if (isPlaying) AppPlaybackState.PLAYING else AppPlaybackState.READY
                    _duration.value = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                    if (isPlaying) startProgressTicker() else stopProgressTicker()
                }
                Player.STATE_ENDED -> {
                    _playbackState.value = AppPlaybackState.ENDED
                    stopProgressTicker()
                    playNextTrack()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _playbackState.value = AppPlaybackState.PLAYING
                startProgressTicker()
            } else {
                if (_playbackState.value != AppPlaybackState.BUFFERING && _playbackState.value != AppPlaybackState.IDLE) {
                    _playbackState.value = AppPlaybackState.PAUSED
                }
                stopProgressTicker()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "ExoPlayer Error: ${error.errorCodeName}", error)
            _playbackState.value = AppPlaybackState.ERROR
            _errorMessage.value = "Playback error: ${error.message}"
            stopProgressTicker()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = exoPlayer.currentMediaItemIndex
            if (idx >= 0 && idx < _queue.value.size) {
                _currentIndex.value = idx
                onTrackChangedListener?.invoke(_queue.value[idx])
            }
        }
    }

    fun playTrack(track: Track, streamUrl: String) {
        _queue.value = listOf(track)
        _currentIndex.value = 0
        prepareAndPlay(track, streamUrl)
    }

    fun playQueue(tracks: List<Track>, startIndex: Int, streamUrl: String) {
        if (tracks.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, tracks.size - 1)
        _queue.value = tracks
        _currentIndex.value = validIndex
        prepareAndPlay(tracks[validIndex], streamUrl)
    }

    fun addToQueue(track: Track) {
        val currentList = _queue.value.toMutableList()
        currentList.add(track)
        _queue.value = currentList
    }

    fun playNext(track: Track) {
        val currentList = _queue.value.toMutableList()
        val nextIdx = (_currentIndex.value + 1).coerceAtMost(currentList.size)
        currentList.add(nextIdx, track)
        _queue.value = currentList
    }

    private fun prepareAndPlay(track: Track, streamUrl: String) {
        try {
            _playbackState.value = AppPlaybackState.BUFFERING
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setArtworkUri(Uri.parse(track.highResThumbnailUrl.ifBlank { track.thumbnailUrl }))
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaMetadata(mediaMetadata)
                .build()

            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            _errorMessage.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare ExoPlayer for track ${track.title}", e)
            _playbackState.value = AppPlaybackState.ERROR
            _errorMessage.value = e.message
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun play() {
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, _duration.value.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
        _currentPosition.value = target
    }

    fun seekForward(ms: Long = 10000L) {
        val target = (exoPlayer.currentPosition + ms).coerceAtMost(_duration.value)
        seekTo(target)
    }

    fun seekBackward(ms: Long = 10000L) {
        val target = (exoPlayer.currentPosition - ms).coerceAtLeast(0L)
        seekTo(target)
    }

    fun setVolume(newVolume: Float) {
        val clamped = newVolume.coerceIn(0.0f, 1.0f)
        exoPlayer.volume = clamped
        _volume.value = clamped
    }

    fun toggleShuffle() {
        val newState = !_isShuffle.value
        _isShuffle.value = newState
        exoPlayer.shuffleModeEnabled = newState
    }

    fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer.repeatMode = nextMode
    }

    fun playNextTrack() {
        val list = _queue.value
        if (list.isEmpty()) return
        val current = _currentIndex.value
        val nextIdx = if (_isShuffle.value && list.size > 1) {
            (list.indices - current).random()
        } else {
            current + 1
        }

        if (nextIdx < list.size) {
            _currentIndex.value = nextIdx
            onTrackChangedListener?.invoke(list[nextIdx])
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
            _currentIndex.value = 0
            onTrackChangedListener?.invoke(list[0])
        }
    }

    fun playPreviousTrack() {
        if (exoPlayer.currentPosition > 3000L) {
            seekTo(0L)
            return
        }

        val list = _queue.value
        if (list.isEmpty()) return
        val current = _currentIndex.value
        val prevIdx = current - 1
        if (prevIdx >= 0) {
            _currentIndex.value = prevIdx
            onTrackChangedListener?.invoke(list[prevIdx])
        } else {
            seekTo(0L)
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition
                    val dur = exoPlayer.duration
                    _currentPosition.value = if (pos >= 0) pos else 0L
                    if (dur > 0 && dur != _duration.value) {
                        _duration.value = dur
                    }
                }
                delay(60)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTicker()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
