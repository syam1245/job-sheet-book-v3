package com.example.jobsheetbook.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.jobsheetbook.data.repository.NotificationStateRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationSnoozeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationStateRepo: NotificationStateRepository

    override fun onReceive(context: Context, intent: Intent) {
        val jobId = intent.getLongExtra("job_id", -1L)
        val weeks = intent.getIntExtra("snooze_weeks", 1)
        val notificationId = intent.getIntExtra("notification_id", -1)
        if (jobId == -1L) return

        // Save snooze state using repository
        notificationStateRepo.snooze(jobId, weeks)

        // Cancel the notification
        if (notificationId != -1) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}
