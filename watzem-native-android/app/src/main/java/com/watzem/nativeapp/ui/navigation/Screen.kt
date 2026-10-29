package com.watzem.nativeapp.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Explore : Screen("explore")
    data object Search : Screen("search?query={query}&actorId={actorId}&actorName={actorName}&actorProfile={actorProfile}") {
        fun createRoute(
            query: String = "",
            actorId: Int = 0,
            actorName: String = "",
            actorProfile: String? = null
        ): String {
            val q = if (query.isNotBlank()) Uri.encode(query) else ""
            val name = if (actorName.isNotBlank()) Uri.encode(actorName) else ""
            val profile = if (!actorProfile.isNullOrBlank()) Uri.encode(actorProfile) else ""
            return "search?query=$q&actorId=$actorId&actorName=$name&actorProfile=$profile"
        }
    }
    data object Watchlist : Screen("watchlist")
    data object Update : Screen("update")

    data object Detail : Screen("detail/{mediaId}/{isTv}") {
        fun createRoute(mediaId: Int, isTv: Boolean): String {
            return "detail/$mediaId/$isTv"
        }
    }

    data object VideoPlayer : Screen("video_player?url={url}&title={title}") {
        fun createRoute(url: String, title: String): String {
            val encodedUrl = Uri.encode(url)
            val encodedTitle = Uri.encode(title)
            return "video_player?url=$encodedUrl&title=$encodedTitle"
        }
    }

    data object WebStreamPlayer : Screen("stream_player/{mediaId}/{isTv}?title={title}&season={season}&episode={episode}&backdrop={backdrop}") {
        fun createRoute(
            mediaId: Int,
            isTv: Boolean,
            title: String,
            season: Int = 1,
            episode: Int = 1,
            backdropUrl: String? = null
        ): String {
            val encodedTitle = Uri.encode(title)
            val encodedBackdrop = if (!backdropUrl.isNullOrBlank()) Uri.encode(backdropUrl) else ""
            return "stream_player/$mediaId/$isTv?title=$encodedTitle&season=$season&episode=$episode&backdrop=$encodedBackdrop"
        }
    }
}
