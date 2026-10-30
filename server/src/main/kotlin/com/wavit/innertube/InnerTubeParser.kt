package com.wavit.innertube

import com.wavit.models.Album
import com.wavit.models.ArtistPage
import com.wavit.models.ArtistRef
import com.wavit.models.BrowseItem
import com.wavit.models.HomeShelf
import com.wavit.models.LyricLine
import com.wavit.models.Lyrics
import com.wavit.models.MoodGenre
import com.wavit.models.MoodGenreSection
import com.wavit.models.SearchResult
import com.wavit.models.ShelfItem
import com.wavit.models.Song
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parses raw InnerTube JSON envelopes into Wavit's clean domain models.
 */
object InnerTubeParser {

    // ---- Search Parsing -----------------------------------------------------

    fun parseSearch(query: String, response: JsonObject): SearchResult {
        val songs = mutableListOf<Song>()
        val albums = mutableListOf<BrowseItem>()
        val artists = mutableListOf<BrowseItem>()
        val playlists = mutableListOf<BrowseItem>()

        val renderers = collectRenderers(response, "musicResponsiveListItemRenderer")
        val seenSongs = HashSet<String>()
        val seenBrowse = HashSet<String>()

        renderers.forEach { renderer ->
            val browse = parseBrowseItem(renderer)
            if (browse != null) {
                if (seenBrowse.add(browse.browseId)) {
                    when (browse.type) {
                        "ALBUM" -> albums.add(browse)
                        "ARTIST" -> artists.add(browse)
                        "PLAYLIST" -> playlists.add(browse)
                    }
                }
            } else {
                val song = parseSongRow(renderer)
                if (song != null && seenSongs.add(song.videoId)) {
                    songs.add(song)
                }
            }
        }

        // Also check for cardShelfRenderer (promoted top result)
        val cards = collectRenderers(response, "musicCardShelfRenderer")
        cards.forEach { card ->
            parseCardShelfSong(card)?.let { song ->
                if (seenSongs.add(song.videoId)) {
                    songs.add(0, song)
                }
            }
            parseCardShelfBrowse(card)?.let { browse ->
                if (seenBrowse.add(browse.browseId)) {
                    when (browse.type) {
                        "ALBUM" -> albums.add(0, browse)
                        "ARTIST" -> artists.add(0, browse)
                        "PLAYLIST" -> playlists.add(0, browse)
                    }
                }
            }
        }

        val token = continuationToken(response)
        return SearchResult(
            query = query,
            songs = songs,
            albums = albums,
            artists = artists,
            playlists = playlists,
            continuationToken = token
        )
    }

    fun parseSearchSuggestions(response: JsonObject): List<String> =
        collectRenderers(response, "searchSuggestionRenderer")
            .mapNotNull { renderer ->
                val query = renderer.o("navigationEndpoint").o("searchEndpoint").s("query")
                    ?: renderer.o("suggestion").runs()
                query.takeIf { it.isNotBlank() }
            }
            .distinct()

    // ---- Song Row Parsing ---------------------------------------------------

    fun parseSongRow(renderer: JsonObject): Song? {
        val videoId = renderer.o("playlistItemData").s("videoId")
            ?: renderer.o("overlay")
                .o("musicItemThumbnailOverlayRenderer").o("content")
                .o("musicPlayButtonRenderer").o("playNavigationEndpoint")
                .o("watchEndpoint").s("videoId")
            ?: return null

        val columns = renderer.a("flexColumns").orEmpty()
        val title = columns.getOrNull(0)
            .o("musicResponsiveListItemFlexColumnRenderer").o("text").runs()
        if (title.isBlank()) return null

        val subtitleRuns = columns.getOrNull(1)
            .o("musicResponsiveListItemFlexColumnRenderer").o("text").a("runs").orEmpty()
        val subtitle = subtitleRuns.joinToString("") { it.s("text").orEmpty() }
        val parts = subtitle.split(" • ").filter { it.isNotBlank() }

        val duration = parts.lastOrNull()?.takeIf { it.matches(Regex("""\d+:\d{2}""")) }
            ?: renderer.a("fixedColumns").orEmpty().firstNotNullOfOrNull { column ->
                column.o("musicResponsiveListItemFixedColumnRenderer")
                    .o("text").runs().takeIf { it.matches(Regex("""\d+:\d{2}""")) }
            }

        val rowType = parts.firstOrNull { it.lowercase() in setOf("song", "video") }?.lowercase()
        val artist = parts.firstOrNull {
            !it.matches(Regex("""\d+:\d{2}""")) && it.lowercase() !in setOf("song", "video")
        } ?: "Unknown Artist"

        val credits = creditsOf(subtitleRuns)
        val thumbnails = renderer.o("thumbnail").o("musicThumbnailRenderer")
            .o("thumbnail").a("thumbnails")

        return Song(
            videoId = videoId,
            title = title,
            artist = credits.artistName ?: artist,
            artists = credits.artists,
            albumId = credits.albumId,
            albumName = credits.albumName,
            thumbnailUrl = thumbnails.best(),
            durationText = duration,
            isVideo = rowType == "video"
        )
    }

