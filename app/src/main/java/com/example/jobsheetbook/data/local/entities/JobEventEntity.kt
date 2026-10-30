package com.example.jobsheetbook.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_events",
    indices = [Index(value = ["jobId"])]
)
data class JobEventEntity(
    @PrimaryKey
    val eventId: String = java.util.UUID.randomUUID().toString(),
    val jobId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "STATUS_CHANGE" or "NOTE"
    val description: String,
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "eventId" to eventId,
            "jobId" to jobId,
            "timestamp" to timestamp,
            "eventType" to eventType,
            "description" to description,
            "lastModified" to lastModified,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): JobEventEntity {
            return JobEventEntity(
                eventId = (map["eventId"] as? String) ?: java.util.UUID.randomUUID().toString(),
                jobId = (map["jobId"] as? Number)?.toLong() ?: 0L,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                eventType = (map["eventType"] as? String) ?: "",
                description = (map["description"] as? String) ?: "",
                lastModified = (map["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isDeleted = (map["isDeleted"] as? Boolean) ?: false
            )
        }
    }
}
