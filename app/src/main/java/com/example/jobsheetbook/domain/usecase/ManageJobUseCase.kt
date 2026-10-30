package com.example.jobsheetbook.domain.usecase

import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.example.jobsheetbook.data.repository.JobRepository
import com.example.jobsheetbook.data.repository.PartsOrderRepository
import com.example.jobsheetbook.data.repository.PartsOrderState
import com.example.jobsheetbook.data.sync.FirestoreSyncManager
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.util.PartsKeywordDetector
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ManageJobUseCase @Inject constructor(
    private val jobRepository: JobRepository,
    private val partsOrderRepo: PartsOrderRepository,
    private val chipLevelRepo: ChipLevelRepository,
    private val syncManager: FirestoreSyncManager
) {
    suspend fun saveJob(job: Job): Long {
        val id = jobRepository.saveJob(job)
        managePartsOrderState(id, job.remarks, job.rawStatus)
        return id
    }

    suspend fun updateJob(job: Job) {
        jobRepository.updateJob(job)
        managePartsOrderState(job.jobId, job.remarks, job.rawStatus)
    }

    suspend fun deleteJob(jobId: Long) {
        jobRepository.deleteJob(jobId)
        // Keep the phone-only parts record so restoring this job also restores its tracking.
        chipLevelRepo.deleteChipLevelInfo(jobId)
        try {
            syncManager.deleteChipLevel(jobId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("ManageJobUseCase", "Chip-level deletion upload failed", e)
        }
    }

    private suspend fun managePartsOrderState(jobId: Long, remarks: String, status: String) {
        val detectedParts = PartsKeywordDetector.detectParts(remarks)
        if (detectedParts.isNotEmpty() && status !in listOf("Delivered", "Return", "Chip-Level")) {
            val existing = partsOrderRepo.getState(jobId)
            if (existing == null) {
                partsOrderRepo.saveState(PartsOrderState(jobId = jobId, detectedParts = detectedParts))
            }
        } else if (status in listOf("Delivered", "Return")) {
            partsOrderRepo.clearState(jobId)
        }
    }
}
