package com.wavit.routes

import com.wavit.models.Song
import com.wavit.queue.PlaybackQueue
import com.wavit.queue.RepeatMode
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class SetQueueRequest(val songs: List<Song>, val startIndex: Int = 0)

@Serializable
data class AddSongRequest(val song: Song, val playNext: Boolean = false)

@Serializable
data class ShuffleRequest(val enabled: Boolean)

@Serializable
data class RepeatRequest(val mode: RepeatMode)

// Singleton in-memory queue manager for Phase 1 backend testing
object GlobalQueue {
    val queue = PlaybackQueue()
}

fun Route.queueRoutes() {
    route("/queue") {
        get {
            call.respond(GlobalQueue.queue.snapshot())
        }

        post("/set") {
            val req = call.receive<SetQueueRequest>()
            GlobalQueue.queue.setQueue(req.songs, req.startIndex)
            call.respond(GlobalQueue.queue.snapshot())
        }

        post("/next") {
            val nextSong = GlobalQueue.queue.next()
            if (nextSong != null) {
                call.respond(mapOf("status" to "ok", "song" to nextSong, "state" to GlobalQueue.queue.snapshot()))
            } else {
                call.respond(HttpStatusCode.NoContent, mapOf("status" to "end_of_queue"))
            }
        }

        post("/previous") {
            val prevSong = GlobalQueue.queue.previous()
            if (prevSong != null) {
                call.respond(mapOf("status" to "ok", "song" to prevSong, "state" to GlobalQueue.queue.snapshot()))
            } else {
                call.respond(HttpStatusCode.NoContent, mapOf("status" to "start_of_queue"))
            }
        }

        post("/add") {
            val req = call.receive<AddSongRequest>()
            if (req.playNext) {
                GlobalQueue.queue.playNext(req.song)
            } else {
                GlobalQueue.queue.addToEnd(req.song)
            }
            call.respond(GlobalQueue.queue.snapshot())
        }

        post("/shuffle") {
            val req = call.receive<ShuffleRequest>()
            GlobalQueue.queue.setShuffle(req.enabled)
            call.respond(GlobalQueue.queue.snapshot())
        }

        post("/repeat") {
            val req = call.receive<RepeatRequest>()
            GlobalQueue.queue.setRepeatMode(req.mode)
            call.respond(GlobalQueue.queue.snapshot())
        }
    }
}

