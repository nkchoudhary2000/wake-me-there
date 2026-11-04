package com.wakemethere.app.data.repository

import com.wakemethere.app.data.model.LocationAlarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<LocationAlarm>>
    fun getActiveAlarms(): Flow<List<LocationAlarm>>
    suspend fun getActiveAlarmsSync(): List<LocationAlarm>
    suspend fun getAlarmById(id: Long): LocationAlarm?
    suspend fun saveAlarm(alarm: LocationAlarm): Long
    suspend fun updateAlarm(alarm: LocationAlarm)
    suspend fun deleteAlarm(alarm: LocationAlarm)
    suspend fun deleteAlarmById(id: Long)
    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean)
    suspend fun setAlarmFavorite(id: Long, isFavorite: Boolean)
    suspend fun markAlarmTriggered(id: Long)
}

