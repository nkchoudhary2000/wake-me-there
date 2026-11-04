package com.wakemethere.app.data.model

import com.google.android.gms.maps.model.LatLng

/**
 * Data model for geocoded location search results.
 */
data class SearchResultPlace(
    val displayName: String,
    val secondaryText: String,
    val latLng: LatLng
)

