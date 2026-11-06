package com.wakemethere.app.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import com.wakemethere.app.WakeMeThereApp
import com.wakemethere.app.alarm.AlarmPlayerService
import com.wakemethere.app.ui.active.ActiveAlarmScreen
import com.wakemethere.app.ui.theme.WakeMeThereTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fullscreen Lockscreen Override Activity that wakes up the device
 * and renders above the keyguard when an alarm is triggered.
 *
 * Implements strict lifecycle monitoring: if minimized, closed, or backed out of,
 * the alarm sound is immediately stopped and disabled in the database,
 * preventing phantom background audio and stopping all GPS tracking.
 */
class ActiveAlarmActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var destinationName: String = "Destination"
    private var distanceMeters: Float? = null
    private var radiusMeters: Float = 500f
    private var isActionHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        destinationName = intent.getStringExtra(EXTRA_DESTINATION_NAME) ?: "Destination"
        distanceMeters = if (intent.hasExtra(EXTRA_DISTANCE_METERS)) {
            intent.getFloatExtra(EXTRA_DISTANCE_METERS, 0f)
        } else null
        radiusMeters = intent.getFloatExtra(EXTRA_RADIUS_METERS, 500f)

        configureLockScreenFlags()

        // Handle back button / gesture: immediately silence alarm and mark disabled
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Back button pressed: stopping and disabling alarm.")
                dismissAlarm(isMinimized = true)
            }
        })

        setContent {
            WakeMeThereTheme {
                ActiveAlarmScreen(
                    destinationName = destinationName,
                    distanceMeters = distanceMeters,
                    radiusMeters = radiusMeters,
                    onDismissAlarm = {
                        dismissAlarm(isMinimized = false)
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

    /**
     * Stops the alarm player service immediately and disables the alarm in the database.
     */
    private fun dismissAlarm(isMinimized: Boolean = false) {
        if (isActionHandled) return
        isActionHandled = true

        Log.d(TAG, "dismissAlarm: stopping alarmId=$alarmId (isMinimized=$isMinimized)")

        val stopIntent = Intent(this, AlarmPlayerService::class.java).apply {
            action = AlarmPlayerService.ACTION_STOP_ALARM
            putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, destinationName)
            putExtra(AlarmPlayerService.EXTRA_IS_MINIMIZED, isMinimized)
        }
        startService(stopIntent)

        // Directly disable alarm in Room database so it is guaranteed turned off
        val currentId = alarmId
        if (currentId > 0) {
            val app = application as? WakeMeThereApp
            CoroutineScope(Dispatchers.IO).launch {
                app?.repository?.setAlarmEnabled(currentId, false)
            }
        }

        val message = if (isMinimized) {
            "Alarm for '$destinationName' minimized & disabled"
        } else {
            "Alarm turned off for '$destinationName'"
        }
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

        finishAndRemoveTask()
    }

    private fun snoozeAlarm() {
        if (isActionHandled) return
        isActionHandled = true

        Log.d(TAG, "snoozeAlarm: snoozing alarmId=$alarmId for 5 minutes")

        val snoozeIntent = Intent(this, AlarmPlayerService::class.java).apply {
            action = AlarmPlayerService.ACTION_SNOOZE_ALARM
            putExtra(AlarmPlayerService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmPlayerService.EXTRA_DESTINATION_NAME, destinationName)
        }
        startService(snoozeIntent)

        Toast.makeText(applicationContext, "Alarm snoozed for 5 minutes", Toast.LENGTH_SHORT).show()
        finishAndRemoveTask()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Called when user presses Home or switches apps while alarm is ringing
        if (!isActionHandled) {
            Log.d(TAG, "onUserLeaveHint: Alarm screen minimized by user. Stopping background sound.")
            dismissAlarm(isMinimized = true)
        }
    }

    override fun onStop() {
        super.onStop()
        // If activity stopped (e.g. screen turned off by power button or minimized)
        if (!isActionHandled && !isChangingConfigurations) {
            Log.d(TAG, "onStop: Alarm screen is no longer visible. Silencing background alarm.")
            dismissAlarm(isMinimized = true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isActionHandled) {
            dismissAlarm(isMinimized = true)
        }
    }

    companion object {
        private const val TAG = "ActiveAlarmActivity"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_DESTINATION_NAME = "extra_destination_name"
        const val EXTRA_DISTANCE_METERS = "extra_distance_meters"
        const val EXTRA_RADIUS_METERS = "extra_radius_meters"
    }
}
