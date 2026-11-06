package com.wakemethere.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME) ?: "Destination"
        Log.d(TAG, "Received alarm action: $action for alarmId: $alarmId ($destinationName)")

        when (action) {
            ACTION_DISMISS -> {
                val stopIntent = Intent(context, AlarmPlayerService::class.java).apply {
                    this.action = AlarmPlayerService.ACTION_STOP_ALARM
                    putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, destinationName)
                }
                context.startService(stopIntent)
            }
            ACTION_SNOOZE -> {
                val snoozeIntent = Intent(context, AlarmPlayerService::class.java).apply {
                    this.action = AlarmPlayerService.ACTION_SNOOZE_ALARM
                    putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, destinationName)
                }
                context.startService(snoozeIntent)
            }
            ACTION_START_SNOOZED_ALARM -> {
                val startAlarmIntent = Intent(context, AlarmPlayerService::class.java).apply {
                    this.action = AlarmPlayerService.ACTION_START_ALARM
                    putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, "$destinationName (Snoozed)")
                }
                ContextCompat.startForegroundService(context, startAlarmIntent)
            }
        }
    }

    companion object {
        private const val TAG = "AlarmActionReceiver"
        const val ACTION_DISMISS = "com.wakemethere.app.action.DISMISS"
        const val ACTION_SNOOZE = "com.wakemethere.app.action.SNOOZE"
        const val ACTION_START_SNOOZED_ALARM = "com.wakemethere.app.action.START_SNOOZED_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_DESTINATION_NAME = "extra_destination_name"
    }
}

