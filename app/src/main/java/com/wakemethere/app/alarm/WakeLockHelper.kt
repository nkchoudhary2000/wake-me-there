package com.wakemethere.app.alarm

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.util.Log

object WakeLockHelper {

    private const val TAG = "WakeLockHelper"
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Acquires a wake lock that forces the screen to turn on and keeps the CPU running.
     * Includes a 10-minute timeout safeguard to avoid infinite battery drain if left unattended.
     */
    @SuppressLint("WakelockTimeout")
    fun acquireWakeLock(context: Context, tag: String = "WakeMeThere:AlarmWakeLock") {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                tag
            ).apply {
                setReferenceCounted(false)
                // 10 minutes max safeguard
                acquire(10 * 60 * 1000L)
            }
            Log.d(TAG, "WakeLock acquired successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }
}

