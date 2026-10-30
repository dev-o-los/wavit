package com.wavit.models

import kotlinx.serialization.Serializable

/**
 * Resolved audio stream information for playback.
 */
@Serializable
data class StreamInfo(
    val videoId: String,
    val url: String,
    val mimeType: String,
    val bitrateKbps: Int,
    val durationSeconds: Long = 0L,
    val format: String, // e.g., "m4a", "webm", "opus"
    val loudnessDb: Double? = null,
    val expiresAtMs: Long? = null
)

