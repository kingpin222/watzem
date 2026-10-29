package com.watzem.nativeapp.data.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri

data class DeepLinkTarget(
    val mediaId: Int,
    val isTv: Boolean
)

object DeepLinkHandler {

    const val SCHEME_APP = "watzem"
    const val HOST_WEB = "watzem.app"
    const val DOWNLOAD_URL = "https://github.com/kingpin222/watzem/releases/latest/download/app-release.apk"

    /**
     * Builds custom scheme deeplink: watzem://movie/123 or watzem://tv/123
     */
    fun buildDeepLink(mediaId: Int, isTv: Boolean): String {
        val type = if (isTv) "tv" else "movie"
        return "$SCHEME_APP://$type/$mediaId"
    }

    /**
     * Builds web fallback link: https://watzem.app/movie/123 or https://watzem.app/tv/123
     */
    fun buildWebLink(mediaId: Int, isTv: Boolean): String {
        val type = if (isTv) "tv" else "movie"
        return "https://$HOST_WEB/$type/$mediaId"
    }

    /**
     * Builds comprehensive share text with movie/show info, app deeplink, web link, and APK download URL.
     */
    fun buildShareText(displayTitle: String, mediaId: Int, isTv: Boolean, releaseYear: String? = null): String {
        val yearStr = if (!releaseYear.isNullOrBlank()) " ($releaseYear)" else ""
        val deepLink = buildDeepLink(mediaId, isTv)
        val webLink = buildWebLink(mediaId, isTv)

        return buildString {
            append("🎬 Watch \"$displayTitle\"$yearStr on WATZEM — 100% Free with zero ads!\n\n")
            append("📱 Open in App: $deepLink\n")
            append("🌐 Web Link: $webLink\n")
            append("⬇️ Get App: $DOWNLOAD_URL")
        }
    }

    /**
     * Builds app share text for recommending WATZEM.
     */
    fun buildAppShareText(): String {
        return buildString {
            append("🎬 Stream movies & TV shows in 1080p and 4K with zero ads on WATZEM!\n\n")
            append("⬇️ Download free: $DOWNLOAD_URL")
        }
    }

    /**
     * Parse incoming deep link string or URI.
     * Supported formats:
     * - watzem://movie/123
     * - watzem://tv/123
     * - watzem://detail/123/true (or false)
     * - watzem://123 (defaults to movie)
     * - watzem://?movie=123 or ?tv=123 or ?id=123&type=tv
     * - https://watzem.app/movie/123
     * - https://watzem.app/tv/123
     * - https://watzem.app/detail/123/true
     * - https://watzem.app/?movie=123 or ?tv=123
     */
    fun parse(urlString: String?): DeepLinkTarget? {
        if (urlString.isNullOrBlank()) return null
        val clean = urlString.trim()

        // 1. Query parameters check: ?movie=123 or ?tv=123 or ?id=123&type=tv
        val queryIndex = clean.indexOf('?')
        if (queryIndex != -1) {
            val queryString = clean.substring(queryIndex + 1)
            val params = queryString.split('&').mapNotNull {
                val kv = it.split('=')
                if (kv.size == 2) kv[0].lowercase() to kv[1] else null
            }.toMap()

            val movieParam = params["movie"]?.toIntOrNull()
            if (movieParam != null && movieParam > 0) return DeepLinkTarget(movieParam, isTv = false)

            val tvParam = params["tv"]?.toIntOrNull()
            if (tvParam != null && tvParam > 0) return DeepLinkTarget(tvParam, isTv = true)

            val idParam = params["id"]?.toIntOrNull()
            if (idParam != null && idParam > 0) {
                val isTv = params["type"]?.equals("tv", ignoreCase = true) == true
                return DeepLinkTarget(idParam, isTv = isTv)
            }
        }

        val noQuery = if (queryIndex != -1) clean.substring(0, queryIndex) else clean

        // 2. Custom Scheme: watzem://...
        if (noQuery.startsWith("$SCHEME_APP://", ignoreCase = true)) {
            val path = noQuery.substring("$SCHEME_APP://".length).trim('/')
            val parts = path.split('/').filter { it.isNotBlank() }

            if (parts.size >= 2) {
                val type = parts[0].lowercase()
                val id = parts[1].toIntOrNull()
                if (id != null && id > 0) {
                    return when (type) {
                        "movie" -> DeepLinkTarget(id, isTv = false)
                        "tv" -> DeepLinkTarget(id, isTv = true)
                        "detail" -> {
                            val isTv = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: false
                            DeepLinkTarget(id, isTv = isTv)
                        }
                        else -> null
                    }
                }
            } else if (parts.size == 1) {
                val id = parts[0].toIntOrNull()
                if (id != null && id > 0) return DeepLinkTarget(id, isTv = false)
            }
        }

        // 3. Web URLs: https://watzem.app/...
        val lower = noQuery.lowercase()
        val webPrefix = "watzem.app"
        if (lower.contains(webPrefix)) {
            val afterDomain = noQuery.substring(lower.indexOf(webPrefix) + webPrefix.length).trim('/')
            val parts = afterDomain.split('/').filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val type = parts[0].lowercase()
                val id = parts[1].toIntOrNull()
                if (id != null && id > 0) {
                    return when (type) {
                        "movie" -> DeepLinkTarget(id, isTv = false)
                        "tv" -> DeepLinkTarget(id, isTv = true)
                        "detail" -> {
                            val isTv = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: false
                            DeepLinkTarget(id, isTv = isTv)
                        }
                        else -> null
                    }
                }
            }
        }

        return null
    }

    fun parse(uri: Uri?): DeepLinkTarget? {
        return parse(uri?.toString())
    }

    /**
     * Share a specific movie or TV show using system intent chooser.
     */
    fun shareMedia(
        context: Context,
        title: String,
        mediaId: Int,
        isTv: Boolean,
        releaseYear: String? = null
    ) {
        val shareText = buildShareText(title, mediaId, isTv, releaseYear)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Watch \"$title\" on WATZEM")
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Share \"$title\"")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Share the WATZEM application download link.
     */
    fun shareApp(context: Context) {
        val shareText = buildAppShareText()
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Download WATZEM App")
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Share WATZEM App")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
