package com.wavit.routes

import com.wavit.repository.MusicRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.artistRoutes() {
    route("/artist/{id}") {
        get {
            val browseId = call.parameters["id"]
            if (browseId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing artist browse ID"))
                return@get
            }
            val artist = MusicRepository.getArtist(browseId)
            call.respond(artist)
        }
    }
}

