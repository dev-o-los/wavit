package com.wavit.routes

import com.wavit.models.SearchFilter
import com.wavit.repository.MusicRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class SuggestionsResponse(val query: String, val suggestions: List<String>)

fun Route.searchRoutes() {
    route("/search") {
        get {
            val query = call.parameters["q"]?.trim()
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing or empty query parameter 'q'"))
                return@get
            }
            val filterParam = call.parameters["filter"]
            val filter = SearchFilter.fromString(filterParam)
            val result = MusicRepository.search(query, filter)
            call.respond(result)
        }

        get("/suggestions") {
            val query = call.parameters["q"]?.trim()
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing or empty query parameter 'q'"))
                return@get
            }
            val suggestions = MusicRepository.searchSuggestions(query)
            call.respond(SuggestionsResponse(query = query, suggestions = suggestions))
        }
    }
}

