package com.wavit.queue

import com.wavit.models.Song
import kotlinx.serialization.Serializable

/**
 * Repeat mode behavior for the playback queue.
 */
@Serializable
enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

/**
 * An item in the queue with a unique tracking ID and origin indicator.
 */
@Serializable
data class QueueItem(
    val id: String,
    val song: Song,
    val isAutoplay: Boolean = false
)

/**
 * Snapshot of the current queue state.
 */
@Serializable
data class QueueState(
    val currentItem: QueueItem? = null,
    val currentIndex: Int = -1,
    val items: List<QueueItem> = emptyList(),
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffled: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

/**
 * Pure data-level Queue Manager for Wavit.
 * Platform-independent, testable, handles all track switching logic.
 */
class PlaybackQueue(
    initialSongs: List<Song> = emptyList(),
    startIndex: Int = 0
) {
    private val originalItems = mutableListOf<QueueItem>()
    private val activeItems = mutableListOf<QueueItem>()
    private var currentIndex: Int = -1
    private var repeatMode: RepeatMode = RepeatMode.OFF
    private var isShuffled: Boolean = false
    private var idCounter: Long = 0L

    init {
        if (initialSongs.isNotEmpty()) {
            setQueue(initialSongs, startIndex)
        }
    }

    private fun nextId(): String = "q_${++idCounter}"

    /**
     * Replace the current queue with a new list of songs.
     */
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        originalItems.clear()
        activeItems.clear()
        songs.forEach { song ->
            val item = QueueItem(id = nextId(), song = song, isAutoplay = false)
            originalItems.add(item)
            activeItems.add(item)
        }
        currentIndex = if (activeItems.isNotEmpty()) {
            startIndex.coerceIn(0, activeItems.size - 1)
        } else {
            -1
        }
    }

    /**
     * Current playing song or null.
     */
    val currentSong: Song?
        get() = activeItems.getOrNull(currentIndex)?.song

    val currentItem: QueueItem?
        get() = activeItems.getOrNull(currentIndex)

    /**
     * Move to next track according to queue and repeat mode.
     * @return The next Song, or null if end of queue reached.
     */
    fun next(): Song? {
        if (activeItems.isEmpty()) return null

        if (repeatMode == RepeatMode.ONE && currentIndex in activeItems.indices) {
            return activeItems[currentIndex].song
        }

        if (currentIndex + 1 < activeItems.size) {
            currentIndex++
            return activeItems[currentIndex].song
        }

        if (repeatMode == RepeatMode.ALL) {
            currentIndex = 0
            return activeItems[currentIndex].song
        }

        return null
    }

    /**
     * Move to previous track.
     * @return The previous Song, or null if at beginning.
     */
    fun previous(): Song? {
        if (activeItems.isEmpty()) return null

        if (repeatMode == RepeatMode.ONE && currentIndex in activeItems.indices) {
            return activeItems[currentIndex].song
        }

        if (currentIndex > 0) {
            currentIndex--
            return activeItems[currentIndex].song
        }

        if (repeatMode == RepeatMode.ALL && activeItems.isNotEmpty()) {
            currentIndex = activeItems.size - 1
            return activeItems[currentIndex].song
        }

        return null
    }

    /**
     * Insert a song to play immediately after the current song.
     */
    fun playNext(song: Song) {
        val item = QueueItem(id = nextId(), song = song, isAutoplay = false)
        val insertPos = if (currentIndex in activeItems.indices) currentIndex + 1 else activeItems.size
        activeItems.add(insertPos, item)
        originalItems.add(item)
    }

    /**
     * Append a song to the end of the queue.
     */
    fun addToEnd(song: Song) {
        val item = QueueItem(id = nextId(), song = song, isAutoplay = false)
        activeItems.add(item)
        originalItems.add(item)
    }

    /**
     * Append dynamic radio/recommendation songs (Autoplay tier).
     */
    fun appendAutoplayTracks(songs: List<Song>) {
        songs.forEach { song ->
            // Prevent duplicate insertion of currently queued tracks
            if (activeItems.none { it.song.videoId == song.videoId }) {
                val item = QueueItem(id = nextId(), song = song, isAutoplay = true)
                activeItems.add(item)
                originalItems.add(item)
            }
        }
    }

    /**
     * Toggle shuffle mode.
     */
    fun setShuffle(enabled: Boolean) {
        if (isShuffled == enabled) return
        isShuffled = enabled

        val current = currentItem
        if (enabled) {
            val remaining = activeItems.toMutableList()
            if (current != null) remaining.remove(current)
            remaining.shuffle()
            activeItems.clear()
            if (current != null) activeItems.add(current)
            activeItems.addAll(remaining)
            currentIndex = if (current != null) 0 else -1
        } else {
            // Restore original order
            activeItems.clear()
            activeItems.addAll(originalItems)
            currentIndex = if (current != null) activeItems.indexOfFirst { it.id == current.id } else -1
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        this.repeatMode = mode
    }

    /**
     * Get a current immutable snapshot of the queue state.
     */
    fun snapshot(): QueueState {
        val hasNext = when (repeatMode) {
            RepeatMode.ONE, RepeatMode.ALL -> activeItems.isNotEmpty()
            RepeatMode.OFF -> currentIndex + 1 < activeItems.size
        }
        val hasPrev = when (repeatMode) {
            RepeatMode.ONE, RepeatMode.ALL -> activeItems.isNotEmpty()
            RepeatMode.OFF -> currentIndex > 0
        }
        return QueueState(
            currentItem = currentItem,
            currentIndex = currentIndex,
            items = activeItems.toList(),
            repeatMode = repeatMode,
            isShuffled = isShuffled,
            hasNext = hasNext,
            hasPrevious = hasPrev
        )
    }
}

