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
import androidx.core.app.NotificationManagerCompat
import com.wakemethere.app.R
import com.wakemethere.app.alarm.AlarmActionReceiver
import com.wakemethere.app.ui.ActiveAlarmActivity
import com.wakemethere.app.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_TRACKER_ID = "wake_me_there_tracker_channel"
    const val CHANNEL_ALARM_ID = "wake_me_there_alarm_channel"

    const val TRACKER_NOTIFICATION_ID = 1001
    const val ALARM_NOTIFICATION_ID = 2002
    const val DISMISSED_NOTIFICATION_ID = 3003

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
     * Creates the persistent sticky notification for the LocationTrackerService,
     * dynamically showing real-time remaining distance in KM or meters to the target destination.
     */
    fun buildTrackerNotification(
        context: Context,
        activeCount: Int,
        nearestAlarmName: String? = null,
        remainingDistanceMeters: Float? = null,
        radiusMeters: Float? = null
    ): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val contentText: String
        val bigText: String

        if (activeCount <= 0) {
            title = context.getString(R.string.tracking_notification_title)
            contentText = "Ready • No destinations currently enabled"
            bigText = contentText
        } else if (nearestAlarmName != null && remainingDistanceMeters != null) {
            val distFormatted = DistanceCalculator.formatDistanceDetailed(remainingDistanceMeters)
            val radiusFormatted = radiusMeters?.let { DistanceCalculator.formatDistance(it) } ?: "trigger radius"
            val isInsideRadius = radiusMeters != null && remainingDistanceMeters <= radiusMeters

            if (activeCount == 1) {
                title = if (isInsideRadius) {
                    "Arriving at $nearestAlarmName!"
                } else {
                    "Tracking: $nearestAlarmName"
                }
                contentText = if (isInsideRadius) {
                    "Inside trigger zone • $distFormatted remaining"
                } else {
                    "$distFormatted remaining to destination"
                }
                bigText = "Destination: $nearestAlarmName\n" +
                        "Distance remaining: $distFormatted\n" +
                        if (isInsideRadius) {
                            "Status: Inside $radiusFormatted trigger radius"
                        } else {
                            "Alarm triggers when within $radiusFormatted"
                        }
            } else {
                title = "Nearest: $nearestAlarmName ($activeCount active)"
                contentText = "$distFormatted remaining to $nearestAlarmName"
                bigText = "Nearest destination: $nearestAlarmName\n" +
                        "Distance remaining: $distFormatted (Radius: $radiusFormatted)\n" +
                        "Total monitored destinations: $activeCount"
            }
        } else {
            title = context.getString(R.string.tracking_notification_title)
            contentText = context.getString(R.string.tracking_notification_text, activeCount)
            bigText = "$contentText\nAcquiring current GPS location..."
        }

        return NotificationCompat.Builder(context, CHANNEL_TRACKER_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
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
        destinationName: String,
        distanceMeters: Float? = null,
        radiusMeters: Float? = null
    ): Notification {
        // Fullscreen Intent to launch ActiveAlarmActivity over lock screen
        val fullScreenIntent = Intent(context, ActiveAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ActiveAlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(ActiveAlarmActivity.EXTRA_DESTINATION_NAME, destinationName)
            if (distanceMeters != null) {
                putExtra(ActiveAlarmActivity.EXTRA_DISTANCE_METERS, distanceMeters)
            }
            if (radiusMeters != null) {
                putExtra(ActiveAlarmActivity.EXTRA_RADIUS_METERS, radiusMeters)
            }
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

        val contentText = if (distanceMeters != null) {
            val distStr = DistanceCalculator.formatDistanceDetailed(distanceMeters)
            context.getString(R.string.alarm_notification_text, destinationName) + " ($distStr remaining)"
        } else {
            context.getString(R.string.alarm_notification_text, destinationName)
        }

        val bigText = if (distanceMeters != null && radiusMeters != null) {
            "You have entered the trigger zone for $destinationName!\n" +
                    "Distance remaining: ${DistanceCalculator.formatDistanceDetailed(distanceMeters)} (Radius: ${DistanceCalculator.formatDistance(radiusMeters)})"
        } else {
            context.getString(R.string.alarm_notification_text, destinationName)
        }

        return NotificationCompat.Builder(context, CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.alarm_notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
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

    /**
     * Shows a brief notification informing the user that the alarm was turned off/minimized
     * and has been disabled, assuring them that GPS tracking is stopped to save battery.
     */
    fun showAlarmDismissedNotification(
        context: Context,
        destinationName: String,
        isMinimized: Boolean = false
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isMinimized) "Alarm Minimized & Disabled" else "Arrived at $destinationName"
        val message = "Alarm for '$destinationName' has been turned off and disabled."
        val detail = "$message GPS tracking is now completely off to save battery."

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_TRACKER_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentPendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(DISMISSED_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // In case notification permission is not granted
        }
    }
}

