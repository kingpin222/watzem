package com.watzem.nativeapp.ui.screens.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watzem.nativeapp.data.model.MediaType
import com.watzem.nativeapp.data.model.WatchlistItem
import com.watzem.nativeapp.ui.components.tvFocusable
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.BgCard
import com.watzem.nativeapp.ui.theme.BgMain
import com.watzem.nativeapp.ui.theme.BgSurfaceElevated
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.RatingBadgeBg
import com.watzem.nativeapp.ui.theme.StarGold
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onItemClick: (id: Int, isTv: Boolean) -> Unit,
    onExploreClick: () -> Unit
) {
    val allItems by viewModel.watchlistItems.collectAsState()
    var filterType by remember { mutableStateOf("all") } // "all", "movie", "tv"

    val filteredItems = remember(allItems, filterType) {
        when (filterType) {
            "movie" -> allItems.filter { it.mediaType == MediaType.MOVIE }
            "tv" -> allItems.filter { it.mediaType == MediaType.TV }
            else -> allItems
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "My Watchlist",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Saved movies and TV series",
                color = TextSecondary,
                fontSize = 12.sp
            )

            // Filter Pills when watchlist has items
            if (allItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair("all", "All (${allItems.size})"),
                        Pair("movie", "Movies"),
                        Pair("tv", "TV")
                    ).forEach { (key, label) ->
                        val isSelected = key == filterType
                        Box(
                            modifier = Modifier
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.08f,
                                    focusedBorderColor = AccentAmber,
                                    borderWidth = 2.dp,
                                    onClick = { filterType = key }
                                )
                                .clip(CircleShape)
                                .background(if (isSelected) AccentAmber else BgSurfaceElevated)
                                .border(1.dp, if (isSelected) AccentAmber else BorderSubtle, CircleShape)
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (allItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AccentAmber.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Watchlist is empty",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the bookmark icon on any movie or series to save it for quick access anytime.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentAmber,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Explore Movies & Shows",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    WatchlistCard(
                        item = item,
                        onClick = { onItemClick(item.id, item.mediaType == MediaType.TV) },
                        onRemove = { viewModel.removeFromWatchlist(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchlistCard(
    item: WatchlistItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.08f,
                focusedBorderColor = AccentAmber,
                borderWidth = 2.5.dp,
                onClick = onClick
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top right remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = "Remove from watchlist",
                        tint = AccentAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Type badge (Movie vs TV)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (item.mediaType == MediaType.TV) "TV" else "MOVIE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom rating
                if (item.voteAverage > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(RatingBadgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = StarGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", item.voteAverage),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.releaseYear,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
