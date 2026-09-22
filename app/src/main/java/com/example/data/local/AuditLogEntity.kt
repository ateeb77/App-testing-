package com.example.data.local

import androidx.room.*
import com.example.data.model.AuditLog
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val incidentId: String,
    val actor: String,
    val action: String,
    val timestamp: Long,
    val details: String
) {
    fun toAuditLog(): AuditLog = AuditLog(
        id = id,
        incidentId = incidentId,
        actor = actor,
        action = action,
        timestamp = timestamp,
        details = details
    )

    companion object {
        fun fromAuditLog(log: AuditLog): AuditLogEntity = AuditLogEntity(
            id = log.id,
            incidentId = log.incidentId,
            actor = log.actor,
            action = log.action,
            timestamp = log.timestamp,
            details = log.details
        )
    }
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAll()
}
