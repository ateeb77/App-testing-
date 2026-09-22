package com.example.data.repository

import com.example.data.local.AuditLogDao
import com.example.data.local.AuditLogEntity
import com.example.data.local.IncidentDao
import com.example.data.local.IncidentEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.*
import com.example.data.remote.GeminiService
import com.example.data.remote.RealtimeMeshManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class IncidentRepository(
    private val incidentDao: IncidentDao,
    private val auditLogDao: AuditLogDao,
    private val preferencesManager: PreferencesManager,
    private val meshManager: RealtimeMeshManager
) {
    val allIncidents: Flow<List<Incident>> = incidentDao.getAllIncidents().map { list ->
        list.map { it.toIncident() }
    }

    val activeIncidents: Flow<List<Incident>> = incidentDao.getActiveIncidents().map { list ->
        list.map { it.toIncident() }
    }

    val auditLogs: Flow<List<AuditLog>> = auditLogDao.getAllLogs().map { list ->
        list.map { it.toAuditLog() }
    }

    suspend fun createIncident(
        hazardType: HazardType,
        description: String,
        latitude: Double,
        longitude: Double,
        severity: Severity,
        imageUrl: String? = null
    ): Incident {
        val analysis = GeminiService.analyzeReport(hazardType, description, imageUrl != null)

        val incidentId = "inc_" + UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()

        val incident = Incident(
            id = incidentId,
            reporterId = "reporter_${preferencesManager.userRole.value.name.lowercase()}",
            hazardType = hazardType,
            description = description.ifBlank { "Citizen report: ${hazardType.displayName} sighted near current coordinates." },
            latitude = latitude,
            longitude = longitude,
            radiusMeters = hazardType.defaultRadiusMeters,
            severity = severity,
            confidence = Confidence.LOW,
            status = IncidentStatus.ACTIVE,
            source = ReportSource.COMMUNITY,
            imageUrl = imageUrl,
            aiAnalysis = "Visual Consistency: ${analysis.visualConsistency}. Urgency: ${analysis.urgencyScore}/10. ${analysis.explanation}",
            corroboratingReports = 1,
            createdAt = now,
            updatedAt = now,
            expiresAt = now + (hazardType.expiryMinutes * 60 * 1000)
        )

        incidentDao.insertOrUpdate(IncidentEntity.fromIncident(incident))
        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                actor = preferencesManager.userRole.value.name,
                action = "CREATED_INCIDENT",
                timestamp = now,
                details = "Created ${hazardType.displayName} at ($latitude, $longitude)"
            )
        )

        // Broadcast to other phones over the internet mesh
        meshManager.broadcastPayload(
            SyncPayload(
                type = "UPSERT",
                incident = incident,
                senderId = incident.reporterId,
                timestamp = now
            )
        )

        return incident
    }

    suspend fun corroborateIncident(incidentId: String) {
        val incidentEntity = incidentDao.getIncidentById(incidentId) ?: return
        val currentCount = incidentEntity.corroboratingReports + 1
        val newConfidence = when {
            currentCount >= 3 -> Confidence.HIGH
            currentCount >= 2 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }
        val now = System.currentTimeMillis()

        incidentDao.addCorroboration(incidentId, newConfidence.name, now)
        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                actor = preferencesManager.userRole.value.name,
                action = "CORROBORATED",
                timestamp = now,
                details = "Corroborated incident. Count now $currentCount ($newConfidence)"
            )
        )

        meshManager.broadcastPayload(
            SyncPayload(
                type = "CORROBORATE",
                incidentId = incidentId,
                senderId = preferencesManager.userRole.value.name,
                timestamp = now
            )
        )
    }

    suspend fun updateStatus(incidentId: String, newStatus: IncidentStatus, notes: String = "") {
        val now = System.currentTimeMillis()
        incidentDao.updateStatus(incidentId, newStatus.name, now)

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                actor = preferencesManager.userRole.value.name,
                action = "STATUS_UPDATE_${newStatus.name}",
                timestamp = now,
                details = notes.ifBlank { "Status updated to ${newStatus.name}" }
            )
        )

        meshManager.broadcastPayload(
            SyncPayload(
                type = "STATUS_CHANGE",
                incidentId = incidentId,
                newStatus = newStatus.name,
                senderId = preferencesManager.userRole.value.name,
                timestamp = now
            )
        )
    }

    suspend fun updateRadius(incidentId: String, newRadiusMeters: Double) {
        val now = System.currentTimeMillis()
        incidentDao.updateRadius(incidentId, newRadiusMeters, now)

        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                actor = preferencesManager.userRole.value.name,
                action = "RADIUS_MODIFIED",
                timestamp = now,
                details = "Danger radius updated to ${newRadiusMeters.toInt()}m"
            )
        )

        val updated = incidentDao.getIncidentById(incidentId)?.toIncident()
        if (updated != null) {
            meshManager.broadcastPayload(
                SyncPayload(
                    type = "UPSERT",
                    incident = updated,
                    senderId = preferencesManager.userRole.value.name,
                    timestamp = now
                )
            )
        }
    }

    suspend fun reportMistake(incidentId: String) {
        updateStatus(incidentId, IncidentStatus.UNDER_REVIEW, "Reporter flagged: 'I reported this by mistake'")
    }

    /**
     * Built-in Hackathon Demo Scenario: "FLASH FLOOD — LIVE DEMO"
     */
    suspend fun triggerFlashFloodDemoScenario() {
        val now = System.currentTimeMillis()
        val incidentId = "demo_flood_" + now.toString().takeLast(4)

        val demoFlood = Incident(
            id = incidentId,
            reporterId = "reporter_demo_a",
            hazardType = HazardType.FLASH_FLOOD,
            description = "Rapid water accumulation covering road network near XYZ corridor. Low-lying vehicles submerged. Level rising.",
            latitude = DemoLocation.REPORTER_DEFAULT.latitude,
            longitude = DemoLocation.REPORTER_DEFAULT.longitude,
            radiusMeters = 1000.0,
            severity = Severity.HIGH,
            confidence = Confidence.HIGH,
            status = IncidentStatus.ACTIVE,
            source = ReportSource.COMMUNITY,
            imageUrl = null,
            aiAnalysis = "Visual Consistency: HIGH. Urgency: 9/10. Multiple independent inputs corroborating severe road submersion.",
            corroboratingReports = 3,
            createdAt = now,
            updatedAt = now,
            expiresAt = now + (120 * 60 * 1000)
        )

        incidentDao.insertOrUpdate(IncidentEntity.fromIncident(demoFlood))
        auditLogDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                incidentId = incidentId,
                actor = "DEMO_SCENARIO",
                action = "FLASH_FLOOD_LIVE_DEMO_LAUNCHED",
                timestamp = now,
                details = "Demo Scenario: Flood created at (21.1458, 79.0882), radius 1000m"
            )
        )

        meshManager.broadcastPayload(
            SyncPayload(
                type = "UPSERT",
                incident = demoFlood,
                senderId = "reporter_demo_a",
                timestamp = now
            )
        )
    }

    suspend fun resetDemo() {
        incidentDao.clearAll()
        auditLogDao.clearAll()
        meshManager.broadcastPayload(
            SyncPayload(
                type = "RESET",
                senderId = preferencesManager.userRole.value.name,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
