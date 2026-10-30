package com.wavit

import com.wavit.routes.albumRoutes
import com.wavit.routes.artistRoutes
import com.wavit.routes.homeRoutes
import com.wavit.routes.queueRoutes
import com.wavit.routes.searchRoutes
import com.wavit.routes.songRoutes
import com.wavit.routes.streamRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val logger = LoggerFactory.getLogger("WavitServer")
    logger.info("Starting Wavit Music Server on port {}", port)

    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

    install(CallLogging)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val logger = LoggerFactory.getLogger("ExceptionHandler")
            logger.error("Unhandled request exception: {}", cause.message, cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to "error",
                    "message" to (cause.message ?: "Internal server error")
                )
            )
        }
    }

    routing {
        get("/") {
            call.respond(
                mapOf(
                    "service" to "Wavit Music Backend API",
                    "version" to "1.0.0",
                    "status" to "healthy",
                    "endpoints" to listOf(
                        "GET /search?q={query}&filter={all|songs|albums|artists|playlists}",
                        "GET /search/suggestions?q={query}",
                        "GET /song/{id}",
                        "GET /song/{id}/info",
                        "GET /song/{id}/related",
                        "GET /song/{id}/lyrics",
                        "GET /album/{id}",
                        "GET /artist/{id}",
                        "GET /stream/{id}?maxKbps={ceiling}",
                        "GET /home",
                        "GET /explore",
                        "GET /queue",
                        "POST /queue/set",
                        "POST /queue/next",
                        "POST /queue/previous",
                        "POST /queue/add",
                        "POST /queue/shuffle",
                        "POST /queue/repeat"
                    )
                )
            )
        }

        searchRoutes()
        songRoutes()
        albumRoutes()
        artistRoutes()
        streamRoutes()
        homeRoutes()
        queueRoutes()
    }
}

