package com.example.jobsheetbook.domain.models

const val WALK_IN_NAME = "Walk-in"

data class Job(
    val jobId: Long,
    val customerPhone: String,
    val customerName: String,
    val createdAt: Long,
    val itemType: String,
    val make: String,
    val model: String,
    val serialNumber: String,
    val devicePassword: String,
    val remarks: String,
    val status: JobStatus,
    val rawStatus: String,
    val imagePaths: List<String>,
    val accessories: Map<String, Boolean>,
    val jobTypes: List<String>,
    val dueDate: Long?,
    val pendingReason: String,
    val estimatedCost: Int,
    val finalCost: Int,
    val lastModified: Long,
    val isDeleted: Boolean,
    val displayJobId: String?,
    val isArchived: Boolean
) {
    val displayId: String
        get() = if (!displayJobId.isNullOrBlank()) displayJobId else String.format("D-%05d", jobId)
}
