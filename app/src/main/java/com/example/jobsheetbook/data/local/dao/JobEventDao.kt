package com.example.jobsheetbook.data.local.dao

import androidx.room.*
import com.example.jobsheetbook.data.local.entities.JobEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: JobEventEntity)

    @Query("SELECT * FROM job_events WHERE jobId = :jobId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getEventsForJob(jobId: Long): Flow<List<JobEventEntity>>

    @Query("SELECT * FROM job_events WHERE jobId = :jobId")
    suspend fun getAllEventsForJobIncludingDeleted(jobId: Long): List<JobEventEntity>

    @Query("UPDATE job_events SET isDeleted = 1, lastModified = :timestamp WHERE jobId = :jobId")
    suspend fun softDeleteEventsForJob(jobId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE job_events SET isDeleted = 0, lastModified = :timestamp WHERE jobId = :jobId")
    suspend fun restoreEventsForJob(jobId: Long, timestamp: Long)

    @Query("SELECT * FROM job_events WHERE lastModified > :since")
    suspend fun getEventsModifiedSince(since: Long): List<JobEventEntity>

    @Query("SELECT * FROM job_events")
    suspend fun getAllEventsList(): List<JobEventEntity>

    @Query("SELECT MAX(lastModified) FROM job_events")
    suspend fun getMaxLastModified(): Long?

    @Query("SELECT * FROM job_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): JobEventEntity?

    @Query("""
        SELECT * FROM job_events 
        WHERE jobId = :jobId AND eventType = 'STATUS_CHANGE' AND isDeleted = 0
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLastStatusChangeEvent(jobId: Long): JobEventEntity?

    @Query("""
        SELECT e.* FROM job_events e
        WHERE e.jobId IN (:jobIds)
          AND e.eventType = 'STATUS_CHANGE'
          AND e.isDeleted = 0
          AND e.eventId = (
              SELECT latest.eventId FROM job_events latest
              WHERE latest.jobId = e.jobId
                AND latest.eventType = 'STATUS_CHANGE'
                AND latest.isDeleted = 0
              ORDER BY latest.timestamp DESC, latest.eventId DESC
              LIMIT 1
          )
    """)
    suspend fun getLastStatusChangeEvents(jobIds: List<Long>): List<JobEventEntity>
}
