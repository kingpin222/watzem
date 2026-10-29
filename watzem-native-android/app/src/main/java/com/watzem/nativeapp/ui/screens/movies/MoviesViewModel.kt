package com.watzem.nativeapp.ui.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watzem.nativeapp.data.model.Genre
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MoviesUiState(
    val isLoading: Boolean = true,
    val movies: List<MediaItem> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    val selectedSort: String = "popularity.desc",
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null
)

class MoviesViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        loadGenresAndMovies()
    }

    private fun loadGenresAndMovies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val genresRes = repository.getMovieGenres()
            val genres = genresRes.getOrDefault(emptyList())

            val moviesRes = repository.discoverMovies(
                genreId = _uiState.value.selectedGenreId,
                sortBy = _uiState.value.selectedSort,
                page = 1
            )

            if (moviesRes.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    genres = genres,
                    movies = moviesRes.getOrDefault(emptyList()),
                    currentPage = 1,
                    isLoadingMore = false,
                    hasMorePages = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    genres = genres,
                    error = "Failed to load movies."
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
            movies = emptyList(),
            hasMorePages = true
        )
        loadMovies(page = 1)
    }

    fun loadMovies(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.discoverMovies(
                genreId = _uiState.value.selectedGenreId,
                sortBy = _uiState.value.selectedSort,
                page = page
            )
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movies = res.getOrDefault(emptyList()),
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = true,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load movies."
                )
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading || !_uiState.value.hasMorePages) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val nextPage = _uiState.value.currentPage + 1
            val res = repository.discoverMovies(
                genreId = _uiState.value.selectedGenreId,
                sortBy = _uiState.value.selectedSort,
                page = nextPage
            )

            if (res.isSuccess) {
                val newMovies = res.getOrDefault(emptyList())
                if (newMovies.isNotEmpty()) {
                    val currentIds = _uiState.value.movies.map { it.id }.toSet()
                    val deduplicated = newMovies.filter { it.id !in currentIds }
                    _uiState.value = _uiState.value.copy(
                        movies = _uiState.value.movies + deduplicated,
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
