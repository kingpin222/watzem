package com.watzem.nativeapp

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.watzem.nativeapp.data.local.WatchlistManager
import com.watzem.nativeapp.data.repository.MovieRepository
import com.watzem.nativeapp.data.update.AppUpdateManager
import com.watzem.nativeapp.ui.components.AppUpdateModal
import com.watzem.nativeapp.ui.components.SupportTipDialog
import com.watzem.nativeapp.ui.components.TvSidebar
import com.watzem.nativeapp.ui.components.WatzemTopHeader
import com.watzem.nativeapp.ui.navigation.Screen
import com.watzem.nativeapp.ui.navigation.WatzemBottomNav
import com.watzem.nativeapp.ui.navigation.WatzemNavGraph
import com.watzem.nativeapp.ui.theme.BgMain
import com.watzem.nativeapp.ui.theme.WatzemTheme

class MainActivity : ComponentActivity() {

    private var isPlayerScreenActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent the app and screen from sleeping / timing out while in use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Full screen edge-to-edge with transparent/translucent system bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Disable contrast enforcement so status and navigation bars are genuinely translucent without OS dimming scrims
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        applySystemBarsMode(isPlayer = false)

        // Safety handler: swallow Compose internal focus traversal glitches (ActiveParent with no focused child)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is IllegalArgumentException && throwable.message?.contains("ActiveParent with no focused child") == true) {
                android.util.Log.w("WatzemFocus", "Recovered from Compose focus traversal glitch: ${throwable.message}")
                return@setDefaultUncaughtExceptionHandler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            WatzemTheme {
                MainAppContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBarsMode(isPlayerScreenActive)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBarsMode(isPlayerScreenActive)
        }
    }

    fun applySystemBarsMode(isPlayer: Boolean) {
        isPlayerScreenActive = isPlayer
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        // Full screen app: Hide both status bar and navigation bar across the entire application
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    @Composable
    private fun MainAppContent() {
        val context = LocalContext.current
        val uiModeManager = remember { context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager }
        val isTvDevice = remember {
            uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val useTvLayout = isTvDevice || isLandscape

        val navController = rememberNavController()
        val repository = remember {
            val watchlistManager = WatchlistManager.getInstance(applicationContext)
            MovieRepository(watchlistManager = watchlistManager)
        }
        val updateManager = remember { AppUpdateManager.getInstance(applicationContext) }

        val watchlist by repository.watchlistFlow.collectAsState()
        val isUpdateAvailable by updateManager.isUpdateAvailable.collectAsState()
        val updateInfo by updateManager.updateInfo.collectAsState()

        var isTv by rememberSaveable { mutableStateOf(false) }
        var showTipDialog by remember { mutableStateOf(false) }
        var showUpdateModal by remember { mutableStateOf(false) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Auto-check for update on startup (like old watzem)
        LaunchedEffect(Unit) {
            val hasUpdate = updateManager.checkForUpdate()
            if (hasUpdate && !updateManager.isDismissedForSession) {
                showUpdateModal = true
            }
        }

        // Hide navigation chrome on player & details screens
        val isPlayerScreen = currentRoute?.startsWith("video_player") == true ||
                currentRoute?.startsWith("stream_player") == true
        val isDetailScreen = currentRoute?.startsWith("detail") == true

        // Fullscreen immersive mode: translucent status bar and hidden bottom navigation bar
        LaunchedEffect(isPlayerScreen) {
            applySystemBarsMode(isPlayer = isPlayerScreen)
        }

        if (useTvLayout) {
            // Android TV & Landscape Layout: TV Menu Sidebar on the left + D-Pad Spatial Navigation
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgMain)
            ) {
                if (!isPlayerScreen && !isDetailScreen) {
                    TvSidebar(
                        currentRoute = currentRoute,
                        isTv = isTv,
                        watchlistCount = watchlist.size,
                        isUpdateAvailable = isUpdateAvailable,
                        onModeChange = { newTvMode ->
                            isTv = newTvMode
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onUpdateClick = { showUpdateModal = true },
                        onTipClick = { showTipDialog = true }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    WatzemNavGraph(
                        navController = navController,
                        repository = repository,
                        updateManager = updateManager,
                        isTv = isTv,
                        innerPadding = PaddingValues(0.dp)
                    )
                }
            }
        } else {
            // Mobile Portrait Layout: TopHeader + BottomNav
            val showTopHeader = !isPlayerScreen && !isDetailScreen
            val showBottomBar = !isPlayerScreen && !isDetailScreen

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgMain),
                containerColor = BgMain,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (showTopHeader) {
                        WatzemTopHeader(
                            isTv = isTv,
                            onModeChange = { isTv = it },
                            onLogoClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        WatzemBottomNav(
                            currentRoute = currentRoute,
                            isTv = isTv,
                            watchlistCount = watchlist.size,
                            isUpdateAvailable = isUpdateAvailable,
                            onNavigate = { route ->
                                if (route == "support") {
                                    showTipDialog = true
                                } else {
                                    navController.navigate(route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                WatzemNavGraph(
                    navController = navController,
                    repository = repository,
                    updateManager = updateManager,
                    isTv = isTv,
                    innerPadding = if (isPlayerScreen) PaddingValues(0.dp) else innerPadding
                )
            }
        }

        if (showTipDialog) {
            SupportTipDialog(
                isTv = isTv,
                updateManager = updateManager,
                onDismiss = { showTipDialog = false }
            )
        }

        if (showUpdateModal && updateInfo != null) {
            AppUpdateModal(
                updateInfo = updateInfo!!,
                updateManager = updateManager,
                onDismiss = {
                    updateManager.dismissForSession()
                    showUpdateModal = false
                }
            )
        }
    }
}
