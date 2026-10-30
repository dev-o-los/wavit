package com.wavit.repository

import com.wavit.models.Song
import com.wavit.models.StreamInfo

/**
 * Universal KMP Repository interface for Wavit.
 * Used by Android, Desktop, and iOS UI layers to communicate with the music backend.
 */
interface WavitRepository {
    /**
     * Search songs given a text query.
     */
    suspend fun searchSongs(query: String): Result<List<Song>>

    /**
     * Resolve playable audio stream URL and metadata for a given YouTube videoId.
     */
    suspend fun getStreamInfo(videoId: String): Result<StreamInfo>

    /**
     * Fetch complete song metadata for a track.
     */
    suspend fun getSong(videoId: String): Result<Song>
}