    private fun parseCardShelfSong(renderer: JsonObject): Song? {
        val videoId = renderer.o("onTap").o("watchEndpoint").s("videoId") ?: return null
        val title = renderer.o("title").runs()
        if (title.isBlank()) return null
        val subtitleRuns = renderer.o("subtitle").a("runs").orEmpty()
        val subtitle = subtitleRuns.joinToString("") { it.s("text").orEmpty() }
        val parts = subtitle.split(" • ").filter { it.isNotBlank() }
        val duration = parts.lastOrNull()?.takeIf { it.matches(Regex("""\d+:\d{2}""")) }
        val credits = creditsOf(subtitleRuns)

        return Song(
            videoId = videoId,
            title = title,
            artist = credits.artistName ?: parts.getOrNull(0) ?: "Unknown Artist",
            artists = credits.artists,
            albumId = credits.albumId,
            albumName = credits.albumName,
            thumbnailUrl = renderer.o("thumbnail").o("musicThumbnailRenderer")
                .o("thumbnail").a("thumbnails").best(),
            durationText = duration
        )
    }

    private fun parseCardShelfBrowse(renderer: JsonObject): BrowseItem? {
        val endpoint = renderer.o("onTap").o("browseEndpoint") ?: return null
        val browseId = endpoint.s("browseId") ?: return null
        val pageType = endpoint.o("browseEndpointContextSupportedConfigs")
            .o("browseEndpointContextMusicConfig").s("pageType").orEmpty()
        val title = renderer.o("title").runs()
        if (title.isBlank()) return null
        val subtitle = renderer.o("subtitle").runs()

        return BrowseItem(
            browseId = browseId,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = renderer.o("thumbnail").o("musicThumbnailRenderer")
                .o("thumbnail").a("thumbnails").best(),
            type = when {
                "ALBUM" in pageType -> "ALBUM"
                "ARTIST" in pageType -> "ARTIST"
                "PLAYLIST" in pageType -> "PLAYLIST"
                else -> "OTHER"
            }
        )
    }

    private fun parseBrowseItem(renderer: JsonObject): BrowseItem? {
        val endpoint = renderer.o("navigationEndpoint").o("browseEndpoint") ?: return null
        val browseId = endpoint.s("browseId") ?: return null
        val pageType = endpoint.o("browseEndpointContextSupportedConfigs")
            .o("browseEndpointContextMusicConfig").s("pageType").orEmpty()

        val columns = renderer.a("flexColumns").orEmpty()
        val title = columns.getOrNull(0)
            .o("musicResponsiveListItemFlexColumnRenderer").o("text").runs()
        if (title.isBlank()) return null

        val subtitle = columns.getOrNull(1)
            .o("musicResponsiveListItemFlexColumnRenderer").o("text").runs()

        return BrowseItem(
            browseId = browseId,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = renderer.o("thumbnail").o("musicThumbnailRenderer")
                .o("thumbnail").a("thumbnails").best(),
            type = when {
                "ALBUM" in pageType -> "ALBUM"
                "ARTIST" in pageType -> "ARTIST"
                "PLAYLIST" in pageType -> "PLAYLIST"
                else -> "OTHER"
            }
        )
    }

