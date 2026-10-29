package com.watzem.nativeapp.ui.screens.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watzem.nativeapp.data.model.Genre
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TvShowsUiState(
    val isLoading: Boolean = true,
    val tvShows: List<MediaItem> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null
)

class TvShowsViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TvShowsUiState())
    val uiState: StateFlow<TvShowsUiState> = _uiState.asStateFlow()

    init {
        loadGenresAndTv()
    }

    private fun loadGenresAndTv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val genresRes = repository.getTvGenres()
            val genres = genresRes.getOrDefault(emptyList())

            val tvRes = repository.discoverTv(
                genreId = _uiState.value.selectedGenreId,
                page = 1
            )
            if (tvRes.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    genres = genres,
                    tvShows = tvRes.getOrDefault(emptyList()),
                    currentPage = 1,
                    isLoadingMore = false,
                    hasMorePages = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    genres = genres,
                    error = "Failed to load TV shows."
                )
            }
        }
    }

    fun selectGenre(genre: Genre?) {
        val newGenreId = genre?.id
        if (_uiState.value.selectedGenreId == newGenreId) return
        _uiState.value = _uiState.value.copy(
            selectedGenreId = newGenreId,
            isLoading = true,
            currentPage = 1,
            tvShows = emptyList(),
            hasMorePages = true
        )
        loadTvShows(page = 1)
    }

    fun loadTvShows(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.discoverTv(
                genreId = _uiState.value.selectedGenreId,
                page = page
            )
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tvShows = res.getOrDefault(emptyList()),
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load TV series."
                )
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading || !_uiState.value.hasMorePages) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val nextPage = _uiState.value.currentPage + 1
            val res = repository.discoverTv(
                genreId = _uiState.value.selectedGenreId,
                page = nextPage
            )

            if (res.isSuccess) {
                val newTv = res.getOrDefault(emptyList())
                if (newTv.isNotEmpty()) {
                    val currentIds = _uiState.value.tvShows.map { it.id }.toSet()
                    val deduplicated = newTv.filter { it.id !in currentIds }
                    _uiState.value = _uiState.value.copy(
                        tvShows = _uiState.value.tvShows + deduplicated,
                        currentPage = nextPage,
                        isLoadingMore = false,
                        hasMorePages = nextPage < 50
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, hasMorePages = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
}
