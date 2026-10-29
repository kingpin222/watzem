package com.watzem.nativeapp.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.UnfoldMore
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.watzem.nativeapp.data.adblock.AdBlockerEngine
import com.watzem.nativeapp.data.local.WatchlistManager
import com.watzem.nativeapp.data.model.StreamServer
import com.watzem.nativeapp.data.model.TVEpisode
import com.watzem.nativeapp.data.repository.MovieRepository
import com.watzem.nativeapp.data.stream.ExtractedStream
import com.watzem.nativeapp.data.stream.StreamExtractor
import com.watzem.nativeapp.ui.components.SeriesSelectorDialog
import com.watzem.nativeapp.ui.components.ServerSelectorSheet
import com.watzem.nativeapp.ui.components.tvFocusable
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.CinemaCard
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebStreamPlayerScreen(
    mediaId: Int,
    isTv: Boolean,
    title: String,
    initialSeason: Int = 1,
    initialEpisode: Int = 1,
    backdropUrl: String? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val initialServer = remember { StreamServer.getLastWorkingServer(context) }
    var selectedServer by remember { mutableStateOf(initialServer) }
    var currentServerIndex by remember { mutableIntStateOf(StreamServer.ALL_SERVERS.indexOf(initialServer).coerceAtLeast(0)) }
    var serversTriedCount by remember { mutableIntStateOf(0) }
    var showServerSheet by remember { mutableStateOf(false) }
    var showEpisodeDialog by remember { mutableStateOf(false) }
    var currentSeason by remember { mutableIntStateOf(initialSeason) }
    var currentEpisode by remember { mutableIntStateOf(initialEpisode) }

    // TV Seasons & Episodes Cache
    val repository = remember {
        val watchlistManager = WatchlistManager.getInstance(context.applicationContext)
        MovieRepository(watchlistManager = watchlistManager)
    }
    var availableSeasons by remember { mutableStateOf<List<Int>>(listOf(initialSeason)) }
    var seasonEpisodes by remember { mutableStateOf<List<TVEpisode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }

    LaunchedEffect(mediaId, isTv) {
        if (isTv) {
            val detailsRes = repository.getMediaDetails(mediaId, isTv = true)
            detailsRes.onSuccess { details ->
                val seasons = details.safeSeasons.filter { it.seasonNumber > 0 }.map { it.seasonNumber }
                if (seasons.isNotEmpty()) {
                    availableSeasons = seasons
                } else if ((details.numberOfSeasons ?: 0) > 0) {
                    availableSeasons = (1..(details.numberOfSeasons ?: 1)).toList()
                }
            }
        }
    }

    LaunchedEffect(mediaId, currentSeason, isTv) {
        if (isTv) {
            isLoadingEpisodes = true
            val epRes = repository.getSeasonEpisodes(mediaId, currentSeason)
            epRes.onSuccess { eps ->
                if (eps.isNotEmpty()) {
                    seasonEpisodes = eps
                } else {
                    seasonEpisodes = (1..24).map { epNum ->
                        TVEpisode(
                            id = epNum,
                            episodeNumber = epNum,
                            name = "Episode $epNum",
                            overview = "",
                            stillPath = null,
                            voteAverage = 0.0,
                            seasonNumber = currentSeason
                        )
                    }
                }
                isLoadingEpisodes = false
            }.onFailure {
                if (seasonEpisodes.isEmpty()) {
                    seasonEpisodes = (1..24).map { epNum ->
                        TVEpisode(
                            id = epNum,
                            episodeNumber = epNum,
                            name = "Episode $epNum",
                            overview = "",
                            stillPath = null,
                            voteAverage = 0.0,
                            seasonNumber = currentSeason
                        )
                    }
                }
                isLoadingEpisodes = false
            }
        }
    }

    // Stream extraction state
    var extractedStream by remember { mutableStateOf<ExtractedStream?>(null) }
    var isExtracting by remember { mutableStateOf(true) }
    var allServersFailed by remember { mutableStateOf(false) }

    val loadingMessages = remember {
        listOf(
            "Fetching stream server...",
            "Connecting to secure mirror...",
            "Locating high-speed video source...",
            "Negotiating audio & subtitle tracks...",
            "Buffering direct 4K playback...",
            "Preparing cinema experience..."
        )
    }
    var messageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1800L)
            messageIndex = (messageIndex + 1) % loadingMessages.size
        }
    }

    val retryFocusRequester = remember { FocusRequester() }
    val changeServerFocusRequester = remember { FocusRequester() }

    LaunchedEffect(allServersFailed) {
        delay(150L)
        try {
            if (allServersFailed) {
                retryFocusRequester.requestFocus()
            } else {
                changeServerFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val currentStreamUrl = remember(selectedServer, mediaId, isTv, currentSeason, currentEpisode) {
        selectedServer.buildUrl(mediaId, isTv, currentSeason, currentEpisode)
    }

    fun tryNextServer() {
        serversTriedCount++
        if (serversTriedCount < StreamServer.ALL_SERVERS.size) {
            val nextIdx = (currentServerIndex + 1) % StreamServer.ALL_SERVERS.size
            currentServerIndex = nextIdx
            selectedServer = StreamServer.ALL_SERVERS[nextIdx]
            extractedStream = null
            isExtracting = true
            allServersFailed = false
        } else {
            isExtracting = false
            allServersFailed = true
        }
    }

    // Persist last working server as soon as stream extraction succeeds
    LaunchedEffect(extractedStream) {
        if (extractedStream != null) {
            serversTriedCount = 0
            StreamServer.saveLastWorkingServer(context, selectedServer)
        }
    }

    // Auto-timeout per server: allow 15 seconds for each server to resolve manifests and media
    LaunchedEffect(currentStreamUrl) {
        extractedStream = null
        isExtracting = true
        allServersFailed = false
        delay(15000L)
        if (extractedStream == null) {
            tryNextServer()
        }
    }

    // Lock to landscape during movie/tv playback on mobile, keep TV untouched, hide status & nav bars
    DisposableEffect(Unit) {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTvDevice = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!isTvDevice) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            webViewInstance?.destroy()
            if (!isTvDevice) {
                insetsController?.hide(WindowInsetsCompat.Type.systemBars())
                activity?.requestedOrientation = if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    originalOrientation
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    }

    BackHandler {
        onBackClick()
    }

    val displayTitle = remember(title, isTv, currentSeason, currentEpisode) {
        if (isTv) "$title - S${currentSeason}E${currentEpisode}" else title
    }

    // If stream is extracted, render 100% Native ExoPlayer!
    if (extractedStream != null) {
        VideoPlayerScreen(
            videoUrl = extractedStream!!.url,
            title = displayTitle,
            headers = extractedStream!!.headers,
            currentSeason = if (isTv) currentSeason else null,
            currentEpisode = if (isTv) currentEpisode else null,
            onEpisodeChange = { s, e ->
                currentSeason = s
                currentEpisode = e
                extractedStream = null
                isExtracting = true
                allServersFailed = false
            },
            onOpenEpisodeSelector = if (isTv) { { showEpisodeDialog = true } } else null,
            serverName = selectedServer.shortName,
            onOpenServerSelector = { showServerSheet = true },
            onBackClick = onBackClick,
            onPlaybackError = {
                tryNextServer()
            }
        )

        if (showServerSheet) {
            ServerSelectorSheet(
                servers = StreamServer.ALL_SERVERS,
                selectedServer = selectedServer,
                onServerSelected = { server ->
                    selectedServer = server
                    currentServerIndex = StreamServer.ALL_SERVERS.indexOf(server).coerceAtLeast(0)
                    serversTriedCount = 0
                    StreamServer.saveLastWorkingServer(context, server)
                    extractedStream = null
                    isExtracting = true
                    allServersFailed = false
                    showServerSheet = false
                },
                onDismiss = { showServerSheet = false }
            )
        }

        if (showEpisodeDialog && isTv) {
            SeriesSelectorDialog(
                title = title,
                currentSeason = currentSeason,
                currentEpisode = currentEpisode,
                availableSeasons = availableSeasons,
                episodes = seasonEpisodes,
                isLoadingEpisodes = isLoadingEpisodes,
                onSeasonSelected = { newSeason ->
                    currentSeason = newSeason
                },
                onEpisodeSelected = { newSeason, newEpisode ->
                    currentSeason = newSeason
                    currentEpisode = newEpisode
                    extractedStream = null
                    isExtracting = true
                    allServersFailed = false
                    showEpisodeDialog = false
                },
                onDismiss = { showEpisodeDialog = false }
            )
        }
        return
    }

    // While resolving stream: background headless WebView runs sniffer, UI displays full backdrop + loading indicator + alternating text
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0B0E))
    ) {
        // 1. Movie Backdrop Image in the background
        if (!backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backdropUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Cinematic Gradient Overlays (darkened so text and spinner pop with pristine contrast)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x990A0B0E),
                            Color(0xFA0A0B0E)
                        ),
                        radius = 1200f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0x66000000),
                            Color(0xEE0A0B0E)
                        )
                    )
                )
        )

        // Invisible Headless WebView (pure background extractor)
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    isFocusable = false
                    isFocusableInTouchMode = false
                    clearFocus()
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowContentAccess = true
                        allowFileAccess = true
                        loadsImagesAutomatically = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        try {
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(this, emptySet())
                            }
                        } catch (_: Exception) {}
                        userAgentString = StreamExtractor.USER_AGENT
                    }

                    android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    }

                    // Register bridge for JS stream notifications
                    addJavascriptInterface(
                        StreamExtractor.StreamBridge(
                            onStreamFound = { stream ->
                                activity?.runOnUiThread {
                                    if (extractedStream == null) {
                                        extractedStream = stream
                                        isExtracting = false
                                        allServersFailed = false
                                    }
                                }
                            }
                        ),
                        "WatzemStreamBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            view?.evaluateJavascript(AdBlockerEngine.INJECTED_ADBLOCK_JS, null)
                            view?.evaluateJavascript(StreamExtractor.INJECTED_SNIFFER_JS, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(AdBlockerEngine.INJECTED_ADBLOCK_JS, null)
                            view?.evaluateJavascript(StreamExtractor.INJECTED_SNIFFER_JS, null)
                        }

                        override fun onLoadResource(view: WebView?, url: String?) {
                            super.onLoadResource(view, url)
                            view?.evaluateJavascript(AdBlockerEngine.INJECTED_ADBLOCK_JS, null)
                            view?.evaluateJavascript(StreamExtractor.INJECTED_SNIFFER_JS, null)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url ?: return false
                            val scheme = url.scheme?.lowercase() ?: ""
                            if (scheme != "http" && scheme != "https") return true
                            if (AdBlockerEngine.isAdUrl(url.toString())) return true
                            return false
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url?.toString() ?: ""
                            if (AdBlockerEngine.isAdUrl(url)) {
                                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                            }

                            // Sniff video streams at native network layer
                            if (StreamExtractor.isVideoStreamUrl(url)) {
                                val unwrappedUrl = StreamExtractor.unwrapProxyUrl(url)
                                val reqHeaders = request?.requestHeaders ?: emptyMap()
                                val headers = StreamExtractor.buildPlaybackHeaders(currentStreamUrl, reqHeaders)
                                activity?.runOnUiThread {
                                    if (extractedStream == null) {
                                        extractedStream = ExtractedStream(
                                            url = unwrappedUrl,
                                            headers = headers,
                                            source = "network_interceptor"
                                        )
                                        isExtracting = false
                                        allServersFailed = false
                                    }
                                }
                            }

                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    tag = currentStreamUrl
                    loadUrl(currentStreamUrl)
                    webViewInstance = this
                }
            },
            update = { wv ->
                if (wv.tag != currentStreamUrl) {
                    wv.tag = currentStreamUrl
                    wv.loadUrl(currentStreamUrl)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.01f)
                .focusProperties { canFocus = false }
                .focusable(false)
        )

        // Centered Cinema Loading & Failover Overlay (No Boxed Modal)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (allServersFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Failed",
                        tint = CinemaAmber,
                        modifier = Modifier.size(52.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Stream Unavailable",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Unable to resolve playback stream from tested servers.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Retry from server 1
                        Box(
                            modifier = Modifier
                                .focusRequester(retryFocusRequester)
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.1f,
                                    focusedBorderColor = Color.White,
                                    borderWidth = 3.dp,
                                    onClick = {
                                        serversTriedCount = 0
                                        currentServerIndex = 0
                                        selectedServer = StreamServer.ALL_SERVERS[0]
                                        extractedStream = null
                                        isExtracting = true
                                        allServersFailed = false
                                    }
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(CinemaAmber)
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Retry",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Pick Server
                        Box(
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.1f,
                                    focusedBorderColor = CinemaAmber,
                                    borderWidth = 3.dp,
                                    onClick = { showServerSheet = true }
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select Server",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Go Back
                        Box(
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.1f,
                                    focusedBorderColor = CinemaAmber,
                                    borderWidth = 3.dp,
                                    onClick = onBackClick
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Back",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    // Movie Title
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isTv) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedScale = 1.08f,
                                    focusedBorderColor = CinemaAmber,
                                    borderWidth = 1.5.dp,
                                    onClick = { showEpisodeDialog = true }
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33F59E0B))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Season $currentSeason • Episode $currentEpisode",
                                    color = CinemaAmber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.UnfoldMore,
                                    contentDescription = "Change Episode",
                                    tint = CinemaAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Modern Glowing Cinema Spinner
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CinemaAmber.copy(alpha = 0.2f),
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )
                        CircularProgressIndicator(
                            color = CinemaAmber,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Interactive alternating text with slide & fade transitions
                    AnimatedContent(
                        targetState = loadingMessages[messageIndex],
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { it / 2 }) togetherWith
                            (fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -it / 2 })
                        },
                        label = "loadingTextTransition"
                    ) { messageText ->
                        Text(
                            text = messageText,
                            color = CinemaAmber,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Trying ${selectedServer.name} (${currentServerIndex + 1}/${StreamServer.ALL_SERVERS.size})...",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Minimal bottom controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Change Server button
                        Box(
                            modifier = Modifier
                                .focusRequester(changeServerFocusRequester)
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.08f,
                                    focusedBorderColor = CinemaAmber,
                                    borderWidth = 2.dp,
                                    onClick = { showServerSheet = true }
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x44000000))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Dns, contentDescription = null, tint = CinemaAmber, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Change Server",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Change Episode button (TV series only)
                        if (isTv) {
                            Box(
                                modifier = Modifier
                                    .tvFocusable(
                                        shape = RoundedCornerShape(10.dp),
                                        focusedScale = 1.08f,
                                        focusedBorderColor = CinemaAmber,
                                        borderWidth = 2.dp,
                                        onClick = { showEpisodeDialog = true }
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x44000000))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = CinemaAmber, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Change Episode",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Cancel button
                        Box(
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.08f,
                                    focusedBorderColor = CinemaAmber,
                                    borderWidth = 2.dp,
                                    onClick = onBackClick
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33000000))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Compact Server Selector Dialog
        if (showServerSheet) {
            ServerSelectorSheet(
                servers = StreamServer.ALL_SERVERS,
                selectedServer = selectedServer,
                onServerSelected = { server ->
                    selectedServer = server
                    currentServerIndex = StreamServer.ALL_SERVERS.indexOf(server).coerceAtLeast(0)
                    serversTriedCount = 0
                    StreamServer.saveLastWorkingServer(context, server)
                    extractedStream = null
                    isExtracting = true
                    allServersFailed = false
                    showServerSheet = false
                },
                onDismiss = { showServerSheet = false }
            )
        }

        // Compact Series / Episode Selector Dialog
        if (showEpisodeDialog && isTv) {
            SeriesSelectorDialog(
                title = title,
                currentSeason = currentSeason,
                currentEpisode = currentEpisode,
                availableSeasons = availableSeasons,
                episodes = seasonEpisodes,
                isLoadingEpisodes = isLoadingEpisodes,
                onSeasonSelected = { newSeason ->
                    currentSeason = newSeason
                },
                onEpisodeSelected = { newSeason, newEpisode ->
                    currentSeason = newSeason
                    currentEpisode = newEpisode
                    extractedStream = null
                    isExtracting = true
                    allServersFailed = false
                    showEpisodeDialog = false
                },
                onDismiss = { showEpisodeDialog = false }
            )
        }
    }
}
