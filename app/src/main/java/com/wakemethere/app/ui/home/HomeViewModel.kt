package com.wakemethere.app.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.location.Location
import android.media.RingtoneManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.wakemethere.app.WakeMeThereApp
import com.wakemethere.app.alarm.AlarmPlayerService
import com.wakemethere.app.data.model.AlarmCategory
import com.wakemethere.app.data.model.LocationAlarm
import com.wakemethere.app.data.repository.AlarmRepository
import com.wakemethere.app.location.GeofenceManager
import com.wakemethere.app.location.LocationTrackerService
import com.wakemethere.app.util.BatteryOptimizationHelper
import com.wakemethere.app.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository =
        (application as WakeMeThereApp).repository

    private val geofenceManager = GeofenceManager(application)

    val alarms: StateFlow<List<LocationAlarm>> = repository.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isIgnoringBatteryOptimizations = MutableStateFlow(true)
    val isIgnoringBatteryOptimizations: StateFlow<Boolean> = _isIgnoringBatteryOptimizations.asStateFlow()

    private val _hasFineLocationPermission = MutableStateFlow(false)
    val hasFineLocationPermission: StateFlow<Boolean> = _hasFineLocationPermission.asStateFlow()

    private val _hasBackgroundLocationPermission = MutableStateFlow(false)
    val hasBackgroundLocationPermission: StateFlow<Boolean> = _hasBackgroundLocationPermission.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(false)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    init {
        refreshSystemStatus()
    }

    fun refreshSystemStatus() {
        val context = getApplication<Application>()
        _isIgnoringBatteryOptimizations.value =
            BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        _hasFineLocationPermission.value = PermissionUtils.hasFineLocationPermission(context)
        _hasBackgroundLocationPermission.value = PermissionUtils.hasBackgroundLocationPermission(context)
        _hasNotificationPermission.value = PermissionUtils.hasNotificationPermission(context)
        refreshLocation()
    }

    @SuppressLint("MissingPermission")
    fun refreshLocation() {
        val context = getApplication<Application>()
        if (!PermissionUtils.hasFineLocationPermission(context)) return
        viewModelScope.launch {
            try {
                val loc = fusedLocationClient.lastLocation.await()
                if (loc != null) {
                    _currentLocation.value = loc
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleAlarm(alarm: LocationAlarm, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setAlarmEnabled(alarm.id, isEnabled)

            // Re-sync tracker service
            val context = getApplication<Application>()
            val activeAlarms = repository.getActiveAlarmsSync()
            if (activeAlarms.isNotEmpty()) {
                geofenceManager.registerGeofences(activeAlarms)
                LocationTrackerService.startService(context)
            } else {
                geofenceManager.removeGeofence(alarm.id)
                LocationTrackerService.stopService(context)
            }
        }
    }

    fun toggleFavorite(alarm: LocationAlarm) {
        viewModelScope.launch {
            repository.setAlarmFavorite(alarm.id, !alarm.isFavorite)
        }
    }

    fun saveCurrentLocationAsPreset(
        category: AlarmCategory,
        customName: String = "",
        onComplete: (Boolean) -> Unit
    ) {
        val context = getApplication<Application>()
        if (!PermissionUtils.hasFineLocationPermission(context)) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                val location = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                    ?: fusedClient.lastLocation.await()

                if (location != null) {
                    val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                    val placeName = customName.trim().ifBlank { category.title }
                    val newAlarm = LocationAlarm(
                        name = placeName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radiusMeters = category.defaultRadiusMeters,
                        isEnabled = false,
                        ringtoneUri = defaultRingtoneUri.toString(),
                        ringtoneTitle = "Default Alarm Sound",
                        vibrate = true,
                        isOneShot = true,
                        enableVoiceAnnouncement = true,
                        voiceAnnouncementStyle = category.defaultVoiceStyle,
                        category = category.id,
                        isFavorite = true
                    )
                    repository.saveAlarm(newAlarm)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun deleteAlarm(alarm: LocationAlarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            geofenceManager.removeGeofence(alarm.id)

            val activeAlarms = repository.getActiveAlarmsSync()
            val context = getApplication<Application>()
            if (activeAlarms.isNotEmpty()) {
                LocationTrackerService.startService(context)
            } else {
                LocationTrackerService.stopService(context)
            }
        }
    }

    fun testTriggerAlarm(alarm: LocationAlarm) {
        val context = getApplication<Application>()
        val startAlarmIntent = Intent(context, AlarmPlayerService::class.java).apply {
            action = AlarmPlayerService.ACTION_START_ALARM
            putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, "${alarm.name} (Test Preview)")
            putExtra(AlarmPlayerService.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
            putExtra(AlarmPlayerService.EXTRA_VIBRATE, alarm.vibrate)
            putExtra(AlarmPlayerService.EXTRA_ENABLE_VOICE, alarm.enableVoiceAnnouncement)
            putExtra(AlarmPlayerService.EXTRA_VOICE_STYLE, alarm.voiceAnnouncementStyle)
        }
        ContextCompat.startForegroundService(context, startAlarmIntent)
    }
}

