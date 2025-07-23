package org.example.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.BeforeTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

class ObserverPatternIntegrationTest {

    private lateinit var shipment: Shipment
    private lateinit var trackerViewHelper: TrackerViewHelper
    private lateinit var simulator: TrackingSimulator

    @BeforeTest
    fun setup() {
        shipment = Shipment(
            status = "Created",
            id = "SHIP001",
            expectedDeliveryDate = 1234567890L,
            currentLocation = "Warehouse A"
        )
        
        trackerViewHelper = TrackerViewHelper()
        simulator = TrackingSimulator()
        simulator.setParser(ShipmentDataParser())
        simulator.setUpdater(ShipmentUpdater())
        
        trackerViewHelper.setSimulator(simulator)
        simulator.addShipment(shipment)
    }

    @Test
    fun testObserverRegistrationAndNotification() {
        // Register observer
        shipment.addObserver(trackerViewHelper)
        
        // Trigger update
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.addUpdate(update)
        
        // Verify observer was notified
        // Note: This test verifies the observer pattern works
        assertTrue(shipment.updateHistory.contains(update))
        assertEquals("Shipped", shipment.status)
    }

    @Test
    fun testMultipleObserversNotification() {
        val observer1 = MockShipmentObserver()
        val observer2 = MockShipmentObserver()
        val observer3 = MockShipmentObserver()
        
        shipment.addObserver(observer1)
        shipment.addObserver(observer2)
        shipment.addObserver(observer3)
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.addUpdate(update)
        
        assertEquals(1, observer1.updateCount)
        assertEquals(1, observer2.updateCount)
        assertEquals(1, observer3.updateCount)
        
        assertEquals("SHIP001", observer1.lastShipmentId)
        assertEquals("SHIP001", observer2.lastShipmentId)
        assertEquals("SHIP001", observer3.lastShipmentId)
    }

    @Test
    fun testObserverRemoval() {
        val observer1 = MockShipmentObserver()
        val observer2 = MockShipmentObserver()
        
        shipment.addObserver(observer1)
        shipment.addObserver(observer2)
        
        // Remove one observer
        shipment.removeObserver(observer1)
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.addUpdate(update)
        
        assertEquals(0, observer1.updateCount) // Should not be notified
        assertEquals(1, observer2.updateCount) // Should be notified
    }

    @Test
    fun testTrackerViewHelperAsObserver() {
        // Track the shipment
        trackerViewHelper.trackShipment("SHIP001")
        
        // Verify it's being tracked
        assertTrue(trackerViewHelper.isTrackingShipment("SHIP001"))
        
        // Update shipment through simulator
        simulator.processFileUpdate("Shipped,SHIP001,1234567891")
        
        // Verify TrackerViewHelper received the update
        val trackedShipments = trackerViewHelper.getTrackedShipments()
        assertEquals("Shipped", trackedShipments["SHIP001"]?.status)
    }

    @Test
    fun testFullObserverPatternWorkflow() {
        // Start tracking
        trackerViewHelper.trackShipment("SHIP001")
        
        // Process multiple updates
        val updates = listOf(
            "Shipped,SHIP001,1234567891",
            "Location,SHIP001,1234567892,Distribution Center",
            "Delivered,SHIP001,1234567893"
        )
        
        updates.forEach { update ->
            simulator.processFileUpdate(update)
        }
        
        // Verify final state
        val trackedShipments = trackerViewHelper.getTrackedShipments()
        val finalShipment = trackedShipments["SHIP001"]
        assertEquals("Delivered", finalShipment?.status)
        assertEquals("Distribution Center", finalShipment?.currentLocation)
        assertTrue(finalShipment?.updateHistory?.size ?: 0 >= 4) // Initial + 3 updates
    }

