package com.watzem.nativeapp.ui.screens.detail

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.watzem.nativeapp.data.model.TVEpisode
import com.watzem.nativeapp.data.model.VideoItem
import com.watzem.nativeapp.ui.components.CastCarousel
import com.watzem.nativeapp.ui.components.ErrorStateView
import com.watzem.nativeapp.ui.components.LoadingSpinner
import com.watzem.nativeapp.ui.components.MediaPosterCard
import com.watzem.nativeapp.ui.components.SectionHeader
import com.watzem.nativeapp.ui.components.TrailerPlayerDialog
import com.watzem.nativeapp.ui.components.VideoTrailersRow
import com.watzem.nativeapp.ui.components.tvFocusable
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.BgCard
import com.watzem.nativeapp.ui.theme.BgMain
import com.watzem.nativeapp.ui.theme.BgSurfaceElevated
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.RatingBadgeBg
import com.watzem.nativeapp.ui.theme.StarGold
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onWatchClick: (mediaId: Int, isTv: Boolean, title: String, backdropUrl: String?) -> Unit,
    onWatchEpisodeClick: (mediaId: Int, isTv: Boolean, title: String, season: Int, episode: Int, backdropUrl: String?) -> Unit,
    onTrailerClick: (VideoItem) -> Unit,
    onSimilarMediaClick: (MediaItem) -> Unit,
    onSearchKeyword: (String) -> Unit,
    onArtistClick: ((personId: Int, name: String, profilePath: String?) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler {
        onBackClick()
    }

    when {
        state.isLoading -> {
            LoadingSpinner(
                modifier = Modifier.fillMaxSize(),
                message = "Loading media details..."
            )
        }
        state.error != null -> {
            ErrorStateView(
                message = state.error ?: "Error loading title",
                onRetry = { viewModel.loadDetails() },
                modifier = Modifier.fillMaxSize()
            )
        }
        state.details != null -> {
            val details = state.details!!
            val isTv = details.numberOfSeasons != null || details.safeSeasons.isNotEmpty()
            val activeColor = if (isTv) AccentRose else AccentAmber
            var playingTrailer by remember { mutableStateOf<VideoItem?>(null) }
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgMain)
            ) {
                if (isLandscape) {
                    // Landscape Cinema Hero: Full-bleed backdrop in background with dark vignette,
                    // and Title, Rating, Year, Duration, Genres, and primary "Watch" button immediately visible upfront!
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((configuration.screenHeightDp * 0.92f).dp.coerceIn(380.dp, 600.dp))
                        ) {
                            // 1. Movie Backdrop
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(details.backdropUrl ?: details.posterUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = details.displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // 2. Cinematic Vignette (Deep dark on the left so content pops, soft fade across & down)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                BgMain.copy(alpha = 0.98f),
                                                BgMain.copy(alpha = 0.94f),
                                                BgMain.copy(alpha = 0.78f),
                                                Color(0x33000000),
                                                Color.Transparent
                                            ),
                                            startX = 0f,
                                            endX = 1400f
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x66000000),
                                                Color.Transparent,
                                                BgMain.copy(alpha = 0.8f),
                                                BgMain
                                            )
                                        )
                                    )
                            )

                            // 3. Left-aligned Hero Content (Back button, Title, Metadata, Watch Button)
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.58f)
                                    .padding(start = 28.dp, top = 20.dp, bottom = 18.dp, end = 16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top row: Back button
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .tvFocusable(
                                            shape = CircleShape,
                                            focusedScale = 1.15f,
                                            focusedBorderColor = activeColor,
                                            borderWidth = 2.dp,
                                            onClick = onBackClick
                                        )
                                        .clip(CircleShape)
                                        .background(Color(0x99000000))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Middle section: Title, Tagline, Metadata, Genres, Storyline preview
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = details.displayTitle,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!details.tagline.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "“${details.tagline}”",
                                            color = activeColor,
                                            fontSize = 13.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Rating, Year, Duration, Status Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (details.voteAverage > 0) {
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
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = details.ratingFormatted,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        if (details.releaseYear.isNotBlank()) {
                                            Text(
                                                text = details.releaseYear,
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        if (details.durationOrSeasons.isNotBlank()) {
                                            Text(text = "•", color = TextMuted)
                                            Text(
                                                text = details.durationOrSeasons,
                                                color = TextSecondary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        if (!details.status.isNullOrBlank()) {
                                            Text(text = "•", color = TextMuted)
                                            Text(
                                                text = details.status,
                                                color = activeColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Genre Chips
                                    if (details.safeGenres.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            details.safeGenres.take(4).forEach { genre ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(BgCard)
                                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = genre.name,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Storyline Preview
                                    if (details.overview.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = details.overview,
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Bottom Actions: WATCH NOW + Bookmark + Trailer + Share
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onWatchClick(details.id, isTv, details.displayTitle, details.backdropUrl ?: details.posterUrl) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = activeColor,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .tvFocusable(
                                                shape = RoundedCornerShape(10.dp),
                                                focusedScale = 1.05f,
                                                focusedBorderColor = Color.White,
                                                borderWidth = 2.dp
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Watch",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isTv) "Stream Episodes" else "Watch Movie",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    // Watchlist Bookmark Button
                                    OutlinedButton(
                                        onClick = { viewModel.toggleWatchlist() },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.tvFocusable(
                                            shape = RoundedCornerShape(10.dp),
                                            focusedScale = 1.05f,
                                            focusedBorderColor = activeColor,
                                            borderWidth = 2.dp
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (state.isWatchlisted) activeColor else TextPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (state.isWatchlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Watchlist",
                                            tint = if (state.isWatchlisted) activeColor else TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Trailer Button if available
                                    details.primaryTrailer?.let { trailer ->
                                        OutlinedButton(
                                            onClick = {
                                                playingTrailer = trailer
                                                onTrailerClick(trailer)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.tvFocusable(
                                                shape = RoundedCornerShape(10.dp),
                                                focusedScale = 1.05f,
                                                focusedBorderColor = activeColor,
                                                borderWidth = 2.dp
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = "Trailer",
                                                tint = activeColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Share Button
                                    OutlinedButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Watch ${details.displayTitle} on WATZEM!")
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Share ${details.displayTitle}")
                                            context.startActivity(shareIntent)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.tvFocusable(
                                            shape = RoundedCornerShape(10.dp),
                                            focusedScale = 1.05f,
                                            focusedBorderColor = Color.White,
                                            borderWidth = 2.dp
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Top Backdrop with Back Button and Overlay (Portrait)
                    item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(details.backdropUrl ?: details.posterUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = details.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Vertical Gradient Fade into Background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x99000000),
                                            Color.Transparent,
                                            BgMain.copy(alpha = 0.8f),
                                            BgMain
                                        ),
                                        startY = 0f
                                    )
                                )
                        )

                        // Back navigation circle button
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .padding(top = 36.dp, start = 16.dp)
                                .size(40.dp)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.15f,
                                    focusedBorderColor = activeColor,
                                    borderWidth = 2.dp,
                                    onClick = onBackClick
                                )
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        // Rating badge overlay bottom-right of backdrop
                        if (details.voteAverage > 0) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RatingBadgeBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = StarGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = details.ratingFormatted,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = " (${details.voteCount})",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Title, Tagline & Metadata
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = details.displayTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        if (!details.tagline.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "“${details.tagline}”",
                                color = activeColor,
                                fontSize = 13.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Year, Duration/Seasons, Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (details.releaseYear.isNotBlank()) {
                                Text(
                                    text = details.releaseYear,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            if (details.durationOrSeasons.isNotBlank()) {
                                Text(text = "•", color = TextMuted)
                                Text(
                                    text = details.durationOrSeasons,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            if (!details.status.isNullOrBlank()) {
                                Text(text = "•", color = TextMuted)
                                Text(
                                    text = details.status,
                                    color = activeColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Genre tags
                        if (details.safeGenres.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                details.safeGenres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BgCard)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre.name,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Main Action Buttons: Watch Now, Watchlist, Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onWatchClick(details.id, isTv, details.displayTitle, details.backdropUrl ?: details.posterUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(10.dp),
                                        focusedScale = 1.05f,
                                        focusedBorderColor = Color.White,
                                        borderWidth = 2.dp
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTv) "Stream Episodes" else "Watch Movie",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Watchlist Bookmark Button
                            OutlinedButton(
                                onClick = { viewModel.toggleWatchlist() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.05f,
                                    focusedBorderColor = activeColor,
                                    borderWidth = 2.dp
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (state.isWatchlisted) activeColor else TextPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (state.isWatchlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (state.isWatchlisted) activeColor else TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Share Button
                            OutlinedButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Watch ${details.displayTitle} on WATZEM!")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share ${details.displayTitle}")
                                    context.startActivity(shareIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.05f,
                                    focusedBorderColor = Color.White,
                                    borderWidth = 2.dp
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Trailer button if available
                        details.primaryTrailer?.let { trailer ->
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    playingTrailer = trailer
                                    onTrailerClick(trailer)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(
                                        shape = RoundedCornerShape(10.dp),
                                        focusedScale = 1.04f,
                                        focusedBorderColor = activeColor,
                                        borderWidth = 2.dp
                                    ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Trailer",
                                    tint = activeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Watch Official Trailer",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Synopsis / Storyline
                        Text(
                            text = "Storyline",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (details.overview.isNotBlank()) details.overview else "No synopsis available for this title.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

                // TV Seasons & Episodes Guide Section (Only when isTv)
                if (isTv) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = activeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Episodes Guide",
                                        color = TextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (state.episodes.isNotEmpty()) {
                                    Text(
                                        text = "${state.episodes.size} EPISODES",
                                        color = activeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Seasons Horizontal Selector Tabs
                            val seasons = details.safeSeasons.filter { it.seasonNumber > 0 }
                            if (seasons.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(seasons) { s ->
                                        val isSelected = s.seasonNumber == state.activeSeasonNumber
                                        Box(
                                            modifier = Modifier
                                                .tvFocusable(
                                                    shape = RoundedCornerShape(20.dp),
                                                    focusedScale = 1.08f,
                                                    focusedBorderColor = activeColor,
                                                    borderWidth = 2.dp,
                                                    onClick = { viewModel.selectSeason(s.seasonNumber) }
                                                )
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) activeColor else BgSurfaceElevated)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) activeColor else BorderSubtle,
                                                    RoundedCornerShape(20.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = s.name.ifBlank { "Season ${s.seasonNumber}" },
                                                color = if (isSelected) Color.Black else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Episodes List
                    if (state.isLoadingEpisodes) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = activeColor, modifier = Modifier.size(24.dp))
                            }
                        }
                    } else if (state.episodes.isNotEmpty()) {
                        items(state.episodes, key = { "ep_${it.id}" }) { ep ->
                            EpisodeCard(
                                episode = ep,
                                activeColor = activeColor,
                                onClick = {
                                    onWatchEpisodeClick(
                                        details.id,
                                        true,
                                        details.displayTitle,
                                        state.activeSeasonNumber,
                                        ep.episodeNumber,
                                        ep.stillUrl ?: details.backdropUrl ?: details.posterUrl
                                    )
                                }
                            )
                        }
                    }
                }

                // Media Details Section (Director, Language, Status, Budget, Box Office, Network)
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Media Details",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (details.safeCreatedBy.isNotEmpty()) {
                            DetailInfoRow("Creator", details.safeCreatedBy.joinToString { it.name })
                        }
                        details.status?.let { DetailInfoRow("Status", it) }
                        details.originalLanguage?.let { DetailInfoRow("Language", it.uppercase()) }
                        details.formattedBudget?.let { DetailInfoRow("Budget", it) }
                        details.formattedRevenue?.let { DetailInfoRow("Box Office", it) }
                        if (details.safeNetworks.isNotEmpty()) {
                            DetailInfoRow("Network", details.safeNetworks.joinToString { it.name })
                        }
                    }
                }

                // Keywords Section
                details.keywords?.allKeywords?.takeIf { it.isNotEmpty() }?.let { keywords ->
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = activeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Keywords",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                keywords.take(12).forEach { kw ->
                                    Box(
                                        modifier = Modifier
                                            .tvFocusable(
                                                shape = RoundedCornerShape(8.dp),
                                                focusedScale = 1.08f,
                                                focusedBorderColor = activeColor,
                                                borderWidth = 1.5.dp,
                                                onClick = { onSearchKeyword(kw) }
                                            )
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BgSurfaceElevated)
                                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "#$kw",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Cast & Crew
                details.credits?.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "Top Cast & Characters")
                        CastCarousel(
                            cast = cast,
                            onMemberClick = { member ->
                                if (onArtistClick != null) {
                                    onArtistClick(member.id, member.name, member.profilePath)
                                } else {
                                    onSearchKeyword(member.name)
                                }
                            }
                        )
                    }
                }

                // Official Trailers & Clips Row
                details.videos?.results?.takeIf { it.isNotEmpty() }?.let { videos ->
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(
                            title = "Trailers & Previews",
                            icon = Icons.Default.Videocam
                        )
                        VideoTrailersRow(
                            videos = videos,
                            onVideoClick = {
                                playingTrailer = it
                                onTrailerClick(it)
                            }
                        )
                    }
                }

                // Similar / Recommended Media
                details.similar?.results?.takeIf { it.isNotEmpty() }?.let { similar ->
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "You Might Also Like")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(similar, key = { it.id }) { item ->
                                MediaPosterCard(
                                    item = item,
                                    onClick = { onSimilarMediaClick(item) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }

            playingTrailer?.let { trailer ->
                TrailerPlayerDialog(
                    trailer = trailer,
                    onDismiss = { playingTrailer = null }
                )
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EpisodeCard(
    episode: TVEpisode,
    activeColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.04f,
                focusedBorderColor = activeColor,
                borderWidth = 2.dp,
                onClick = onClick
            )
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Still or Episode Badge
        Box(
            modifier = Modifier
                .size(width = 86.dp, height = 54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BgSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (episode.stillUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.stillUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = activeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Episode Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "E${episode.episodeNumber} • ${episode.name.ifBlank { "Episode ${episode.episodeNumber}" }}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (episode.voteAverage > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", episode.voteAverage),
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (!episode.airDate.isNullOrBlank() || episode.runtime != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    episode.airDate?.let {
                        Text(text = it, color = TextMuted, fontSize = 10.sp)
                    }
                    episode.runtime?.let {
                        Text(text = "${it}m", color = TextMuted, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            if (episode.overview.isNotBlank()) {
                Text(
                    text = episode.overview,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
