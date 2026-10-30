package com.wavit.models

import kotlinx.serialization.Serializable

/**
 * Artist profile containing top songs, albums, singles, and related artists.
 */
@Serializable
data class ArtistPage(
    val browseId: String,
    val name: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val subscriberCountText: String? = null,
    val monthlyListenerCount: String? = null,
    val topSongs: List<Song> = emptyList(),
    val sections: List<HomeShelf> = emptyList()
)

