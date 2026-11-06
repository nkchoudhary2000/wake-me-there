package com.wakemethere.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.wakemethere.app.WakeMeThereApp
import com.wakemethere.app.ui.ActiveAlarmActivity
import com.wakemethere.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmPlayerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var voiceAnnouncementHelper: VoiceAnnouncementHelper? = null
    private var currentAlarmId: Long = -1L

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        initVibrator()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        val destinationName = intent?.getStringExtra(EXTRA_DESTINATION_NAME) ?: "Destination"
        val ringtoneUriStr = intent?.getStringExtra(EXTRA_RINGTONE_URI)
        val shouldVibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true
        val enableVoice = intent?.getBooleanExtra(EXTRA_ENABLE_VOICE, true) ?: true
        val voiceStyle = intent?.getIntExtra(EXTRA_VOICE_STYLE, 0) ?: 0
        val distanceMeters = if (intent?.hasExtra(EXTRA_DISTANCE_METERS) == true) {
            intent.getFloatExtra(EXTRA_DISTANCE_METERS, 0f)
        } else null
        val radiusMeters = intent?.getFloatExtra(EXTRA_RADIUS_METERS, 500f) ?: 500f

        Log.d(TAG, "onStartCommand: action=$action, alarmId=$alarmId, enableVoice=$enableVoice")

        when (action) {
            ACTION_START_ALARM -> {
                currentAlarmId = alarmId
                startAlarmForeground(
                    alarmId,
                    destinationName,
                    ringtoneUriStr,
                    shouldVibrate,
                    enableVoice,
                    voiceStyle,
                    distanceMeters,
                    radiusMeters
                )
            }
            ACTION_STOP_ALARM -> {
                val isMinimized = intent?.getBooleanExtra(EXTRA_IS_MINIMIZED, false) ?: false
                stopAlarmAndSelf(alarmId, destinationName, isMinimized)
            }
            ACTION_SNOOZE_ALARM -> {
                snoozeAlarmAndSelf(alarmId, destinationName)
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarmForeground(
        alarmId: Long,
        destinationName: String,
        ringtoneUriStr: String?,
        shouldVibrate: Boolean,
        enableVoice: Boolean = true,
        voiceStyle: Int = 0,
        distanceMeters: Float? = null,
        radiusMeters: Float? = null
    ) {
        // 1. Acquire wake lock to turn on screen and guarantee CPU execution
        WakeLockHelper.acquireWakeLock(this, "WakeMeThere:AlarmActiveWakeLock")

        // 2. Build high priority notification with FullScreenIntent
        val notification = NotificationHelper.buildAlarmNotification(
            this,
            alarmId,
            destinationName,
            distanceMeters,
            radiusMeters
        )

        // 3. Start foreground
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.ALARM_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NotificationHelper.ALARM_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm foreground service", e)
        }

        // 4. Request Audio Focus
        requestAudioFocus()

        // 5. Play ringtone on USAGE_ALARM stream
        playRingtone(ringtoneUriStr)

        // 6. Natural Voice Spoken Announcement (with audio ducking over ringtone)
        if (enableVoice) {
            voiceAnnouncementHelper?.shutdown()
            voiceAnnouncementHelper = VoiceAnnouncementHelper(this) { isSpeaking ->
                // Smooth audio ducking: lower ringtone to 18% when voice is announcing, restore to 100% when done
                val volume = if (isSpeaking) 0.18f else 1.0f
                mediaPlayer?.setVolume(volume, volume)
            }
            voiceAnnouncementHelper?.startRepeatingAnnouncement(
                destinationName = destinationName,
                style = voiceStyle,
                intervalSeconds = 9L
            )
        }

        // 7. Start vibration if enabled
        if (shouldVibrate) {
            startVibration()
        }

        // 8. Update database record (mark triggered & disable if one-shot)
        if (alarmId > 0) {
            serviceScope.launch {
                val app = application as? WakeMeThereApp
                app?.repository?.markAlarmTriggered(alarmId)
            }
        }

        // 9. Launch ActiveAlarmActivity directly as fallback
        val activityIntent = Intent(this, ActiveAlarmActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ActiveAlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(ActiveAlarmActivity.EXTRA_DESTINATION_NAME, destinationName)
            if (distanceMeters != null) {
                putExtra(ActiveAlarmActivity.EXTRA_DISTANCE_METERS, distanceMeters)
            }
            putExtra(ActiveAlarmActivity.EXTRA_RADIUS_METERS, radiusMeters)
        }
        startActivity(activityIntent)
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: return
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { /* maintain alarm */ }
                .build()
            audioFocusRequest?.let { am.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun playRingtone(ringtoneUriStr: String?) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            val targetUri = ringtoneUriStr?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmPlayerService, targetUri)
                setAudioAttributes(audioAttributes)
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "MediaPlayer playing alarm stream successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm audio, trying fallback beep", e)
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmPlayerService, fallbackUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback MediaPlayer also failed", ex)
            }
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 800, 400, 800, 400, 1200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    private fun stopAlarmAndSelf(
        alarmId: Long = -1L,
        destinationName: String = "",
        isMinimized: Boolean = false,
        disableInDb: Boolean = true
    ) {
        Log.d(TAG, "Stopping alarm (alarmId=$alarmId, isMinimized=$isMinimized)...")
        try {
            voiceAnnouncementHelper?.shutdown()
            voiceAnnouncementHelper = null

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            vibrator?.cancel()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }

            WakeLockHelper.releaseWakeLock()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)

            // Explicitly disable alarm in Room database so it is guaranteed turned off
            val targetId = if (alarmId > 0) alarmId else currentAlarmId
            if (disableInDb && targetId > 0) {
                serviceScope.launch {
                    val app = application as? WakeMeThereApp
                    app?.repository?.setAlarmEnabled(targetId, false)
                    Log.i(TAG, "Alarm $targetId disabled in database upon alarm stop")
                }
            }

            // Post dismissal notification to clearly reassure the user that alarm is disabled
            if (destinationName.isNotBlank() && destinationName != "Destination") {
                NotificationHelper.showAlarmDismissedNotification(this, destinationName, isMinimized)
            }

            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm service", e)
        }
    }

    private fun snoozeAlarmAndSelf(alarmId: Long, destinationName: String) {
        Log.d(TAG, "Snoozing alarm $alarmId for 5 minutes...")
        scheduleSnoozeWithAlarmManager(alarmId, destinationName)
        // Stop audio & vibration now without disabling in database so it can ring after 5 mins
        stopAlarmAndSelf(alarmId = alarmId, destinationName = destinationName, disableInDb = false)
    }

    private fun scheduleSnoozeWithAlarmManager(alarmId: Long, destinationName: String) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
                action = AlarmActionReceiver.ACTION_START_SNOOZED_ALARM
                putExtra(AlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmActionReceiver.EXTRA_DESTINATION_NAME, destinationName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId.toInt(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAtMillis = System.currentTimeMillis() + 5 * 60 * 1000L // 5 minutes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d(TAG, "Snooze AlarmManager scheduled for $triggerAtMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze via AlarmManager", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        voiceAnnouncementHelper?.shutdown()
        voiceAnnouncementHelper = null
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        WakeLockHelper.releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmPlayerService"
        const val ACTION_START_ALARM = "com.wakemethere.app.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.wakemethere.app.action.STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.wakemethere.app.action.SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_DESTINATION_NAME = "extra_destination_name"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_ENABLE_VOICE = "extra_enable_voice"
        const val EXTRA_VOICE_STYLE = "extra_voice_style"
        const val EXTRA_DISTANCE_METERS = "extra_distance_meters"
        const val EXTRA_RADIUS_METERS = "extra_radius_meters"
        const val EXTRA_IS_MINIMIZED = "extra_is_minimized"
    }
}

