package com.example.jobsheetbook.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.jobsheetbook.ui.theme.*

object StatusUtil {
    fun getStatusColor(status: String): Color {
        return when (status) {
            "Received" -> StatusReceived
            "Work in-progress" -> Color(0xFF3B82F6) // Blue
            "Ready" -> StatusReady
            "Sent Out", "Chip-Level" -> StatusDiagnosing
            "Returned - Awaiting Delivery" -> Color(0xFF8B5CF6) // Purple/Indigo
            "Pending" -> StatusWaiting
            "Delivered" -> StatusDelivered
            "Return" -> StatusReturned
            else -> StatusReceived
        }
    }

    fun getDeviceIcon(itemType: String): ImageVector {
        return when (itemType.lowercase()) {
            "laptop" -> Icons.Default.Laptop
            "desktop" -> Icons.Default.DesktopWindows
            "tablet" -> Icons.Default.TabletAndroid
            "mobile", "phone", "smartphone" -> Icons.Default.PhoneAndroid
            "server" -> Icons.Default.Dns
            "printer" -> Icons.Default.Print
            else -> Icons.Default.DeviceUnknown
        }
    }
}
