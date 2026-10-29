package com.watzem.nativeapp.ui.screens.update

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.data.update.AppUpdateManager
import com.watzem.nativeapp.ui.components.tvFocusable
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.BgMain
import com.watzem.nativeapp.ui.theme.BgSurfaceElevated
import com.watzem.nativeapp.ui.theme.BorderSubtle
import com.watzem.nativeapp.ui.theme.TextMuted
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EmeraldGreen = Color(0xFF10B981)
private val EmeraldDark = Color(0xFF064E3B)

@Composable
fun UpdateScreen(
    updateManager: AppUpdateManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val updateInfo by updateManager.updateInfo.collectAsState()
    val isUpdateAvailable by updateManager.isUpdateAvailable.collectAsState()
    val isChecking by updateManager.isChecking.collectAsState()
    val isDownloading by updateManager.isDownloading.collectAsState()
    val downloadProgress by updateManager.downloadProgress.collectAsState()
    val downloadStatusText by updateManager.downloadStatusText.collectAsState()
    val downloadBytesText by updateManager.downloadBytesText.collectAsState()
    val errorMessage by updateManager.error.collectAsState()

    var checkSuccessMessage by remember { mutableStateOf<String?>(null) }
    val isMandatory = updateInfo?.mandatory == true && isUpdateAvailable

    BackHandler(enabled = !isMandatory) {
        onBack()
    }

    LaunchedEffect(Unit) {
        if (updateInfo == null) {
            updateManager.checkForUpdate()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val spinTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Sticky Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isMandatory) {
                    Row(
                        modifier = Modifier
                            .tvFocusable(
                                onClick = onBack,
                                focusedBorderColor = AccentAmber,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(BgSurfaceElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                            .clickable { onBack() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Back",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Status Badge Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isUpdateAvailable) AccentAmber.copy(alpha = 0.15f)
                            else EmeraldGreen.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isUpdateAvailable) AccentAmber.copy(alpha = 0.35f)
                            else EmeraldGreen.copy(alpha = 0.35f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isUpdateAvailable) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isUpdateAvailable) AccentAmber else EmeraldGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUpdateAvailable) "Update Available" else "Up to Date",
                        color = if (isUpdateAvailable) AccentAmber else EmeraldGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Unified Master Card
                Box(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.7f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E1A14),
                                    Color(0xFF14120E)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (isUpdateAvailable) AccentAmber.copy(alpha = 0.4f)
                            else EmeraldGreen.copy(alpha = 0.4f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(28.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Cluster: Icon with glowing ring
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = CircleShape,
                                    spotColor = if (isUpdateAvailable) AccentAmber else EmeraldGreen
                                )
                                .clip(CircleShape)
                                .background(
                                    if (isUpdateAvailable)
                                        Brush.radialGradient(listOf(AccentAmber.copy(alpha = pulseAlpha), Color(0xFF2E2405)))
                                    else
                                        Brush.radialGradient(listOf(EmeraldGreen.copy(alpha = pulseAlpha), EmeraldDark))
                                )
                                .border(
                                    2.dp,
                                    if (isUpdateAvailable) AccentAmber.copy(alpha = 0.7f) else EmeraldGreen.copy(alpha = 0.7f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isUpdateAvailable) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                                contentDescription = "Watzem Update",
                                tint = if (isUpdateAvailable) Color(0xFF1F1500) else Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Title
                        Text(
                            text = if (isUpdateAvailable) {
                                updateInfo?.title ?: "WATZEM Update Required"
                            } else {
                                "WATZEM is Up to Date"
                            },
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Version Transition Pill Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "v${updateManager.currentVersionName}",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BgSurfaceElevated)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )

                            if (isUpdateAvailable && updateInfo != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "v${updateInfo?.displayVersion}",
                                    color = Color(0xFF1E1700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentAmber)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )

                                if (isMandatory) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Required",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AccentRose)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Latest Official Build",
                                    color = EmeraldGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Specs Strip
                        if (isUpdateAvailable && updateInfo != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                updateInfo?.fileSize?.let { size ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(size, color = TextSecondary, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Verified APK", color = TextSecondary, fontSize = 12.sp)
                                }

                                updateInfo?.releaseDate?.let { date ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(date, color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Changelog Area
                            val releaseNotes = updateInfo?.releaseNotes ?: emptyList()
                            if (releaseNotes.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x2B000000))
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "What's New in v${updateInfo?.displayVersion}:",
                                        color = AccentAmber,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    releaseNotes.forEach { note ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(AccentAmber.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = AccentAmber,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = note,
                                                color = TextPrimary.copy(alpha = 0.9f),
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(22.dp))
                            }
                        }

                        // Active Downloading Progress Bar State
                        if (isDownloading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$downloadProgress%",
                                        color = AccentAmber,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    color = AccentAmber,
                                    trackColor = Color(0x33FFFFFF)
                                )

                                if (downloadBytesText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = downloadBytesText,
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else if (isUpdateAvailable) {
                            // Action Buttons when update available
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Install Now Action Button
                                Row(
                                    modifier = Modifier
                                        .tvFocusable(
                                            onClick = {
                                                coroutineScope.launch {
                                                    updateManager.startUpdateDownload()
                                                }
                                            },
                                            focusedBorderColor = Color.White,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clip(RoundedCornerShape(16.dp))
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
                                        .padding(horizontal = 24.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF1A1300),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isMandatory) "Install Required Update" else "Update & Install Now",
                                        color = Color(0xFF1A1300),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                if (!isMandatory) {
                                    Text(
                                        text = "Remind Me Later",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .tvFocusable(
                                                onClick = onBack,
                                                focusedBorderColor = AccentAmber,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .clickable { onBack() }
                                            .padding(horizontal = 18.dp, vertical = 14.dp)
                                    )
                                }
                            }
                        } else {
                            // When Up to Date
                            Text(
                                text = "You are running the latest version. All streaming scrapers, server mirrors, and security patches are operational.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Check For Updates Button
                            Row(
                                modifier = Modifier
                                    .tvFocusable(
                                        onClick = {
                                            if (!isChecking) {
                                                coroutineScope.launch {
                                                    checkSuccessMessage = null
                                                    val found = updateManager.checkForUpdate(force = true)
                                                    if (!found) {
                                                        checkSuccessMessage = "WATZEM is up to date!"
                                                        delay(4000L)
                                                        checkSuccessMessage = null
                                                    }
                                                }
                                            }
                                        },
                                        focusedBorderColor = AccentAmber,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BgSurfaceElevated)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                    .clickable(enabled = !isChecking) {
                                        coroutineScope.launch {
                                            checkSuccessMessage = null
                                            val found = updateManager.checkForUpdate(force = true)
                                            if (!found) {
                                                checkSuccessMessage = "WATZEM is up to date!"
                                                delay(4000L)
                                                checkSuccessMessage = null
                                            }
                                        }
                                    }
                                    .padding(horizontal = 22.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = AccentAmber,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isChecking) "Checking for Updates..." else "Check for Updates",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            checkSuccessMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(msg, color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        errorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Error: $err",
                                color = AccentRose,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Safe Security Tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Signed APK package • Seamless in-place upgrade",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

