package com.example.jobsheetbook.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index(value = ["phoneNumber"])]
)
data class CustomerEntity(
    @PrimaryKey
    val phoneNumber: String,
    val name: String,
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "phoneNumber" to phoneNumber,
            "name" to name,
            "lastModified" to lastModified,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): CustomerEntity {
            return CustomerEntity(
                phoneNumber = (map["phoneNumber"] as? String) ?: "",
                name = (map["name"] as? String) ?: "",
                lastModified = (map["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isDeleted = (map["isDeleted"] as? Boolean) ?: false
            )
        }
    }
}
