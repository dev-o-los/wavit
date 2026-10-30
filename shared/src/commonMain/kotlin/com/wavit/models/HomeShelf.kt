package com.wavit.models

import kotlinx.serialization.Serializable

/**
 * An individual item inside a home or browse carousel.
 */
@Serializable
data class ShelfItem(
    val title: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
    val videoId: String? = null,
    val browseId: String? = null
)

/**
 * A shelf or carousel section on the Home or Explore feeds.
 */
@Serializable
data class HomeShelf(
    val title: String,
    val subtitle: String? = null,
    val items: List<ShelfItem> = emptyList(),
    val moreBrowseId: String? = null,
    val moreParams: String? = null
)

/**
 * Home feed wrapper containing all top-level shelves and continuation.
 */
@Serializable
data class HomeFeed(
    val shelves: List<HomeShelf> = emptyList(),
    val continuation: String? = null
)

/**
 * A mood or genre section for the Explore view.
 */
@Serializable
data class MoodGenre(
    val title: String,
    val browseId: String,
    val params: String? = null,
    val thumbnailUrl: String? = null
)

@Serializable
data class MoodGenreSection(
    val title: String,
    val items: List<MoodGenre> = emptyList()
)

