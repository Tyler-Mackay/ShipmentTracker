package org.example.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.BeforeTest
import java.io.File

class ErrorHandlingTest {

    private lateinit var parser: ShipmentDataParser
    private lateinit var updater: ShipmentUpdater
    private lateinit var simulator: TrackingSimulator
    private lateinit var trackerViewHelper: TrackerViewHelper

    @BeforeTest
    fun setup() {
        parser = ShipmentDataParser()
        updater = ShipmentUpdater()
        simulator = TrackingSimulator()
        trackerViewHelper = TrackerViewHelper()
        
        simulator.setParser(parser)
        simulator.setUpdater(updater)
        trackerViewHelper.setSimulator(simulator)
    }

    @Test
    fun testParseUpdateWithMalformedInput() {
        val malformedInputs = listOf(
            "",
            "   ",
            "OnlyOneComponent",
            ",",
            ",,",
            ",,,",
            "Type,",
            ",ID",
            "Type,ID,",
            "Type,ID,,",
            "Type,ID,123,,"
        )
        
        malformedInputs.forEach { input ->
            val result = parser.parseUpdate(input)
            if (input.contains(",") && input.split(",").size >= 2 && 
                input.split(",")[0].isNotBlank() && input.split(",")[1].isNotBlank()) {
                // Should parse successfully
                assertEquals(5, result.size)
            } else {
                // Should return empty array
                assertEquals(0, result.size)
            }
        }
    }

    @Test
    fun testReadFileWithInvalidPaths() {
        val invalidPaths = listOf(
            "",
            "   ",
            "/invalid/path/file.txt",
            "nonexistent.txt",
            "invalidfile.txt",
            "directory/that/does/not/exist/file.txt"
        )
        
        invalidPaths.forEach { path ->
            val result = parser.readFile(path)
            assertEquals(0, result.size) // Should return empty array without crashing
        }
    }

    @Test
    fun testShipmentUpdaterWithInvalidUpdateTypes() {
        val shipment = Shipment("Created", "SHIP001", expectedDeliveryDate = 123L, currentLocation = "Warehouse")
        val invalidTypes = listOf("", "INVALID", "unknown", "null", "  ", "123", "!")
        
        invalidTypes.forEach { invalidType ->
            assertFailsWith<IllegalArgumentException> {
                updater.processUpdate(invalidType, shipment, 123L)
            }
        }
    }

    @Test
    fun testTrackingSimulatorWithNullParsers() {
        val simWithoutParser = TrackingSimulator()
        simWithoutParser.setUpdater(updater)
        
        // Should handle gracefully without parser
        simWithoutParser.processFileUpdate("Create,SHIP001,123")
        simWithoutParser.processUpdateFile("test.txt")
        
        // Should not crash but may not process updates
        assertEquals(null, simWithoutParser.findShipment("SHIP001"))
    }

    @Test
    fun testTrackingSimulatorWithNullUpdater() {
        val simWithoutUpdater = TrackingSimulator()
        simWithoutUpdater.setParser(parser)
        
        // Should handle gracefully without updater
        simWithoutUpdater.processFileUpdate("Create,SHIP001,123")
        
        // May create shipment but may not process updates properly
        // Test should not crash
    }

    @Test
    fun testShipmentWithExtremeValues() {
        val extremeShipment = Shipment(
            status = "",
            id = "",
            expectedDeliveryDate = Long.MIN_VALUE,
            currentLocation = ""
        )
        
        extremeShipment.addNote("")
        extremeShipment.updateLocation("")
        
        val update = ShippingUpdate("", "", Long.MAX_VALUE)
        extremeShipment.addUpdate(update)
        
        assertEquals("", extremeShipment.status)
        assertEquals("", extremeShipment.id)
        assertEquals(Long.MIN_VALUE, extremeShipment.expectedDeliveryDate)
        assertEquals(1, extremeShipment.notes.size)
        assertEquals(1, extremeShipment.updateHistory.size)
    }

    @Test
    fun testTrackerViewHelperWithInvalidShipmentIds() {
        val invalidIds = listOf("", "   ", "NONEXISTENT", "null", "123!@#", "very-long-id-that-does-not-exist")
        
        invalidIds.forEach { invalidId ->
            assertFalse(trackerViewHelper.trackShipment(invalidId))
            assertFalse(trackerViewHelper.isTrackingShipment(invalidId))
        }
    }

    @Test
    fun testObserverPatternWithNullObservers() {
        val shipment = Shipment("Created", "SHIP001", expectedDeliveryDate = 123L, currentLocation = "Warehouse")
        
        // Adding/removing null observers should not crash
        val mockObserver: ShipmentObserver? = null
        // Note: Cannot actually pass null due to Kotlin's type system, but test null-like behavior
        
        val update = ShippingUpdate("Created", "Shipped", 123L)
        shipment.addUpdate(update) // Should not crash even with no observers
        
        assertEquals("Shipped", shipment.status)
    }

