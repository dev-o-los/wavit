package com.wavit.innertube

import com.wavit.models.StreamInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves directly streamable audio URLs from YouTube's InnerTube player endpoint.
 */
object StreamResolver {
    private val logger = LoggerFactory.getLogger(StreamResolver::class.java)

    private val cache = ConcurrentHashMap<String, CachedStream>()
    private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes

    private data class CachedStream(
        val streamInfo: StreamInfo,
        val timestamp: Long
    )

    /**
     * Resolves playable audio stream for a YouTube videoId.
     *
     * @param videoId The 11-character YouTube video ID.
     * @param maxBitrateKbps Optional bitrate cap (e.g., 128, 256). Defaults to Int.MAX_VALUE (highest available).
     */
    suspend fun resolve(videoId: String, maxBitrateKbps: Int = Int.MAX_VALUE): StreamInfo {
        // Check cache
        val cached = cache[videoId]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.streamInfo
        }

        // Try IOS client first (most reliable for direct audio streams with no cipher), then ANDROID_VR
        val clients = listOf("IOS", "ANDROID_VR", "WEB_REMIX")
        var lastError: Exception? = null

        for (client in clients) {
            try {
                val playerResponse = InnerTubeClient.player(videoId, client)
                val stream = extractAudioStream(videoId, playerResponse, maxBitrateKbps)
                if (stream != null) {
                    cache[videoId] = CachedStream(stream, System.currentTimeMillis())
                    logger.info("Resolved stream for {} via {} ({} kbps, {})", videoId, client, stream.bitrateKbps, stream.format)
                    return stream
                }
            } catch (e: Exception) {
                lastError = e
                logger.warn("Stream resolution attempt via {} failed for {}: {}", client, videoId, e.message)
            }
        }

        throw IllegalStateException("Failed to resolve stream for videoId '$videoId': ${lastError?.message ?: "No usable audio stream found"}")
    }

    private fun extractAudioStream(videoId: String, response: JsonObject, maxBitrateKbps: Int): StreamInfo? {
        val streamingData = response["streamingData"] as? JsonObject ?: return null
        val formats = (streamingData["adaptiveFormats"] as? JsonArray).orEmpty()

        val audioFormats = formats.mapNotNull { it as? JsonObject }
            .filter { format ->
                val mime = format.s("mimeType").orEmpty()
                mime.startsWith("audio/")
            }

        if (audioFormats.isEmpty()) return null

        // Pick format with highest bitrate <= maxBitrateKbps
        val candidates = audioFormats.mapNotNull { format ->
            val url = format.s("url") ?: return@mapNotNull null
            val bitrate = format.s("bitrate")?.toIntOrNull() ?: 0
            val kbps = bitrate / 1000
            val mime = format.s("mimeType").orEmpty()
            val formatName = when {
                "mp4" in mime || "m4a" in mime -> "m4a"
                "opus" in mime -> "opus"
                else -> "webm"
            }
            val durationSec = format.s("approxDurationMs")?.toLongOrNull()?.let { it / 1000 } ?: 0L
            val loudness = format.s("loudnessDb")?.toDoubleOrNull()

            StreamInfo(
                videoId = videoId,
                url = url,
                mimeType = mime,
                bitrateKbps = kbps,
                durationSeconds = durationSec,
                format = formatName,
                loudnessDb = loudness,
                expiresAtMs = System.currentTimeMillis() + CACHE_TTL_MS
            )
        }

        if (candidates.isEmpty()) return null

        val withinBudget = candidates.filter { it.bitrateKbps <= maxBitrateKbps }
        return withinBudget.maxByOrNull { it.bitrateKbps } ?: candidates.maxByOrNull { it.bitrateKbps }
    }

    private fun JsonObject.s(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull
}

