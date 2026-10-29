package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.CinemaCard
import com.watzem.nativeapp.ui.theme.CinemaCardBorder
import com.watzem.nativeapp.ui.theme.RatingBadgeBg
import com.watzem.nativeapp.ui.theme.StarGold
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@Composable
fun MediaPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(125.dp)
) {
    Card(
        modifier = modifier
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.08f,
                focusedBorderColor = AccentAmber,
                borderWidth = 2.5.dp,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CinemaCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaCardBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(CinemaCard)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom subtle gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000))
                            )
                        )
                )

                // Rating badge top-right
                if (item.voteAverage > 0) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(RatingBadgeBg)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
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
                            text = item.ratingFormatted,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Media type badge (TV / Movie) top-left
                val typeLabel = if (item.rawMediaType == "tv") "TV" else "MOVIE"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xB31E293B))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeLabel,
                        color = Color(0xFFE2E8F0),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Title & Year
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.releaseYear.isNotBlank()) item.releaseYear else " ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

