package com.wavit.models

import kotlinx.serialization.Serializable

/**
 * Represents a credited artist with their display name and browse ID for YouTube Music.
 *
 * @property name The artist's visible name (e.g., "The Weeknd")
 * @property browseId The unique channel identifier (starts with "UC...") to fetch their profile
 */
@Serializable
data class ArtistRef(
    val name: String,
    val browseId: String? = null
)

