package com.example.jobsheetbook.data.local

import androidx.compose.ui.graphics.Color

data class BrandOption(
    val label: String,
    val color: Color
)

// Legacy static brand list — kept for backward-compat with brand parse-back in ViewModel
val BRANDS = listOf(
    BrandOption("HP",       Color(0xFF0096D6)),
    BrandOption("Dell",     Color(0xFF007DB8)),
    BrandOption("Lenovo",   Color(0xFFE2231A)),
    BrandOption("Asus",     Color(0xFF00539B)),
    BrandOption("Acer",     Color(0xFF83B81A)),
    BrandOption("Apple",    Color(0xFF555555)),
    BrandOption("Samsung",  Color(0xFF1428A0)),
    BrandOption("MSI",      Color(0xFFD00000)),
    BrandOption("OTHER",    Color(0xFF757575))
)

// ── Ordered list of item types shown in the dropdown ──
val ITEM_TYPES = listOf(
    "Laptop",
    "Desktop",
    "Printer",
    "Server",
    "Tablet",
    "UPS",
    "Others"
)

// ── Per-item-type make lists ──
// "Others" at the end of each list signals the UI to allow free-text entry.
val ITEM_TYPE_MAKES: Map<String, List<String>> = mapOf(
    "Laptop"  to listOf("HP", "Dell", "Lenovo", "Asus", "Acer", "Apple", "MSI", "Microsoft", "Others"),
    "Desktop" to listOf("Assembled (Custom PC)", "HP", "Dell", "Lenovo", "Apple", "Asus", "Acer", "Others"),
    "Printer" to listOf("HP", "Canon", "Epson", "Brother", "Samsung", "TVS", "Pantum", "Others"),
    "Server"  to listOf("Dell EMC", "HP Enterprise (HPE)", "Lenovo", "IBM", "Cisco", "Others"),
    "Tablet"  to listOf("Apple", "Samsung", "Lenovo", "Xiaomi", "Realme", "OnePlus", "Others"),
    "UPS"     to listOf("V-Guard", "APC", "Microtek", "Foxin", "Fingers", "iBall", "iBell",
                         "Luminous", "Zebronics", "Others"),
    "Others"  to emptyList()   // free-text only
)

/**
 * Returns the list of known makes for the given [itemType].
 * Returns an empty list for unknown types, signalling free-text only.
 */
fun makesForItemType(itemType: String): List<String> =
    ITEM_TYPE_MAKES[itemType] ?: emptyList()

val COMMON_COMPLAINTS = listOf(
    "No Display",
    "Not Powering On",
    "Overheating",
    "Slow Performance",
    "No WiFi",
    "Keyboard Not Working",
    "Battery Not Charging",
    "Hinges Broken",
    "Motherboard Issue",
    "Water Damage",
    "Virus / Malware",
    "OS Not Booting",
    "Hard Disk Failure",
    "RAM Issue",
    "Speaker Not Working",
    "Touchpad Not Working",
    "USB Port Not Working",
    "Screen Cracked",
    "Fan Not Working",
    "Backlight Issue"
)
