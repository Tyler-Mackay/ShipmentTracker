package org.example.project

import org.example.project.Shipment.*
import org.example.project.ShippingUpdate.ShippingUpdate
import kotlin.test.*

class ShipmentTest {

    @Test
    fun testStandardShipmentValidation() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "STD001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L,
            currentLocation = "Warehouse"
        )
        
        // Standard shipments should always be valid
        val result = shipment.validateDeliveryDate(1000L, 5000L, "shipped")
        assertTrue(result.isValid)
        assertEquals("Standard shipment delivery timing is acceptable", result.message)
    }

    @Test
    fun testExpressShipmentValidation() {
        val creationTime = 1000L
        val threeDaysLater = creationTime + (3 * 24 * 60 * 60 * 1000L)
        val fourDaysLater = creationTime + (4 * 24 * 60 * 60 * 1000L)
        
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.EXPRESS,
            status = "Created",
            id = "EXP001",
            creationDate = creationTime,
            expectedDeliveryDateTimestamp = threeDaysLater,
            currentLocation = "Warehouse"
        )
        
        // Valid: within 3 days
        val validResult = shipment.validateDeliveryDate(creationTime, threeDaysLater, "shipped")
        assertTrue(validResult.isValid)
        
        // Invalid: over 3 days
        val invalidResult = shipment.validateDeliveryDate(creationTime, fourDaysLater, "shipped")
        assertFalse(invalidResult.isValid)
    }

    @Test
    fun testOvernightShipmentValidation() {
        val creationTime = 1000L
        val nextDay = creationTime + (24 * 60 * 60 * 1000L)
        val twoDays = creationTime + (2 * 24 * 60 * 60 * 1000L)
        
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.OVERNIGHT,
            status = "Created",
            id = "ON001",
            creationDate = creationTime,
            expectedDeliveryDateTimestamp = nextDay
        )
        
        // Valid: next day
        val validResult = shipment.validateDeliveryDate(creationTime, nextDay, "shipped")
        assertTrue(validResult.isValid)
        
        // Invalid: over 1 day
        val invalidResult = shipment.validateDeliveryDate(creationTime, twoDays, "shipped")
        assertFalse(invalidResult.isValid)
    }

    @Test
    fun testBulkShipmentValidation() {
        val creationTime = 1000L
        val twoDays = creationTime + (2 * 24 * 60 * 60 * 1000L)
        val fourDays = creationTime + (4 * 24 * 60 * 60 * 1000L)
        
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.BULK,
            status = "Created",
            id = "BULK001",
            creationDate = creationTime,
            expectedDeliveryDateTimestamp = fourDays
        )
        
        // Invalid: too soon (less than 3 days)
        val invalidResult = shipment.validateDeliveryDate(creationTime, twoDays, "shipped")
        assertFalse(invalidResult.isValid)
        
        // Valid: 3+ days
        val validResult = shipment.validateDeliveryDate(creationTime, fourDays, "shipped")
        assertTrue(validResult.isValid)
    }

    @Test
    fun testShipmentBasicProperties() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "TEST001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L,
            currentLocation = "Warehouse"
        )
        
        assertEquals("Created", shipment.status)
        assertEquals("TEST001", shipment.id)
        assertEquals(ShipmentType.STANDARD, shipment.shipmentType)
        assertEquals(1000L, shipment.creationDate)
        assertEquals(5000L, shipment.expectedDeliveryDateTimestamp)
        assertEquals("Warehouse", shipment.currentLocation)
        assertFalse(shipment.isAbnormal)
        assertEquals("", shipment.abnormality)
        assertTrue(shipment.notes.isEmpty())
        assertTrue(shipment.updateHistory.isEmpty())
    }

    @Test
    fun testAddNote() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "TEST001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L
        )
        
        shipment.addNote("First note")
        shipment.addNote("Second note")
        
        assertEquals(2, shipment.notes.size)
        assertEquals("First note", shipment.notes[0])
        assertEquals("Second note", shipment.notes[1])
    }

    @Test
    fun testUpdateLocation() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "TEST001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L,
            currentLocation = "Initial"
        )
        
        assertEquals("Initial", shipment.currentLocation)
        
        shipment.updateLocation("New Location")
        assertEquals("New Location", shipment.currentLocation)
    }

    @Test
    fun testMarkAbnormal() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "TEST001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L
        )
        
        assertFalse(shipment.isAbnormal)
        assertEquals("", shipment.abnormality)
        
        shipment.markAbnormal("Package damaged")
        assertTrue(shipment.isAbnormal)
        assertEquals("Package damaged", shipment.abnormality)
    }

    @Test
    fun testAddUpdate() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "TEST001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L
        )
        
        val update = ShippingUpdate("Created", "Shipped", 2000L)
        shipment.addUpdate(update)
        
        assertEquals("Shipped", shipment.status)
        assertEquals(1, shipment.updateHistory.size)
        assertEquals(update, shipment.updateHistory[0])
    }
} 