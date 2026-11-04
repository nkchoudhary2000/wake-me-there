package com.wakemethere.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wakemethere.app.R
import com.wakemethere.app.alarm.AlarmActionReceiver
import com.wakemethere.app.ui.ActiveAlarmActivity
import com.wakemethere.app.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_TRACKER_ID = "wake_me_there_tracker_channel"
    const val CHANNEL_ALARM_ID = "wake_me_there_alarm_channel"

    const val TRACKER_NOTIFICATION_ID = 1001
    const val ALARM_NOTIFICATION_ID = 2002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Silent low-priority channel for persistent location tracking service
            val trackerChannel = NotificationChannel(
                CHANNEL_TRACKER_ID,
                context.getString(R.string.channel_tracker_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_tracker_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // 2. High-priority loud alarm channel with USAGE_ALARM audio attributes
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                context.getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarm_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 1000)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes)
            }

            notificationManager.createNotificationChannel(trackerChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    /**
     * Creates the persistent sticky notification for the LocationTrackerService.
     */
    fun buildTrackerNotification(context: Context, activeCount: Int): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (activeCount > 0) {
            context.getString(R.string.tracking_notification_text, activeCount)
        } else {
            "Ready • No destinations currently enabled"
        }

        return NotificationCompat.Builder(context, CHANNEL_TRACKER_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.tracking_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Creates the high-priority alarm notification with Full-Screen Intent to wake the screen.
     */
    fun buildAlarmNotification(
        context: Context,
        alarmId: Long,
        destinationName: String
    ): Notification {
        // Fullscreen Intent to launch ActiveAlarmActivity over lock screen
        val fullScreenIntent = Intent(context, ActiveAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ActiveAlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(ActiveAlarmActivity.EXTRA_DESTINATION_NAME, destinationName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_DISMISS
            putExtra(AlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE
            putExtra(AlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.alarm_notification_title))
            .setContentText(context.getString(R.string.alarm_notification_text, destinationName))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.action_dismiss), dismissPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, context.getString(R.string.action_snooze), snoozePendingIntent)
            .build()
    }
}

