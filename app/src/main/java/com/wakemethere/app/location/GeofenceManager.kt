package com.wakemethere.app.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.wakemethere.app.data.model.LocationAlarm
import com.wakemethere.app.util.PermissionUtils
import kotlinx.coroutines.tasks.await

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, GEOFENCE_PENDING_INTENT_REQ_CODE, intent, flags)
    }

    /**
     * Registers all active location alarms with Google Play Services Geofencing API.
     */
    @SuppressLint("MissingPermission")
    suspend fun registerGeofences(alarms: List<LocationAlarm>): Boolean {
        if (!PermissionUtils.hasFineLocationPermission(context)) {
            Log.w(TAG, "Cannot register geofences: fine location permission missing")
            return false
        }

        val activeAlarms = alarms.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) {
            removeAllGeofences()
            return true
        }

        val geofenceList = activeAlarms.map { alarm ->
            Geofence.Builder()
                .setRequestId(alarm.id.toString())
                .setCircularRegion(
                    alarm.latitude,
                    alarm.longitude,
                    alarm.radiusMeters.coerceAtLeast(50f)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                // 5-second responsiveness ensures fast detection even without internet
                .setNotificationResponsiveness(5000)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        return try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.d(TAG, "Successfully registered ${geofenceList.size} geofence(s)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register geofences", e)
            false
        }
    }

    /**
     * Removes a single geofence by alarm ID.
     */
    suspend fun removeGeofence(alarmId: Long): Boolean {
        return try {
            geofencingClient.removeGeofences(listOf(alarmId.toString())).await()
            Log.d(TAG, "Removed geofence for alarm: $alarmId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove geofence $alarmId", e)
            false
        }
    }

    /**
     * Removes all registered geofences.
     */
    suspend fun removeAllGeofences(): Boolean {
        return try {
            geofencingClient.removeGeofences(geofencePendingIntent).await()
            Log.d(TAG, "Removed all geofences")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove all geofences", e)
            false
        }
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_PENDING_INTENT_REQ_CODE = 9001
    }
}

