package com.example.jobsheetbook.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.example.jobsheetbook.util.TechMechStatusUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ChipLevelInfo(
    val jobId: Long,
    val tmJobId: String,
    val tmCost: Int = 0,
    val finalCost: Int = 0,
    val workDone: String,
    val tmStatus: String = "Not Marked", // "Not Marked", "In Hand", "Delivered"
    val pickedUp: Boolean = false,        // Has TechMech physically collected the device?
    val pickedUpAt: Long? = null,         // Timestamp when marked picked up
    val lastModified: Long = System.currentTimeMillis(),
    val lastSyncUpdate: Long = System.currentTimeMillis(),
    // Soft-delete tombstone: set to true when deleted so Firestore sync won't re-insert it
    val isDeleted: Boolean = false
) {
    val profit: Int get() = if (finalCost > 0) finalCost - tmCost else 0

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "jobId" to jobId,
            "tmJobId" to tmJobId,
            "tmCost" to tmCost,
            "finalCost" to finalCost,
            "workDone" to workDone,
            "tmStatus" to tmStatus,
            "pickedUp" to pickedUp,
            "pickedUpAt" to pickedUpAt,
            "lastModified" to lastModified,
            "lastSyncUpdate" to lastSyncUpdate,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): ChipLevelInfo {
            return ChipLevelInfo(
                jobId = (map["jobId"] as? Number)?.toLong() ?: 0L,
                tmJobId = (map["tmJobId"] as? String) ?: "",
                tmCost = (map["tmCost"] as? Number)?.toInt() ?: (map["cost"] as? Number)?.toInt() ?: 0,
                finalCost = (map["finalCost"] as? Number)?.toInt() ?: 0,
                workDone = (map["workDone"] as? String) ?: "",
                tmStatus = (map["tmStatus"] as? String) ?: "Not Marked",
                // Default to false for records predating the pickedUp feature (safe migration)
                pickedUp = (map["pickedUp"] as? Boolean) ?: false,
                pickedUpAt = (map["pickedUpAt"] as? Number)?.toLong(),
                lastModified = (map["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastSyncUpdate = (map["lastSyncUpdate"] as? Number)?.toLong() ?: ((map["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()),
                isDeleted = (map["isDeleted"] as? Boolean) ?: false
            )
        }
    }
}


@Singleton
class ChipLevelRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chiplevel_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _chipLevelData = MutableStateFlow<Map<Long, ChipLevelInfo>>(loadAllData())
    val chipLevelData: StateFlow<Map<Long, ChipLevelInfo>> = _chipLevelData.asStateFlow()

    // NOTE: We do NOT use OnSharedPreferenceChangeListener here.
    // saveChipLevelInfo() already updates _chipLevelData in-memory directly (O(1)),
    // so a listener that calls loadAllData() — O(N) JSON parsing — would be pure overhead
    // and cause UI stutter when editing tracker fields with a large dataset.

    fun unregisterListener() {
        // No-op: listener was removed as part of the O(N) parsing bottleneck fix.
        // Kept for API compatibility in case callers invoke it (e.g. onCleared in ViewModel).
    }

    private fun loadAllData(): Map<Long, ChipLevelInfo> {
        val map = mutableMapOf<Long, ChipLevelInfo>()
        prefs.all.forEach { (key, value) ->
            if (value is String) {
                try {
                    val info = gson.fromJson(value, ChipLevelInfo::class.java)
                    // Skip tombstone records (soft-deleted); they only exist for Firestore sync
                    if (info != null && !info.isDeleted) {
                        map[key.toLong()] = info
                    }
                } catch (e: Exception) {
                    // Ignore malformed JSON
                }
            }
        }
        return map
    }

    fun saveChipLevelInfo(info: ChipLevelInfo, preserveLastModified: Boolean = false) {
        val updatedInfo = if (preserveLastModified) {
            // Keep the cloud version when importing. This timestamp is used for
            // conflict resolution, so replacing it with the local receipt time
            // can make an older payload appear newer than a real remote edit.
            info
        } else {
            info.copy(lastModified = System.currentTimeMillis(), lastSyncUpdate = System.currentTimeMillis())
        }
        val json = gson.toJson(updatedInfo)
        prefs.edit().putString(updatedInfo.jobId.toString(), json).apply()
        val currentMap = _chipLevelData.value.toMutableMap()
        currentMap[updatedInfo.jobId] = updatedInfo
        _chipLevelData.value = currentMap
    }

    fun getChipLevelInfo(jobId: Long): ChipLevelInfo? {
        return _chipLevelData.value[jobId]
    }

    fun deleteChipLevelInfo(jobId: Long) {
        // Write a tombstone to SharedPreferences so uploadChipLevel() can push isDeleted=true
        // to Firestore. This prevents the record from being re-inserted during the next sync.
        val existing = _chipLevelData.value[jobId]
        val tombstone = (existing ?: ChipLevelInfo(jobId = jobId, tmJobId = "", workDone = ""))
            .copy(isDeleted = true, lastModified = System.currentTimeMillis(), lastSyncUpdate = System.currentTimeMillis())
        val json = gson.toJson(tombstone)
        prefs.edit().putString(jobId.toString(), json).apply()
        // Remove from the in-memory map so the UI no longer shows the deleted record
        val currentMap = _chipLevelData.value.toMutableMap()
        currentMap.remove(jobId)
        _chipLevelData.value = currentMap
    }

    /**
     * Returns soft-deleted (tombstone) records from SharedPreferences.
     * Used by FirestoreSyncManager during manual sync to ensure deletion
     * tombstones are uploaded even if the original deleteChipLevel() call
     * failed (e.g. due to no network at deletion time).
     */
    fun getTombstones(): List<ChipLevelInfo> {
        val tombstones = mutableListOf<ChipLevelInfo>()
        prefs.all.forEach { (_, value) ->
            if (value is String) {
                try {
                    val info = gson.fromJson(value, ChipLevelInfo::class.java)
                    if (info != null && info.isDeleted) {
                        tombstones.add(info)
                    }
                } catch (e: Exception) { /* ignore */ }
            }
        }
        return tombstones
    }

    /**
     * Returns the highest lastModified timestamp among all chip records (including tombstones).
     * Used by syncOnce() to query only modified chip records.
     */
    fun getMaxLastModified(): Long {
        val activeMax = _chipLevelData.value.values.maxOfOrNull { it.lastModified } ?: 0L
        val tombstoneMax = getTombstones().maxOfOrNull { it.lastModified } ?: 0L
        return maxOf(activeMax, tombstoneMax)
    }

    /**
     * Returns all chip records (including soft-delete tombstones) modified after the given timestamp.
     * Used by performFullManualSync() to only upload locally modified records.
     */
    fun getChipRecordsModifiedSince(since: Long): List<ChipLevelInfo> {
        val active = _chipLevelData.value.values.filter { it.lastModified > since }
        val tombstones = getTombstones().filter { it.lastModified > since }
        return active + tombstones
    }

    /** Returns every local record, including tombstones, for a full integrity sync. */
    fun getAllRecords(): List<ChipLevelInfo> =
        _chipLevelData.value.values.toList() + getTombstones()
}

