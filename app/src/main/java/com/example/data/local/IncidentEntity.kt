package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val reporterId: String,
    val hazardType: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val severity: String,
    val confidence: String,
    val status: String,
    val source: String,
    val imageUrl: String?,
    val aiAnalysis: String?,
    val corroboratingReports: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long,
    val acknowledgedByLocalUser: Boolean = false
) {
    fun toIncident(): Incident = Incident(
        id = id,
        reporterId = reporterId,
        hazardType = HazardType.fromString(hazardType),
        description = description,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        severity = Severity.fromString(severity),
        confidence = Confidence.fromString(confidence),
        status = IncidentStatus.fromString(status),
        source = ReportSource.fromString(source),
        imageUrl = imageUrl,
        aiAnalysis = aiAnalysis,
        corroboratingReports = corroboratingReports,
        createdAt = createdAt,
        updatedAt = updatedAt,
        expiresAt = expiresAt
    )

    companion object {
        fun fromIncident(incident: Incident, acknowledged: Boolean = false): IncidentEntity = IncidentEntity(
            id = incident.id,
            reporterId = incident.reporterId,
            hazardType = incident.hazardType.name,
            description = incident.description,
            latitude = incident.latitude,
            longitude = incident.longitude,
            radiusMeters = incident.radiusMeters,
            severity = incident.severity.name,
            confidence = incident.confidence.name,
            status = incident.status.name,
            source = incident.source.name,
            imageUrl = incident.imageUrl,
            aiAnalysis = incident.aiAnalysis,
            corroboratingReports = incident.corroboratingReports,
            createdAt = incident.createdAt,
            updatedAt = incident.updatedAt,
            expiresAt = incident.expiresAt,
            acknowledgedByLocalUser = acknowledged
        )
    }
}

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE status = 'ACTIVE' OR status = 'VERIFIED' OR status = 'UNDER_REVIEW' ORDER BY createdAt DESC")
    fun getActiveIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getIncidentById(id: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(incident: IncidentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(incidents: List<IncidentEntity>)

    @Query("UPDATE incidents SET status = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, timestamp: Long)

    @Query("UPDATE incidents SET radiusMeters = :radius, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateRadius(id: String, radius: Double, timestamp: Long)

    @Query("UPDATE incidents SET corroboratingReports = corroboratingReports + 1, confidence = :confidence, updatedAt = :timestamp WHERE id = :id")
    suspend fun addCorroboration(id: String, confidence: String, timestamp: Long)

    @Query("UPDATE incidents SET acknowledgedByLocalUser = 1 WHERE id = :id")
    suspend fun markAcknowledged(id: String)

    @Query("DELETE FROM incidents")
    suspend fun clearAll()
}
