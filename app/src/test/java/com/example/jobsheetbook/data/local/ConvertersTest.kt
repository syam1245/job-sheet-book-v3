package com.example.jobsheetbook.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `list of strings serializes and deserializes correctly`() {
        val originalList = listOf("Motherboard", "Battery", "Keyboard")
        
        val serialized = converters.toStringList(originalList)
        val deserialized = converters.fromStringList(serialized)
        
        assertEquals(originalList, deserialized)
    }

    @Test
    fun `empty list serializes and deserializes correctly`() {
        val originalList = emptyList<String>()
        
        val serialized = converters.toStringList(originalList)
        val deserialized = converters.fromStringList(serialized)
        
        assertEquals(originalList, deserialized)
    }
    
    @Test
    fun `malformed list string returns empty list`() {
        val deserialized = converters.fromStringList("invalid json")
        assertEquals(emptyList<String>(), deserialized)
    }

    @Test
    fun `map of string to boolean serializes and deserializes correctly`() {
        val originalMap = mapOf(
            "adapter" to true,
            "cord" to false,
            "bag" to true
        )
        
        val serialized = converters.toMap(originalMap)
        val deserialized = converters.fromMap(serialized)
        
        assertEquals(originalMap, deserialized)
    }

    @Test
    fun `empty map serializes and deserializes correctly`() {
        val originalMap = emptyMap<String, Boolean>()
        
        val serialized = converters.toMap(originalMap)
        val deserialized = converters.fromMap(serialized)
        
        assertEquals(originalMap, deserialized)
    }
    
    @Test
    fun `malformed map string returns empty map`() {
        val deserialized = converters.fromMap("invalid json")
        assertEquals(emptyMap<String, Boolean>(), deserialized)
    }
}
