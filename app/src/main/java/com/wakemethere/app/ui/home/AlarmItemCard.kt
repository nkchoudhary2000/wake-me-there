package com.wakemethere.app.ui.home

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wakemethere.app.data.model.LocationAlarm
import com.wakemethere.app.ui.theme.AlarmRed
import com.wakemethere.app.util.DistanceCalculator
import java.util.Locale

@Composable
fun AlarmItemCard(
    alarm: LocationAlarm,
    currentLocation: Location? = null,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestTrigger: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardAlpha = if (alarm.isEnabled) 1.0f else 0.65f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(cardAlpha)
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarm.isEnabled) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Icon + Title + Favorite Star + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (alarm.isEnabled) {
                            Icons.Default.NotificationsActive
                        } else {
                            alarm.alarmCategory.getIcon()
                        },
                        contentDescription = null,
                        tint = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${alarm.alarmCategory.emoji} ${alarm.name}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (alarm.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Toggle favorite",
                                    tint = if (alarm.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "%.5f, %.5f", alarm.latitude, alarm.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Row: Sleek, compact pills in a horizontal scrollable strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Distance pill (concise, e.g. "0 m" or "2.4 km")
                currentLocation?.let { loc ->
                    val distance = DistanceCalculator.calculateDistanceMeters(
                        loc.latitude,
                        loc.longitude,
                        alarm.latitude,
                        alarm.longitude
                    )
                    CompactBadge(
                        text = DistanceCalculator.formatDistance(distance),
                        icon = Icons.Default.NearMe,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Radius pill
                CompactBadge(
                    text = "${DistanceCalculator.formatDistance(alarm.radiusMeters)} radius",
                    icon = Icons.Outlined.Place,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Voice announcement pill (concise: "Voice")
                if (alarm.enableVoiceAnnouncement) {
                    CompactBadge(
                        text = "Voice",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // 1-Shot pill
                if (alarm.isOneShot) {
                    CompactBadge(
                        text = "1-Shot",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.8.dp
            )

            // Dedicated Actions Footer: Status on left, action buttons on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (alarm.isEnabled) "● Armed & monitoring" else "○ Inactive",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Test trigger preview button
                    IconButton(
                        onClick = onTestTrigger,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test Alarm",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Edit button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Alarm",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Alarm",
                            tint = AlarmRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBadge(
    text: String,
    icon: ImageVector? = null,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = contentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
