package com.wakemethere.app.ui.navigation

import java.net.URLEncoder
import java.util.Locale

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object MapPicker : Screen("map_picker")
    data object AlarmConfig : Screen("alarm_config/{lat}/{lng}/{radius}?name={name}") {
        fun createRoute(lat: Double, lng: Double, radius: Float, name: String = ""): String {
            val encodedName = try {
                URLEncoder.encode(name, "UTF-8")
            } catch (e: Exception) {
                ""
            }
            return String.format(Locale.US, "alarm_config/%.6f/%.6f/%.1f?name=%s", lat, lng, radius, encodedName)
        }
    }
    data object EditAlarm : Screen("edit_alarm/{alarmId}") {
        fun createRoute(alarmId: Long): String = "edit_alarm/$alarmId"
    }
}
