package com.wakemethere.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        Log.d(TAG, "Received alarm action: $action for alarmId: $alarmId")

        when (action) {
            ACTION_DISMISS -> {
                val stopIntent = Intent(context, AlarmPlayerService::class.java).apply {
                    this.action = AlarmPlayerService.ACTION_STOP_ALARM
                    putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
                }
                context.startService(stopIntent)
            }
            ACTION_SNOOZE -> {
                val snoozeIntent = Intent(context, AlarmPlayerService::class.java).apply {
                    this.action = AlarmPlayerService.ACTION_SNOOZE_ALARM
                    putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
                }
                context.startService(snoozeIntent)
            }
        }
    }

    companion object {
        private const val TAG = "AlarmActionReceiver"
        const val ACTION_DISMISS = "com.wakemethere.app.action.DISMISS"
        const val ACTION_SNOOZE = "com.wakemethere.app.action.SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}

