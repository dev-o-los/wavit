package com.wavit.models

import kotlinx.serialization.Serializable

/**
 * Core playable track entity for Wavit.
 * Contains all metadata necessary for display, playback, and queue management.
 */
@Serializable
data class Song(
    val videoId: String,
    val title: String,
    val artist: String,
    val artists: List<ArtistRef> = emptyList(),
    val albumId: String? = null,
    val albumName: String? = null,
    val thumbnailUrl: String? = null,
    val durationText: String? = null,
    val durationSeconds: Long = 0L,
    val isExplicit: Boolean = false,
    val isVideo: Boolean = false
) {
    /**
     * Helper to compute millisecond duration from durationText (e.g., "3:45" or "1:02:15").
     */
    val durationMillis: Long
        get() {
            if (durationSeconds > 0) return durationSeconds * 1000L
            val parts = durationText?.trim()?.split(":") ?: return 0L
            val numbers = parts.map { it.trim().toLongOrNull() ?: return 0L }
            return when (numbers.size) {
                2 -> (numbers[0] * 60 + numbers[1]) * 1000L
                3 -> (numbers[0] * 3600 + numbers[1] * 60 + numbers[2]) * 1000L
                else -> 0L
            }
        }
}

