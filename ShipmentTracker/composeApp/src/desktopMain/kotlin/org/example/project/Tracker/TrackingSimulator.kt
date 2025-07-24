package org.example.project.Tracker

import org.example.project.Shipment.Shipment
import org.example.project.Shipment.ShipmentDataParser
import org.example.project.Shipment.ShipmentUpdater
import org.example.project.Shipment.ShipmentFactory
import org.example.project.Shipment.ShipmentType
import org.example.project.ShippingUpdate.ShippingUpdate
import org.example.project.TrackingServer
import org.example.project.CreateShipmentRequest
import org.example.project.UpdateShipmentRequest

enum class SimulatorMode {
    FILE_BASED,    // Original file-based simulation (currently disabled)
    NETWORK_BASED  // Network mode using TrackingServer
}

class TrackingSimulator {
    private val shipments: MutableList<Shipment> = mutableListOf()
    private var parser: ShipmentDataParser? = null
    private var updater: ShipmentUpdater? = null
    private var mode: SimulatorMode = SimulatorMode.NETWORK_BASED
    private val trackingServer = TrackingServer.getInstance()
    
    fun findShipment(id: String): Shipment? {
        return when (mode) {
            SimulatorMode.FILE_BASED -> shipments.find { it.id == id }
            SimulatorMode.NETWORK_BASED -> trackingServer.getShipment(id)
        }
    }
    
    fun addShipment(shipment: Shipment) {
        when (mode) {
            SimulatorMode.FILE_BASED -> {
                shipments.add(shipment)
                
                val createUpdate = ShippingUpdate(
                    previousStatus = "",
                    newStatus = "Created",
                    timestamp = System.currentTimeMillis()
                )
                
                shipment.addUpdate(createUpdate)
            }
            SimulatorMode.NETWORK_BASED -> {
                // In network mode, we don't directly add shipments here
                // They should be created through the TrackingServer's createShipment method
                println("Warning: addShipment called in NETWORK_BASED mode. Use TrackingServer.createShipment instead.")
            }
        }
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
        when (mode) {
            SimulatorMode.FILE_BASED -> processFileUpdateFileBased(update)
            SimulatorMode.NETWORK_BASED -> processFileUpdateNetworkBased(update)
        }
    }
    
    private fun processFileUpdateFileBased(update: String) {
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
            
            var shipment = shipments.find { it.id == shipmentId }
            if (shipment == null) {
                if (updateType == "Create") {
                    shipment = ShipmentFactory.createShipment(
                        type = ShipmentType.STANDARD, // Default to standard
                        status = "Created",
                        id = shipmentId,
                        creationDate = timestamp,
                        expectedDeliveryDateTimestamp = timestamp + (7 * 24 * 60 * 60 * 1000),
                        currentLocation = "Initial Location"
                    )
                    shipments.add(shipment)
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
    
    private fun processFileUpdateNetworkBased(update: String) {
        // In network mode, we delegate to TrackingServer
        // This is typically called from TrackerViewHelper when processing simulation files
        println("Processing update in network mode: $update")
        
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
            
                            try {
                when (updateType.toLowerCase()) {
                    "create" -> {
                        // Create shipment data in the format expected by CreateShipmentRequest
                        val createData = "created,$shipmentId,standard,$timestamp"
                        val createRequest = CreateShipmentRequest(createData)
                        trackingServer.createShipment(createRequest)
                        println("Created new shipment via TrackingServer: $shipmentId")
                    }
                    else -> {
                        // Handle other update types
                        val existingShipment = trackingServer.getShipment(shipmentId)
                        if (existingShipment != null) {
                            updater?.let { updater ->
                                val shippingUpdate = updater.processUpdate(updateType, existingShipment, timestamp)
                                
                                if (location.isNotBlank()) {
                                    existingShipment.updateLocation(location)
                                    existingShipment.addNote("Location update: $location")
                                }
                                
                                if (notes.isNotBlank()) {
                                    existingShipment.addNote(notes)
                                }
                                
                                existingShipment.addUpdate(shippingUpdate)
                                
                                // Create update data in the format expected by UpdateShipmentRequest
                                val updateData = "$updateType,$shipmentId,$timestamp"
                                val updateRequest = UpdateShipmentRequest(updateData)
                                trackingServer.updateShipment(updateRequest)
                                
                                println("Successfully processed update via TrackingServer for shipment $shipmentId: $updateType")
                            }
                        } else {
                            println("Warning: Shipment not found in TrackingServer for update: $shipmentId")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error processing update via TrackingServer: ${e.message}")
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
    
    fun setMode(mode: SimulatorMode) {
        this.mode = mode
        println("TrackingSimulator mode set to: $mode")
    }
    
    fun getMode(): SimulatorMode = mode
} 