package com.watzem.nativeapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.watzem.nativeapp.data.model.AppUpdateInfo
import com.watzem.nativeapp.data.update.AppUpdateManager
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.BgSurfaceElevated
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val EmeraldGreen = Color(0xFF10B981)

@Composable
fun AppUpdateModal(
    updateInfo: AppUpdateInfo,
    updateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isDownloading by updateManager.isDownloading.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()
    val downloadStatusText by updateManager.downloadStatusText.collectAsState()
    val downloadBytesText by updateManager.downloadBytesText.collectAsState()
    val errorMessage by updateManager.error.collectAsState()

    val isMandatory = updateInfo.mandatory

    val infiniteTransition = rememberInfiniteTransition(label = "modalPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "modalGlowPulse"
    )

    Dialog(
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        // Semi-transparent backdrop with click-to-dismiss for non-mandatory
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 430.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = Color.Black.copy(alpha = 0.85f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF221D14),
                                Color(0xFF15130E)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = AccentAmber.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(20.dp)
            ) {
                // Close 'X' button in top-right corner
                if (!isDownloading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, BorderSubtle, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Glowing App Icon
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(16.dp, CircleShape, spotColor = AccentAmber)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        AccentAmber.copy(alpha = pulseAlpha),
                                        Color(0xFF2B2104)
                                    )
                                )
                            )
                            .border(1.5.dp, AccentAmber.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFF1A1200),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = updateInfo.title ?: if (isMandatory) "WATZEM Update Required" else "Update Available",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Version Transition Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "v${updateManager.currentVersionName}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BgSurfaceElevated)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(12.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "v${updateInfo.displayVersion}",
                            color = Color(0xFF1A1300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentAmber)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )

                        if (isMandatory) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Required",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentRose)
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Specs Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        updateInfo.fileSize?.let { size ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(size, color = TextSecondary, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified APK", color = TextSecondary, fontSize = 11.sp)
                        }

                        updateInfo.releaseDate?.let { date ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(date, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Release Notes (Compact Container)
                    if (updateInfo.releaseNotes.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x3B000000))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "What's New:",
                                color = AccentAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            updateInfo.releaseNotes.take(4).forEach { note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 3.dp)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(AccentAmber.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(9.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = note,
                                        color = TextPrimary.copy(alpha = 0.92f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Progress or Action Buttons
                    if (isDownloading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = downloadStatusText.ifEmpty { "Downloading update..." },
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$downloadProgress%",
                                    color = AccentAmber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = AccentAmber,
                                trackColor = Color(0x33FFFFFF)
                            )

                            if (downloadBytesText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = downloadBytesText,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Action Buttons: Primary Install CTA + Remind Later
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tvFocusable(
                                        onClick = {
                                            coroutineScope.launch {
                                                updateManager.startUpdateDownload()
                                            }
                                        },
                                        focusedBorderColor = Color.White,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                AccentAmber,
                                                Color(0xFFF59E0B)
                                            )
                                        )
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            updateManager.startUpdateDownload()
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF1A1300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isMandatory) "Install Required Update" else "Update & Install Now",
                                    color = Color(0xFF1A1300),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (!isDownloading) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Remind Me Later",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .tvFocusable(
                                            onClick = onDismiss,
                                            focusedBorderColor = AccentAmber,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onDismiss() }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = err,
                            color = AccentRose,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Security Footer Note
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Signed APK • Seamless in-place upgrade",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

