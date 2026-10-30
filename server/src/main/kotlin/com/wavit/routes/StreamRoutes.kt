package com.wavit.routes

import com.wavit.repository.MusicRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.streamRoutes() {
    route("/stream/{id}") {
        get {
            val videoId = call.parameters["id"]
            if (videoId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing video ID for stream"))
                return@get
            }
            val maxKbps = call.parameters["maxKbps"]?.toIntOrNull() ?: Int.MAX_VALUE
            try {
                val stream = MusicRepository.getStream(videoId, maxKbps)
                call.respond(stream)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Stream resolution failed")))
            }
        }
    }
}

