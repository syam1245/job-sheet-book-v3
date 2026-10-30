package com.example.jobsheetbook.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: FirestoreSyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("sync_timestamps", Context.MODE_PRIVATE)
        val lastAutoSync = prefs.getLong("last_auto_sync", 0L)
        val currentTime = System.currentTimeMillis()

        // Throttle: Only run auto-sync if 15 minutes have passed since the last successful sync
        if (currentTime - lastAutoSync < 15 * 60 * 1000L) {
            return Result.success()
        }

        return try {
            // Include local changes so uploads that failed during a save are
            // retried automatically, rather than waiting for a manual sync.
            val syncResult = syncManager.performFullManualSync()
            if (!syncResult.success) return Result.retry()
            prefs.edit().putLong("last_auto_sync", currentTime).apply()
            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.retry()
        }
    }
}
