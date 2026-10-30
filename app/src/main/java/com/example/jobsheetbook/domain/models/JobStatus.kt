package com.example.jobsheetbook.domain.models

enum class JobStatus(val value: String) {
    RECEIVED("Received"),
    IN_PROGRESS("Work in-progress"),
    READY("Ready"),
    CHIP_LEVEL("Chip-Level"),
    PENDING("Pending"),
    DELIVERED("Delivered"),
    RETURN("Return"),
    SENT_OUT("Sent Out"),
    RETURNED_AWAITING_DELIVERY("Returned - Awaiting Delivery"),
    UNKNOWN("Unknown");

    companion object {
        fun fromString(status: String?): JobStatus {
            return entries.find { it.value.equals(status, ignoreCase = true) }
                ?: when (status?.lowercase()) {
                    "in progress", "in-progress" -> IN_PROGRESS
                    else -> UNKNOWN
                }
        }
    }
}
