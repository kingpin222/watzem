package com.watzem.nativeapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.watzem.nativeapp.ui.theme.AccentAmber

/**
 * Reusable D-Pad focus modifier for TV elements (cards, buttons, tabs, links, chips, list items).
 * Provides cinematic scaling, zIndex elevation, and high-contrast glowing border on remote focus.
 */
@Composable
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedScale: Float = 1.06f,
    focusedBorderColor: Color = AccentAmber,
    borderWidth: Dp = 2.dp,
    onClick: (() -> Unit)? = null
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) focusedScale else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "tvFocusScale"
    )

    return this
        .scale(scale)
        .zIndex(if (isFocused && enabled) 10f else 0f)
        .then(
            if (isFocused && enabled) {
                Modifier.border(borderWidth, focusedBorderColor, shape)
            } else {
                Modifier
            }
        )
        .onFocusChanged { state ->
            isFocused = state.isFocused
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else if (enabled) {
                Modifier.focusable()
            } else {
                Modifier
            }
        )
}
