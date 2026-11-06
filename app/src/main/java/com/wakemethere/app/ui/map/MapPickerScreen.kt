package com.wakemethere.app.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.wakemethere.app.ui.theme.CircleFillColor
import com.wakemethere.app.ui.theme.CircleStrokeColor
import com.wakemethere.app.util.DistanceCalculator
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MapPickerScreen(
    viewModel: MapPickerViewModel,
    onNavigateBack: () -> Unit,
    onConfirmLocation: (Double, Double, Float, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val selectedLocation by viewModel.selectedLatLng.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val currentAreaName by viewModel.currentAreaName.collectAsState()
    val selectedAreaName by viewModel.selectedAreaName.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val radiusMeters by viewModel.radiusMeters.collectAsState()
    val isOfflineMode by viewModel.isOfflineFallbackMode.collectAsState()

    // When search suggestions are actively shown, pressing hardware back dismisses them
    BackHandler(enabled = searchResults.isNotEmpty()) {
        viewModel.clearSearch()
        focusManager.clearFocus()
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation, 15f)
    }

    val markerState = rememberMarkerState(position = selectedLocation)

    // Keep marker in sync with selected location
    LaunchedEffect(selectedLocation) {
        markerState.position = selectedLocation
    }

    // Keep selected location in sync if marker is dragged on map
    LaunchedEffect(markerState.position) {
        if (markerState.position != selectedLocation) {
            viewModel.setLocation(markerState.position)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Base Map Layer
        if (isOfflineMode) {
            OfflineMapFallback(
                selectedLocation = selectedLocation,
                radiusMeters = radiusMeters,
                currentLocation = currentLocation,
                onLocationChanged = { latLng ->
                    viewModel.setLocation(latLng)
                }
            )
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = currentLocation != null,
                    isBuildingEnabled = true
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = true
                ),
                onMapClick = { latLng ->
                    focusManager.clearFocus()
                    if (searchResults.isNotEmpty()) {
                        viewModel.clearSearch()
                    }
                    viewModel.setLocation(latLng)
                }
            ) {
                // Target Pin Marker
                Marker(
                    state = markerState,
                    title = selectedAreaName,
                    snippet = "Radius: ${DistanceCalculator.formatDistance(radiusMeters)}",
                    draggable = true
                )

                // Visual Radius Circle
                Circle(
                    center = selectedLocation,
                    radius = radiusMeters.toDouble(),
                    fillColor = CircleFillColor,
                    strokeColor = CircleStrokeColor,
                    strokeWidth = 4f
                )
            }
        }

        // 2. My Location Floating Button (only visible when not browsing search suggestions)
        if (!isOfflineMode && currentLocation != null && searchResults.isEmpty()) {
            SmallFloatingActionButton(
                onClick = {
                    currentLocation?.let { current ->
                        viewModel.setLocation(current, currentAreaName)
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(current, 16f)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 350.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on GPS")
            }
        }

        // 3. Bottom Configuration & Confirmation Sheet ("Set Alarm for This Area")
        // Visible by default and whenever search suggestions are not active.
        AnimatedVisibility(
            visible = searchResults.isEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Destination Name and Distance Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedAreaName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(Locale.US, "%.5f, %.5f", selectedLocation.latitude, selectedLocation.longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Distance from Current Location
                        currentLocation?.let { current ->
                            val distance = DistanceCalculator.calculateDistanceMeters(
                                current.latitude,
                                current.longitude,
                                selectedLocation.latitude,
                                selectedLocation.longitude
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Distance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DistanceCalculator.formatDistance(distance),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${distance.toInt()} m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Real-time Distance & Trigger Zone Banner
                    currentLocation?.let { current ->
                        val distance = DistanceCalculator.calculateDistanceMeters(
                            current.latitude,
                            current.longitude,
                            selectedLocation.latitude,
                            selectedLocation.longitude
                        )
                        val remainingToTrigger = (distance - radiusMeters).coerceAtLeast(0f)
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (distance <= radiusMeters) {
                                        "Inside trigger radius • ${DistanceCalculator.formatDistance(distance)}"
                                    } else {
                                        "${DistanceCalculator.formatDistance(distance)} away • ${DistanceCalculator.formatDistance(remainingToTrigger)} to radius"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Radius Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trigger Radius",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = DistanceCalculator.formatDistance(radiusMeters),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Slider from 100m to 5000m
                    Slider(
                        value = radiusMeters,
                        onValueChange = { viewModel.setRadius(it) },
                        valueRange = 100f..5000f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Quick Radius Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(100f, 250f, 500f, 1000f, 2000f, 5000f)
                        presets.forEach { preset ->
                            FilterChip(
                                selected = (radiusMeters == preset),
                                onClick = { viewModel.setRadius(preset) },
                                label = { Text(DistanceCalculator.formatDistance(preset)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Destination Button
                    Button(
                        onClick = {
                            onConfirmLocation(
                                selectedLocation.latitude,
                                selectedLocation.longitude,
                                radiusMeters,
                                selectedAreaName
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Set Alarm for This Area",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        // 4. Top Search Bar & Suggestions Overlay (Placed LAST so it renders ON TOP of everything)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 42.dp, start = 16.dp, end = 16.dp)
        ) {
            // Search Box Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button ALWAYS navigates back
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search area or place name...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                viewModel.performSearch(searchQuery)
                            }
                        )
                    )

                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotBlank() || searchResults.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.clearSearch()
                                focusManager.clearFocus()
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }

                    // Toggle Map vs Offline Radar
                    IconButton(onClick = { viewModel.toggleOfflineFallbackMode() }) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle Mode",
                            tint = if (isOfflineMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Results Dropdown List (renders immediately below search box on top layer)
            AnimatedVisibility(
                visible = searchResults.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 350.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(searchResults) { place ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusManager.clearFocus()
                                        viewModel.selectPlace(place)
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(place.latLng, 16f),
                                                durationMs = 800
                                            )
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = place.secondaryText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Distance chip if current GPS is available
                                currentLocation?.let { current ->
                                    val dist = DistanceCalculator.calculateDistanceMeters(
                                        current.latitude,
                                        current.longitude,
                                        place.latLng.latitude,
                                        place.latLng.longitude
                                    )
                                    Text(
                                        text = DistanceCalculator.formatDistance(dist),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // Current Area GPS Banner (only shown when search suggestions are not active)
            if (searchResults.isEmpty() && currentAreaName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .clickable {
                            currentLocation?.let { current ->
                                viewModel.setLocation(current, currentAreaName)
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(current, 16f)
                                    )
                                }
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You are in: $currentAreaName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
