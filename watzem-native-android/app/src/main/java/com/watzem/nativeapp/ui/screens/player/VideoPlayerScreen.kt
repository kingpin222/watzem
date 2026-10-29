package com.watzem.nativeapp.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.watzem.nativeapp.data.cache.VideoCacheManager
import com.watzem.nativeapp.ui.components.tvFocusable
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    title: String,
    headers: Map<String, String> = emptyMap(),
    currentSeason: Int? = null,
    currentEpisode: Int? = null,
    onEpisodeChange: ((Int, Int) -> Unit)? = null,
    onOpenEpisodeSelector: (() -> Unit)? = null,
    serverName: String? = null,
    onOpenServerSelector: (() -> Unit)? = null,
    onBackClick: () -> Unit,
    onPlaybackError: ((Exception) -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_ZOOM) }
    var userInteractionTrigger by remember { mutableLongStateOf(0L) }

    val playPauseFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }

    val exoPlayer = remember(videoUrl, headers) {
        val mediaSourceFactory = DefaultMediaSourceFactory(
            VideoCacheManager.getCacheDataSourceFactory(context, headers)
        )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                try {
                    val mediaItem = MediaItem.Builder()
                        .setUri(videoUrl)
                        .apply {
                            when {
                                videoUrl.contains("m3u8", ignoreCase = true) ||
                                        videoUrl.contains("hls", ignoreCase = true) ||
                                        videoUrl.contains("playlist", ignoreCase = true) ||
                                        videoUrl.contains("type=/index.m3u8", ignoreCase = true) -> {
                                    setMimeType(MimeTypes.APPLICATION_M3U8)
                                }
                                videoUrl.contains("mpd", ignoreCase = true) ||
                                        videoUrl.contains("dash", ignoreCase = true) -> {
                                    setMimeType(MimeTypes.APPLICATION_MPD)
                                }
                                videoUrl.contains(".mp4", ignoreCase = true) -> {
                                    setMimeType(MimeTypes.APPLICATION_MP4)
                                }
                                videoUrl.contains(".mkv", ignoreCase = true) -> {
                                    setMimeType(MimeTypes.APPLICATION_MATROSKA)
                                }
                            }
                        }
                        .build()
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                } catch (e: Exception) {
                    onPlaybackError?.invoke(e)
                }
            }
    }

    // Lock to landscape during video playback on mobile, keep TV untouched, hide status & navigation bars
    DisposableEffect(Unit) {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!isTv) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlaybackError?.invoke(error)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            if (!isTv) {
                insetsController?.hide(WindowInsetsCompat.Type.systemBars())
                activity?.requestedOrientation = if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    originalOrientation
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    }

    // Track playback progress
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Auto-hide controls after 4.5s of inactivity
    LaunchedEffect(showControls, isPlaying, userInteractionTrigger) {
        if (showControls && isPlaying) {
            delay(4500L)
            try {
                rootFocusRequester.requestFocus()
            } catch (_: Exception) {}
            showControls = false
        }
    }

    // Focus management: request Play/Pause button focus when controls appear
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(60L)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            delay(60L)
            try {
                rootFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // First Back key press hides controls; second Back key exits player
    BackHandler {
        if (showControls) {
            try {
                rootFocusRequester.requestFocus()
            } catch (_: Exception) {}
            showControls = false
        } else {
            onBackClick()
        }
    }

    val animatedControlsAlpha by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        animationSpec = tween(250),
        label = "playerControlsAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

                if (!showControls) {
                    // Controls are currently hidden: any navigation key reveals controls and performs the action
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            showControls = true
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            showControls = true
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L))
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            showControls = true
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            showControls = true
                            true
                        }
                        else -> false
                    }
                } else {
                    // Controls are visible: reset hide timer and let child focusable buttons handle D-pad navigation
                    userInteractionTrigger += 1
                    false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    userInteractionTrigger += 1
                    if (showControls) {
                        try {
                            rootFocusRequester.requestFocus()
                        } catch (_: Exception) {}
                        showControls = false
                    } else {
                        showControls = true
                    }
                }
            }
    ) {
        // ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering indicator
        if (isBuffering) {
            CircularProgressIndicator(
                color = CinemaAmber,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }

        // Overlay Controls (Kept in composition to prevent Focus activeChild crashes, visibility & focusability animated)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(animatedControlsAlpha)
                .focusProperties { canFocus = showControls }
                .background(Color(0x77000000))
        ) {
                // Top App Bar inside Player
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.15f,
                                focusedBorderColor = CinemaAmber,
                                borderWidth = 2.dp,
                                onClick = onBackClick
                            )
                            .clip(CircleShape)
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Episode Prev / Next navigation for TV series
                    if (currentSeason != null && currentEpisode != null && onEpisodeChange != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x88000000))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .tvFocusable(
                                        enabled = currentEpisode > 1,
                                        shape = RoundedCornerShape(6.dp),
                                        focusedScale = 1.15f,
                                        focusedBorderColor = CinemaAmber,
                                        borderWidth = 2.dp,
                                        onClick = {
                                            userInteractionTrigger += 1
                                            if (currentEpisode > 1) onEpisodeChange(currentSeason, currentEpisode - 1)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                    contentDescription = "Previous Episode",
                                    tint = if (currentEpisode > 1) TextPrimary else Color(0x66FFFFFF)
                                )
                            }

                            // Center Episode Selector Pill (Clickable & D-Pad focusable)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(
                                        if (onOpenEpisodeSelector != null) {
                                            Modifier.tvFocusable(
                                                shape = RoundedCornerShape(6.dp),
                                                focusedScale = 1.08f,
                                                focusedBorderColor = CinemaAmber,
                                                borderWidth = 1.5.dp,
                                                onClick = {
                                                    userInteractionTrigger += 1
                                                    onOpenEpisodeSelector()
                                                }
                                            )
                                        } else Modifier
                                    )
                                    .background(if (onOpenEpisodeSelector != null) Color(0x33F59E0B) else Color.Transparent)
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "S$currentSeason • E$currentEpisode",
                                    color = CinemaAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (onOpenEpisodeSelector != null) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.UnfoldMore,
                                        contentDescription = "Episodes",
                                        tint = CinemaAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(6.dp),
                                        focusedScale = 1.15f,
                                        focusedBorderColor = CinemaAmber,
                                        borderWidth = 2.dp,
                                        onClick = {
                                            userInteractionTrigger += 1
                                            onEpisodeChange(currentSeason, currentEpisode + 1)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                    contentDescription = "Next Episode",
                                    tint = TextPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Server Switcher Pill
                    if (serverName != null && onOpenServerSelector != null) {
                        Row(
                            modifier = Modifier
                                .tvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedScale = 1.08f,
                                    focusedBorderColor = CinemaAmber,
                                    borderWidth = 1.5.dp,
                                    onClick = {
                                        userInteractionTrigger += 1
                                        onOpenServerSelector()
                                    }
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x88000000))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Server",
                                tint = CinemaAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = serverName,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Aspect Ratio Toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.15f,
                                focusedBorderColor = CinemaAmber,
                                borderWidth = 2.dp,
                                onClick = {
                                    userInteractionTrigger += 1
                                    resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    } else {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                }
                            )
                            .clip(CircleShape)
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) "Fit Screen" else "Zoom Screen",
                            tint = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) CinemaAmber else TextPrimary
                        )
                    }

                    // Picture-in-Picture
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.15f,
                                    focusedBorderColor = CinemaAmber,
                                    borderWidth = 2.dp,
                                    onClick = {
                                        userInteractionTrigger += 1
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        activity?.enterPictureInPictureMode(params)
                                    }
                                )
                                .clip(CircleShape)
                                .background(Color(0x88000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "PiP",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                // Center Play/Pause & Seek buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Rewind 10s
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.2f,
                                focusedBorderColor = CinemaAmber,
                                borderWidth = 2.dp,
                                onClick = {
                                    userInteractionTrigger += 1
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L))
                                }
                            )
                            .clip(CircleShape)
                            .background(Color(0x55000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play / Pause Primary Button (Initial D-pad Focus)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .focusRequester(playPauseFocusRequester)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.15f,
                                focusedBorderColor = Color.White,
                                borderWidth = 3.dp,
                                onClick = {
                                    userInteractionTrigger += 1
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            )
                            .clip(CircleShape)
                            .background(CinemaAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Forward 10s
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.2f,
                                focusedBorderColor = CinemaAmber,
                                borderWidth = 2.dp,
                                onClick = {
                                    userInteractionTrigger += 1
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                                }
                            )
                            .clip(CircleShape)
                            .background(Color(0x55000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Timeline Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    var isUserSeeking by remember { mutableStateOf(false) }
                    var seekPosition by remember { mutableFloatStateOf(0f) }

                    val sliderValue = if (isUserSeeking) {
                        seekPosition
                    } else if (totalDuration > 0L) {
                        (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { value ->
                            userInteractionTrigger += 1
                            isUserSeeking = true
                            seekPosition = value
                        },
                        onValueChangeFinished = {
                            userInteractionTrigger += 1
                            val newPositionMs = (seekPosition * totalDuration).toLong()
                            exoPlayer.seekTo(newPositionMs)
                            isUserSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = CinemaAmber,
                            activeTrackColor = CinemaAmber,
                            inactiveTrackColor = Color(0x55FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusable(
                                shape = RoundedCornerShape(8.dp),
                                focusedScale = 1.02f,
                                focusedBorderColor = CinemaAmber,
                                borderWidth = 2.dp
                            )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(if (isUserSeeking) (seekPosition * totalDuration).toLong() else currentPosition),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(totalDuration),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
