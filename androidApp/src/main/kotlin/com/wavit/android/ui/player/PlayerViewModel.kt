package com.wavit.android.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.wavit.repository.WavitApiRepository
import com.wavit.repository.WavitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI State for Player Screen.
 */
data class PlayerUiState(
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String? = null,
    val isResolvingStream: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isUserSeeking: Boolean = false,
    val userSeekPositionMs: Long = 0L,
    val errorMessage: String? = null
) {
    /**
     * Effective slider position: uses user's active drag position if seeking,
     * otherwise the real-time player position.
     */
    val displayPositionMs: Long
        get() = if (isUserSeeking) userSeekPositionMs else currentPositionMs

    val progressFraction: Float
        get() = if (durationMs > 0) (displayPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

class PlayerViewModel(
    application: Application,
    private val repository: WavitRepository = WavitApiRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    init {
        setupExoPlayer()
    }

    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val isBuffering = playbackState == Player.STATE_BUFFERING
                    val duration = if (duration > 0) duration else 0L

                    _uiState.update {
                        it.copy(
                            isBuffering = isBuffering,
                            durationMs = duration
                        )
                    }

                    if (playbackState == Player.STATE_READY) {
                        startProgressTracker()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = false,
                            errorMessage = error.message ?: "Audio playback error"
                        )
                    }
                }
            })
        }
    }

    /**
     * Initializes the player with selected track and immediately fetches stream URL from KMP backend.
     */
    fun loadAndPlay(videoId: String, title: String, artist: String, thumbnailUrl: String?) {
        // If already playing this track, don't re-trigger
        if (_uiState.value.videoId == videoId && exoPlayer?.playbackState != Player.STATE_IDLE) {
            return
        }

        _uiState.update {
            it.copy(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                isResolvingStream = true,
                errorMessage = null,
                currentPositionMs = 0L,
                durationMs = 0L
            )
        }

        viewModelScope.launch {
            repository.getStreamInfo(videoId)
                .onSuccess { streamInfo ->
                    _uiState.update { it.copy(isResolvingStream = false) }
                    playStreamUrl(streamInfo.url)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isResolvingStream = false,
                            errorMessage = throwable.message ?: "Failed to resolve stream URL from InnerTube"
                        )
                    }
                }
        }
    }

    private fun playStreamUrl(url: String) {
        val player = exoPlayer ?: return
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun onSeekStarted(targetFraction: Float) {
        val duration = _uiState.value.durationMs
        val targetMs = (targetFraction * duration).toLong()
        _uiState.update {
            it.copy(
                isUserSeeking = true,
                userSeekPositionMs = targetMs
            )
        }
    }

    fun onSeekProgress(targetFraction: Float) {
        val duration = _uiState.value.durationMs
        val targetMs = (targetFraction * duration).toLong()
        _uiState.update { it.copy(userSeekPositionMs = targetMs) }
    }

    fun onSeekFinished(targetFraction: Float) {
        val duration = _uiState.value.durationMs
        val targetMs = (targetFraction * duration).toLong()
        exoPlayer?.seekTo(targetMs)
        _uiState.update {
            it.copy(
                isUserSeeking = false,
                currentPositionMs = targetMs
            )
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (!_uiState.value.isUserSeeking) {
                        val current = player.currentPosition.coerceAtLeast(0L)
                        val total = if (player.duration > 0) player.duration else _uiState.value.durationMs
                        _uiState.update {
                            it.copy(
                                currentPositionMs = current,
                                durationMs = total
                            )
                        }
                    }
                }
                delay(300L) // Update every 300ms for smooth progress bar
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}

