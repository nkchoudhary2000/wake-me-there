package com.wakemethere.app.ui.active

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wakemethere.app.ui.components.SlideToDismissBar
import com.wakemethere.app.ui.theme.AlarmRed
import com.wakemethere.app.util.DistanceCalculator

@Composable
fun ActiveAlarmScreen(
    destinationName: String,
    distanceMeters: Float? = null,
    radiusMeters: Float = 500f,
    onDismissAlarm: () -> Unit,
    onSnoozeAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing circle animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0C20),
                        Color(0xFF1E0A10),
                        Color(0xFF0A0A0A)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Destination Announcement
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DESTINATION REACHED",
                    color = AlarmRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = destinationName,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                val statusDetail = if (distanceMeters != null) {
                    "Inside ${DistanceCalculator.formatDistance(radiusMeters)} radius • ${DistanceCalculator.formatDistanceDetailed(distanceMeters)} remaining to center"
                } else {
                    "You have entered your ${DistanceCalculator.formatDistance(radiusMeters)} trigger radius."
                }
                Text(
                    text = statusDetail,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Pulsing Alarm Beacon Animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // Outer Pulsing Aura
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .background(AlarmRed.copy(alpha = pulseAlpha), shape = CircleShape)
                )

                // Middle Beacon
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(AlarmRed.copy(alpha = 0.35f), shape = CircleShape)
                )

                // Inner Solid Center
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(AlarmRed, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Active Alarm",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            // Bottom Action Area: Massive Slide To Dismiss & Snooze
            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Massive Draggable Slide To Dismiss
                SlideToDismissBar(
                    onDismiss = onDismissAlarm,
                    text = ">>> Slide to Turn Off Alarm >>>"
                )

                // Snooze Button
                OutlinedButton(
                    onClick = onSnoozeAlarm,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.85f)
                    )
                ) {
                    Text(
                        text = "Snooze for 5 minutes",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

