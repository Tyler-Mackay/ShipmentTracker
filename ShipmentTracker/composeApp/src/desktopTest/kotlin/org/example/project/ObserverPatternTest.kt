package org.example.project

import org.example.project.Shipment.*
import org.example.project.ShippingUpdate.ShippingUpdate
import kotlin.test.*

class ObserverPatternTest {

    private lateinit var shipment: Shipment
    private lateinit var testObserver: TestObserver

    @BeforeTest
    fun setup() {
        shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "OBS001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L,
            currentLocation = "Warehouse"
        )
        testObserver = TestObserver()
    }

    @Test
    fun testAddObserver() {
        shipment.addObserver(testObserver)
        
        val update = ShippingUpdate("Created", "Shipped", 2000L)
        shipment.addUpdate(update)
        
        assertEquals(1, testObserver.updateCount)
        assertEquals("OBS001", testObserver.lastShipmentId)
        assertEquals(update, testObserver.lastUpdate)
    }

    @Test
    fun testRemoveObserver() {
        shipment.addObserver(testObserver)
        shipment.removeObserver(testObserver)
        
        val update = ShippingUpdate("Created", "Shipped", 2000L)
        shipment.addUpdate(update)
        
        assertEquals(0, testObserver.updateCount)
    }

    @Test
    fun testMultipleObservers() {
        val observer1 = TestObserver()
        val observer2 = TestObserver()
        
        shipment.addObserver(observer1)
        shipment.addObserver(observer2)
        
        val update = ShippingUpdate("Created", "Shipped", 2000L)
        shipment.addUpdate(update)
        
        assertEquals(1, observer1.updateCount)
        assertEquals(1, observer2.updateCount)
        assertEquals("OBS001", observer1.lastShipmentId)
        assertEquals("OBS001", observer2.lastShipmentId)
    }

    @Test
    fun testDuplicateObserver() {
        shipment.addObserver(testObserver)
        shipment.addObserver(testObserver) // Add same observer twice
        
        val update = ShippingUpdate("Created", "Shipped", 2000L)
        shipment.addUpdate(update)
        
        // Should only be notified once
        assertEquals(1, testObserver.updateCount)
    }

    @Test
    fun testObserverWithMultipleUpdates() {
        shipment.addObserver(testObserver)
        
        val update1 = ShippingUpdate("Created", "Shipped", 2000L)
        val update2 = ShippingUpdate("Shipped", "In Transit", 3000L)
        val update3 = ShippingUpdate("In Transit", "Delivered", 4000L)
        
        shipment.addUpdate(update1)
        shipment.addUpdate(update2)
        shipment.addUpdate(update3)
        
        assertEquals(3, testObserver.updateCount)
        assertEquals(update3, testObserver.lastUpdate) // Should be the last one
    }

    @Test
    fun testObserverAfterRemovalDoesNotGetNotified() {
        val observer1 = TestObserver()
        val observer2 = TestObserver()
        
        shipment.addObserver(observer1)
        shipment.addObserver(observer2)
        
        // First update - both should get notified
        val update1 = ShippingUpdate("Created", "Shipped", 2000L)
        shipment.addUpdate(update1)
        
        assertEquals(1, observer1.updateCount)
        assertEquals(1, observer2.updateCount)
        
        // Remove observer1
        shipment.removeObserver(observer1)
        
        // Second update - only observer2 should get notified
        val update2 = ShippingUpdate("Shipped", "Delivered", 3000L)
        shipment.addUpdate(update2)
        
        assertEquals(1, observer1.updateCount) // Still 1
        assertEquals(2, observer2.updateCount) // Now 2
    }

    private class TestObserver : ShipmentObserver {
        var updateCount = 0
        var lastShipmentId: String? = null
        var lastUpdate: ShippingUpdate? = null

        override fun onShipmentUpdate(shipmentId: String, update: ShippingUpdate) {
            updateCount++
            lastShipmentId = shipmentId
            lastUpdate = update
        }
    }
} 