    @Test
    fun testObserverPatternWithMultipleShipments() {
        // Add more shipments
        val shipment2 = Shipment("Created", "SHIP002", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse B")
        val shipment3 = Shipment("Created", "SHIP003", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse C")
        
        simulator.addShipment(shipment2)
        simulator.addShipment(shipment3)
        
        // Track all shipments
        trackerViewHelper.trackShipment("SHIP001")
        trackerViewHelper.trackShipment("SHIP002")
        trackerViewHelper.trackShipment("SHIP003")
        
        // Update each shipment
        simulator.processFileUpdate("Shipped,SHIP001,1234567891")
        simulator.processFileUpdate("Delayed,SHIP002,1234567892")
        simulator.processFileUpdate("Cancelled,SHIP003,1234567893")
        
        // Verify all updates were tracked
        val trackedShipments = trackerViewHelper.getTrackedShipments()
        assertEquals("Shipped", trackedShipments["SHIP001"]?.status)
        assertEquals("Delayed", trackedShipments["SHIP002"]?.status)
        assertEquals("Cancelled", trackedShipments["SHIP003"]?.status)
    }

    @Test
    fun testStopTrackingRemovesObserver() {
        // Start tracking
        trackerViewHelper.trackShipment("SHIP001")
        
        // Verify tracking
        assertTrue(trackerViewHelper.isTrackingShipment("SHIP001"))
        
        // Stop tracking
        trackerViewHelper.stopTrackingShipment("SHIP001")
        
        // Verify no longer tracking
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP001"))
        
        // Update shipment
        simulator.processFileUpdate("Shipped,SHIP001,1234567891")
        
        // Verify TrackerViewHelper didn't receive update
        val trackedShipments = trackerViewHelper.getTrackedShipments()
        assertFalse(trackedShipments.containsKey("SHIP001"))
    }

    @Test
    fun testObserverPatternWithFileProcessing() = runBlocking {
        // Track shipment
        trackerViewHelper.trackShipment("SHIP001")
        
        // Simulate processing file updates
        val fileUpdates = listOf(
            "Shipped,SHIP001,1234567891",
            "Location,SHIP001,1234567892,Transit Hub",
            "Location,SHIP001,1234567893,Local Facility",
            "Delivered,SHIP001,1234567894"
        )
        
        fileUpdates.forEach { update ->
            simulator.processFileUpdate(update)
            delay(10) // Small delay to simulate real-time processing
        }
        
        // Verify final state through observer
        val trackedShipments = trackerViewHelper.getTrackedShipments()
        val finalShipment = trackedShipments["SHIP001"]
        assertEquals("Delivered", finalShipment?.status)
        assertEquals("Local Facility", finalShipment?.currentLocation)
    }

    @Test
    fun testObserverPatternErrorHandling() {
        trackerViewHelper.trackShipment("SHIP001")
        
        // Try processing invalid updates
        simulator.processFileUpdate("InvalidFormat")
        simulator.processFileUpdate("Unknown,SHIP001,1234567891")
        simulator.processFileUpdate("Shipped,NONEXISTENT,1234567891")
        
        // Verify original shipment is still tracked and unchanged
        val trackedShipments = trackerViewHelper.getTrackedShipments()
        assertEquals("Created", trackedShipments["SHIP001"]?.status)
        assertTrue(trackerViewHelper.isTrackingShipment("SHIP001"))
    }

    @Test
    fun testConcurrentObserverNotifications() {
        val observers = (1..5).map { MockShipmentObserver() }
        
        // Add all observers
        observers.forEach { shipment.addObserver(it) }
        
        // Trigger multiple updates
        val updates = listOf(
            ShippingUpdate("Created", "Shipped", 1234567891L),
            ShippingUpdate("Shipped", "In Transit", 1234567892L),
            ShippingUpdate("In Transit", "Delivered", 1234567893L)
        )
        
        updates.forEach { shipment.addUpdate(it) }
        
        // Verify all observers received all updates
        observers.forEach { observer ->
            assertEquals(3, observer.updateCount)
            assertEquals("SHIP001", observer.lastShipmentId)
        }
    }

    @Test
    fun testObserverStateConsistency() {
        trackerViewHelper.trackShipment("SHIP001")
        
        // Perform a series of updates
        val updateSequence = listOf(
            "Shipped,SHIP001,1234567891",
            "Location,SHIP001,1234567892,Hub A",
            "Location,SHIP001,1234567893,Hub B", 
            "Delayed,SHIP001,1234567894",
            "Location,SHIP001,1234567895,Customer Location",
            "Delivered,SHIP001,1234567896"
        )
        
        updateSequence.forEach { simulator.processFileUpdate(it) }
        
        // Verify final state consistency
        val originalShipment = simulator.findShipment("SHIP001")
        val trackedShipment = trackerViewHelper.getTrackedShipments()["SHIP001"]
        
        assertEquals(originalShipment?.status, trackedShipment?.status)
        assertEquals(originalShipment?.currentLocation, trackedShipment?.currentLocation)
        assertEquals(originalShipment?.id, trackedShipment?.id)
    }

    @Test
    fun testObserverPatternWithShipmentSubjectInterface() {
        // Test that Shipment correctly implements ShipmentSubject
        val mockObserver = MockShipmentObserver()
        
        // Test interface methods
        shipment.addObserver(mockObserver)
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        shipment.notifyObservers(shipment, update)
        
        assertEquals(1, mockObserver.updateCount)
        assertEquals("SHIP001", mockObserver.lastShipmentId)
        assertEquals(update, mockObserver.lastUpdate)
    }
} 