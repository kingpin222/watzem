package com.watzem.nativeapp.ui.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.ui.components.ErrorStateView
import com.watzem.nativeapp.ui.components.GenreChipsRow
import com.watzem.nativeapp.ui.components.LoadMoreButton
import com.watzem.nativeapp.ui.components.LoadingSpinner
import com.watzem.nativeapp.ui.components.MediaPosterCard
import com.watzem.nativeapp.ui.theme.CinemaBackground
import com.watzem.nativeapp.ui.theme.TextPrimary

@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel,
    onMediaClick: (MediaItem) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    // Automatic infinite scroll detection
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingMore && state.hasMorePages) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBackground)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Movies",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Genre Filter Chips
        GenreChipsRow(
            genres = state.genres,
            selectedGenreId = state.selectedGenreId,
            onGenreSelected = { viewModel.selectGenre(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> {
                LoadingSpinner(
                    modifier = Modifier.fillMaxSize(),
                    message = "Finding top movies..."
                )
            }
            state.error != null -> {
                ErrorStateView(
                    message = state.error ?: "Error loading movies",
                    onRetry = { viewModel.loadMovies(page = 1) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.movies, key = { it.id }) { movie ->
                        MediaPosterCard(
                            item = movie,
                            onClick = { onMediaClick(movie) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Load More Footer Button spanning full width
                    if (state.hasMorePages) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadMoreButton(
                                isLoadingMore = state.isLoadingMore,
                                onClick = { viewModel.loadMore() },
                                text = "Load More Movies"
                            )
                        }
                    }
                }
            }
        }
    }
}
