package org.example.project

import org.example.project.Shipment.*
import org.example.project.Tracker.*
import kotlin.test.*

class TrackingSimulatorTest {

    private lateinit var simulator: TrackingSimulator

    @BeforeTest
    fun setup() {
        simulator = TrackingSimulator()
        simulator.setParser(ShipmentDataParser())
        simulator.setUpdater(ShipmentUpdater())
    }

    @Test
    fun testDefaultMode() {
        assertEquals(SimulatorMode.NETWORK_BASED, simulator.getMode())
    }

    @Test
    fun testSetMode() {
        simulator.setMode(SimulatorMode.FILE_BASED)
        assertEquals(SimulatorMode.FILE_BASED, simulator.getMode())

        simulator.setMode(SimulatorMode.NETWORK_BASED)
        assertEquals(SimulatorMode.NETWORK_BASED, simulator.getMode())
    }

    @Test
    fun testAddShipmentFileBased() {
        simulator.setMode(SimulatorMode.FILE_BASED)

        val shipment = ShipmentFactory.createShipment(
            type = ShipmentType.STANDARD,
            status = "Created",
            id = "FILE001",
            creationDate = 1000L,
            expectedDeliveryDateTimestamp = 5000L,
            currentLocation = "Warehouse"
        )

        simulator.addShipment(shipment)

        val found = simulator.findShipment("FILE001")
        assertNotNull(found)
        assertEquals("FILE001", found.id)
        assertEquals("Created", found.status)
    }

    @Test
    fun testFindShipmentNetworkBased() {
        simulator.setMode(SimulatorMode.NETWORK_BASED)

        // Create through server
        val server = TrackingServer.getInstance()
        val request = CreateShipmentRequest("created,NET001,standard,1000")
        server.createShipment(request)

        val found = simulator.findShipment("NET001")
        assertNotNull(found)
        assertEquals("NET001", found.id)
    }

    @Test
    fun testFindShipmentNotFound() {
        simulator.setMode(SimulatorMode.FILE_BASED)
        val found = simulator.findShipment("NONEXISTENT")
        assertNull(found)
    }

    @Test
    fun testProcessFileUpdateFileBased() {
        simulator.setMode(SimulatorMode.FILE_BASED)

        // Create a shipment first
        simulator.processFileUpdate("create,UPDATE001,1000")

        val created = simulator.findShipment("UPDATE001")
        assertNotNull(created)
        assertEquals("Created", created.status)

        // Update it
        simulator.processFileUpdate("shipped,UPDATE001,2000")

        val updated = simulator.findShipment("UPDATE001")
        assertNotNull(updated)
        assertEquals("Shipped", updated.status)
    }

    @Test
    fun testProcessFileUpdateNetworkBased() {
        simulator.setMode(SimulatorMode.NETWORK_BASED)

        // Create through simulator (delegates to server)
        simulator.processFileUpdate("create,NET002,1000")

        val found = simulator.findShipment("NET002")
        assertNotNull(found)
        assertEquals("NET002", found.id)
    }

    @Test
    fun testSetParser() {
        val parser = ShipmentDataParser()
        simulator.setParser(parser)
        // Test passes if no exception thrown
    }

    @Test
    fun testSetUpdater() {
        val updater = ShipmentUpdater()
        simulator.setUpdater(updater)
        // Test passes if no exception thrown
    }

    @Test
    fun testRunSimulationEmpty() {
        simulator.setMode(SimulatorMode.FILE_BASED)
        simulator.runSimulation()
        // Should not crash with empty shipments
    }

    @Test
    fun testProcessInvalidUpdate() {
        simulator.setMode(SimulatorMode.FILE_BASED)

        // Should handle invalid format gracefully
        simulator.processFileUpdate("invalid")
        simulator.processFileUpdate("")
        simulator.processFileUpdate("onlyonepart")

        // Test passes if no exception thrown
    }

    @Test
    fun testModeTransition() {
        // Start in file-based mode
        simulator.setMode(SimulatorMode.FILE_BASED)
        simulator.processFileUpdate("create,TRANS001,1000")

        val fileShipment = simulator.findShipment("TRANS001")
        assertNotNull(fileShipment)

        // Switch to network-based mode
        simulator.setMode(SimulatorMode.NETWORK_BASED)

        // Create in network mode
        simulator.processFileUpdate("create,TRANS002,1000")

        val networkShipment = simulator.findShipment("TRANS002")
        assertNotNull(networkShipment)

        // File-based shipment should still exist in file mode
        simulator.setMode(SimulatorMode.FILE_BASED)
        val stillExists = simulator.findShipment("TRANS001")
        assertNotNull(stillExists)
    }
}