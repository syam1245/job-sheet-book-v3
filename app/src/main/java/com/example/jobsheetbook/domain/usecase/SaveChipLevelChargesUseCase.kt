package com.example.jobsheetbook.domain.usecase

import com.example.jobsheetbook.data.local.entities.JobEventEntity
import com.example.jobsheetbook.data.repository.ChipLevelInfo
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.example.jobsheetbook.data.repository.JobRepository
import javax.inject.Inject

class SaveChipLevelChargesUseCase @Inject constructor(
    private val chipLevelRepo: ChipLevelRepository,
    private val jobRepository: JobRepository,
    private val manageJobUseCase: ManageJobUseCase
) {
    suspend fun execute(
        jobId: Long,
        tmJobId: String,
        workDone: String,
        tmCost: Int,
        finalCost: Int
    ) {
        val existing = chipLevelRepo.getChipLevelInfo(jobId)
        val info = ChipLevelInfo(
            jobId = jobId,
            tmJobId = tmJobId,
            tmCost = tmCost,
            finalCost = finalCost,
            workDone = workDone,
            tmStatus = existing?.tmStatus ?: "Not Marked",
            pickedUp = existing?.pickedUp ?: true,
            pickedUpAt = existing?.pickedUpAt ?: System.currentTimeMillis()
        )
        chipLevelRepo.saveChipLevelInfo(info)
        jobRepository.uploadChipLevelInfo(info)
        jobRepository.insertEvent(
            JobEventEntity(
                jobId = jobId,
                eventType = "NOTE",
                description = "TechMech Update: Cost set to ₹$tmCost / Notes added: $workDone"
            )
        )

        val job = jobRepository.getJobById(jobId)
        if (job != null && job.finalCost != finalCost) {
            manageJobUseCase.updateJob(job.copy(finalCost = finalCost))
        }
    }
}
