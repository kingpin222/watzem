package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.watzem.nativeapp.data.model.StreamServer
import com.watzem.nativeapp.ui.theme.CinemaAmber
import com.watzem.nativeapp.ui.theme.TextPrimary
import com.watzem.nativeapp.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Compact, cinema-grade Server Selector dialog with full D-Pad navigation support.
 * Designed to fit seamlessly on Android TV (16:9) and landscape/portrait mobile devices.
 */
@Composable
fun ServerSelectorSheet(
    servers: List<StreamServer>,
    selectedServer: StreamServer,
    onServerSelected: (StreamServer) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lastWorkingServer = remember { StreamServer.getLastWorkingServer(context) }
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80L)
        try {
            initialFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Semi-transparent backdrop scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC050608))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Compact Cinema Dialog Card
            Column(
                modifier = Modifier
                    .widthIn(min = 340.dp, max = 500.dp)
                    .heightIn(max = 440.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click inside
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF141620),
                                Color(0xFF0C0D13)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x33F59E0B),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22F59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = CinemaAmber,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stream Servers",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x26F59E0B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${servers.size} MIRRORS",
                                    color = CinemaAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Switch mirror if current server buffers or experiences downtime",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.15f,
                                focusedBorderColor = CinemaAmber,
                                borderWidth = 2.dp,
                                onClick = onDismiss
                            )
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Server List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(servers, key = { it.id }) { server ->
                        val isSelected = server.id == selectedServer.id
                        val isLastWorking = server.id == lastWorkingServer.id
                        val isDefault = server.id == StreamServer.DEFAULT_SERVER.id
                        val itemFocusRequester = remember { FocusRequester() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isSelected) Modifier.focusRequester(initialFocusRequester) else Modifier
                                )
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.02f,
                                    focusedBorderColor = CinemaAmber,
                                    borderWidth = 2.dp,
                                    onClick = {
                                        onServerSelected(server)
                                        onDismiss()
                                    }
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0x28F59E0B) else Color(0x1F222F)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CinemaAmber.copy(alpha = 0.7f) else Color(0x18FFFFFF),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status / Selection Indicator Dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) CinemaAmber else Color(0x55FFFFFF))
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Server Info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = server.name,
                                        color = if (isSelected) CinemaAmber else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isSelected) CinemaAmber else Color(0x26F59E0B))
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = server.badge,
                                            color = if (isSelected) Color.Black else CinemaAmber,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (isLastWorking) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LAST WORKING",
                                            color = Color(0xFF10B981),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (isDefault) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "RECOMMENDED",
                                            color = Color(0xFF10B981),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = server.description,
                                    color = TextSecondary,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Active Checkmark
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = CinemaAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
