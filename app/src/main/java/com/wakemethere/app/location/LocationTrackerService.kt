package com.wakemethere.app.location

import android.annotation.SuppressLint
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

class LocationTrackerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofenceManager: GeofenceManager
    private var activeAlarms: List<LocationAlarm> = emptyList()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            checkProximityToActiveAlarms(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofenceManager = GeofenceManager(this)

        startForegroundWithNotification(0)
        observeActiveAlarms()
        startLocationUpdates()
    }

    private fun startForegroundWithNotification(activeCount: Int) {
        val notification = NotificationHelper.buildTrackerNotification(this, activeCount)
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

    private fun updateNotification(activeCount: Int) {
        val notification = NotificationHelper.buildTrackerNotification(this, activeCount)
        NotificationManagerCompat.from(this).notify(
            NotificationHelper.TRACKER_NOTIFICATION_ID,
            notification
        )
    }

    private fun observeActiveAlarms() {
        val app = application as? WakeMeThereApp ?: return
        serviceScope.launch {
            app.repository.getActiveAlarms().collectLatest { alarms ->
                activeAlarms = alarms
                Log.d(TAG, "Active alarms updated: ${alarms.size}")
                updateNotification(alarms.size)

                // Sync with Geofencing API
                geofenceManager.registerGeofences(alarms)

                if (alarms.isEmpty()) {
                    Log.d(TAG, "No active alarms. Location tracker in idle mode.")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!PermissionUtils.hasFineLocationPermission(this)) {
            Log.w(TAG, "Missing fine location permission for LocationTrackerService")
            return
        }

        // High accuracy GPS request for offline resiliency (works even without WiFi/Cell towers)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_DISPLACEMENT_METERS)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Active GPS location tracking started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request location updates", e)
        }
    }

    /**
     * Dual-redundancy check: Even if Geofencing API batching is delayed or offline,
     * this direct calculation guarantees immediate trigger upon entering radius!
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
                triggerAlarm(alarm)
            }
        }
    }

    private fun triggerAlarm(alarm: LocationAlarm) {
        val startAlarmIntent = Intent(this, AlarmPlayerService::class.java).apply {
            action = AlarmPlayerService.ACTION_START_ALARM
            putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, alarm.name)
            putExtra(AlarmPlayerService.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
            putExtra(AlarmPlayerService.EXTRA_VIBRATE, alarm.vibrate)
            putExtra(AlarmPlayerService.EXTRA_ENABLE_VOICE, alarm.enableVoiceAnnouncement)
            putExtra(AlarmPlayerService.EXTRA_VOICE_STYLE, alarm.voiceAnnouncementStyle)
        }
        ContextCompat.startForegroundService(this, startAlarmIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Sticky ensures Android automatically restarts the tracker service if killed
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        Log.d(TAG, "LocationTrackerService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationTrackerService"
        private const val LOCATION_INTERVAL_MS = 10_000L // 10 seconds
        private const val FASTEST_LOCATION_INTERVAL_MS = 5_000L // 5 seconds
        private const val MIN_DISTANCE_DISPLACEMENT_METERS = 10f // 10 meters

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

