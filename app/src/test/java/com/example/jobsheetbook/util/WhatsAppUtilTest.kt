package com.example.jobsheetbook.util

import android.net.Uri
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WhatsAppUtilTest {

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.encode(any()) } answers { firstArg<String>() }
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `getWhatsAppIntentUrl returns null for blank phone`() {
        assertNull(WhatsAppUtil.getWhatsAppIntentUrl("", "Hello"))
        assertNull(WhatsAppUtil.getWhatsAppIntentUrl("   ", "Hello"))
        assertNull(WhatsAppUtil.getWhatsAppIntentUrl("---", "Hello"))
    }

    @Test
    fun `getWhatsAppIntentUrl formats valid 10-digit phone`() {
        val url = WhatsAppUtil.getWhatsAppIntentUrl("9876543210", "Hello")
        assertEquals("https://api.whatsapp.com/send?phone=919876543210&text=Hello", url)
    }

    @Test
    fun `getWhatsAppIntentUrl formats valid phone with prefix`() {
        val url = WhatsAppUtil.getWhatsAppIntentUrl("+91 98765 43210", "Hello")
        assertEquals("https://api.whatsapp.com/send?phone=919876543210&text=Hello", url)
    }

    @Test
    fun `getWhatsAppIntentUrl formats international phone`() {
        val url = WhatsAppUtil.getWhatsAppIntentUrl("+971 50 123 4567", "Hello")
        assertEquals("https://api.whatsapp.com/send?phone=971501234567&text=Hello", url)
    }

    @Test
    fun `getWhatsAppIntentUrl returns null for landline`() {
        val url = WhatsAppUtil.getWhatsAppIntentUrl("04712345678", "Hello")
        assertNull(url)
    }

    @Test
    fun `generateStatusMessage for Received includes device with brand, complaint, and Regards footer`() {
        val msg = WhatsAppUtil.generateStatusMessage(
            customerName = "Jithesh",
            itemType = "Laptop",
            jobId = "D-13580",
            status = "Received",
            make = "Dell",
            model = "Inspiron 15",
            remarks = "SSD install, Keyboard replacement"
        )

        val expected = """
            Hi Jithesh,

            we have successfully received your Dell Laptop (D-13580) at Admin Info Solution. We will begin the diagnostic process shortly and keep you updated.

            Device: Dell Inspiron 15 Laptop
            Complaint: SSD install, Keyboard replacement

            Regards,
            Admin Info Solution, Haripad
        """.trimIndent()

        assertEquals(expected, msg)
    }

    @Test
    fun `generateStatusMessage for Ready includes device with brand and complaint but NO Regards footer`() {
        val msg = WhatsAppUtil.generateStatusMessage(
            customerName = "Jithesh",
            itemType = "Laptop",
            jobId = "D-13580",
            status = "Ready",
            make = "Dell",
            model = "Inspiron 15",
            remarks = "SSD install, Keyboard replacement"
        )

        val expected = """
            Hi Jithesh,

            Great news! your Dell Laptop (D-13580) is now ready for pickup. Please visit our shop to collect it at your convenience.

            Device: Dell Inspiron 15 Laptop
            Complaint: SSD install, Keyboard replacement
        """.trimIndent()

        assertEquals(expected, msg)
        assertFalse(msg.contains("Regards,"))
    }

    @Test
    fun `generateStatusMessage for other statuses omit Regards footer`() {
        val statuses = listOf("Work in-progress", "Sent Out", "Chip-Level", "Returned - Awaiting Delivery", "Pending", "Delivered", "Return")
        for (status in statuses) {
            val msg = WhatsAppUtil.generateStatusMessage(
                customerName = "Anand",
                itemType = "Desktop",
                jobId = "D-10001",
                status = status,
                make = "HP",
                model = "Pavilion",
                remarks = "No display"
            )
            assertFalse("Status $status should not have Regards footer", msg.contains("Regards,"))
            assertTrue(msg.contains("Device: HP Pavilion Desktop"))
            assertTrue(msg.contains("your HP Desktop (D-10001)"))
            assertTrue(msg.contains("Complaint: No display"))
        }
    }

    @Test
    fun `generateStatusMessage formats Dell Laptop with complaint Keyboard change`() {
        val msg = WhatsAppUtil.generateStatusMessage(
            customerName = "Rahul",
            itemType = "Laptop",
            jobId = "D-10003",
            status = "Received",
            make = "Dell",
            model = "",
            remarks = "Keyboard change"
        )

        assertTrue(msg.contains("your Dell Laptop (D-10003)"))
        assertTrue(msg.contains("Device: Dell Laptop"))
        assertTrue(msg.contains("Complaint: Keyboard change"))
    }

    @Test
    fun `generateStatusMessage formats Desktop Assembled correctly`() {
        val msg = WhatsAppUtil.generateStatusMessage(
            customerName = "Suresh",
            itemType = "Desktop",
            jobId = "D-10004",
            status = "Received",
            make = "Assembled (Custom PC)",
            model = "",
            remarks = "SMPS replacement"
        )

        assertTrue(msg.contains("your Desktop (Assembled) (D-10004)"))
        assertTrue(msg.contains("Device: Desktop (Assembled)"))
        assertTrue(msg.contains("Complaint: SMPS replacement"))
    }

    @Test
    fun `generateStatusMessage formats HP Desktop without model correctly`() {
        val msg = WhatsAppUtil.generateStatusMessage(
            customerName = "Manoj",
            itemType = "Desktop",
            jobId = "D-10005",
            status = "Ready",
            make = "HP",
            model = "",
            remarks = "OS Installation"
        )

        assertTrue(msg.contains("your HP Desktop (D-10005)"))
        assertTrue(msg.contains("Device: HP Desktop"))
        assertTrue(msg.contains("Complaint: OS Installation"))
    }

    @Test
    fun `generateStatusMessage handles blank make, model, remarks, and walk-in customer`() {
        val msg = WhatsAppUtil.generateStatusMessage(
            customerName = "Walk-in",
            itemType = "UPS",
            jobId = "D-10002",
            status = "Ready",
            make = "",
            model = "",
            remarks = "   "
        )

        val expected = """
            Hello,

            Great news! your UPS (D-10002) is now ready for pickup. Please visit our shop to collect it at your convenience.

            Device: UPS
        """.trimIndent()

        assertEquals(expected, msg)
        assertFalse(msg.contains("Complaint:"))
        assertFalse(msg.contains("Regards,"))
    }
}
