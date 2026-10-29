package com.watzem.nativeapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.ui.components.ErrorStateView
import com.watzem.nativeapp.ui.components.LoadMoreButton
import com.watzem.nativeapp.ui.components.LoadingSpinner
import com.watzem.nativeapp.ui.components.MediaBackdropCard
import com.watzem.nativeapp.ui.components.MediaPosterCard
import com.watzem.nativeapp.ui.components.RotatingHeroBanner
import com.watzem.nativeapp.ui.components.SectionHeader
import com.watzem.nativeapp.ui.components.tvFocusable
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.BgCard
import com.watzem.nativeapp.ui.theme.BgMain
import com.watzem.nativeapp.ui.theme.BgSurfaceElevated
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isTv: Boolean,
    onMediaClick: (MediaItem) -> Unit,
    onWatchClick: (MediaItem) -> Unit,
    onSeeAllExplore: () -> Unit
) {
    LaunchedEffect(isTv) {
        viewModel.setMode(isTv)
    }

    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val activeColor = if (isTv) AccentRose else AccentAmber
    val configuration = LocalConfiguration.current
    val gridCols = when {
        configuration.screenWidthDp >= 1300 -> 7
        configuration.screenWidthDp >= 800 -> 6
        configuration.screenWidthDp >= 600 -> 4
        configuration.screenWidthDp >= 450 -> 3
        else -> 2
    }

    val movieCategories = remember {
        listOf(
            HomeCategory("popular", "Popular", Icons.Default.Whatshot),
            HomeCategory("trending", "Trending", Icons.AutoMirrored.Filled.TrendingUp),
            HomeCategory("imdb_top", "IMDb Top", Icons.Default.Star),
            HomeCategory("now_playing", "In Theaters", Icons.Default.Movie),
            HomeCategory("upcoming", "Upcoming", Icons.Default.CalendarToday)
        )
    }

    val tvCategories = remember {
        listOf(
            HomeCategory("popular", "Popular", Icons.Default.Whatshot),
            HomeCategory("trending", "Trending", Icons.AutoMirrored.Filled.TrendingUp),
            HomeCategory("imdb_top", "IMDb Top", Icons.Default.Star),
            HomeCategory("on_the_air", "On Air", Icons.Default.Tv),
            HomeCategory("airing_today", "Airing Today", Icons.Default.Schedule)
        )
    }

    val activeCategories = if (isTv) tvCategories else movieCategories

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingMore && state.hasMorePages) {
            viewModel.loadMore()
        }
    }

    when {
        state.isLoading -> {
            LoadingSpinner(
                modifier = Modifier.fillMaxSize(),
                message = if (isTv) "Loading TV series catalog..." else "Loading cinema catalog..."
            )
        }
        state.error != null -> {
            ErrorStateView(
                message = state.error ?: "An unexpected error occurred",
                onRetry = { viewModel.loadHomeData() },
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgMain)
            ) {
                // Auto-Rotating Hero Banner (up to 10 rotating featured titles)
                item {
                    if (state.heroItems.isNotEmpty()) {
                        RotatingHeroBanner(
                            items = state.heroItems,
                            onWatchClick = { onWatchClick(it) },
                            onDetailsClick = { onMediaClick(it) }
                        )
                    }
                }

                // Sticky/Category Bar matching WATZEM
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeCategories) { cat ->
                            val isSelected = cat.key == state.activeCategory
                            Row(
                                modifier = Modifier
                                    .tvFocusable(
                                        shape = CircleShape,
                                        focusedScale = 1.08f,
                                        focusedBorderColor = activeColor,
                                        borderWidth = 2.dp,
                                        onClick = { viewModel.selectCategory(cat.key) }
                                    )
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeColor else BgSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) activeColor else BorderSubtle,
                                        CircleShape
                                    )
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else activeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.label,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // If a non-popular filter is active, directly display the category feed grid!
                if (state.activeCategory != "popular") {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val catLabel = activeCategories.firstOrNull { it.key == state.activeCategory }?.label ?: "Catalog"
                            Text(
                                text = "$catLabel ${if (isTv) "Series" else "Movies"}",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.categoryItems.size} titles",
                                color = activeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Render items in responsive grid
                    val chunked = state.categoryItems.chunked(gridCols)
                    items(chunked) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in rowItems) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            val emptySlots = gridCols - rowItems.size
                            repeat(emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    // Default "Popular" feed: Curated Shelves + Infinite Grid
                    // Section 1: Trending Now
                    item {
                        SectionHeader(
                            title = if (isTv) "Trending Series" else "Trending Cinema",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            onSeeAllClick = onSeeAllExplore
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.section1Items, key = { "trend_${it.id}" }) { item ->
                                MediaPosterCard(
                                    item = item,
                                    onClick = { onMediaClick(item) }
                                )
                            }
                        }
                    }

                    // Section 2: Popular (Wide Backdrops)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            title = if (isTv) "Popular TV Series" else "Blockbuster Movies",
                            icon = if (isTv) Icons.Default.Tv else Icons.Default.Movie,
                            onSeeAllClick = onSeeAllExplore
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.section2Items, key = { "pop_${it.id}" }) { item ->
                                MediaBackdropCard(
                                    item = item,
                                    onClick = { onMediaClick(item) }
                                )
                            }
                        }
                    }

                    // Section 3: Top Rated
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            title = if (isTv) "Critically Acclaimed Series" else "Top Rated Cinema",
                            icon = Icons.Default.Star,
                            onSeeAllClick = onSeeAllExplore
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.section3Items, key = { "top_${it.id}" }) { item ->
                                MediaPosterCard(
                                    item = item,
                                    onClick = { onMediaClick(item) }
                                )
                            }
                        }
                    }

                    // Section 4: Upcoming / Airing
                    if (state.section4Items.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(
                                title = if (isTv) "Airing Recently" else "Coming Soon to Theaters",
                                icon = if (isTv) Icons.Default.Schedule else Icons.Default.CalendarToday
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.section4Items, key = { "sec4_${it.id}" }) { item ->
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) }
                                    )
                                }
                            }
                        }
                    }

                    // Genre Spotlight 1 (Sci-Fi)
                    if (state.genre1Items.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(
                                title = if (isTv) "Sci-Fi & Fantasy" else "Sci-Fi & Cyberpunk",
                                icon = Icons.Default.RocketLaunch
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.genre1Items, key = { "g1_${it.id}" }) { item ->
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) }
                                    )
                                }
                            }
                        }
                    }

                    // Genre Spotlight 2 (Action / Crime)
                    if (state.genre2Items.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(
                                title = if (isTv) "Crime & Thrillers" else "High Octane Action",
                                icon = if (isTv) Icons.Default.LocalPolice else Icons.Default.Bolt
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.genre2Items, key = { "g2_${it.id}" }) { item ->
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) }
                                    )
                                }
                            }
                        }
                    }

                    // Explore Grid Header
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isTv) "Explore TV Series" else "Explore Cinema",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.categoryItems.size} titles",
                                color = activeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Chunked responsive grid of popular feed
                    val chunked = state.categoryItems.chunked(gridCols)
                    items(chunked) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in rowItems) {
                                Box(modifier = Modifier.weight(1f)) {
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            val emptySlots = gridCols - rowItems.size
                            repeat(emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Load More Button
                if (state.hasMorePages) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        LoadMoreButton(
                            isLoadingMore = state.isLoadingMore,
                            onClick = { viewModel.loadMore() },
                            text = if (isTv) "Load More Series" else "Load More Cinema"
                        )
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
