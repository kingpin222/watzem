package com.watzem.nativeapp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.data.update.AppUpdateManager
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.BgCard
import com.watzem.nativeapp.ui.theme.BgSurfaceElevated
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val EmeraldGreen = Color(0xFF10B981)

@Composable
fun SupportTipDialog(
    isTv: Boolean,
    updateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeColor = if (isTv) AccentRose else AccentAmber

    val currentVersion = updateManager.currentVersionName
    val updateInfo by updateManager.updateInfo.collectAsState()
    val isUpdateAvailable by updateManager.isUpdateAvailable.collectAsState()
    val isChecking by updateManager.isChecking.collectAsState()
    val isDownloading by updateManager.isDownloading.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()
    val downloadStatusText by updateManager.downloadStatusText.collectAsState()
    val downloadBytesText by updateManager.downloadBytesText.collectAsState()
    val errorMessage by updateManager.error.collectAsState()

    var checkResultMessage by remember { mutableStateOf<String?>(null) }

    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        containerColor = BgSurfaceElevated,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.widthIn(min = 320.dp, max = 500.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(activeColor.copy(alpha = 0.25f), activeColor.copy(alpha = 0.10f))
                            )
                        )
                        .border(1.dp, activeColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Support WATZEM",
                        tint = activeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Support WATZEM",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version $currentVersion",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WATZEM is an independent cinema & TV series streaming platform crafted for speed, zero intrusive ads, and cinematic quality.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // 1. UPDATE APP SECTION (Inside Support)
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isUpdateAvailable) {
                                Brush.verticalGradient(
                                    listOf(Color(0xFF261E08), BgCard)
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(BgCard, Color(0xFF141720))
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isUpdateAvailable) AccentAmber.copy(alpha = 0.7f) else BorderSubtle,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = if (isUpdateAvailable) AccentAmber else activeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Software Updates",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isUpdateAvailable) AccentAmber.copy(alpha = 0.2f)
                                        else Color(0x33FFFFFF)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isUpdateAvailable) "Update Ready" else "v$currentVersion",
                                    color = if (isUpdateAvailable) AccentAmber else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // A. Downloading State
                        if (isDownloading) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = downloadStatusText.ifBlank { "Downloading update..." },
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "$downloadProgress%",
                                        color = AccentAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (downloadProgress / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AccentAmber,
                                    trackColor = Color(0x33FFFFFF)
                                )
                                if (downloadBytesText.isNotBlank()) {
                                    Text(
                                        text = downloadBytesText,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                        // B. Update Available State
                        else if (isUpdateAvailable && updateInfo != null) {
                            val info = updateInfo!!
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "WATZEM v${info.displayVersion} is now available!",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (info.releaseNotes.isNotEmpty()) {
                                    Text(
                                        text = info.releaseNotes.joinToString("\n• ", prefix = "• "),
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 15.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            updateManager.startUpdateDownload()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tvFocusable(
                                            shape = RoundedCornerShape(10.dp),
                                            focusedScale = 1.04f,
                                            focusedBorderColor = Color.White,
                                            borderWidth = 2.dp,
                                            onClick = {
                                                coroutineScope.launch {
                                                    updateManager.startUpdateDownload()
                                                }
                                            }
                                        ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentAmber,
                                        contentColor = Color(0xFF1E1400)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Download & Install Update",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        // C. Up to date State
                        else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = checkResultMessage ?: "App is up to date with latest fixes.",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            checkResultMessage = null
                                            val hasUpdate = updateManager.checkForUpdate(force = true)
                                            if (!hasUpdate) {
                                                checkResultMessage = "You have the latest version (v$currentVersion)"
                                            }
                                        }
                                    },
                                    enabled = !isChecking,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tvFocusable(
                                            shape = RoundedCornerShape(10.dp),
                                            focusedScale = 1.04f,
                                            focusedBorderColor = Color.White,
                                            borderWidth = 2.dp,
                                            onClick = {
                                                coroutineScope.launch {
                                                    checkResultMessage = null
                                                    val hasUpdate = updateManager.checkForUpdate(force = true)
                                                    if (!hasUpdate) {
                                                        checkResultMessage = "You have the latest version (v$currentVersion)"
                                                    }
                                                }
                                            }
                                        ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isChecking) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = activeColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Checking...", fontSize = 11.sp, color = TextPrimary)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Check for Updates", fontSize = 11.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = AccentRose,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 2. COMMUNITY & TIP OPTIONS
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .tvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedScale = 1.03f,
                                    focusedBorderColor = AccentAmber,
                                    borderWidth = 1.5.dp,
                                    onClick = { openBrowser("https://ko-fi.com") }
                                )
                                .clickable { openBrowser("https://ko-fi.com") }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalCafe,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Buy us a Coffee (Ko-fi)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Support server and infrastructure costs",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .tvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedScale = 1.03f,
                                    focusedBorderColor = AccentRose,
                                    borderWidth = 1.5.dp,
                                    onClick = { openBrowser("https://github.com/kingpin222/watzem") }
                                )
                                .clickable { openBrowser("https://github.com/kingpin222/watzem") }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Star on GitHub & Share",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Leave a star and help spread WATZEM",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.tvFocusable(
                    shape = RoundedCornerShape(10.dp),
                    focusedScale = 1.08f,
                    focusedBorderColor = Color.White,
                    borderWidth = 2.dp,
                    onClick = onDismiss
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor,
                    contentColor = if (isTv) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    )
}
