package com.wakemethere.app.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wakemethere.app.alarm.AlarmPlayerService
import com.wakemethere.app.ui.active.ActiveAlarmScreen
import com.wakemethere.app.ui.theme.WakeMeThereTheme

/**
 * Fullscreen Lockscreen Override Activity that wakes up the device
 * and renders above the keyguard when an alarm is triggered.
 */
class ActiveAlarmActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var destinationName: String = "Destination"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME) ?: "Destination"

        configureLockScreenFlags()

        setContent {
            WakeMeThereTheme {
                ActiveAlarmScreen(
                    destinationName = destinationName,
                    onDismissAlarm = {
                        dismissAlarm()
                    },
                    onSnoozeAlarm = {
                        snoozeAlarm()
                    }
                )
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun dismissAlarm() {
        val stopIntent = Intent(this, AlarmPlayerService::class.java).apply {
            action = AlarmPlayerService.ACTION_STOP_ALARM
            putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
        }
        startService(stopIntent)
        finishAndRemoveTask()
    }

    private fun snoozeAlarm() {
        val snoozeIntent = Intent(this, AlarmPlayerService::class.java).apply {
            action = AlarmPlayerService.ACTION_SNOOZE_ALARM
            putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
        }
        startService(snoozeIntent)
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_DESTINATION_NAME = "extra_destination_name"
    }
}