    // ---- Home Feed Parsing --------------------------------------------------

    fun parseHome(response: JsonObject): List<HomeShelf> {
        val sections = response.o("contents")
            .o("singleColumnBrowseResultsRenderer").a("tabs")?.firstOrNull()
            .o("tabRenderer").o("content").o("sectionListRenderer").a("contents")
            .orEmpty()

        return sections.mapNotNull { section ->
            section.o("musicCarouselShelfRenderer")?.let(::parseCarouselShelf)
                ?: section.o("musicShelfRenderer")?.let(::parsePlainShelf)
        }
    }

    private fun parseCarouselShelf(carousel: JsonObject): HomeShelf? {
        val header = carousel.o("header").o("musicCarouselShelfBasicHeaderRenderer")
        val title = header.o("title").runs()
        val strapline = header.o("strapline").runs()
        val items = carousel.a("contents").orEmpty().mapNotNull { item ->
            parseTwoRowItem(item.o("musicTwoRowItemRenderer"))
                ?: parseSongRow(item.o("musicResponsiveListItemRenderer") ?: JsonObject(emptyMap()))?.let { song ->
                    ShelfItem(song.title, song.artist, song.thumbnailUrl, song.videoId, null)
                }
        }
        return if (items.isEmpty()) null else HomeShelf(title.ifBlank { "Featured" }, strapline, items)
    }

    private fun parsePlainShelf(shelf: JsonObject): HomeShelf? {
        val title = shelf.o("title").runs()
        val items = shelf.a("contents").orEmpty().mapNotNull {
            parseSongRow(it.o("musicResponsiveListItemRenderer") ?: JsonObject(emptyMap()))
        }.map { ShelfItem(it.title, it.artist, it.thumbnailUrl, it.videoId, null) }
        return if (items.isEmpty()) null else HomeShelf(title.ifBlank { "For You" }, null, items)
    }

    private fun parseTwoRowItem(renderer: JsonObject?): ShelfItem? {
        if (renderer == null) return null
        val title = renderer.o("title").runs()
        if (title.isBlank()) return null
        val endpoint = renderer.o("navigationEndpoint")
        val browseId = endpoint.o("browseEndpoint").s("browseId")
        val videoId = endpoint.o("watchEndpoint").s("videoId")
            ?: browseId?.takeIf { it.startsWith("MPED") }?.removePrefix("MPED")
        val resolvedBrowseId = browseId?.takeUnless { it.startsWith("MPED") }
        val thumbnails = renderer.o("thumbnailRenderer").o("musicThumbnailRenderer")
            .o("thumbnail").a("thumbnails")
        val subtitle = renderer.o("subtitle").runs()

        return ShelfItem(
            title = title,
            subtitle = subtitle,
            thumbnailUrl = thumbnails.best(),
            videoId = videoId,
            browseId = resolvedBrowseId
        )
    }

    // ---- Moods & Genres Parsing ---------------------------------------------

    fun parseMoodAndGenres(response: JsonObject): List<MoodGenreSection> {
        val sections = response.o("contents")
            .o("singleColumnBrowseResultsRenderer").a("tabs")?.firstOrNull()
            .o("tabRenderer").o("content").o("sectionListRenderer").a("contents")
            .orEmpty()

        return sections.mapNotNull { section ->
            val grid = section.o("gridRenderer") ?: return@mapNotNull null
            val title = grid.o("header").o("gridHeaderRenderer").o("title").runs()
            val items = grid.a("items").orEmpty().mapNotNull { item ->
                val button = item.o("musicNavigationButtonRenderer") ?: return@mapNotNull null
                val endpoint = button.o("clickCommand").o("browseEndpoint")
                    ?: button.o("navigationEndpoint").o("browseEndpoint")
                    ?: return@mapNotNull null
                val browseId = endpoint.s("browseId") ?: return@mapNotNull null
                val label = button.o("buttonText").runs()
                if (label.isBlank()) return@mapNotNull null
                MoodGenre(title = label, browseId = browseId, params = endpoint.s("params"))
            }
            if (title.isBlank() || items.isEmpty()) null else MoodGenreSection(title, items)
        }
    }

