package com.example.jobsheetbook.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "jobs",
    indices = [
        Index(value = ["customerPhone"])
    ]
)
data class JobEntity(
    @PrimaryKey
    val jobId: Long,
    val customerPhone: String,
    val customerName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val itemType: String, // Laptop/Desktop/Tablet
    val make: String,
    val model: String,
    val serialNumber: String,
    val devicePassword: String,
    val remarks: String,
    val status: String = "Received",
    // KNOWN LIMITATION: imagePaths stores absolute local file:// URIs (e.g. /data/user/0/.../files/img.jpg).
    // These paths are synced to Firestore but are only valid on the device that captured the images.
    // On other devices, the paths will be broken. Cross-device image access requires Firebase Storage.
    // Tracked in TECH_DEBT.md — do not fix speculatively without user request.
    val imagePaths: List<String> = emptyList(),
    // Accessories (Stored as JSON via Converter)
    val accessories: Map<String, Boolean> = emptyMap(),
    
    // Priority (Deprecated, keeping column with default to avoid DB migration)
    val priority: String = "P2 Standard",
    val jobTypes: List<String> = emptyList(),
    val dueDate: Long? = null,
    val pendingReason: String = "",
    
    // Cost tracking
    val estimatedCost: Int = 0,
    val finalCost: Int = 0,
    
    // Sync fields
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    
    // Digital formatted ID (Deprecated in DB, keeping column to avoid migration)
    val displayJobId: String? = null,
    
    // Archive
    val isArchived: Boolean = false
) {
    val displayId: String
        get() = if (!displayJobId.isNullOrBlank()) displayJobId else String.format("D-%05d", jobId)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "jobId" to jobId,
            "customerPhone" to customerPhone,
            "customerName" to customerName,
            "createdAt" to createdAt,
            "itemType" to itemType,
            "make" to make,
            "model" to model,
            "serialNumber" to serialNumber,
            "devicePassword" to devicePassword,
            "remarks" to remarks,
            "status" to status,
            "imagePaths" to imagePaths,
            "accessories" to accessories,
            "priority" to priority,
            "jobTypes" to jobTypes,
            "dueDate" to dueDate,
            "pendingReason" to pendingReason,
            "estimatedCost" to estimatedCost,
            "finalCost" to finalCost,
            "lastModified" to lastModified,
            "isDeleted" to isDeleted,
            "displayJobId" to displayJobId,
            "isArchived" to isArchived
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): JobEntity {
            return JobEntity(
                jobId = (map["jobId"] as? Number)?.toLong() ?: 0L,
                customerPhone = (map["customerPhone"] as? String) ?: "",
                customerName = (map["customerName"] as? String) ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                itemType = (map["itemType"] as? String) ?: "",
                make = (map["make"] as? String) ?: "",
                model = (map["model"] as? String) ?: "",
                serialNumber = (map["serialNumber"] as? String) ?: "",
                devicePassword = (map["devicePassword"] as? String) ?: "",
                remarks = (map["remarks"] as? String) ?: "",
                status = (map["status"] as? String) ?: "Received",
                imagePaths = (map["imagePaths"] as? List<String>) ?: emptyList(),
                accessories = (map["accessories"] as? Map<String, Boolean>) ?: emptyMap(),
                priority = (map["priority"] as? String) ?: "P2 Standard",
                jobTypes = (map["jobTypes"] as? List<String>) ?: emptyList(),
                dueDate = (map["dueDate"] as? Number)?.toLong(),
                pendingReason = (map["pendingReason"] as? String) ?: "",
                estimatedCost = (map["estimatedCost"] as? Number)?.toInt() ?: 0,
                finalCost = (map["finalCost"] as? Number)?.toInt() ?: 0,
                lastModified = (map["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isDeleted = (map["isDeleted"] as? Boolean) ?: false,
                displayJobId = map["displayJobId"] as? String,
                isArchived = (map["isArchived"] as? Boolean) ?: false
            )
        }
    }
}
