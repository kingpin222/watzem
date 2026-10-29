package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watzem.nativeapp.data.model.VideoItem
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.CinemaCard
import com.watzem.nativeapp.ui.theme.CinemaCardBorder
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@Composable
fun VideoTrailersRow(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val youtubeVideos = videos.filter { it.site.equals("YouTube", ignoreCase = true) }
    if (youtubeVideos.isEmpty()) return

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(youtubeVideos, key = { it.id }) { video ->
            Card(
                modifier = Modifier
                    .width(200.dp)
                    .padding(end = 12.dp)
                    .tvFocusable(
                        shape = RoundedCornerShape(10.dp),
                        focusedScale = 1.06f,
                        focusedBorderColor = CinemaAmber,
                        borderWidth = 2.dp,
                        onClick = { onVideoClick(video) }
                    ),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = CinemaCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CinemaCardBorder)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(video.youtubeThumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = video.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Dark tint
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x55000000))
                        )

                        // Play circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CinemaAmber.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Trailer",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Type badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.type,
                                color = CinemaAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = video.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "YouTube HD",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

