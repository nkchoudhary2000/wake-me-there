package com.wakemethere.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Predefined location categories for frequent destinations (Home, Work, Bus Stop, etc.)
 * allowing one-tap commute planning and smart radius defaults.
 */
enum class AlarmCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val defaultRadiusMeters: Float,
    val defaultVoiceStyle: Int
) {
    BUS_STOP("BUS_STOP", "Bus Stop", "🚌", 500f, 1),
    METRO("METRO", "Metro / Train", "🚆", 600f, 1),
    HOME("HOME", "Home", "🏠", 800f, 0),
    WORK("WORK", "Work / Office", "🏢", 800f, 0),
    COLLEGE("COLLEGE", "College", "🎓", 700f, 0),
    OTHER("OTHER", "Custom Place", "📍", 500f, 0);

    fun getIcon(): ImageVector {
        return when (this) {
            BUS_STOP -> Icons.Default.DirectionsBus
            METRO -> Icons.Default.DirectionsSubway
            HOME -> Icons.Default.Home
            WORK -> Icons.Default.Work
            COLLEGE -> Icons.Default.School
            OTHER -> Icons.Default.Place
        }
    }

    companion object {
        fun fromId(id: String?): AlarmCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}

