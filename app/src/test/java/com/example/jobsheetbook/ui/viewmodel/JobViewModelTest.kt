package com.example.jobsheetbook.ui.viewmodel

import com.example.jobsheetbook.data.local.AppPreferences
import com.example.jobsheetbook.data.local.entities.CustomerEntity
import com.example.jobsheetbook.data.local.entities.JobEventEntity
import com.example.jobsheetbook.data.repository.*
import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.domain.models.JobStatus
import com.example.jobsheetbook.domain.policy.StatusTransitionPolicy
import com.example.jobsheetbook.domain.policy.TransitionResult
import com.example.jobsheetbook.domain.usecase.GenerateReportUseCase
import com.example.jobsheetbook.domain.usecase.ManageJobUseCase
import com.example.jobsheetbook.util.DataExportManager
import com.example.jobsheetbook.util.TechMechEnquiryMessage
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobViewModelTest {

    private lateinit var viewModel: JobViewModel
    private val repository = mockk<JobRepository>(relaxed = true)
    private val chipLevelRepo = mockk<ChipLevelRepository>(relaxed = true)
    private val appPreferences = mockk<AppPreferences>(relaxed = true)
    private val notificationStateRepo = mockk<NotificationStateRepository>(relaxed = true)
    private val partsOrderRepo = mockk<PartsOrderRepository>(relaxed = true)
    private val dataExportManager = mockk<DataExportManager>(relaxed = true)
    private val manageJobUseCase = mockk<ManageJobUseCase>(relaxed = true)
    private val generateReportUseCase = mockk<GenerateReportUseCase>(relaxed = true)
    private val transitionPolicy = mockk<StatusTransitionPolicy>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { appPreferences.themeMode } returns MutableStateFlow(com.example.jobsheetbook.data.local.ThemeMode.SYSTEM)
        viewModel = JobViewModel(
            repository, chipLevelRepo, appPreferences, notificationStateRepo,
            partsOrderRepo, dataExportManager, manageJobUseCase, generateReportUseCase,
            transitionPolicy, testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockJob(status: JobStatus, isArchived: Boolean = false): Job {
        return Job(
            jobId = 1L, customerPhone = "", customerName = "", createdAt = 0L, itemType = "",
            make = "", model = "", serialNumber = "", devicePassword = "", remarks = "",
            status = status, rawStatus = status.value, imagePaths = emptyList(),
            accessories = emptyMap(), jobTypes = emptyList(), dueDate = null,
            pendingReason = "", estimatedCost = 0, finalCost = 0, lastModified = 0L,
            isDeleted = false, displayJobId = null, isArchived = isArchived
        )
    }

    @Test
    fun `Allowed path calls use case once and emits no pending state`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.RECEIVED)
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        every { transitionPolicy.evaluate(any(), any(), any()) } returns TransitionResult.Allowed

        viewModel.updateJobStatus(job, JobStatus.IN_PROGRESS.value, "Note")
        advanceUntilIdle()

        assertNull(viewModel.pendingStatusTransition.value)
        coVerify(exactly = 1) { manageJobUseCase.updateJob(any()) }
    }

    @Test
    fun `Warn path emits pending and makes zero use case calls`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.DELIVERED)
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        val reasons = listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal)
        every { transitionPolicy.evaluate(any(), any(), any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, reasons)

        viewModel.updateJobStatus(job, JobStatus.READY.value, "Reopen note")
        advanceUntilIdle()

        val pending = viewModel.pendingStatusTransition.value
        assertNotNull(pending)
        assertEquals(JobStatus.READY, pending?.to)
        assertEquals(JobStatus.DELIVERED, pending?.from)
        coVerify(exactly = 0) { manageJobUseCase.updateJob(any()) }
    }

    @Test
    fun `confirm applies once and clears`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.DELIVERED)
        coEvery { repository.getJobById(1L) } returns job
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        val reasons = listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal)
        every { transitionPolicy.evaluate(any(), any(), any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, reasons)

        viewModel.pendingStatusTransition.test {
            assertNull(awaitItem())

            viewModel.updateJobStatus(job, JobStatus.READY.value, "Reopen note")
            val pending = awaitItem()
            assertNotNull(pending)
            assertEquals(JobStatus.DELIVERED, pending?.from)
            assertEquals(JobStatus.READY, pending?.to)

            viewModel.confirmStatusTransition()
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        coVerify(exactly = 1) { manageJobUseCase.updateJob(any()) }
    }

    @Test
    fun `cancel clears with zero use case calls`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.DELIVERED)
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        val reasons = listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal)
        every { transitionPolicy.evaluate(any(), any(), any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, reasons)

        viewModel.pendingStatusTransition.test {
            assertNull(awaitItem())

            viewModel.updateJobStatus(job, JobStatus.READY.value, "Note")
            val pending = awaitItem()
            assertNotNull(pending)

            viewModel.cancelStatusTransition()
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        coVerify(exactly = 0) { manageJobUseCase.updateJob(any()) }
    }

    @Test
    fun `strong override note prefix`() = runTest(testDispatcher) {
        // Assert prefix does not contain restricted words
        val prefix = JobViewModel.POLICY_OVERRIDE_PREFIX
        assertFalse(prefix.contains("delivered", ignoreCase = true))
        assertFalse(prefix.contains("return", ignoreCase = true))
        assertFalse(prefix.contains("chip-level", ignoreCase = true))

        val job = mockJob(JobStatus.DELIVERED)
        coEvery { repository.getJobById(1L) } returns job
        
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        val reasons = listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal)
        every { transitionPolicy.evaluate(any(), any(), any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, reasons)
        
        viewModel.updateJobStatus(job, JobStatus.READY.value, "Note")
        advanceUntilIdle()
        viewModel.confirmStatusTransition()
        advanceUntilIdle()

        val eventSlot = slot<com.example.jobsheetbook.data.local.entities.JobEventEntity>()
        coVerify { repository.insertEvent(capture(eventSlot)) }
        assertTrue(eventSlot.captured.description.startsWith(JobViewModel.POLICY_OVERRIDE_PREFIX))
    }

    @Test
    fun `isArchived resets to false on reopen and stays as-is for terminal targets`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.DELIVERED, isArchived = true)
        coEvery { repository.getJobById(1L) } returns job
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        
        // Reopen to READY
        every { transitionPolicy.evaluate(any(), JobStatus.READY, any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal))
        viewModel.updateJobStatus(job, JobStatus.READY.value, "Note")
        advanceUntilIdle()
        viewModel.confirmStatusTransition()
        advanceUntilIdle()
        
        // DELIVERED to RETURN stays archived
        every { transitionPolicy.evaluate(any(), JobStatus.RETURN, any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal))
        viewModel.updateJobStatus(job, JobStatus.RETURN.value, "Note")
        advanceUntilIdle()
        viewModel.confirmStatusTransition()
        advanceUntilIdle()
        
        val jobUpdates = mutableListOf<Job>()
        coVerify(exactly = 2) { manageJobUseCase.updateJob(capture(jobUpdates)) }
        assertFalse(jobUpdates[0].isArchived)
        assertTrue(jobUpdates[1].isArchived)
    }
    @Test
    fun `stale from re-evaluates`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.DELIVERED)
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        every { transitionPolicy.evaluate(any(), any(), any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal))
        viewModel.updateJobStatus(job, JobStatus.READY.value, "Note")
        advanceUntilIdle()

        // Sync changed status to RETURN in the background
        val staleJob = mockJob(JobStatus.RETURN)
        coEvery { repository.getJobById(1L) } returns staleJob
        
        viewModel.confirmStatusTransition()
        advanceUntilIdle()

        // It should clear pending and re-evaluate updateJobStatus, which calls policy again with RETURN
        assertNotNull(viewModel.pendingStatusTransition.value)
        assertEquals(JobStatus.RETURN, viewModel.pendingStatusTransition.value?.from)
        coVerify(exactly = 0) { manageJobUseCase.updateJob(any()) }
    }

    @Test
    fun `ZeroCost reads the tracker cost and null tracker`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.READY)
        
        // Case 1: tracker exists
        val trackerInfo = ChipLevelInfo(jobId = 1L, tmJobId = "1", tmCost = 100, finalCost = 1500, workDone = "Done", tmStatus = "Delivered", pickedUp = false, pickedUpAt = null)
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns trackerInfo
        every { transitionPolicy.evaluate(any(), any(), any()) } returns TransitionResult.Allowed
        
        viewModel.updateJobStatus(job, JobStatus.DELIVERED.value, "Note")
        advanceUntilIdle()
        
        verify { transitionPolicy.evaluate(any(), JobStatus.DELIVERED, 1500) }
        
        // Case 2: tracker null
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        viewModel.updateJobStatus(job, JobStatus.DELIVERED.value, "Note")
        advanceUntilIdle()
        
        verify { transitionPolicy.evaluate(any(), JobStatus.DELIVERED, null) }
    }

    @Test
    fun `confirm double tap updates once`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.DELIVERED)
        coEvery { repository.getJobById(1L) } returns job
        
        coEvery { chipLevelRepo.getChipLevelInfo(any()) } returns null
        val reasons = listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal)
        every { transitionPolicy.evaluate(any(), any(), any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, reasons)
        
        viewModel.updateJobStatus(job, JobStatus.READY.value, "Note")
        advanceUntilIdle()
        assertNotNull(viewModel.pendingStatusTransition.value)
        
        // Double tap
        viewModel.confirmStatusTransition()
        viewModel.confirmStatusTransition()
        advanceUntilIdle()

        coVerify(exactly = 1) { manageJobUseCase.updateJob(any()) }
        coVerify(exactly = 1) { repository.insertEvent(any()) }
    }

    @Test
    fun `same status selection does nothing`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.RECEIVED)
        viewModel.updateJobStatus(job, JobStatus.RECEIVED.value, "Note")
        advanceUntilIdle()

        verify(exactly = 0) { transitionPolicy.evaluate(any(), any(), any()) }
        coVerify(exactly = 0) { manageJobUseCase.updateJob(any()) }
        coVerify(exactly = 0) { repository.insertEvent(any()) }
        assertNull(viewModel.pendingStatusTransition.value)
    }

    @Test
    fun `event description formatting Allowed Ready change with note`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.RECEIVED)
        every { transitionPolicy.evaluate(any(), JobStatus.READY, any()) } returns TransitionResult.Allowed
        viewModel.updateJobStatus(job, JobStatus.READY.value, "abc")
        advanceUntilIdle()

        val eventSlot = slot<com.example.jobsheetbook.data.local.entities.JobEventEntity>()
        coVerify(exactly = 1) { repository.insertEvent(capture(eventSlot)) }
        assertEquals("Ready. Note: abc", eventSlot.captured.description)
        assertFalse(eventSlot.captured.description.contains("$"))
    }

    @Test
    fun `event description formatting Strong override to Delivered with note`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.RETURN)
        coEvery { repository.getJobById(1L) } returns job
        every { transitionPolicy.evaluate(any(), JobStatus.DELIVERED, any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal))
        
        viewModel.updateJobStatus(job, JobStatus.DELIVERED.value, "abc")
        advanceUntilIdle()
        viewModel.confirmStatusTransition()
        advanceUntilIdle()

        val eventSlot = slot<com.example.jobsheetbook.data.local.entities.JobEventEntity>()
        coVerify(exactly = 1) { repository.insertEvent(capture(eventSlot)) }
        assertEquals("[Policy Override] Delivered (Final handover date & time). Note: abc", eventSlot.captured.description)
        assertFalse(eventSlot.captured.description.contains("$"))
    }

    @Test
    fun `event description formatting Override to Ready with an empty note`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.DELIVERED)
        coEvery { repository.getJobById(1L) } returns job
        every { transitionPolicy.evaluate(any(), JobStatus.READY, any()) } returns TransitionResult.Warn(com.example.jobsheetbook.domain.policy.WarningLevel.STRONG, listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal))
        
        viewModel.updateJobStatus(job, JobStatus.READY.value, "")
        advanceUntilIdle()
        viewModel.confirmStatusTransition()
        advanceUntilIdle()

        val eventSlot = slot<com.example.jobsheetbook.data.local.entities.JobEventEntity>()
        coVerify(exactly = 1) { repository.insertEvent(capture(eventSlot)) }
        assertEquals("[Policy Override] Ready", eventSlot.captured.description)
        assertFalse(eventSlot.captured.description.contains("$"))
    }

    @Test
    fun `Allowed move to Chip-Level emits OpenChipLevelDialog event once`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.IN_PROGRESS)
        every { transitionPolicy.evaluate(any(), JobStatus.CHIP_LEVEL, any()) } returns TransitionResult.Allowed

        viewModel.uiEvents.test {
            viewModel.updateJobStatus(job, JobStatus.CHIP_LEVEL.value)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is JobUiEvent.OpenChipLevelDialog)
            assertEquals(1L, (event as JobUiEvent.OpenChipLevelDialog).jobId)
            expectNoEvents()
        }
    }

    @Test
    fun `warned move to Chip-Level emits none until confirm and cancel emits none`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.DELIVERED)
        coEvery { repository.getJobById(1L) } returns job
        every { transitionPolicy.evaluate(any(), JobStatus.CHIP_LEVEL, any()) } returns TransitionResult.Warn(
            com.example.jobsheetbook.domain.policy.WarningLevel.STRONG,
            listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal)
        )

        // Case 1: Cancel emits none
        viewModel.uiEvents.test {
            viewModel.updateJobStatus(job, JobStatus.CHIP_LEVEL.value)
            advanceUntilIdle()
            expectNoEvents()

            viewModel.cancelStatusTransition()
            advanceUntilIdle()
            expectNoEvents()
        }

        // Case 2: Confirm emits once
        viewModel.uiEvents.test {
            viewModel.updateJobStatus(job, JobStatus.CHIP_LEVEL.value)
            advanceUntilIdle()
            expectNoEvents()

            viewModel.confirmStatusTransition()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is JobUiEvent.OpenChipLevelDialog)
            assertEquals(1L, (event as JobUiEvent.OpenChipLevelDialog).jobId)
            expectNoEvents()
        }
    }

    @Test
    fun `reselect Chip-Level while already Chip-Level emits OpenChipLevelDialog once`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.CHIP_LEVEL)

        viewModel.uiEvents.test {
            viewModel.updateJobStatus(job, JobStatus.CHIP_LEVEL.value)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is JobUiEvent.OpenChipLevelDialog)
            assertEquals(1L, (event as JobUiEvent.OpenChipLevelDialog).jobId)
            expectNoEvents()
        }

        verify(exactly = 0) { transitionPolicy.evaluate(any(), any(), any()) }
        coVerify(exactly = 0) { manageJobUseCase.updateJob(any()) }
    }

    @Test
    fun `new pending transition set during confirm survives`() = runTest(testDispatcher) {
        val job1 = mockJob(JobStatus.DELIVERED).copy(jobId = 1L)
        val job2 = mockJob(JobStatus.RECEIVED).copy(jobId = 2L)
        
        coEvery { repository.getJobById(1L) } returns job1
        every { transitionPolicy.evaluate(job1, JobStatus.READY, any()) } returns TransitionResult.Warn(
            com.example.jobsheetbook.domain.policy.WarningLevel.STRONG,
            listOf(com.example.jobsheetbook.domain.policy.WarningReason.LeavingTerminal)
        )
        every { transitionPolicy.evaluate(job2, JobStatus.READY, any()) } returns TransitionResult.Warn(
            com.example.jobsheetbook.domain.policy.WarningLevel.SOFT,
            listOf(com.example.jobsheetbook.domain.policy.WarningReason.SkippedStep)
        )

        // 1. Trigger transition for job1
        viewModel.updateJobStatus(job1, JobStatus.READY.value)
        advanceUntilIdle()
        assertEquals(1L, viewModel.pendingStatusTransition.value?.jobId)

        // 2. Start confirm for job1 (which calls getAndUpdate { null } immediately)
        viewModel.confirmStatusTransition()
        assertNull(viewModel.pendingStatusTransition.value)

        // 3. Before confirm finishes, set new pending transition for job2
        viewModel.updateJobStatus(job2, JobStatus.READY.value)
        advanceUntilIdle()

        // 4. Assert job2's pending transition survives and was not cleared by confirm
        assertNotNull(viewModel.pendingStatusTransition.value)
        assertEquals(2L, viewModel.pendingStatusTransition.value?.jobId)
    }

    @Test
    fun `Quick save calls saveJob once with status DELIVERED, inserts exactly two events with the exact descriptions, and creates no customer record on a blank phone`() = runTest(testDispatcher) {
        coEvery { repository.getNextJobId() } returns 100L
        coEvery { repository.getJobByIdIncludingDeleted(100L) } returns null

        viewModel.setQuickMode(true)
        viewModel.onJobIdChange("D-00100")
        viewModel.onCustomerNameChange("")
        viewModel.onCustomerPhoneChange("")
        viewModel.onFinalCostChange("2500")
        viewModel.onRemarksChange("Screen replacement service done")

        assertTrue(viewModel.isFormValid())

        val jobSlot = slot<Job>()
        val events = mutableListOf<JobEventEntity>()
        coEvery { manageJobUseCase.saveJob(capture(jobSlot)) } returns 100L
        coEvery { repository.insertEvent(capture(events)) } just Runs

        var savedJobId: Long? = null
        viewModel.saveJob { savedJobId = it }
        advanceUntilIdle()

        assertEquals(100L, savedJobId)
        coVerify(exactly = 1) { manageJobUseCase.saveJob(any()) }
        assertEquals(JobStatus.DELIVERED, jobSlot.captured.status)
        assertEquals("Delivered", jobSlot.captured.rawStatus)
        assertEquals(2500, jobSlot.captured.finalCost)
        assertEquals("", jobSlot.captured.customerName)
        assertEquals("", jobSlot.captured.customerPhone)

        // Verifies exactly two events inserted: Creation event and Delivered event
        assertEquals(2, events.size)
        assertEquals("STATUS_CHANGE", events[0].eventType)
        assertEquals("Job created (Quick job)", events[0].description)
        assertFalse(events[0].description.contains("Delivered"))
        assertFalse(events[0].description.contains("Return"))
        assertFalse(events[0].description.contains("Chip-Level"))
        assertFalse(events[0].description.contains("Screen replacement"))

        assertEquals("STATUS_CHANGE", events[1].eventType)
        assertEquals("Delivered (Final handover date & time)", events[1].description)
        assertFalse(events[1].description.contains("Screen replacement"))

        // Verifies NO customer record created on blank phone
        coVerify(exactly = 0) { repository.saveCustomer(any()) }
    }

    @Test
    fun `Quick mode - phone 12 invalid, blank phone valid, 10-digit phone valid`() = runTest(testDispatcher) {
        viewModel.setQuickMode(true)
        viewModel.onJobIdChange("D-00100")
        viewModel.onCustomerNameChange("")

        // 1. Blank phone is valid
        viewModel.onCustomerPhoneChange("")
        assertTrue(viewModel.isFormValid())

        // 2. Short phone "12" is invalid
        viewModel.onCustomerPhoneChange("12")
        assertFalse(viewModel.isFormValid())

        // 3. 10-digit phone is valid
        viewModel.onCustomerPhoneChange("9876543210")
        assertTrue(viewModel.isFormValid())
    }

    @Test
    fun `Full mode - blank name or short phone still invalid, valid name and 10-digit phone is valid`() = runTest(testDispatcher) {
        viewModel.setQuickMode(false)
        viewModel.onJobIdChange("D-00100")

        // 1. Blank name with 10-digit phone is invalid
        viewModel.onCustomerNameChange("")
        viewModel.onCustomerPhoneChange("9876543210")
        assertFalse(viewModel.isFormValid())

        // 2. Short phone with valid name is invalid
        viewModel.onCustomerNameChange("Alice")
        viewModel.onCustomerPhoneChange("12")
        assertFalse(viewModel.isFormValid())

        // 3. Blank phone with valid name is invalid
        viewModel.onCustomerPhoneChange("")
        assertFalse(viewModel.isFormValid())

        // 4. Valid name and 10-digit phone is valid
        viewModel.onCustomerPhoneChange("9876543210")
        assertTrue(viewModel.isFormValid())

        // 5. Valid international phone is valid
        viewModel.onCustomerPhoneChange("+971 50 123 4567")
        assertTrue(viewModel.isFormValid())

        // 6. Formatted Indian phone is valid
        viewModel.onCustomerPhoneChange("+91 98765 43210")
        assertTrue(viewModel.isFormValid())

        // 7. Invalid phone with valid name is invalid
        viewModel.onCustomerPhoneChange("12345")
        assertFalse(viewModel.isFormValid())
    }

    @Test
    fun `Editing a Quick job with blank name and phone saves with the name still blank`() = runTest(testDispatcher) {
        val existingQuickJob = mockJob(JobStatus.DELIVERED).copy(
            jobId = 77L,
            customerPhone = "",
            customerName = "Walk-in",
            displayJobId = "D-00077"
        )
        coEvery { repository.getJobById(77L) } returns existingQuickJob
        coEvery { repository.getJobByIdIncludingDeleted(77L) } returns existingQuickJob

        viewModel.loadJobForEditing(77L)
        advanceUntilIdle()

        assertTrue(viewModel.jobFormUiState.value.isEditing)
        assertTrue(viewModel.jobFormUiState.value.originalPhoneWasBlank)
        // Name field should be displayed empty so "Walk-in" is not written back into entity
        assertEquals("", viewModel.jobFormUiState.value.customerName)
        assertEquals("", viewModel.jobFormUiState.value.customerPhone)
        assertTrue(viewModel.isFormValid())

        val updateSlot = slot<Job>()
        coEvery { manageJobUseCase.updateJob(capture(updateSlot)) } just Runs

        viewModel.saveJob {}
        advanceUntilIdle()

        coVerify(exactly = 1) { manageJobUseCase.updateJob(any()) }
        assertEquals("", updateSlot.captured.customerName)
        assertEquals("", updateSlot.captured.customerPhone)
        coVerify(exactly = 0) { repository.saveCustomer(any()) }
    }

    @Test
    fun `Editing a Quick job with a newly typed 10-digit phone saves`() = runTest(testDispatcher) {
        val existingQuickJob = mockJob(JobStatus.DELIVERED).copy(
            jobId = 88L,
            customerPhone = "",
            customerName = "Walk-in",
            displayJobId = "D-00088"
        )
        coEvery { repository.getJobById(88L) } returns existingQuickJob
        coEvery { repository.getJobByIdIncludingDeleted(88L) } returns existingQuickJob

        viewModel.loadJobForEditing(88L)
        advanceUntilIdle()

        assertTrue(viewModel.jobFormUiState.value.originalPhoneWasBlank)
        // Type a new 10-digit phone
        viewModel.onCustomerPhoneChange("9123456789")
        assertTrue(viewModel.isFormValid())

        val updateSlot = slot<Job>()
        coEvery { manageJobUseCase.updateJob(capture(updateSlot)) } just Runs

        viewModel.saveJob {}
        advanceUntilIdle()

        coVerify(exactly = 1) { manageJobUseCase.updateJob(any()) }
        assertEquals("9123456789", updateSlot.captured.customerPhone)
        coVerify(exactly = 1) { repository.saveCustomer(match { it.phoneNumber == "9123456789" }) }
    }

    @Test
    fun `resetForm resets isQuickMode to false and restores clean state`() = runTest(testDispatcher) {
        viewModel.setQuickMode(true)
        viewModel.onCustomerPhoneChange("12345")
        assertTrue(viewModel.jobFormUiState.value.isQuickMode)

        coEvery { repository.getNextJobId() } returns 123L
        viewModel.resetForm()
        advanceUntilIdle()

        assertFalse(viewModel.jobFormUiState.value.isQuickMode)
        assertFalse(viewModel.jobFormUiState.value.originalPhoneWasBlank)
        assertEquals("D-00123", viewModel.jobFormUiState.value.jobIdInput)
        assertEquals("", viewModel.jobFormUiState.value.customerPhone)
    }

    @Test
    fun `onJobCardLongClick opens enquiry sheet only for Chip-Level status jobs`() = runTest(testDispatcher) {
        val testLabels = TechMechEnquiryMessage.Labels(
            jobNo = "Job no:",
            customer = "Customer:",
            device = "Device:",
            problem = "Problem:",
            techMechRef = "TechMech ref:",
            closing = "Estimation & Status?",
            notRecorded = "Not recorded",
            walkIn = "Walk-in"
        )

        // Non-chip-level jobs should NOT trigger enquiry state
        val readyJob = mockJob(JobStatus.READY).copy(displayJobId = "D-00001")
        viewModel.onJobCardLongClick(readyJob, testLabels)
        advanceUntilIdle()
        assertNull(viewModel.techMechEnquiryState.value)

        val receivedJob = mockJob(JobStatus.RECEIVED).copy(displayJobId = "D-00002")
        viewModel.onJobCardLongClick(receivedJob, testLabels)
        advanceUntilIdle()
        assertNull(viewModel.techMechEnquiryState.value)

        // Chip-level job status SHOULD trigger enquiry state
        val chipLevelJob = mockJob(JobStatus.CHIP_LEVEL).copy(displayJobId = "D-00003")
        viewModel.onJobCardLongClick(chipLevelJob, testLabels)
        advanceUntilIdle()
        assertNotNull(viewModel.techMechEnquiryState.value)
        assertEquals("D-00003", viewModel.techMechEnquiryState.value?.job?.displayId)
        assertTrue(viewModel.techMechEnquiryState.value?.message?.contains("Job no: D-00003") == true)

        // Dismiss enquiry
        viewModel.dismissTechMechEnquiry()
        assertNull(viewModel.techMechEnquiryState.value)

        // Sent out job SHOULD trigger enquiry state
        val sentOutJob = mockJob(JobStatus.SENT_OUT).copy(displayJobId = "D-00004")
        viewModel.onJobCardLongClick(sentOutJob, testLabels)
        advanceUntilIdle()
        assertNotNull(viewModel.techMechEnquiryState.value)
        assertEquals("D-00004", viewModel.techMechEnquiryState.value?.job?.displayId)

        // Returned awaiting delivery job SHOULD trigger enquiry state
        val returnedJob = mockJob(JobStatus.RETURNED_AWAITING_DELIVERY).copy(displayJobId = "D-00005")
        viewModel.onJobCardLongClick(returnedJob, testLabels)
        advanceUntilIdle()
        assertNotNull(viewModel.techMechEnquiryState.value)
        assertEquals("D-00005", viewModel.techMechEnquiryState.value?.job?.displayId)
    }

    @Test
    fun `loadJobForEditing sets hasTrackerRow true when tracker record exists`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.CHIP_LEVEL).copy(jobId = 42L, finalCost = 2500)
        coEvery { repository.getJobById(42L) } returns job
        val trackerInfo = ChipLevelInfo(jobId = 42L, tmJobId = "TM-42", tmCost = 1000, finalCost = 2500, workDone = "Replaced IC", tmStatus = "Not Marked")
        coEvery { chipLevelRepo.getChipLevelInfo(42L) } returns trackerInfo

        viewModel.loadJobForEditing(42L)
        advanceUntilIdle()

        val state = viewModel.jobFormUiState.value
        assertTrue(state.isEditing)
        assertEquals(42L, state.editingJobId)
        assertTrue(state.hasTrackerRow)
        assertEquals(2500, state.finalCost)
    }

    @Test
    fun `loadJobForEditing sets hasTrackerRow false when no tracker record exists`() = runTest(testDispatcher) {
        val job = mockJob(JobStatus.RECEIVED).copy(jobId = 43L, finalCost = 1500)
        coEvery { repository.getJobById(43L) } returns job
        coEvery { chipLevelRepo.getChipLevelInfo(43L) } returns null

        viewModel.loadJobForEditing(43L)
        advanceUntilIdle()

        val state = viewModel.jobFormUiState.value
        assertTrue(state.isEditing)
        assertEquals(43L, state.editingJobId)
        assertFalse(state.hasTrackerRow)
        assertEquals(1500, state.finalCost)
    }
}
