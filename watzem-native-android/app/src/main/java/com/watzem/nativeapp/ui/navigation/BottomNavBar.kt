package com.watzem.nativeapp.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.BgMain
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextSecondary

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Explore : BottomNavItem(Screen.Explore.route, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore)
    data object Search : BottomNavItem(Screen.Search.createRoute(), "Search", Icons.Filled.Search, Icons.Outlined.Search)
    data object Watchlist : BottomNavItem(Screen.Watchlist.route, "Watchlist", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)
    data object Support : BottomNavItem("support", "Support", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
}

val watzemNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Explore,
    BottomNavItem.Search,
    BottomNavItem.Watchlist,
    BottomNavItem.Support
)

@Composable
fun WatzemBottomNav(
    currentRoute: String?,
    isTv: Boolean,
    watchlistCount: Int,
    isUpdateAvailable: Boolean = false,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor by animateColorAsState(
        targetValue = if (isTv) AccentRose else AccentAmber,
        label = "active_color"
    )

    val navBarShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(navBarShape)
            // Translucent background with no border
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xB30A0D18), // Translucent soft dark gradient
                        Color(0xE60A0D18),
                        Color(0xF50A0D18)
                    )
                )
            )
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            watzemNavItems.forEach { item ->
                val isSelected = if (item == BottomNavItem.Search) {
                    currentRoute?.startsWith("search") == true
                } else {
                    currentRoute == item.route
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onNavigate(item.route)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Icon with Badge
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = if (isSelected) activeColor else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )

                            // Watchlist counter badge
                            if (item == BottomNavItem.Watchlist && watchlistCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 10.dp, y = (-5).dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentRose)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (watchlistCount > 99) "99+" else watchlistCount.toString(),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Support update badge
                            if (item == BottomNavItem.Support && isUpdateAvailable) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(AccentAmber)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Label
                        Text(
                            text = item.title,
                            color = if (isSelected) activeColor else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )

                        // Active Dot
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(5.5.dp))
                        }
                    }
                }
            }
        }
    }
}
