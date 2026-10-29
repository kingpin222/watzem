package com.watzem.nativeapp.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.CinemaBackground
import com.watzem.nativeapp.ui.theme.RatingBadgeBg
import com.watzem.nativeapp.ui.theme.StarGold
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ROTATION_INTERVAL_MS = 6500L // 6.5s matching WATZEM

@Composable
fun RotatingHeroBanner(
    items: List<MediaItem>,
    onWatchClick: (MediaItem) -> Unit,
    onDetailsClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val displayItems = items.take(10)
    val pagerState = rememberPagerState(pageCount = { displayItems.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-rotate every 6.5 seconds when not manually dragged
    LaunchedEffect(isDragged, displayItems.size) {
        if (!isDragged && displayItems.size > 1) {
            while (true) {
                delay(ROTATION_INTERVAL_MS)
                val nextPage = (pagerState.currentPage + 1) % displayItems.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 750)
                )
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val bannerModifier = if (isLandscape) {
        modifier
            .fillMaxWidth()
            .height(minOf(310.dp, (configuration.screenHeightDp * 0.58f).dp))
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
    }

    Box(
        modifier = bannerModifier
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = displayItems[page]
            HeroBannerSlide(
                item = item,
                isLandscape = isLandscape,
                onWatchClick = { onWatchClick(item) },
                onDetailsClick = { onDetailsClick(item) }
            )
        }

        // Left / Right Navigation Chevrons
        if (displayItems.size > 1) {
            // Previous Button
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val prev = if (pagerState.currentPage > 0) pagerState.currentPage - 1 else displayItems.size - 1
                        pagerState.animateScrollToPage(prev)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(36.dp)
                    .tvFocusable(
                        shape = CircleShape,
                        focusedScale = 1.15f,
                        focusedBorderColor = CinemaAmber,
                        borderWidth = 2.dp,
                        onClick = {
                            coroutineScope.launch {
                                val prev = if (pagerState.currentPage > 0) pagerState.currentPage - 1 else displayItems.size - 1
                                pagerState.animateScrollToPage(prev)
                            }
                        }
                    )
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = "Previous Banner",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Next Button
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val next = (pagerState.currentPage + 1) % displayItems.size
                        pagerState.animateScrollToPage(next)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(36.dp)
                    .tvFocusable(
                        shape = CircleShape,
                        focusedScale = 1.15f,
                        focusedBorderColor = CinemaAmber,
                        borderWidth = 2.dp,
                        onClick = {
                            coroutineScope.launch {
                                val next = (pagerState.currentPage + 1) % displayItems.size
                                pagerState.animateScrollToPage(next)
                            }
                        }
                    )
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Next Banner",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Pagination Dots Indicator at Top Right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x88000000))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayItems.indices.forEach { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 6.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "dot_width"
                )

                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) CinemaAmber else Color(0x66FFFFFF))
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun HeroBannerSlide(
    item: MediaItem,
    isLandscape: Boolean,
    onWatchClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDetailsClick() }
    ) {
        // High-res backdrop
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.backdropUrl ?: item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.displayTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CinemaBackground.copy(alpha = 0.55f),
                            CinemaBackground
                        ),
                        startY = 60f
                    )
                )
        )

        // Slide Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Badges row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isTv = item.rawMediaType == "tv"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CinemaAmber)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isTv) "FEATURED SERIES" else "FEATURED CINEMA",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (item.voteAverage > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RatingBadgeBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = StarGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.ratingFormatted,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (item.releaseYear.isNotBlank()) {
                    Text(
                        text = "•   ${item.releaseYear}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = item.displayTitle,
                color = TextPrimary,
                fontSize = if (isLandscape) 22.sp else 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.overview,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth(if (isLandscape) 0.65f else 1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Watch Now Button
                Row(
                    modifier = Modifier
                        .tvFocusable(
                            shape = RoundedCornerShape(10.dp),
                            focusedScale = 1.08f,
                            focusedBorderColor = Color.White,
                            borderWidth = 2.dp,
                            onClick = onWatchClick
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(CinemaAmber)
                        .clickable(onClick = onWatchClick)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch Now",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Details Button
                Row(
                    modifier = Modifier
                        .tvFocusable(
                            shape = RoundedCornerShape(10.dp),
                            focusedScale = 1.08f,
                            focusedBorderColor = CinemaAmber,
                            borderWidth = 2.dp,
                            onClick = onDetailsClick
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDetailsClick)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Details",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
