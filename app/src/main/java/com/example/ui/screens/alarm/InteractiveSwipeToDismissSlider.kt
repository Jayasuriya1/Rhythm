package com.example.ui.screens.alarm

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun InteractiveSwipeToDismissSlider(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val trackHeight = 68.dp
    val thumbSize = 56.dp
    val trackPadding = 6.dp

    val trackPaddingPx = with(density) { trackPadding.toPx() }
    val thumbSizePx = with(density) { thumbSize.toPx() }

    val offsetX = remember { Animatable(0f) }
    var hasDismissed by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .height(trackHeight)
            .testTag("alarm_swipe_slider")
            .semantics {
                role = Role.Button
                customActions = listOf(
                    CustomAccessibilityAction("Slide to Dismiss Alarm") {
                        if (!hasDismissed) {
                            hasDismissed = true
                            onDismiss()
                        }
                        true
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val maxDragDistance = (totalWidthPx - thumbSizePx - (trackPaddingPx * 2)).coerceAtLeast(1f)
        val progress = (offsetX.value / maxDragDistance).coerceIn(0f, 1f)

        // 1. Background Track Pill
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(36.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(36.dp)
                )
        ) {
            // Filled progress glow as user drags
            val progressWidth = (trackPaddingPx + offsetX.value + thumbSizePx / 2f).coerceIn(0f, totalWidthPx)
            val progressWidthDp = with(density) { progressWidth.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(progressWidthDp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
            )

            // Centered Hint Text (Fades as slider advances)
            val textAlpha = (1f - progress * 1.6f).coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Slide to Dismiss",
                    color = Color.White.copy(alpha = textAlpha * 0.92f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha * 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. Draggable Thumb Handle
        val thumbOffsetInt = (trackPaddingPx + offsetX.value).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffsetInt, y = 0) }
                .padding(vertical = trackPadding)
                .size(thumbSize)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    if (progress >= 0.80f) {
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                )
                .pointerInput(maxDragDistance) {
                    detectHorizontalDragGestures(
                        onDragStart = { /* touch down */ },
                        onDragEnd = {
                            if (!hasDismissed) {
                                if (offsetX.value >= maxDragDistance * 0.80f) {
                                    // Successfully completed gesture
                                    hasDismissed = true
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            targetValue = maxDragDistance,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        onDismiss()
                                    }
                                } else {
                                    // Released early: spring back to starting anchor
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            if (!hasDismissed) {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!hasDismissed) {
                                change.consume()
                                val nextValue = (offsetX.value + dragAmount).coerceIn(0f, maxDragDistance)
                                coroutineScope.launch {
                                    offsetX.snapTo(nextValue)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (progress >= 0.80f) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Dismissed",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Swipe Handle",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
