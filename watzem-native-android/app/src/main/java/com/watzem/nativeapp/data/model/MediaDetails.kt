package com.watzem.nativeapp.data.model

import com.google.gson.annotations.SerializedName

data class Genre(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)

data class GenreListResponse(
    @SerializedName("genres")
    val genres: List<Genre>
)

data class CastMember(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("character")
    val character: String?,
    @SerializedName("profile_path")
    val profilePath: String?
) {
    val profileUrl: String?
        get() = profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
}

data class TVSeason(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("season_number")
    val seasonNumber: Int = 1,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("overview")
    val overview: String? = null,
    @SerializedName("episode_count")
    val episodeCount: Int = 0,
    @SerializedName("poster_path")
    val posterPath: String? = null
) {
    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }
}

data class TVEpisode(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("episode_number")
    val episodeNumber: Int = 1,
    @SerializedName("season_number")
    val seasonNumber: Int = 1,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("overview")
    val overview: String = "",
    @SerializedName("still_path")
    val stillPath: String? = null,
    @SerializedName("vote_average")
    val voteAverage: Double = 0.0,
    @SerializedName("air_date")
    val airDate: String? = null,
    @SerializedName("runtime")
    val runtime: Int? = null
) {
    val stillUrl: String?
        get() = stillPath?.let { "https://image.tmdb.org/t/p/w300$it" }
}

data class SeasonDetailsResponse(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("season_number")
    val seasonNumber: Int = 1,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("episodes")
    val episodes: List<TVEpisode> = emptyList()
)

data class Network(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("logo_path")
    val logoPath: String? = null
)

data class Creator(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("name")
    val name: String = ""
)

data class MediaKeyword(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("name")
    val name: String = ""
)

data class KeywordsResponse(
    @SerializedName("keywords")
    val keywords: List<MediaKeyword>? = null,
    @SerializedName("results")
    val results: List<MediaKeyword>? = null
) {
    val allKeywords: List<String>
        get() = (keywords ?: results ?: emptyList()).map { it.name }
}

data class CreditsResponse(
    @SerializedName("cast")
    val cast: List<CastMember> = emptyList()
)

data class VideoItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("key")
    val key: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("site")
    val site: String,
    @SerializedName("type")
    val type: String
) {
    val youtubeTrailerUrl: String
        get() = "https://www.youtube.com/watch?v=$key"

    val youtubeThumbnailUrl: String
        get() = "https://img.youtube.com/vi/$key/hqdefault.jpg"
}

data class VideosResponse(
    @SerializedName("results")
    val results: List<VideoItem> = emptyList()
)

data class MediaDetails(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    private val _title: String? = null,
    @SerializedName("name")
    private val _name: String? = null,
    @SerializedName("overview")
    val overview: String = "",
    @SerializedName("tagline")
    val tagline: String? = null,
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
    @SerializedName("runtime")
    val runtime: Int? = null,
    @SerializedName("number_of_seasons")
    val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes")
    val numberOfEpisodes: Int? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("original_language")
    val originalLanguage: String? = null,
    @SerializedName("budget")
    val budget: Long? = null,
    @SerializedName("revenue")
    val revenue: Long? = null,
    @SerializedName("genres")
    val genres: List<Genre>? = null,
    @SerializedName("seasons")
    val seasons: List<TVSeason>? = null,
    @SerializedName("networks")
    val networks: List<Network>? = null,
    @SerializedName("created_by")
    val createdBy: List<Creator>? = null,
    @SerializedName("keywords")
    val keywords: KeywordsResponse? = null,
    @SerializedName("videos")
    val videos: VideosResponse? = null,
    @SerializedName("credits")
    val credits: CreditsResponse? = null,
    @SerializedName("similar")
    val similar: TmdbPaginatedResponse<MediaItem>? = null
) {
    val safeGenres: List<Genre>
        get() = genres ?: emptyList()

    val safeSeasons: List<TVSeason>
        get() = seasons ?: emptyList()

    val safeNetworks: List<Network>
        get() = networks ?: emptyList()

    val safeCreatedBy: List<Creator>
        get() = createdBy ?: emptyList()
    val displayTitle: String
        get() = _title ?: _name ?: "Untitled"

    val releaseYear: String
        get() {
            val date = _releaseDate ?: _firstAirDate ?: return ""
            return if (date.length >= 4) date.substring(0, 4) else date
        }

    val durationOrSeasons: String
        get() {
            if (runtime != null && runtime > 0) {
                val hours = runtime / 60
                val mins = runtime % 60
                return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
            }
            if (numberOfSeasons != null && numberOfSeasons > 0) {
                return if (numberOfSeasons == 1) "1 Season" else "$numberOfSeasons Seasons"
            }
            return ""
        }

    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

    val backdropUrl: String?
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }

    val ratingFormatted: String
        get() = String.format("%.1f", voteAverage)

    val formattedBudget: String?
        get() = budget?.takeIf { it > 0 }?.let {
            if (it >= 1_000_000) "$${it / 1_000_000}M" else "$${it / 1_000}K"
        }

    val formattedRevenue: String?
        get() = revenue?.takeIf { it > 0 }?.let {
            if (it >= 1_000_000) "$${it / 1_000_000}M" else "$${it / 1_000}K"
        }

    val primaryTrailer: VideoItem?
        get() = videos?.results?.firstOrNull {
            it.site.equals("YouTube", ignoreCase = true) &&
                    (it.type.equals("Trailer", ignoreCase = true) || it.type.equals("Teaser", ignoreCase = true))
        } ?: videos?.results?.firstOrNull { it.site.equals("YouTube", ignoreCase = true) }
}

data class PersonSearchItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("profile_path")
    val profilePath: String? = null,
    @SerializedName("known_for_department")
    val knownForDepartment: String? = null,
    @SerializedName("popularity")
    val popularity: Double = 0.0,
    @SerializedName("known_for")
    val knownFor: List<MediaItem> = emptyList()
) {
    val profileUrl: String?
        get() = profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
}

data class PersonCombinedCreditsResponse(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("cast")
    val cast: List<MediaItem> = emptyList()
)

