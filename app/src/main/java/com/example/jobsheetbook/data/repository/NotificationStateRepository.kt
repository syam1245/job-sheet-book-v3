package com.example.jobsheetbook.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class JobNotificationState(
    val jobId: Long,
    val notifyCount: Int = 0,
    val snoozedUntil: Long = 0L,
    val lastNotifiedAt: Long = 0L
)

@Singleton
class NotificationStateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notification_state_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getState(jobId: Long): JobNotificationState {
        val json = prefs.getString("job_state_$jobId", null) ?: return JobNotificationState(jobId = jobId)
        return try {
            gson.fromJson(json, JobNotificationState::class.java) ?: JobNotificationState(jobId = jobId)
        } catch (e: Exception) {
            JobNotificationState(jobId = jobId)
        }
    }

    fun saveState(state: JobNotificationState) {
        val json = gson.toJson(state)
        prefs.edit().putString("job_state_${state.jobId}", json).apply()
    }

    fun resetState(jobId: Long) {
        prefs.edit().remove("job_state_$jobId").apply()
    }


    fun snooze(jobId: Long, weeks: Int) {
        val current = getState(jobId)
        val snoozedUntil = System.currentTimeMillis() + weeks * 7L * 24 * 60 * 60 * 1000L
        saveState(current.copy(snoozedUntil = snoozedUntil))
    }

    fun isSnoozed(jobId: Long): Boolean {
        return getState(jobId).snoozedUntil > System.currentTimeMillis()
    }

    fun incrementCount(jobId: Long): Int {
        val current = getState(jobId)
        val updated = current.copy(
            notifyCount = current.notifyCount + 1,
            lastNotifiedAt = System.currentTimeMillis()
        )
        saveState(updated)
        return updated.notifyCount
    }

    fun clearAllStates() {
        prefs.edit().clear().apply()
    }

    fun hasCompletedMigration(version: Int): Boolean {
        return prefs.getInt("migration_version", 0) >= version
    }

    fun markMigrationComplete(version: Int) {
        prefs.edit().putInt("migration_version", version).apply()
    }
}
