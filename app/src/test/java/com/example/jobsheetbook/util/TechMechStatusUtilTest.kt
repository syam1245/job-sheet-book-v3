package com.example.jobsheetbook.util

import com.example.jobsheetbook.domain.models.JobStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class TechMechStatusUtilTest {

    private val now = 1000000000000L

    private fun daysAgo(days: Int): Long =
        now - TimeUnit.DAYS.toMillis(days.toLong())

    @Test
    fun `getFollowUpState returns NONE when not at TechMech`() {
        assertEquals(FollowUpState.NONE, TechMechStatusUtil.getFollowUpState(false, daysAgo(10), now))
        assertEquals(FollowUpState.NONE, TechMechStatusUtil.getFollowUpState(false, daysAgo(4), now))
        assertEquals(FollowUpState.NONE, TechMechStatusUtil.getFollowUpState(false, daysAgo(0), now))
    }

    @Test
    fun `getFollowUpState returns NONE when elapsed days is 0-2`() {
        assertEquals(FollowUpState.NONE, TechMechStatusUtil.getFollowUpState(true, daysAgo(0), now))
        assertEquals(FollowUpState.NONE, TechMechStatusUtil.getFollowUpState(true, daysAgo(1), now))
        assertEquals(FollowUpState.NONE, TechMechStatusUtil.getFollowUpState(true, daysAgo(2), now))
    }

    @Test
    fun `getFollowUpState returns NEEDS_FOLLOW_UP when elapsed days is 3-6`() {
        assertEquals(FollowUpState.NEEDS_FOLLOW_UP, TechMechStatusUtil.getFollowUpState(true, daysAgo(3), now))
        assertEquals(FollowUpState.NEEDS_FOLLOW_UP, TechMechStatusUtil.getFollowUpState(true, daysAgo(4), now))
        assertEquals(FollowUpState.NEEDS_FOLLOW_UP, TechMechStatusUtil.getFollowUpState(true, daysAgo(6), now))
    }

    @Test
    fun `getFollowUpState returns OVERDUE when elapsed days is 7 or more`() {
        assertEquals(FollowUpState.OVERDUE, TechMechStatusUtil.getFollowUpState(true, daysAgo(7), now))
        assertEquals(FollowUpState.OVERDUE, TechMechStatusUtil.getFollowUpState(true, daysAgo(14), now))
    }

    @Test
    fun `getOutcomeLabel maps statuses correctly`() {
        assertEquals("At TechMech", TechMechStatusUtil.getOutcomeLabel(JobStatus.CHIP_LEVEL))
        assertEquals("At TechMech", TechMechStatusUtil.getOutcomeLabel(JobStatus.SENT_OUT))
        assertEquals("Repaired", TechMechStatusUtil.getOutcomeLabel(JobStatus.READY))
        assertEquals("Not repaired", TechMechStatusUtil.getOutcomeLabel(JobStatus.RETURN))
        assertEquals("Delivered", TechMechStatusUtil.getOutcomeLabel(JobStatus.DELIVERED))
        assertEquals("Back at shop", TechMechStatusUtil.getOutcomeLabel(JobStatus.RETURNED_AWAITING_DELIVERY))
        assertEquals("Back at shop", TechMechStatusUtil.getOutcomeLabel(JobStatus.RECEIVED))
        assertEquals("Back at shop", TechMechStatusUtil.getOutcomeLabel(JobStatus.IN_PROGRESS))
        assertEquals("Back at shop", TechMechStatusUtil.getOutcomeLabel(JobStatus.PENDING))
        assertEquals("Back at shop", TechMechStatusUtil.getOutcomeLabel(JobStatus.UNKNOWN))

        // String overloads
        assertEquals("At TechMech", TechMechStatusUtil.getOutcomeLabel("Chip-Level"))
        assertEquals("At TechMech", TechMechStatusUtil.getOutcomeLabel("Sent Out"))
        assertEquals("Repaired", TechMechStatusUtil.getOutcomeLabel("Ready"))
        assertEquals("Not repaired", TechMechStatusUtil.getOutcomeLabel("Return"))
        assertEquals("Delivered", TechMechStatusUtil.getOutcomeLabel("Delivered"))
        assertEquals("Back at shop", TechMechStatusUtil.getOutcomeLabel("Received"))
        assertEquals("Back at shop", TechMechStatusUtil.getOutcomeLabel("Pending"))
        assertEquals("Back at shop", TechMechStatusUtil.getOutcomeLabel(null))
    }
}
