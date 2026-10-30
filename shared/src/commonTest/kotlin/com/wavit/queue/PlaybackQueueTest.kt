package com.wavit.queue

import com.wavit.models.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackQueueTest {

    private val song1 = Song(videoId = "vid1", title = "Blinding Lights", artist = "The Weeknd")
    private val song2 = Song(videoId = "vid2", title = "Save Your Tears", artist = "The Weeknd")
    private val song3 = Song(videoId = "vid3", title = "Starboy", artist = "The Weeknd")

    @Test
    fun testQueueInitializationAndNavigation() {
        val queue = PlaybackQueue(listOf(song1, song2, song3), startIndex = 0)
        assertEquals(song1, queue.currentSong)

        val next = queue.next()
        assertEquals(song2, next)

        val next2 = queue.next()
        assertEquals(song3, next2)

        val end = queue.next()
        assertNull(end)
    }

    @Test
    fun testPreviousTrackNavigation() {
        val queue = PlaybackQueue(listOf(song1, song2, song3), startIndex = 2)
        assertEquals(song3, queue.currentSong)

        val prev = queue.previous()
        assertEquals(song2, prev)

        val prev2 = queue.previous()
        assertEquals(song1, prev2)

        val start = queue.previous()
        assertNull(start)
    }

    @Test
    fun testRepeatModeAll() {
        val queue = PlaybackQueue(listOf(song1, song2), startIndex = 1)
        queue.setRepeatMode(RepeatMode.ALL)

        val wrapped = queue.next()
        assertEquals(song1, wrapped)
    }

    @Test
    fun testRepeatModeOne() {
        val queue = PlaybackQueue(listOf(song1, song2), startIndex = 0)
        queue.setRepeatMode(RepeatMode.ONE)

        val same = queue.next()
        assertEquals(song1, same)
    }

    @Test
    fun testPlayNext() {
        val queue = PlaybackQueue(listOf(song1, song3), startIndex = 0)
        queue.playNext(song2)

        val next = queue.next()
        assertEquals(song2, next)

        val next2 = queue.next()
        assertEquals(song3, next2)
    }

    @Test
    fun testAddToEnd() {
        val queue = PlaybackQueue(listOf(song1, song2), startIndex = 0)
        queue.addToEnd(song3)

        val snapshot = queue.snapshot()
        assertEquals(3, snapshot.items.size)
        assertEquals(song3, snapshot.items.last().song)
    }

    @Test
    fun testAutoplayAppending() {
        val queue = PlaybackQueue(listOf(song1), startIndex = 0)
        queue.appendAutoplayTracks(listOf(song2, song3))

        val snapshot = queue.snapshot()
        assertEquals(3, snapshot.items.size)
        assertTrue(snapshot.items[1].isAutoplay)
        assertTrue(snapshot.items[2].isAutoplay)
    }
}

