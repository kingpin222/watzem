package com.watzem.nativeapp.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watzem.nativeapp.data.model.MediaDetails
import com.watzem.nativeapp.data.model.MediaType
import com.watzem.nativeapp.data.model.TVEpisode
import com.watzem.nativeapp.data.model.WatchlistItem
import com.watzem.nativeapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val details: MediaDetails? = null,
    val isWatchlisted: Boolean = false,
    val activeSeasonNumber: Int = 1,
    val episodes: List<TVEpisode> = emptyList(),
    val isLoadingEpisodes: Boolean = false,
    val error: String? = null
)

class DetailViewModel(
    private val repository: MovieRepository,
    private val mediaId: Int,
    private val isTv: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = repository.getMediaDetails(mediaId, isTv)
            if (res.isSuccess) {
                val details = res.getOrNull()
                val isSaved = repository.isWatchlisted(mediaId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    details = details,
                    isWatchlisted = isSaved,
                    error = null
                )

                // If TV series, auto-fetch season 1 episodes
                if (isTv || details?.numberOfSeasons != null) {
                    val initialSeason = details?.safeSeasons?.firstOrNull { it.seasonNumber > 0 }?.seasonNumber ?: 1
                    loadEpisodes(initialSeason)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load title details."
                )
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        if (_uiState.value.activeSeasonNumber == seasonNumber && _uiState.value.episodes.isNotEmpty()) return
        loadEpisodes(seasonNumber)
    }

    private fun loadEpisodes(seasonNumber: Int) {
        _uiState.value = _uiState.value.copy(
            activeSeasonNumber = seasonNumber,
            isLoadingEpisodes = true
        )
        viewModelScope.launch {
            val res = repository.getSeasonEpisodes(mediaId, seasonNumber)
            res.onSuccess { epList ->
                _uiState.value = _uiState.value.copy(
                    episodes = epList,
                    isLoadingEpisodes = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingEpisodes = false)
            }
        }
    }

    fun toggleWatchlist() {
        val details = _uiState.value.details ?: return
        val type = if (isTv || details.numberOfSeasons != null) MediaType.TV else MediaType.MOVIE
        val watchlistItem = WatchlistItem.fromDetails(details, type)
        val isAdded = repository.toggleWatchlist(watchlistItem)
        _uiState.value = _uiState.value.copy(isWatchlisted = isAdded)
    }
}
