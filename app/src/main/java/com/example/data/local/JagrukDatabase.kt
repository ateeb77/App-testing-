package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IncidentEntity::class, AuditLogEntity::class], version = 1, exportSchema = false)
abstract class JagrukDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: JagrukDatabase? = null

        fun getInstance(context: Context): JagrukDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JagrukDatabase::class.java,
                    "jagruk_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
