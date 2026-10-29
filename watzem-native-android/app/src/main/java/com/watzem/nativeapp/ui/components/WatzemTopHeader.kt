package com.watzem.nativeapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.BgMain
import com.watzem.nativeapp.ui.theme.BgSurfaceElevated
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@Composable
fun WatzemTopHeader(
    isTv: Boolean,
    onModeChange: (isTv: Boolean) -> Unit,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val cutoutInsets = WindowInsets.displayCutout.asPaddingValues()
    // Safe margins for physical notch, punch-hole cameras, and dynamic islands
    val safeTopPadding = maxOf(cutoutInsets.calculateTopPadding(), 12.dp)
    val safeStartPadding = maxOf(cutoutInsets.calculateStartPadding(layoutDirection), 16.dp)
    val safeEndPadding = maxOf(cutoutInsets.calculateEndPadding(layoutDirection), 16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Translucent background with no border
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE60A0D18), // Translucent dark glass
                        Color(0xCC0A0D18),
                        Color(0x990A0D18)
                    )
                )
            )
            .padding(
                start = safeStartPadding,
                end = safeEndPadding,
                top = safeTopPadding,
                bottom = 10.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo
            WatzemLogo(
                isTv = isTv,
                onClick = onLogoClick
            )

            // Mode Switcher Pill
            ModeSwitcherPill(
                isTv = isTv,
                onModeChange = onModeChange
            )
        }
    }
}

@Composable
fun ModeSwitcherPill(
    isTv: Boolean,
    onModeChange: (isTv: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillWidth = 146.dp
    val pillHeight = 32.dp

    BoxWithConstraints(
        modifier = modifier
            .width(pillWidth)
            .height(pillHeight)
            .clip(CircleShape)
            .background(Color(0x28FFFFFF))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0x40FFFFFF),
                        Color(0x15FFFFFF)
                    )
                ),
                shape = CircleShape
            )
            .padding(3.dp)
    ) {
        val halfWidth = maxWidth / 2f
        val thumbOffset by animateFloatAsState(
            targetValue = if (isTv) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "thumb_offset"
        )
        val thumbColor by animateColorAsState(
            targetValue = if (isTv) AccentRose else AccentAmber,
            label = "thumb_color"
        )

        // Animated Sliding Capsule Thumb
        Box(
            modifier = Modifier
                .offset(x = halfWidth * thumbOffset)
                .width(halfWidth)
                .height(pillHeight - 6.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )

        // Two Buttons Overlay
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Movies Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(pillHeight - 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isTv) onModeChange(false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "Movies",
                        tint = if (!isTv) Color.Black else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Movies",
                        color = if (!isTv) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (!isTv) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // TV Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(pillHeight - 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isTv) onModeChange(true)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "TV",
                        tint = if (isTv) Color.White else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TV",
                        color = if (isTv) Color.White else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isTv) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
