package com.wavit.models

import kotlinx.serialization.Serializable

/**
 * Filter categories supported for search queries.
 */
@Serializable
enum class SearchFilter(val params: String?) {
    ALL(null),
    SONGS("EgWKAQIIAWoKEAkQChAFEAMQBA=="),
    VIDEOS("EgWKAQIQAWoKEAkQChAFEAMQBA=="),
    ALBUMS("EgWKAQIYAWoKEAkQChAFEAMQBA=="),
    ARTISTS("EgWKAQIgAWoKEAkQChAFEAMQBA=="),
    PLAYLISTS("EgWKAQIoAWoKEAkQChAFEAMQBA==");

    companion object {
        fun fromString(value: String?): SearchFilter = when (value?.lowercase()) {
            "songs", "song" -> SONGS
            "videos", "video" -> VIDEOS
            "albums", "album" -> ALBUMS
            "artists", "artist" -> ARTISTS
            "playlists", "playlist" -> PLAYLISTS
            else -> ALL
        }
    }
}

/**
 * A non-track search or browse item (album, artist, playlist).
 */
@Serializable
data class BrowseItem(
    val browseId: String,
    val title: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
    val type: String // "ALBUM", "ARTIST", "PLAYLIST", "OTHER"
)

/**
 * Composite search result containing both songs and browse items.
 */
@Serializable
data class SearchResult(
    val query: String,
    val filter: SearchFilter = SearchFilter.ALL,
    val songs: List<Song> = emptyList(),
    val albums: List<BrowseItem> = emptyList(),
    val artists: List<BrowseItem> = emptyList(),
    val playlists: List<BrowseItem> = emptyList(),
    val continuationToken: String? = null
)

