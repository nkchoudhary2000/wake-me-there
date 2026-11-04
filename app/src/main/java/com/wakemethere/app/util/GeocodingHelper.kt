package com.wakemethere.app.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import com.wakemethere.app.data.model.SearchResultPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object GeocodingHelper {

    /**
     * Resolves human-readable area name from latitude and longitude (Reverse Geocoding).
     * e.g., "Marathahalli, Bengaluru"
     */
    suspend fun getAddressFromLocation(
        context: Context,
        lat: Double,
        lng: Double
    ): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            continuation.resume(formatAddress(addr))
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(addresses?.firstOrNull())
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Searches for candidate places and areas by name (Forward Geocoding).
     * e.g., "Marathahalli", "Whitefield", "Indiranagar"
     */
    suspend fun searchPlacesByName(
        context: Context,
        query: String
    ): List<SearchResultPlace> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || !Geocoder.isPresent()) return@withContext emptyList()

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(cleanQuery, 6, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val results = addresses.mapNotNull { addressToSearchResult(it) }
                            continuation.resume(results)
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(emptyList())
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(cleanQuery, 6)
                addresses?.mapNotNull { addressToSearchResult(it) } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun addressToSearchResult(address: Address): SearchResultPlace? {
        val lat = address.latitude
        val lng = address.longitude

        val primaryName = address.featureName
            ?: address.subLocality
            ?: address.locality
            ?: "Location"

        val secondaryParts = mutableListOf<String>()
        if (!address.subLocality.isNullOrBlank() && address.subLocality != primaryName) {
            secondaryParts.add(address.subLocality)
        }
        if (!address.locality.isNullOrBlank() && address.locality != primaryName) {
            secondaryParts.add(address.locality)
        }
        if (!address.adminArea.isNullOrBlank() && address.adminArea != primaryName) {
            secondaryParts.add(address.adminArea)
        }
        if (!address.countryName.isNullOrBlank()) {
            secondaryParts.add(address.countryName)
        }

        val secondaryText = if (secondaryParts.isNotEmpty()) {
            secondaryParts.joinToString(", ")
        } else {
            String.format(Locale.US, "%.4f, %.4f", lat, lng)
        }

        return SearchResultPlace(
            displayName = primaryName,
            secondaryText = secondaryText,
            latLng = LatLng(lat, lng)
        )
    }

    private fun formatAddress(address: Address?): String? {
        if (address == null) return null
        val subLocality = address.subLocality
        val locality = address.locality ?: address.subAdminArea
        val feature = address.featureName

        return when {
            !subLocality.isNullOrBlank() && !locality.isNullOrBlank() -> "$subLocality, $locality"
            !subLocality.isNullOrBlank() -> subLocality
            !locality.isNullOrBlank() -> locality
            !feature.isNullOrBlank() -> feature
            else -> address.getAddressLine(0)
        }
    }
}

