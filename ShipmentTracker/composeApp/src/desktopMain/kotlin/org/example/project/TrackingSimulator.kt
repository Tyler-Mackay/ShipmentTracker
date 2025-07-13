package org.example.project

/**
 * TrackingSimulator manages shipments and handles updates through simulation
 * This class serves as the main controller for the shipment tracking system
 */
class TrackingSimulator {
    // Properties as defined in the UML diagram
    private val shipments: MutableList<Shipment> = mutableListOf()
    private var parser: ShipmentDataParser? = null
    private var updater: ShipmentUpdater? = null
    private val observers: MutableList<ShipmentObserver> = mutableListOf()
    private var viewHelper: TrackerViewHelper? = null
    
    /**
     * Finds a shipment by its ID
     * @param id The shipment ID to search for
     * @return The shipment if found, null otherwise
     */
    fun findShipment(id: String): Shipment? {
        return shipments.find { it.id == id }
    }
    
    /**
     * Adds a shipment to the tracking system
     * @param shipment The shipment to add
     */
    fun addShipment(shipment: Shipment) {
        shipments.add(shipment)
        
        // Create an initial "Created" update
        val createUpdate = ShippingUpdate(
            previousStatus = "",
            newStatus = "Created",
            timestamp = System.currentTimeMillis()
        )
        
        // Add the update to the shipment's history
        shipment.addUpdate(createUpdate)
        
        // Notify observers of the new shipment
        notifyObservers(shipment, createUpdate)
    }
    
    /**
     * Runs the simulation process
     * This method orchestrates the simulation of shipment updates
     * @param updateFilePath Optional path to an update file to process
     */
    fun runSimulation(updateFilePath: String? = null) {
        println("Starting simulation...")
        
        // Display current shipments
        if (shipments.isEmpty()) {
            println("No shipments to track")
            return
        }
        
        shipments.forEach { shipment ->
            println("Tracking shipment: ${shipment.id} - Status: ${shipment.status}")
        }
        
        // Process update file if provided
        updateFilePath?.let { filePath ->
            println("Processing update file: $filePath")
            processUpdateFile(filePath)
        }
        
        println("Simulation completed. Total shipments: ${shipments.size}")
    }
    
    /**
     * Processes all updates from a file
     * @param filePath The path to the update file
     */
    fun processUpdateFile(filePath: String) {
        parser?.let { parser ->
            val updates = parser.readFile(filePath)
            if (updates.isEmpty()) {
                println("No updates found in file: $filePath")
                return
            }
            
            println("Processing ${updates.size} updates from file...")
            var processedCount = 0
            var errorCount = 0
            
            updates.forEach { update ->
                if (update.isNotBlank()) {
                    try {
                        processFileUpdate(update)
                        processedCount++
                    } catch (e: Exception) {
                        println("Error processing update '$update': ${e.message}")
                        errorCount++
                    }
                }
            }
            
            println("Update processing complete. Processed: $processedCount, Errors: $errorCount")
        } ?: run {
            println("Error: No parser available to process update file")
        }
    }
    
    /**
     * Processes a file update string
     * @param update The update string to process
     */
    fun processFileUpdate(update: String) {
        parser?.let { parser ->
            if (!parser.isValidUpdateFormat(update)) {
                println("Warning: Invalid update format: $update")
                return
            }
            
            // Parse the update string
            val components = parser.parseUpdate(update)
            if (components.isEmpty()) {
                println("Warning: Failed to parse update: $update")
                return
            }
            
            // Extract components: [SHIPMENT_ID, UPDATE_TYPE, TIMESTAMP, LOCATION, NOTES]
            val shipmentId = components[0]
            val updateType = components[1]
            val timestampStr = components.getOrNull(2) ?: ""
            val location = components.getOrNull(3) ?: ""
            val notes = components.getOrNull(4) ?: ""
            
            // Parse timestamp
            val timestamp = parser.parseTimestamp(timestampStr)
            
            // Handle creating shipments for "Create" updates
            var shipment = findShipment(shipmentId)
            if (shipment == null) {
                if (updateType == "Create") {
                    // Create a new shipment using addShipment which handles the initial update
                    shipment = Shipment(
                        status = "Created",
                        id = shipmentId,
                        expectedDeliveryDate = timestamp + (7 * 24 * 60 * 60 * 1000), // 7 days from creation
                        currentLocation = "Initial Location"
                    )
                    addShipment(shipment)
                    println("Created new shipment: $shipmentId")
                    return // Early return since addShipment already handled the update
                } else {
                    println("Warning: Shipment not found for update: $shipmentId")
                    return
                }
            }
            
            // Apply the update using the updater
            updater?.let { updater ->
                try {
                    val shippingUpdate = updater.processUpdate(updateType, shipment, timestamp)
                    
                    // Update the shipment with the new information
                    if (location.isNotBlank()) {
                        shipment.updateLocation(location)
                        shipment.addNote("Location update: $location")
                    }
                    
                    if (notes.isNotBlank()) {
                        shipment.addNote(notes)
                    }
                    
                    // Add the update to the shipment's history
                    shipment.addUpdate(shippingUpdate)
                    
                    // Notify observers
                    notifyObservers(shipment, shippingUpdate)
                    
                    println("Successfully processed update for shipment $shipmentId: $updateType")
                } catch (e: Exception) {
                    println("Error processing update for shipment $shipmentId: ${e.message}")
                }
            } ?: run {
                println("Warning: No updater available to process update")
            }
        } ?: run {
            println("Warning: No parser available to process update: $update")
        }
    }
    
    /**
     * Adds an observer to the tracking system
     * @param observer The observer to add
     */
    fun addObserver(observer: ShipmentObserver) {
        observers.add(observer)
    }
    
    /**
     * Notifies all observers of a shipment update
     * @param shipment The shipment that was updated
     * @param update The update that occurred
     */
    fun notifyObservers(shipment: Shipment, update: ShippingUpdate) {
        observers.forEach { observer ->
            observer.onShipmentUpdate(shipment.id, update)
        }
    }
    
    /**
     * Sets the shipment data parser
     * @param parser The parser to use for processing update strings
     */
    fun setParser(parser: ShipmentDataParser) {
        this.parser = parser
    }
    
    /**
     * Sets the shipment updater
     * @param updater The updater to use for applying updates to shipments
     */
    fun setUpdater(updater: ShipmentUpdater) {
        this.updater = updater
    }
    
    /**
     * Sets the view helper
     * @param viewHelper The view helper for UI interactions
     */
    fun setViewHelper(viewHelper: TrackerViewHelper) {
        this.viewHelper = viewHelper
    }
    
    /**
     * Gets all shipments in the system
     * @return A list of all shipments
     */
    fun getAllShipments(): List<Shipment> {
        return shipments.toList()
    }
    
    /**
     * Gets the number of shipments being tracked
     * @return The count of shipments
     */
    fun getShipmentCount(): Int {
        return shipments.size
    }
    
    /**
     * Removes a shipment from the tracking system
     * @param id The ID of the shipment to remove
     * @return True if the shipment was removed, false if not found
     */
    fun removeShipment(id: String): Boolean {
        return shipments.removeIf { it.id == id }
    }
} 