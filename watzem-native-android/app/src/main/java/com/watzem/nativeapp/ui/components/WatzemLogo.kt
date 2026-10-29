package com.watzem.nativeapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watzem.nativeapp.ui.theme.AccentAmber
import com.watzem.nativeapp.ui.theme.AccentAmberDark
import com.watzem.nativeapp.ui.theme.AccentAmberLight
import com.watzem.nativeapp.ui.theme.AccentRose
import com.watzem.nativeapp.ui.theme.AccentRoseDark
import com.watzem.nativeapp.ui.theme.AccentRoseLight
import com.watzem.nativeapp.ui.theme.TextMuted

@Composable
fun WatzemLogo(
    isTv: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo Geometric Prism Mark
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(30.dp)) {
                val w = size.width
                val h = size.height

                // Base Rounded Plate with Gradient
                val plateBrush = if (isTv) {
                    Brush.linearGradient(
                        colors = listOf(AccentRose, AccentRoseDark),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(AccentAmber, AccentAmberDark),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                }

                drawRoundRect(
                    brush = plateBrush,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                // Glass Corner Sheen
                drawRoundRect(
                    color = Color(0x55FFFFFF),
                    size = Size(w - 2f, h - 2f),
                    topLeft = Offset(1f, 1f),
                    cornerRadius = CornerRadius(7.5.dp.toPx(), 7.5.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Scale factor for 48x48 base coordinates
                val s = w / 48f

                // Left Wing of 'W'
                val wPath = Path().apply {
                    moveTo(12f * s, 16f * s)
                    lineTo(16.5f * s, 30f * s)
                    lineTo(20f * s, 19f * s)
                    lineTo(23f * s, 23f * s)
                    lineTo(21.5f * s, 28f * s)
                }
                drawPath(
                    path = wPath,
                    color = Color.White,
                    style = Stroke(
                        width = 2.8f * s,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Right Play Chevron
                val playPath = Path().apply {
                    moveTo(25f * s, 18f * s)
                    lineTo(35f * s, 24f * s)
                    lineTo(25f * s, 30f * s)
                    close()
                }
                drawPath(
                    path = playPath,
                    color = Color.White
                )

                // Radiant Sparkle Circle
                drawCircle(
                    color = Color.White,
                    radius = 1.8f * s,
                    center = Offset(34f * s, 14f * s)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Typography: WATZ + EM
        val emBrush = if (isTv) {
            Brush.linearGradient(listOf(AccentRoseLight, AccentRose, AccentRoseDark))
        } else {
            Brush.linearGradient(listOf(AccentAmberLight, AccentAmber, AccentAmberDark))
        }

        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.5).sp
                        )
                    ) {
                        append("WATZ")
                    }
                    withStyle(
                        SpanStyle(
                            brush = emBrush,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.5).sp
                        )
                    ) {
                        append("EM")
                    }
                },
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
