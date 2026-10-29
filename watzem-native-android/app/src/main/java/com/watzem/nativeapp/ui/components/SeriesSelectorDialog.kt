package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.watzem.nativeapp.data.model.TVEpisode
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Compact, professional Series / Episode Selector dialog with seamless D-Pad support.
 * Allows quick jumping across seasons and episodes during playback or browsing.
 */
@Composable
fun SeriesSelectorDialog(
    title: String,
    currentSeason: Int,
    currentEpisode: Int,
    availableSeasons: List<Int>,
    episodes: List<TVEpisode>,
    isLoadingEpisodes: Boolean = false,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeSelected: (season: Int, episode: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSeason by remember(currentSeason) { mutableIntStateOf(currentSeason) }
    var focusedEpisode by remember {
        mutableStateOf(episodes.find { it.episodeNumber == currentEpisode } ?: episodes.firstOrNull())
    }

    val activeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(90L)
        try {
            activeFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC050608))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 360.dp, max = 540.dp)
                    .heightIn(max = 440.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF141620),
                                Color(0xFF0C0D13)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x33F59E0B),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22F59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = CinemaAmber,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Season $selectedSeason",
                                color = CinemaAmber,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (episodes.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${episodes.size} Episodes",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.15f,
                                focusedBorderColor = CinemaAmber,
                                borderWidth = 2.dp,
                                onClick = onDismiss
                            )
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Seasons Horizontal Row
                val seasonsList = if (availableSeasons.isNotEmpty()) availableSeasons else listOf(selectedSeason)
                if (seasonsList.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        items(seasonsList) { s ->
                            val isSelected = s == selectedSeason
                            Box(
                                modifier = Modifier
                                    .tvFocusable(
                                        shape = RoundedCornerShape(12.dp),
                                        focusedScale = 1.06f,
                                        focusedBorderColor = CinemaAmber,
                                        borderWidth = 2.dp,
                                        onClick = {
                                            selectedSeason = s
                                            onSeasonSelected(s)
                                        }
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) CinemaAmber else Color(0x1F222F)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CinemaAmber else Color(0x1CFFFFFF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Season $s",
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Episodes Grid Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 120.dp, max = 250.dp)
                ) {
                    if (isLoadingEpisodes) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = CinemaAmber,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Loading episodes...",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (episodes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No episodes found for Season $selectedSeason",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 58.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(episodes, key = { it.id }) { ep ->
                                val isCurrent = ep.episodeNumber == currentEpisode && selectedSeason == currentSeason

                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .then(
                                            if (isCurrent) Modifier.focusRequester(activeFocusRequester) else Modifier
                                        )
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                focusedEpisode = ep
                                            }
                                        }
                                        .tvFocusable(
                                            shape = RoundedCornerShape(8.dp),
                                            focusedScale = 1.08f,
                                            focusedBorderColor = CinemaAmber,
                                            borderWidth = 2.dp,
                                            onClick = {
                                                onEpisodeSelected(selectedSeason, ep.episodeNumber)
                                                onDismiss()
                                            }
                                        )
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isCurrent) CinemaAmber else Color(0x1F222F)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isCurrent) CinemaAmber else Color(0x1AFFFFFF),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "E${if (ep.episodeNumber < 10) "0${ep.episodeNumber}" else ep.episodeNumber}",
                                        color = if (isCurrent) Color.Black else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Focused Episode Preview Footer
                val previewEp = focusedEpisode ?: episodes.find { it.episodeNumber == currentEpisode }
                if (previewEp != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1A000000))
                            .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CinemaAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "E${previewEp.episodeNumber} • ${previewEp.name.ifBlank { "Episode ${previewEp.episodeNumber}" }}",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        previewEp.runtime?.let {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${it}m",
                                color = CinemaAmber,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

