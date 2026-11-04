package com.wakemethere.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wakemethere.app.ui.theme.AlarmRed
import com.wakemethere.app.ui.theme.AlarmRedContainer

@Composable
fun BatteryOptimizationBanner(
    isIgnoringBatteryOptimizations: Boolean,
    onOptOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isIgnoringBatteryOptimizations) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = AlarmRedContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = AlarmRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Battery Optimization Active",
                        style = MaterialTheme.typography.titleMedium,
                        color = AlarmRed
                    )
                }
                Text(
                    text = "Android may freeze or kill location tracking while sleeping. Disable battery optimization to guarantee alarms trigger offline in your pocket.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = onOptOutClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AlarmRed,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Disable Optimization")
                }
            }
        }
    }
}

