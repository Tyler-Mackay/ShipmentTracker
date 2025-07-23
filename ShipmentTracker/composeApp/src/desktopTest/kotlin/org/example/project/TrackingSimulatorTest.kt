package org.example.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.BeforeTest
import java.io.File

class TrackingSimulatorTest {

    private lateinit var simulator: TrackingSimulator
    private lateinit var parser: ShipmentDataParser
    private lateinit var updater: ShipmentUpdater

    @BeforeTest
    fun setup() {
        simulator = TrackingSimulator()
        parser = ShipmentDataParser()
        updater = ShipmentUpdater()
        
        simulator.setParser(parser)
        simulator.setUpdater(updater)
    }

    @Test
    fun testAddShipment() {
        val shipment = Shipment(
            status = "Created",
            id = "SHIP001",
            expectedDeliveryDate = 1234567890L,
            currentLocation = "Warehouse A"
        )
        
        simulator.addShipment(shipment)
        
        val foundShipment = simulator.findShipment("SHIP001")
        assertNotNull(foundShipment)
        assertEquals("SHIP001", foundShipment.id)
        assertEquals("Created", foundShipment.status)
        assertEquals(1, foundShipment.updateHistory.size) // Should have initial "Created" update
    }

    @Test
    fun testFindShipmentExists() {
        val shipment = Shipment(
            status = "Created",
            id = "SHIP002",
            expectedDeliveryDate = 1234567890L,
            currentLocation = "Distribution Center"
        )
        
        simulator.addShipment(shipment)
        // Update to shipped status
        simulator.processFileUpdate("Shipped,SHIP002,1234567891")
        
        val foundShipment = simulator.findShipment("SHIP002")
        assertNotNull(foundShipment)
        assertEquals("SHIP002", foundShipment.id)
        assertEquals("Shipped", foundShipment.status)
    }

    @Test
    fun testFindShipmentNotExists() {
        val foundShipment = simulator.findShipment("NONEXISTENT")
        assertNull(foundShipment)
    }

    @Test
    fun testRunSimulationWithEmptyShipments() {
        // Should not crash with empty shipments
        simulator.runSimulation()
        // Test passes if no exception is thrown
    }

