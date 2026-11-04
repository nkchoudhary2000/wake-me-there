package com.wakemethere.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a saved location-based alarm.
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val isEnabled: Boolean = true,
    val ringtoneUri: String,
    val ringtoneTitle: String,
    val vibrate: Boolean = true,
    val isOneShot: Boolean = true,
    val enableVoiceAnnouncement: Boolean = true,
    val voiceAnnouncementStyle: Int = 0,
    val category: String = "OTHER",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null
)

