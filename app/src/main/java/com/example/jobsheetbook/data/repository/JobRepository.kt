package com.example.jobsheetbook.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.jobsheetbook.data.local.dao.CustomerDao
import com.example.jobsheetbook.data.local.dao.JobDao
import com.example.jobsheetbook.data.local.dao.JobEventDao
import com.example.jobsheetbook.data.local.entities.CustomerEntity
import com.example.jobsheetbook.data.local.entities.JobEventEntity
import com.example.jobsheetbook.data.local.entities.JobEntity
import com.example.jobsheetbook.data.mappers.toDomain
import com.example.jobsheetbook.data.mappers.toEntity
import com.example.jobsheetbook.data.sync.FirestoreSyncManager
import com.example.jobsheetbook.data.sync.SyncResult
import com.example.jobsheetbook.domain.models.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import com.example.jobsheetbook.util.PhoneNormalizer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class JobRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customerDao: CustomerDao,
    private val jobDao: JobDao,
    private val jobEventDao: JobEventDao,
    private val syncManager: FirestoreSyncManager
) {
    private fun urgentDateBounds(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 2)
        return tomorrowStart to calendar.timeInMillis
    }

    private suspend fun uploadSafely(operation: String, upload: suspend () -> Unit) {
        try {
            upload()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("JobRepository", "$operation failed; local data is retained for a later sync", e)
        }
    }

    private val _oneMonthCutoff: Long
        get() = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

    fun getOneMonthCutoff(): Long = _oneMonthCutoff

    fun getJobsFromDateOrActive(cutoffMs: Long): Flow<List<Job>> =
        jobDao.getAllJobsFromDateOrActive(cutoffMs).map { list -> list.map { it.toDomain() } }

    fun getPagedActiveJobs(): Flow<PagingData<Job>> {
        return Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false, maxSize = 100),
            pagingSourceFactory = { jobDao.getActiveJobsPaging() }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    fun getPagedJobs(query: String = "", status: String? = null, cutoff: Long = 0L): Flow<PagingData<Job>> {
        val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val (tomorrowStart, thirdDayStart) = urgentDateBounds()
        
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5,
                maxSize = 100
            ),
            pagingSourceFactory = {
                if (query.isNotBlank()) {
                    jobDao.searchAllJobsWithQuery(query)
                } else if (status == "Urgent") {
                    jobDao.getUrgentJobsPaging(tomorrowStart, thirdDayStart)
                } else if (status != null) {
                    jobDao.searchJobsByCategoryPaging("", status, ninetyDaysAgo)
                } else {
                    jobDao.getAllNonDeletedJobsPaging()
                }
            }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    fun getStatusCount(status: String): Flow<Int> = jobDao.getStatusCount(status)
    fun getTotalJobsCount(): Flow<Int> = jobDao.getTotalJobsCount()
    fun getAllJobs(): Flow<List<Job>> = jobDao.getAllJobs().map { list -> list.map { it.toDomain() } }

    fun getJobsWithLimit(query: String = "", status: String? = null, limit: Int = 20): Flow<List<Job>> {
        val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val (tomorrowStart, thirdDayStart) = urgentDateBounds()
        
        val effectiveStatus = if (query.isNotBlank()) null else status
        
        return jobDao.getJobsWithLimit(query.trim(), effectiveStatus, limit, ninetyDaysAgo, tomorrowStart, thirdDayStart)
            .map { list -> list.map { it.toDomain() } }
    }

    fun getFilteredJobsCount(query: String = "", status: String? = null): Flow<Int> {
        val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val (tomorrowStart, thirdDayStart) = urgentDateBounds()
        
        val effectiveStatus = if (query.isNotBlank()) null else status
        
        return jobDao.getFilteredJobsCount(query.trim(), effectiveStatus, ninetyDaysAgo, tomorrowStart, thirdDayStart)
    }

    fun getNewestJobsCount(): Flow<Int> = jobDao.getNewestJobsCount()

    fun getOldJobsCount(): Flow<Int> {
        val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        return jobDao.getOldJobsCount(cutoff)
    }

    suspend fun unarchiveRecentDelivered(cutoff: Long) =
        jobDao.unarchiveRecentDelivered(cutoff, System.currentTimeMillis())
    
    fun getOverdueJobs(): Flow<List<Job>> = jobDao.getOverdueJobs().map { list -> list.map { it.toDomain() } }
    fun getUrgentJobs(): Flow<List<Job>> {
        val (tomorrowStart, thirdDayStart) = urgentDateBounds()
        return jobDao.getUrgentJobs(tomorrowStart, thirdDayStart).map { list -> list.map { it.toDomain() } }
    }

    fun getUrgentJobCount(): Flow<Int> {
        val (tomorrowStart, thirdDayStart) = urgentDateBounds()
        return jobDao.getUrgentJobCount(tomorrowStart, thirdDayStart)
    }

    suspend fun getCustomerByPhone(phone: String): CustomerEntity? {
        val normalized = when (val res = PhoneNormalizer.normalize(phone)) {
            is PhoneNormalizer.Result.Valid -> res.stored
            else -> phone
        }
        return customerDao.getCustomerByPhone(normalized)
    }

    suspend fun saveCustomer(customer: CustomerEntity) {
        if (customer.phoneNumber.isBlank()) return
        val existingCustomer = customerDao.getCustomerByPhone(customer.phoneNumber)

        if (existingCustomer == null || existingCustomer.name != customer.name) {
            val updatedCustomer = customer.copy(lastModified = System.currentTimeMillis())
            customerDao.insertCustomer(updatedCustomer)
            uploadSafely("Customer upload") { syncManager.uploadCustomer(updatedCustomer) }
        }
    }

    suspend fun saveCustomer(phone: String, name: String) {
        saveCustomer(CustomerEntity(phoneNumber = phone, name = name))
    }

    suspend fun saveJob(job: Job): Long {
        val jobEntity = job.toEntity()
        val now = System.currentTimeMillis()
        val updatedJob = jobEntity.copy(lastModified = now)
        val insertedId = jobDao.insertJob(updatedJob)
        uploadSafely("Job upload") { syncManager.uploadJob(updatedJob) }
        return insertedId
    }

    suspend fun updateJob(job: Job) {
        val jobEntity = job.toEntity()
        val normalizedJob = if (jobEntity.status !in listOf("Delivered", "Return")) {
            jobEntity.copy(isArchived = false)
        } else jobEntity
        val existingJob = jobDao.getJobById(jobEntity.jobId)
        if (existingJob != null) {
            val dataChanged = existingJob.copy(lastModified = 0L) != normalizedJob.copy(lastModified = 0L)
            if (!dataChanged) {
                return 
            }
        }

        val updatedJob = normalizedJob.copy(lastModified = System.currentTimeMillis())
        jobDao.updateJob(updatedJob)
        uploadSafely("Job upload") { syncManager.uploadJob(updatedJob) }
    }

    suspend fun deleteJob(jobId: Long) {
        val timestamp = System.currentTimeMillis()
        val existingJob = jobDao.getJobByIdIncludingDeleted(jobId)
        jobDao.softDeleteJob(jobId, timestamp)
        jobEventDao.softDeleteEventsForJob(jobId, timestamp)
        
        val jobToSync = (existingJob ?: jobDao.getJobByIdIncludingDeleted(jobId))
            ?.copy(isDeleted = true, lastModified = timestamp)
            ?: JobEntity(
                jobId = jobId,
                customerPhone = "",
                customerName = "",
                itemType = "",
                make = "",
                model = "",
                serialNumber = "",
                devicePassword = "",
                remarks = "",
                isDeleted = true,
                lastModified = timestamp
            )
        uploadSafely("Job deletion upload") { syncManager.uploadJob(jobToSync) }
    }


    suspend fun countLegacyVendorStatusJobs(): Int {
        return jobDao.countLegacyVendorStatusJobs()
    }

    suspend fun getJobById(jobId: Long): Job? {
        return jobDao.getJobById(jobId)?.toDomain()
    }

    suspend fun getJobByIdIncludingDeleted(jobId: Long): Job? {
        return jobDao.getJobByIdIncludingDeleted(jobId)?.toDomain()
    }

    fun getDeletedJobs(query: String): Flow<List<Job>> =
        jobDao.searchDeletedJobs(query.trim()).map { jobs -> jobs.map { it.toDomain() } }

    suspend fun restoreDeletedJob(jobId: Long): Boolean {
        val job = jobDao.getJobByIdIncludingDeleted(jobId)?.takeIf { it.isDeleted } ?: return false
        val timestamp = System.currentTimeMillis()
        val restoredJob = job.copy(isDeleted = false, isArchived = false, lastModified = timestamp)
        jobDao.updateJob(restoredJob)
        uploadSafely("Restored job upload") { syncManager.uploadJob(restoredJob) }

        val events = jobEventDao.getAllEventsForJobIncludingDeleted(jobId)
        jobEventDao.restoreEventsForJob(jobId, timestamp)
        events.forEach { event ->
            val restoredEvent = event.copy(isDeleted = false, lastModified = timestamp)
            uploadSafely("Restored job event upload") { syncManager.uploadEvent(restoredEvent) }
        }
        return true
    }

    fun getJobByIdFlow(jobId: Long): Flow<Job?> {
        return jobDao.getJobByIdFlow(jobId).map { it?.toDomain() }
    }

    suspend fun getNextJobId(): Long {
        val maxId = jobDao.getMaxJobId()
        return if (maxId == null || maxId < 12983) {
            12983
        } else {
            maxId + 1
        }
    }

    fun getEventsForJob(jobId: Long): Flow<List<JobEventEntity>> {
        return jobEventDao.getEventsForJob(jobId)
    }

    suspend fun insertEvent(event: JobEventEntity) {
        val updatedEvent = event.copy(lastModified = System.currentTimeMillis())
        jobEventDao.insertEvent(updatedEvent)
        uploadSafely("Job event upload") { syncManager.uploadEvent(updatedEvent) }
    }

    val lastSyncTime: StateFlow<Long?> = syncManager.lastSyncTime

    suspend fun performManualSync(): SyncResult {
        return syncManager.performFullManualSync()
    }

    suspend fun uploadChipLevelInfo(info: ChipLevelInfo) {
        uploadSafely("Chip-level upload") { syncManager.uploadChipLevel(info) }
    }
}
