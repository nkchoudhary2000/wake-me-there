package com.wakemethere.app.data.model

import com.wakemethere.app.data.local.AlarmEntity

/**
 * Domain model for a location-based alarm.
 */
data class LocationAlarm(
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
) {
    val alarmCategory: AlarmCategory
        get() = AlarmCategory.fromId(category)

    fun toEntity(): AlarmEntity = AlarmEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        isEnabled = isEnabled,
        ringtoneUri = ringtoneUri,
        ringtoneTitle = ringtoneTitle,
        vibrate = vibrate,
        isOneShot = isOneShot,
        enableVoiceAnnouncement = enableVoiceAnnouncement,
        voiceAnnouncementStyle = voiceAnnouncementStyle,
        category = category,
        isFavorite = isFavorite,
        createdAt = createdAt,
        lastTriggeredAt = lastTriggeredAt
    )

    companion object {
        fun fromEntity(entity: AlarmEntity): LocationAlarm = LocationAlarm(
            id = entity.id,
            name = entity.name,
            latitude = entity.latitude,
            longitude = entity.longitude,
            radiusMeters = entity.radiusMeters,
            isEnabled = entity.isEnabled,
            ringtoneUri = entity.ringtoneUri,
            ringtoneTitle = entity.ringtoneTitle,
            vibrate = entity.vibrate,
            isOneShot = entity.isOneShot,
            enableVoiceAnnouncement = entity.enableVoiceAnnouncement,
            voiceAnnouncementStyle = entity.voiceAnnouncementStyle,
            category = entity.category,
            isFavorite = entity.isFavorite,
            createdAt = entity.createdAt,
            lastTriggeredAt = entity.lastTriggeredAt
        )
    }
}

