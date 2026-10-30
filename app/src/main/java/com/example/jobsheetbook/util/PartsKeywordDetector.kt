package com.example.jobsheetbook.util

object PartsKeywordDetector {

    private val keywordMap = mapOf(
        "Display" to listOf("display", "screen", "lcd", "panel"),
        "Battery" to listOf("battery", "batt"),
        "Keyboard" to listOf("keyboard", "kb", "keypad"),
        "Storage" to listOf("hdd", "ssd", "hard disk", "storage"),
        "Speaker" to listOf("speaker", "spkr", "audio"),
        "Charging" to listOf("charger", "adapter", "charging port", 
                             "charging section"),
        "RAM" to listOf("ram", "memory"),
        "Cooling" to listOf("fan", "cooling", "heat"),
        "Motherboard" to listOf("motherboard", "board", "mobo"),
        "Camera" to listOf("camera", "webcam"),
        "Touchpad" to listOf("touchpad", "trackpad"),
        "Power" to listOf("power button", "power switch", "powr switch")
    )

    fun detectParts(remarks: String): List<String> {
        if (remarks.isBlank()) return emptyList()
        val lower = remarks.lowercase()
        return keywordMap.entries
            .filter { (_, keywords) -> keywords.any { lower.contains(it) } }
            .map { it.key }
    }
}
