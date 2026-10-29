package com.watzem.nativeapp.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watzem.nativeapp.data.model.WatchlistItem
import com.watzem.nativeapp.data.repository.MovieRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WatchlistViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    val watchlistItems: StateFlow<List<WatchlistItem>> = repository.watchlistFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeFromWatchlist(mediaId: Int) {
        repository.removeFromWatchlist(mediaId)
    }
}

