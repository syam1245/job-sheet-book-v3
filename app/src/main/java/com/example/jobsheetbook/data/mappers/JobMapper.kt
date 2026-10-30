package com.example.jobsheetbook.data.mappers

import com.example.jobsheetbook.data.local.entities.JobEntity
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.domain.models.JobStatus
import com.example.jobsheetbook.domain.models.WALK_IN_NAME

fun JobEntity.toDomain(): Job {
    return Job(
        jobId = this.jobId,
        customerPhone = this.customerPhone,
        customerName = this.customerName.ifBlank { WALK_IN_NAME },
        createdAt = this.createdAt,
        itemType = this.itemType,
        make = this.make,
        model = this.model,
        serialNumber = this.serialNumber,
        devicePassword = this.devicePassword,
        remarks = this.remarks,
        status = JobStatus.fromString(this.status),
        rawStatus = this.status,
        imagePaths = this.imagePaths,
        accessories = this.accessories,
        jobTypes = this.jobTypes,
        dueDate = this.dueDate,
        pendingReason = this.pendingReason,
        estimatedCost = this.estimatedCost,
        finalCost = this.finalCost,
        lastModified = this.lastModified,
        isDeleted = this.isDeleted,
        displayJobId = this.displayJobId,
        isArchived = this.isArchived
    )
}

fun Job.toEntity(): JobEntity {
    return JobEntity(
        jobId = this.jobId,
        customerPhone = this.customerPhone,
        customerName = this.customerName,
        createdAt = this.createdAt,
        itemType = this.itemType,
        make = this.make,
        model = this.model,
        serialNumber = this.serialNumber,
        devicePassword = this.devicePassword,
        remarks = this.remarks,
        status = if (this.status == JobStatus.UNKNOWN) this.rawStatus else this.status.value,
        imagePaths = this.imagePaths,
        accessories = this.accessories,
        jobTypes = this.jobTypes,
        dueDate = this.dueDate,
        pendingReason = this.pendingReason,
        estimatedCost = this.estimatedCost,
        finalCost = this.finalCost,
        lastModified = this.lastModified,
        isDeleted = this.isDeleted,
        displayJobId = this.displayJobId,
        isArchived = this.isArchived
    )
}
