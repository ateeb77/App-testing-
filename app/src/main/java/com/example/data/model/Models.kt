package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.squareup.moshi.JsonClass

enum class HazardType(
    val displayName: String,
    val emoji: String,
    val defaultRadiusMeters: Double,
    val defaultAction: String,
    val expiryMinutes: Long
) {
    FLASH_FLOOD(
        displayName = "Flash Flood",
        emoji = "🌊",
        defaultRadiusMeters = 1000.0,
        defaultAction = "Move toward higher ground immediately. Avoid flooded roads, bridges and underpasses.",
        expiryMinutes = 120
    ),
    DANGEROUS_WILDLIFE(
        displayName = "Dangerous Wildlife",
        emoji = "🐘",
        defaultRadiusMeters = 500.0,
        defaultAction = "Stay indoors or inside vehicles. Do not approach or provoke the animal. Maintain safe distance.",
        expiryMinutes = 30
    ),
    DANGEROUS_ANIMAL(
        displayName = "Dangerous Animal",
        emoji = "🐅",
        defaultRadiusMeters = 500.0,
        defaultAction = "Alert neighbors, secure pets and livestock, and remain in safe shelter.",
        expiryMinutes = 30
    ),
    FIRE(
        displayName = "Fire",
        emoji = "🔥",
        defaultRadiusMeters = 1000.0,
        defaultAction = "Evacuate upwind. Stay low if smoke is present. Call Fire Department (101).",
        expiryMinutes = 120
    ),
    ROAD_ACCIDENT(
        displayName = "Road Accident",
        emoji = "🚗",
        defaultRadiusMeters = 250.0,
        defaultAction = "Slow down and proceed with caution. Clear way for emergency responders.",
        expiryMinutes = 60
    ),
    STRUCTURAL_HAZARD(
        displayName = "Structural Hazard",
        emoji = "🏚️",
        defaultRadiusMeters = 300.0,
        defaultAction = "Evacuate compromised structures immediately. Keep away from falling debris.",
        expiryMinutes = 120
    ),
    ELECTRICAL_HAZARD(
        displayName = "Electrical Hazard",
        emoji = "⚡",
        defaultRadiusMeters = 200.0,
        defaultAction = "Do not touch fallen wires or standing water nearby. Maintain at least 10m distance.",
        expiryMinutes = 90
    ),
    HAZARDOUS_MATERIAL(
        displayName = "Hazardous Material",
        emoji = "☣️",
        defaultRadiusMeters = 1500.0,
        defaultAction = "Shelter in place, close windows and turn off ventilation. Cover mouth and nose.",
        expiryMinutes = 180
    ),
    OTHER(
        displayName = "Other Hazard",
        emoji = "⚠️",
        defaultRadiusMeters = 500.0,
        defaultAction = "Exercise extreme caution and stay aware of your surroundings.",
        expiryMinutes = 60
    );

    companion object {
        fun fromString(value: String): HazardType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class Severity(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4);

    companion object {
        fun fromString(value: String): Severity {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class Confidence(val label: String, val colorHex: Long) {
    LOW("Low Confidence", 0xFFEAB308),
    MEDIUM("Medium Confidence", 0xFFF97316),
    HIGH("High Confidence", 0xFFEF4444),
    VERIFIED("Verified by Admin", 0xFF2563EB);

    companion object {
        fun fromString(value: String): Confidence {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LOW
        }
    }
}

enum class IncidentStatus {
    ACTIVE,
    UNDER_REVIEW,
    VERIFIED,
    DISMISSED,
    RESOLVED;

    companion object {
        fun fromString(value: String): IncidentStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

enum class ReportSource {
    COMMUNITY,
    OFFICIAL,
    ADMIN,
    AI_ASSISTED;

    companion object {
        fun fromString(value: String): ReportSource {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COMMUNITY
        }
    }
}

enum class UserRole(val title: String, val description: String) {
    REPORTER("Reporter", "Can report hazards and submit evidence"),
    RESIDENT("Resident", "Receives hyperlocal alerts inside danger zones"),
    ADMIN("Admin", "Full Command Center verification and radius controls")
}

@JsonClass(generateAdapter = true)
data class Incident(
    val id: String,
    val reporterId: String,
    val hazardType: HazardType,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val severity: Severity,
    val confidence: Confidence,
    val status: IncidentStatus,
    val source: ReportSource,
    val imageUrl: String? = null,
    val aiAnalysis: String? = null,
    val corroboratingReports: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (hazardType.expiryMinutes * 60 * 1000)
)

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val type: String, // "UPSERT", "STATUS_CHANGE", "CORROBORATE", "ACK", "RESET"
    val incident: Incident? = null,
    val incidentId: String? = null,
    val newStatus: String? = null,
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SafetyAlert(
    val incident: Incident,
    val distanceMeters: Double,
    val isInsideDangerZone: Boolean,
    val acknowledged: Boolean = false
)

data class AuditLog(
    val id: String,
    val incidentId: String,
    val actor: String,
    val action: String,
    val timestamp: Long,
    val details: String
)

data class DemoLocation(
    val label: String,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val REPORTER_DEFAULT = DemoLocation("Demo Location A (Reporter)", 21.1458, 79.0882)
        val RESIDENT_DEFAULT = DemoLocation("Demo Location B (Resident inside ~420m)", 21.1480, 79.0900)
        val OUTSIDE_DEFAULT = DemoLocation("Demo Location C (Outside Zone ~4.7km)", 21.1800, 79.1200)
    }
}
