package com.example.jobsheetbook.util

object TechMechEnquiryMessage {

    data class Labels(
        val jobNo: String = "Job no:",
        val customer: String = "Customer:",
        val device: String = "Device:",
        val problem: String = "Problem:",
        val techMechRef: String = "TechMech ref:",
        val closing: String = "Estimation & Status?",
        val notRecorded: String = "Not recorded",
        val walkIn: String = "Walk-in"
    )

    fun formatProblem(remarks: String, notRecordedText: String = "Not recorded"): String {
        val trimmed = remarks.trim()
        if (trimmed.isEmpty()) return notRecordedText
        val collapsed = trimmed.replace(Regex("\\s+"), " ")
        return if (collapsed.length > 300) {
            collapsed.take(300) + "..."
        } else {
            collapsed
        }
    }

    fun build(
        jobNumber: String,
        customerName: String,
        make: String,
        model: String,
        remarks: String,
        tmJobId: String,
        labels: Labels = Labels()
    ): String {
        val lines = mutableListOf<String>()

        // 1. Job no: <job number>
        lines.add("${labels.jobNo.trimEnd()} $jobNumber")

        // 2. Customer: <customer name> (blank becomes "Walk-in")
        val effectiveCustomer = if (customerName.isBlank()) labels.walkIn else customerName
        lines.add("${labels.customer.trimEnd()} $effectiveCustomer")

        // 3. Device: <make> <model> (omit this line when both are blank)
        val deviceParts = listOf(make.trim(), model.trim()).filter { it.isNotBlank() }
        if (deviceParts.isNotEmpty()) {
            lines.add("${labels.device.trimEnd()} ${deviceParts.joinToString(" ")}")
        }

        // 4. Problem: <remarks>
        val problemText = formatProblem(remarks, labels.notRecorded)
        lines.add("${labels.problem.trimEnd()} $problemText")

        // 5. TechMech ref: <tmJobId> (omit this line when tmJobId is blank)
        if (tmJobId.isNotBlank()) {
            lines.add("${labels.techMechRef.trimEnd()} ${tmJobId.trim()}")
        }

        // Blank line before "Estimation & Status?"
        return lines.joinToString("\n") + "\n\n" + labels.closing
    }
}
