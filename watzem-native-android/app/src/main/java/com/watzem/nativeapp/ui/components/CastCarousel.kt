package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watzem.nativeapp.data.model.CastMember
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.CinemaCard
import com.watzem.nativeapp.ui.theme.CinemaCardBorder
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@Composable
fun CastCarousel(
    cast: List<CastMember>,
    modifier: Modifier = Modifier,
    onMemberClick: ((CastMember) -> Unit)? = null
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(cast.take(15), key = { it.id }) { member ->
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .padding(end = 10.dp)
                    .tvFocusable(
                        enabled = onMemberClick != null,
                        shape = RoundedCornerShape(12.dp),
                        focusedScale = 1.1f,
                        focusedBorderColor = CinemaAmber,
                        borderWidth = 2.dp,
                        onClick = { onMemberClick?.invoke(member) }
                    )
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(CinemaCard)
                        .border(1.dp, CinemaCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (member.profileUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(member.profileUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = member.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = member.name,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                if (!member.character.isNullOrBlank()) {
                    Text(
                        text = member.character,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

