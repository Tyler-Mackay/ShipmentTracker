package org.example.project

import org.example.project.Tracker.*
import org.example.project.Shipment.*
import kotlin.test.*

class TrackingServerTest {

    private lateinit var server: TrackingServer

    @BeforeTest
    fun setup() {
        server = TrackingServer.getInstance()
    }

    @Test
    fun testSingletonPattern() {
        val server1 = TrackingServer.getInstance()
        val server2 = TrackingServer.getInstance()

        assertSame(server1, server2)
    }

        @Test
    fun testCreateShipmentSuccess() {
        val request = CreateShipmentRequest("created,TEST001,standard,1000")
        val response = server.createShipment(request)
        
        assertTrue(response.success)
        assertNotNull(response.shipmentData)
        assertEquals("TEST001", response.shipmentData?.id)
        assertTrue(response.message.contains("successfully"))
    }

    @Test
    fun testCreateShipmentWithDifferentTypes() {
        val types = listOf("standard", "express", "overnight", "bulk")

        types.forEach { type ->
            val request = CreateShipmentRequest("created,${type.uppercase()}001,$type,1000")
            val response = server.createShipment(request)

            assertTrue(response.success, "Failed for type: $type")
            assertNotNull(response.shipmentData)
        }
    }

    @Test
    fun testCreateShipmentInvalidData() {
        val request = CreateShipmentRequest("invalid,data")
        val response = server.createShipment(request)

        assertFalse(response.success)
        assertNull(response.shipmentData)
        assertTrue(response.message.contains("Failed"))
    }

    @Test
    fun testUpdateShipmentSuccess() {
        // First create a shipment
        val createRequest = CreateShipmentRequest("created,UPDATE001,standard,1000")
        val createResponse = server.createShipment(createRequest)
        assertTrue(createResponse.success)

        // Then update it
        val updateRequest = UpdateShipmentRequest("shipped,UPDATE001,2000")
        val updateResponse = server.updateShipment(updateRequest)

        assertTrue(updateResponse.success)
        assertNotNull(updateResponse.shipmentData)
        assertEquals("Shipped", updateResponse.shipmentData?.status)
        assertTrue(updateResponse.message.contains("successfully"))
    }

    @Test
    fun testUpdateNonExistentShipment() {
        val request = UpdateShipmentRequest("shipped,NONEXISTENT,1000")
        val response = server.updateShipment(request)

        assertFalse(response.success)
        assertNull(response.shipmentData)
        assertTrue(response.message.contains("not found"))
    }

    @Test
    fun testGetShipment() {
        // Create a shipment first
        val createRequest = CreateShipmentRequest("created,GET001,standard,1000")
        server.createShipment(createRequest)

                // Get the shipment
        val shipment = server.getShipment("GET001")
        
        assertNotNull(shipment)
        assertEquals("GET001", shipment.id)
    }

    @Test
    fun testGetNonExistentShipment() {
        val shipment = server.getShipment("NONEXISTENT")
        assertNull(shipment)
    }

    @Test
    fun testUpdateShipmentLocation() {
        // Create and then update location
        val createRequest = CreateShipmentRequest("created,LOC001,standard,1000")
        server.createShipment(createRequest)

        val updateRequest = UpdateShipmentRequest("location,LOC001,2000,Distribution Center")
        val response = server.updateShipment(updateRequest)

        assertTrue(response.success)
        assertEquals("Distribution Center", response.shipmentData?.currentLocation)
    }

    @Test
    fun testUpdateShipmentWithNote() {
        // Create and then add note
        val createRequest = CreateShipmentRequest("created,NOTE001,standard,1000")
        server.createShipment(createRequest)

        val updateRequest = UpdateShipmentRequest("noteadded,NOTE001,2000,,Handle with care")
        val response = server.updateShipment(updateRequest)

        assertTrue(response.success)
        assertTrue(response.shipmentData?.notes?.any { it.contains("Handle with care") } == true)
    }

    @Test
    fun testMultipleOperations() {
        val shipmentId = "MULTI001"

        // Create
        val createRequest = CreateShipmentRequest("created,$shipmentId,standard,1000")
        val createResponse = server.createShipment(createRequest)
        assertTrue(createResponse.success)

        // Update to shipped
        val shippedRequest = UpdateShipmentRequest("shipped,$shipmentId,2000")
        val shippedResponse = server.updateShipment(shippedRequest)
        assertTrue(shippedResponse.success)

        // Update location
        val locationRequest = UpdateShipmentRequest("location,$shipmentId,3000,In Transit")
        val locationResponse = server.updateShipment(locationRequest)
        assertTrue(locationResponse.success)

        // Final verification
        val finalShipment = server.getShipment(shipmentId)
        assertNotNull(finalShipment)
        assertEquals("In Transit", finalShipment.status)
        assertEquals("In Transit", finalShipment.currentLocation)
    }

        @Test
    fun testServerFileWatcher() {
        // Test that file watcher can be started without crashing
        server.startFileWatcher()
        // Test passes if no exception is thrown
        server.stopServer()
    }
}