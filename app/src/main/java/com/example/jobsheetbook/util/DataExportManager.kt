package com.example.jobsheetbook.util

import android.content.Context
import android.os.Environment
import com.example.jobsheetbook.data.local.dao.JobDao
import com.example.jobsheetbook.data.local.dao.JobEventDao
import com.example.jobsheetbook.data.repository.ChipLevelRepository
import com.example.jobsheetbook.data.repository.PartsOrderRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportScope { ACTIVE_ONLY, FULL_HISTORY }
enum class CustomerScope { ACTIVE_ONLY, ALL }
enum class ExportFormat(val extension: String) { CSV("csv"), JSON("json") }

private fun csvText(value: String): String {
    val firstMeaningfulChar = value.indexOfFirst { !it.isWhitespace() }
    val needsFormulaProtection = firstMeaningfulChar >= 0 &&
        value[firstMeaningfulChar] in "=+-@"
    val safeValue = if (needsFormulaProtection) "'$value" else value
    return "\"${safeValue.replace("\"", "\"\"")}\""
}

private fun exportTimestamp(): String = SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT
).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

@Singleton
class DataExportManager @Inject constructor(
    private val jobDao: JobDao,
    private val jobEventDao: JobEventDao,
    private val chipLevelRepo: ChipLevelRepository,
    private val partsOrderRepo: PartsOrderRepository,
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportDatabase(scope: ExportScope, format: ExportFormat): File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val allDbJobs = jobDao.getAllJobsList()
        val jobs = if (scope == ExportScope.ACTIVE_ONLY) {
            allDbJobs.filter { !it.isDeleted && !it.isArchived && !it.status.equals("Delivered", ignoreCase = true) && !it.status.equals("Return", ignoreCase = true) }
        } else {
            allDbJobs
        }

        val allJobIds = jobs.map { it.jobId }
        val events = jobEventDao.getAllEventsList().filter { it.jobId in allJobIds }
        val chipLevelData = chipLevelRepo.chipLevelData.value
        val partsOrders = partsOrderRepo.getAllPendingStates().filter { it.jobId in allJobIds }

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JobSheetBook_backup_$timestamp.${format.extension}"
        val file = File(downloadsDir, fileName)

        if (format == ExportFormat.JSON) {
            val exportMap = mapOf(
                "export_date" to exportTimestamp(),
                "scope" to scope.name.lowercase(),
                "jobs" to jobs,
                "events" to events,
                "chip_level_info" to chipLevelData.values.filter { it.jobId in allJobIds },
                "parts_orders" to partsOrders
            )
            file.writeText(gson.toJson(exportMap))
        } else {
            // CSV Format
            val sb = StringBuilder()
            // Write Jobs
            sb.appendLine("job_id,display_id,customer_name,device_make,device_model,status,remarks,created_at,chip_level_status,tm_cost,final_cost")
            for (job in jobs) {
                val chipInfo = chipLevelData[job.jobId]
                val tmStatus = TechMechStatusUtil.getOutcomeLabel(job.status)
                val tmCost = chipInfo?.tmCost ?: 0
                val finalCost = chipInfo?.finalCost ?: 0
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(job.createdAt))
                sb.appendLine("${job.jobId},${csvText(job.displayId)},${csvText(job.customerName)},${csvText(job.make)},${csvText(job.model)},${csvText(job.status)},${csvText(job.remarks)},$dateStr,${csvText(tmStatus)},$tmCost,$finalCost")
            }
            file.writeText(sb.toString())
        }
        file
    }

    suspend fun exportCustomerList(scope: CustomerScope, format: ExportFormat): File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val allDbJobs = jobDao.getAllJobsList()
        
        // Group by phone number
        val customersMap = allDbJobs.groupBy { it.customerPhone }
        
        val customerList = customersMap.mapNotNull { (phone, customerJobs) ->
            val hasActiveJob = customerJobs.any { !it.isDeleted && !it.isArchived && !it.status.equals("Delivered", ignoreCase = true) && !it.status.equals("Return", ignoreCase = true) }
            if (scope == CustomerScope.ACTIVE_ONLY && !hasActiveJob) return@mapNotNull null
            
            val validJobs = if (scope == CustomerScope.ACTIVE_ONLY) {
                customerJobs.filter {
                    !it.isDeleted && !it.isArchived &&
                        !it.status.equals("Delivered", ignoreCase = true) &&
                        !it.status.equals("Return", ignoreCase = true)
                }
            } else {
                customerJobs
            }.sortedBy { it.createdAt }
            if (validJobs.isEmpty()) return@mapNotNull null

            val name = validJobs.maxByOrNull { it.createdAt }?.customerName ?: ""
            val devices = validJobs.map { "${it.make} ${it.model}" }.distinct()
            val firstVisit = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(validJobs.minOf { it.createdAt }))
            val lastVisit = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(validJobs.maxOf { it.createdAt }))
            
            mapOf(
                "name" to name,
                "phone" to phone,
                "device_brand" to validJobs.last().make,
                "device_model" to validJobs.last().model,
                "devices" to devices,
                "first_visit" to firstVisit,
                "last_visit" to lastVisit,
                "job_count" to validJobs.size
            )
        }.sortedBy { it["name"].toString() }

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JobSheetBook_customers_$timestamp.${format.extension}"
        val file = File(downloadsDir, fileName)

        if (format == ExportFormat.JSON) {
            val exportMap = mapOf(
                "export_date" to exportTimestamp(),
                "scope" to scope.name.lowercase(),
                "customer_count" to customerList.size,
                "customers" to customerList
            )
            file.writeText(gson.toJson(exportMap))
        } else {
            val sb = StringBuilder()
            sb.appendLine("customer_name,phone_number,device_brand,device_model,first_visit,last_visit,job_count")
            for (c in customerList) {
                sb.appendLine("${csvText(c["name"].toString())},${csvText(c["phone"].toString())},${csvText(c["device_brand"].toString())},${csvText(c["device_model"].toString())},${csvText(c["first_visit"].toString())},${csvText(c["last_visit"].toString())},${c["job_count"]}")
            }
            file.writeText(sb.toString())
        }
        file
    }
}
