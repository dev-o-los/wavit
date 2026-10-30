package com.wavit.repository

import com.wavit.innertube.InnerTubeClient
import com.wavit.innertube.InnerTubeParser
import com.wavit.innertube.StreamResolver
import com.wavit.models.Album
import com.wavit.models.ArtistPage
import com.wavit.models.HomeFeed
import com.wavit.models.Lyrics
import com.wavit.models.MoodGenreSection
import com.wavit.models.SearchFilter
import com.wavit.models.SearchResult
import com.wavit.models.Song
import com.wavit.models.SongInfo
import com.wavit.models.StreamInfo
import org.slf4j.LoggerFactory

/**
 * Main application repository coordinating InnerTube data calls, parsing, and streaming.
 */
object MusicRepository {
    private val logger = LoggerFactory.getLogger(MusicRepository::class.java)

    /**
     * Search songs, albums, artists, or playlists.
     */
    suspend fun search(query: String, filter: SearchFilter = SearchFilter.ALL): SearchResult {
        logger.info("Executing search: query='{}', filter={}", query, filter)
        val response = InnerTubeClient.search(query, filter.params)
        return InnerTubeParser.parseSearch(query, response).copy(filter = filter)
    }

    /**
     * Get search typeahead suggestions.
     */
    suspend fun searchSuggestions(input: String): List<String> {
        val response = InnerTubeClient.searchSuggestions(input)
        return InnerTubeParser.parseSearchSuggestions(response)
    }

    /**
     * Get full song metadata and watch entry links.
     */
    suspend fun getSong(videoId: String): Song {
        logger.info("Fetching song details for videoId: {}", videoId)
        val nextResponse = InnerTubeClient.next(videoId)
        val songs = InnerTubeParser.parseWatchQueue(nextResponse)
        val matched = songs.firstOrNull { it.videoId == videoId }
        if (matched != null) return matched

        // Fallback: search for the videoId
        val searchResult = search(videoId, SearchFilter.SONGS)
        return searchResult.songs.firstOrNull { it.videoId == videoId }
            ?: Song(videoId = videoId, title = "Unknown Track", artist = "Unknown Artist")
    }

    /**
     * Get related songs / radio mix for autoplay queue extension.
     */
    suspend fun getRelatedSongs(videoId: String): List<Song> {
        logger.info("Fetching related/radio songs for videoId: {}", videoId)
        val response = InnerTubeClient.next(videoId)
        return InnerTubeParser.parseWatchQueue(response).filterNot { it.videoId == videoId }
    }

    /**
     * Get album details, track listing, and cover art.
     */
    suspend fun getAlbum(browseId: String): Album {
        logger.info("Fetching album details for browseId: {}", browseId)
        val response = InnerTubeClient.browse(browseId)
        return InnerTubeParser.parseAlbum(browseId, response)
    }

    /**
     * Get artist profile, top songs, and discography sections.
     */
    suspend fun getArtist(browseId: String): ArtistPage {
        logger.info("Fetching artist profile for browseId: {}", browseId)
        val response = InnerTubeClient.browse(browseId)
        return InnerTubeParser.parseArtistPage(browseId, response)
    }

    /**
     * Fetch stream URL and metadata.
     */
    suspend fun getStream(videoId: String, maxKbps: Int = Int.MAX_VALUE): StreamInfo {
        logger.info("Resolving stream for videoId: {} (maxKbps: {})", videoId, maxKbps)
        return StreamResolver.resolve(videoId, maxKbps)
    }

    /**
     * Fetch extended song info (views, likes, release date).
     */
    suspend fun getSongInfo(videoId: String): SongInfo {
        return SongInfoRepository.getSongInfo(videoId)
    }

    /**
     * Fetch Home feed shelves.
     */
    suspend fun getHome(): HomeFeed {
        logger.info("Fetching home feed")
        val response = InnerTubeClient.browse("FEmusic_home")
        val shelves = InnerTubeParser.parseHome(response)
        val token = InnerTubeParser.continuationToken(response)
        return HomeFeed(shelves, token)
    }

    /**
     * Fetch Explore moods and genres.
     */
    suspend fun getExplore(): List<MoodGenreSection> {
        logger.info("Fetching explore moods and genres")
        val response = InnerTubeClient.browse("FEmusic_moods_and_genres")
        return InnerTubeParser.parseMoodAndGenres(response)
    }

    /**
     * Fetch lyrics if available.
     */
    suspend fun getLyrics(videoId: String): Lyrics {
        logger.info("Fetching lyrics transcript for videoId: {}", videoId)
        return try {
            val response = InnerTubeClient.transcript(videoId)
            InnerTubeParser.parseTranscript(videoId, response)
        } catch (e: Exception) {
            logger.warn("No transcript available for {}: {}", videoId, e.message)
            Lyrics(videoId = videoId, plainLyrics = null, syncedLyrics = emptyList())
        }
    }
}

