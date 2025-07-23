package org.example.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.BeforeTest

class TrackerViewHelperTest {

    private lateinit var trackerViewHelper: TrackerViewHelper
    private lateinit var simulator: TrackingSimulator

    @BeforeTest
    fun setup() {
        trackerViewHelper = TrackerViewHelper()
        simulator = TrackingSimulator()
        simulator.setParser(ShipmentDataParser())
        simulator.setUpdater(ShipmentUpdater())
        
        trackerViewHelper.setSimulator(simulator)
        
        // Add test shipments
        val shipment1 = Shipment("Created", "SHIP001", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse A")
        val shipment2 = Shipment("Created", "SHIP002", expectedDeliveryDate = 1234567891L, currentLocation = "Transit")
        
        simulator.addShipment(shipment1)
        simulator.addShipment(shipment2)
        
        // Manually update shipment2 to "Shipped" status after adding to simulator
        simulator.processFileUpdate("Shipped,SHIP002,1234567892")
    }

    @Test
    fun testTrackShipmentSuccess() {
        val result = trackerViewHelper.trackShipment("SHIP001")
        
        assertTrue(result)
        assertTrue(trackerViewHelper.isTrackingShipment("SHIP001"))
        assertEquals(1, trackerViewHelper.activeTrackingShipments.size)
        assertEquals("SHIP001", trackerViewHelper.activeTrackingShipments[0])
    }

    @Test
    fun testTrackShipmentNonExistent() {
        val result = trackerViewHelper.trackShipment("NONEXISTENT")
        
        assertFalse(result)
        assertFalse(trackerViewHelper.isTrackingShipment("NONEXISTENT"))
        assertEquals(0, trackerViewHelper.activeTrackingShipments.size)
    }

    @Test
    fun testTrackShipmentAlreadyTracked() {
        // Track first time
        trackerViewHelper.trackShipment("SHIP001")
        
        // Track again - should return false since already tracked
        val result = trackerViewHelper.trackShipment("SHIP001")
        
        assertFalse(result)
        assertEquals(1, trackerViewHelper.activeTrackingShipments.size) // Still only one
    }

    @Test
    fun testStopTrackingShipment() {
        trackerViewHelper.trackShipment("SHIP001")
        assertTrue(trackerViewHelper.isTrackingShipment("SHIP001"))
        
        trackerViewHelper.stopTrackingShipment("SHIP001")
        
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP001"))
        assertEquals(0, trackerViewHelper.activeTrackingShipments.size)
    }

    @Test
    fun testStopTrackingAll() {
        trackerViewHelper.trackShipment("SHIP001")
        trackerViewHelper.trackShipment("SHIP002")
        assertEquals(2, trackerViewHelper.activeTrackingShipments.size)
        
        trackerViewHelper.stopTracking()
        
        assertEquals(0, trackerViewHelper.activeTrackingShipments.size)
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP001"))
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP002"))
    }

    @Test
    fun testGetTrackedShipments() {
        trackerViewHelper.trackShipment("SHIP001")
        trackerViewHelper.trackShipment("SHIP002")
        
        val trackedShipments = trackerViewHelper.getTrackedShipments()
        
        assertEquals(2, trackedShipments.size)
        assertTrue(trackedShipments.containsKey("SHIP001"))
        assertTrue(trackedShipments.containsKey("SHIP002"))
        assertEquals("Created", trackedShipments["SHIP001"]?.status)
        assertEquals("Shipped", trackedShipments["SHIP002"]?.status)
    }

    @Test
    fun testStatePropertiesInitialization() {
        assertEquals("", trackerViewHelper.shipmentId)
        assertEquals(0, trackerViewHelper.shipmentTotes.size)
        assertEquals(0, trackerViewHelper.shipmentUpdateHistory.size)
        assertEquals(0, trackerViewHelper.expectedShipmentDeliveryDate.size)
        assertEquals("", trackerViewHelper.shipmentStatus)
    }

    @Test
    fun testStatePropertiesAfterTracking() {
        trackerViewHelper.trackShipment("SHIP001")
        
        assertEquals("SHIP001", trackerViewHelper.shipmentId)
        assertTrue(trackerViewHelper.shipmentTotes.isNotEmpty())
        assertTrue(trackerViewHelper.expectedShipmentDeliveryDate.isNotEmpty())
        assertEquals("Created", trackerViewHelper.shipmentStatus)
    }

    @Test
    fun testShipmentTonesContent() {
        trackerViewHelper.trackShipment("SHIP001")
        
        val totes = trackerViewHelper.shipmentTotes
        assertTrue(totes.isNotEmpty())
        assertTrue(totes[0].contains("SHIP001"))
        assertTrue(totes[0].contains("Created"))
        assertTrue(totes[0].contains("Warehouse A"))
    }

    @Test
    fun testExpectedDeliveryDateFormat() {
        trackerViewHelper.trackShipment("SHIP001")
        
        val deliveryDates = trackerViewHelper.expectedShipmentDeliveryDate
        assertTrue(deliveryDates.isNotEmpty())
        assertTrue(deliveryDates[0].contains("SHIP001"))
        // Should contain formatted date
        assertTrue(deliveryDates[0].length > "SHIP001: ".length)
    }

    @Test
    fun testOnShipmentUpdate() {
        trackerViewHelper.trackShipment("SHIP001")
        
        val update = ShippingUpdate("Created", "Shipped", 1234567891L)
        trackerViewHelper.onShipmentUpdate("SHIP001", update)
        
        // Verify state was updated
        val trackedShipments = trackerViewHelper.getTrackedShipments()
        // Note: The actual status update happens through the simulator/shipment relationship
        assertTrue(trackedShipments.containsKey("SHIP001"))
    }

    @Test
    fun testUpdateHistoryFormatting() {
        trackerViewHelper.trackShipment("SHIP001")
        
        // Process an update through simulator to trigger history
        simulator.processFileUpdate("Shipped,SHIP001,1234567891")
        
        val updateHistory = trackerViewHelper.shipmentUpdateHistory
        assertTrue(updateHistory.isNotEmpty())
        
        // Should contain shipment ID and status information
        val hasShipmentUpdate = updateHistory.any { it.contains("SHIP001") }
        assertTrue(hasShipmentUpdate)
    }

    @Test
    fun testMultipleShipmentTracking() {
        trackerViewHelper.trackShipment("SHIP001")
        trackerViewHelper.trackShipment("SHIP002")
        
        assertEquals(2, trackerViewHelper.activeTrackingShipments.size)
        assertEquals("SHIP002", trackerViewHelper.shipmentId) // Should be most recent
        
        val totes = trackerViewHelper.shipmentTotes
        assertEquals(2, totes.size)
        
        val deliveryDates = trackerViewHelper.expectedShipmentDeliveryDate
        assertEquals(2, deliveryDates.size)
    }

    @Test
    fun testShipmentExists() {
        assertTrue(trackerViewHelper.shipmentExists("SHIP001"))
        assertTrue(trackerViewHelper.shipmentExists("SHIP002"))
        assertFalse(trackerViewHelper.shipmentExists("NONEXISTENT"))
    }

    @Test
    fun testNotifyIfShipmentDoesntExist() {
        // Should not crash when called
        trackerViewHelper.notifyIfShipmentDoesntExist()
        // This is primarily for UI notification, so we just verify it doesn't crash
    }

    @Test
    fun testToggleTrackingStart() {
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP001"))
        
        trackerViewHelper.toggleTracking("SHIP001")
        
        assertTrue(trackerViewHelper.isTrackingShipment("SHIP001"))
    }

    @Test
    fun testToggleTrackingStop() {
        trackerViewHelper.trackShipment("SHIP001")
        assertTrue(trackerViewHelper.isTrackingShipment("SHIP001"))
        
        trackerViewHelper.toggleTracking("SHIP001")
        
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP001"))
    }

    @Test
    fun testToggleTrackingWithEmptyId() {
        // Should handle empty ID gracefully
        trackerViewHelper.toggleTracking("")
        trackerViewHelper.toggleTracking("   ")
        
        // Should not have tracked anything
        assertEquals(0, trackerViewHelper.activeTrackingShipments.size)
    }

    @Test
    fun testErrorMessageState() {
        assertNull(trackerViewHelper.errorMessage)
        
        // Toggle with invalid ID should set error
        trackerViewHelper.toggleTracking("")
        
        assertEquals("Please enter a shipment ID", trackerViewHelper.errorMessage)
    }

    @Test
    fun testClearError() {
        // Set an error first
        trackerViewHelper.toggleTracking("")
        assertEquals("Please enter a shipment ID", trackerViewHelper.errorMessage)
        
        trackerViewHelper.clearError()
        
        // Note: clearError uses coroutine, so we test the method call doesn't crash
        // The actual clearing might be asynchronous
    }

    @Test
    fun testLoadingState() {
        assertFalse(trackerViewHelper.isLoading)
        // Loading state is managed internally during async operations
        // We verify the initial state and that it's accessible
    }

    @Test
    fun testFormatDate() {
        val timestamp = 1234567890L
        val formatted = trackerViewHelper.formatDate(timestamp)
        
        assertTrue(formatted.isNotEmpty())
        // 1234567890L is Feb 13, 2009 23:31:30 UTC (or local equivalent)
        assertTrue(formatted.contains("2009") || formatted.contains("Feb") || formatted.contains("13"))
    }

    @Test
    fun testStatePropertyUpdatesOnTracking() {
        // Initially empty
        assertEquals("", trackerViewHelper.shipmentId)
        assertEquals("", trackerViewHelper.shipmentStatus)
        
        trackerViewHelper.trackShipment("SHIP001")
        
        // Should be updated
        assertEquals("SHIP001", trackerViewHelper.shipmentId)
        assertEquals("Created", trackerViewHelper.shipmentStatus)
        
        trackerViewHelper.trackShipment("SHIP002")
        
        // Should reflect most recent
        assertEquals("SHIP002", trackerViewHelper.shipmentId)
        assertEquals("Shipped", trackerViewHelper.shipmentStatus)
    }

    @Test
    fun testStatePropertyClearingOnStopAll() {
        trackerViewHelper.trackShipment("SHIP001")
        trackerViewHelper.trackShipment("SHIP002")
        
        // Verify state is populated
        assertTrue(trackerViewHelper.shipmentTotes.isNotEmpty())
        assertTrue(trackerViewHelper.expectedShipmentDeliveryDate.isNotEmpty())
        
        trackerViewHelper.stopTracking()
        
        // Should be cleared
        assertEquals("", trackerViewHelper.shipmentId)
        assertEquals("", trackerViewHelper.shipmentStatus)
        assertEquals(0, trackerViewHelper.shipmentTotes.size)
        assertEquals(0, trackerViewHelper.expectedShipmentDeliveryDate.size)
        assertEquals(0, trackerViewHelper.shipmentUpdateHistory.size)
    }

    @Test
    fun testCleanup() {
        trackerViewHelper.trackShipment("SHIP001")
        trackerViewHelper.trackShipment("SHIP002")
        
        trackerViewHelper.cleanup()
        
        // Should stop tracking all shipments
        assertEquals(0, trackerViewHelper.activeTrackingShipments.size)
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP001"))
        assertFalse(trackerViewHelper.isTrackingShipment("SHIP002"))
    }
} 