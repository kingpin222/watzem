package com.watzem.nativeapp.data.remote

import com.watzem.nativeapp.data.model.GenreListResponse
import com.watzem.nativeapp.data.model.MediaDetails
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.model.SeasonDetailsResponse
import com.watzem.nativeapp.data.model.TmdbPaginatedResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("trending/{media_type}/{time_window}")
    suspend fun getTrending(
        @Path("media_type") mediaType: String = "all",
        @Path("time_window") timeWindow: String = "day",
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("tv/popular")
    suspend fun getPopularTv(
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("tv/top_rated")
    suspend fun getTopRatedTv(
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("tv/on_the_air")
    suspend fun getOnTheAirTv(
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("tv/airing_today")
    suspend fun getAiringTodayTv(
        @Query("page") page: Int = 1
    ): TmdbPaginatedResponse<MediaItem>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("append_to_response") appendToResponse: String = "videos,credits,similar,keywords"
    ): MediaDetails

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("append_to_response") appendToResponse: String = "videos,credits,similar,keywords"
    ): MediaDetails

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int
    ): SeasonDetailsResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbPaginatedResponse<MediaItem>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbPaginatedResponse<MediaItem>

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbPaginatedResponse<MediaItem>

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("page") page: Int = 1,
        @Query("with_genres") withGenres: String? = null,
        @Query("with_networks") withNetworks: String? = null,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = "US",
        @Query("with_companies") withCompanies: String? = null,
        @Query("primary_release_year") primaryReleaseYear: String? = null,
        @Query("primary_release_date.gte") primaryReleaseDateGte: String? = null,
        @Query("primary_release_date.lte") primaryReleaseDateLte: String? = null,
        @Query("sort_by") sortBy: String? = "popularity.desc"
    ): TmdbPaginatedResponse<MediaItem>

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("page") page: Int = 1,
        @Query("with_genres") withGenres: String? = null,
        @Query("with_networks") withNetworks: String? = null,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = "US",
        @Query("first_air_date_year") firstAirDateYear: String? = null,
        @Query("first_air_date.gte") firstAirDateGte: String? = null,
        @Query("first_air_date.lte") firstAirDateLte: String? = null,
        @Query("sort_by") sortBy: String? = "popularity.desc"
    ): TmdbPaginatedResponse<MediaItem>

    @GET("genre/movie/list")
    suspend fun getMovieGenres(): GenreListResponse

    @GET("genre/tv/list")
    suspend fun getTvGenres(): GenreListResponse

    @GET("search/person")
    suspend fun searchPerson(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbPaginatedResponse<com.watzem.nativeapp.data.model.PersonSearchItem>

    @GET("person/{person_id}/combined_credits")
    suspend fun getPersonCombinedCredits(
        @Path("person_id") personId: Int
    ): com.watzem.nativeapp.data.model.PersonCombinedCreditsResponse
}

