package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.TextPrimary

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CinemaAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            // Glowing Amber Accent Dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(CinemaAmber)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (onSeeAllClick != null) {
            Box(
                modifier = Modifier
                    .tvFocusable(
                        shape = RoundedCornerShape(8.dp),
                        focusedScale = 1.1f,
                        focusedBorderColor = CinemaAmber,
                        borderWidth = 1.5.dp,
                        onClick = onSeeAllClick
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = "See All",
                    color = CinemaAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
