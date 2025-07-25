package org.example.project

import org.example.project.Shipment.*
import kotlin.test.*

class ShipmentFactoryTest {

    @Test
    fun testCreateStandardShipment() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "STD001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 2000L,
            currentLocation = "Warehouse"
        )
        
        assertTrue(shipment is StandardShipment)
        assertEquals(ShipmentType.STANDARD, shipment.shipmentType)
        assertEquals("Created", shipment.status)
        assertEquals("STD001", shipment.id)
        assertEquals(1000L, shipment.creationDate)
        assertEquals(2000L, shipment.expectedDeliveryDateTimestamp)
        assertEquals("Warehouse", shipment.currentLocation)
    }

    @Test
    fun testCreateExpressShipment() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.EXPRESS,
            status = "Shipped",
            id = "EXP001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 2000L,
            currentLocation = "Transit"
        )
        
        assertTrue(shipment is ExpressShipment)
        assertEquals(ShipmentType.EXPRESS, shipment.shipmentType)
        assertEquals("Shipped", shipment.status)
        assertEquals("EXP001", shipment.id)
    }

    @Test
    fun testCreateOvernightShipment() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.OVERNIGHT,
            status = "In Transit",
            id = "ON001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 2000L
        )
        
        assertTrue(shipment is OvernightShipment)
        assertEquals(ShipmentType.OVERNIGHT, shipment.shipmentType)
        assertEquals("", shipment.currentLocation) // Default empty location
    }

    @Test
    fun testCreateBulkShipment() {
        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.BULK,
            status = "Created",
            id = "BULK001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 10000L,
            currentLocation = "Dock"
        )
        
        assertTrue(shipment is BulkShipment)
        assertEquals(ShipmentType.BULK, shipment.shipmentType)
        assertEquals("Dock", shipment.currentLocation)
    }

    @Test
    fun testParseShipmentType() {
        assertEquals(ShipmentType.STANDARD, ShipmentFactory.parseShipmentType("standard"))
        assertEquals(ShipmentType.EXPRESS, ShipmentFactory.parseShipmentType("express"))
        assertEquals(ShipmentType.OVERNIGHT, ShipmentFactory.parseShipmentType("overnight"))
        assertEquals(ShipmentType.BULK, ShipmentFactory.parseShipmentType("bulk"))
        
        // Test case insensitive
        assertEquals(ShipmentType.STANDARD, ShipmentFactory.parseShipmentType("STANDARD"))
        assertEquals(ShipmentType.EXPRESS, ShipmentFactory.parseShipmentType("Express"))
    }

    @Test
    fun testParseShipmentTypeInvalid() {
        assertFailsWith<IllegalArgumentException> {
            ShipmentFactory.parseShipmentType("invalid")
        }
        
        assertFailsWith<IllegalArgumentException> {
            ShipmentFactory.parseShipmentType("")
        }
    }
} 