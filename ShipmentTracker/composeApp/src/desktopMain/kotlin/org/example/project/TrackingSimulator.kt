package org.example.project

class TrackingSimulator {
    private val shipments: MutableList<Shipment> = mutableListOf()
    private var parser: ShipmentDataParser? = null
    private var updater: ShipmentUpdater? = null
    
    fun findShipment(id: String): Shipment? {
        return shipments.find { it.id == id }
    }
    
    fun addShipment(shipment: Shipment) {
        shipments.add(shipment)
        
        val createUpdate = ShippingUpdate(
            previousStatus = "",
            newStatus = "Created",
            timestamp = System.currentTimeMillis()
        )
        
        shipment.addUpdate(createUpdate)
    }
    
    fun runSimulation(updateFilePath: String? = null) {
        println("Starting simulation...")
        
        if (shipments.isEmpty()) {
            println("No shipments to track")
            return
        }
        
        shipments.forEach { shipment ->
            println("Tracking shipment: ${shipment.id} - Status: ${shipment.status}")
        }
        
        updateFilePath?.let { filePath ->
            println("Processing update file: $filePath")
            processUpdateFile(filePath)
        }
        
        println("Simulation completed. Total shipments: ${shipments.size}")
    }
    
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
    
    fun processFileUpdate(update: String) {
        parser?.let { parser ->
            if (!parser.isValidUpdateFormat(update)) {
                println("Warning: Invalid update format: $update")
                return
            }
            
            val components = parser.parseUpdate(update)
            if (components.isEmpty()) {
                println("Warning: Failed to parse update: $update")
                return
            }
            
            val shipmentId = components[0]
            val updateType = components[1]
            val timestampStr = components.getOrNull(2) ?: ""
            val location = components.getOrNull(3) ?: ""
            val notes = components.getOrNull(4) ?: ""
            
            val timestamp = parser.parseTimestamp(timestampStr)
            
            var shipment = findShipment(shipmentId)
            if (shipment == null) {
                if (updateType == "Create") {
                    shipment = Shipment(
                        status = "Created",
                        id = shipmentId,
                        expectedDeliveryDate = timestamp + (7 * 24 * 60 * 60 * 1000),
                        currentLocation = "Initial Location"
                    )
                    addShipment(shipment)
                    println("Created new shipment: $shipmentId")
                    return
                } else {
                    println("Warning: Shipment not found for update: $shipmentId")
                    return
                }
            }
            
            updater?.let { updater ->
                try {
                    val shippingUpdate = updater.processUpdate(updateType, shipment, timestamp)
                    
                    if (location.isNotBlank()) {
                        shipment.updateLocation(location)
                        shipment.addNote("Location update: $location")
                    }
                    
                    if (notes.isNotBlank()) {
                        shipment.addNote(notes)
                    }
                    
                    shipment.addUpdate(shippingUpdate)
                    
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
    
    fun setParser(parser: ShipmentDataParser) {
        this.parser = parser
    }
    
    fun setUpdater(updater: ShipmentUpdater) {
        this.updater = updater
    }
} 