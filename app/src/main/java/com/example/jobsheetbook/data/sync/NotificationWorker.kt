package com.example.jobsheetbook.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.jobsheetbook.MainActivity
import com.example.jobsheetbook.R
import com.example.jobsheetbook.data.local.dao.JobDao
import com.example.jobsheetbook.data.local.dao.JobEventDao
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.example.jobsheetbook.data.repository.NotificationStateRepository
import com.example.jobsheetbook.data.repository.PartsOrderRepository
import com.example.jobsheetbook.util.FollowUpState
import com.example.jobsheetbook.util.TechMechStatusUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val jobDao: JobDao,
    private val jobEventDao: JobEventDao,
    private val chipLevelRepo: ChipLevelRepository,
    private val notificationStateRepo: NotificationStateRepository,
    private val partsOrderRepo: PartsOrderRepository
) : CoroutineWorker(context, params) {

    /**
     * Single source of truth for working hours enforcement.
     * Mon–Sat (Calendar.MONDAY=2 .. Calendar.SATURDAY=7), 10:00 AM–7:59 PM.
     * Notifications must NEVER fire outside this window for any rule type.
     */
    private fun isWithinWorkingHours(): Boolean {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return hour in 10..19 && day in Calendar.MONDAY..Calendar.SATURDAY
    }

    private suspend fun getJobsMissingFinalCostCombined(
        jobDao: JobDao,
        chipLevelRepo: ChipLevelRepository
    ): List<com.example.jobsheetbook.data.local.entities.JobEntity> {
        val candidates = jobDao.getJobsMissingFinalCost()
        return candidates.filter { job ->
            val chipInfo = chipLevelRepo.getChipLevelInfo(job.jobId)
            val hasChipFinalCost = chipInfo?.finalCost?.let { it > 0 } ?: false
            !hasChipFinalCost // only flag if NEITHER source has a final cost
        }
    }

    override suspend fun doWork(): Result {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val ignoreTimeCheck = inputData.getBoolean("ignore_time_check", false)
        val isWorkingHours = isWithinWorkingHours()

        // ── Working-hours gate — MUST be first: no DAO queries, no channel creation before this ──
        if (!ignoreTimeCheck && !isWorkingHours) {
            return Result.success()
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val isMissingCostCheckTime = (currentHour == 10 && currentMinute in 25..35) ||
                                     (currentHour == 13 && currentMinute in 0..10) ||
                                     (currentHour == 19 && currentMinute in 0..10)

        if (isMissingCostCheckTime) {
            val missingCostJobs = getJobsMissingFinalCostCombined(jobDao, chipLevelRepo)
            if (missingCostJobs.isNotEmpty()) {
                val prefs = context.getSharedPreferences("missing_cost_prefs", Context.MODE_PRIVATE)
                val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                val slotKey = "last_missing_cost_notify_${currentHour}_${today}"
                
                if (!prefs.getBoolean(slotKey, false)) {
                    val title = "💰 ${missingCostJobs.size} Job(s) Missing Final Cost"
                    val jobList = missingCostJobs.take(5).joinToString("\n") { 
                        "• #${it.displayId} - ${it.customerName} (${it.status})" 
                    }
                    val body = if (missingCostJobs.size > 5) {
                        "$jobList\n...and ${missingCostJobs.size - 5} more. Update final cost before reporting."
                    } else {
                        "$jobList\nUpdate final cost before reporting."
                    }
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        if (missingCostJobs.size == 1) {
                            putExtra("navigate_to_job", missingCostJobs.first().jobId)
                        }
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        888888,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val builder = NotificationCompat.Builder(context, "job_alerts")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                    notificationManager.notify(888888, builder.build())
                    
                    prefs.edit().putBoolean(slotKey, true).apply()
                }
            }
        }


        val activeJobs = jobDao.getActiveJobsForNotification()
        val chipLevelData = chipLevelRepo.chipLevelData.value
        val lastStatusChangeByJobId = jobEventDao
            .getLastStatusChangeEvents(activeJobs.map { it.jobId })
            .associateBy { it.jobId }
        
        var qualifyingJobsCount = 0

        for (job in activeJobs) {
            // Skip if snoozed
            if (notificationStateRepo.isSnoozed(job.jobId)) continue

            val lastEvent = lastStatusChangeByJobId[job.jobId]
            val referenceTime = lastEvent?.timestamp ?: job.createdAt
            val daysElapsed = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - referenceTime).toInt()
            
            var title: String? = null
            var body: String? = null

            val displayId = job.displayId
            val customerName = job.customerName
            val make = job.make
            val model = job.model

            // FIX 1: Loosened string matching to prevent trailing spaces from breaking logic
            val currentStatus = job.status.trim()
            val isChipLevel = currentStatus.equals("Chip-Level", ignoreCase = true) || 
                              currentStatus.equals("Chip Level", ignoreCase = true) ||
                              currentStatus.contains("Chip", ignoreCase = true)

            val chipInfo = chipLevelData[job.jobId]
            val tmSince = chipInfo?.pickedUpAt ?: job.createdAt
            val tmDaysElapsed = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - tmSince).toInt()
            // Shared follow-up state — identical thresholds as ChipLevelTrackerScreen
            val followUpState = TechMechStatusUtil.getFollowUpState(
                isAtTechMech = isChipLevel,
                sinceMillis = tmSince
            )

            val dueDate = job.dueDate
            val daysUntilDue: Long? = if (dueDate != null) {
                val calDue = Calendar.getInstance().apply { timeInMillis = dueDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                val calToday = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                (calDue.timeInMillis - calToday.timeInMillis) / (24*60*60*1000L)
            } else null
            val partsState = partsOrderRepo.getState(job.jobId)
            val partsOrderStatus = partsState?.orderStatus

            when {
                // Rule 4A: Job Overdue
                dueDate != null && daysUntilDue!! < 0 && !currentStatus.equals("Ready", ignoreCase = true) && !currentStatus.equals("Delivered", ignoreCase = true) && !currentStatus.startsWith("Return", ignoreCase = true) -> {
                    title = "🚨 Due Date MISSED"
                    body = "Job #$displayId overdue by ${-daysUntilDue} days, customer may arrive."
                }
                // Rule 4B: Job Due Tomorrow — notify in morning (10-12) or afternoon (15-17)
                dueDate != null && daysUntilDue!! == 1L && !currentStatus.equals("Ready", ignoreCase = true) && !currentStatus.equals("Delivered", ignoreCase = true) && !currentStatus.startsWith("Return", ignoreCase = true) && (currentHour in 10..12 || currentHour in 15..17) -> {
                    title = "⚠️ Due Date Tomorrow"
                    body = "Job #$displayId must be ready by EOD tomorrow."
                }

                // Rule 4C: Job Due in 3 Days (Chip-Level) — notify during morning window
                dueDate != null && daysUntilDue!! in 0..3 && isChipLevel && currentHour in 10..12 -> {
                    title = "⚠️ Due in $daysUntilDue days"
                    body = "Follow up with TechMech for Job #$displayId, TechMech deadline is ${daysUntilDue - 2} days away."
                }
                // Rule 4D: Job Due in 2 Days (Parts Pending) — notify in morning or midday window
                dueDate != null && daysUntilDue!! in 0..2 && (partsOrderStatus.equals("Pending", ignoreCase = true) || partsOrderStatus.equals("Ordered", ignoreCase = true)) && (currentHour in 10..12 || currentHour in 13..15) -> {
                    title = "⚠️ Due in $daysUntilDue days"
                    body = "Parts still pending for Job #$displayId, may miss deadline."
                }
                // Rule 3C - Overdue at TechMech (delegated to TechMechStatusUtil)
                isChipLevel && followUpState == FollowUpState.OVERDUE -> {
                    title = "⚠️ TechMech Job Overdue"
                    body = "Job #$displayId | $customerName's $make $model has been at TechMech for $tmDaysElapsed days with NO update. Call TechMech immediately."
                }
                // Rule 3A - Needs follow-up at TechMech (delegated to TechMechStatusUtil)
                isChipLevel && followUpState == FollowUpState.NEEDS_FOLLOW_UP -> {
                    title = "TechMech Follow-Up Needed"
                    body = "Job #$displayId | $customerName's $make $model was sent to TechMech $tmDaysElapsed days ago with no update."
                }
                // Rule 2
                (currentStatus.equals("Work in-progress", ignoreCase = true) || currentStatus.equals("In Progress", ignoreCase = true)) && daysElapsed >= 4 -> {
                    title = "Repair Taking Longer Than Usual"
                    body = "Job #$displayId | $customerName's $make $model is in progress for $daysElapsed days. Any update on the repair?"
                }
                // Rule 4
                currentStatus.equals("Ready", ignoreCase = true) && daysElapsed >= 3 -> {
                    title = "Customer Not Collecting"
                    body = "Job #$displayId | $customerName's $make $model has been Ready for $daysElapsed days. Has the customer been contacted?"
                }
                // Rule 5
                currentStatus.equals("Pending", ignoreCase = true) && daysElapsed >= 5 -> {
                    title = "Pending Job Needs Attention"
                    body = "Job #$displayId | $customerName's $make $model has been on hold for $daysElapsed days. Is the pending reason resolved?"
                }
                // Rule 1
                currentStatus.equals("Received", ignoreCase = true) && daysElapsed >= 1 -> {
                    title = "Waiting for Work to Start"
                    body = "Job #$displayId | $customerName's $make $model has been sitting at Received for $daysElapsed day(s). Has work started yet?"
                }
            }

            if (title != null && body != null) {
                val count = notificationStateRepo.incrementCount(job.jobId)
                
                // FIX 3: Prevent Overdue/Critical alerts from being swallowed by the 5-limit auto-snooze
                val isCritical = title?.contains("Overdue") == true || title?.contains("Unpaid") == true || title?.contains("⚠️") == true || title?.contains("🚨") == true
                val isMedium = title?.contains("Stuck") == true || title?.contains("Follow-Up") == true || title?.contains("Due in") == true
                
                // Note: Rule 4C & 4D titles start with ⚠️ but are Medium severity. Let's adjust isCritical logic
                val isActuallyCritical = isCritical && !isMedium
                
                if (count > 5 && !isActuallyCritical) {
                    // Auto-snooze regular alerts after 5 fires
                    notificationStateRepo.snooze(job.jobId, weeks = 1)
                    continue
                } else if (count > 3 && isMedium) {
                    // Medium alerts snooze after 3 fires
                    notificationStateRepo.snooze(job.jobId, weeks = 2)
                    continue
                }

                qualifyingJobsCount++
                sendNotification(notificationManager, job.jobId.toInt(), job.jobId, title, body)
            }
        }

        // Set badge count if possible
        updateBadgeCount(qualifyingJobsCount, manager = notificationManager)

        // Parts order check loop
        val pendingParts = partsOrderRepo.getAllPendingStates()

        for (state in pendingParts) {
            val job = jobDao.getJobById(state.jobId) ?: continue
            val displayId = job.displayId
            val customerName = job.customerName
            val make = job.make
            val model = job.model
            val parts = state.detectedParts.joinToString(", ")
            
            var pTitle: String? = null
            var pBody: String? = null
            var notificationId: Int = 0

            val partsPendingDays = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - state.detectedAt
            ).toInt()

            val partsPrefs = context.getSharedPreferences(
                "parts_notify_prefs", Context.MODE_PRIVATE)
            val today = java.text.SimpleDateFormat(
                "yyyyMMdd", java.util.Locale.getDefault())
                .format(java.util.Date())

            val missedFridayKey = "friday_missed_notify_${state.jobId}"
            val detectedDay = Calendar.getInstance().apply { timeInMillis = state.detectedAt }
                .get(Calendar.DAY_OF_WEEK)
            if (state.orderStatus == "Pending" &&
                dayOfWeek == Calendar.MONDAY && currentHour == 10 &&
                detectedDay == Calendar.FRIDAY &&
                partsPrefs.getString(missedFridayKey, "") != today) {
                pTitle = "📦 Friday Order Missed"
                pBody = "Job #${displayId} | ${parts} not ordered Friday. Order now — parts arrive Monday. Complete job Monday from 10 AM."
                notificationId = (state.jobId * 10 + 3).toInt()
                partsPrefs.edit().putString(missedFridayKey, today).apply()
            } else if (state.confirmedOrder && state.orderStatus == "Pending" && partsPendingDays >= 3) {
                val lastDate = partsPrefs.getString("last_parts_notify_${state.jobId}", "")
                if (lastDate != today) {
                    pTitle = "📦 Parts Order Stuck (3+ Days)"
                    pBody = "Job #${displayId} | ${customerName}'s ${make} needs: ${parts}. Order placed ${partsPendingDays} days ago — follow up with supplier or cancel."
                    notificationId = (state.jobId * 10 + 6).toInt()
                    
                    partsPrefs.edit()
                        .putString("last_parts_notify_${state.jobId}", today)
                        .apply()
                }
            } else if (state.confirmedOrder &&
                state.orderStatus == "Pending" &&
                currentHour in 10..17) {
                // Only notify once per day — check lastPartsNotifiedDate
                val lastDate = partsPrefs.getString(
                    "last_parts_notify_${state.jobId}", "")
                
                if (lastDate != today) {
                    pTitle = "📦 Parts Order Pending"
                    pBody = "Job #${displayId} | ${customerName}'s ${make}: ${parts} order not placed yet. Open job to order via WhatsApp."
                    notificationId = (state.jobId * 10 + 5).toInt()
                    
                    // Save today's date so it only notifies once per day
                    partsPrefs.edit()
                        .putString("last_parts_notify_${state.jobId}", today)
                        .apply()
                }
            } else if (state.orderStatus == "Pending" && currentHour >= 19) {
                // "Courier Cutoff Passed" — fire once per day after 7 PM if not yet ordered
                val pastKey = "cutoffpast_notify_${state.jobId}"
                if (partsPrefs.getString(pastKey, "") != today) {
                    pTitle = "🚨 Courier Cutoff Passed!"
                    pBody = "Job #${displayId} | ${customerName}'s ${make} ${model}: ${parts} not ordered. Order first thing tomorrow morning."
                    notificationId = (state.jobId * 10 + 2).toInt()
                    partsPrefs.edit().putString(pastKey, today).apply()
                }
            } else if (state.orderStatus == "Pending" && currentHour >= 18) {
                // "Order Before Cutoff" — fire once per day after 6 PM if not yet ordered
                val cutoffKey = "cutoff_notify_${state.jobId}"
                if (partsPrefs.getString(cutoffKey, "") != today) {
                    pTitle = "⚠️ Order Parts Before Cutoff!"
                    pBody = "Job #${displayId} needs: ${parts}. Order before 7 PM or customer waits another day!"
                    notificationId = (state.jobId * 10 + 1).toInt()
                    partsPrefs.edit().putString(cutoffKey, today).apply()
                }
            } else if (state.orderStatus == "Ordered" && dayOfWeek == 2 && currentHour == 10) {
                val orderedDay = if (state.orderedAt != null) Calendar.getInstance().apply { timeInMillis = state.orderedAt }.get(Calendar.DAY_OF_WEEK) else -1
                if (orderedDay == 7) { // Saturday
                    pTitle = "📬 Parts Should Arrive Today"
                    pBody = "Job #${displayId} | ${customerName}'s ${make} ${model}: ${parts} ordered Saturday. Check delivery and start repair."
                    notificationId = (state.jobId * 10 + 4).toInt()
                }
            }

            if (pTitle != null && pBody != null) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigate_to_job", state.jobId)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, notificationId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, "job_alerts")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(pTitle)
                    .setContentText(pBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(pBody))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .addAction(0, "View Job", pendingIntent)

                notificationManager.notify(notificationId, builder.build())
            }
        }

        // Remind at 7:30 PM (19:30) if report not yet generated today
        if (currentHour == 19 && currentMinute >= 30) {
            val reportPrefs = context.getSharedPreferences(
                "report_prefs", Context.MODE_PRIVATE)
            val lastReportDate = reportPrefs.getString("last_report_date", "")
            val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                .format(java.util.Date())
            
            if (lastReportDate != today) {
                sendReportReminderNotification(notificationManager)
            }
        }

        return Result.success()
    }

    private fun sendReportReminderNotification(manager: NotificationManager) {
        val builder = NotificationCompat.Builder(context, "job_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Report Pending")
            .setContentText(
                "End of shift approaching. Don't forget to generate today's service report!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
        manager.notify(999999, builder.build())
    }

    private fun sendNotification(manager: NotificationManager, notificationId: Int, jobId: Long, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_job", jobId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze1Intent = Intent(context, NotificationSnoozeReceiver::class.java).apply {
            putExtra("job_id", jobId)
            putExtra("snooze_weeks", 1)
            putExtra("notification_id", notificationId)
        }
        val snooze1PendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 200000, snooze1Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze2Intent = Intent(context, NotificationSnoozeReceiver::class.java).apply {
            putExtra("job_id", jobId)
            putExtra("snooze_weeks", 2)
            putExtra("notification_id", notificationId)
        }
        val snooze2PendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 300000, snooze2Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "job_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "View Job", pendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .addAction(0, "Snooze 1 Week", snooze1PendingIntent)
            .addAction(0, "Snooze 2 Weeks", snooze2PendingIntent)

        manager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "job_alerts",
                "Job Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Smart alerts for overdue or inactive jobs"
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateBadgeCount(count: Int, manager: NotificationManager) {
        // Fallback to NotificationChannel badge configuration if ShortcutBadger isn't used
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = manager.getNotificationChannel("job_alerts")
            if (channel != null) {
                channel.setShowBadge(true)
            }
        }
    }
}
