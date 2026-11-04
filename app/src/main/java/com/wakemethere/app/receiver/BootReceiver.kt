package com.wakemethere.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wakemethere.app.WakeMeThereApp
import com.wakemethere.app.location.GeofenceManager
import com.wakemethere.app.location.LocationTrackerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? WakeMeThereApp
                    val activeAlarms = app?.repository?.getActiveAlarmsSync() ?: emptyList()
                    Log.d(TAG, "Reboot recovery: found ${activeAlarms.size} active alarms")

                    if (activeAlarms.isNotEmpty()) {
                        // Re-register geofences with Google Play Services
                        val geofenceManager = GeofenceManager(context)
                        geofenceManager.registerGeofences(activeAlarms)

                        // Restart continuous location tracker foreground service
                        LocationTrackerService.startService(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

