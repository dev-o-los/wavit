package com.wavit.routes

import com.wavit.repository.MusicRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.homeRoutes() {
    get("/home") {
        val homeFeed = MusicRepository.getHome()
        call.respond(homeFeed)
    }

    get("/explore") {
        val explore = MusicRepository.getExplore()
        call.respond(mapOf("sections" to explore))
    }
}

