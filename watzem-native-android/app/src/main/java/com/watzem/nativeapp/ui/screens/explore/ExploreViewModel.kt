package com.watzem.nativeapp.ui.screens.explore

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreNetworkDef(
    val id: Int,
    val name: String,
    val providerId: Int,
    val networkId: Int,
    val icon: ImageVector
)

data class ExploreGenreDef(
    val id: Int,
    val tvId: Int,
    val name: String,
    val icon: ImageVector
)

data class ExploreEraDef(
    val id: String,
    val name: String,
    val badge: String? = null,
    val year: Int? = null,
    val yearGte: Int? = null,
    val yearLte: Int? = null,
    val icon: ImageVector
)

data class ExploreFranchiseDef(
    val id: Int,
    val name: String,
    val query: String,
    val icon: ImageVector
)

data class ExploreStudioDef(
    val id: Int,
    val name: String,
    val companyId: Int,
    val icon: ImageVector
)

sealed class ExploreCategoryContext {
    data class Network(val def: ExploreNetworkDef) : ExploreCategoryContext()
    data class Genre(val def: ExploreGenreDef) : ExploreCategoryContext()
    data class Era(val def: ExploreEraDef) : ExploreCategoryContext()
    data class Franchise(val def: ExploreFranchiseDef) : ExploreCategoryContext()
    data class Studio(val def: ExploreStudioDef) : ExploreCategoryContext()
}

data class ExploreUiState(
    val isTv: Boolean = false,
    val isLoading: Boolean = false,
    val activeTitle: String? = null,
    val activeContext: ExploreCategoryContext? = null,
    val selectedFilterYear: String = "all",
    val items: List<MediaItem> = emptyList(),
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null
)

class ExploreViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    // Networks & Streaming Providers with Lucide / Material vector icons
    val networks = listOf(
        ExploreNetworkDef(213, "Netflix", providerId = 8, networkId = 213, icon = Icons.Default.LiveTv),
        ExploreNetworkDef(49, "HBO / Max", providerId = 1899, networkId = 49, icon = Icons.Default.Movie),
        ExploreNetworkDef(2739, "Disney+", providerId = 337, networkId = 2739, icon = Icons.Default.Castle),
        ExploreNetworkDef(1024, "Prime Video", providerId = 9, networkId = 1024, icon = Icons.Default.VideoLibrary),
        ExploreNetworkDef(2552, "Apple TV+", providerId = 350, networkId = 2552, icon = Icons.Default.Devices),
        ExploreNetworkDef(453, "Hulu", providerId = 15, networkId = 453, icon = Icons.Default.Layers),
        ExploreNetworkDef(4330, "Paramount+", providerId = 531, networkId = 4330, icon = Icons.Default.Explore),
        ExploreNetworkDef(3353, "Peacock", providerId = 386, networkId = 3353, icon = Icons.Default.AutoAwesome)
    )

    // Popular Genres with Lucide / Material vector icons
    val genres = listOf(
        ExploreGenreDef(28, 10759, "Action", Icons.Default.Bolt),
        ExploreGenreDef(12, 10759, "Adventure", Icons.Default.Explore),
        ExploreGenreDef(16, 16, "Animation", Icons.Default.Palette),
        ExploreGenreDef(35, 35, "Comedy", Icons.Default.SentimentSatisfiedAlt),
        ExploreGenreDef(80, 80, "Crime", Icons.Default.Security),
        ExploreGenreDef(99, 99, "Documentary", Icons.Default.Groups),
        ExploreGenreDef(18, 18, "Drama", Icons.Default.TheaterComedy),
        ExploreGenreDef(10751, 10751, "Family", Icons.Default.FamilyRestroom),
        ExploreGenreDef(14, 10765, "Fantasy", Icons.Default.AutoAwesome),
        ExploreGenreDef(36, 18, "History", Icons.Default.AccountBalance),
        ExploreGenreDef(27, 9648, "Horror", Icons.Default.Warning),
        ExploreGenreDef(9648, 9648, "Mystery", Icons.Default.Search),
        ExploreGenreDef(10749, 10749, "Romance", Icons.Default.Favorite),
        ExploreGenreDef(878, 10765, "Sci-Fi", Icons.Default.RocketLaunch),
        ExploreGenreDef(53, 9648, "Thriller", Icons.Default.Visibility),
        ExploreGenreDef(10752, 10768, "War", Icons.Default.Shield),
        ExploreGenreDef(37, 37, "Western", Icons.Default.MyLocation),
        ExploreGenreDef(10402, 10402, "Music", Icons.Default.MusicNote)
    )

    // Release Years & Eras with Lucide / Material vector icons
    val releaseYears = listOf(
        ExploreEraDef("2026", "2026", badge = "Upcoming", year = 2026, icon = Icons.Default.AutoAwesome),
        ExploreEraDef("2025", "2025", badge = "Latest", year = 2025, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2024", "2024", badge = "Hits", year = 2024, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2023", "2023", year = 2023, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2022", "2022", year = 2022, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2021", "2021", year = 2021, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2020", "2020", year = 2020, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2019", "2019", year = 2019, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2018", "2018", year = 2018, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2015", "2015", year = 2015, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2010", "2010", year = 2010, icon = Icons.Default.CalendarToday),
        ExploreEraDef("2010s", "2010s", badge = "Decade", yearGte = 2010, yearLte = 2019, icon = Icons.Default.History),
        ExploreEraDef("2000s", "2000s", badge = "Decade", yearGte = 2000, yearLte = 2009, icon = Icons.Default.History),
        ExploreEraDef("1990s", "90s", badge = "Retro", yearGte = 1990, yearLte = 1999, icon = Icons.Default.History),
        ExploreEraDef("1980s", "80s", badge = "Retro", yearGte = 1980, yearLte = 1989, icon = Icons.Default.History),
        ExploreEraDef("classics", "Classics", badge = "Pre-80s", yearLte = 1979, icon = Icons.Default.Schedule)
    )

    // Popular Movie Franchises
    val collections = listOf(
        ExploreFranchiseDef(86311, "The Avengers", "Avengers", Icons.Default.Shield),
        ExploreFranchiseDef(1241, "Harry Potter", "Harry Potter", Icons.Default.AutoAwesome),
        ExploreFranchiseDef(10, "Star Wars", "Star Wars", Icons.Default.RocketLaunch),
        ExploreFranchiseDef(9485, "Fast & Furious", "Fast and Furious", Icons.Default.Speed),
        ExploreFranchiseDef(119, "Lord of the Rings", "Lord of the Rings", Icons.Default.EmojiEvents),
        ExploreFranchiseDef(295, "Pirates of the Caribbean", "Pirates of the Caribbean", Icons.Default.Sailing),
        ExploreFranchiseDef(529892, "Spider-Man", "Spider-Man", Icons.Default.TravelExplore),
        ExploreFranchiseDef(263, "The Dark Knight", "Dark Knight", Icons.Default.Nightlight),
        ExploreFranchiseDef(87096, "Avatar", "Avatar", Icons.Default.Public),
        ExploreFranchiseDef(8091, "Alien", "Alien", Icons.Default.BrightnessAuto)
    )

    // Film Studios (Movie Mode Only)
    val studios = listOf(
        ExploreStudioDef(1, "Marvel Studios", companyId = 420, icon = Icons.Default.Bolt),
        ExploreStudioDef(2, "Pixar", companyId = 3, icon = Icons.Default.AutoAwesome),
        ExploreStudioDef(3, "A24", companyId = 41077, icon = Icons.Default.Camera),
        ExploreStudioDef(4, "Studio Ghibli", companyId = 10342, icon = Icons.Default.Eco),
        ExploreStudioDef(5, "Warner Bros", companyId = 174, icon = Icons.Default.Movie),
        ExploreStudioDef(6, "Walt Disney", companyId = 2, icon = Icons.Default.Castle)
    )

    val yearFilters = listOf("all", "2026", "2025", "2024", "2023", "2020s", "2010s", "classics")

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    fun setMode(isTv: Boolean) {
        if (_uiState.value.isTv != isTv) {
            _uiState.value = _uiState.value.copy(
                isTv = isTv,
                activeTitle = null,
                activeContext = null,
                selectedFilterYear = "all",
                items = emptyList(),
                currentPage = 1,
                hasMorePages = true
            )
        }
    }

    fun openNetwork(network: ExploreNetworkDef) {
        val title = "${network.name} ${if (_uiState.value.isTv) "Series" else "Movies"}"
        _uiState.value = _uiState.value.copy(
            activeTitle = title,
            activeContext = ExploreCategoryContext.Network(network),
            selectedFilterYear = "all",
            items = emptyList(),
            currentPage = 1,
            hasMorePages = true
        )
        loadCategoryItems(page = 1)
    }

    fun openGenre(genre: ExploreGenreDef) {
        val title = "${genre.name} ${if (_uiState.value.isTv) "Series" else "Cinema"}"
        _uiState.value = _uiState.value.copy(
            activeTitle = title,
            activeContext = ExploreCategoryContext.Genre(genre),
            selectedFilterYear = "all",
            items = emptyList(),
            currentPage = 1,
            hasMorePages = true
        )
        loadCategoryItems(page = 1)
    }

    fun openEra(era: ExploreEraDef) {
        val title = "${era.name} ${if (_uiState.value.isTv) "Series" else "Movies"}"
        _uiState.value = _uiState.value.copy(
            activeTitle = title,
            activeContext = ExploreCategoryContext.Era(era),
            selectedFilterYear = era.id,
            items = emptyList(),
            currentPage = 1,
            hasMorePages = true
        )
        loadCategoryItems(page = 1)
    }

    fun openFranchise(franchise: ExploreFranchiseDef) {
        _uiState.value = _uiState.value.copy(
            activeTitle = franchise.name,
            activeContext = ExploreCategoryContext.Franchise(franchise),
            selectedFilterYear = "all",
            items = emptyList(),
            currentPage = 1,
            hasMorePages = true
        )
        loadCategoryItems(page = 1)
    }

    fun openStudio(studio: ExploreStudioDef) {
        _uiState.value = _uiState.value.copy(
            activeTitle = studio.name,
            activeContext = ExploreCategoryContext.Studio(studio),
            selectedFilterYear = "all",
            items = emptyList(),
            currentPage = 1,
            hasMorePages = true
        )
        loadCategoryItems(page = 1)
    }

    fun selectFilterYear(year: String) {
        if (_uiState.value.selectedFilterYear == year) return
        _uiState.value = _uiState.value.copy(
            selectedFilterYear = year,
            currentPage = 1,
            items = emptyList()
        )
        loadCategoryItems(page = 1)
    }

    fun resetToOverview() {
        _uiState.value = _uiState.value.copy(
            activeTitle = null,
            activeContext = null,
            selectedFilterYear = "all",
            items = emptyList(),
            currentPage = 1
        )
    }

    fun loadCategoryItems(page: Int = 1) {
        val state = _uiState.value
        val ctx = state.activeContext ?: return
        val isTv = state.isTv
        val yearFilter = state.selectedFilterYear

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = page == 1, error = null)

            val res = when (ctx) {
                is ExploreCategoryContext.Network -> {
                    if (isTv) {
                        repository.discoverTvWithFilter(
                            networkId = ctx.def.networkId,
                            providerId = ctx.def.providerId,
                            year = yearFilter,
                            page = page
                        )
                    } else {
                        repository.discoverMoviesWithFilter(
                            networkId = ctx.def.networkId,
                            providerId = ctx.def.providerId,
                            year = yearFilter,
                            page = page
                        )
                    }
                }
                is ExploreCategoryContext.Genre -> {
                    val genreId = if (isTv) ctx.def.tvId else ctx.def.id
                    if (isTv) {
                        repository.discoverTvWithFilter(genreId = genreId, year = yearFilter, page = page)
                    } else {
                        repository.discoverMoviesWithFilter(genreId = genreId, year = yearFilter, page = page)
                    }
                }
                is ExploreCategoryContext.Era -> {
                    if (isTv) {
                        repository.discoverTvWithFilter(
                            year = ctx.def.id,
                            yearGte = ctx.def.yearGte,
                            yearLte = ctx.def.yearLte,
                            page = page
                        )
                    } else {
                        repository.discoverMoviesWithFilter(
                            year = ctx.def.id,
                            yearGte = ctx.def.yearGte,
                            yearLte = ctx.def.yearLte,
                            page = page
                        )
                    }
                }
                is ExploreCategoryContext.Franchise -> {
                    repository.searchCategory(ctx.def.query, if (isTv) "tv" else "movie", page)
                }
                is ExploreCategoryContext.Studio -> {
                    repository.discoverMoviesWithFilter(
                        companyId = ctx.def.companyId,
                        year = yearFilter,
                        page = page
                    )
                }
            }

            if (res.isSuccess) {
                val newItems = res.getOrDefault(emptyList())
                val current = if (page == 1) emptyList() else _uiState.value.items
                val currentIds = current.map { it.id }.toSet()
                val deduplicated = newItems.filter { it.id !in currentIds }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    items = current + deduplicated,
                    currentPage = page,
                    hasMorePages = newItems.size >= 10,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = "Failed to load titles."
                )
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading || !_uiState.value.hasMorePages) return
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        loadCategoryItems(_uiState.value.currentPage + 1)
    }
}
