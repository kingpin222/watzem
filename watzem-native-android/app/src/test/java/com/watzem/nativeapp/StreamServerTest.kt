package com.watzem.nativeapp

import com.watzem.nativeapp.data.model.StreamServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamServerTest {

    @Test
    fun testServerUrlBuilding_movie() {
        val server = StreamServer.DEFAULT_SERVER
        val movieUrl = server.buildUrl(tmdbId = 550, isTv = false)
        assertTrue(movieUrl.contains("550"))
        assertTrue(movieUrl.contains("cinesrc.st"))
        assertEquals("Server 1", server.name)
        assertEquals("Server 1", server.shortName)
    }

    @Test
    fun testServerUrlBuilding_tvWithSeasonEpisode() {
        val server = StreamServer.ALL_SERVERS.first { it.id == "server1" }
        val tvUrl = server.buildUrl(tmdbId = 1399, isTv = true, season = 3, episode = 9)
        assertTrue(tvUrl.contains("1399"))
        assertTrue(tvUrl.contains("s=3"))
        assertTrue(tvUrl.contains("e=9"))
    }

    @Test
    fun testAllServersConfigured() {
        assertEquals(4, StreamServer.ALL_SERVERS.size)
        // Verify all server names are generic (Server 1, Server 2, Server 3, Server 4)
        StreamServer.ALL_SERVERS.forEachIndexed { index, server ->
            assertEquals("Server ${index + 1}", server.name)
            assertEquals("Server ${index + 1}", server.shortName)
        }
        val server2 = StreamServer.ALL_SERVERS.first { it.id == "server2" }
        val tvUrl2 = server2.buildUrl(tmdbId = 1399, isTv = true, season = 2, episode = 4)
        assertTrue(tvUrl2.contains("cinesrc.st"))
        assertTrue(tvUrl2.contains("quality=1080p"))
        assertTrue(tvUrl2.contains("s=2"))
        assertTrue(tvUrl2.contains("e=4"))

        val server3 = StreamServer.ALL_SERVERS.first { it.id == "server3" }
        val movieUrl3 = server3.buildUrl(tmdbId = 550, isTv = false)
        assertTrue(movieUrl3.contains("cinesrc.st"))
        assertTrue(movieUrl3.contains("prioritize=true"))
        assertTrue(movieUrl3.contains("550"))

        val server4 = StreamServer.ALL_SERVERS.first { it.id == "server4" }
        val movieUrl4 = server4.buildUrl(tmdbId = 550, isTv = false)
        assertTrue(movieUrl4.contains("vidcore.org"))
        assertTrue(movieUrl4.contains("550"))
        val tvUrl4 = server4.buildUrl(tmdbId = 1399, isTv = true, season = 1, episode = 1)
        assertTrue(tvUrl4.contains("vidcore.org"))
        assertTrue(tvUrl4.contains("1399/1/1"))
    }
}
