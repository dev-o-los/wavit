package com.wavit.models

import kotlinx.serialization.Serializable

/**
 * Detailed representation of an album release.
 */
@Serializable
data class Album(
    val browseId: String,
    val title: String,
    val subtitle: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val year: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val tracks: List<Song> = emptyList(),
    val trackCount: Int = tracks.size
)

