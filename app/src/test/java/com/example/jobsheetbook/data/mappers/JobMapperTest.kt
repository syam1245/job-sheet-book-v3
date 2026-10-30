package com.example.jobsheetbook.data.mappers

import com.example.jobsheetbook.data.local.entities.JobEntity
import com.example.jobsheetbook.domain.models.JobStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class JobMapperTest {

    @Test
    fun `toDomain maps basic fields correctly`() {
        val entity = JobEntity(
            jobId = 1L,
            customerPhone = "1234567890",
            customerName = "John Doe",
            createdAt = 1000L,
            itemType = "Laptop",
            make = "Dell",
            model = "XPS",
            serialNumber = "SN123",
            devicePassword = "pass",
            remarks = "Broken screen",
            status = "Work in-progress",
            imagePaths = listOf("path1", "path2"),
            accessories = mapOf("adapter" to true),
            jobTypes = listOf("HW Repair"),
            dueDate = 2000L,
            pendingReason = "Waiting for parts",
            estimatedCost = 5000,
            finalCost = 0,
            lastModified = 1500L,
            isDeleted = false,
            displayJobId = "D-00001",
            isArchived = false
        )

        val domain = entity.toDomain()

        assertEquals(1L, domain.jobId)
        assertEquals(JobStatus.IN_PROGRESS, domain.status)
        assertEquals("Work in-progress", domain.rawStatus)
        assertEquals("John Doe", domain.customerName)
        assertEquals(listOf("path1", "path2"), domain.imagePaths)
        assertEquals(mapOf("adapter" to true), domain.accessories)
    }

    @Test
    fun `toEntity maps back correctly preserving unknown status`() {
        val entity = JobEntity(
            jobId = 1L,
            customerPhone = "123",
            customerName = "Jane",
            itemType = "PC",
            make = "HP",
            model = "Pavilion",
            serialNumber = "",
            devicePassword = "",
            remarks = "",
            status = "Some weird legacy status" // UNKNOWN
        )

        val domain = entity.toDomain()
        assertEquals(JobStatus.UNKNOWN, domain.status)
        assertEquals("Some weird legacy status", domain.rawStatus)

        val mappedBack = domain.toEntity()
        assertEquals("Some weird legacy status", mappedBack.status)
    }

    @Test
    fun `Blank name maps to Walk-in through the mapper`() {
        val entity = JobEntity(
            jobId = 1L,
            customerPhone = "",
            customerName = "",
            createdAt = 1000L,
            itemType = "Laptop",
            make = "Dell",
            model = "XPS",
            serialNumber = "",
            devicePassword = "",
            remarks = "",
            status = "Delivered",
            imagePaths = emptyList(),
            accessories = emptyMap(),
            jobTypes = emptyList(),
            dueDate = null,
            pendingReason = "",
            estimatedCost = 0,
            finalCost = 1000,
            lastModified = 1500L,
            isDeleted = false,
            displayJobId = null,
            isArchived = false
        )

        val domain = entity.toDomain()
        assertEquals("Walk-in", domain.customerName)
    }
}
