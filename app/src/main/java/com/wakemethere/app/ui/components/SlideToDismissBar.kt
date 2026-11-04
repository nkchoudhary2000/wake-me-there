package com.wakemethere.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakemethere.app.ui.theme.AlarmRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SlideToDismissBar(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Slide to Turn Off Alarm"
) {
    val coroutineScope = rememberCoroutineScope()
    val thumbOffset = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(42.dp))
            .background(Color(0xFF2C2C2C))
            .padding(6.dp)
    ) {
        val density = LocalDensity.current
        val thumbSizePx = with(density) { 72.dp.toPx() }
        val maxDragPx = (constraints.maxWidth - thumbSizePx).coerceAtLeast(0f)

        // Progress fraction from 0f to 1f
        val progress = if (maxDragPx > 0f) (thumbOffset.value / maxDragPx).coerceIn(0f, 1f) else 0f

        // Text label centered, fading out as thumb advances
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = (1f - progress * 1.5f).coerceIn(0f, 1f)),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Draggable Thumb Button
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.value.roundToInt(), 0) }
                .size(72.dp)
                .shadow(elevation = 12.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (progress > 0.7f) Color(0xFF4CAF50) else AlarmRed)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val newOffset = (thumbOffset.value + delta).coerceIn(0f, maxDragPx)
                            thumbOffset.snapTo(newOffset)
                        }
                    },
                    onDragStopped = {
                        coroutineScope.launch {
                            if (progress >= 0.80f) {
                                // Animate to end and invoke callback
                                thumbOffset.animateTo(
                                    maxDragPx,
                                    spring(stiffness = Spring.StiffnessMedium)
                                )
                                onDismiss()
                            } else {
                                // Snap back to start smoothly
                                thumbOffset.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (progress > 0.7f) Icons.Default.AlarmOff else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Slide to dismiss",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

