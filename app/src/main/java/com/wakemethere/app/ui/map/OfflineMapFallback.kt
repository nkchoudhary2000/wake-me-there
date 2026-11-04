package com.wakemethere.app.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.wakemethere.app.util.DistanceCalculator
import java.util.Locale

@Composable
fun OfflineMapFallback(
    selectedLocation: LatLng,
    radiusMeters: Float,
    currentLocation: LatLng?,
    onLocationChanged: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualInputDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E2429)),
        contentAlignment = Alignment.Center
    ) {
        // Radar / Grid Canvas for offline spatial visualization
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2.2f

            // Concentric radar circles
            for (i in 1..4) {
                val r = maxRadius * (i / 4f)
                drawCircle(
                    color = Color(0xFF37474F),
                    radius = r,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            // Crosshair axes
            drawLine(
                color = Color(0xFF37474F),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0xFF37474F),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 2f
            )

            // Radius Circle in green
            drawCircle(
                color = Color(0x334CAF50),
                radius = maxRadius * 0.45f,
                center = center
            )
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = maxRadius * 0.45f,
                center = center,
                style = Stroke(width = 4f)
            )

            // Target Pin Center
            drawCircle(
                color = Color(0xFFE53935),
                radius = 16f,
                center = center
            )
        }

        // Top Status Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC000000)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Offline GPS Navigation Mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Map tiles offline • GPS coordinates fully operational",
                        color = Color(0xFFB0BEC5),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "Target: %.5f, %.5f",
                            selectedLocation.latitude,
                            selectedLocation.longitude
                        ),
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currentLocation != null) {
                        val distance = DistanceCalculator.calculateDistanceMeters(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            selectedLocation.latitude,
                            selectedLocation.longitude
                        )
                        Text(
                            text = "Distance to target: ${DistanceCalculator.formatDistance(distance)}",
                            color = Color(0xFF64B5F6),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showManualInputDialog = true }
                ) {
                    Icon(Icons.Default.EditLocation, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enter Lat/Lng", color = Color.White)
                }

                currentLocation?.let { current ->
                    Button(onClick = { onLocationChanged(current) }) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use My GPS")
                    }
                }
            }
        }
    }

    // Manual Coordinate Input Dialog
    if (showManualInputDialog) {
        var latText by remember { mutableStateOf(selectedLocation.latitude.toString()) }
        var lngText by remember { mutableStateOf(selectedLocation.longitude.toString()) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = { Text("Enter Target GPS Coordinates") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Input precise geographical coordinates. Works 100% offline without cellular or Wi-Fi.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude (-90 to 90)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude (-180 to 180)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            "Invalid coordinates. Please check your numbers.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val lat = latText.toDoubleOrNull()
                        val lng = lngText.toDoubleOrNull()
                        if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                            onLocationChanged(LatLng(lat, lng))
                            showManualInputDialog = false
                        } else {
                            isError = true
                        }
                    }
                ) {
                    Text("Set Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

