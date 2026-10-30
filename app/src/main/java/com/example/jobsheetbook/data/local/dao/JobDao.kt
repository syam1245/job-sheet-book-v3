package com.example.jobsheetbook.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.example.jobsheetbook.data.local.entities.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs WHERE isDeleted = 0 AND isArchived = 0 ORDER BY createdAt DESC, jobId DESC")
    fun getAllJobsPaging(): PagingSource<Int, JobEntity>

    @Query("SELECT * FROM jobs WHERE jobId = :jobId AND isDeleted = 0")
    suspend fun getJobById(jobId: Long): JobEntity?

    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    fun getJobByIdFlow(jobId: Long): Flow<JobEntity?>

    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    suspend fun getJobByIdIncludingDeleted(jobId: Long): JobEntity?

    @Query("""
        SELECT * FROM jobs
        WHERE isDeleted = 1
          AND (:query = '' OR displayJobId LIKE '%' || :query || '%'
            OR CAST(jobId AS TEXT) LIKE '%' || :query || '%'
            OR customerName LIKE '%' || :query || '%'
            OR customerPhone LIKE '%' || :query || '%'
            OR make LIKE '%' || :query || '%'
            OR model LIKE '%' || :query || '%'
            OR remarks LIKE '%' || :query || '%')
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun searchDeletedJobs(query: String): Flow<List<JobEntity>>

    @Query("""
        SELECT * FROM jobs 
        WHERE (customerPhone LIKE '%' || :query || '%' OR displayJobId LIKE '%' || :query || '%' OR jobId LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%')
        AND (
            (:status = 'Newest' AND status NOT IN ('Delivered', 'Return')) OR
            (:status = 'Old-Jobs' AND createdAt < :ninetyDaysAgo) OR
            (:status = 'Chip-Level' AND (status LIKE '%Chip%Level%' OR status LIKE '%chip%level%') AND isArchived = 0) OR
            (:status = 'Delivered' AND status = 'Delivered') OR
            (:status = 'Return' AND status = 'Return') OR
            (:status = 'Work in-progress' AND status IN ('Work in-progress', 'In Progress', 'In-Progress', 'Pending')) OR
            (:status NOT IN ('Newest', 'Old-Jobs', 'Chip-Level', 'Delivered', 'Return', 'Work in-progress') AND status = :status AND isArchived = 0)
        )
        AND isDeleted = 0
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun searchJobsByCategoryPaging(query: String, status: String, ninetyDaysAgo: Long): PagingSource<Int, JobEntity>

    @Query("""
        SELECT * FROM jobs 
        WHERE (:query = '' OR
            customerPhone LIKE '%' || :query || '%' 
            OR displayJobId LIKE '%' || :query || '%' 
            OR CAST(jobId AS TEXT) LIKE '%' || :query || '%' 
            OR customerName LIKE '%' || :query || '%'
            OR make LIKE '%' || :query || '%'
            OR model LIKE '%' || :query || '%'
            OR remarks LIKE '%' || :query || '%')
        AND isDeleted = 0
        AND isArchived = 0
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun searchActiveJobsWithQuery(query: String): PagingSource<Int, JobEntity>

    @Query("""
        SELECT * FROM jobs 
        WHERE (:query = '' OR
            customerPhone LIKE '%' || :query || '%' 
            OR displayJobId LIKE '%' || :query || '%' 
            OR CAST(jobId AS TEXT) LIKE '%' || :query || '%' 
            OR customerName LIKE '%' || :query || '%'
            OR make LIKE '%' || :query || '%'
            OR model LIKE '%' || :query || '%'
            OR remarks LIKE '%' || :query || '%')
        AND isDeleted = 0
        AND isArchived = 1
        ORDER BY jobId DESC
    """)
    fun searchColdStorageJobsWithQuery(query: String): PagingSource<Int, JobEntity>

    @Query("""
        SELECT * FROM jobs 
        WHERE (:query = '' OR
            customerPhone LIKE '%' || :query || '%' 
            OR displayJobId LIKE '%' || :query || '%' 
            OR CAST(jobId AS TEXT) LIKE '%' || :query || '%' 
            OR customerName LIKE '%' || :query || '%'
            OR make LIKE '%' || :query || '%'
            OR model LIKE '%' || :query || '%'
            OR remarks LIKE '%' || :query || '%')
        AND (:query != '' OR isDeleted = 0)
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun searchAllJobsWithQuery(query: String): PagingSource<Int, JobEntity>

    @Query("""
        SELECT COUNT(*) FROM jobs 
        WHERE (
            status = :status 
            OR (:status = 'Chip-Level' AND (status LIKE '%Chip%Level%' OR status LIKE '%chip%level%'))
            OR (:status = 'Work in-progress' AND status IN ('Work in-progress', 'In Progress', 'In-Progress', 'Pending'))
        ) 
        AND isDeleted = 0
    """)
    fun getStatusCount(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM jobs WHERE isDeleted = 0")
    fun getTotalJobsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM jobs WHERE isDeleted = 0 AND status NOT IN ('Delivered', 'Return')")
    fun getNewestJobsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM jobs WHERE isDeleted = 0 AND createdAt < :ninetyDaysAgo")
    fun getOldJobsCount(ninetyDaysAgo: Long): Flow<Int>

    @Query("""
        SELECT * FROM jobs 
        WHERE (:query != '' OR isDeleted = 0)
        AND (:query != '' OR :status IS NULL OR :status IN ('Newest', 'Old-Jobs', 'Delivered', 'Return') OR isArchived = 0)
        AND (
            :status IS NULL 
            OR (:status = 'Newest' AND status NOT IN ('Delivered', 'Return')) 
            OR (:status = 'Old-Jobs' AND createdAt < :ninetyDaysAgo) 
            OR (:status = 'Chip-Level' AND (status LIKE '%Chip%Level%' OR status LIKE '%chip%level%')) 
            OR (:status = 'Work in-progress' AND status IN ('Work in-progress', 'In Progress', 'In-Progress', 'Pending')) 
            OR (:status = 'Urgent' AND dueDate >= :tomorrowStart AND dueDate < :thirdDayStart AND status NOT IN ('Delivered', 'Return') AND isArchived = 0)
            OR (:status NOT IN ('Newest', 'Old-Jobs', 'Chip-Level', 'Work in-progress', 'Urgent') AND status = :status)
        )
        AND (
            :query = '' 
            OR customerPhone LIKE '%' || :query || '%' 
            OR displayJobId LIKE '%' || :query || '%' 
            OR CAST(jobId AS TEXT) LIKE '%' || :query || '%' 
            OR customerName LIKE '%' || :query || '%' 
            OR make LIKE '%' || :query || '%' 
            OR model LIKE '%' || :query || '%' 
            OR remarks LIKE '%' || :query || '%'
        )
        ORDER BY createdAt DESC, jobId DESC
        LIMIT :limit
    """)
    fun getJobsWithLimit(
        query: String,
        status: String?,
        limit: Int,
        ninetyDaysAgo: Long,
        tomorrowStart: Long,
        thirdDayStart: Long
    ): Flow<List<JobEntity>>

    @Query("""
        SELECT COUNT(*) FROM jobs 
        WHERE (:query != '' OR isDeleted = 0)
        AND (:query != '' OR :status IS NULL OR :status IN ('Newest', 'Old-Jobs', 'Delivered', 'Return') OR isArchived = 0)
        AND (
            :status IS NULL 
            OR (:status = 'Newest' AND status NOT IN ('Delivered', 'Return')) 
            OR (:status = 'Old-Jobs' AND createdAt < :ninetyDaysAgo) 
            OR (:status = 'Chip-Level' AND (status LIKE '%Chip%Level%' OR status LIKE '%chip%level%')) 
            OR (:status = 'Work in-progress' AND status IN ('Work in-progress', 'In Progress', 'In-Progress', 'Pending')) 
            OR (:status = 'Urgent' AND dueDate >= :tomorrowStart AND dueDate < :thirdDayStart AND status NOT IN ('Delivered', 'Return') AND isArchived = 0)
            OR (:status NOT IN ('Newest', 'Old-Jobs', 'Chip-Level', 'Work in-progress', 'Urgent') AND status = :status)
        )
        AND (
            :query = '' 
            OR customerPhone LIKE '%' || :query || '%' 
            OR displayJobId LIKE '%' || :query || '%' 
            OR CAST(jobId AS TEXT) LIKE '%' || :query || '%' 
            OR customerName LIKE '%' || :query || '%' 
            OR make LIKE '%' || :query || '%' 
            OR model LIKE '%' || :query || '%' 
            OR remarks LIKE '%' || :query || '%'
        )
    """)
    fun getFilteredJobsCount(
        query: String,
        status: String?,
        ninetyDaysAgo: Long,
        tomorrowStart: Long,
        thirdDayStart: Long
    ): Flow<Int>

    @Query("""
        SELECT * FROM jobs 
        WHERE status NOT IN ('Delivered', 'Return') 
        AND isDeleted = 0 
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun getActiveJobsPaging(): PagingSource<Int, JobEntity>

    @Query("SELECT * FROM jobs WHERE isDeleted = 0 ORDER BY createdAt DESC, jobId DESC")
    fun getAllNonDeletedJobsPaging(): PagingSource<Int, JobEntity>

    @Query("SELECT COUNT(*) FROM jobs WHERE status NOT IN ('Delivered', 'Return') AND isDeleted = 0")
    fun getActiveJobsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity): Long

    @Update
    suspend fun updateJob(job: JobEntity)

    @Delete
    suspend fun deleteJob(job: JobEntity)

    @Query("UPDATE jobs SET isDeleted = 1, lastModified = :timestamp WHERE jobId = :jobId")
    suspend fun softDeleteJob(jobId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT MAX(jobId) FROM jobs")
    suspend fun getMaxJobId(): Long?
    
    @Query("SELECT * FROM jobs WHERE lastModified > :since")
    suspend fun getJobsModifiedSince(since: Long): List<JobEntity>

    @Query("SELECT COUNT(*) FROM jobs WHERE status IN ('Sent Out', 'Returned - Awaiting Delivery')")
    suspend fun countLegacyVendorStatusJobs(): Int



    @Query("SELECT * FROM jobs WHERE isDeleted = 0 ORDER BY createdAt DESC, jobId DESC")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Query("""
        SELECT * FROM jobs
        WHERE isDeleted = 0 AND (createdAt >= :cutoffMs OR status IN ('Chip-Level', 'Sent Out'))
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun getAllJobsFromDateOrActive(cutoffMs: Long): Flow<List<JobEntity>>

    @Query("""
        SELECT * FROM jobs 
        WHERE isDeleted = 0 AND isArchived = 0 
        AND status NOT IN ('Delivered', 'Return')
        AND (createdAt >= :cutoffMs OR status IN ('Chip-Level', 'Sent Out'))
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun getAllJobsPagingFromDate(cutoffMs: Long): PagingSource<Int, JobEntity>

    @Query("""
        SELECT * FROM jobs 
        WHERE isDeleted = 0 AND isArchived = 0
        AND status NOT IN ('Delivered', 'Return')
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun getAllJobsPagingAllTime(): PagingSource<Int, JobEntity>

    @Query("""
        SELECT * FROM jobs
        WHERE isDeleted = 0 AND isArchived = 0
        AND (status = 'Chip-Level' OR status = 'Sent Out')
        AND createdAt >= :cutoffMs
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun getChipLevelJobsFromDate(cutoffMs: Long): Flow<List<JobEntity>>

    @Query("""
        SELECT * FROM jobs
        WHERE isDeleted = 0 AND isArchived = 0
        AND (status = 'Chip-Level' OR status = 'Sent Out')
        ORDER BY createdAt DESC, jobId DESC
    """)
    fun getChipLevelJobsAllTime(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs")
    suspend fun getAllJobsList(): List<JobEntity>

    @Query("""
        SELECT * FROM jobs 
        WHERE isDeleted = 0 
        AND isArchived = 0
        AND status NOT IN ('Delivered', 'Return') 
        ORDER BY jobId ASC
    """)
    suspend fun getActiveJobsForNotification(): List<JobEntity>

    @Query("SELECT MAX(lastModified) FROM jobs")
    suspend fun getMaxLastModified(): Long?

    // Jobs received (created) today
    @Query("""
        SELECT * FROM jobs 
        WHERE isDeleted = 0 
        AND createdAt >= :startOfDay 
        AND createdAt <= :endOfDay
        ORDER BY jobId ASC
    """)
    suspend fun getJobsCreatedToday(startOfDay: Long, endOfDay: Long): List<JobEntity>

    // Jobs whose status changed to Delivered or Ready today
    @Query("""
        SELECT DISTINCT j.* FROM jobs j
        INNER JOIN job_events e ON j.jobId = e.jobId
        WHERE j.isDeleted = 0
        AND e.isDeleted = 0
        AND e.eventType = 'STATUS_CHANGE'
        AND e.description LIKE '%Delivered%'
        AND e.timestamp >= :startOfDay
        AND e.timestamp <= :endOfDay
        ORDER BY j.jobId ASC
    """)
    suspend fun getJobsDeliveredToday(startOfDay: Long, endOfDay: Long): List<JobEntity>

    @Query("""
        SELECT DISTINCT j.* FROM jobs j
        INNER JOIN job_events e ON j.jobId = e.jobId
        WHERE j.isDeleted = 0
        AND e.isDeleted = 0
        AND e.eventType = 'STATUS_CHANGE'
        AND e.description LIKE '%Chip-Level%'
        AND e.timestamp >= :startOfDay
        AND e.timestamp <= :endOfDay
        ORDER BY j.jobId ASC
    """)
    suspend fun getJobsSentToChipLevelToday(startOfDay: Long, endOfDay: Long): List<JobEntity>

    @Query("""
        SELECT * FROM jobs 
        WHERE isDeleted = 0 
        AND status = 'Ready' 
        ORDER BY jobId ASC
    """)
    suspend fun getAllReadyJobs(): List<JobEntity>

    /**
     * Archives jobs that have been in "Delivered" or "Return" status for longer than the
     * given threshold. Uses the most recent STATUS_CHANGE event timestamp (when the job
     * was actually delivered/returned), falling back to lastModified for older jobs that
     * predate the event-tracking system. Does NOT use createdAt — a job created weeks ago
     * but delivered today must NOT be immediately archived.
     */
    @Query("""
        UPDATE jobs SET isArchived = 1
        WHERE status IN ('Delivered', 'Return')
        AND isArchived = 0
        AND isDeleted = 0
        AND COALESCE(
            (SELECT MAX(e.timestamp) FROM job_events e
             WHERE e.jobId = jobs.jobId
               AND e.isDeleted = 0
               AND e.eventType = 'STATUS_CHANGE'
               AND (e.description LIKE '%Delivered%' OR e.description LIKE '%Return%')),
            lastModified
        ) < :threshold
    """)
    suspend fun markAsArchived(threshold: Long)

    @Query("""
        UPDATE jobs SET isArchived = 0, lastModified = :timestamp
        WHERE status IN ('Delivered', 'Return')
        AND isArchived = 1
        AND isDeleted = 0
        AND COALESCE(
            (SELECT MAX(e.timestamp) FROM job_events e
             WHERE e.jobId = jobs.jobId
               AND e.isDeleted = 0
               AND e.eventType = 'STATUS_CHANGE'
               AND (e.description LIKE '%Delivered%' OR e.description LIKE '%Return%')),
            lastModified
        ) >= :cutoff
    """)
    suspend fun unarchiveRecentDelivered(cutoff: Long, timestamp: Long)

    @Query("""
        SELECT * FROM jobs 
        WHERE dueDate IS NOT NULL 
        AND dueDate < :currentTime
        AND status NOT IN ('Ready', 'Delivered', 'Return')
        AND isDeleted = 0
        AND isArchived = 0
        ORDER BY dueDate ASC
    """)
    fun getOverdueJobs(currentTime: Long = System.currentTimeMillis()): Flow<List<JobEntity>>

    @Query("""
        SELECT * FROM jobs 
        WHERE dueDate IS NOT NULL 
        AND dueDate >= :tomorrowStart AND dueDate < :thirdDayStart
        AND status NOT IN ('Delivered', 'Return')
        AND isDeleted = 0
        AND isArchived = 0
        ORDER BY dueDate ASC
    """)
    fun getUrgentJobs(tomorrowStart: Long, thirdDayStart: Long): Flow<List<JobEntity>>

    @Query("""
        SELECT * FROM jobs 
        WHERE dueDate >= :tomorrowStart AND dueDate < :thirdDayStart
        AND status NOT IN ('Delivered', 'Return')
        AND isDeleted = 0
        AND isArchived = 0
        ORDER BY dueDate ASC
    """)
    fun getUrgentJobsPaging(
        tomorrowStart: Long,
        thirdDayStart: Long
    ): PagingSource<Int, JobEntity>

    @Query("""
        SELECT COUNT(*) FROM jobs 
        WHERE dueDate >= :tomorrowStart AND dueDate < :thirdDayStart
        AND status NOT IN ('Delivered', 'Return')
        AND isDeleted = 0
        AND isArchived = 0
    """)
    fun getUrgentJobCount(
        tomorrowStart: Long,
        thirdDayStart: Long
    ): Flow<Int>

    /**
     * Returns delivered jobs whose delivery date falls within [startOfRange, endOfRange].
     * The delivery date is determined by the most recent STATUS_CHANGE event with 'Delivered'
     * in its description, falling back to lastModified for legacy jobs without event tracking.
     * Does NOT filter on isArchived — archived delivered jobs must appear in historical reports.
     */
    @Query("""
        SELECT j.*, 
               COALESCE(
                   (SELECT e.timestamp FROM job_events e 
                    WHERE e.jobId = j.jobId 
                    AND e.isDeleted = 0
                    AND e.eventType = 'STATUS_CHANGE' 
                    AND e.description LIKE '%Delivered%' 
                    ORDER BY e.timestamp DESC LIMIT 1),
                   j.lastModified
               ) AS deliveredAt
        FROM jobs j
        WHERE j.status = 'Delivered'
        AND j.isDeleted = 0
        AND COALESCE(
            (SELECT e.timestamp FROM job_events e 
             WHERE e.jobId = j.jobId 
             AND e.isDeleted = 0
             AND e.eventType = 'STATUS_CHANGE' 
             AND e.description LIKE '%Delivered%' 
             ORDER BY e.timestamp DESC LIMIT 1),
            j.lastModified
        ) BETWEEN :startOfRange AND :endOfRange
        ORDER BY deliveredAt ASC
    """)
    suspend fun getDeliveredJobsInRange(startOfRange: Long, endOfRange: Long): List<JobWithDeliveredAt>

    @Query("""
        SELECT * FROM jobs 
        WHERE status IN ('Ready', 'Delivered') 
        AND finalCost = 0 
        AND isDeleted = 0
        ORDER BY createdAt ASC
    """)
    suspend fun getJobsMissingFinalCost(): List<JobEntity>
}
