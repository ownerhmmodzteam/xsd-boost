package com.example.core.system.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderActive
import com.example.ui.theme.PrimaryWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class EdgeSide(val label: String, val storageValue: String) {
    LEFT("Left Edge", "LEFT"),
    RIGHT("Right Edge", "RIGHT");

    companion object {
        fun fromString(raw: String?): EdgeSide {
            return if (raw.equals("LEFT", ignoreCase = true)) LEFT else RIGHT
        }
    }
}

/**
 * Minimalist Black & White Gradient vertical floating handle pinned to either LEFT or RIGHT edge.
 * Built directly in Jetpack Compose with pointerInput gesture handling:
 * - Long press ([longPressDurationMs], default 420ms) triggers [onLongPress] to display the Game Booster panel.
 * - Vertical drag repositions the handle via [onDragDeltaY] and saves position on [onDragEnd].
 * - Single tap and double tap are intentionally ignored (NO-OP) to avoid accidental game interruption.
 * - Supports customizable [handleSizeDp] (56..120dp), [handleOpacity] (0.25..1.0), and [isHiddenMode].
 */
@Composable
fun FloatingEdgeHandle(
    side: EdgeSide = EdgeSide.RIGHT,
    handleSizeDp: Float = 76f,
    handleOpacity: Float = 0.90f,
    longPressDurationMs: Long = 420L,
    isHiddenMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onDragDeltaY: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnDragDeltaY by rememberUpdatedState(onDragDeltaY)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current
    val gestureScope = rememberCoroutineScope()

    var isInteracting by remember { mutableStateOf(false) }

    val clampedHeight = handleSizeDp.coerceIn(56f, 120f)
    val clampedOpacity = handleOpacity.coerceIn(0.25f, 1.0f)
    val clampedLongPress = longPressDurationMs.coerceIn(250L, 800L)

    val activeBorderColor by animateColorAsState(
        targetValue = if (isInteracting) Color.White else Color.White.copy(alpha = 0.55f),
        animationSpec = tween(140),
        label = "handle_border"
    )
    val normalPillWidth by animateDpAsState(
        targetValue = if (isInteracting) 11.dp else 9.dp,
        animationSpec = tween(140),
        label = "handle_width"
    )
    val hiddenPillWidth by animateDpAsState(
        targetValue = if (isInteracting) 5.dp else 3.dp,
        animationSpec = tween(140),
        label = "hidden_handle_width"
    )

    val isLeft = side == EdgeSide.LEFT

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = if (isLeft) {
                    "XSD BOOST Left Edge Floating Handle"
                } else {
                    "XSD BOOST Right Edge Floating Handle"
                }
            }
            .testTag(if (isLeft) "left_edge_floating_handle" else "right_edge_floating_handle")
            .pointerInput(side, clampedLongPress) {
                val touchSlop = viewConfig.touchSlop
                val locationOnScreen = IntArray(2)

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isInteracting = true

                    view.getLocationOnScreen(locationOnScreen)
                    val downScreenX = down.position.x + locationOnScreen[0]
                    val downScreenY = down.position.y + locationOnScreen[1]
                    var prevScreenY = downScreenY
                    var hasMovedBeyondSlop = false
                    var isDragging = false
                    var longPressFired = false
                    var gestureFinished = false

                    val longPressJob = gestureScope.launch {
                        delay(clampedLongPress)
                        if (!hasMovedBeyondSlop && !gestureFinished) {
                            longPressFired = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnLongPress()
                        }
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val activeChange = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break

                            if (!activeChange.pressed) {
                                activeChange.consume()
                                break
                            }

                            view.getLocationOnScreen(locationOnScreen)
                            val currentScreenX = activeChange.position.x + locationOnScreen[0]
                            val currentScreenY = activeChange.position.y + locationOnScreen[1]

                            val totalDx = currentScreenX - downScreenX
                            val totalDy = currentScreenY - downScreenY

                            if (!hasMovedBeyondSlop && !longPressFired &&
                                (abs(totalDx) > touchSlop || abs(totalDy) > touchSlop)
                            ) {
                                hasMovedBeyondSlop = true
                                longPressJob.cancel()
                                isDragging = true
                            }

                            if (isDragging && !longPressFired) {
                                val deltaY = currentScreenY - prevScreenY
                                if (deltaY != 0f) {
                                    currentOnDragDeltaY(deltaY)
                                }
                                prevScreenY = currentScreenY
                                activeChange.consume()
                            }
                        }
                    } finally {
                        gestureFinished = true
                        longPressJob.cancel()
                        isInteracting = false
                        if (isDragging && !longPressFired) {
                            currentOnDragEnd()
                        }
                    }
                }
            },
        contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Image(
            painter = painterResource(id = R.drawable.xsd_boost_overlay),
            contentDescription = "XSD BOOST overlay icon",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(horizontal = if (isLeft) 2.dp else 0.dp)
                .size(if (isHiddenMode) 42.dp else 68.dp)
                .alpha((clampedOpacity * if (isHiddenMode) 0.58f else 1f).coerceIn(0.15f, 1f))
        )
    }
}

@Composable
fun RightEdgeFloatingHandle(
    isHiddenMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onDragDeltaY: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    FloatingEdgeHandle(
        side = EdgeSide.RIGHT,
        isHiddenMode = isHiddenMode,
        onLongPress = onLongPress,
        onDragDeltaY = onDragDeltaY,
        onDragEnd = onDragEnd,
        modifier = modifier
    )
}

@Composable
fun MiniFloatingBubble(fps: Float, opacity: Float) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF222222).copy(alpha = opacity.coerceIn(0.65f, 0.96f)),
                        Color(0xFF060606).copy(alpha = opacity.coerceIn(0.75f, 0.98f))
                    )
                )
            )
            .border(1.dp, BorderActive, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AccentGreen)
            )
            Text(
                text = "XSD",
                color = PrimaryWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${fps.toInt()} FPS",
                color = PrimaryWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
