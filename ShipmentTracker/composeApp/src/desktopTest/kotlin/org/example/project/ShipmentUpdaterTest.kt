package org.example.project

import org.example.project.Shipment.Shipment
import org.example.project.Shipment.ShipmentUpdater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

class ShipmentUpdaterTest {

    private lateinit var updater: ShipmentUpdater
    private lateinit var testShipment: Shipment

    @BeforeTest
    fun setup() {
        updater = ShipmentUpdater()
        testShipment = Shipment(
            status = "Created",
            id = "SHIP001",
            expectedDeliveryDate = 1234567890L,
            currentLocation = "Warehouse A"
        )
    }

    @Test
    fun testProcessCreateUpdate() {
        val result = updater.processUpdate("Create", testShipment, 1234567891L)
        
        assertEquals("", result.previousStatus)
        assertEquals("Created", result.newStatus)
        assertEquals(1234567891L, result.timestamp)
    }

    @Test
    fun testProcessShippedUpdate() {
        testShipment.status = "Created"
        val result = updater.processUpdate("Shipped", testShipment, 1234567892L)
        
        assertEquals("Created", result.previousStatus)
        assertEquals("Shipped", result.newStatus)
        assertEquals(1234567892L, result.timestamp)
    }

    @Test
    fun testProcessLocationUpdate() {
        testShipment.status = "Shipped"
        val result = updater.processUpdate("Location", testShipment, 1234567893L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("In Transit", result.newStatus)
        assertEquals(1234567893L, result.timestamp)
    }

    @Test
    fun testProcessDeliveredUpdate() {
        testShipment.status = "In Transit"
        val result = updater.processUpdate("Delivered", testShipment, 1234567894L)
        
        assertEquals("In Transit", result.previousStatus)
        assertEquals("Delivered", result.newStatus)
        assertEquals(1234567894L, result.timestamp)
    }

    @Test
    fun testProcessDelayedUpdate() {
        testShipment.status = "Shipped"
        val result = updater.processUpdate("Delayed", testShipment, 1234567895L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("Delayed", result.newStatus)
        assertEquals(1234567895L, result.timestamp)
    }

    @Test
    fun testProcessLostUpdate() {
        testShipment.status = "In Transit"
        val result = updater.processUpdate("Lost", testShipment, 1234567896L)
        
        assertEquals("In Transit", result.previousStatus)
        assertEquals("Lost", result.newStatus)
        assertEquals(1234567896L, result.timestamp)
    }

    @Test
    fun testProcessCancelledUpdate() {
        testShipment.status = "Created"
        val result = updater.processUpdate("Cancelled", testShipment, 1234567897L)
        
        assertEquals("Created", result.previousStatus)
        assertEquals("Cancelled", result.newStatus)
        assertEquals(1234567897L, result.timestamp)
    }

    @Test
    fun testProcessNoteAddedUpdate() {
        testShipment.status = "Shipped"
        val result = updater.processUpdate("NoteAdded", testShipment, 1234567898L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("Shipped", result.newStatus) // Status shouldn't change for note added
        assertEquals(1234567898L, result.timestamp)
    }

    @Test
    fun testProcessUnknownUpdateType() {
        assertFailsWith<IllegalArgumentException> {
            updater.processUpdate("UnknownType", testShipment, 1234567899L)
        }
    }

    @Test
    fun testProcessUpdateWithEmptyUpdateType() {
        assertFailsWith<IllegalArgumentException> {
            updater.processUpdate("", testShipment, 1234567900L)
        }
    }

    @Test
    fun testProcessUpdateWithNullValues() {
        // Test with current timestamp
        val result = updater.processUpdate("Create", testShipment, System.currentTimeMillis())
        
        assertEquals("", result.previousStatus)
        assertEquals("Created", result.newStatus)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun testAllUpdateTypesAreRegistered() {
        val expectedUpdateTypes = setOf(
            "Create", "Shipped", "Location", "Delivered", 
            "Delayed", "Lost", "Cancelled", "NoteAdded"
        )
        
        expectedUpdateTypes.forEach { updateType ->
            // Should not throw exception
            val result = updater.processUpdate(updateType, testShipment, 1234567890L)
            assertNotNull(result)
            assertEquals(1234567890L, result.timestamp)
        }
    }

    @Test
    fun testUpdateSequence() {
        // Test a typical shipment lifecycle
        val timestamps = (1234567890L..1234567898L).toList()
        val updates = listOf(
            "Create" to "",
            "Shipped" to "Created",
            "Location" to "Shipped", 
            "Delivered" to "In Transit"
        )
        
        var currentStatus = ""
        updates.forEachIndexed { index, (updateType, expectedPrevious) ->
            testShipment.status = currentStatus
            val result = updater.processUpdate(updateType, testShipment, timestamps[index])
            
            assertEquals(expectedPrevious, result.previousStatus)
            assertEquals(timestamps[index], result.timestamp)
            currentStatus = result.newStatus
        }
    }

    @Test
    fun testProcessUpdatePreservesShipmentState() {
        val originalId = testShipment.id
        val originalLocation = testShipment.currentLocation
        val originalDeliveryDate = testShipment.expectedDeliveryDate
        
        updater.processUpdate("Shipped", testShipment, 1234567890L)
        
        // Shipment properties should remain unchanged (updater doesn't modify shipment)
        assertEquals(originalId, testShipment.id)
        assertEquals(originalLocation, testShipment.currentLocation)
        assertEquals(originalDeliveryDate, testShipment.expectedDeliveryDate)
    }

    @Test
    fun testMultipleUpdatesOnSameShipment() {
        val updateTypes = listOf("Create", "Shipped", "Location", "Delivered")
        var timestamp = 1234567890L
        
        updateTypes.forEach { updateType ->
            val result = updater.processUpdate(updateType, testShipment, timestamp)
            assertNotNull(result)
            assertEquals(timestamp, result.timestamp)
            
            // Update shipment status for next iteration
            testShipment.status = result.newStatus
            timestamp++
        }
        
        assertEquals("Delivered", testShipment.status)
    }

    @Test
    fun testCaseInsensitivityNotSupported() {
        // The updater expects exact case matching
        assertFailsWith<IllegalArgumentException> {
            updater.processUpdate("create", testShipment, 1234567890L) // lowercase
        }
        
        assertFailsWith<IllegalArgumentException> {
            updater.processUpdate("SHIPPED", testShipment, 1234567890L) // uppercase
        }
    }

    @Test
    fun testUpdateWithZeroTimestamp() {
        val result = updater.processUpdate("Create", testShipment, 0L)
        
        assertEquals("", result.previousStatus)
        assertEquals("Created", result.newStatus)
        assertEquals(0L, result.timestamp)
    }

    @Test
    fun testUpdateWithNegativeTimestamp() {
        val result = updater.processUpdate("Create", testShipment, -1L)
        
        assertEquals("", result.previousStatus)
        assertEquals("Created", result.newStatus)
        assertEquals(-1L, result.timestamp)
    }

    @Test
    fun testShipmentUpdaterInitialization() {
        // Test that a new updater instance has all strategies registered
        val newUpdater = ShipmentUpdater()
        
        val result = newUpdater.processUpdate("Create", testShipment, 1234567890L)
        assertNotNull(result)
        assertEquals("Created", result.newStatus)
    }
} 