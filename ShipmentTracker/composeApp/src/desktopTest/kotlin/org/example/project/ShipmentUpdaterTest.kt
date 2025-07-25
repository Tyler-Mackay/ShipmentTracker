package org.example.project

import org.example.project.Shipment.*
import org.example.project.ShippingUpdate.*
import kotlin.test.*

class ShipmentUpdaterTest {

    private lateinit var updater: ShipmentUpdater
    private lateinit var testShipment: Shipment

    @BeforeTest
    fun setup() {
        updater = ShipmentUpdater()
        testShipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "TEST001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L,
            currentLocation = "Warehouse"
        )
    }

    @Test
    fun testProcessCreateUpdate() {
        val result = updater.processUpdate("Create", testShipment, 1000L)
        
        assertEquals("", result.previousStatus)
        assertEquals("Created", result.newStatus)
        assertEquals(1000L, result.timestamp)
    }

    @Test
    fun testProcessShippedUpdate() {
        testShipment.status = "Created"
        val result = updater.processUpdate("Shipped", testShipment, 2000L)
        
        assertEquals("Created", result.previousStatus)
        assertEquals("Shipped", result.newStatus)
        assertEquals(2000L, result.timestamp)
    }

    @Test
    fun testProcessLocationUpdate() {
        testShipment.status = "Shipped"
        val result = updater.processUpdate("Location", testShipment, 3000L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("In Transit", result.newStatus)
        assertEquals(3000L, result.timestamp)
    }

    @Test
    fun testProcessDeliveredUpdate() {
        testShipment.status = "In Transit"
        val result = updater.processUpdate("Delivered", testShipment, 4000L)
        
        assertEquals("In Transit", result.previousStatus)
        assertEquals("Delivered", result.newStatus)
        assertEquals(4000L, result.timestamp)
    }

    @Test
    fun testProcessDelayedUpdate() {
        testShipment.status = "Shipped"
        val result = updater.processUpdate("Delayed", testShipment, 5000L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("Delayed", result.newStatus)
        assertEquals(5000L, result.timestamp)
    }

    @Test
    fun testProcessLostUpdate() {
        testShipment.status = "In Transit"
        val result = updater.processUpdate("Lost", testShipment, 6000L)
        
        assertEquals("In Transit", result.previousStatus)
        assertEquals("Lost", result.newStatus)
        assertEquals(6000L, result.timestamp)
    }

    @Test
    fun testProcessCancelledUpdate() {
        testShipment.status = "Created"
        val result = updater.processUpdate("Cancelled", testShipment, 7000L)
        
        assertEquals("Created", result.previousStatus)
        assertEquals("Cancelled", result.newStatus)
        assertEquals(7000L, result.timestamp)
    }

    @Test
    fun testProcessNoteAddedUpdate() {
        testShipment.status = "Shipped"
        val result = updater.processUpdate("NoteAdded", testShipment, 8000L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("Shipped", result.newStatus) // Should preserve current status
        assertEquals(8000L, result.timestamp)
    }

    @Test
    fun testProcessInvalidUpdate() {
        assertFailsWith<IllegalArgumentException> {
            updater.processUpdate("InvalidType", testShipment, 9000L)
        }
    }

    @Test
    fun testAllUpdateTypes() {
        val updateTypes = listOf(
            "Create", "Shipped", "Location", "Delivered", 
            "Delayed", "Lost", "Cancelled", "NoteAdded"
        )
        
        updateTypes.forEach { updateType ->
            val result = updater.processUpdate(updateType, testShipment, 1000L)
            assertNotNull(result)
            assertEquals(1000L, result.timestamp)
        }
    }
} 