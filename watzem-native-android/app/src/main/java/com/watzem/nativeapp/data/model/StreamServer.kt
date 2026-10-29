package com.watzem.nativeapp.data.model

import android.content.Context

data class StreamServer(
    val id: String,
    val name: String,
    val shortName: String,
    val badge: String,
    val description: String
) {
    fun buildUrl(tmdbId: Int, isTv: Boolean, season: Int = 1, episode: Int = 1): String {
        return when (id) {
            // Server 1 (CineSrc Pro - Auto-Skip Default)
            "server1" -> if (isTv) {
                "https://cinesrc.st/embed/tv/$tmdbId?s=$season&e=$episode&autoplay=true&color=%23f59e0b&autonext=true&autoskip=true"
            } else {
                "https://cinesrc.st/embed/movie/$tmdbId?autoplay=true&color=%23f59e0b&autonext=true&autoskip=true"
            }

            // Server 2 (CineSrc 1080p Pro)
            "server2" -> if (isTv) {
                "https://cinesrc.st/embed/tv/$tmdbId?s=$season&e=$episode&autoplay=true&quality=1080p&color=%23f59e0b&autonext=true"
            } else {
                "https://cinesrc.st/embed/movie/$tmdbId?autoplay=true&quality=1080p&color=%23f59e0b&autonext=true"
            }

            // Server 3 (CineSrc Edge - Low-Latency Priority)
            "server3" -> if (isTv) {
                "https://cinesrc.st/embed/tv/$tmdbId?s=$season&e=$episode&autoplay=true&prioritize=true&color=%23f59e0b&autonext=true"
            } else {
                "https://cinesrc.st/embed/movie/$tmdbId?autoplay=true&prioritize=true&color=%23f59e0b&autonext=true"
            }

            // Server 4 (VidCore - Multi-CDN 4K Mirror)
            "server4" -> if (isTv) {
                "https://vidcore.org/embed/tv/$tmdbId/$season/$episode"
            } else {
                "https://vidcore.org/embed/movie/$tmdbId"
            }

            else -> if (isTv) {
                "https://cinesrc.st/embed/tv/$tmdbId?s=$season&e=$episode&autoplay=true&color=%23f59e0b&autonext=true&autoskip=true"
            } else {
                "https://cinesrc.st/embed/movie/$tmdbId?autoplay=true&color=%23f59e0b&autonext=true&autoskip=true"
            }
        }
    }

    companion object {
        val ALL_SERVERS = listOf(
            StreamServer(
                id = "server1",
                name = "Server 1",
                shortName = "Server 1",
                badge = "1080p Auto",
                description = "Primary high-speed stream with smart auto-skip"
            ),
            StreamServer(
                id = "server2",
                name = "Server 2",
                shortName = "Server 2",
                badge = "1080p Pro",
                description = "Clean 1080p high-definition stream"
            ),
            StreamServer(
                id = "server3",
                name = "Server 3",
                shortName = "Server 3",
                badge = "Fast Edge",
                description = "Low-latency global edge CDN priority stream"
            ),
            StreamServer(
                id = "server4",
                name = "Server 4",
                shortName = "Server 4",
                badge = "VidCore 4K",
                description = "Ultra-fast multi-source VidCore CDN mirror"
            )
        )

        val DEFAULT_SERVER = ALL_SERVERS[0]
        private const val PREFS_NAME = "watzem_server_prefs"
        private const val KEY_LAST_WORKING_SERVER = "last_working_server_id"

        fun getLastWorkingServer(context: Context): StreamServer {
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedId = prefs.getString(KEY_LAST_WORKING_SERVER, null)
                val effectiveId = if (savedId == "server8") "server4" else savedId
                ALL_SERVERS.find { it.id == effectiveId } ?: DEFAULT_SERVER
            } catch (_: Exception) {
                DEFAULT_SERVER
            }
        }

        fun saveLastWorkingServer(context: Context, server: StreamServer) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_LAST_WORKING_SERVER, server.id).apply()
            } catch (_: Exception) {}
        }

        fun saveLastWorkingServerId(context: Context, serverId: String) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val effectiveId = if (serverId == "server8") "server4" else serverId
                prefs.edit().putString(KEY_LAST_WORKING_SERVER, effectiveId).apply()
            } catch (_: Exception) {}
        }
    }
}


data class DirectStreamItem(
    val title: String,
    val quality: String,
    val streamUrl: String
) {
    companion object {
        val DEMO_STREAMS = listOf(
            DirectStreamItem(
                title = "Big Buck Bunny (4K / 60fps)",
                quality = "4K UHD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            DirectStreamItem(
                title = "Elephants Dream (1080p)",
                quality = "1080p HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            ),
            DirectStreamItem(
                title = "Tears of Steel (Sci-Fi / 1080p)",
                quality = "1080p HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            DirectStreamItem(
                title = "Sintel (Blender Open Movie / 720p)",
                quality = "720p HD",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            )
        )
    }
}
