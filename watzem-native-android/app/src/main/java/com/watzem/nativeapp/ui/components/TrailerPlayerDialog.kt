package com.watzem.nativeapp.ui.components

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.watzem.nativeapp.data.model.VideoItem
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.BgCard
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.CinemaCard
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerPlayerDialog(
    trailer: VideoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val youtubeHtml = remember(trailer.key) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body {
              width: 100%;
              height: 100%;
              background-color: #000000;
              overflow: hidden;
            }
            iframe {
              position: absolute;
              top: 0;
              left: 0;
              width: 100%;
              height: 100%;
              border: 0;
            }
          </style>
        </head>
        <body>
          <iframe
            src="https://www.youtube-nocookie.com/embed/${trailer.key}?autoplay=1&rel=0&modestbranding=1&playsinline=1"
            referrerpolicy="strict-origin-when-cross-origin"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
            allowfullscreen>
          </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    fun openInYouTube() {
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${trailer.key}"))
            context.startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${trailer.key}"))
            context.startActivity(webIntent)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.loadUrl("about:blank")
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler {
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33FF0000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFFF0000),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = trailer.name.ifBlank { "Official Trailer" },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Open in YouTube Button
                        IconButton(
                            onClick = { openInYouTube() },
                            modifier = Modifier
                                .size(32.dp)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.15f,
                                    focusedBorderColor = AccentAmber,
                                    borderWidth = 2.dp,
                                    onClick = { openInYouTube() }
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in YouTube",
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedScale = 1.15f,
                                    focusedBorderColor = AccentAmber,
                                    borderWidth = 2.dp,
                                    onClick = onDismiss
                                )
                                .clip(CircleShape)
                                .background(CinemaCard)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Video Container 16:9
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                keepScreenOn = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadsImagesAutomatically = true
                                    allowContentAccess = true
                                    databaseEnabled = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                    }
                                }
                                webChromeClient = WebChromeClient()

                                loadDataWithBaseURL(
                                    "https://watzem.app",
                                    youtubeHtml,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AccentAmber,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
