package com.wakemethere.app.util

import android.location.Location
import java.util.Locale

object DistanceCalculator {

    /**
     * Calculates the geodesic distance between two points in meters.
     */
    fun calculateDistanceMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }

    /**
     * Nicely formats distance in meters or kilometers (compact).
     */
    fun formatDistance(meters: Float): String {
        return if (meters < 1000f) {
            String.format(Locale.getDefault(), "%d m", meters.toInt())
        } else {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
        }
    }

    /**
     * Formats distance showing both kilometers and meters with high clarity
     * (e.g., "2.40 km (2,400 m)" or "450 m").
     */
    fun formatDistanceDetailed(meters: Float): String {
        return if (meters < 1000f) {
            String.format(Locale.getDefault(), "%d m", meters.toInt())
        } else {
            String.format(Locale.getDefault(), "%.2f km (%,d m)", meters / 1000f, meters.toInt())
        }
    }
}

