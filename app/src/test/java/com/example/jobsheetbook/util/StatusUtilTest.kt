package com.example.jobsheetbook.util

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.jobsheetbook.ui.theme.StatusReceived
import com.example.jobsheetbook.ui.theme.StatusReady
import com.example.jobsheetbook.ui.theme.StatusDiagnosing
import com.example.jobsheetbook.ui.theme.StatusWaiting
import com.example.jobsheetbook.ui.theme.StatusDelivered
import com.example.jobsheetbook.ui.theme.StatusReturned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

class StatusUtilTest {

    @Test
    fun `getStatusColor maps statuses correctly`() {
        assertEquals(StatusReceived, StatusUtil.getStatusColor("Received"))
        assertEquals(Color(0xFF3B82F6), StatusUtil.getStatusColor("Work in-progress"))
        assertEquals(StatusReady, StatusUtil.getStatusColor("Ready"))
        assertEquals(StatusDiagnosing, StatusUtil.getStatusColor("Sent Out"))
        assertEquals(StatusDiagnosing, StatusUtil.getStatusColor("Chip-Level"))
        assertEquals(Color(0xFF8B5CF6), StatusUtil.getStatusColor("Returned - Awaiting Delivery"))
        assertEquals(StatusWaiting, StatusUtil.getStatusColor("Pending"))
        assertEquals(StatusDelivered, StatusUtil.getStatusColor("Delivered"))
        assertEquals(StatusReturned, StatusUtil.getStatusColor("Return"))
        
        // Default case
        assertEquals(StatusReceived, StatusUtil.getStatusColor("Unknown Status"))
    }

    @Test
    fun `getDeviceIcon maps item types correctly`() {
        assertEquals(Icons.Default.Laptop, StatusUtil.getDeviceIcon("Laptop"))
        assertEquals(Icons.Default.DesktopWindows, StatusUtil.getDeviceIcon("desktop"))
        assertEquals(Icons.Default.TabletAndroid, StatusUtil.getDeviceIcon("Tablet"))
        assertEquals(Icons.Default.PhoneAndroid, StatusUtil.getDeviceIcon("mobile"))
        assertEquals(Icons.Default.PhoneAndroid, StatusUtil.getDeviceIcon("phone"))
        assertEquals(Icons.Default.PhoneAndroid, StatusUtil.getDeviceIcon("smartphone"))
        assertEquals(Icons.Default.Dns, StatusUtil.getDeviceIcon("SERVER"))
        assertEquals(Icons.Default.Print, StatusUtil.getDeviceIcon("printer"))
        
        // Default case
        assertEquals(Icons.Default.DeviceUnknown, StatusUtil.getDeviceIcon("Random Gadget"))
    }
}
