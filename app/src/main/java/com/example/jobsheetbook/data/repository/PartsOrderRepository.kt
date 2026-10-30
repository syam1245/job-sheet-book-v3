package com.example.jobsheetbook.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PartsOrderState(
    val jobId: Long,
    val detectedParts: List<String>,
    val orderStatus: String = "Pending",
    val confirmedOrder: Boolean = false,
    val detectedAt: Long = System.currentTimeMillis(),
    val orderedAt: Long? = null,
    val arrivedAt: Long? = null
)

@Singleton
class PartsOrderRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // KNOWN LIMITATION: PartsOrderState is stored only in 'parts_order_prefs' SharedPreferences
    // on a single device. It is NOT synced to Firestore, so parts order status will not be
    // visible on other devices. Implementing cloud sync would require adding a 'parts_orders'
    // Firestore collection and integrating it into FirestoreSyncManager. Tracked in TECH_DEBT.md.
    private val prefs: SharedPreferences = context.getSharedPreferences("parts_order_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // NOTE: We do NOT use OnSharedPreferenceChangeListener here.
    // saveState/clearState already update _partsData in-memory directly (O(1)),
    // so a listener that calls loadAllData() — O(N) JSON parsing — would be pure
    // overhead. This mirrors the same pattern used in ChipLevelRepository.
    private val _partsData = MutableStateFlow<Map<Long, PartsOrderState>>(loadAllData())
    val partsData: StateFlow<Map<Long, PartsOrderState>> = _partsData.asStateFlow()

    private fun loadAllData(): Map<Long, PartsOrderState> {
        val map = mutableMapOf<Long, PartsOrderState>()
        for (entry in prefs.all) {
            val key = entry.key
            if (!key.startsWith("parts_")) continue
            val json = entry.value as? String ?: continue
            try {
                val state = gson.fromJson(json, PartsOrderState::class.java)
                if (state != null) {
                    map[state.jobId] = state
                }
            } catch (e: Exception) {
                // Ignore malformed JSON entries
            }
        }
        return map
    }

    fun saveState(state: PartsOrderState) {
        prefs.edit().putString("parts_${state.jobId}", gson.toJson(state)).apply()
        // O(1) in-memory update — no full re-scan
        val current = _partsData.value.toMutableMap()
        current[state.jobId] = state
        _partsData.value = current
    }

    fun getState(jobId: Long): PartsOrderState? {
        return _partsData.value[jobId]
    }

    fun clearState(jobId: Long) {
        prefs.edit().remove("parts_$jobId").apply()
        // O(1) in-memory removal
        val current = _partsData.value.toMutableMap()
        current.remove(jobId)
        _partsData.value = current
    }

    /**
     * Returns all non-cleared parts states.
     * O(N) in-memory filter — no disk I/O or JSON parsing on every call.
     * Previously this was O(N) SharedPreferences disk scan + JSON deserialization
     * running every 2 hours inside NotificationWorker.
     */
    fun getAllPendingStates(): List<PartsOrderState> {
        return _partsData.value.values.filter { it.orderStatus != "Cleared" }
    }

    fun markOrdered(jobId: Long) {
        getState(jobId)?.let { state ->
            saveState(state.copy(orderStatus = "Ordered", orderedAt = System.currentTimeMillis()))
        }
    }

    fun markArrived(jobId: Long) {
        getState(jobId)?.let { state ->
            saveState(state.copy(orderStatus = "Arrived", arrivedAt = System.currentTimeMillis()))
        }
    }

    fun confirmOrder(jobId: Long) {
        val current = getState(jobId) ?: return
        saveState(current.copy(confirmedOrder = true))
    }
}
