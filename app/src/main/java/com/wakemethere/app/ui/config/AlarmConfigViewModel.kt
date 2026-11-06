package com.wakemethere.app.ui.config

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wakemethere.app.WakeMeThereApp
import com.wakemethere.app.alarm.VoiceAnnouncementHelper
import com.wakemethere.app.data.model.AlarmCategory
import com.wakemethere.app.data.model.LocationAlarm
import com.wakemethere.app.data.repository.AlarmRepository
import com.wakemethere.app.location.GeofenceManager
import com.wakemethere.app.location.LocationTrackerService
import com.wakemethere.app.util.PermissionUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AlarmConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository = (application as WakeMeThereApp).repository
    private val geofenceManager = GeofenceManager(application)
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _editingAlarmId = MutableStateFlow<Long?>(null)
    val editingAlarmId: StateFlow<Long?> = _editingAlarmId.asStateFlow()

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _radiusMeters = MutableStateFlow(500f)
    val radiusMeters: StateFlow<Float> = _radiusMeters.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val defaultRingtoneUri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private val _ringtoneUri = MutableStateFlow(defaultRingtoneUri.toString())
    val ringtoneUri: StateFlow<String> = _ringtoneUri.asStateFlow()

    private val _ringtoneTitle = MutableStateFlow("Default System Alarm")
    val ringtoneTitle: StateFlow<String> = _ringtoneTitle.asStateFlow()

    private val _vibrate = MutableStateFlow(true)
    val vibrate: StateFlow<Boolean> = _vibrate.asStateFlow()

    private val _isOneShot = MutableStateFlow(true)
    val isOneShot: StateFlow<Boolean> = _isOneShot.asStateFlow()

    private val _category = MutableStateFlow(AlarmCategory.OTHER.id)
    val category: StateFlow<String> = _category.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _enableVoiceAnnouncement = MutableStateFlow(true)
    val enableVoiceAnnouncement: StateFlow<Boolean> = _enableVoiceAnnouncement.asStateFlow()

    private val _voiceAnnouncementStyle = MutableStateFlow(0)
    val voiceAnnouncementStyle: StateFlow<Int> = _voiceAnnouncementStyle.asStateFlow()

    private val _isVoicePreviewPlaying = MutableStateFlow(false)
    val isVoicePreviewPlaying: StateFlow<Boolean> = _isVoicePreviewPlaying.asStateFlow()

    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Unit>()
    val saveSuccessEvent: SharedFlow<Unit> = _saveSuccessEvent.asSharedFlow()

    private var previewPlayer: MediaPlayer? = null
    private var voicePreviewHelper: VoiceAnnouncementHelper? = null

    init {
        resolveRingtoneTitle(defaultRingtoneUri)
        fetchCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val context = getApplication<Application>()
        if (!PermissionUtils.hasFineLocationPermission(context)) return

        viewModelScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                    ?: fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                if (location != null) {
                    _currentLocation.value = location
                }
            } catch (e: Exception) {
                Log.w("AlarmConfigViewModel", "Failed to fetch current location", e)
            }
        }
    }

    fun setName(newName: String) {
        _name.value = newName
        if (newName.isNotBlank()) {
            _nameError.value = null
        }
    }

    fun initLocation(lat: Double, lng: Double, radius: Float) {
        if (_editingAlarmId.value == null) {
            _latitude.value = lat
            _longitude.value = lng
            _radiusMeters.value = radius
        }
    }

    fun setRadius(radius: Float) {
        _radiusMeters.value = radius
    }

    fun loadAlarmForEdit(alarmId: Long) {
        _editingAlarmId.value = alarmId
        viewModelScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                _name.value = alarm.name
                _latitude.value = alarm.latitude
                _longitude.value = alarm.longitude
                _radiusMeters.value = alarm.radiusMeters
                _ringtoneUri.value = alarm.ringtoneUri
                _ringtoneTitle.value = alarm.ringtoneTitle
                _vibrate.value = alarm.vibrate
                _isOneShot.value = alarm.isOneShot
                _category.value = alarm.category
                _isFavorite.value = alarm.isFavorite
                _enableVoiceAnnouncement.value = alarm.enableVoiceAnnouncement
                _voiceAnnouncementStyle.value = alarm.voiceAnnouncementStyle
            }
        }
    }

    fun setVibrate(enabled: Boolean) {
        _vibrate.value = enabled
    }

    fun setIsOneShot(oneShot: Boolean) {
        _isOneShot.value = oneShot
    }

    fun setCategory(newCategory: AlarmCategory) {
        _category.value = newCategory.id
        if (newCategory != AlarmCategory.OTHER) {
            _isFavorite.value = true // Automatically favorite commute presets like Home/Work/Bus Stop
            _voiceAnnouncementStyle.value = newCategory.defaultVoiceStyle
            // Pre-fill name if empty or generic
            if (_name.value.isBlank() || AlarmCategory.entries.any { it.title.equals(_name.value, ignoreCase = true) }) {
                _name.value = newCategory.title
                _nameError.value = null
            }
        }
    }

    fun setIsFavorite(favorite: Boolean) {
        _isFavorite.value = favorite
    }

    fun setEnableVoiceAnnouncement(enabled: Boolean) {
        _enableVoiceAnnouncement.value = enabled
        if (!enabled) {
            stopVoicePreview()
        }
    }

    fun setVoiceAnnouncementStyle(style: Int) {
        _voiceAnnouncementStyle.value = style
        if (_isVoicePreviewPlaying.value) {
            stopVoicePreview()
            startVoicePreview()
        }
    }

    fun toggleVoicePreview() {
        if (_isVoicePreviewPlaying.value) {
            stopVoicePreview()
        } else {
            startVoicePreview()
        }
    }

    private fun startVoicePreview() {
        stopPreview() // Stop ringtone preview if running
        stopVoicePreview()

        val destination = _name.value.trim().ifBlank { "Destination" }
        val sampleText = VoiceAnnouncementHelper.getAnnouncementText(destination, _voiceAnnouncementStyle.value)

        _isVoicePreviewPlaying.value = true
        if (voicePreviewHelper == null) {
            voicePreviewHelper = VoiceAnnouncementHelper(getApplication())
        }
        voicePreviewHelper?.speakOnce(sampleText) {
            _isVoicePreviewPlaying.value = false
        }
    }

    private fun stopVoicePreview() {
        voicePreviewHelper?.stopRepeating()
        _isVoicePreviewPlaying.value = false
    }

    fun setRingtone(uri: Uri?) {
        stopPreview()
        stopVoicePreview()
        if (uri != null) {
            _ringtoneUri.value = uri.toString()
            resolveRingtoneTitle(uri)
        }
    }

    private fun resolveRingtoneTitle(uri: Uri) {
        try {
            val ringtone = RingtoneManager.getRingtone(getApplication(), uri)
            _ringtoneTitle.value = ringtone.getTitle(getApplication()) ?: "Alarm Sound"
        } catch (e: Exception) {
            _ringtoneTitle.value = "Selected Ringtone"
        }
    }

    fun togglePreviewPlayback() {
        if (_isPreviewPlaying.value) {
            stopPreview()
        } else {
            startPreview()
        }
    }

    private fun startPreview() {
        stopPreview()
        try {
            val targetUri = Uri.parse(_ringtoneUri.value)
            previewPlayer = MediaPlayer().apply {
                setDataSource(getApplication<Application>(), targetUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                setOnCompletionListener {
                    _isPreviewPlaying.value = false
                }
                prepare()
                start()
            }
            _isPreviewPlaying.value = true
        } catch (e: Exception) {
            _isPreviewPlaying.value = false
        }
    }

    private fun stopPreview() {
        previewPlayer?.stop()
        previewPlayer?.release()
        previewPlayer = null
        _isPreviewPlaying.value = false
    }

    fun saveAlarm(fallbackLat: Double = 0.0, fallbackLng: Double = 0.0, fallbackRadius: Float = 500f) {
        val currentName = _name.value.trim()
        if (currentName.isBlank()) {
            _nameError.value = "Please enter a destination name"
            return
        }

        stopPreview()
        stopVoicePreview()

        val finalLat = if (_latitude.value != 0.0) _latitude.value else fallbackLat
        val finalLng = if (_longitude.value != 0.0) _longitude.value else fallbackLng
        val finalRadius = if (_radiusMeters.value > 0f) _radiusMeters.value else fallbackRadius

        viewModelScope.launch {
            val editId = _editingAlarmId.value
            if (editId != null && editId > 0L) {
                // Update existing alarm
                val existing = repository.getAlarmById(editId)
                val updatedAlarm = (existing ?: LocationAlarm(
                    name = currentName,
                    latitude = finalLat,
                    longitude = finalLng,
                    radiusMeters = finalRadius,
                    ringtoneUri = _ringtoneUri.value,
                    ringtoneTitle = _ringtoneTitle.value
                )).copy(
                    id = editId,
                    name = currentName,
                    latitude = finalLat,
                    longitude = finalLng,
                    radiusMeters = finalRadius,
                    ringtoneUri = _ringtoneUri.value,
                    ringtoneTitle = _ringtoneTitle.value,
                    vibrate = _vibrate.value,
                    isOneShot = _isOneShot.value,
                    enableVoiceAnnouncement = _enableVoiceAnnouncement.value,
                    voiceAnnouncementStyle = _voiceAnnouncementStyle.value,
                    category = _category.value,
                    isFavorite = _isFavorite.value
                )
                repository.updateAlarm(updatedAlarm)

                // Sync geofences
                val activeAlarms = repository.getActiveAlarmsSync()
                geofenceManager.registerGeofences(activeAlarms)
                if (activeAlarms.isNotEmpty()) {
                    LocationTrackerService.startService(getApplication())
                } else {
                    LocationTrackerService.stopService(getApplication())
                }
            } else {
                // Save new alarm
                val newAlarm = LocationAlarm(
                    name = currentName,
                    latitude = finalLat,
                    longitude = finalLng,
                    radiusMeters = finalRadius,
                    isEnabled = true,
                    ringtoneUri = _ringtoneUri.value,
                    ringtoneTitle = _ringtoneTitle.value,
                    vibrate = _vibrate.value,
                    isOneShot = _isOneShot.value,
                    enableVoiceAnnouncement = _enableVoiceAnnouncement.value,
                    voiceAnnouncementStyle = _voiceAnnouncementStyle.value,
                    category = _category.value,
                    isFavorite = _isFavorite.value
                )

                repository.saveAlarm(newAlarm)

                // Register Geofence & Start Tracker Service
                val activeAlarms = repository.getActiveAlarmsSync()
                geofenceManager.registerGeofences(activeAlarms)
                LocationTrackerService.startService(getApplication())
            }

            _saveSuccessEvent.emit(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
        stopVoicePreview()
        voicePreviewHelper?.shutdown()
        voicePreviewHelper = null
    }
}