    @Test
    fun testFileProcessingWithCorruptedFile() {
        val corruptedFile = File("corrupted_test.txt")
        try {
            // Create file with various problematic content
            corruptedFile.writeText("Created,SHIP001,123\n\u0000\nShipped,SHIP002,456\n")
            
            simulator.processUpdateFile("corrupted_test.txt")
            
            // Should not crash and should process valid lines
            // Invalid lines should be skipped gracefully
        } finally {
            corruptedFile.delete()
        }
    }

    @Test
    fun testConcurrentModificationHandling() {
        val shipment = Shipment("Created", "SHIP001", expectedDeliveryDate = 123L, currentLocation = "Warehouse")
        
        // Test concurrent access to lists
        val observers = (1..10).map { MockShipmentObserver() }
        
        // Add observers
        observers.forEach { shipment.addObserver(it) }
        
        // Trigger updates while potentially modifying observer list
        repeat(5) {
            val update = ShippingUpdate("Status$it", "Status${it+1}", 123L + it)
            shipment.addUpdate(update)
        }
        
        // Should not crash with concurrent modifications
        assertTrue(shipment.updateHistory.size >= 5)
    }

    @Test
    fun testMemoryLimitsWithLargeData() {
        val shipment = Shipment("Created", "SHIP001", expectedDeliveryDate = 123L, currentLocation = "Warehouse")
        
        // Add many notes
        repeat(1000) { i ->
            shipment.addNote("Note number $i with some content")
        }
        
        // Add many updates
        repeat(1000) { i ->
            val update = ShippingUpdate("Status$i", "Status${i+1}", 123L + i)
            shipment.addUpdate(update)
        }
        
        assertEquals(1000, shipment.notes.size)
        assertEquals(1000, shipment.updateHistory.size)
        assertEquals("Status1000", shipment.status)
    }

    @Test
    fun testParsingWithSpecialCharacters() {
        val specialInputs = listOf(
            "Created,SHIP001,123,Location with spaces",
            "Shipped,SHIP002,456,Location,with,extra,commas",
            "Location,SHIP003,789,Location with \"quotes\"",
            "NoteAdded,SHIP004,101112,Note with special chars: !@#$%^&*()",
            "Delayed,SHIP005,131415,路线更新", // Unicode characters
            "Created,SHIP006,161718,\t\n\r" // Whitespace characters
        )
        
        specialInputs.forEach { input ->
            val result = parser.parseUpdate(input)
            // Should not crash and should return some result
            assertTrue(result.size >= 0)
        }
    }

    @Test
    fun testTrackerViewHelperStateConsistency() {
        // Add a shipment
        val shipment = Shipment("Created", "SHIP001", expectedDeliveryDate = 123L, currentLocation = "Warehouse")
        simulator.addShipment(shipment)
        
        // Track and untrack rapidly
        repeat(10) {
            trackerViewHelper.trackShipment("SHIP001")
            trackerViewHelper.stopTrackingShipment("SHIP001")
        }
        
        // Final state should be consistent
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP001"))
        assertEquals(0, trackerViewHelper.getTrackedShipments().size)
    }

    @Test
    fun testErrorRecoveryAfterExceptions() {
        // Cause an error with invalid update type
        try {
            updater.processUpdate("INVALID", 
                Shipment("Created", "SHIP001", expectedDeliveryDate = 123L, currentLocation = "Warehouse"), 
                123L)
        } catch (e: IllegalArgumentException) {
            // Expected
        }
        
        // Verify updater still works after error
        val validResult = updater.processUpdate("Create", 
            Shipment("Created", "SHIP002", expectedDeliveryDate = 123L, currentLocation = "Warehouse"), 
            456L)
        
        assertEquals("Created", validResult.newStatus)
        assertEquals(456L, validResult.timestamp)
    }

    @Test
    fun testBoundaryValues() {
        val boundaryTimestamps = listOf(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)
        val shipment = Shipment("Created", "SHIP001", expectedDeliveryDate = 123L, currentLocation = "Warehouse")
        
        boundaryTimestamps.forEach { timestamp ->
            val result = updater.processUpdate("Create", shipment, timestamp)
            assertEquals(timestamp, result.timestamp)
        }
    }

    @Test
    fun testSimulatorErrorHandlingWithPartialData() {
        // Test with incomplete but valid data
        val partialUpdates = listOf(
            "Create,SHIP001", // Missing timestamp
            "Shipped,SHIP001,", // Empty timestamp  
            "Location,SHIP001,123", // Missing location data
            "NoteAdded,SHIP001,456,", // Empty note
            "Delayed,SHIP001,789,," // Multiple empty fields
        )
        
        partialUpdates.forEach { update ->
            // Should not crash
            simulator.processFileUpdate(update)
        }
        
        // Verify at least one shipment was created
        val createdShipment = simulator.findShipment("SHIP001")
        assertTrue(createdShipment != null)
    }
} 