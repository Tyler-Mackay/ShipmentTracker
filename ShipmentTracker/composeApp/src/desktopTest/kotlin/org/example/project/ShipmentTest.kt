package org.example.project

import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

class ShipmentTest {

    private lateinit var shipment: Shipment
    private lateinit var mockObserver: MockShipmentObserver

    @BeforeTest
    fun setup() {
        shipment = Shipment(
            status = "Created",
            id = "SHIP001",
            expectedDeliveryDate = 1234567890L,
            currentLocation = "Warehouse A"
        )
        mockObserver = MockShipmentObserver()
    }

    @Test
    fun testShipmentInitialization() {
        assertEquals("Created", shipment.status)
        assertEquals("SHIP001", shipment.id)
        assertEquals(1234567890L, shipment.expectedDeliveryDate)
        assertEquals("Warehouse A", shipment.currentLocation)
        assertTrue(shipment.notes.isEmpty())
        assertTrue(shipment.updateHistory.isEmpty())
    }

    @Test
    fun testAddNote() {
        val note = "Package delayed due to weather"
        shipment.addNote(note)
        
        assertEquals(1, shipment.notes.size)
        assertEquals(note, shipment.notes[0])
    }

    @Test
    fun testAddMultipleNotes() {
        shipment.addNote("First note")
        shipment.addNote("Second note")
        shipment.addNote("Third note")
        
        assertEquals(3, shipment.notes.size)
        assertEquals("First note", shipment.notes[0])
        assertEquals("Second note", shipment.notes[1])
        assertEquals("Third note", shipment.notes[2])
    }

    @Test
    fun testAddUpdate() {
        val update = ShippingUpdate(
            previousStatus = "Created",
            newStatus = "Shipped",
            timestamp = 1234567891L
        )
        
        shipment.addUpdate(update)
        
        assertEquals("Shipped", shipment.status)
        assertEquals(1, shipment.updateHistory.size)
        assertEquals(update, shipment.updateHistory[0])
    }

    @Test
    fun testAddMultipleUpdates() {
        val update1 = ShippingUpdate("Created", "Shipped", 1234567891L)
        val update2 = ShippingUpdate("Shipped", "In Transit", 1234567892L)
        val update3 = ShippingUpdate("In Transit", "Delivered", 1234567893L)
        
        shipment.addUpdate(update1)
        shipment.addUpdate(update2)
        shipment.addUpdate(update3)
        
        assertEquals("Delivered", shipment.status)
        assertEquals(3, shipment.updateHistory.size)
        assertEquals(update1, shipment.updateHistory[0])
        assertEquals(update2, shipment.updateHistory[1])
        assertEquals(update3, shipment.updateHistory[2])
    }

    @Test
    fun testUpdateLocation() {
        val newLocation = "Distribution Center B"
        shipment.updateLocation(newLocation)
        
        assertEquals(newLocation, shipment.currentLocation)
    }

    @Test
    fun testAddObserver() {
        shipment.addObserver(mockObserver)
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.addUpdate(update)
        
        assertEquals(1, mockObserver.updateCount)
        assertEquals("SHIP001", mockObserver.lastShipmentId)
        assertEquals(update, mockObserver.lastUpdate)
    }

    @Test
    fun testAddSameObserverTwice() {
        shipment.addObserver(mockObserver)
        shipment.addObserver(mockObserver) // Should not add duplicate
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.addUpdate(update)
        
        assertEquals(1, mockObserver.updateCount) // Should only be called once
    }

    @Test
    fun testRemoveObserver() {
        shipment.addObserver(mockObserver)
        shipment.removeObserver(mockObserver)
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.addUpdate(update)
        
        assertEquals(0, mockObserver.updateCount)
    }

    @Test
    fun testMultipleObservers() {
        val observer1 = MockShipmentObserver()
        val observer2 = MockShipmentObserver()
        
        shipment.addObserver(observer1)
        shipment.addObserver(observer2)
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.addUpdate(update)
        
        assertEquals(1, observer1.updateCount)
        assertEquals(1, observer2.updateCount)
        assertEquals("SHIP001", observer1.lastShipmentId)
        assertEquals("SHIP001", observer2.lastShipmentId)
    }

    @Test
    fun testNotifyObserversWithUpdate() {
        shipment.addObserver(mockObserver)
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.addUpdate(update)
        
        assertEquals("SHIP001", mockObserver.lastShipmentId)
        assertEquals(update, mockObserver.lastUpdate)
        assertEquals("Shipped", mockObserver.lastUpdate?.newStatus)
        assertEquals("Created", mockObserver.lastUpdate?.previousStatus)
        assertEquals(1234567891L, mockObserver.lastUpdate?.timestamp)
    }

    @Test
    fun testShipmentDataClass() {
        val shipment1 = Shipment("Created", "SHIP001", mutableListOf(), mutableListOf(), 123L, "Location A")
        val shipment2 = Shipment("Created", "SHIP001", mutableListOf(), mutableListOf(), 123L, "Location A")
        
        assertEquals(shipment1, shipment2)
        assertEquals(shipment1.hashCode(), shipment2.hashCode())
    }

    @Test
    fun testShipmentWithEmptyValues() {
        val emptyShipment = Shipment(
            status = "",
            id = "",
            expectedDeliveryDate = 0L,
            currentLocation = ""
        )
        
        assertEquals("", emptyShipment.status)
        assertEquals("", emptyShipment.id)
        assertEquals(0L, emptyShipment.expectedDeliveryDate)
        assertEquals("", emptyShipment.currentLocation)
    }
}

class MockShipmentObserver : ShipmentObserver {
    var updateCount = 0
    var lastShipmentId: String? = null
    var lastUpdate: ShippingUpdate? = null
    
    override fun onShipmentUpdate(shipmentId: String, update: ShippingUpdate) {
        updateCount++
        lastShipmentId = shipmentId
        lastUpdate = update
    }
} 