package com.example.jobsheetbook.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNormalizerTest {

    @Test
    fun `table-driven test for valid Indian number 9020448019 variants`() {
        val inputs = listOf(
            "9020448019",
            "+919020448019",
            "+91 9020448019",
            "+91 90204 48019",
            "+919020 448019",
            "09020448019",
            "09020 448019",
            "919020448019",
            "91 90204 48019",
            "0091 9020448019",
            "091 9020448019",
            "(90204) 48019",
            "90204-48019",
            "90204.48019",
            "  9020448019  "
        )

        for (input in inputs) {
            val result = PhoneNormalizer.normalize(input)
            assertEquals("Expected Valid(\"9020448019\") for input: '$input'", PhoneNormalizer.Result.Valid("9020448019"), result)
        }
    }

    @Test
    fun `valid international numbers`() {
        val testCases = mapOf(
            "+971 50 123 4567" to "971501234567",
            "00971501234567" to "971501234567"
        )

        for ((input, expected) in testCases) {
            val result = PhoneNormalizer.normalize(input)
            assertEquals("Expected Valid(\"$expected\") for input: '$input'", PhoneNormalizer.Result.Valid(expected), result)
        }
    }

    @Test
    fun `valid landline stores 4712345678 and toWhatsAppDigits returns null`() {
        val result = PhoneNormalizer.normalize("04712345678")
        assertEquals(PhoneNormalizer.Result.Valid("4712345678"), result)
        assertNull(PhoneNormalizer.toWhatsAppDigits("4712345678"))
    }

    @Test
    fun `blank inputs return Blank`() {
        assertEquals(PhoneNormalizer.Result.Blank, PhoneNormalizer.normalize(""))
        assertEquals(PhoneNormalizer.Result.Blank, PhoneNormalizer.normalize("   "))
        assertEquals(PhoneNormalizer.Result.Blank, PhoneNormalizer.normalize(null))
    }

    @Test
    fun `invalid inputs return Invalid`() {
        val invalidInputs = listOf(
            "12345",
            "902044801",
            "90204480199",
            "+91 902044801",
            "971501234567",
            "+1",
            "abc",
            "9020x448019"
        )

        for (input in invalidInputs) {
            val result = PhoneNormalizer.normalize(input)
            assertEquals("Expected Invalid for input: '$input'", PhoneNormalizer.Result.Invalid, result)
        }
    }

    @Test
    fun `formatForDisplay, toWhatsAppDigits, and toDialNumber for Indian number`() {
        val stored = "9020448019"
        assertEquals("90204 48019", PhoneNormalizer.formatForDisplay(stored))
        assertEquals("919020448019", PhoneNormalizer.toWhatsAppDigits(stored))
        assertEquals("+919020448019", PhoneNormalizer.toDialNumber(stored))
    }

    @Test
    fun `formatForDisplay, toWhatsAppDigits, and toDialNumber for international number`() {
        val stored = "971501234567"
        assertEquals("+971501234567", PhoneNormalizer.formatForDisplay(stored))
        assertEquals("971501234567", PhoneNormalizer.toWhatsAppDigits(stored))
        assertEquals("+971501234567", PhoneNormalizer.toDialNumber(stored))
    }

    @Test
    fun `formatForDisplay, toWhatsAppDigits, and toDialNumber for legacy input with prefix`() {
        val legacy = "+91 90204 48019"
        assertEquals("90204 48019", PhoneNormalizer.formatForDisplay(legacy))
        assertEquals("919020448019", PhoneNormalizer.toWhatsAppDigits(legacy))
        assertEquals("+919020448019", PhoneNormalizer.toDialNumber(legacy))
    }

    @Test
    fun `formatForDisplay, toWhatsAppDigits, and toDialNumber for garbage input falling back to raw text`() {
        val garbage = "invalid-garbage"
        assertEquals("invalid-garbage", PhoneNormalizer.formatForDisplay(garbage))
        assertNull(PhoneNormalizer.toWhatsAppDigits(garbage))
        assertNull(PhoneNormalizer.toDialNumber(garbage))
    }
}
