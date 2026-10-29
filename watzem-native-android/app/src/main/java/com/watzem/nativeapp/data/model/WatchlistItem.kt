package com.watzem.nativeapp.data.model

data class WatchlistItem(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val mediaType: MediaType,
    val voteAverage: Double,
    val releaseYear: String,
    val addedAt: Long = System.currentTimeMillis()
) {
    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val ratingFormatted: String
        get() = String.format("%.1f", voteAverage)

    companion object {
        fun fromMediaItem(item: MediaItem): WatchlistItem {
            return WatchlistItem(
                id = item.id,
                title = item.displayTitle,
                posterPath = item.posterPath,
                mediaType = item.mediaType,
                voteAverage = item.voteAverage,
                releaseYear = item.releaseYear
            )
        }

        fun fromDetails(details: MediaDetails, type: MediaType): WatchlistItem {
            return WatchlistItem(
                id = details.id,
                title = details.displayTitle,
                posterPath = details.posterPath,
                mediaType = type,
                voteAverage = details.voteAverage,
                releaseYear = details.releaseYear
            )
        }
    }
}

