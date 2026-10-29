package com.watzem.nativeapp.data.repository

import com.watzem.nativeapp.data.local.WatchlistManager
import com.watzem.nativeapp.data.model.Genre
import com.watzem.nativeapp.data.model.MediaDetails
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.model.MediaType
import com.watzem.nativeapp.data.model.PersonSearchItem
import com.watzem.nativeapp.data.model.WatchlistItem
import com.watzem.nativeapp.data.remote.TmdbApiClient
import com.watzem.nativeapp.data.remote.TmdbApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class MovieRepository(
    private val api: TmdbApiService = TmdbApiClient.service,
    private val watchlistManager: WatchlistManager
) {
    val watchlistFlow: StateFlow<List<WatchlistItem>> = watchlistManager.watchlistFlow

    suspend fun getTrending(
        mediaType: String = "all",
        timeWindow: String = "day",
        page: Int = 1
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTrending(mediaType, timeWindow, page)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularMovies(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPopularMovies(page)
            val mapped = response.results.map { it.copy(rawMediaType = "movie") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopRatedMovies(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTopRatedMovies(page)
            val mapped = response.results.map { it.copy(rawMediaType = "movie") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUpcomingMovies(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUpcomingMovies(page)
            val mapped = response.results.map { it.copy(rawMediaType = "movie") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNowPlayingMovies(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getNowPlayingMovies(page)
            val mapped = response.results.map { it.copy(rawMediaType = "movie") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularTv(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPopularTv(page)
            val mapped = response.results.map { it.copy(rawMediaType = "tv") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopRatedTv(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTopRatedTv(page)
            val mapped = response.results.map { it.copy(rawMediaType = "tv") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOnTheAirTv(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getOnTheAirTv(page)
            val mapped = response.results.map { it.copy(rawMediaType = "tv") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAiringTodayTv(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAiringTodayTv(page)
            val mapped = response.results.map { it.copy(rawMediaType = "tv") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeasonEpisodes(tvId: Int, seasonNumber: Int): Result<List<com.watzem.nativeapp.data.model.TVEpisode>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getSeasonDetails(tvId, seasonNumber)
                Result.success(response.episodes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getCategoryFeed(isTv: Boolean, categoryKey: String, page: Int = 1): Result<List<MediaItem>> {
        return if (!isTv) {
            when (categoryKey) {
                "popular" -> getPopularMovies(page)
                "trending" -> getTrending("movie", "week", page)
                "imdb_top" -> getTopRatedMovies(page)
                "now_playing" -> getNowPlayingMovies(page)
                "upcoming" -> getUpcomingMovies(page)
                else -> getPopularMovies(page)
            }
        } else {
            when (categoryKey) {
                "popular" -> getPopularTv(page)
                "trending" -> getTrending("tv", "week", page)
                "imdb_top" -> getTopRatedTv(page)
                "on_the_air" -> getOnTheAirTv(page)
                "airing_today" -> getAiringTodayTv(page)
                else -> getPopularTv(page)
            }
        }
    }

    suspend fun getMediaDetails(id: Int, isTv: Boolean): Result<MediaDetails> = withContext(Dispatchers.IO) {
        try {
            val details = if (isTv) {
                api.getTvDetails(id)
            } else {
                api.getMovieDetails(id)
            }
            Result.success(details)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(query: String, page: Int = 1): Result<List<MediaItem>> = searchCategory(query, "multi", page)

    suspend fun searchCategory(
        query: String,
        category: String = "multi",
        page: Int = 1
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())
            val response = when (category) {
                "movie" -> api.searchMovies(query, page)
                "tv" -> api.searchTv(query, page)
                else -> api.searchMulti(query, page)
            }
            val filtered = response.results.filter {
                it.rawMediaType == "movie" || it.rawMediaType == "tv" || it.rawMediaType == null
            }

            if (filtered.isNotEmpty()) {
                Result.success(filtered)
            } else if (page == 1) {
                // If direct media search returned empty results, check if the query is an actor or creator name
                try {
                    val personResponse = api.searchPerson(query, 1)
                    val person = personResponse.results.firstOrNull()
                    if (person != null) {
                        val creditsResponse = api.getPersonCombinedCredits(person.id)
                        val cast = creditsResponse.cast
                        val sorted = cast.sortedByDescending { it.voteAverage * it.voteCount }
                        val creditsFiltered = when (category) {
                            "movie" -> sorted.filter { it.rawMediaType == "movie" || it.rawMediaType == null }
                            "tv" -> sorted.filter { it.rawMediaType == "tv" }
                            else -> sorted
                        }
                        Result.success(creditsFiltered)
                    } else {
                        Result.success(emptyList())
                    }
                } catch (pe: Exception) {
                    Result.success(emptyList())
                }
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPerson(query: String, page: Int = 1): Result<List<PersonSearchItem>> = withContext(Dispatchers.IO) {
        try {
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@withContext Result.success(emptyList())
            val response = api.searchPerson(trimmed, page)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActorCredits(personId: Int): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPersonCombinedCredits(personId)
            val seen = mutableSetOf<String>()
            val deduped = mutableListOf<MediaItem>()
            for (item in response.cast) {
                if (item.posterPath == null) continue
                val key = "${item.rawMediaType ?: "movie"}-${item.id}"
                if (seen.add(key)) {
                    deduped.add(item)
                }
            }
            // Sort by vote count * vote average descending, or vote count
            val sorted = deduped.sortedByDescending { it.voteCount * it.voteAverage }
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverMoviesWithFilter(
        genreId: Int? = null,
        networkId: Int? = null,
        providerId: Int? = null,
        companyId: Int? = null,
        year: String? = null,
        yearGte: Int? = null,
        yearLte: Int? = null,
        page: Int = 1
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val genreParam = genreId?.takeIf { it > 0 }?.toString()
            val networkParam = networkId?.takeIf { it > 0 }?.toString()
            val providerParam = providerId?.takeIf { it > 0 }?.toString()
            val companyParam = companyId?.takeIf { it > 0 }?.toString()
            val yearParam = if (year != null && year != "all" && year.length == 4 && year.toIntOrNull() != null) year else null
            val dateGte = when {
                yearGte != null -> "$yearGte-01-01"
                year == "2020s" -> "2020-01-01"
                year == "2010s" -> "2010-01-01"
                year == "2000s" -> "2000-01-01"
                year == "1990s" || year == "90s" -> "1990-01-01"
                year == "1980s" || year == "80s" -> "1980-01-01"
                else -> null
            }
            val dateLte = when {
                yearLte != null -> "$yearLte-12-31"
                year == "2010s" -> "2019-12-31"
                year == "2000s" -> "2009-12-31"
                year == "1990s" || year == "90s" -> "1999-12-31"
                year == "1980s" || year == "80s" -> "1989-12-31"
                year == "Classic" || year == "classics" -> "1979-12-31"
                else -> null
            }

            val response = api.discoverMovies(
                page = page,
                withGenres = genreParam,
                withNetworks = networkParam,
                withWatchProviders = providerParam,
                withCompanies = companyParam,
                primaryReleaseYear = yearParam,
                primaryReleaseDateGte = dateGte,
                primaryReleaseDateLte = dateLte
            )
            val mapped = response.results.map { it.copy(rawMediaType = "movie") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverTvWithFilter(
        genreId: Int? = null,
        networkId: Int? = null,
        providerId: Int? = null,
        year: String? = null,
        yearGte: Int? = null,
        yearLte: Int? = null,
        page: Int = 1
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val genreParam = genreId?.takeIf { it > 0 }?.toString()
            val networkParam = networkId?.takeIf { it > 0 }?.toString()
            val providerParam = providerId?.takeIf { it > 0 }?.toString()
            val yearParam = if (year != null && year != "all" && year.length == 4 && year.toIntOrNull() != null) year else null
            val dateGte = when {
                yearGte != null -> "$yearGte-01-01"
                year == "2020s" -> "2020-01-01"
                year == "2010s" -> "2010-01-01"
                year == "2000s" -> "2000-01-01"
                year == "1990s" || year == "90s" -> "1990-01-01"
                year == "1980s" || year == "80s" -> "1980-01-01"
                else -> null
            }
            val dateLte = when {
                yearLte != null -> "$yearLte-12-31"
                year == "2010s" -> "2019-12-31"
                year == "2000s" -> "2009-12-31"
                year == "1990s" || year == "90s" -> "1999-12-31"
                year == "1980s" || year == "80s" -> "1989-12-31"
                year == "Classic" || year == "classics" -> "1979-12-31"
                else -> null
            }

            val response = api.discoverTv(
                page = page,
                withGenres = genreParam,
                withNetworks = networkParam,
                withWatchProviders = providerParam,
                firstAirDateYear = yearParam,
                firstAirDateGte = dateGte,
                firstAirDateLte = dateLte
            )
            val mapped = response.results.map { it.copy(rawMediaType = "tv") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverMovies(
        genreId: Int? = null,
        sortBy: String = "popularity.desc",
        page: Int = 1
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val genreParam = genreId?.takeIf { it > 0 }?.toString()
            val response = api.discoverMovies(page, genreParam, sortBy)
            val mapped = response.results.map { it.copy(rawMediaType = "movie") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverTv(
        genreId: Int? = null,
        sortBy: String = "popularity.desc",
        page: Int = 1
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val genreParam = genreId?.takeIf { it > 0 }?.toString()
            val response = api.discoverTv(page, genreParam, sortBy)
            val mapped = response.results.map { it.copy(rawMediaType = "tv") }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieGenres(): Result<List<Genre>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMovieGenres()
            Result.success(response.genres)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTvGenres(): Result<List<Genre>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTvGenres()
            Result.success(response.genres)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isWatchlisted(id: Int): Boolean = watchlistManager.isWatchlisted(id)

    fun toggleWatchlist(item: WatchlistItem): Boolean = watchlistManager.toggleWatchlist(item)

    fun removeFromWatchlist(id: Int) = watchlistManager.removeFromWatchlist(id)
}

