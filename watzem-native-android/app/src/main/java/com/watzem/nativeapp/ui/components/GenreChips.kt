package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.data.model.Genre
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.CinemaCard
import com.watzem.nativeapp.ui.theme.CinemaCardBorder
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@Composable
fun GenreChipsRow(
    genres: List<Genre>,
    selectedGenreId: Int?,
    onGenreSelected: (Genre?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // "All" chip
        item {
            val isSelected = selectedGenreId == null || selectedGenreId == 0
            GenreChip(
                name = "All",
                isSelected = isSelected,
                onClick = { onGenreSelected(null) }
            )
        }

        items(genres, key = { it.id }) { genre ->
            val isSelected = selectedGenreId == genre.id
            GenreChip(
                name = genre.name,
                isSelected = isSelected,
                onClick = { onGenreSelected(genre) }
            )
        }
    }
}

@Composable
fun GenreChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(end = 8.dp)
            .tvFocusable(
                shape = RoundedCornerShape(20.dp),
                focusedScale = 1.08f,
                focusedBorderColor = CinemaAmber,
                borderWidth = 2.dp,
                onClick = onClick
            )
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CinemaAmber else CinemaCard)
            .border(
                width = 1.dp,
                color = if (isSelected) CinemaAmber else CinemaCardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.Black else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

