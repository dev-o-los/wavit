package com.wavit.models

import kotlinx.serialization.Serializable

/**
 * Extended song metadata fetched from YouTube's watch page.
 */
@Serializable
data class SongInfo(
    val videoId: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val releaseYear: String? = null,
    val releaseDate: String? = null,
    val views: String? = null,
    val likes: String? = null,
    val comments: String? = null,
    val description: String? = null
)

/**
 * Timed or plain lyrics entity.
 */
@Serializable
data class LyricLine(
    val timeMs: Long,
    val text: String
)

@Serializable
data class Lyrics(
    val videoId: String,
    val plainLyrics: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val source: String = "YouTube"
)

