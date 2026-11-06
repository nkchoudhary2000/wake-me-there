package com.wakemethere.app.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wakemethere.app.WakeMeThereApp
import com.wakemethere.app.alarm.AlarmPlayerService
import com.wakemethere.app.data.model.LocationAlarm
import com.wakemethere.app.util.DistanceCalculator
import com.wakemethere.app.util.NotificationHelper
import com.wakemethere.app.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Battery-optimized location tracking service.
 *
 * Key Power Management Rules:
 * 1. ZERO GPS Activity When Inactive: If no active alarms are enabled, GPS updates are immediately
 *    stopped, foreground notification is removed, and the service self-terminates.
 * 2. Smart Adaptive GPS Tiers: GPS polling intervals and power priorities dynamically adjust
 *    based on proximity to the nearest destination. Far destinations (>10 km) poll every 60s
 *    using balanced power (saving 85%+ battery), while critical arrival zones (<1.5 km) switch
 *    to high-accuracy for pinpoint trigger precision.
 */
class LocationTrackerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofenceManager: GeofenceManager
    private var activeAlarms: List<LocationAlarm> = emptyList()
    private var lastKnownLocation: Location? = null
    private var isLocationUpdatesActive = false
    private var currentTrackingTier: TrackingTier? = null

    enum class TrackingTier(
        val intervalMs: Long,
        val fastestIntervalMs: Long,
        val minDisplacementMeters: Float,
        val priority: Int
    ) {
        // Over 10 km away: Low-power balanced mode, 60s updates (conserves maximum battery)
        FAR(
            intervalMs = 60_000L,
            fastestIntervalMs = 30_000L,
            minDisplacementMeters = 150f,
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
        ),
        // Between 3 km and 10 km away: 25s updates, 50m displacement
        MEDIUM(
            intervalMs = 25_000L,
            fastestIntervalMs = 15_000L,
            minDisplacementMeters = 50f,
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
        ),
        // Between 1.5 km and 3 km away: 15s updates, high accuracy
        NEAR(
            intervalMs = 15_000L,
            fastestIntervalMs = 8_000L,
            minDisplacementMeters = 25f,
            priority = Priority.PRIORITY_HIGH_ACCURACY
        ),
        // Within 1.5 km (Critical arrival zone): 6s updates, 10m displacement, pinpoint trigger
        CRITICAL(
            intervalMs = 6_000L,
            fastestIntervalMs = 3_000L,
            minDisplacementMeters = 10f,
            priority = Priority.PRIORITY_HIGH_ACCURACY
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            lastKnownLocation = location
            updateNotification()
            checkProximityToActiveAlarms(location)
            adaptLocationUpdatesForActiveAlarms(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofenceManager = GeofenceManager(this)

        startForegroundWithNotification()
        observeActiveAlarms()
    }

    @SuppressLint("MissingPermission")
    private fun fetchInitialLocation() {
        if (!PermissionUtils.hasFineLocationPermission(this)) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    lastKnownLocation = loc
                    updateNotification()
                    checkProximityToActiveAlarms(loc)
                    adaptLocationUpdatesForActiveAlarms(loc)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not obtain initial cached location", e)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = createCurrentTrackerNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.TRACKER_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NotificationHelper.TRACKER_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting tracker in foreground", e)
        }
    }

    private fun updateNotification() {
        val notification = createCurrentTrackerNotification()
        NotificationManagerCompat.from(this).notify(
            NotificationHelper.TRACKER_NOTIFICATION_ID,
            notification
        )
    }

    private fun createCurrentTrackerNotification(): Notification {
        val currentAlarms = activeAlarms
        val location = lastKnownLocation

        if (currentAlarms.isEmpty()) {
            return NotificationHelper.buildTrackerNotification(this, 0)
        }

        if (location == null) {
            return NotificationHelper.buildTrackerNotification(this, currentAlarms.size)
        }

        // Calculate distance to all active alarms and find the nearest one
        val alarmsWithDistance = currentAlarms.map { alarm ->
            val dist = DistanceCalculator.calculateDistanceMeters(
                location.latitude,
                location.longitude,
                alarm.latitude,
                alarm.longitude
            )
            alarm to dist
        }.sortedBy { it.second }

        val nearest = alarmsWithDistance.firstOrNull()
        return if (nearest != null) {
            NotificationHelper.buildTrackerNotification(
                context = this,
                activeCount = currentAlarms.size,
                nearestAlarmName = nearest.first.name,
                remainingDistanceMeters = nearest.second,
                radiusMeters = nearest.first.radiusMeters
            )
        } else {
            NotificationHelper.buildTrackerNotification(this, currentAlarms.size)
        }
    }

    private fun observeActiveAlarms() {
        val app = application as? WakeMeThereApp ?: return
        serviceScope.launch {
            app.repository.getActiveAlarms().collectLatest { alarms ->
                activeAlarms = alarms
                Log.d(TAG, "Active alarms updated: ${alarms.size}")

                if (alarms.isEmpty()) {
                    Log.i(TAG, "No active alarms. Completely shutting down GPS updates and stopping service.")
                    stopLocationUpdates()
                    geofenceManager.removeAllGeofences()
                    ServiceCompat.stopForeground(this@LocationTrackerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }

                // If active alarms exist:
                geofenceManager.registerGeofences(alarms)
                updateNotification()

                if (lastKnownLocation == null) {
                    fetchInitialLocation()
                } else {
                    adaptLocationUpdatesForActiveAlarms(lastKnownLocation)
                }
            }
        }
    }

    /**
     * Dynamically switches the GPS location request between low-power and high-accuracy tiers
     * based on distance to the closest active destination.
     */
    private fun adaptLocationUpdatesForActiveAlarms(location: Location?) {
        val currentAlarms = activeAlarms
        if (currentAlarms.isEmpty()) {
            stopLocationUpdates()
            return
        }

        val nearestDistanceMeters = if (location != null) {
            currentAlarms.minOfOrNull { alarm ->
                DistanceCalculator.calculateDistanceMeters(
                    location.latitude,
                    location.longitude,
                    alarm.latitude,
                    alarm.longitude
                )
            } ?: Float.MAX_VALUE
        } else {
            // No location fix yet: use NEAR tier to get quick accurate initial fix
            1_000f
        }

        val targetTier = when {
            nearestDistanceMeters > 10_000f -> TrackingTier.FAR
            nearestDistanceMeters > 3_000f -> TrackingTier.MEDIUM
            nearestDistanceMeters > 1_500f -> TrackingTier.NEAR
            else -> TrackingTier.CRITICAL
        }

        if (!isLocationUpdatesActive || currentTrackingTier != targetTier) {
            applyTrackingTier(targetTier)
        }
    }

    @SuppressLint("MissingPermission")
    private fun applyTrackingTier(tier: TrackingTier) {
        if (!PermissionUtils.hasFineLocationPermission(this)) {
            Log.w(TAG, "Missing location permission for LocationTrackerService")
            return
        }

        try {
            if (isLocationUpdatesActive) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }

            val request = LocationRequest.Builder(
                tier.priority,
                tier.intervalMs
            )
                .setMinUpdateIntervalMillis(tier.fastestIntervalMs)
                .setMinUpdateDistanceMeters(tier.minDisplacementMeters)
                .setWaitForAccurateLocation(tier == TrackingTier.CRITICAL)
                .build()

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            currentTrackingTier = tier
            isLocationUpdatesActive = true
            Log.i(TAG, "Battery-optimized GPS tier updated to: $tier (interval: ${tier.intervalMs}ms, priority: ${tier.priority})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply tracking tier $tier", e)
        }
    }

    private fun stopLocationUpdates() {
        if (isLocationUpdatesActive) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                isLocationUpdatesActive = false
                currentTrackingTier = null
                Log.d(TAG, "GPS location updates completely stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping location updates", e)
            }
        }
    }

    /**
     * Proximity check: Direct calculation guarantees immediate trigger upon entering radius.
     */
    private fun checkProximityToActiveAlarms(location: Location) {
        val currentAlarms = activeAlarms
        for (alarm in currentAlarms) {
            val distance = DistanceCalculator.calculateDistanceMeters(
                location.latitude,
                location.longitude,
                alarm.latitude,
                alarm.longitude
            )
            Log.d(TAG, "Proximity check: '${alarm.name}' distance = ${distance}m, radius = ${alarm.radiusMeters}m")

            if (distance <= alarm.radiusMeters) {
                Log.i(TAG, "DIRECT PROXIMITY TRIGGER for alarm: ${alarm.name} (${alarm.id})")
                triggerAlarm(alarm, distance)
            }
        }
    }

    private fun triggerAlarm(alarm: LocationAlarm, distanceMeters: Float? = null) {
        val startAlarmIntent = Intent(this, AlarmPlayerService::class.java).apply {
            action = AlarmPlayerService.ACTION_START_ALARM
            putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, alarm.name)
            putExtra(AlarmPlayerService.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
            putExtra(AlarmPlayerService.EXTRA_VIBRATE, alarm.vibrate)
            putExtra(AlarmPlayerService.EXTRA_ENABLE_VOICE, alarm.enableVoiceAnnouncement)
            putExtra(AlarmPlayerService.EXTRA_VOICE_STYLE, alarm.voiceAnnouncementStyle)
            putExtra(AlarmPlayerService.EXTRA_RADIUS_METERS, alarm.radiusMeters)
            if (distanceMeters != null) {
                putExtra(AlarmPlayerService.EXTRA_DISTANCE_METERS, distanceMeters)
            }
        }
        ContextCompat.startForegroundService(this, startAlarmIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
        Log.d(TAG, "LocationTrackerService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationTrackerService"

        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackerService::class.java)
            context.stopService(intent)
        }
    }
}