    // ---- Album Details Parsing ----------------------------------------------

    fun parseAlbum(browseId: String, response: JsonObject): Album {
        val header = collectRenderers(response, "musicResponsiveHeaderRenderer").firstOrNull()
            ?: collectRenderers(response, "musicDetailHeaderRenderer").firstOrNull()

        val title = header?.o("title").runs().ifBlank { "Album" }
        val subtitleRuns = header?.o("subtitle").a("runs").orEmpty()
        val subtitle = subtitleRuns.joinToString("") { it.s("text").orEmpty() }
        val parts = subtitle.split(" • ", " · ").map { it.trim() }

        val artist = parts.firstOrNull { it != "Album" && it != "Single" && it != "EP" && !it.matches(Regex("""\d{4}""")) }
        val year = parts.firstOrNull { it.matches(Regex("""\d{4}""")) }
        val thumbnailUrl = collectRenderers(header ?: response, "musicThumbnailRenderer")
            .firstOrNull().o("thumbnail").a("thumbnails").best()

        val description = collectRenderers(response, "musicDescriptionShelfRenderer")
            .firstOrNull()?.o("description").runs()

        // Extract tracks
        val tracks = collectSongsDeep(response)

        return Album(
            browseId = browseId,
            title = title,
            subtitle = subtitle,
            artist = artist,
            year = year,
            thumbnailUrl = thumbnailUrl,
            description = description,
            tracks = tracks
        )
    }

    // ---- Artist Page Parsing ------------------------------------------------

    fun parseArtistPage(browseId: String, response: JsonObject): ArtistPage {
        val header = response.o("header")
        val name = header.o("musicImmersiveHeaderRenderer").o("title").runs()
            .ifBlank { header.o("musicVisualHeaderRenderer").o("title").runs() }
            .ifBlank { "Artist" }

        val thumbnailUrl = header.o("musicImmersiveHeaderRenderer").o("thumbnail")
            .o("musicThumbnailRenderer").o("thumbnail").a("thumbnails").best()
            ?: header.o("musicVisualHeaderRenderer").o("foregroundThumbnail")
                .o("musicThumbnailRenderer").o("thumbnail").a("thumbnails").best()

        val description = collectRenderers(response, "musicDescriptionShelfRenderer")
            .firstOrNull()?.o("description").runs()

        val topSongs = mutableListOf<Song>()
        val sections = mutableListOf<HomeShelf>()

        val rawSections = response.o("contents")
            .o("singleColumnBrowseResultsRenderer").a("tabs")?.firstOrNull()
            .o("tabRenderer").o("content").o("sectionListRenderer").a("contents")
            .orEmpty()

        rawSections.forEach { section ->
            section.o("musicShelfRenderer")?.let { shelf ->
                shelf.a("contents").orEmpty().forEach { row ->
                    parseSongRow(row.o("musicResponsiveListItemRenderer") ?: JsonObject(emptyMap()))?.let {
                        topSongs.add(it)
                    }
                }
            }
            section.o("musicCarouselShelfRenderer")?.let { carousel ->
                parseCarouselShelf(carousel)?.let { sections.add(it) }
            }
        }

        return ArtistPage(
            browseId = browseId,
            name = name,
            description = description,
            thumbnailUrl = thumbnailUrl,
            topSongs = topSongs,
            sections = sections
        )
    }

    // ---- Watch Queue & Radio Parsing ----------------------------------------

    fun parseWatchQueue(response: JsonObject): List<Song> {
        val out = mutableListOf<Song>()
        collectRenderers(response, "playlistPanelVideoRenderer").forEach { renderer ->
            val videoId = renderer.s("videoId") ?: return@forEach
            val title = renderer.o("title").runs()
            if (title.isBlank()) return@forEach
            val bylineRuns = renderer.o("longBylineText").a("runs").orEmpty()
            val byline = bylineRuns.map { it.s("text").orEmpty() }
            val artist = byline.takeWhile { !it.contains("•") }.joinToString("").trim()
            val credits = creditsOf(bylineRuns)

            out.add(
                Song(
                    videoId = videoId,
                    title = title,
                    artist = if (artist.isNotBlank()) artist else "Unknown Artist",
                    artists = credits.artists,
                    albumId = credits.albumId,
                    albumName = credits.albumName,
                    thumbnailUrl = renderer.o("thumbnail").a("thumbnails").best(),
                    durationText = renderer.o("lengthText").runs()
                )
            )
        }
        return out
    }

