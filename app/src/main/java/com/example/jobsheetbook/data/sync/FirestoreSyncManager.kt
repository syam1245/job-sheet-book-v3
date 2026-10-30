package com.example.jobsheetbook.data.sync

import android.util.Log
import com.example.jobsheetbook.data.local.dao.CustomerDao
import com.example.jobsheetbook.data.local.dao.JobDao
import com.example.jobsheetbook.data.local.dao.JobEventDao
import com.example.jobsheetbook.data.local.entities.CustomerEntity
import com.example.jobsheetbook.data.local.entities.JobEntity
import com.example.jobsheetbook.data.local.entities.JobEventEntity
import com.example.jobsheetbook.data.repository.ChipLevelInfo
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val success: Boolean,
    val jobsUpdatedLocally: Int = 0,
    val jobsUploadedToCloud: Int = 0,
    val customersSynced: Int = 0,
    val chipLevelSynced: Int = 0,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class FirestoreSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val jobDao: JobDao,
    private val customerDao: CustomerDao,
    private val jobEventDao: JobEventDao,
    private val chipLevelRepo: ChipLevelRepository
) {
    private val TAG = "FirestoreSyncManager"
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    suspend fun syncOnce() {
        performInitialCatchUp()
    }

    private suspend fun performInitialCatchUp() {
        try {
            // ── Jobs ─────────────────────────────────────────────────────────
            Log.d(TAG, "CatchUp: localJobCount=${jobDao.getMaxJobId() ?: 0}")

            // A maximum local timestamp is not a safe remote cursor: another record may
            // have an older timestamp (or a device clock may be ahead). Pull the complete
            // collection and resolve each document by its own version instead.
            val remoteJobs = firestore.collection("jobs").get().await()

            Log.d(TAG, "CatchUp: remoteJobsFetched=${remoteJobs.documents.size}")

            var jobsInserted = 0
            for (doc in remoteJobs.documents) {
                val data = doc.data ?: error("Missing job data in ${doc.id}")
                try {
                    val remoteJob = JobEntity.fromMap(data)
                    val localJob = jobDao.getJobByIdIncludingDeleted(remoteJob.jobId)
                    if (localJob == null || remoteJob.lastModified > localJob.lastModified) {
                        jobDao.insertJob(remoteJob)
                        jobsInserted++
                    }
                } catch (e: Exception) { Log.e(TAG, "Job Sync Error", e); throw e }
            }
            Log.d(TAG, "CatchUp: jobsInserted=$jobsInserted")



            // ── Customers ────────────────────────────────────────────────────
            // Full scan avoids gaps caused by using a maximum row timestamp as a cursor.
            val remoteCustomers = firestore.collection("customers").get().await()

            Log.d(TAG, "CatchUp: remoteCustomersFetched=${remoteCustomers.documents.size}")

            for (doc in remoteCustomers.documents) {
                val data = doc.data ?: error("Missing customer data in ${doc.id}")
                try {
                    val remoteCustomer = CustomerEntity.fromMap(data)
                    val localCustomer = customerDao.getCustomerByPhone(remoteCustomer.phoneNumber)
                    if (localCustomer == null || remoteCustomer.lastModified > localCustomer.lastModified) {
                        customerDao.insertCustomer(remoteCustomer)
                    }
                } catch (e: Exception) { Log.e(TAG, "Customer Sync Error", e); throw e }
            }

            // ── ChipLevel ────────────────────────────────────────────────────
            // Full scan avoids missing older cloud records when local clocks or rows differ.
            val remoteChipLevel = firestore.collection("chiplevel_tracker").get().await()
            val localChipLevel = chipLevelRepo.getAllRecords().associateBy { it.jobId }

            for (doc in remoteChipLevel.documents) {
                val data = doc.data ?: error("Missing chip-level data in ${doc.id}")
                try {
                    val remoteInfo = ChipLevelInfo.fromMap(data)
                    val localInfo = localChipLevel[remoteInfo.jobId]
                    if (localInfo == null || remoteInfo.lastSyncUpdate > localInfo.lastSyncUpdate) {
                        if (remoteInfo.isDeleted) {
                            chipLevelRepo.deleteChipLevelInfo(remoteInfo.jobId)
                        } else {
                            chipLevelRepo.saveChipLevelInfo(remoteInfo, preserveLastModified = true)
                        }
                    }
                } catch (e: Exception) { Log.e(TAG, "ChipLevel Sync Error", e); throw e }
            }

            // ── Job Events ───────────────────────────────────────────────────
            // Events are stored as customers/{phone}/job_events/{eventId} subcollections.
            // A collection group query fetches all events across customers without needing
            // phone numbers. Full scan avoids gaps from a local maximum timestamp cursor.
            try {
                val remoteEvents = firestore.collectionGroup("job_events")
                    .get().await()

                for (doc in remoteEvents.documents) {
                    val data = doc.data ?: error("Missing event data in ${doc.id}")
                    try {
                        val remoteEvent = JobEventEntity.fromMap(data)
                        val localEvent = jobEventDao.getEventById(remoteEvent.eventId)
                        if (localEvent == null || remoteEvent.lastModified > localEvent.lastModified) {
                            jobEventDao.insertEvent(remoteEvent)
                        }
                    } catch (e: Exception) { Log.e(TAG, "Event Sync Error", e); throw e }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Event catch-up skipped (index may not be ready)", e)
                throw e
            }

            // Update lastSyncTime so the Dashboard "Last synced" indicator reflects
            // the automatic catch-up — not only manual syncs.
            _lastSyncTime.value = System.currentTimeMillis()

        } catch (e: Exception) {
            Log.e(TAG, "Catch-up failed", e)
            throw e
        }
    }

    /**
     * Full Bidirectional Manual Sync:
     * 1. Fetches all remote records and compares lastModified against local Room DB.
     * 2. Pushes newer/missing local jobs and customers to Firestore.
     * 3. Pulls newer/missing remote jobs and customers to local Room DB.
     * 4. Syncs Chip-Level outsource details to/from Firestore 'chiplevel_tracker'.
     */
    // Full bidirectional sync is used by both the background worker and manual action.
    // It is intended for current data volumes (<5000 jobs).
    // Add cursor-based pagination before the dataset grows beyond that scale.
    suspend fun performFullManualSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            var jobsUpdatedLocally = 0
            var jobsUploadedToCloud = 0
            var customersSynced = 0
            var chipLevelSynced = 0

            Log.d(TAG, "ManualSync: Starting full bidirectional sync")

            // 1. Sync Jobs
            // Compare the full remote set with all local rows so failed prior uploads and
            // edits older than any saved timestamp are retried.
            val remoteJobsSnapshots = firestore.collection("jobs").get().await()
            val remoteJobsMap = mutableMapOf<Long, JobEntity>()
            for (doc in remoteJobsSnapshots.documents) {
                val data = doc.data ?: error("Missing job data in ${doc.id}")
                try {
                    val remoteJob = JobEntity.fromMap(data)
                    remoteJobsMap[remoteJob.jobId] = remoteJob
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing remote job: ${doc.id}", e)
                    throw e
                }
            }

            val localJobsList = jobDao.getAllJobsList()
            for ((jobId, remoteJob) in remoteJobsMap) {
                val localJob = jobDao.getJobByIdIncludingDeleted(jobId)
                if (localJob == null || remoteJob.lastModified > localJob.lastModified) {
                    jobDao.insertJob(remoteJob)
                    jobsUpdatedLocally++
                } else if (localJob.lastModified > remoteJob.lastModified) {
                    uploadJob(localJob)
                    jobsUploadedToCloud++
                }
            }

            for (localJob in localJobsList) {
                if (!remoteJobsMap.containsKey(localJob.jobId)) {
                    uploadJob(localJob)
                    jobsUploadedToCloud++
                }
            }

            // 2. Sync Customers
            val remoteCustomersSnapshots = firestore.collection("customers").get().await()
            val remoteCustomersMap = mutableMapOf<String, CustomerEntity>()
            for (doc in remoteCustomersSnapshots.documents) {
                val data = doc.data ?: error("Missing customer data in ${doc.id}")
                try {
                    val remoteCustomer = CustomerEntity.fromMap(data)
                    remoteCustomersMap[remoteCustomer.phoneNumber] = remoteCustomer
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing remote customer: ${doc.id}", e)
                    throw e
                }
            }

            val localCustomersList = customerDao.getAllCustomersList()
            for ((phone, remoteCustomer) in remoteCustomersMap) {
                val localCustomer = customerDao.getCustomerByPhone(phone)
                if (localCustomer == null || remoteCustomer.lastModified > localCustomer.lastModified) {
                    customerDao.insertCustomer(remoteCustomer)
                    customersSynced++
                } else if (localCustomer.lastModified > remoteCustomer.lastModified) {
                    uploadCustomer(localCustomer)
                    customersSynced++
                }
            }

            for (localCustomer in localCustomersList) {
                if (!remoteCustomersMap.containsKey(localCustomer.phoneNumber)) {
                    uploadCustomer(localCustomer)
                    customersSynced++
                }
            }

            // 3. Sync Chip-Level Outsource Tracking Details
            try {
                val remoteChipSnapshots = firestore.collection("chiplevel_tracker").get().await()
                val remoteChipMap = mutableMapOf<Long, ChipLevelInfo>()
                val localChipMap = chipLevelRepo.getAllRecords().associateBy { it.jobId }
                for (doc in remoteChipSnapshots.documents) {
                    val data = doc.data ?: error("Missing chip-level data in ${doc.id}")
                    try {
                        val remoteInfo = ChipLevelInfo.fromMap(data)
                        remoteChipMap[remoteInfo.jobId] = remoteInfo
                        val localInfo = localChipMap[remoteInfo.jobId]
                        if (localInfo == null || remoteInfo.lastSyncUpdate > localInfo.lastSyncUpdate) {
                            if (remoteInfo.isDeleted) {
                                chipLevelRepo.deleteChipLevelInfo(remoteInfo.jobId)
                            } else {
                                chipLevelRepo.saveChipLevelInfo(remoteInfo, preserveLastModified = true)
                            }
                            chipLevelSynced++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing remote chip-level info: ${doc.id}", e)
                        throw e
                    }
                }

                val localModifiedChips = chipLevelRepo.getAllRecords()
                for (localInfo in localModifiedChips) {
                    val remoteInfo = remoteChipMap[localInfo.jobId]
                    if (remoteInfo == null || localInfo.lastSyncUpdate > remoteInfo.lastSyncUpdate) {
                        uploadChipLevel(localInfo)
                        chipLevelSynced++
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Chip-level sync warning", e)
                throw e
            }

            // 4. Sync Job Events (pull from Firestore)
            // Events are uploaded fire-and-forget via uploadEvent(); here we pull any events
            // that were created on another device but not yet present locally.
            try {
                val remoteEventsSnap = firestore.collectionGroup("job_events").get().await()

                val remoteEventsMap = mutableMapOf<String, JobEventEntity>()
                var eventsSynced = 0
                for (doc in remoteEventsSnap.documents) {
                    val data = doc.data ?: error("Missing event data in ${doc.id}")
                    try {
                        val remoteEvent = JobEventEntity.fromMap(data)
                        remoteEventsMap[remoteEvent.eventId] = remoteEvent
                        val localEvent = jobEventDao.getEventById(remoteEvent.eventId)
                        if (localEvent == null || remoteEvent.lastModified > localEvent.lastModified) {
                            jobEventDao.insertEvent(remoteEvent)
                            eventsSynced++
                        }
                    } catch (e: Exception) { Log.e(TAG, "Event Sync Error", e); throw e }
                }

                // Retry local events whose previous Firestore write failed. Full scans
                // on every run also pick up edits made while this sync is in progress.
                for (localEvent in jobEventDao.getAllEventsList()) {
                    val remoteEvent = remoteEventsMap[localEvent.eventId]
                    if (remoteEvent == null || localEvent.lastModified > remoteEvent.lastModified) {
                        uploadEvent(localEvent)
                    }
                }
                Log.d(TAG, "FullSync: eventsSynced=$eventsSynced")
            } catch (e: Exception) {
                Log.w(TAG, "Event full-sync skipped (index may not be ready)", e)
                throw e
            }

            val syncCompletedAt = System.currentTimeMillis()
            _lastSyncTime.value = syncCompletedAt

            SyncResult(
                success = true,
                jobsUpdatedLocally = jobsUpdatedLocally,
                jobsUploadedToCloud = jobsUploadedToCloud,
                customersSynced = customersSynced,
                chipLevelSynced = chipLevelSynced,
                message = "Synchronization completed successfully.",
                timestamp = syncCompletedAt
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Manual Sync Failed", e)
            SyncResult(
                success = false,
                message = e.localizedMessage ?: "Sync failed. Please check your internet connection."
            )
        }
    }

    suspend fun uploadJob(job: JobEntity) {
        try {
            firestore.collection("jobs").document(job.jobId.toString())
                .set(job.toMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Upload Job Fail", e)
            throw e
        }
    }

    suspend fun uploadCustomer(customer: CustomerEntity) {
        try {
            firestore.collection("customers").document(customer.phoneNumber)
                .set(customer.toMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Upload Customer Fail", e)
            throw e
        }
    }

    suspend fun uploadEvent(event: JobEventEntity) {
        withContext(Dispatchers.IO) {
            try {
                // Include soft-deleted jobs so event tombstones stay in the same
                // customer subcollection as their original cloud records.
                val job = jobDao.getJobByIdIncludingDeleted(event.jobId)
                val customerPhone = job?.customerPhone?.takeIf { it.isNotBlank() } ?: "orphans"
                firestore.collection("customers").document(customerPhone)
                    .collection("job_events").document(event.eventId)
                    .set(event.toMap(), SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.e(TAG, "Upload Event Fail", e)
                throw e
            }
        }
    }

    suspend fun uploadChipLevel(info: ChipLevelInfo) {
        try {
            firestore.collection("chiplevel_tracker").document(info.jobId.toString())
                .set(info.toMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Upload ChipLevel Fail", e)
            throw e
        }
    }

    suspend fun deleteChipLevel(jobId: Long) {
        try {
            // Soft-delete: set isDeleted=true so other devices see the tombstone on next sync
            // and skip the record, instead of re-inserting it from their local copy.
            firestore.collection("chiplevel_tracker").document(jobId.toString())
                .set(
                    mapOf(
                        "isDeleted" to true,
                        "lastModified" to System.currentTimeMillis(),
                        "lastSyncUpdate" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Delete ChipLevel Fail", e)
            throw e
        }
    }
}
