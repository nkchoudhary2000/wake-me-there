package com.wakemethere.app.ui.config

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakemethere.app.alarm.VoiceAnnouncementHelper
import com.wakemethere.app.data.model.AlarmCategory
import com.wakemethere.app.util.DistanceCalculator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmConfigScreen(
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    radiusMeters: Float = 500f,
    initialName: String = "",
    alarmIdToEdit: Long? = null,
    viewModel: AlarmConfigViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val name by viewModel.name.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val ringtoneTitle by viewModel.ringtoneTitle.collectAsState()
    val ringtoneUri by viewModel.ringtoneUri.collectAsState()
    val vibrate by viewModel.vibrate.collectAsState()
    val isOneShot by viewModel.isOneShot.collectAsState()
    val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsState()
    val enableVoiceAnnouncement by viewModel.enableVoiceAnnouncement.collectAsState()
    val voiceAnnouncementStyle by viewModel.voiceAnnouncementStyle.collectAsState()
    val isVoicePreviewPlaying by viewModel.isVoicePreviewPlaying.collectAsState()
    val category by viewModel.category.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val editingAlarmId by viewModel.editingAlarmId.collectAsState()
    val currentLat by viewModel.latitude.collectAsState()
    val currentLng by viewModel.longitude.collectAsState()
    val currentRadius by viewModel.radiusMeters.collectAsState()
    val userLocation by viewModel.currentLocation.collectAsState()

    val displayLat = if (currentLat != 0.0) currentLat else latitude
    val displayLng = if (currentLng != 0.0) currentLng else longitude
    val displayRadius = if (currentRadius > 0f) currentRadius else radiusMeters

    val distanceFromUser = userLocation?.let { loc ->
        if (displayLat != 0.0 || displayLng != 0.0) {
            DistanceCalculator.calculateDistanceMeters(
                loc.latitude,
                loc.longitude,
                displayLat,
                displayLng
            )
        } else null
    }

    // Refresh user location when screen appears
    LaunchedEffect(Unit) {
        viewModel.fetchCurrentLocation()
    }

    // Load alarm if in edit mode, or initialize location if in new mode
    LaunchedEffect(alarmIdToEdit, latitude, longitude, radiusMeters) {
        if (alarmIdToEdit != null && alarmIdToEdit > 0L) {
            viewModel.loadAlarmForEdit(alarmIdToEdit)
        } else {
            viewModel.initLocation(latitude, longitude, radiusMeters)
        }
    }

    // Pre-fill destination name if passed from Map Picker search
    LaunchedEffect(initialName) {
        if (initialName.isNotBlank() && viewModel.name.value.isBlank() && alarmIdToEdit == null) {
            viewModel.setName(initialName)
        }
    }

    // Listen for save completion
    LaunchedEffect(Unit) {
        viewModel.saveSuccessEvent.collect {
            onSaveSuccess()
        }
    }

    // Android System Ringtone Picker Contract Launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            viewModel.setRingtone(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingAlarmId != null) "Edit Alarm" else "Configure Alarm", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Destination Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.setName(it) },
                label = { Text("Destination Name") },
                placeholder = { Text("e.g., Home, Work Station, Downtown") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Preset Quick Selection (Home, Work, Bus Stop, etc.)
            Column {
                Text(
                    text = "Quick Place Preset",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlarmCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat.id,
                            onClick = { viewModel.setCategory(cat) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.getIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text("${cat.emoji} ${cat.title}") }
                        )
                    }
                }
            }

            // Save as Favorite / Quick Commute Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Save to Quick Commute",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "One-tap arming from Home Screen when you travel/sleep",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { viewModel.setIsFavorite(it) }
                    )
                }
            }

            // Location Target Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Target Coordinates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format(Locale.US, "%.5f, %.5f", displayLat, displayLng),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Trigger Radius: ${DistanceCalculator.formatDistance(displayRadius)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Real-time Distance from User Section
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Distance from your location",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                                if (distanceFromUser != null) {
                                    Text(
                                        text = DistanceCalculator.formatDistanceDetailed(distanceFromUser),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    val distanceToBoundary = (distanceFromUser - displayRadius).coerceAtLeast(0f)
                                    val statusText = if (distanceFromUser <= displayRadius) {
                                        "🎯 Inside ${DistanceCalculator.formatDistance(displayRadius)} radius"
                                    } else {
                                        "🔔 Triggers in ${DistanceCalculator.formatDistance(distanceToBoundary)}"
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Acquiring GPS location...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Adjust Trigger Radius:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = displayRadius,
                        onValueChange = { viewModel.setRadius(it) },
                        valueRange = 100f..5000f,
                        steps = 48,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Ringtone Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ringtone",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = ringtoneTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Play / Stop Preview button
                        IconButton(onClick = { viewModel.togglePreviewPlayback() }) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Preview sound",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose Alarm Ringtone")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(ringtoneUri))
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Change Ringtone")
                    }
                }
            }

            // Voice Announcement on Arrival Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Voice Announcement",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Speak destination with natural voice",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = enableVoiceAnnouncement,
                            onCheckedChange = { viewModel.setEnableVoiceAnnouncement(it) }
                        )
                    }

                    if (enableVoiceAnnouncement) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Announcement Style",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Style selector chips (horizontal scroll + concise label prevents text wrapping)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VoiceAnnouncementHelper.STYLES.forEach { (styleId, _) ->
                                val shortLabel = VoiceAnnouncementHelper.getStyleShortLabel(styleId)
                                FilterChip(
                                    selected = voiceAnnouncementStyle == styleId,
                                    onClick = { viewModel.setVoiceAnnouncementStyle(styleId) },
                                    label = {
                                        Text(
                                            text = shortLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Current speech sample quotation box
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💬 \"${VoiceAnnouncementHelper.getAnnouncementText(name, voiceAnnouncementStyle)}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FilledTonalButton(
                            onClick = { viewModel.toggleVoicePreview() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isVoicePreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isVoicePreviewPlaying) "Stop Voice Preview" else "🔊 Test Natural Voice Announcement")
                        }
                    }
                }
            }

            // Vibration Setting Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Vibrate on Alarm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Pulsing vibration pattern",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { viewModel.setVibrate(it) }
                    )
                }
            }

            // One-Shot Alarm Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Disable After Trigger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Turns off alarm automatically once arrived at destination",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isOneShot,
                        onCheckedChange = { viewModel.setIsOneShot(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save Alarm Button
            Button(
                onClick = {
                    viewModel.saveAlarm(latitude, longitude, radiusMeters)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingAlarmId != null) "Update Alarm" else "Save & Activate Alarm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

