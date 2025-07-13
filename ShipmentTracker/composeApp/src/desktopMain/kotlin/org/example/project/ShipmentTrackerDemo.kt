package org.example.project

/**
 * Demonstration class showing how to use the complete shipment tracking system
 */
class ShipmentTrackerDemo {
    
    /**
     * Runs a complete demo of the shipment tracking system
     */
    fun runDemo() {
        println("=== Shipment Tracker Demo ===")
        
        // Create the main simulator
        val simulator = TrackingSimulator()
        
        // Create and configure dependencies
        val parser = ShipmentDataParser()
        val updater = ShipmentUpdater()
        val viewHelper = TrackerViewHelper()
        
        // Set up the simulator with dependencies
        simulator.setParser(parser)
        simulator.setUpdater(updater)
        simulator.setViewHelper(viewHelper)
        
        // Create a simple observer to log updates
        val observer = object : ShipmentObserver {
            override fun onShipmentUpdate(shipmentId: String, update: ShippingUpdate) {
                println("📦 UPDATE RECEIVED for $shipmentId: ${update.previousStatus} -> ${update.newStatus} at ${update.timestamp}")
            }
        }
        
        simulator.addObserver(observer)
        
        // Create some sample shipments
        val shipment1 = Shipment(
            status = "Created",
            id = "SHIP001",
            expectedDeliveryDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days from now
            currentLocation = "Warehouse A"
        )
        
        val shipment2 = Shipment(
            status = "Created", 
            id = "SHIP002",
            expectedDeliveryDate = System.currentTimeMillis() + (5 * 24 * 60 * 60 * 1000), // 5 days from now
            currentLocation = "Warehouse B"
        )
        
        // Add shipments to the simulator
        println("\n--- Adding Shipments ---")
        simulator.addShipment(shipment1)
        simulator.addShipment(shipment2)
        
        // Track shipments with the view helper
        viewHelper.trackShipment("SHIP001")
        viewHelper.trackShipment("SHIP002")
        
        // Process some sample updates
        println("\n--- Processing Updates ---")
        
        // Sample updates in CSV format: ID,UPDATE_TYPE,TIMESTAMP,LOCATION,NOTES
        val sampleUpdates = listOf(
            "SHIP001,Shipped,${System.currentTimeMillis()},Distribution Center,Package shipped from warehouse",
            "SHIP002,Shipped,${System.currentTimeMillis()},Distribution Center,Package shipped from warehouse",
            "SHIP001,Location,${System.currentTimeMillis()},In Transit - Highway 101,Package on delivery truck",
            "SHIP002,Delayed,${System.currentTimeMillis()},Distribution Center,Weather delay",
            "SHIP001,Delivered,${System.currentTimeMillis()},Customer Address,Package delivered successfully",
            "SHIP002,Location,${System.currentTimeMillis()},Local Facility,Package arrived at local facility"
        )
        
        // Process each update
        sampleUpdates.forEach { update ->
            println("Processing: $update")
            simulator.processFileUpdate(update)
            Thread.sleep(100) // Small delay for demonstration
        }
        
        // Run the simulation
        println("\n--- Running Simulation ---")
        simulator.runSimulation()
        
        // Display final status
        println("\n--- Final Status ---")
        simulator.getAllShipments().forEach { shipment ->
            println("Shipment ${shipment.id}:")
            println("  Status: ${shipment.status}")
            println("  Location: ${shipment.currentLocation}")
            println("  Notes: ${shipment.notes.joinToString("; ")}")
            println("  Update History: ${shipment.updateHistory.size} updates")
            println("  Latest Update: ${shipment.getLatestUpdate()?.let { "${it.newStatus} at ${it.timestamp}" } ?: "None"}")
            println()
        }
        
        // Show tracked shipments
        println("--- Tracked Shipments ---")
        viewHelper.getActiveTrackingShipments().forEach { id ->
            println("Tracking: $id")
        }
        
        println("\n=== Demo Complete ===")
    }
}

 