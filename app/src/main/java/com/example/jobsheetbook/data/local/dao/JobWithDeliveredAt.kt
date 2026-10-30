package com.example.jobsheetbook.data.local.dao

import androidx.room.Embedded
import com.example.jobsheetbook.data.local.entities.JobEntity

/**
 * Projection class for the delivered-jobs-in-range query.
 * Room maps `j.*` columns to the embedded [JobEntity], and the computed
 * `deliveredAt` column (COALESCE of the STATUS_CHANGE event timestamp
 * or the job's lastModified) to the standalone Long field.
 */
data class JobWithDeliveredAt(
    @Embedded val job: JobEntity,
    val deliveredAt: Long
)
