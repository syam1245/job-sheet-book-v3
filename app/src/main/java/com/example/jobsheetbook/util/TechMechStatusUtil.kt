package com.example.jobsheetbook.util

import com.example.jobsheetbook.domain.models.JobStatus
import java.util.concurrent.TimeUnit

/**
 * Shared utility for TechMech follow-up/overdue state and outcome labels.
 *
 * Single source of truth for the 3-day / 7-day thresholds used by both:
 *  - ChipLevelTrackerScreen (pipeline summary + card border accents)
 *  - NotificationWorker Rules 3A / 3C
 */
enum class FollowUpState { NONE, NEEDS_FOLLOW_UP, OVERDUE }

object TechMechStatusUtil {

    /**
     * Returns the number of whole days elapsed between [timestamp] and [now].
     */
    fun daysSince(timestamp: Long, now: Long = System.currentTimeMillis()): Int =
        TimeUnit.MILLISECONDS.toDays(now - timestamp).toInt()

    /**
     * Derives the follow-up urgency for a job.
     *
     * Rules:
     *  - If the job is not at TechMech -> NONE
     *  - If at TechMech and elapsed days >= 7 -> OVERDUE
     *  - If at TechMech and elapsed days >= 3 -> NEEDS_FOLLOW_UP
     *  - Otherwise -> NONE
     */
    fun getFollowUpState(
        isAtTechMech: Boolean,
        sinceMillis: Long,
        now: Long = System.currentTimeMillis()
    ): FollowUpState {
        if (!isAtTechMech) return FollowUpState.NONE
        val days = daysSince(sinceMillis, now)
        return when {
            days >= 7 -> FollowUpState.OVERDUE
            days >= 3 -> FollowUpState.NEEDS_FOLLOW_UP
            else      -> FollowUpState.NONE
        }
    }

    /**
     * Maps job status to an outcome label for TechMech tracking and reports.
     * Chip-Level: "At TechMech"
     * Ready: "Repaired"
     * Return: "Not repaired"
     * Delivered: "Delivered"
     * Any other status: "Back at shop"
     */
    fun getOutcomeLabel(status: JobStatus): String = when (status) {
        JobStatus.CHIP_LEVEL, JobStatus.SENT_OUT -> "At TechMech"
        JobStatus.READY -> "Repaired"
        JobStatus.RETURN -> "Not repaired"
        JobStatus.DELIVERED -> "Delivered"
        JobStatus.RETURNED_AWAITING_DELIVERY,
        JobStatus.RECEIVED,
        JobStatus.IN_PROGRESS,
        JobStatus.PENDING,
        JobStatus.UNKNOWN -> "Back at shop"
    }

    fun getOutcomeLabel(status: String?): String =
        getOutcomeLabel(JobStatus.fromString(status))
}

