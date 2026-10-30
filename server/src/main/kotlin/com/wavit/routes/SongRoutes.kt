package com.wavit.routes

import com.wavit.repository.MusicRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.songRoutes() {
    route("/song/{id}") {
        get {
            val videoId = call.parameters["id"]
            if (videoId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing song ID"))
                return@get
            }
            val song = MusicRepository.getSong(videoId)
            call.respond(song)
        }

        get("/info") {
            val videoId = call.parameters["id"]
            if (videoId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing song ID"))
                return@get
            }
            val info = MusicRepository.getSongInfo(videoId)
            call.respond(info)
        }

        get("/related") {
            val videoId = call.parameters["id"]
            if (videoId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing song ID"))
                return@get
            }
            val related = MusicRepository.getRelatedSongs(videoId)
            call.respond(mapOf("tracks" to related))
        }

        get("/lyrics") {
            val videoId = call.parameters["id"]
            if (videoId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing song ID"))
                return@get
            }
            val lyrics = MusicRepository.getLyrics(videoId)
            call.respond(lyrics)
        }
    }
}

