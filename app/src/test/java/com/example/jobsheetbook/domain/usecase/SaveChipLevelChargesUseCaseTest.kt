package com.example.jobsheetbook.domain.usecase

import com.example.jobsheetbook.data.local.entities.JobEventEntity
import com.example.jobsheetbook.data.repository.ChipLevelInfo
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.example.jobsheetbook.data.repository.JobRepository
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.domain.models.JobStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveChipLevelChargesUseCaseTest {

    private val chipLevelRepo = mockk<ChipLevelRepository>(relaxed = true)
    private val jobRepository = mockk<JobRepository>(relaxed = true)
    private val manageJobUseCase = mockk<ManageJobUseCase>(relaxed = true)

    private lateinit var useCase: SaveChipLevelChargesUseCase

    @Before
    fun setup() {
        useCase = SaveChipLevelChargesUseCase(chipLevelRepo, jobRepository, manageJobUseCase)
    }

    private fun mockJob(jobId: Long = 1L, finalCost: Int = 0): Job = Job(
        jobId = jobId,
        customerPhone = "1234567890",
        customerName = "Test Customer",
        createdAt = 1000L,
        itemType = "Laptop",
        make = "Dell",
        model = "Inspiron",
        serialNumber = "SN123",
        devicePassword = "",
        remarks = "Issue",
        status = JobStatus.CHIP_LEVEL,
        rawStatus = "Chip-Level",
        imagePaths = emptyList(),
        accessories = emptyMap(),
        jobTypes = emptyList(),
        dueDate = null,
        pendingReason = "",
        estimatedCost = 0,
        finalCost = finalCost,
        lastModified = 1000L,
        isDeleted = false,
        displayJobId = null,
        isArchived = false
    )

    @Test
    fun `updates job finalCost when changed and uploads info and note event`() = runTest {
        val job = mockJob(finalCost = 500)
        coEvery { jobRepository.getJobById(1L) } returns job
        coEvery { chipLevelRepo.getChipLevelInfo(1L) } returns null

        useCase.execute(
            jobId = 1L,
            tmJobId = "TM-101",
            workDone = "Replaced IC",
            tmCost = 1200,
            finalCost = 2500
        )

        // Verify chipLevelRepo save
        val infoSlot = slot<ChipLevelInfo>()
        coVerify { chipLevelRepo.saveChipLevelInfo(capture(infoSlot)) }
        assertEquals("TM-101", infoSlot.captured.tmJobId)
        assertEquals("Replaced IC", infoSlot.captured.workDone)
        assertEquals(1200, infoSlot.captured.tmCost)
        assertEquals(2500, infoSlot.captured.finalCost)
        assertTrue(infoSlot.captured.pickedUp)

        // Verify upload to remote
        coVerify { jobRepository.uploadChipLevelInfo(infoSlot.captured) }

        // Verify note event
        val eventSlot = slot<JobEventEntity>()
        coVerify { jobRepository.insertEvent(capture(eventSlot)) }
        assertEquals("NOTE", eventSlot.captured.eventType)
        assertTrue(eventSlot.captured.description.contains("1200"))
        assertTrue(eventSlot.captured.description.contains("Replaced IC"))

        // Verify job finalCost update
        val updatedJobSlot = slot<Job>()
        coVerify(exactly = 1) { manageJobUseCase.updateJob(capture(updatedJobSlot)) }
        assertEquals(2500, updatedJobSlot.captured.finalCost)
    }

    @Test
    fun `does not update job when finalCost is unchanged`() = runTest {
        val job = mockJob(finalCost = 2000)
        coEvery { jobRepository.getJobById(1L) } returns job
        coEvery { chipLevelRepo.getChipLevelInfo(1L) } returns null

        useCase.execute(
            jobId = 1L,
            tmJobId = "TM-102",
            workDone = "Short cleared",
            tmCost = 1000,
            finalCost = 2000
        )

        coVerify { chipLevelRepo.saveChipLevelInfo(any()) }
        coVerify { jobRepository.uploadChipLevelInfo(any()) }
        coVerify { jobRepository.insertEvent(any()) }
        coVerify(exactly = 0) { manageJobUseCase.updateJob(any()) }
    }

    @Test
    fun `preserves existing pickedUp and pickedUpAt and tmStatus from previous record`() = runTest {
        val existingInfo = ChipLevelInfo(
            jobId = 1L,
            tmJobId = "OLD-1",
            tmCost = 500,
            finalCost = 1000,
            workDone = "Old work",
            tmStatus = "In Hand",
            pickedUp = true,
            pickedUpAt = 99999L
        )
        coEvery { chipLevelRepo.getChipLevelInfo(1L) } returns existingInfo
        coEvery { jobRepository.getJobById(1L) } returns mockJob(finalCost = 1000)

        useCase.execute(
            jobId = 1L,
            tmJobId = "NEW-1",
            workDone = "New work",
            tmCost = 800,
            finalCost = 1000
        )

        val infoSlot = slot<ChipLevelInfo>()
        coVerify { chipLevelRepo.saveChipLevelInfo(capture(infoSlot)) }
        assertEquals("In Hand", infoSlot.captured.tmStatus)
        assertTrue(infoSlot.captured.pickedUp)
        assertEquals(99999L, infoSlot.captured.pickedUpAt)
        assertEquals("NEW-1", infoSlot.captured.tmJobId)
        assertEquals(800, infoSlot.captured.tmCost)
    }

    private fun isSaveDetailsEnabled(
        originalInfo: ChipLevelInfo,
        originalJobFinalCost: Int,
        newTmJobId: String,
        newWorkDone: String,
        newTmCost: Int,
        newFinalCost: Int
    ): Boolean {
        val isFieldChanged = newTmJobId != originalInfo.tmJobId ||
                newWorkDone != originalInfo.workDone ||
                newTmCost != originalInfo.tmCost ||
                newFinalCost != originalInfo.finalCost
        val isFinalCostMismatch = newFinalCost != originalJobFinalCost
        return isFieldChanged || isFinalCostMismatch
    }

    @Test
    fun `details sheet save button enabled logic behaves correctly`() {
        val info = ChipLevelInfo(1L, "TM-1", 500, 1000, "Done", "Not Marked")

        // 1. Completely unchanged and job.finalCost == info.finalCost -> disabled
        assertFalse(isSaveDetailsEnabled(info, 1000, "TM-1", "Done", 500, 1000))

        // 2. Unchanged fields but job.finalCost mismatch -> enabled
        assertTrue(isSaveDetailsEnabled(info, 0, "TM-1", "Done", 500, 1000))

        // 3. tmCost changed -> enabled
        assertTrue(isSaveDetailsEnabled(info, 1000, "TM-1", "Done", 600, 1000))

        // 4. finalCost changed -> enabled
        assertTrue(isSaveDetailsEnabled(info, 1000, "TM-1", "Done", 500, 1500))

        // 5. tmJobId changed -> enabled
        assertTrue(isSaveDetailsEnabled(info, 1000, "TM-2", "Done", 500, 1000))

        // 6. workDone changed -> enabled
        assertTrue(isSaveDetailsEnabled(info, 1000, "TM-1", "New Work", 500, 1000))
    }
}
