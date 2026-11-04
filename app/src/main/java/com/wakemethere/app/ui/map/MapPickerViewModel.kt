package com.wakemethere.app.ui.map

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.wakemethere.app.data.model.SearchResultPlace
import com.wakemethere.app.util.GeocodingHelper
import com.wakemethere.app.util.PermissionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    // Default fallback coordinate (Googleplex)
    private val defaultCoordinates = LatLng(12.9569, 77.7011) // Marathahalli, Bengaluru coordinate as sensible regional default

    private val _selectedLatLng = MutableStateFlow(defaultCoordinates)
    val selectedLatLng: StateFlow<LatLng> = _selectedLatLng.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _currentAreaName = MutableStateFlow<String?>(null)
    val currentAreaName: StateFlow<String?> = _currentAreaName.asStateFlow()

    private val _selectedAreaName = MutableStateFlow("Target Location")
    val selectedAreaName: StateFlow<String> = _selectedAreaName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultPlace>>(emptyList())
    val searchResults: StateFlow<List<SearchResultPlace>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _radiusMeters = MutableStateFlow(500f)
    val radiusMeters: StateFlow<Float> = _radiusMeters.asStateFlow()

    private val _isOfflineFallbackMode = MutableStateFlow(false)
    val isOfflineFallbackMode: StateFlow<Boolean> = _isOfflineFallbackMode.asStateFlow()

    private var searchJob: Job? = null

    init {
        fetchCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val context = getApplication<Application>()
        if (!PermissionUtils.hasFineLocationPermission(context)) return

        viewModelScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    _currentLocation.value = latLng

                    // Reverse geocode to get current area name (e.g., "Marathahalli, Bengaluru")
                    val areaName = GeocodingHelper.getAddressFromLocation(
                        context,
                        location.latitude,
                        location.longitude
                    )
                    _currentAreaName.value = areaName

                    // If user hasn't chosen a different destination yet, default to current location
                    if (_selectedAreaName.value == "Target Location" && !areaName.isNullOrBlank()) {
                        _selectedLatLng.value = latLng
                        _selectedAreaName.value = areaName
                    }
                }
            } catch (e: Exception) {
                // Keep defaults
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()

        if (newQuery.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            // Debounce for 350ms to avoid excessive geocoding calls while typing
            delay(350)
            val context = getApplication<Application>()
            val results = GeocodingHelper.searchPlacesByName(context, newQuery)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun performSearch(query: String) {
        searchJob?.cancel()
        if (query.trim().isBlank()) return

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            val context = getApplication<Application>()
            val results = GeocodingHelper.searchPlacesByName(context, query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun selectPlace(place: SearchResultPlace) {
        _selectedLatLng.value = place.latLng
        _selectedAreaName.value = place.displayName
        _searchQuery.value = place.displayName
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun setLocation(latLng: LatLng, areaName: String? = null) {
        _selectedLatLng.value = latLng
        if (areaName != null) {
            _selectedAreaName.value = areaName
        } else {
            // Reverse geocode asynchronously
            viewModelScope.launch {
                val context = getApplication<Application>()
                val resolvedName = GeocodingHelper.getAddressFromLocation(
                    context,
                    latLng.latitude,
                    latLng.longitude
                )
                if (!resolvedName.isNullOrBlank()) {
                    _selectedAreaName.value = resolvedName
                }
            }
        }
    }

    fun setRadius(radius: Float) {
        _radiusMeters.value = radius.coerceIn(50f, 5000f)
    }

    fun toggleOfflineFallbackMode() {
        _isOfflineFallbackMode.value = !_isOfflineFallbackMode.value
    }
}
