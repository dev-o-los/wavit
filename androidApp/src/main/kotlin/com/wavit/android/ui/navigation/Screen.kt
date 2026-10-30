package com.wavit.android.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Navigation destination routes for Wavit Android.
 */
sealed class Screen(val route: String) {
    data object Search : Screen("search")

    data object Player : Screen("player/{videoId}?title={title}&artist={artist}&thumbnailUrl={thumbnailUrl}") {
        fun createRoute(
            videoId: String,
            title: String,
            artist: String,
            thumbnailUrl: String?
        ): String {
            val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            val encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8.toString())
            val encodedThumb = URLEncoder.encode(thumbnailUrl.orEmpty(), StandardCharsets.UTF_8.toString())
            return "player/$videoId?title=$encodedTitle&artist=$encodedArtist&thumbnailUrl=$encodedThumb"
        }

        fun decodeParam(value: String?): String =
            value?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }.orEmpty()
    }
}

