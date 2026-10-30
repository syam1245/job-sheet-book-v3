package com.example.jobsheetbook

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.jobsheetbook.data.sync.FirestoreSyncManager
import dagger.hilt.android.HiltAndroidApp
import coil.Coil
import coil.ImageLoader
import coil.memory.MemoryCache
import javax.inject.Inject

@HiltAndroidApp
class JobSheetApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var syncManager: FirestoreSyncManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationStateRepo: com.example.jobsheetbook.data.repository.NotificationStateRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Manual Coil setup to reduce memory cache usage below standard 25% (Optimization)
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // 15% RAM limit
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)
        
        // Enqueue one-time sync request instead of real-time listener
        val syncConstraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.jobsheetbook.data.sync.SyncWorker>()
            .setConstraints(syncConstraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
            "sync_on_launch",
            androidx.work.ExistingWorkPolicy.KEEP,
            syncRequest
        )

        val immediateNotificationRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.jobsheetbook.data.sync.NotificationWorker>()
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
            "notification_on_launch",
            androidx.work.ExistingWorkPolicy.REPLACE,
            immediateNotificationRequest
        )

        scheduleWorkers()

        // One-time migration: clear corrupted notification counts
        // from before the incrementCount bug fix (migration version 1)
        if (!notificationStateRepo.hasCompletedMigration(1)) {
            notificationStateRepo.clearAllStates()
            notificationStateRepo.markMigrationComplete(1)
        }
    }

    private fun scheduleWorkers() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
            .build()

        val notificationRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.jobsheetbook.data.sync.NotificationWorker>(2, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "job_notification_worker",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            notificationRequest
        )

        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        if (calendar.before(java.util.Calendar.getInstance())) {
            calendar.add(java.util.Calendar.HOUR_OF_DAY, 24)
        }
        val delay = calendar.timeInMillis - System.currentTimeMillis()

        val archiveRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.jobsheetbook.data.sync.ArchiveWorker>(24, java.util.concurrent.TimeUnit.HOURS)
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "job_archive_worker",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            archiveRequest
        )
    }
}
