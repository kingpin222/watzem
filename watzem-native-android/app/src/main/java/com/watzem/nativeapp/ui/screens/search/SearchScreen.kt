package com.watzem.nativeapp.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.model.PersonSearchItem
import com.watzem.nativeapp.ui.components.EmptyStateView
import com.watzem.nativeapp.ui.components.LoadMoreButton
import com.watzem.nativeapp.ui.components.MediaPosterCard
import com.watzem.nativeapp.ui.components.tvFocusable
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.BgCard
import com.watzem.nativeapp.ui.theme.BgMain
import com.watzem.nativeapp.ui.theme.BgSurfaceElevated
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onMediaClick: (MediaItem) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val posterGridCols = when {
        configuration.screenWidthDp >= 1300 -> 7
        configuration.screenWidthDp >= 800 -> 6
        configuration.screenWidthDp >= 600 -> 4
        configuration.screenWidthDp >= 450 -> 3
        else -> 2
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingMore && state.hasMorePages && state.searchViewMode != "artist") {
            viewModel.loadMore()
        }
    }

    val searchCategories = listOf(
        Pair("multi", "All"),
        Pair("movie", "Movies"),
        Pair("tv", "TV Shows")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        // Search Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Search movies, TV series, actors, artists...", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AccentAmber
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentAmber,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = BgSurfaceElevated,
                    unfocusedContainerColor = BgSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentAmber
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Category Filter Pills: All | Movies | TV Shows
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchCategories) { (catKey, catLabel) ->
                val isSelected = catKey == state.category
                val count = when (catKey) {
                    "movie" -> state.movieCount
                    "tv" -> state.tvCount
                    else -> state.results.size
                }

                Box(
                    modifier = Modifier
                        .tvFocusable(
                            shape = CircleShape,
                            focusedScale = 1.08f,
                            focusedBorderColor = AccentAmber,
                            borderWidth = 2.dp,
                            onClick = { viewModel.setCategory(catKey) }
                        )
                        .clip(CircleShape)
                        .background(if (isSelected) AccentAmber else BgSurfaceElevated)
                        .border(1.dp, if (isSelected) AccentAmber else BorderSubtle, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = catLabel,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (state.query.isNotBlank() && count > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color.Black.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$count",
                                    color = if (isSelected) Color.Black else AccentAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Content Area
        when {
            state.isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentAmber, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (state.activeArtist != null) "Loading ${state.activeArtist?.name}'s filmography..." else "Searching titles and artists...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            state.query.isBlank() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    // Recent Searches Section
                    if (state.recentSearches.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = AccentAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Recent Searches",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Clear All",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .tvFocusable(
                                            shape = RoundedCornerShape(4.dp),
                                            focusedScale = 1.05f,
                                            focusedBorderColor = AccentAmber,
                                            borderWidth = 1.dp,
                                            onClick = { viewModel.clearRecentSearches() }
                                        )
                                        .clickable { viewModel.clearRecentSearches() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.recentSearches.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .tvFocusable(
                                                shape = RoundedCornerShape(12.dp),
                                                focusedScale = 1.06f,
                                                focusedBorderColor = AccentAmber,
                                                borderWidth = 2.dp,
                                                onClick = { viewModel.submitSearch(item) }
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BgSurfaceElevated)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                            .padding(start = 10.dp, top = 5.dp, bottom = 5.dp, end = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = item, color = TextPrimary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeRecentSearch(item) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Trending Searches Section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Trending Searches & Artists",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.trendingSuggestions.forEach { suggestion ->
                                Box(
                                    modifier = Modifier
                                        .tvFocusable(
                                            shape = RoundedCornerShape(12.dp),
                                            focusedScale = 1.06f,
                                            focusedBorderColor = AccentAmber,
                                            borderWidth = 2.dp,
                                            onClick = { viewModel.submitSearch(suggestion) }
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BgCard)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = suggestion,
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }

            else -> {
                // Results View (Media and/or Artist Filmography)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Matched Artists Horizontal Row (when TMDB found matching people)
                    if (state.matchedArtists.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = AccentAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Artists & Cast",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(state.matchedArtists) { artist ->
                                        val isCurrentActive = state.activeArtist?.id == artist.id
                                        ArtistAvatarChip(
                                            artist = artist,
                                            isActive = isCurrentActive,
                                            onClick = { viewModel.selectArtist(artist) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Active Artist Spotlight Card
                    if (state.activeArtist != null) {
                        item {
                            ArtistSpotlightCard(
                                artist = state.activeArtist!!,
                                searchViewMode = state.searchViewMode,
                                movieCount = state.movieCount,
                                tvCount = state.tvCount,
                                mediaCount = state.mediaResults.size,
                                filmographyCount = state.artistCredits.size,
                                onToggleMode = {
                                    val target = if (state.searchViewMode == "artist") "media" else "artist"
                                    viewModel.setSearchViewMode(target)
                                },
                                onClose = { viewModel.clearActiveArtist() }
                            )
                        }
                    }

                    // 3. Results Section Header & Breakdown
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (state.searchViewMode == "artist")
                                        "${state.activeArtist?.name ?: "Artist"}'s Filmography"
                                    else
                                        "Search Results",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.results.size} titles found",
                                    color = AccentAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (state.movieCount > 0) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BgSurfaceElevated)
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${state.movieCount} Movies",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                if (state.tvCount > 0) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BgSurfaceElevated)
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${state.tvCount} TV Shows",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Results Grid or Empty Message
                    if (state.results.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No Titles Found",
                                subtitle = if (state.searchViewMode == "artist")
                                    "No titles found for ${state.activeArtist?.name} under this category."
                                else
                                    "No titles matched your search query. Try selecting an artist above or another keyword."
                            )
                        }
                    } else {
                        val chunked = state.results.chunked(posterGridCols)
                        items(chunked) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        MediaPosterCard(
                                            item = item,
                                            onClick = { onMediaClick(item) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                val emptySlots = posterGridCols - rowItems.size
                                repeat(emptySlots) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        if (state.hasMorePages && state.searchViewMode != "artist") {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                LoadMoreButton(
                                    isLoadingMore = state.isLoadingMore,
                                    onClick = { viewModel.loadMore() },
                                    text = "Load More Results"
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistAvatarChip(
    artist: PersonSearchItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .tvFocusable(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.08f,
                focusedBorderColor = AccentAmber,
                borderWidth = 2.dp,
                onClick = onClick
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) AccentAmber.copy(alpha = 0.2f) else BgSurfaceElevated)
            .border(
                1.5.dp,
                if (isActive) AccentAmber else BorderSubtle,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgCard)
                .border(1.dp, if (isActive) AccentAmber else BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (artist.profileUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artist.profileUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = artist.name,
                color = if (isActive) AccentAmber else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist.knownForDepartment ?: "Actor / Artist",
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ArtistSpotlightCard(
    artist: PersonSearchItem,
    searchViewMode: String,
    movieCount: Int,
    tvCount: Int,
    mediaCount: Int,
    filmographyCount: Int,
    onToggleMode: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgSurfaceElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large circular avatar
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(BgCard)
                    .border(2.dp, AccentAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (artist.profileUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artist.profileUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Badge Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (searchViewMode == "artist") "Artist Filmography" else "Artist Match",
                        color = AccentAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = artist.name,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Starring in $movieCount movies & $tvCount TV series",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                if (mediaCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .tvFocusable(
                                shape = RoundedCornerShape(8.dp),
                                focusedScale = 1.06f,
                                focusedBorderColor = AccentAmber,
                                borderWidth = 1.5.dp,
                                onClick = onToggleMode
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (searchViewMode == "artist") BgCard else AccentAmber.copy(alpha = 0.2f))
                            .border(1.dp, if (searchViewMode == "artist") BorderSubtle else AccentAmber, RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleMode)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (searchViewMode == "artist") Icons.Default.Movie else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (searchViewMode == "artist")
                                "View $mediaCount titles from search"
                            else
                                "View filmography ($filmographyCount titles)",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dismiss Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(28.dp)
                    .tvFocusable(
                        shape = CircleShape,
                        focusedScale = 1.15f,
                        focusedBorderColor = AccentAmber,
                        borderWidth = 1.5.dp,
                        onClick = onClose
                    )
                    .clip(CircleShape)
                    .background(BgCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