    // ---- Transcript Parsing (Lyrics) ---------------------------------------

    fun parseTranscript(videoId: String, response: JsonObject): Lyrics {
        val lines = mutableListOf<LyricLine>()
        collectRenderers(response, "transcriptCueGroupRenderer").forEach { cueGroup ->
            val renderer = cueGroup.o("transcriptCueRenderer")
            val text = renderer.o("cue").runs()
            val startMs = renderer.s("startOffsetMs")?.toLongOrNull() ?: 0L
            if (text.isNotBlank()) {
                lines.add(LyricLine(timeMs = startMs, text = text))
            }
        }
        val plain = lines.joinToString("\n") { it.text }
        return Lyrics(
            videoId = videoId,
            plainLyrics = plain.ifBlank { null },
            syncedLyrics = lines,
            source = "YouTube Transcript"
        )
    }

    // ---- Helper Utilities ---------------------------------------------------

    private fun collectSongsDeep(root: JsonElement): List<Song> {
        val out = mutableListOf<Song>()
        val seen = HashSet<String>()
        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    node["musicResponsiveListItemRenderer"]?.let { renderer ->
                        (renderer as? JsonObject)?.let {
                            parseSongRow(it)?.let { song ->
                                if (seen.add(song.videoId)) out.add(song)
                            }
                        }
                    }
                    node.values.forEach(::walk)
                }
                is JsonArray -> node.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return out
    }

    private data class ParsedCredits(
        val artistId: String? = null,
        val artistName: String? = null,
        val artists: List<ArtistRef> = emptyList(),
        val albumId: String? = null,
        val albumName: String? = null
    )

    private fun creditsOf(runs: List<JsonElement>): ParsedCredits {
        var artistId: String? = null
        var artistName: String? = null
        var albumId: String? = null
        var albumName: String? = null
        val artists = mutableListOf<ArtistRef>()

        runs.forEach { run ->
            val browse = run.o("navigationEndpoint").o("browseEndpoint")
            val id = browse.s("browseId") ?: return@forEach
            val pageType = browse.o("browseEndpointContextSupportedConfigs")
                .o("browseEndpointContextMusicConfig").s("pageType").orEmpty()
            val text = run.s("text").orEmpty()

            if ("ARTIST" in pageType) {
                if (text.isNotBlank()) artists.add(ArtistRef(name = text, browseId = id))
                if (artistId == null) {
                    artistId = id
                    artistName = text
                }
            } else if ("ALBUM" in pageType && albumId == null) {
                albumId = id
                albumName = text
            }
        }
        return ParsedCredits(artistId, artistName, artists, albumId, albumName)
    }

    fun continuationToken(root: JsonElement): String? {
        val endpoint = collectRenderers(root, "continuationItemRenderer").firstOrNull()
            .o("continuationEndpoint")
        return endpoint.o("continuationCommand").s("token")
            ?: endpoint.o("commandExecutorCommand").a("commands")
                ?.firstNotNullOfOrNull { it.o("continuationCommand") }?.s("token")
    }

    private fun collectRenderers(root: JsonElement, name: String): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> {
                    (node[name] as? JsonObject)?.let(out::add)
                    node.values.forEach(::walk)
                }
                is JsonArray -> node.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return out
    }
}

// ---- Mini JSON Extensions ---------------------------------------------------

private fun JsonElement?.o(key: String): JsonObject? =
    (this as? JsonObject)?.get(key) as? JsonObject

private fun JsonElement?.a(key: String): JsonArray? =
    (this as? JsonObject)?.get(key) as? JsonArray

private fun JsonElement?.s(key: String): String? =
    ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull

private fun JsonElement?.runs(): String =
    this.a("runs")?.joinToString("") { it.s("text").orEmpty() }.orEmpty()

private fun JsonArray?.best(): String? = this?.lastOrNull().s("url")

