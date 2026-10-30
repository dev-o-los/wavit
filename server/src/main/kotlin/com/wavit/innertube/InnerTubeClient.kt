package com.wavit.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * InnerTube (youtubei) API client for Wavit.
 * Communicates with YouTube Music's backend endpoints using WEB_REMIX, IOS, and Android identities.
 */
object InnerTubeClient {
    private val logger = LoggerFactory.getLogger(InnerTubeClient::class.java)

    private const val MUSIC_BASE = "https://music.youtube.com/youtubei/v1"
    private const val YOUTUBE_BASE = "https://www.youtube.com/youtubei/v1"
    private const val SW_JS_DATA = "https://www.youtube.com/sw.js_data"

    private const val WEB_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
    private const val IOS_USER_AGENT =
        "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 20_000
        }
    }

    @Volatile
    private var visitorData: String? = null
    private val visitorDataMutex = Mutex()

    /**
     * Obtains or refreshes YouTube's visitor data token required to prevent 403 / bot detection.
     */
    suspend fun ensureVisitorData(refresh: Boolean = false): String? {
        if (!refresh && visitorData != null) return visitorData
        visitorDataMutex.withLock {
            if (!refresh && visitorData != null) return visitorData
            try {
                val responseText = httpClient.get(SW_JS_DATA) {
                    header("User-Agent", WEB_USER_AGENT)
                }.bodyAsText()

                val trimmed = responseText.substringAfter("\n", responseText.drop(5))
                val parsed = json.parseToJsonElement(trimmed)
                val token = findVisitorData(parsed)
                if (token != null) {
                    visitorData = token
                    logger.info("Minted fresh YouTube visitorData: {}", token)
                }
            } catch (e: Exception) {
                logger.warn("Failed to mint visitor data: {}", e.message)
            }
            return visitorData
        }
    }

    private fun findVisitorData(element: JsonElement): String? = when (element) {
        is JsonArray -> element.firstNotNullOfOrNull { findVisitorData(it) }
        is JsonPrimitive -> element.contentOrNull?.takeIf { Regex("""Cg[A-Za-z0-9_%-]{40,}""").matches(it) }
        else -> null
    }

    /**
     * Builds standard InnerTube request body with appropriate client context.
     */
    private suspend fun buildContext(
        clientName: String = "WEB_REMIX",
        clientVersion: String = "1.20250101.01.00",
        additionalBody: (kotlinx.serialization.json.JsonObjectBuilder.() -> Unit)? = null
    ): JsonObject {
        val visitor = ensureVisitorData()
        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", clientName)
                    put("clientVersion", clientVersion)
                    put("hl", "en")
                    put("gl", "US")
                    if (visitor != null) {
                        put("visitorData", visitor)
                    }
                }
            }
            additionalBody?.invoke(this)
        }
    }

    /**
     * POST request to music.youtube.com/youtubei/v1
     */
    private suspend fun postMusic(
        endpoint: String,
        additionalBody: (kotlinx.serialization.json.JsonObjectBuilder.() -> Unit)? = null
    ): JsonObject {
        val payload = buildContext("WEB_REMIX", "1.20250101.01.00", additionalBody)
        val response = httpClient.post("$MUSIC_BASE/$endpoint") {
            header("User-Agent", WEB_USER_AGENT)
            header("Referer", "https://music.youtube.com/")
            header("Origin", "https://music.youtube.com")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    /**
     * Search songs, albums, artists, or playlists.
     */
    suspend fun search(query: String, params: String? = null): JsonObject =
        postMusic("search") {
            put("query", query)
            params?.let { put("params", it) }
        }

    /**
     * Search typeahead suggestions.
     */
    suspend fun searchSuggestions(input: String): JsonObject =
        postMusic("music/get_search_suggestions") {
            put("input", input)
        }

    /**
     * Browse an entity (album, artist, playlist, or home feed).
     */
    suspend fun browse(browseId: String, params: String? = null): JsonObject =
        postMusic("browse") {
            put("browseId", browseId)
            params?.let { put("params", it) }
        }

    /**
     * Browse continuation for pagination.
     */
    suspend fun browseContinuation(token: String): JsonObject =
        postMusic("browse") {
            put("continuation", token)
        }

    /**
     * Watch queue & next radio tracks for a videoId.
     */
    suspend fun next(videoId: String): JsonObject =
        postMusic("next") {
            put("videoId", videoId)
            put("playlistId", "RDAMVM$videoId")
            put("isAudioOnly", true)
        }

    /**
     * Fetch player response for playback / streamingData formats.
     */
    suspend fun player(videoId: String, client: String = "IOS"): JsonObject {
        val (clientName, clientVersion, userAgent, baseUrl) = when (client.uppercase()) {
            "IOS" -> Quad("IOS", "21.26.4", IOS_USER_AGENT, YOUTUBE_BASE)
            "ANDROID_VR" -> Quad(
                "ANDROID_VR",
                "1.65.10",
                "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
                YOUTUBE_BASE
            )
            else -> Quad("WEB_REMIX", "1.20250101.01.00", WEB_USER_AGENT, MUSIC_BASE)
        }

        val payload = buildContext(clientName, clientVersion) {
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }

        val response = httpClient.post("$baseUrl/player") {
            header("User-Agent", userAgent)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    /**
     * Fetch timed caption transcript for lyrics.
     */
    suspend fun transcript(videoId: String): JsonObject =
        postMusic("get_transcript") {
            val bytes = byteArrayOf(10, videoId.toByteArray().size.toByte()) + videoId.toByteArray()
            put("params", Base64.getEncoder().encodeToString(bytes))
        }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