    @Test
    fun testRunSimulationWithShipments() {
        val shipment1 = Shipment("Created", "SHIP001", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse")
        val shipment2 = Shipment("Shipped", "SHIP002", expectedDeliveryDate = 1234567891L, currentLocation = "Transit")
        
        simulator.addShipment(shipment1)
        simulator.addShipment(shipment2)
        
        simulator.runSimulation()
        // Test passes if no exception is thrown
    }

    @Test
    fun testProcessUpdateFileWithNonExistentFile() {
        simulator.processUpdateFile("non_existent_file.txt")
        // Should handle gracefully without crashing
    }

    @Test
    fun testProcessUpdateFileWithValidFile() {
        val testFile = File("test_updates.txt")
        try {
            testFile.writeText("Created,SHIP003,1234567890\nShipped,SHIP003,1234567891")
            
            simulator.processUpdateFile("test_updates.txt")
            
            val foundShipment = simulator.findShipment("SHIP003")
            assertNotNull(foundShipment)
            assertEquals("SHIP003", foundShipment.id)
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testProcessFileUpdateCreateNewShipment() {
        val updateString = "Create,SHIP004,1234567890"
        
        simulator.processFileUpdate(updateString)
        
        val foundShipment = simulator.findShipment("SHIP004")
        assertNotNull(foundShipment)
        assertEquals("SHIP004", foundShipment.id)
        assertEquals("Created", foundShipment.status)
    }

    @Test
    fun testProcessFileUpdateExistingShipment() {
        // First create a shipment
        val shipment = Shipment("Created", "SHIP005", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse")
        simulator.addShipment(shipment)
        
        // Then update it
        val updateString = "Shipped,SHIP005,1234567891"
        simulator.processFileUpdate(updateString)
        
        val foundShipment = simulator.findShipment("SHIP005")
        assertNotNull(foundShipment)
        assertEquals("Shipped", foundShipment.status)
        assertTrue(foundShipment.updateHistory.size >= 2) // Original + new update
    }

    @Test
    fun testProcessFileUpdateWithLocation() {
        val shipment = Shipment("Created", "SHIP006", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse")
        simulator.addShipment(shipment)
        
        val updateString = "Location,SHIP006,1234567891,New Distribution Center"
        simulator.processFileUpdate(updateString)
        
        val foundShipment = simulator.findShipment("SHIP006")
        assertNotNull(foundShipment)
        assertEquals("New Distribution Center", foundShipment.currentLocation)
        assertTrue(foundShipment.notes.isNotEmpty()) // Should have location update note
    }

    @Test
    fun testProcessFileUpdateWithNotes() {
        val shipment = Shipment("Created", "SHIP007", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse")
        simulator.addShipment(shipment)
        
        val updateString = "NoteAdded,SHIP007,1234567891,,Important package - handle with care"
        simulator.processFileUpdate(updateString)
        
        val foundShipment = simulator.findShipment("SHIP007")
        assertNotNull(foundShipment)
        assertTrue(foundShipment.notes.any { it.contains("Important package") })
    }

    @Test
    fun testProcessFileUpdateInvalidFormat() {
        val updateString = "InvalidFormat"
        
        // Should handle gracefully without crashing
        simulator.processFileUpdate(updateString)
        // Test passes if no exception is thrown
    }

    @Test
    fun testProcessFileUpdateNonExistentShipment() {
        val updateString = "Shipped,NONEXISTENT,1234567891"
        
        // Should handle gracefully without crashing
        simulator.processFileUpdate(updateString)
        
        // Shipment should not be created for non-Create updates
        val foundShipment = simulator.findShipment("NONEXISTENT")
        assertNull(foundShipment)
    }

    @Test
    fun testSetParser() {
        val newParser = ShipmentDataParser()
        simulator.setParser(newParser)
        
        // Test that parser is set by trying to process an update
        val updateString = "Create,SHIP008,1234567890"
        simulator.processFileUpdate(updateString)
        
        val foundShipment = simulator.findShipment("SHIP008")
        assertNotNull(foundShipment)
    }

    @Test
    fun testSetUpdater() {
        val newUpdater = ShipmentUpdater()
        simulator.setUpdater(newUpdater)
        
        // Test that updater is set by creating and updating a shipment
        val shipment = Shipment("Created", "SHIP009", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse")
        simulator.addShipment(shipment)
        
        val updateString = "Shipped,SHIP009,1234567891"
        simulator.processFileUpdate(updateString)
        
        val foundShipment = simulator.findShipment("SHIP009")
        assertNotNull(foundShipment)
        assertEquals("Shipped", foundShipment.status)
    }

    @Test
    fun testMultipleShipmentsTracking() {
        val shipments = listOf(
            Shipment("Created", "SHIP010", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse A"),
            Shipment("Created", "SHIP011", expectedDeliveryDate = 1234567891L, currentLocation = "Transit"),
            Shipment("Created", "SHIP012", expectedDeliveryDate = 1234567892L, currentLocation = "Customer")
        )
        
        shipments.forEach { simulator.addShipment(it) }
        
        // Update statuses after adding
        simulator.processFileUpdate("Shipped,SHIP011,1234567893")
        simulator.processFileUpdate("Delivered,SHIP012,1234567894")
        
        assertEquals("SHIP010", simulator.findShipment("SHIP010")?.id)
        assertEquals("SHIP011", simulator.findShipment("SHIP011")?.id)
        assertEquals("SHIP012", simulator.findShipment("SHIP012")?.id)
        
        assertEquals("Created", simulator.findShipment("SHIP010")?.status)
        assertEquals("Shipped", simulator.findShipment("SHIP011")?.status)
        assertEquals("Delivered", simulator.findShipment("SHIP012")?.status)
    }

    @Test
    fun testProcessFileUpdateWithEmptyComponents() {
        val shipment = Shipment("Created", "SHIP013", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse")
        simulator.addShipment(shipment)
        
        val updateString = "Shipped,SHIP013,1234567891,,"
        simulator.processFileUpdate(updateString)
        
        val foundShipment = simulator.findShipment("SHIP013")
        assertNotNull(foundShipment)
        assertEquals("Shipped", foundShipment.status)
    }

    @Test
    fun testRunSimulationWithUpdateFile() {
        val testFile = File("simulation_test.txt")
        try {
            testFile.writeText("Created,SHIP014,1234567890\nShipped,SHIP014,1234567891")
            
            simulator.runSimulation("simulation_test.txt")
            
            val foundShipment = simulator.findShipment("SHIP014")
            assertNotNull(foundShipment)
            assertEquals("Shipped", foundShipment.status)
        } finally {
            testFile.delete()
        }
    }

    @Test
    fun testShipmentCreationWithAutoUpdate() {
        val shipment = Shipment("In Progress", "SHIP015", expectedDeliveryDate = 1234567890L, currentLocation = "Warehouse")
        
        simulator.addShipment(shipment)
        
        val foundShipment = simulator.findShipment("SHIP015")
        assertNotNull(foundShipment)
        assertEquals("Created", foundShipment.status) // Should be overridden to "Created"
        assertEquals(1, foundShipment.updateHistory.size) // Should have auto-created update
        assertEquals("Created", foundShipment.updateHistory[0].newStatus)
    }
} 