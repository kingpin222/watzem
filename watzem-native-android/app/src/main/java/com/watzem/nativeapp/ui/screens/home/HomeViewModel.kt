package com.watzem.nativeapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.repository.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeCategory(
    val key: String,
    val label: String,
    val icon: ImageVector
)

data class HomeUiState(
    val isTv: Boolean = false,
    val isLoading: Boolean = true,
    val activeCategory: String = "popular",
    val heroItems: List<MediaItem> = emptyList(),
    val section1Items: List<MediaItem> = emptyList(), // Trending
    val section2Items: List<MediaItem> = emptyList(), // Popular
    val section3Items: List<MediaItem> = emptyList(), // Top Rated
    val section4Items: List<MediaItem> = emptyList(), // Upcoming or On Air
    val genre1Items: List<MediaItem> = emptyList(),   // Sci-Fi
    val genre2Items: List<MediaItem> = emptyList(),   // Action / Crime
    val categoryItems: List<MediaItem> = emptyList(), // Currently selected category items for infinite grid
    val categoryPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun setMode(isTv: Boolean) {
        if (_uiState.value.isTv != isTv) {
            _uiState.value = _uiState.value.copy(
                isTv = isTv,
                isLoading = true,
                activeCategory = "popular",
                categoryPage = 1,
                hasMorePages = true
            )
            loadHomeData()
        }
    }

    fun selectCategory(categoryKey: String) {
        if (_uiState.value.activeCategory == categoryKey && _uiState.value.categoryItems.isNotEmpty()) return
        val isTv = _uiState.value.isTv
        _uiState.value = _uiState.value.copy(
            activeCategory = categoryKey,
            categoryPage = 1,
            isLoadingMore = true
        )
        viewModelScope.launch {
            val result = repository.getCategoryFeed(isTv, categoryKey, page = 1)
            result.onSuccess { items ->
                _uiState.value = _uiState.value.copy(
                    categoryItems = items,
                    categoryPage = 1,
                    isLoadingMore = false,
                    hasMorePages = items.isNotEmpty()
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun loadHomeData() {
        val isTv = _uiState.value.isTv
        val cat = _uiState.value.activeCategory
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val trendingDeferred = async {
                repository.getTrending(if (isTv) "tv" else "movie", "day", page = 1)
            }
            val popularDeferred = async {
                if (isTv) repository.getPopularTv(page = 1) else repository.getPopularMovies(page = 1)
            }
            val topRatedDeferred = async {
                if (isTv) repository.getTopRatedTv(page = 1) else repository.getTopRatedMovies(page = 1)
            }
            val upcomingDeferred = async {
                if (isTv) repository.getOnTheAirTv(page = 1) else repository.getUpcomingMovies(page = 1)
            }
            val genre1Deferred = async {
                if (isTv) repository.discoverTv(genreId = 10765, page = 1) else repository.discoverMovies(genreId = 878, page = 1)
            }
            val genre2Deferred = async {
                if (isTv) repository.discoverTv(genreId = 80, page = 1) else repository.discoverMovies(genreId = 28, page = 1)
            }

            val trendingRes = trendingDeferred.await()
            val popularRes = popularDeferred.await()
            val topRatedRes = topRatedDeferred.await()
            val upcomingRes = upcomingDeferred.await()
            val genre1Res = genre1Deferred.await()
            val genre2Res = genre2Deferred.await()

            if (trendingRes.isSuccess || popularRes.isSuccess) {
                val trending = trendingRes.getOrDefault(emptyList())
                val popular = popularRes.getOrDefault(emptyList())
                val heroes = trending.filter { it.backdropPath != null }.take(10).ifEmpty {
                    trending.take(10)
                }

                val initialCatItems = when (cat) {
                    "popular" -> popular.ifEmpty { trending }
                    "trending" -> trending
                    "imdb_top" -> topRatedRes.getOrDefault(emptyList())
                    "now_playing", "upcoming", "on_the_air", "airing_today" -> upcomingRes.getOrDefault(emptyList())
                    else -> popular
                }

                _uiState.value = HomeUiState(
                    isTv = isTv,
                    isLoading = false,
                    activeCategory = cat,
                    heroItems = heroes,
                    section1Items = trending,
                    section2Items = popular,
                    section3Items = topRatedRes.getOrDefault(emptyList()),
                    section4Items = upcomingRes.getOrDefault(emptyList()),
                    genre1Items = genre1Res.getOrDefault(emptyList()),
                    genre2Items = genre2Res.getOrDefault(emptyList()),
                    categoryItems = initialCatItems,
                    categoryPage = 1,
                    hasMorePages = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load content. Please check your internet connection."
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMorePages) return

        val nextPage = state.categoryPage + 1
        val isTv = state.isTv
        val cat = state.activeCategory
        _uiState.value = state.copy(isLoadingMore = true)

        viewModelScope.launch {
            val result = repository.getCategoryFeed(isTv, cat, page = nextPage)
            result.onSuccess { newItems ->
                if (newItems.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        hasMorePages = false
                    )
                } else {
                    val currentIds = _uiState.value.categoryItems.map { it.id }.toSet()
                    val unique = newItems.filter { it.id !in currentIds }
                    _uiState.value = _uiState.value.copy(
                        categoryItems = _uiState.value.categoryItems + unique,
                        categoryPage = nextPage,
                        isLoadingMore = false,
                        hasMorePages = newItems.size >= 10
                    )
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
}
