package com.wavit.repository

import com.wavit.models.SearchResult
import com.wavit.models.Song
import com.wavit.models.StreamInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Default implementation of [WavitRepository] that communicates with the Wavit Ktor server.
 *
 * @param baseUrl Base URL of the Wavit REST API (defaults to 10.0.2.2:8080 for Android Emulator).
 */
class WavitApiRepository(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val httpClient: HttpClient = defaultHttpClient
) : WavitRepository {

    companion object {
        /** Default loopback IP for standard Android Studio Emulator */
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

        val defaultHttpClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 20_000
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 15_000
                }
            }
        }
    }

    override suspend fun searchSongs(query: String): Result<List<Song>> = runCatching {
        val result: SearchResult = httpClient.get("$baseUrl/search") {
            parameter("q", query)
            parameter("filter", "songs")
        }.body()
        result.songs
    }

    override suspend fun getStreamInfo(videoId: String): Result<StreamInfo> = runCatching {
        httpClient.get("$baseUrl/stream/$videoId").body()
    }

    override suspend fun getSong(videoId: String): Result<Song> = runCatching {
        httpClient.get("$baseUrl/song/$videoId").body()
    }
}

