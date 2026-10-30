package com.example.jobsheetbook.util

import android.net.Uri
import com.example.jobsheetbook.domain.models.Job

object WhatsAppUtil {
    private const val SHOP_NAME = "Admin Info Solution, Haripad"

    fun formatDeviceSummary(itemType: String, make: String = "", model: String = ""): String {
        val makeTrimmed = make.trim()
        val modelTrimmed = model.trim()
        val typeTrimmed = itemType.trim()

        val isDesktop = typeTrimmed.equals("Desktop", ignoreCase = true)
        val isAssembled = makeTrimmed.contains("Assembled", ignoreCase = true)

        return when {
            // Assembled Desktops -> "Desktop (Assembled)" or "Desktop (Assembled - Model)"
            isDesktop && isAssembled -> {
                if (modelTrimmed.isNotBlank()) {
                    "Desktop (Assembled - $modelTrimmed)"
                } else {
                    "Desktop (Assembled)"
                }
            }
            isAssembled -> {
                val base = if (typeTrimmed.isNotBlank()) "$typeTrimmed (Assembled)" else "Assembled"
                if (modelTrimmed.isNotBlank()) "$base - $modelTrimmed" else base
            }
            // Branded device (e.g. "Dell Laptop", "Dell Inspiron 15 Laptop", "HP Desktop")
            makeTrimmed.isNotBlank() -> {
                val brandAndModel = if (modelTrimmed.isNotBlank()) "$makeTrimmed $modelTrimmed" else makeTrimmed
                if (typeTrimmed.isBlank() || typeTrimmed.equals("Others", ignoreCase = true)) {
                    brandAndModel
                } else if (brandAndModel.contains(typeTrimmed, ignoreCase = true)) {
                    brandAndModel
                } else {
                    "$brandAndModel $typeTrimmed"
                }
            }
            // No brand specified, but model exists
            modelTrimmed.isNotBlank() -> {
                if (typeTrimmed.isBlank() || typeTrimmed.equals("Others", ignoreCase = true)) {
                    modelTrimmed
                } else if (modelTrimmed.contains(typeTrimmed, ignoreCase = true)) {
                    modelTrimmed
                } else {
                    "$modelTrimmed $typeTrimmed"
                }
            }
            else -> typeTrimmed
        }
    }

    fun formatComplaintSummary(remarks: String): String {
        val trimmed = remarks.trim()
        if (trimmed.isBlank()) return ""
        val clean = trimmed.replace(Regex("\\s*\\|\\s*"), ", ")
            .replace(Regex("\\s+"), " ")
        return clean
    }

    fun generateStatusMessage(job: Job): String {
        return generateStatusMessage(
            customerName = job.customerName,
            itemType = job.itemType,
            jobId = job.displayId,
            status = job.rawStatus,
            make = job.make,
            model = job.model,
            remarks = job.remarks,
            pendingReason = job.pendingReason
        )
    }

    fun generateStatusMessage(
        customerName: String,
        itemType: String,
        jobId: String,
        status: String,
        make: String = "",
        model: String = "",
        remarks: String = "",
        pendingReason: String = ""
    ): String {
        val greeting = if (customerName.isNotBlank() && customerName != "Walk-in") {
            "Hi $customerName,"
        } else {
            "Hello,"
        }

        val makeTrimmed = make.trim()
        val typeTrimmed = itemType.trim()
        val isDesktop = typeTrimmed.equals("Desktop", ignoreCase = true)
        val isAssembled = makeTrimmed.contains("Assembled", ignoreCase = true)

        val deviceDisplayName = when {
            isDesktop && isAssembled -> "Desktop (Assembled)"
            isAssembled -> if (typeTrimmed.isNotBlank()) "$typeTrimmed (Assembled)" else "Assembled"
            makeTrimmed.isNotBlank() && typeTrimmed.isNotBlank() && !typeTrimmed.equals("Others", ignoreCase = true) -> {
                if (makeTrimmed.contains(typeTrimmed, ignoreCase = true)) makeTrimmed else "$makeTrimmed $typeTrimmed"
            }
            makeTrimmed.isNotBlank() -> makeTrimmed
            typeTrimmed.isNotBlank() -> typeTrimmed
            else -> "device"
        }

        val deviceShort = "your $deviceDisplayName ($jobId)"

        val messageBody = when (status) {
            "Received" -> 
                "we have successfully received $deviceShort at Admin Info Solution. We will begin the diagnostic process shortly and keep you updated."
            "Work in-progress" -> 
                "we are currently working on $deviceShort. We will inform you about the progress very soon."
            "Sent Out", "Chip-Level" -> 
                "we have sent out $deviceShort for Chip-Level service. We will keep you updated on the progress."
            "Returned - Awaiting Delivery" -> 
                "your $deviceShort has been received back from Chip-Level service and is awaiting final inspection and delivery."
            "Pending" -> {
                val reason = if (pendingReason.isNotBlank()) "\nReason: $pendingReason." else ""
                "repair for $deviceShort is currently on hold. We will resume the work as soon as possible.$reason"
            }
            "Ready" -> 
                "Great news! $deviceShort is now ready for pickup. Please visit our shop to collect it at your convenience."
            "Delivered" -> 
                "$deviceShort has been delivered successfully. Thank you for choosing Admin Info Solution! We hope you are satisfied with our service."
            "Return" -> 
                "$deviceShort is being returned unrepaired. Please visit our shop to collect the device."
            else -> 
                "the status of $deviceShort has been updated to '$status'. Please contact us for more details."
        }

        val detailsList = mutableListOf<String>()
        val deviceSummary = formatDeviceSummary(itemType, make, model)
        if (deviceSummary.isNotBlank()) {
            detailsList.add("Device: $deviceSummary")
        }
        val complaintSummary = formatComplaintSummary(remarks)
        if (complaintSummary.isNotBlank()) {
            detailsList.add("Complaint: $complaintSummary")
        }

        val detailsBlock = if (detailsList.isNotEmpty()) {
            "\n\n" + detailsList.joinToString("\n")
        } else {
            ""
        }

        val footer = if (status.equals("Received", ignoreCase = true)) {
            "\n\nRegards,\n$SHOP_NAME"
        } else {
            ""
        }

        return "$greeting\n\n$messageBody$detailsBlock$footer"
    }

    fun getWhatsAppIntentUrl(phoneNumber: String, message: String): String? {
        val digits = PhoneNormalizer.toWhatsAppDigits(phoneNumber) ?: return null
        return "https://api.whatsapp.com/send?phone=$digits&text=${Uri.encode(message)}"
    }
}
