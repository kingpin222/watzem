package com.watzem.nativeapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.watzem.nativeapp.ui.navigation.Screen
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CyanGlow = Color(0xFF06B6D4)
private val PurpleGlow = Color(0xFFC084FC)

@Composable
fun TvSidebar(
    currentRoute: String?,
    isTv: Boolean,
    watchlistCount: Int,
    isUpdateAvailable: Boolean = false,
    onModeChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onUpdateClick: () -> Unit = {},
    onTipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focusedItemKey by remember { mutableStateOf<String?>(null) }
    val isExpanded = focusedItemKey != null

    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 220.dp else 74.dp,
        animationSpec = tween(durationMillis = 240),
        label = "tvSidebarWidth"
    )

    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000L)
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    Box(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .zIndex(100f)
            // Translucent background with no border
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xF2090B14),
                        Color(0xD90A0D18)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top: Brand Header & Navigation Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand Logo Header
                TvSidebarBrand(
                    isExpanded = isExpanded,
                    onFocusChanged = { isFocused ->
                        focusedItemKey = if (isFocused) "brand" else if (focusedItemKey == "brand") null else focusedItemKey
                    },
                    onClick = {
                        onModeChange(false)
                        onNavigate(Screen.Home.route)
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Search
                val isSearchActive = currentRoute?.startsWith("search") == true
                TvSidebarItem(
                    icon = Icons.Default.Search,
                    label = "Search",
                    isActive = isSearchActive,
                    isExpanded = isExpanded,
                    activeColor = AccentAmber,
                    onFocusChanged = { isFocused ->
                        focusedItemKey = if (isFocused) "search" else if (focusedItemKey == "search") null else focusedItemKey
                    },
                    onClick = { onNavigate(Screen.Search.createRoute()) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Movies
                val isMoviesActive = (currentRoute == Screen.Home.route || currentRoute == null) && !isTv
                TvSidebarItem(
                    icon = Icons.Default.Movie,
                    label = "Movies",
                    isActive = isMoviesActive,
                    isExpanded = isExpanded,
                    activeColor = AccentAmber,
                    onFocusChanged = { isFocused ->
                        focusedItemKey = if (isFocused) "movies" else if (focusedItemKey == "movies") null else focusedItemKey
                    },
                    onClick = {
                        onModeChange(false)
                        onNavigate(Screen.Home.route)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 3. TV Series
                val isTvSeriesActive = (currentRoute == Screen.Home.route) && isTv
                TvSidebarItem(
                    icon = Icons.Default.Tv,
                    label = "TV Series",
                    isActive = isTvSeriesActive,
                    isExpanded = isExpanded,
                    activeColor = CyanGlow,
                    onFocusChanged = { isFocused ->
                        focusedItemKey = if (isFocused) "tv" else if (focusedItemKey == "tv") null else focusedItemKey
                    },
                    onClick = {
                        onModeChange(true)
                        onNavigate(Screen.Home.route)
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Explore
                val isExploreActive = currentRoute?.startsWith("explore") == true
                TvSidebarItem(
                    icon = Icons.Default.Explore,
                    label = "Explore",
                    isActive = isExploreActive,
                    isExpanded = isExpanded,
                    activeColor = PurpleGlow,
                    onFocusChanged = { isFocused ->
                        focusedItemKey = if (isFocused) "explore" else if (focusedItemKey == "explore") null else focusedItemKey
                    },
                    onClick = { onNavigate(Screen.Explore.route) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 5. Watchlist
                val isWatchlistActive = currentRoute?.startsWith("watchlist") == true
                TvSidebarItem(
                    icon = Icons.Default.Bookmark,
                    label = "Watchlist",
                    isActive = isWatchlistActive,
                    isExpanded = isExpanded,
                    activeColor = AccentAmber,
                    badgeCount = watchlistCount,
                    onFocusChanged = { isFocused ->
                        focusedItemKey = if (isFocused) "watchlist" else if (focusedItemKey == "watchlist") null else focusedItemKey
                    },
                    onClick = { onNavigate(Screen.Watchlist.route) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 6. Support (Includes Software Updates)
                TvSidebarItem(
                    icon = Icons.Default.Favorite,
                    label = if (isUpdateAvailable) "Support (Update)" else "Support",
                    isActive = false,
                    isExpanded = isExpanded,
                    activeColor = AccentRose,
                    hasDotBadge = isUpdateAvailable,
                    onFocusChanged = { isFocused ->
                        focusedItemKey = if (isFocused) "support" else if (focusedItemKey == "support") null else focusedItemKey
                    },
                    onClick = onTipClick
                )
            }

            // Bottom Footer: Live Clock
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Clock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Clock",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentTime,
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSidebarBrand(
    isExpanded: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "brandScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color(0x33F59E0B) else Color.Transparent)
            .then(
                if (isFocused) Modifier.border(2.dp, AccentAmber, RoundedCornerShape(12.dp))
                else Modifier
            )
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (isExpanded) 12.dp else 6.dp, vertical = 8.dp),
        contentAlignment = if (isExpanded) Alignment.CenterStart else Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Watzem Mark
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "W",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "WATZEM",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            )
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TV",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun TvSidebarItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isExpanded: Boolean,
    activeColor: Color = AccentAmber,
    badgeCount: Int = 0,
    hasDotBadge: Boolean = false,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "itemScale"
    )

    val bgModifier = when {
        isFocused -> Modifier.background(
            Brush.horizontalGradient(
                listOf(activeColor.copy(alpha = 0.32f), activeColor.copy(alpha = 0.12f))
            )
        )
        isActive -> Modifier.background(Color(0x22FFFFFF))
        else -> Modifier.background(Color.Transparent)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .then(bgModifier)
            .then(
                if (isFocused) Modifier.border(2.dp, activeColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .onFocusChanged { state ->
                isFocused = state.isFocused
                onFocusChanged(state.isFocused)
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (isExpanded) 12.dp else 6.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
            // Active left indicator bar
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(activeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = when {
                        isFocused || isActive -> activeColor
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(22.dp)
                )

                if (!isExpanded && (badgeCount > 0 || hasDotBadge)) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(if (hasDotBadge) AccentAmber else AccentRose)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    color = if (isFocused || isActive) Color.White else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isActive || isFocused) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (hasDotBadge && badgeCount <= 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AccentAmber.copy(alpha = 0.25f))
                            .border(1.dp, AccentAmber.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW",
                            color = AccentAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AccentRose.copy(alpha = 0.25f))
                            .border(1.dp, AccentRose.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            color = AccentRose,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
