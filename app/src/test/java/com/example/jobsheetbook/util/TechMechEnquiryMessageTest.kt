package com.example.jobsheetbook.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TechMechEnquiryMessageTest {

    private val fixedLabels = TechMechEnquiryMessage.Labels(
        jobNo = "Job no:",
        customer = "Customer:",
        device = "Device:",
        problem = "Problem:",
        techMechRef = "TechMech ref:",
        closing = "Estimation & Status?",
        notRecorded = "Not recorded",
        walkIn = "Walk-in"
    )

    @Test
    fun `full message with all fields`() {
        val result = TechMechEnquiryMessage.build(
            jobNumber = "D-00042",
            customerName = "John Doe",
            make = "Dell",
            model = "Inspiron 15",
            remarks = "No display",
            tmJobId = "TM-1001",
            labels = fixedLabels
        )

        val expected = """
            Job no: D-00042
            Customer: John Doe
            Device: Dell Inspiron 15
            Problem: No display
            TechMech ref: TM-1001

            Estimation & Status?
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `no device line when make and model are blank`() {
        val result = TechMechEnquiryMessage.build(
            jobNumber = "D-00042",
            customerName = "John Doe",
            make = "",
            model = "   ",
            remarks = "Motherboard issue",
            tmJobId = "TM-1001",
            labels = fixedLabels
        )

        val expected = """
            Job no: D-00042
            Customer: John Doe
            Problem: Motherboard issue
            TechMech ref: TM-1001

            Estimation & Status?
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `no ref line when tmJobId is blank`() {
        val result = TechMechEnquiryMessage.build(
            jobNumber = "D-00042",
            customerName = "John Doe",
            make = "HP",
            model = "Pavilion",
            remarks = "Battery issue",
            tmJobId = "",
            labels = fixedLabels
        )

        val expected = """
            Job no: D-00042
            Customer: John Doe
            Device: HP Pavilion
            Problem: Battery issue

            Estimation & Status?
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `not recorded for blank remarks`() {
        val result = TechMechEnquiryMessage.build(
            jobNumber = "D-00042",
            customerName = "John Doe",
            make = "HP",
            model = "Pavilion",
            remarks = "   ",
            tmJobId = "TM-1002",
            labels = fixedLabels
        )

        val expected = """
            Job no: D-00042
            Customer: John Doe
            Device: HP Pavilion
            Problem: Not recorded
            TechMech ref: TM-1002

            Estimation & Status?
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `whitespace and newline collapsing in problem remarks`() {
        val result = TechMechEnquiryMessage.build(
            jobNumber = "D-00042",
            customerName = "John Doe",
            make = "Lenovo",
            model = "ThinkPad",
            remarks = "  Screen \n broken   \t and \r\n no power   ",
            tmJobId = "TM-1003",
            labels = fixedLabels
        )

        val expected = """
            Job no: D-00042
            Customer: John Doe
            Device: Lenovo ThinkPad
            Problem: Screen broken and no power
            TechMech ref: TM-1003

            Estimation & Status?
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `problem remarks cut at 300 characters with ellipsis`() {
        val longRemarks = "a".repeat(350)
        val result = TechMechEnquiryMessage.build(
            jobNumber = "D-00042",
            customerName = "John Doe",
            make = "Apple",
            model = "MacBook Pro",
            remarks = longRemarks,
            tmJobId = "TM-1004",
            labels = fixedLabels
        )

        val expectedProblem = "a".repeat(300) + "..."
        val expected = """
            Job no: D-00042
            Customer: John Doe
            Device: Apple MacBook Pro
            Problem: $expectedProblem
            TechMech ref: TM-1004

            Estimation & Status?
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun `walk-in customer name passes through when blank or Walk-in`() {
        val resultBlank = TechMechEnquiryMessage.build(
            jobNumber = "D-00042",
            customerName = "",
            make = "Asus",
            model = "ZenBook",
            remarks = "Hinges broken",
            tmJobId = "TM-1005",
            labels = fixedLabels
        )

        val resultWalkIn = TechMechEnquiryMessage.build(
            jobNumber = "D-00042",
            customerName = "Walk-in",
            make = "Asus",
            model = "ZenBook",
            remarks = "Hinges broken",
            tmJobId = "TM-1005",
            labels = fixedLabels
        )

        val expected = """
            Job no: D-00042
            Customer: Walk-in
            Device: Asus ZenBook
            Problem: Hinges broken
            TechMech ref: TM-1005

            Estimation & Status?
        """.trimIndent()

        assertEquals(expected, resultBlank)
        assertEquals(expected, resultWalkIn)
    }

    @Test
    fun `exact line order and blank line before Estimation & Status`() {
        val result = TechMechEnquiryMessage.build(
            jobNumber = "D-00001",
            customerName = "Alice",
            make = "Acer",
            model = "Aspire",
            remarks = "Water damage",
            tmJobId = "TM-001",
            labels = fixedLabels
        )

        val lines = result.lines()
        assertEquals(7, lines.size)
        assertEquals("Job no: D-00001", lines[0])
        assertEquals("Customer: Alice", lines[1])
        assertEquals("Device: Acer Aspire", lines[2])
        assertEquals("Problem: Water damage", lines[3])
        assertEquals("TechMech ref: TM-001", lines[4])
        assertEquals("", lines[5])
        assertEquals("Estimation & Status?", lines[6])
    }

    @Test
    fun `TechMech enquiry phone constant normalizes to WhatsApp digits 919809194221`() {
        val digits = PhoneNormalizer.toWhatsAppDigits(TechMechContact.ENQUIRY_PHONE)
        assertEquals("919809194221", digits)
    }
}
