package com.example.jobsheetbook.domain.policy

import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.domain.models.JobStatus
import javax.inject.Inject

class StatusTransitionPolicy @Inject constructor() {

    companion object {
        private val phaseScore = mapOf(
            JobStatus.RECEIVED to 1,
            JobStatus.IN_PROGRESS to 2,
            JobStatus.READY to 3,
            JobStatus.CHIP_LEVEL to 3,
            JobStatus.PENDING to 3,
            JobStatus.DELIVERED to 4,
            JobStatus.RETURN to 4
        )

        private val orderMap = mapOf(
            WarningReason.LeavingTerminal to 1,
            WarningReason.SkippedStep to 3,
            WarningReason.BackwardMove to 4,
            WarningReason.ZeroCost to 5
        )
    }

    fun evaluate(job: Job, newStatus: JobStatus, trackerFinalCost: Int? = null): TransitionResult {
        val fromStatus = mapLegacy(job.status)
        val toStatus = mapLegacy(newStatus)

        if (fromStatus == toStatus) return TransitionResult.Allowed

        val reasons = mutableListOf<WarningReason>()

        if (fromStatus == JobStatus.DELIVERED || fromStatus == JobStatus.RETURN) {
            reasons.add(WarningReason.LeavingTerminal)
        }

        val fromScore = phaseScore[fromStatus] ?: 0
        val toScore = phaseScore[toStatus] ?: 0

        val isLeavingTerminal = reasons.contains(WarningReason.LeavingTerminal)
        val isTargetReturn = toStatus == JobStatus.RETURN

        if (!isLeavingTerminal && !isTargetReturn) {
            if (fromScore > 0 && toScore > 0) {
                if (fromStatus == JobStatus.PENDING && toStatus == JobStatus.IN_PROGRESS) {
                    // Allowed exception
                } else if (fromScore > toScore) {
                    reasons.add(WarningReason.BackwardMove)
                } else if (toScore - fromScore > 1) {
                    reasons.add(WarningReason.SkippedStep)
                }
            }

            if (fromStatus == JobStatus.READY && toStatus == JobStatus.CHIP_LEVEL) {
                reasons.add(WarningReason.BackwardMove)
            }
        }

        if (toStatus == JobStatus.DELIVERED && fromStatus != JobStatus.DELIVERED) {
            if (job.finalCost == 0 && (trackerFinalCost ?: 0) == 0) {
                reasons.add(WarningReason.ZeroCost)
            }
        }

        if (reasons.isEmpty()) {
            return TransitionResult.Allowed
        }

        val sortedReasons = reasons.sortedBy { orderMap[it] ?: 99 }.distinct()
        val level = if (sortedReasons.contains(WarningReason.LeavingTerminal)) {
            WarningLevel.STRONG
        } else {
            WarningLevel.SOFT
        }

        return TransitionResult.Warn(level, sortedReasons)
    }

    private fun mapLegacy(status: JobStatus): JobStatus {
        return when (status) {
            JobStatus.SENT_OUT, JobStatus.RETURNED_AWAITING_DELIVERY -> JobStatus.CHIP_LEVEL
            else -> status
        }
    }
}
