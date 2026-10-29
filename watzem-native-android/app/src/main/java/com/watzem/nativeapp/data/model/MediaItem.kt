package com.watzem.nativeapp.data.model

import com.google.gson.annotations.SerializedName

enum class MediaType(val value: String) {
    MOVIE("movie"),
    TV("tv");

    companion object {
        fun fromString(type: String?): MediaType {
            return when (type?.lowercase()) {
                "tv" -> TV
                else -> MOVIE
            }
        }
    }
}

data class MediaItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    private val _title: String? = null,
    @SerializedName("name")
    private val _name: String? = null,
    @SerializedName("overview")
    val overview: String = "",
    @SerializedName("poster_path")
    val posterPath: String? = null,
    @SerializedName("backdrop_path")
    val backdropPath: String? = null,
    @SerializedName("vote_average")
    val voteAverage: Double = 0.0,
    @SerializedName("vote_count")
    val voteCount: Int = 0,
    @SerializedName("release_date")
    private val _releaseDate: String? = null,
    @SerializedName("first_air_date")
    private val _firstAirDate: String? = null,
    @SerializedName("media_type")
    val rawMediaType: String? = null,
    @SerializedName("genre_ids")
    val genreIds: List<Int> = emptyList()
) {
    val displayTitle: String
        get() = _title ?: _name ?: "Untitled"

    val releaseYear: String
        get() {
            val date = _releaseDate ?: _firstAirDate ?: return ""
            return if (date.length >= 4) date.substring(0, 4) else date
        }

    val mediaType: MediaType
        get() = MediaType.fromString(rawMediaType)

    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val backdropUrl: String?
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }

    val ratingFormatted: String
        get() = String.format("%.1f", voteAverage)
}

data class TmdbPaginatedResponse<T>(
    @SerializedName("page")
    val page: Int,
    @SerializedName("results")
    val results: List<T>,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("total_results")
    val totalResults: Int
)

