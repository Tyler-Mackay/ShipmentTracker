package org.example.project.Tracker

import org.example.project.Shipment.Shipment
import org.example.project.Shipment.ShipmentDataParser
import org.example.project.Shipment.ShipmentUpdater
import org.example.project.Shipment.ShipmentFactory
import org.example.project.Shipment.ShipmentType
import org.example.project.ShippingUpdate.ShippingUpdate

enum class SimulatorMode {
    FILE_BASED,
    NETWORK_BASED
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
                // In network mode, shipments are created through TrackingServer
            }
        }
    }
    
    fun runSimulation(updateFilePath: String? = null) {
        if (shipments.isEmpty()) {
            return
        }
        
        updateFilePath?.let { filePath ->
            processUpdateFile(filePath)
        }
    }
    
    fun processUpdateFile(filePath: String) {
        parser?.let { parser ->
            val updates = parser.readFile(filePath)
            if (updates.isEmpty()) return
            
            var processedCount = 0
            var errorCount = 0
            
            updates.forEach { update ->
                if (update.isNotBlank()) {
                    try {
                        processFileUpdate(update)
                        processedCount++
                    } catch (e: Exception) {
                        errorCount++
                    }
                }
            }
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
            if (!parser.isValidUpdateFormat(update)) return
            
            val components = parser.parseUpdate(update)
            if (components.isEmpty()) return
            
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
                        type = ShipmentType.STANDARD,
                        status = "Created",
                        id = shipmentId,
                        creationDate = timestamp,
                        expectedDeliveryDateTimestamp = timestamp + (7 * 24 * 60 * 60 * 1000),
                        currentLocation = "Initial Location"
                    )
                    shipments.add(shipment)
                    return
                } else {
                    return
                }
            }
            
            updater?.let { updater ->
                try {
                    val shippingUpdate = updater.processUpdate(updateType, shipment, timestamp)
                    val updateTypeLower = updateType.lowercase()
                    var estimatedDeliveryTimestamp: Long? = null
                    if (updateTypeLower == "create" || updateTypeLower == "shipped" || updateTypeLower == "delayed") {
                        // For these types, the estimated delivery timestamp is the last number in the CSV
                        // For shipped/delayed, it's the 4th component (index 3)
                        val estStr = if (updateTypeLower == "create") timestampStr else (if (components.size > 3) components[3] else null)
                        estimatedDeliveryTimestamp = estStr?.toLongOrNull()
                    }
                    if (location.isNotBlank()) {
                        shipment.updateLocation(location)
                        shipment.addNote("Location update: $location")
                    }
                    if (notes.isNotBlank()) {
                        shipment.addNote(notes)
                    }
                    shipment.addUpdate(shippingUpdate, estimatedDeliveryTimestamp, updateTypeLower)
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }
    }
    
    private fun processFileUpdateNetworkBased(update: String) {
        parser?.let { parser ->
            if (!parser.isValidUpdateFormat(update)) return
            
            val components = parser.parseUpdate(update)
            if (components.isEmpty()) return
            
            val shipmentId = components[0]
            val updateType = components[1]
            val timestampStr = components.getOrNull(2) ?: ""
            val location = components.getOrNull(3) ?: ""
            val notes = components.getOrNull(4) ?: ""
            
            val timestamp = parser.parseTimestamp(timestampStr)
            
            try {
                when (updateType.toLowerCase()) {
                    "create" -> {
                        val createData = "created,$shipmentId,standard,$timestamp"
                        val createRequest = CreateShipmentRequest(createData)
                        trackingServer.createShipment(createRequest)
                    }
                    else -> {
                        val existingShipment = trackingServer.getShipment(shipmentId)
                        existingShipment?.let { shipment ->
                            updater?.let { updater ->
                                val shippingUpdate = updater.processUpdate(updateType, shipment, timestamp)
                                
                                if (location.isNotBlank()) {
                                    shipment.updateLocation(location)
                                    shipment.addNote("Location update: $location")
                                }
                                
                                if (notes.isNotBlank()) {
                                    shipment.addNote(notes)
                                }
                                
                                shipment.addUpdate(shippingUpdate)
                                
                                val updateData = "$updateType,$shipmentId,$timestamp"
                                val updateRequest = UpdateShipmentRequest(updateData)
                                trackingServer.updateShipment(updateRequest)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
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
    }
}