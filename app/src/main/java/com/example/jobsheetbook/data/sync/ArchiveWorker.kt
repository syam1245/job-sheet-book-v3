package com.example.jobsheetbook.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.jobsheetbook.data.local.dao.JobDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ArchiveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val jobDao: JobDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Archiving hides finished jobs from the active list; keep their records for search and sync.
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000L)
        jobDao.markAsArchived(sevenDaysAgo)
        return Result.success()
    }
}
