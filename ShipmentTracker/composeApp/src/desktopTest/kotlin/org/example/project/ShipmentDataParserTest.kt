package org.example.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertContentEquals
import java.io.File
import java.io.IOException

class ShipmentDataParserTest {

    private val parser = ShipmentDataParser()

    @Test
    fun testReadFileWithValidFile() {
        // Create a temporary test file
        val testFile = File("test_data.txt")
        try {
            testFile.writeText("Created,s10000,1234567890\nShipped,s10001,1234567891")
            
            val result = parser.readFile("test_data.txt")
            
            assertEquals(2, result.size)
            assertEquals("Created,s10000,1234567890", result[0])
            assertEquals("Shipped,s10001,1234567891", result[1])
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testReadFileWithEmptyFile() {
        val testFile = File("empty_test.txt")
        try {
            testFile.writeText("")
            
            val result = parser.readFile("empty_test.txt")
            
            assertEquals(0, result.size)
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testReadFileWithNonExistentFile() {
        val result = parser.readFile("non_existent_file.txt")
        assertEquals(0, result.size)
    }

    @Test
    fun testParseUpdateWithValidFormat() {
        val updateString = "Created,s10000,1234567890"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("s10000", result[0]) // shipmentId
        assertEquals("Create", result[1]) // normalized updateType
        assertEquals("1234567890", result[2]) // timestamp
        assertEquals("", result[3]) // location
        assertEquals("", result[4]) // notes
    }

    @Test
    fun testParseUpdateWithLocationUpdate() {
        val updateString = "Location,s10000,1234567890,New York City"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("s10000", result[0])
        assertEquals("Location", result[1])
        assertEquals("1234567890", result[2])
        assertEquals("New York City", result[3]) // location
        assertEquals("", result[4])
    }

    @Test
    fun testParseUpdateWithNoteAdded() {
        val updateString = "NoteAdded,s10000,1234567890,Package requires signature"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("s10000", result[0])
        assertEquals("NoteAdded", result[1])
        assertEquals("1234567890", result[2])
        assertEquals("", result[3])
        assertEquals("Package requires signature", result[4]) // notes
    }

    @Test
    fun testParseUpdateWithShippedType() {
        val updateString = "Shipped,s10000,1234567890,1234567900"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("s10000", result[0])
        assertEquals("Shipped", result[1])
        assertEquals("1234567900", result[2]) // For shipped, use 4th component as timestamp
        assertEquals("", result[3])
        assertEquals("", result[4])
    }

    @Test
    fun testParseUpdateWithDelayedType() {
        val updateString = "Delayed,s10000,1234567890,1234567950"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("s10000", result[0])
        assertEquals("Delayed", result[1])
        assertEquals("1234567950", result[2]) // For delayed, use 4th component as timestamp
        assertEquals("", result[3])
        assertEquals("", result[4])
    }

    @Test
    fun testParseUpdateWithInvalidFormat() {
        val updateString = "InvalidFormat"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(0, result.size)
    }

    @Test
    fun testParseUpdateWithEmptyString() {
        val result = parser.parseUpdate("")
        assertEquals(0, result.size)
    }

    @Test
    fun testParseUpdateWithBlankString() {
        val result = parser.parseUpdate("   ")
        assertEquals(0, result.size)
    }

    @Test
    fun testParseUpdateWithMinimumComponents() {
        val updateString = "Created,s10000"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("s10000", result[0])
        assertEquals("Create", result[1])
        assertEquals("", result[2]) // empty timestamp
        assertEquals("", result[3])
        assertEquals("", result[4])
    }

    @Test
    fun testNormalizeUpdateTypes() {
        val testCases = mapOf(
            "created" to "Create",
            "shipped" to "Shipped",
            "location" to "Location",
            "delivered" to "Delivered",
            "delayed" to "Delayed",
            "lost" to "Lost",
            "canceled" to "Cancelled",
            "noteadded" to "NoteAdded",
            "CustomType" to "CustomType"
        )
        
        testCases.forEach { (input, expected) ->
            val result = parser.parseUpdate("$input,s10000,123")
            assertEquals(expected, result[1], "Failed to normalize $input to $expected")
        }
    }

    @Test
    fun testIsValidUpdateFormatWithValidInputs() {
        assertTrue(parser.isValidUpdateFormat("Created,s10000"))
        assertTrue(parser.isValidUpdateFormat("Shipped,s10001,1234567890"))
        assertTrue(parser.isValidUpdateFormat("Location,s10002,1234567890,New York"))
        assertTrue(parser.isValidUpdateFormat("NoteAdded,s10003,1234567890,Important note"))
    }

    @Test
    fun testIsValidUpdateFormatWithInvalidInputs() {
        assertFalse(parser.isValidUpdateFormat(""))
        assertFalse(parser.isValidUpdateFormat("   "))
        assertFalse(parser.isValidUpdateFormat("OnlyOneComponent"))
        assertFalse(parser.isValidUpdateFormat(",s10000")) // empty first component
        assertFalse(parser.isValidUpdateFormat("Created,")) // empty second component
    }

    @Test
    fun testParseTimestampWithValidTimestamp() {
        val timestamp = parser.parseTimestamp("1234567890")
        assertEquals(1234567890L, timestamp)
    }

    @Test
    fun testParseTimestampWithEmptyString() {
        val timestamp = parser.parseTimestamp("")
        assertTrue(timestamp > 0) // Should return current time
        assertTrue(timestamp <= System.currentTimeMillis())
    }

    @Test
    fun testParseTimestampWithInvalidFormat() {
        val timestamp = parser.parseTimestamp("invalid_timestamp")
        assertTrue(timestamp > 0) // Should return current time
        assertTrue(timestamp <= System.currentTimeMillis())
    }

    @Test
    fun testParseTimestampWithBlankString() {
        val timestamp = parser.parseTimestamp("   ")
        assertTrue(timestamp > 0)
        assertTrue(timestamp <= System.currentTimeMillis())
    }

    @Test
    fun testParseUpdateWithWhitespace() {
        val updateString = " Created , s10000 , 1234567890 "
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("s10000", result[0]) // Should trim whitespace
        assertEquals("Create", result[1])
        assertEquals("1234567890", result[2])
    }

    @Test
    fun testReadFileWithSingleLine() {
        val testFile = File("single_line_test.txt")
        try {
            testFile.writeText("Created,s10000,1234567890")
            
            val result = parser.readFile("single_line_test.txt")
            
            assertEquals(1, result.size)
            assertEquals("Created,s10000,1234567890", result[0])
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testReadFileWithMultipleLines() {
        val testFile = File("multi_line_test.txt")
        try {
            val content = """
                Created,s10000,1234567890
                Shipped,s10000,1234567891
                Location,s10000,1234567892,Distribution Center
                Delivered,s10000,1234567893
            """.trimIndent()
            
            testFile.writeText(content)
            
            val result = parser.readFile("multi_line_test.txt")
            
            assertEquals(4, result.size)
            assertEquals("Created,s10000,1234567890", result[0])
            assertEquals("Shipped,s10000,1234567891", result[1])
            assertEquals("Location,s10000,1234567892,Distribution Center", result[2])
            assertEquals("Delivered,s10000,1234567893", result[3])
        } finally {
            testFile.delete()
        }
    }
} 