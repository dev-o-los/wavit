package com.wavit.routes

import com.wavit.repository.MusicRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.albumRoutes() {
    route("/album/{id}") {
        get {
            val browseId = call.parameters["id"]
            if (browseId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing album browse ID"))
                return@get
            }
            val album = MusicRepository.getAlbum(browseId)
            call.respond(album)
        }
    }
}

