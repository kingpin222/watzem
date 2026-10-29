package com.watzem.nativeapp.ui.screens.explore

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.ui.components.ErrorStateView
import com.watzem.nativeapp.ui.components.LoadMoreButton
import com.watzem.nativeapp.ui.components.LoadingSpinner
import com.watzem.nativeapp.ui.components.MediaPosterCard
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
fun ExploreScreen(
    viewModel: ExploreViewModel,
    isTv: Boolean,
    onMediaClick: (MediaItem) -> Unit
) {
    LaunchedEffect(isTv) {
        viewModel.setMode(isTv)
    }

    val state by viewModel.uiState.collectAsState()
    val activeColor = if (state.isTv) AccentRose else AccentAmber
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val posterGridCols = when {
        configuration.screenWidthDp >= 1300 -> 7
        configuration.screenWidthDp >= 800 -> 6
        configuration.screenWidthDp >= 600 -> 4
        configuration.screenWidthDp >= 450 -> 3
        else -> 2
    }
    val tileCols = when {
        configuration.screenWidthDp >= 1100 -> 4
        configuration.screenWidthDp >= 750 -> 3
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
        if (shouldLoadMore && !state.isLoadingMore && state.hasMorePages && state.activeTitle != null) {
            viewModel.loadMore()
        }
    }

    BackHandler(enabled = state.activeTitle != null) {
        viewModel.resetToOverview()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        if (state.activeTitle == null) {
            // Main Browse Categories Grid
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                // Section: Header Description
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = if (state.isTv) "Explore Series" else "Explore Cinema",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (state.isTv)
                                "Discover top shows by streaming network, genre, and release era"
                            else
                                "Browse blockbusters by streaming platform, genre, franchise, or studio",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Section 1: Quick Year Filter Strip (Browse by Year)
                item {
                    SectionHeader(
                        title = "Browse by Year",
                        icon = Icons.Default.CalendarToday,
                        horizontalPadding = 0.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        items(viewModel.releaseYears) { era ->
                            Row(
                                modifier = Modifier
                                    .tvFocusable(
                                        shape = CircleShape,
                                        focusedScale = 1.08f,
                                        focusedBorderColor = activeColor,
                                        borderWidth = 2.dp,
                                        onClick = { viewModel.openEra(era) }
                                    )
                                    .clip(CircleShape)
                                    .background(BgSurfaceElevated)
                                    .border(1.dp, BorderSubtle, CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = era.icon,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = era.name,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (era.badge != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(activeColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = era.badge,
                                            color = activeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Streaming & Networks (2-Column Cards Grid)
                item {
                    SectionHeader(
                        title = "Streaming & Networks",
                        icon = Icons.Default.Tv,
                        horizontalPadding = 0.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                val chunkedNetworks = viewModel.networks.chunked(tileCols)
                items(chunkedNetworks) { rowTiles ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTiles.forEach { network ->
                            Box(modifier = Modifier.weight(1f)) {
                                ExploreCardTile(
                                    name = network.name,
                                    icon = network.icon,
                                    activeColor = activeColor,
                                    onClick = { viewModel.openNetwork(network) }
                                )
                            }
                        }
                        val emptySlots = tileCols - rowTiles.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Section 3: Popular Genres (Responsive Cards Grid)
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(
                        title = "Popular Genres",
                        icon = Icons.Default.Bolt,
                        horizontalPadding = 0.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                val chunkedGenres = viewModel.genres.chunked(tileCols)
                items(chunkedGenres) { rowTiles ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTiles.forEach { genre ->
                            Box(modifier = Modifier.weight(1f)) {
                                ExploreCardTile(
                                    name = genre.name,
                                    icon = genre.icon,
                                    activeColor = activeColor,
                                    onClick = { viewModel.openGenre(genre) }
                                )
                            }
                        }
                        val emptySlots = tileCols - rowTiles.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Section 4: Release Years & Eras (Responsive Cards Grid)
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(
                        title = "Release Years & Eras",
                        icon = Icons.Default.History,
                        horizontalPadding = 0.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                val chunkedEras = viewModel.releaseYears.chunked(tileCols)
                items(chunkedEras) { rowTiles ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTiles.forEach { era ->
                            Box(modifier = Modifier.weight(1f)) {
                                ExploreCardTile(
                                    name = era.name,
                                    icon = era.icon,
                                    badge = era.badge,
                                    activeColor = activeColor,
                                    onClick = { viewModel.openEra(era) }
                                )
                            }
                        }
                        val emptySlots = tileCols - rowTiles.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Section 5: Movie Franchises (Cinema Mode Only)
                if (!state.isTv) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        SectionHeader(
                            title = "Movie Franchises",
                            icon = Icons.Default.Shield,
                            horizontalPadding = 0.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    val chunkedFranchises = viewModel.collections.chunked(tileCols)
                    items(chunkedFranchises) { rowTiles ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowTiles.forEach { franchise ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ExploreCardTile(
                                        name = franchise.name,
                                        icon = franchise.icon,
                                        activeColor = activeColor,
                                        onClick = { viewModel.openFranchise(franchise) }
                                    )
                                }
                            }
                            val emptySlots = tileCols - rowTiles.size
                            repeat(emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Section 6: Film Studios (Cinema Mode Only)
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        SectionHeader(
                            title = "Film Studios",
                            icon = Icons.Default.Movie,
                            horizontalPadding = 0.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    val chunkedStudios = viewModel.studios.chunked(tileCols)
                    items(chunkedStudios) { rowTiles ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowTiles.forEach { studio ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ExploreCardTile(
                                        name = studio.name,
                                        icon = studio.icon,
                                        activeColor = activeColor,
                                        onClick = { viewModel.openStudio(studio) }
                                    )
                                }
                            }
                            val emptySlots = tileCols - rowTiles.size
                            repeat(emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        } else {
            // Category Drill-Down View (Filtered Grid View)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar with Back Button and Title
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedScale = 1.06f,
                                    focusedBorderColor = activeColor,
                                    borderWidth = 1.5.dp,
                                    onClick = { viewModel.resetToOverview() }
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgSurfaceElevated)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Categories",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.activeTitle ?: "",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${state.items.size} titles",
                                color = activeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Category Secondary Year Filter Strip
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.yearFilters) { y ->
                            val isSelected = y == state.selectedFilterYear
                            val label = if (y == "all") "All Years" else y
                            Box(
                                modifier = Modifier
                                    .tvFocusable(
                                        shape = CircleShape,
                                        focusedScale = 1.08f,
                                        focusedBorderColor = activeColor,
                                        borderWidth = 2.dp,
                                        onClick = { viewModel.selectFilterYear(y) }
                                    )
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeColor else BgSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) activeColor else BorderSubtle,
                                        CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when {
                    state.isLoading -> {
                        item {
                            LoadingSpinner(modifier = Modifier.fillMaxWidth().height(260.dp))
                        }
                    }
                    state.error != null -> {
                        item {
                            ErrorStateView(
                                message = state.error ?: "Error loading media",
                                onRetry = { viewModel.loadCategoryItems(1) }
                            )
                        }
                    }
                    state.items.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No titles found for this category and year.",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        val chunked = state.items.chunked(posterGridCols)
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

                        if (state.hasMorePages) {
                            item {
                                LoadMoreButton(
                                    isLoadingMore = state.isLoadingMore,
                                    onClick = { viewModel.loadMore() },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
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
}

@Composable
fun ExploreModeChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .tvFocusable(
                shape = CircleShape,
                focusedScale = 1.08f,
                focusedBorderColor = activeColor,
                borderWidth = 2.dp,
                onClick = onClick
            )
            .clip(CircleShape)
            .background(if (isActive) activeColor else BgCard)
            .border(1.dp, if (isActive) activeColor else BorderSubtle, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.Black else TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isActive) Color.Black else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ExploreCardTile(
    name: String,
    icon: ImageVector,
    badge: String? = null,
    activeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.05f,
                focusedBorderColor = activeColor,
                borderWidth = 2.dp,
                onClick = onClick
            )
            .clip(RoundedCornerShape(12.dp))
            .background(BgSurfaceElevated)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(activeColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(activeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    color = activeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
