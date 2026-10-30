package com.wavit.repository

import com.wavit.innertube.InnerTubeClient
import com.wavit.models.SongInfo
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Scrapes detailed song info (views, likes, release date, description) from YouTube's watch page.
 */
object SongInfoRepository {
    private val logger = LoggerFactory.getLogger(SongInfoRepository::class.java)
    private val cache = ConcurrentHashMap<String, SongInfo>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getSongInfo(videoId: String): SongInfo {
        cache[videoId]?.let { return it }

        return try {
            val html = InnerTubeClient.httpClient.get("https://www.youtube.com/watch?v=$videoId") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                header("Cookie", "SOCS=CAI") // Cookie consent bypass
            }.bodyAsText()

            val doc = Jsoup.parse(html)
            val scripts = doc.select("script").map { it.data() }

            val playerScript = scripts.firstNotNullOfOrNull { extractJson(it, "ytInitialPlayerResponse") }
            val videoDetails = playerScript?.get("videoDetails") as? JsonObject

            val title = videoDetails?.s("title")
            val artist = videoDetails?.s("author")
            val views = videoDetails?.s("viewCount")?.let { formatCount(it) }
            val description = videoDetails?.s("shortDescription")

            // Release date extraction from shortDescription
            val releaseDate = description?.lineSequence()?.firstNotNullOfOrNull { line ->
                Regex("""Released on:\s*(\d{4}-\d{2}-\d{2})""", RegexOption.IGNORE_CASE)
                    .find(line.trim())?.groupValues?.get(1)
            }

            // Extract initialData for likes/comments
            val dataScript = scripts.firstNotNullOfOrNull { extractJson(it, "ytInitialData") }
            val primary = dataScript?.findFirst("videoPrimaryInfoRenderer")
            val likes = primary?.findFirst("likeButtonViewModel")?.s("title")
                ?: primary?.findFirst("toggleButtonRenderer")?.o("defaultText")?.s("simpleText")

            val info = SongInfo(
                videoId = videoId,
                title = title,
                artist = artist,
                views = views,
                likes = likes?.let { formatCount(it) },
                releaseDate = releaseDate,
                description = description
            )
            cache[videoId] = info
            info
        } catch (e: Exception) {
            logger.warn("Failed to scrape song info for {}: {}", videoId, e.message)
            SongInfo(videoId = videoId)
        }
    }

    private fun extractJson(script: String, varName: String): JsonObject? {
        val pattern = Regex("""(?:var\s+)?$varName\s*=\s*""")
        val match = pattern.find(script) ?: return null
        val start = match.range.last + 1
        if (script.getOrNull(start) != '{') return null

        var depth = 0
        var quoted = false
        var escaped = false

        for (i in start until script.length) {
            val c = script[i]
            if (quoted) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') quoted = false
            } else when (c) {
                '"' -> quoted = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return runCatching {
                            json.parseToJsonElement(script.substring(start, i + 1)) as? JsonObject
                        }.getOrNull()
                    }
                }
            }
        }
        return null
    }

    private fun formatCount(raw: String): String {
        val n = raw.replace(",", "").toLongOrNull() ?: return raw
        return when {
            n >= 1_000_000_000 -> String.format("%.1fB", n / 1_000_000_000.0)
            n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
            n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
            else -> n.toString()
        }
    }

    private fun JsonElement.findFirst(key: String): JsonObject? = when (this) {
        is JsonObject -> this[key] as? JsonObject ?: values.firstNotNullOfOrNull { it.findFirst(key) }
        is JsonArray -> firstNotNullOfOrNull { it.findFirst(key) }
        else -> null
    }

    private fun JsonObject.s(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.o(key: String): JsonObject? = this[key] as? JsonObject
}

