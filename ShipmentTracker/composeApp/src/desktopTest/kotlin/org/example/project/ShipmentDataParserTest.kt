package org.example.project

import org.example.project.Shipment.ShipmentDataParser
import kotlin.test.*
import java.io.File

class ShipmentDataParserTest {

    private lateinit var parser: ShipmentDataParser

    @BeforeTest
    fun setup() {
        parser = ShipmentDataParser()
    }

    @Test
    fun testParseUpdateValidFormat() {
        val updateString = "shipped,SHIP001,1234567890"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("SHIP001", result[0]) // shipmentId
        assertEquals("Shipped", result[1]) // normalized updateType
        assertEquals("1234567890", result[2]) // timestamp
        assertEquals("", result[3]) // location
        assertEquals("", result[4]) // notes
    }

    @Test
    fun testParseUpdateWithLocation() {
        val updateString = "location,SHIP002,1234567890,Distribution Center"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("SHIP002", result[0])
        assertEquals("Location", result[1])
        assertEquals("1234567890", result[2])
        assertEquals("Distribution Center", result[3])
        assertEquals("", result[4])
    }

    @Test
    fun testParseUpdateWithNotes() {
        val updateString = "noteadded,SHIP003,1234567890,,Important package"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("SHIP003", result[0])
        assertEquals("NoteAdded", result[1])
        assertEquals("1234567890", result[2])
        assertEquals("", result[3])
        assertEquals("Important package", result[4])
    }

    @Test
    fun testParseUpdateInvalidFormat() {
        val updateString = "invalid"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(0, result.size)
    }

    @Test
    fun testParseUpdateBlankInput() {
        val result = parser.parseUpdate("")
        assertEquals(0, result.size)
        
        val result2 = parser.parseUpdate("   ")
        assertEquals(0, result2.size)
    }

    @Test
    fun testParseUpdateMinimalValid() {
        val updateString = "create,SHIP004"
        val result = parser.parseUpdate(updateString)
        
        assertEquals(5, result.size)
        assertEquals("SHIP004", result[0])
        assertEquals("Create", result[1])
        assertEquals("", result[2]) // empty timestamp
        assertEquals("", result[3])
        assertEquals("", result[4])
    }

    @Test
    fun testNormalizeUpdateType() {
        // Test various input formats are normalized correctly
        val testCases = listOf(
            "created" to "Create",
            "SHIPPED" to "Shipped", 
            "Location" to "Location",
            "delivered" to "Delivered",
            "DELAYED" to "Delayed",
            "lost" to "Lost",
            "canceled" to "Cancelled",
            "NOTEADDED" to "NoteAdded"
        )
        
        testCases.forEach { (input, expected) ->
            val result = parser.parseUpdate("$input,SHIP001,1000")
            assertEquals(expected, result[1], "Failed for input: $input")
        }
    }

    @Test
    fun testIsValidUpdateFormat() {
        assertTrue(parser.isValidUpdateFormat("create,SHIP001"))
        assertTrue(parser.isValidUpdateFormat("shipped,SHIP002,1000"))
        assertTrue(parser.isValidUpdateFormat("location,SHIP003,1000,Hub"))
        
        assertFalse(parser.isValidUpdateFormat(""))
        assertFalse(parser.isValidUpdateFormat("   "))
        assertFalse(parser.isValidUpdateFormat("onlyonefield"))
        assertFalse(parser.isValidUpdateFormat(","))
        assertFalse(parser.isValidUpdateFormat("create,"))
    }

    @Test
    fun testParseTimestamp() {
        assertEquals(1234567890L, parser.parseTimestamp("1234567890"))
        assertEquals(0L, parser.parseTimestamp("0"))
        
        // Should return current time for invalid input
        val currentTime = System.currentTimeMillis()
        val result = parser.parseTimestamp("invalid")
        assertTrue(result >= currentTime - 1000) // Within last second
        
        val result2 = parser.parseTimestamp("")
        assertTrue(result2 >= currentTime - 1000)
    }

    @Test
    fun testReadFileWithTempFile() {
        val tempFile = File.createTempFile("test", ".txt")
        try {
            tempFile.writeText("line1\nline2\nline3")
            
            val result = parser.readFile(tempFile.absolutePath)
            
            assertEquals(3, result.size)
            assertEquals("line1", result[0])
            assertEquals("line2", result[1])
            assertEquals("line3", result[2])
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testReadFileNonExistent() {
        val result = parser.readFile("non_existent_file.txt")
        assertEquals(0, result.size)
    }

    @Test
    fun testReadFileEmpty() {
        val tempFile = File.createTempFile("empty", ".txt")
        try {
            tempFile.writeText("")
            
            val result = parser.readFile(tempFile.absolutePath)
            assertEquals(0, result.size)
        } finally {
            tempFile.delete()
        }
    }
} 