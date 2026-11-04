package com.wakemethere.app.data.repository

import com.wakemethere.app.data.local.AlarmDao
import com.wakemethere.app.data.model.LocationAlarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepositoryImpl(
    private val dao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<LocationAlarm>> {
        return dao.getAllAlarmsFlow().map { list ->
            list.map { LocationAlarm.fromEntity(it) }
        }
    }

    override fun getActiveAlarms(): Flow<List<LocationAlarm>> {
        return dao.getActiveAlarmsFlow().map { list ->
            list.map { LocationAlarm.fromEntity(it) }
        }
    }

    override suspend fun getActiveAlarmsSync(): List<LocationAlarm> {
        return dao.getActiveAlarmsSync().map { LocationAlarm.fromEntity(it) }
    }

    override suspend fun getAlarmById(id: Long): LocationAlarm? {
        return dao.getAlarmById(id)?.let { LocationAlarm.fromEntity(it) }
    }

    override suspend fun saveAlarm(alarm: LocationAlarm): Long {
        return dao.insertAlarm(alarm.toEntity())
    }

    override suspend fun updateAlarm(alarm: LocationAlarm) {
        dao.updateAlarm(alarm.toEntity())
    }

    override suspend fun deleteAlarm(alarm: LocationAlarm) {
        dao.deleteAlarm(alarm.toEntity())
    }

    override suspend fun deleteAlarmById(id: Long) {
        dao.deleteAlarmById(id)
    }

    override suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean) {
        dao.setAlarmEnabled(id, isEnabled)
    }

    override suspend fun setAlarmFavorite(id: Long, isFavorite: Boolean) {
        dao.setAlarmFavorite(id, isFavorite)
    }

    override suspend fun markAlarmTriggered(id: Long) {
        dao.updateLastTriggered(id, System.currentTimeMillis())
        val alarm = dao.getAlarmById(id)
        if (alarm != null && alarm.isOneShot) {
            // Turn off one-shot alarm automatically after trigger
            dao.setAlarmEnabled(id, false)
        }
    }
}

