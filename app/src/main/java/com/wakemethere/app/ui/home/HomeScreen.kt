package com.wakemethere.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.wakemethere.app.data.model.AlarmCategory
import com.wakemethere.app.data.model.LocationAlarm
import com.wakemethere.app.ui.components.BatteryOptimizationBanner
import com.wakemethere.app.ui.components.PermissionBanner
import com.wakemethere.app.ui.theme.AlarmRed
import com.wakemethere.app.util.BatteryOptimizationHelper
import com.wakemethere.app.util.DistanceCalculator

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToMapPicker: () -> Unit,
    onNavigateToEditAlarm: (Long) -> Unit,
    onRequestForegroundLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check permissions and battery optimization status whenever user returns to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val alarms by viewModel.alarms.collectAsState()
    val isIgnoringBatteryOpt by viewModel.isIgnoringBatteryOptimizations.collectAsState()
    val hasFineLocation by viewModel.hasFineLocationPermission.collectAsState()
    val hasBackgroundLocation by viewModel.hasBackgroundLocationPermission.collectAsState()
    val hasNotification by viewModel.hasNotificationPermission.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    var alarmToDelete by remember { mutableStateOf<LocationAlarm?>(null) }
    var showQuickSaveSpotDialog by remember { mutableStateOf(false) }

    val favoriteAlarms = remember(alarms) {
        alarms.filter { it.isFavorite || it.category != "OTHER" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Wake Me There",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val activeCount = alarms.count { it.isEnabled }
                        Text(
                            text = if (activeCount > 0) {
                                "$activeCount active • GPS tracking"
                            } else {
                                "All idle • GPS off (saving battery)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick Save Current Location Spot Button
                    IconButton(
                        onClick = {
                            if (!hasFineLocation) {
                                onRequestForegroundLocation()
                            } else {
                                showQuickSaveSpotDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Save Current Spot as Preset",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { viewModel.refreshSystemStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh status")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToMapPicker,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Alarm", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission & Battery Optimization Banners
            PermissionBanner(
                hasFineLocation = hasFineLocation,
                hasBackgroundLocation = hasBackgroundLocation,
                hasNotification = hasNotification,
                onRequestLocation = onRequestForegroundLocation,
                onRequestBackgroundLocation = onRequestBackgroundLocation,
                onRequestNotification = onRequestNotification
            )

            BatteryOptimizationBanner(
                isIgnoringBatteryOptimizations = isIgnoringBatteryOpt,
                onOptOutClick = {
                    val intent = BatteryOptimizationHelper.createIgnoreBatteryOptimizationIntent(context)
                    context.startActivity(intent)
                }
            )

            if (alarms.isEmpty()) {
                // Empty State UI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Location Alarms",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Save your frequent stops (Home, Work, Bus Stop) once. When commuting or traveling, tap once to arm the alarm and sleep without worries!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            onClick = {
                                if (!hasFineLocation) {
                                    onRequestForegroundLocation()
                                } else {
                                    showQuickSaveSpotDialog = true
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📍 Save Current Spot as Preset (Home/Work/Stop)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    // Quick Commute Presets Carousel (if any saved presets exist)
                    if (favoriteAlarms.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Quick Commute • 1-Tap Arming",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    favoriteAlarms.forEach { favAlarm ->
                                        Card(
                                            modifier = Modifier
                                                .width(185.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    val newState = !favAlarm.isEnabled
                                                    viewModel.toggleAlarm(favAlarm, newState)
                                                    val msg = if (newState) {
                                                        "Armed '${favAlarm.name}'. Sleep well — we will wake you up!"
                                                    } else {
                                                        "Alarm disarmed for '${favAlarm.name}'."
                                                    }
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (favAlarm.isEnabled) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                }
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = favAlarm.alarmCategory.emoji,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        text = if (favAlarm.isEnabled) "ACTIVE" else "TAP TO ARM",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (favAlarm.isEnabled) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = favAlarm.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = DistanceCalculator.formatDistance(favAlarm.radiusMeters),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    // Main List of All Alarms
                    items(items = alarms, key = { it.id }) { alarm ->
                        AlarmItemCard(
                            alarm = alarm,
                            currentLocation = currentLocation,
                            onToggle = { isEnabled -> viewModel.toggleAlarm(alarm, isEnabled) },
                            onEdit = { onNavigateToEditAlarm(alarm.id) },
                            onDelete = { alarmToDelete = alarm },
                            onTestTrigger = { viewModel.testTriggerAlarm(alarm) },
                            onToggleFavorite = { viewModel.toggleFavorite(alarm) }
                        )
                    }
                }
            }
        }
    }

    // Quick Save Current Spot Dialog
    if (showQuickSaveSpotDialog) {
        var selectedCategory by remember { mutableStateOf(AlarmCategory.BUS_STOP) }
        var spotName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickSaveSpotDialog = false },
            title = { Text("Save Current Spot", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Instantly save your current GPS position as a preset destination so you can arm it anytime with 1 tap.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Category chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AlarmCategory.entries.filter { it != AlarmCategory.OTHER }.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    if (spotName.isBlank()) {
                                        spotName = "My ${cat.title}"
                                    }
                                },
                                label = { Text("${cat.emoji} ${cat.title}") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = spotName,
                        onValueChange = { spotName = it },
                        label = { Text("Name this location") },
                        placeholder = { Text("e.g. My Bus Stop, Office, Home") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nameToSave = spotName.trim().ifBlank { "My ${selectedCategory.title}" }
                        viewModel.saveCurrentLocationAsPreset(selectedCategory, nameToSave) { success ->
                            if (success) {
                                Toast.makeText(context, "Saved '$nameToSave'! Tap it on Home Screen to arm.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Could not acquire current GPS location. Please ensure GPS is enabled.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showQuickSaveSpotDialog = false
                    }
                ) {
                    Text("Save Preset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickSaveSpotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    alarmToDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = { Text("Delete Alarm?") },
            text = { Text("Are you sure you want to remove '${alarm.name}'? Geofence monitoring for this destination will be cancelled.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAlarm(alarm)
                        alarmToDelete = null
                    }
                ) {
                    Text("Delete", color = AlarmRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
