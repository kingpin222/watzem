package com.watzem.nativeapp.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.watzem.nativeapp.data.model.DirectStreamItem
import com.watzem.nativeapp.data.repository.MovieRepository
import com.watzem.nativeapp.data.update.AppUpdateManager
import com.watzem.nativeapp.ui.screens.detail.DetailScreen
import com.watzem.nativeapp.ui.screens.detail.DetailViewModel
import com.watzem.nativeapp.ui.screens.explore.ExploreScreen
import com.watzem.nativeapp.ui.screens.explore.ExploreViewModel
import com.watzem.nativeapp.ui.screens.home.HomeScreen
import com.watzem.nativeapp.ui.screens.home.HomeViewModel
import com.watzem.nativeapp.ui.screens.player.VideoPlayerScreen
import com.watzem.nativeapp.ui.screens.player.WebStreamPlayerScreen
import com.watzem.nativeapp.ui.screens.search.SearchScreen
import com.watzem.nativeapp.ui.screens.search.SearchViewModel
import com.watzem.nativeapp.ui.screens.update.UpdateScreen
import com.watzem.nativeapp.ui.screens.watchlist.WatchlistScreen
import com.watzem.nativeapp.ui.screens.watchlist.WatchlistViewModel

@Composable
fun WatzemNavGraph(
    navController: NavHostController,
    repository: MovieRepository,
    updateManager: AppUpdateManager,
    isTv: Boolean,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier.padding(innerPadding)
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            val homeVm: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(repository) as T
                    }
                }
            )

            HomeScreen(
                viewModel = homeVm,
                isTv = isTv,
                onMediaClick = { item ->
                    val itemIsTv = item.rawMediaType == "tv" || isTv
                    navController.navigate(Screen.Detail.createRoute(item.id, itemIsTv))
                },
                onWatchClick = { item ->
                    val itemIsTv = item.rawMediaType == "tv" || isTv
                    navController.navigate(
                        Screen.WebStreamPlayer.createRoute(
                            item.id,
                            itemIsTv,
                            item.displayTitle,
                            backdropUrl = item.backdropUrl ?: item.posterUrl
                        )
                    )
                },
                onSeeAllExplore = { navController.navigate(Screen.Explore.route) }
            )
        }

        // Explore Screen
        composable(Screen.Explore.route) {
            val exploreVm: ExploreViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ExploreViewModel(repository) as T
                    }
                }
            )

            ExploreScreen(
                viewModel = exploreVm,
                isTv = isTv,
                onMediaClick = { item ->
                    val itemIsTv = item.rawMediaType == "tv" || isTv
                    navController.navigate(Screen.Detail.createRoute(item.id, itemIsTv))
                }
            )
        }

        // Search Screen
        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("actorId") {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument("actorName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("actorProfile") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val initialQuery = Uri.decode(backStackEntry.arguments?.getString("query") ?: "")
            val actorId = backStackEntry.arguments?.getInt("actorId") ?: 0
            val actorName = Uri.decode(backStackEntry.arguments?.getString("actorName") ?: "")
            val actorProfile = Uri.decode(backStackEntry.arguments?.getString("actorProfile") ?: "")

            val searchVm: SearchViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SearchViewModel(repository) as T
                    }
                }
            )

            androidx.compose.runtime.LaunchedEffect(initialQuery, actorId, actorName) {
                if (actorId > 0 && actorName.isNotBlank()) {
                    searchVm.searchActorDirectly(
                        actorId = actorId,
                        actorName = actorName,
                        profilePath = actorProfile.takeIf { it.isNotBlank() }
                    )
                } else if (initialQuery.isNotBlank() && initialQuery != "{query}") {
                    searchVm.submitSearch(initialQuery)
                }
            }

            SearchScreen(
                viewModel = searchVm,
                onMediaClick = { item ->
                    val itemIsTv = item.rawMediaType == "tv"
                    navController.navigate(Screen.Detail.createRoute(item.id, itemIsTv))
                }
            )
        }

        // Watchlist Screen
        composable(Screen.Watchlist.route) {
            val watchlistVm: WatchlistViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return WatchlistViewModel(repository) as T
                    }
                }
            )

            WatchlistScreen(
                viewModel = watchlistVm,
                onItemClick = { id, tv ->
                    navController.navigate(Screen.Detail.createRoute(id, tv))
                },
                onExploreClick = {
                    navController.navigate(Screen.Explore.route)
                }
            )
        }

        // Detail Screen
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.IntType },
                navArgument("isTv") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: 0
            val detailIsTv = backStackEntry.arguments?.getBoolean("isTv") ?: isTv

            val detailVm: DetailViewModel = viewModel(
                key = "detail_${mediaId}_$detailIsTv",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return DetailViewModel(repository, mediaId, detailIsTv) as T
                    }
                }
            )

            DetailScreen(
                viewModel = detailVm,
                onBackClick = { navController.popBackStack() },
                onWatchClick = { id, tv, title, backdrop ->
                    navController.navigate(
                        Screen.WebStreamPlayer.createRoute(id, tv, title, backdropUrl = backdrop)
                    )
                },
                onWatchEpisodeClick = { id, tv, title, season, episode, backdrop ->
                    navController.navigate(
                        Screen.WebStreamPlayer.createRoute(id, tv, title, season, episode, backdropUrl = backdrop)
                    )
                },
                onTrailerClick = { _ ->
                    // Handled in-place by DetailScreen TrailerPlayerDialog
                },
                onSimilarMediaClick = { item ->
                    val simIsTv = item.rawMediaType == "tv" || detailIsTv
                    navController.navigate(Screen.Detail.createRoute(item.id, simIsTv))
                },
                onSearchKeyword = { keyword ->
                    navController.navigate(Screen.Search.createRoute(query = keyword))
                },
                onArtistClick = { personId, name, profilePath ->
                    navController.navigate(
                        Screen.Search.createRoute(
                            query = name,
                            actorId = personId,
                            actorName = name,
                            actorProfile = profilePath
                        )
                    )
                }
            )
        }

        // Video Player Screen
        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: "Video Player"
            val url = Uri.decode(encodedUrl)
            val title = Uri.decode(encodedTitle)

            VideoPlayerScreen(
                videoUrl = url,
                title = title,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Stream Player Screen
        composable(
            route = Screen.WebStreamPlayer.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.IntType },
                navArgument("isTv") { type = NavType.BoolType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = "Streaming Player"
                },
                navArgument("season") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("episode") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("backdrop") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: 0
            val streamIsTv = backStackEntry.arguments?.getBoolean("isTv") ?: isTv
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: "Streaming Player"
            val title = Uri.decode(encodedTitle)
            val season = backStackEntry.arguments?.getInt("season") ?: 1
            val episode = backStackEntry.arguments?.getInt("episode") ?: 1
            val encodedBackdrop = backStackEntry.arguments?.getString("backdrop") ?: ""
            val backdropUrl = Uri.decode(encodedBackdrop).ifBlank { null }

            WebStreamPlayerScreen(
                mediaId = mediaId,
                isTv = streamIsTv,
                title = title,
                initialSeason = season,
                initialEpisode = episode,
                backdropUrl = backdropUrl,
                onBackClick = { navController.popBackStack() }
            )
        }

        // App Update Screen
        composable(Screen.Update.route) {
            UpdateScreen(
                updateManager = updateManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
