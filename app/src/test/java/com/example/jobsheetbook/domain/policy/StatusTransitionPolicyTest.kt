package com.example.jobsheetbook.domain.policy

import com.example.jobsheetbook.domain.models.Job
import com.example.jobsheetbook.domain.models.JobStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatusTransitionPolicyTest {

    private lateinit var policy: StatusTransitionPolicy

    @Before
    fun setup() {
        policy = StatusTransitionPolicy()
    }

    private fun mockJob(status: JobStatus, finalCost: Int = 100): Job {
        return Job(
            jobId = 1L, customerPhone = "", customerName = "", createdAt = 0L, itemType = "",
            make = "", model = "", serialNumber = "", devicePassword = "", remarks = "",
            status = status, rawStatus = status.value, imagePaths = emptyList(),
            accessories = emptyMap(), jobTypes = emptyList(), dueDate = null,
            pendingReason = "", estimatedCost = 0, finalCost = finalCost, lastModified = 0L,
            isDeleted = false, displayJobId = null, isArchived = false
        )
    }

    private fun getBaseExpectedMatrix(): Map<Pair<JobStatus, JobStatus>, List<WarningReason>> {
        return mapOf(
            Pair(JobStatus.RECEIVED, JobStatus.RECEIVED) to emptyList(),
            Pair(JobStatus.RECEIVED, JobStatus.IN_PROGRESS) to emptyList(),
            Pair(JobStatus.RECEIVED, JobStatus.READY) to listOf(WarningReason.SkippedStep),
            Pair(JobStatus.RECEIVED, JobStatus.CHIP_LEVEL) to listOf(WarningReason.SkippedStep),
            Pair(JobStatus.RECEIVED, JobStatus.PENDING) to listOf(WarningReason.SkippedStep),
            Pair(JobStatus.RECEIVED, JobStatus.DELIVERED) to listOf(WarningReason.SkippedStep),
            Pair(JobStatus.RECEIVED, JobStatus.RETURN) to emptyList(),

            Pair(JobStatus.IN_PROGRESS, JobStatus.RECEIVED) to listOf(WarningReason.BackwardMove),
            Pair(JobStatus.IN_PROGRESS, JobStatus.IN_PROGRESS) to emptyList(),
            Pair(JobStatus.IN_PROGRESS, JobStatus.READY) to emptyList(),
            Pair(JobStatus.IN_PROGRESS, JobStatus.CHIP_LEVEL) to emptyList(),
            Pair(JobStatus.IN_PROGRESS, JobStatus.PENDING) to emptyList(),
            Pair(JobStatus.IN_PROGRESS, JobStatus.DELIVERED) to listOf(WarningReason.SkippedStep),
            Pair(JobStatus.IN_PROGRESS, JobStatus.RETURN) to emptyList(),

            Pair(JobStatus.READY, JobStatus.RECEIVED) to listOf(WarningReason.BackwardMove),
            Pair(JobStatus.READY, JobStatus.IN_PROGRESS) to listOf(WarningReason.BackwardMove),
            Pair(JobStatus.READY, JobStatus.READY) to emptyList(),
            Pair(JobStatus.READY, JobStatus.CHIP_LEVEL) to listOf(WarningReason.BackwardMove),
            Pair(JobStatus.READY, JobStatus.PENDING) to emptyList(),
            Pair(JobStatus.READY, JobStatus.DELIVERED) to emptyList(),
            Pair(JobStatus.READY, JobStatus.RETURN) to emptyList(),

            Pair(JobStatus.CHIP_LEVEL, JobStatus.RECEIVED) to listOf(WarningReason.BackwardMove),
            Pair(JobStatus.CHIP_LEVEL, JobStatus.IN_PROGRESS) to listOf(WarningReason.BackwardMove),
            Pair(JobStatus.CHIP_LEVEL, JobStatus.READY) to emptyList(),
            Pair(JobStatus.CHIP_LEVEL, JobStatus.CHIP_LEVEL) to emptyList(),
            Pair(JobStatus.CHIP_LEVEL, JobStatus.PENDING) to emptyList(),
            Pair(JobStatus.CHIP_LEVEL, JobStatus.DELIVERED) to emptyList(),
            Pair(JobStatus.CHIP_LEVEL, JobStatus.RETURN) to emptyList(),

            Pair(JobStatus.PENDING, JobStatus.RECEIVED) to listOf(WarningReason.BackwardMove),
            Pair(JobStatus.PENDING, JobStatus.IN_PROGRESS) to emptyList(),
            Pair(JobStatus.PENDING, JobStatus.READY) to emptyList(),
            Pair(JobStatus.PENDING, JobStatus.CHIP_LEVEL) to emptyList(),
            Pair(JobStatus.PENDING, JobStatus.PENDING) to emptyList(),
            Pair(JobStatus.PENDING, JobStatus.DELIVERED) to emptyList(),
            Pair(JobStatus.PENDING, JobStatus.RETURN) to emptyList(),

            Pair(JobStatus.DELIVERED, JobStatus.RECEIVED) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.DELIVERED, JobStatus.IN_PROGRESS) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.DELIVERED, JobStatus.READY) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.DELIVERED, JobStatus.CHIP_LEVEL) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.DELIVERED, JobStatus.PENDING) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.DELIVERED, JobStatus.DELIVERED) to emptyList(),
            Pair(JobStatus.DELIVERED, JobStatus.RETURN) to listOf(WarningReason.LeavingTerminal),

            Pair(JobStatus.RETURN, JobStatus.RECEIVED) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.RETURN, JobStatus.IN_PROGRESS) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.RETURN, JobStatus.READY) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.RETURN, JobStatus.CHIP_LEVEL) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.RETURN, JobStatus.PENDING) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.RETURN, JobStatus.DELIVERED) to listOf(WarningReason.LeavingTerminal),
            Pair(JobStatus.RETURN, JobStatus.RETURN) to emptyList()
        )
    }

    @Test
    fun `test matrix of all 49 cells for normal valid paths`() {
        val allStatuses = JobStatus.values().toList()
        val baseMatrix = getBaseExpectedMatrix()

        for (from in allStatuses) {
            for (to in allStatuses) {
                if (!baseMatrix.containsKey(Pair(from, to))) continue

                val expected = baseMatrix[Pair(from, to)]!!.toMutableList()
                val sortedExpected = expected.sortedBy { it.javaClass.simpleName }

                val result = policy.evaluate(mockJob(from), to, null)
                
                if (expected.isEmpty()) {
                    assertEquals("Expected Allowed for $from -> $to", TransitionResult.Allowed, result)
                } else {
                    assertTrue("Expected Warn for $from -> $to", result is TransitionResult.Warn)
                    val warn = result as TransitionResult.Warn
                    assertEquals("Reasons mismatch for $from -> $to", sortedExpected, warn.reasons.sortedBy { it.javaClass.simpleName })
                }
            }
        }
    }

    @Test
    fun `test ZeroCost respects job and tracker costs`() {
        // job 0 with tracker 1500 gives no ZeroCost
        val r1 = policy.evaluate(mockJob(JobStatus.READY, finalCost = 0), JobStatus.DELIVERED, 1500)
        assertEquals(TransitionResult.Allowed, r1)
        
        // job 1500 with tracker 0 gives none
        val r2 = policy.evaluate(mockJob(JobStatus.READY, finalCost = 1500), JobStatus.DELIVERED, 0)
        assertEquals(TransitionResult.Allowed, r2)
        
        // both 0 gives ZeroCost
        val r3 = policy.evaluate(mockJob(JobStatus.READY, finalCost = 0), JobStatus.DELIVERED, 0)
        assertTrue(r3 is TransitionResult.Warn)
        assertTrue((r3 as TransitionResult.Warn).reasons.contains(WarningReason.ZeroCost))
        
        // tracker null with job 0 gives ZeroCost
        val r4 = policy.evaluate(mockJob(JobStatus.READY, finalCost = 0), JobStatus.DELIVERED, null)
        assertTrue(r4 is TransitionResult.Warn)
        assertTrue((r4 as TransitionResult.Warn).reasons.contains(WarningReason.ZeroCost))
    }

    @Test
    fun `test legacy status mapping behaves as CHIP_LEVEL`() {
        val result1 = policy.evaluate(mockJob(JobStatus.SENT_OUT), JobStatus.READY, 100)
        assertEquals(TransitionResult.Allowed, result1)

        val result2 = policy.evaluate(mockJob(JobStatus.RETURNED_AWAITING_DELIVERY), JobStatus.RECEIVED, 100)
        assertTrue(result2 is TransitionResult.Warn)
        assertTrue((result2 as TransitionResult.Warn).reasons.contains(WarningReason.BackwardMove))
    }

    @Test
    fun `test multi-reason ordering and level calculation`() {
        val result = policy.evaluate(mockJob(JobStatus.DELIVERED, finalCost = 0), JobStatus.RECEIVED, 0)
        assertTrue(result is TransitionResult.Warn)
        val warn = result as TransitionResult.Warn
        assertEquals(WarningLevel.STRONG, warn.level)
        // Order: LeavingTerminal (1), others omitted
        assertEquals(listOf(WarningReason.LeavingTerminal), warn.reasons)

        val result2 = policy.evaluate(mockJob(JobStatus.IN_PROGRESS, finalCost = 0), JobStatus.DELIVERED, 0)
        assertTrue(result2 is TransitionResult.Warn)
        val warn2 = result2 as TransitionResult.Warn
        assertEquals(WarningLevel.SOFT, warn2.level)
        // Order: SkippedStep (3), ZeroCost (5)
        assertEquals(listOf(WarningReason.SkippedStep, WarningReason.ZeroCost), warn2.reasons)
    }
}
