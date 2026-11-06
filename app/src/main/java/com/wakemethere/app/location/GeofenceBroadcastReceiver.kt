package com.wakemethere.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.wakemethere.app.WakeMeThereApp
import com.wakemethere.app.alarm.AlarmPlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofence event error: $errorMessage (code: ${geofencingEvent.errorCode})")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        Log.d(TAG, "Geofence transition received: $transition")

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            transition == Geofence.GEOFENCE_TRANSITION_DWELL
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? WakeMeThereApp
                    val repository = app?.repository

                    for (geofence in triggeringGeofences) {
                        val alarmId = geofence.requestId.toLongOrNull() ?: continue
                        Log.i(TAG, "Geofence triggered for alarm ID: $alarmId")

                        val alarm = repository?.getAlarmById(alarmId)
                        if (alarm != null && alarm.isEnabled) {
                            val startAlarmIntent = Intent(context, AlarmPlayerService::class.java).apply {
                                action = AlarmPlayerService.ACTION_START_ALARM
                                putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarm.id)
                                putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, alarm.name)
                                putExtra(AlarmPlayerService.EXTRA_RINGTONE_URI, alarm.ringtoneUri)
                                putExtra(AlarmPlayerService.EXTRA_VIBRATE, alarm.vibrate)
                                putExtra(AlarmPlayerService.EXTRA_ENABLE_VOICE, alarm.enableVoiceAnnouncement)
                                putExtra(AlarmPlayerService.EXTRA_VOICE_STYLE, alarm.voiceAnnouncementStyle)
                                putExtra(AlarmPlayerService.EXTRA_RADIUS_METERS, alarm.radiusMeters)
                            }
                            ContextCompat.startForegroundService(context, startAlarmIntent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling geofence transition", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}

