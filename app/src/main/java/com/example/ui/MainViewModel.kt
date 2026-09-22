package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.JagrukDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.*
import com.example.data.remote.ConnectivityStatus
import com.example.data.remote.RealtimeMeshManager
import com.example.data.repository.IncidentRepository
import com.example.util.AlertSoundManager
import com.example.util.GeoUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserSafetyState(
    val isSafe: Boolean,
    val nearestIncident: Incident? = null,
    val distanceMeters: Double = 0.0,
    val isInsideDangerZone: Boolean = false
)

data class AdminStatistics(
    val activeIncidents: Int,
    val highRiskIncidents: Int,
    val affectedUsersCount: Int,
    val pendingVerification: Int,
    val resolvedIncidents: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val preferencesManager = PreferencesManager(application)
    private val database = JagrukDatabase.getInstance(application)
    val meshManager = RealtimeMeshManager(application, database.incidentDao(), preferencesManager)
    val repository = IncidentRepository(
        database.incidentDao(),
        database.auditLogDao(),
        preferencesManager,
        meshManager
    )

    val userRole: StateFlow<UserRole> = preferencesManager.userRole
    val isDemoMode: StateFlow<Boolean> = preferencesManager.isDemoMode
    val userLat: StateFlow<Double> = preferencesManager.latitude
    val userLon: StateFlow<Double> = preferencesManager.longitude
    val locationLabel: StateFlow<String> = preferencesManager.locationLabel
    val soundEnabled: StateFlow<Boolean> = preferencesManager.soundEnabled
    val vibrateEnabled: StateFlow<Boolean> = preferencesManager.vibrateEnabled
    val syncChannel: StateFlow<String> = preferencesManager.syncChannel
    val trustLevel: StateFlow<Int> = preferencesManager.trustLevel

    val connectivityStatus: StateFlow<ConnectivityStatus> = meshManager.connectivityStatus
    val criticalAlertIncident: StateFlow<Incident?> = meshManager.criticalAlertIncident

    val allIncidents: StateFlow<List<Incident>> = repository.allIncidents.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activeIncidents: StateFlow<List<Incident>> = repository.activeIncidents.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Reactive Safety Status based on current location and active incidents
    val safetyState: StateFlow<UserSafetyState> = combine(
        userLat,
        userLon,
        activeIncidents
    ) { lat, lon, incidents ->
        if (incidents.isEmpty()) {
            UserSafetyState(isSafe = true)
        } else {
            var minDistance = Double.MAX_VALUE
            var nearest: Incident? = null
            var insideAnyZone = false

            for (inc in incidents) {
                val dist = GeoUtils.calculateDistanceMeters(lat, lon, inc.latitude, inc.longitude)
                if (dist < minDistance) {
                    minDistance = dist
                    nearest = inc
                }
                if (dist <= inc.radiusMeters) {
                    insideAnyZone = true
                }
            }

            UserSafetyState(
                isSafe = !insideAnyZone,
                nearestIncident = nearest,
                distanceMeters = if (nearest != null) minDistance else 0.0,
                isInsideDangerZone = insideAnyZone
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserSafetyState(isSafe = true)
    )

    val adminStatistics: StateFlow<AdminStatistics> = allIncidents.map { list ->
        val active = list.filter { it.status == IncidentStatus.ACTIVE || it.status == IncidentStatus.VERIFIED }
        val highRisk = active.filter { it.severity == Severity.HIGH || it.severity == Severity.CRITICAL }
        val pending = list.filter { it.status == IncidentStatus.UNDER_REVIEW || it.confidence == Confidence.LOW }
        val resolved = list.filter { it.status == IncidentStatus.RESOLVED }

        AdminStatistics(
            activeIncidents = active.size,
            highRiskIncidents = highRisk.size,
            affectedUsersCount = if (active.isNotEmpty()) 2 else 0, // In demo: Resident is affected
            pendingVerification = pending.size,
            resolvedIncidents = resolved.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AdminStatistics(0, 0, 0, 0, 0)
    )

    fun setUserRole(role: UserRole) {
        preferencesManager.setUserRole(role)
    }

    fun setDemoLocation(location: DemoLocation) {
        preferencesManager.setLocation(location)
    }

    fun setCustomCoordinates(lat: Double, lon: Double, label: String) {
        preferencesManager.setCustomCoordinates(lat, lon, label)
    }

    fun toggleDemoMode(enabled: Boolean) {
        preferencesManager.setDemoMode(enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        preferencesManager.setSoundEnabled(enabled)
    }

    fun setVibrateEnabled(enabled: Boolean) {
        preferencesManager.setVibrateEnabled(enabled)
    }

    fun setSyncChannel(channel: String) {
        preferencesManager.setSyncChannel(channel)
        meshManager.startWebSocketListener()
    }

    fun submitReport(
        hazardType: HazardType,
        description: String,
        severity: Severity,
        imageUrl: String? = null,
        onSuccess: (Incident) -> Unit
    ) {
        viewModelScope.launch {
            val incident = repository.createIncident(
                hazardType = hazardType,
                description = description,
                latitude = userLat.value,
                longitude = userLon.value,
                severity = severity,
                imageUrl = imageUrl
            )
            preferencesManager.incrementTrust()
            onSuccess(incident)
        }
    }

    fun corroborateIncident(incidentId: String) {
        viewModelScope.launch {
            repository.corroborateIncident(incidentId)
        }
    }

    fun reportMistake(incidentId: String) {
        viewModelScope.launch {
            repository.reportMistake(incidentId)
        }
    }

    fun adminVerify(incidentId: String) {
        viewModelScope.launch {
            repository.updateStatus(incidentId, IncidentStatus.VERIFIED, "Verified by Jagruk Administrator")
        }
    }

    fun adminDismiss(incidentId: String) {
        viewModelScope.launch {
            repository.updateStatus(incidentId, IncidentStatus.DISMISSED, "Dismissed by Jagruk Administrator (Insufficient corroboration)")
        }
    }

    fun adminResolve(incidentId: String) {
        viewModelScope.launch {
            repository.updateStatus(incidentId, IncidentStatus.RESOLVED, "Hazard resolved by responders / authorities")
        }
    }

    fun adminUpdateRadius(incidentId: String, newRadiusMeters: Double) {
        viewModelScope.launch {
            repository.updateRadius(incidentId, newRadiusMeters)
        }
    }

    fun acknowledgeAlert(incidentId: String) {
        meshManager.acknowledgeAlert(incidentId)
    }

    fun dismissCriticalAlert() {
        meshManager.dismissCriticalAlert()
    }

    fun triggerFlashFloodDemo() {
        viewModelScope.launch {
            repository.triggerFlashFloodDemoScenario()
        }
    }

    fun resetDemo() {
        viewModelScope.launch {
            repository.resetDemo()
        }
    }

    fun testEmergencyChime() {
        AlertSoundManager.playEmergencyAlert(getApplication(), vibrateEnabled.value)
    }
}
