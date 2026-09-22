package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DemoLocation
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jagruk_prefs", Context.MODE_PRIVATE)

    private val _userRole = MutableStateFlow(
        UserRole.valueOf(prefs.getString("user_role", UserRole.RESIDENT.name) ?: UserRole.RESIDENT.name)
    )
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _isDemoMode = MutableStateFlow(prefs.getBoolean("is_demo_mode", true))
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _latitude = MutableStateFlow(
        prefs.getFloat("latitude", DemoLocation.RESIDENT_DEFAULT.latitude.toFloat()).toDouble()
    )
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(
        prefs.getFloat("longitude", DemoLocation.RESIDENT_DEFAULT.longitude.toFloat()).toDouble()
    )
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _locationLabel = MutableStateFlow(
        prefs.getString("location_label", DemoLocation.RESIDENT_DEFAULT.label) ?: DemoLocation.RESIDENT_DEFAULT.label
    )
    val locationLabel: StateFlow<String> = _locationLabel.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound_enabled", true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrateEnabled = MutableStateFlow(prefs.getBoolean("vibrate_enabled", true))
    val vibrateEnabled: StateFlow<Boolean> = _vibrateEnabled.asStateFlow()

    private val _syncChannel = MutableStateFlow(
        prefs.getString("sync_channel", "jagruk_national_mesh_c3c9") ?: "jagruk_national_mesh_c3c9"
    )
    val syncChannel: StateFlow<String> = _syncChannel.asStateFlow()

    private val _trustLevel = MutableStateFlow(prefs.getInt("trust_level", 88))
    val trustLevel: StateFlow<Int> = _trustLevel.asStateFlow()

    fun setUserRole(role: UserRole) {
        prefs.edit().putString("user_role", role.name).apply()
        _userRole.value = role
        // Also update default location if in demo mode
        if (_isDemoMode.value) {
            when (role) {
                UserRole.REPORTER -> setLocation(DemoLocation.REPORTER_DEFAULT)
                UserRole.RESIDENT -> setLocation(DemoLocation.RESIDENT_DEFAULT)
                UserRole.ADMIN -> Unit
            }
        }
    }

    fun setDemoMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_demo_mode", enabled).apply()
        _isDemoMode.value = enabled
    }

    fun setLocation(location: DemoLocation) {
        prefs.edit()
            .putFloat("latitude", location.latitude.toFloat())
            .putFloat("longitude", location.longitude.toFloat())
            .putString("location_label", location.label)
            .apply()
        _latitude.value = location.latitude
        _longitude.value = location.longitude
        _locationLabel.value = location.label
    }

    fun setCustomCoordinates(lat: Double, lon: Double, label: String = "Custom GPS") {
        prefs.edit()
            .putFloat("latitude", lat.toFloat())
            .putFloat("longitude", lon.toFloat())
            .putString("location_label", label)
            .apply()
        _latitude.value = lat
        _longitude.value = lon
        _locationLabel.value = label
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _soundEnabled.value = enabled
    }

    fun setVibrateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibrate_enabled", enabled).apply()
        _vibrateEnabled.value = enabled
    }

    fun setSyncChannel(channel: String) {
        val clean = channel.trim().lowercase().replace(" ", "_")
        prefs.edit().putString("sync_channel", clean).apply()
        _syncChannel.value = clean
    }

    fun incrementTrust() {
        val newTrust = (_trustLevel.value + 3).coerceAtMost(100)
        prefs.edit().putInt("trust_level", newTrust).apply()
        _trustLevel.value = newTrust
    }
}
