package com.wakemethere.app

import android.app.Application
import android.util.Log
import com.wakemethere.app.data.local.AlarmDatabase
import com.wakemethere.app.data.repository.AlarmRepository
import com.wakemethere.app.data.repository.AlarmRepositoryImpl
import com.wakemethere.app.location.LocationTrackerService
import com.wakemethere.app.util.NotificationHelper
import com.wakemethere.app.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WakeMeThereApp : Application() {

    lateinit var database: AlarmDatabase
        private set

    lateinit var repository: AlarmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Room Database & Repository
        database = AlarmDatabase.getInstance(this)
        repository = AlarmRepositoryImpl(database.alarmDao())

        // Create system notification channels
        NotificationHelper.createNotificationChannels(this)

        // Check active alarms and prime foreground tracker if permissions are granted
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeAlarms = repository.getActiveAlarmsSync()
                if (activeAlarms.isNotEmpty() && PermissionUtils.hasFineLocationPermission(this@WakeMeThereApp)) {
                    Log.d("WakeMeThereApp", "Auto-starting tracker service for ${activeAlarms.size} active alarms")
                    LocationTrackerService.startService(this@WakeMeThereApp)
                }
            } catch (e: Exception) {
                Log.e("WakeMeThereApp", "Error during app init alarm check", e)
            }
        }
    }

    companion object {
        lateinit var instance: WakeMeThereApp
            private set
    }
}

