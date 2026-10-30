package com.example.jobsheetbook.util

import com.example.jobsheetbook.data.local.dao.JobDao
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DailyReportGenerator {

    suspend fun generateReport(
        jobDao: JobDao,
        chipLevelRepo: ChipLevelRepository
    ): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val today = calendar.time
        
        val dateStr = dateFormat.format(today)
        val dayStr = dayFormat.format(today)

        val jobsReceived = jobDao.getJobsCreatedToday(startOfDay, endOfDay)
        val jobsReady = jobDao.getAllReadyJobs()
        val jobsDelivered = jobDao.getJobsDeliveredToday(startOfDay, endOfDay)
        val jobsChipLevel = jobDao.getJobsSentToChipLevelToday(startOfDay, endOfDay)

        val sb = java.lang.StringBuilder()
        sb.appendLine("$dateStr — $dayStr")
        sb.appendLine("───")
        sb.appendLine()

        sb.appendLine("📥 JOBS RECEIVED TODAY (${jobsReceived.size})")
        sb.appendLine("─────────────────────────────")
        if (jobsReceived.isEmpty()) {
            sb.appendLine("(None today)")
        } else {
            for (job in jobsReceived) {
                val displayId = job.displayId
                sb.appendLine("#$displayId | ${job.customerName}")
                sb.appendLine("${job.itemType}: ${job.make} ${job.model}")
                sb.appendLine("Issue: ${job.remarks}")
                sb.appendLine("Status: ${job.status}")
                if (job.finalCost > 0) {
                    sb.appendLine("Final Cost: ₹${job.finalCost}")
                }
                sb.appendLine()
            }
        }
        sb.appendLine()

        sb.appendLine("🟡 READY FOR PICKUP (${jobsReady.size})")
        sb.appendLine("─────────────────────────────")
        if (jobsReady.isEmpty()) {
            sb.appendLine("(None today)")
        } else {
            for (job in jobsReady) {
                val displayId = job.displayId
                sb.appendLine("#$displayId | ${job.customerName}")
                sb.appendLine("${job.itemType}: ${job.make} ${job.model}")
                sb.appendLine("Issue: ${job.remarks}")
                if (job.finalCost > 0) {
                    sb.appendLine("Final Cost: ₹${job.finalCost}")
                }
                sb.appendLine()
            }
        }
        sb.appendLine()

        sb.appendLine("✅ COMPLETED & DELIVERED TODAY (${jobsDelivered.size})")
        sb.appendLine("─────────────────────────────")
        if (jobsDelivered.isEmpty()) {
            sb.appendLine("(None today)")
        } else {
            for (job in jobsDelivered) {
                val displayId = job.displayId
                sb.appendLine("#$displayId | ${job.customerName}")
                sb.appendLine("${job.itemType}: ${job.make} ${job.model}")
                if (job.finalCost > 0) {
                    sb.appendLine("Final Cost: ₹${job.finalCost}")
                }
                sb.appendLine()
            }
        }
        sb.appendLine()

        sb.appendLine("🔧 CHIP-LEVEL SERVICES (${jobsChipLevel.size})")
        sb.appendLine("─────────────────────────────")
        if (jobsChipLevel.isEmpty()) {
            sb.appendLine("(None today)")
        } else {
            for (job in jobsChipLevel) {
                val displayId = job.displayId
                val chipLevelInfo = chipLevelRepo.getChipLevelInfo(job.jobId)
                val tmJobId = chipLevelInfo?.tmJobId ?: "--"
                val tmCost = chipLevelInfo?.tmCost ?: 0
                val tmStatus = TechMechStatusUtil.getOutcomeLabel(job.status)
                
                sb.appendLine("#$displayId | ${job.customerName}")
                sb.appendLine("${job.itemType}: ${job.make} ${job.model}")
                sb.appendLine("TM Job#: $tmJobId | Cost: ₹$tmCost")
                sb.appendLine("TM Status: $tmStatus")
                sb.appendLine()
            }
        }

        return sb.toString().trimEnd()
    }

    /**
     * Generates a plain-text delivery report for jobs delivered within [startDate, endDate].
     * Uses the actual delivery date (from job_events STATUS_CHANGE) rather than createdAt.
     * Resolves workDone and finalCost from ChipLevelInfo when the job went through chip-level.
     */
    suspend fun generateRangeReport(
        jobDao: JobDao,
        chipLevelRepo: ChipLevelRepository,
        startDate: Long,
        endDate: Long
    ): String {
        // DatePicker returns UTC midnight for the selected dates.
        // We must extract the year/month/day in UTC and apply it to a local Calendar.
        val startUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startDate }
        val startCal = Calendar.getInstance().apply {
            set(startUtc.get(Calendar.YEAR), startUtc.get(Calendar.MONTH), startUtc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = endDate }
        val endCal = Calendar.getInstance().apply {
            set(endUtc.get(Calendar.YEAR), endUtc.get(Calendar.MONTH), endUtc.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startOfRange = startCal.timeInMillis
        val endOfRange = endCal.timeInMillis

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timestampFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val deliveredJobs = jobDao.getDeliveredJobsInRange(startOfRange, endOfRange)

        val sb = StringBuilder()
        sb.appendLine("DELIVERY REPORT: ${dateFormat.format(startOfRange)} – ${dateFormat.format(endOfRange)}")
        sb.appendLine("Admin Info Solution")
        sb.appendLine("Generated: ${timestampFormat.format(System.currentTimeMillis())}")
        sb.appendLine()
        sb.appendLine("────────────────────────")

        if (deliveredJobs.isEmpty()) {
            sb.appendLine()
            sb.appendLine("No jobs delivered in this period.")
        } else {
            var totalRevenue = 0

            for (item in deliveredJobs) {
                val job = item.job
                val chipInfo = chipLevelRepo.getChipLevelInfo(job.jobId)

                // Resolve Work Done: chip-level workDone > job remarks > dash
                val workDone = when {
                    chipInfo != null && chipInfo.workDone.isNotBlank() -> chipInfo.workDone
                    job.remarks.isNotBlank() -> job.remarks
                    else -> "—"
                }

                // Resolve Final Cost: job finalCost > chip-level finalCost > 0
                val finalCost = when {
                    job.finalCost > 0 -> job.finalCost
                    chipInfo != null && chipInfo.finalCost > 0 -> chipInfo.finalCost
                    else -> 0
                }
                totalRevenue += finalCost

                sb.appendLine()
                sb.appendLine(dateFormat.format(item.deliveredAt))
                sb.appendLine("Job #${job.displayId}")
                sb.appendLine("Work Done: $workDone")
                sb.appendLine("Final Cost: ${if (finalCost > 0) "₹$finalCost" else "—"}")
            }

            sb.appendLine()
            sb.appendLine("────────────────────────")
            sb.appendLine("Total Jobs Delivered: ${deliveredJobs.size}")
            sb.appendLine("Total Revenue: ₹$totalRevenue")
        }

        return sb.toString().trimEnd()
    }
}
