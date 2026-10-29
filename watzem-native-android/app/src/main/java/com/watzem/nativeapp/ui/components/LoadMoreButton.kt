package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.CinemaCard
import com.watzem.nativeapp.ui.theme.CinemaCardBorder
import com.watzem.nativeapp.ui.theme.TextPrimary

@Composable
fun LoadMoreButton(
    isLoadingMore: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Load More Titles"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .tvFocusable(
                    enabled = !isLoadingMore,
                    shape = RoundedCornerShape(12.dp),
                    focusedScale = 1.04f,
                    focusedBorderColor = CinemaAmber,
                    borderWidth = 2.dp,
                    onClick = onClick
                )
                .clip(RoundedCornerShape(12.dp))
                .background(CinemaCard)
                .border(1.dp, if (isLoadingMore) CinemaAmber else CinemaCardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoadingMore) {
                CircularProgressIndicator(
                    color = CinemaAmber,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Loading more...",
                    color = CinemaAmber,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = CinemaAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

