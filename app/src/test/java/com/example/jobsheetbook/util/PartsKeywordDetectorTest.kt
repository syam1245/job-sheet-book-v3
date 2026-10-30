package com.example.jobsheetbook.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PartsKeywordDetectorTest {

    @Test
    fun `detectParts returns empty list for blank remarks`() {
        val result = PartsKeywordDetector.detectParts("   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectParts identifies multiple hardware categories correctly`() {
        val remarks = "Customer says the display is broken and battery is not charging. The hdd might be failing too."
        val result = PartsKeywordDetector.detectParts(remarks)
        
        assertEquals(3, result.size)
        assertTrue(result.contains("Display"))
        assertTrue(result.contains("Battery"))
        assertTrue(result.contains("Storage"))
    }

    @Test
    fun `detectParts ignores case and handles edge inputs`() {
        // "bAtt" -> Battery, "kEyPAd" -> Keyboard, "moBo" -> Motherboard
        val remarks = "bAtt died, kEyPAd keys sticky, moBo seems fried."
        val result = PartsKeywordDetector.detectParts(remarks)
        
        assertEquals(3, result.size)
        assertTrue(result.contains("Battery"))
        assertTrue(result.contains("Keyboard"))
        assertTrue(result.contains("Motherboard"))
    }
    
    @Test
    fun `detectParts detects all 12 categories`() {
        // One keyword from each of the 12 categories
        val remarks = "display batt kb hdd speaker charger ram heat board webcam touchpad power switch"
        val result = PartsKeywordDetector.detectParts(remarks)
        
        assertEquals(12, result.size)
        val expected = listOf(
            "Display", "Battery", "Keyboard", "Storage", "Speaker", "Charging", 
            "RAM", "Cooling", "Motherboard", "Camera", "Touchpad", "Power"
        )
        expected.forEach { category ->
            assertTrue("Expected to detect $category", result.contains(category))
        }
    }
}
