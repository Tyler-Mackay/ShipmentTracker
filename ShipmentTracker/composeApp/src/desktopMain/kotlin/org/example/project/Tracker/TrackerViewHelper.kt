package org.example.project.Tracker

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.example.project.Shipment.Shipment
import org.example.project.Shipment.ShipmentDataParser
import org.example.project.Shipment.ShipmentObserver
import org.example.project.Shipment.ShipmentUpdater
import org.example.project.Shipment.ShipmentFactory
import org.example.project.Shipment.ShipmentType
import org.example.project.ShippingUpdate.ShippingUpdate
import java.text.SimpleDateFormat
import java.util.*

class TrackerViewHelper : ShipmentObserver, UI {
    val activeTrackingShipments: MutableList<String> = mutableListOf()
    
    private var _shipmentId by mutableStateOf("")
    val shipmentId: String get() = _shipmentId
    
    private var _shipmentTotes by mutableStateOf(arrayOf<String>())
    val shipmentTotes: Array<String> get() = _shipmentTotes
    
    private var _shipmentUpdateHistory by mutableStateOf(arrayOf<String>())
    val shipmentUpdateHistory: Array<String> get() = _shipmentUpdateHistory
    
    private var _expectedShipmentDeliveryDate by mutableStateOf(arrayOf<String>())
    val expectedShipmentDeliveryDate: Array<String> get() = _expectedShipmentDeliveryDate
    
    private var _shipmentStatus by mutableStateOf("")
    val shipmentStatus: String get() = _shipmentStatus
    
    private var _errorMessage by mutableStateOf<String?>(null)
    val errorMessage: String? get() = _errorMessage
    
    private var _isLoading by mutableStateOf(false)
    val isLoading: Boolean get() = _isLoading
    
    private val trackedShipmentsData: MutableMap<String, Shipment> = mutableMapOf()
    private var simulator: TrackingSimulator? = null
    private val viewHelperScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    fun initialize() {
        // TrackingSimulator disabled for now - using TrackingServer instead
        /*
        val trackingSimulator = TrackingSimulator()
        trackingSimulator.setParser(ShipmentDataParser())
        trackingSimulator.setUpdater(ShipmentUpdater())
        
        setSimulator(trackingSimulator)
        */
        
        // Don't load from file - we'll get data from TrackingServer instead
        // loadShipmentsFromFile()
        
        // Create some sample shipments for testing the UI
        createSampleShipments()
    }
    
    fun setSimulator(simulator: TrackingSimulator) {
        this.simulator = simulator
    }
    
    private fun loadShipmentsFromFile() {
        viewHelperScope.launch {
            try {
                val parser = ShipmentDataParser()
                val lines = parser.readFile("test.txt")
                
                if (lines.isEmpty()) {
                    println("No data found in test.txt, falling back to sample data")
                    createSampleShipments()
                    return@launch
                }
                
                println("Starting live file processing - ${lines.size} lines to process")
                
                for ((index, line) in lines.withIndex()) {
                    if (line.isNotBlank()) {
                        try {
                            simulator?.processFileUpdate(line)
                            println("Processed line ${index + 1}/${lines.size}: $line")
                        } catch (e: Exception) {
                            println("Error processing line ${index + 1}: $line - ${e.message}")
                        }
                    }
                    
                    if (index < lines.size - 1) {
                        delay(1000)
                    }
                }
                
                println("Live file processing completed")
                
            } catch (e: Exception) {
                println("Error loading shipments from test.txt: ${e.message}")
                createSampleShipments()
            }
        }
    }
    
    private fun createSampleShipments() {
        val currentTime = System.currentTimeMillis()
        val sampleShipments = listOf(
            ShipmentFactory.createShipment(
                type = ShipmentType.STANDARD,
                status = "Created",
                id = "SHIP001",
                creationDate = currentTime,
                expectedDeliveryDateTimestamp = currentTime + (3 * 24 * 60 * 60 * 1000),
                currentLocation = "Warehouse A"
            ),
            ShipmentFactory.createShipment(
                type = ShipmentType.EXPRESS,
                status = "Shipped",
                id = "SHIP002",
                creationDate = currentTime,
                expectedDeliveryDateTimestamp = currentTime + (5 * 24 * 60 * 60 * 1000),
                currentLocation = "Distribution Center"
            ),
            ShipmentFactory.createShipment(
                type = ShipmentType.OVERNIGHT,
                status = "In Transit",
                id = "SHIP003",
                creationDate = currentTime,
                expectedDeliveryDateTimestamp = currentTime + (2 * 24 * 60 * 60 * 1000),
                currentLocation = "Highway 101"
            )
        )
        
        sampleShipments.forEach { shipment ->
            simulator?.addShipment(shipment)
        }
    }
    
    fun toggleTracking(shipmentId: String) {
        val trimmedId = shipmentId.trim()
        if (trimmedId.isBlank()) {
            _errorMessage = "Please enter a shipment ID"
            return
        }
        
        if (isTrackingShipment(trimmedId)) {
            println("Stopping tracking for shipment: $trimmedId")
            stopTrackingShipment(trimmedId)
        } else {
            println("Starting tracking for shipment: $trimmedId")
            startTracking(trimmedId)
        }
    }
    
    private fun startTracking(shipmentId: String) {
        viewHelperScope.launch {
            _isLoading = true
            _errorMessage = null
            
            if (!shipmentExists(shipmentId)) {
                _errorMessage = "Shipment ID '$shipmentId' not found"
                _isLoading = false
                notifyIfShipmentDoesntExist()
                return@launch
            }
            
            if (trackShipment(shipmentId)) {
                println("Now tracking shipment: $shipmentId")
            } else {
                _errorMessage = "Failed to track shipment '$shipmentId'"
            }
            
            _isLoading = false
        }
    }
    
    fun trackShipment(id: String): Boolean {
        println("DEBUG: TrackerViewHelper.trackShipment called for ID: $id")
        val server = org.example.project.TrackingServer.getInstance()
        println("DEBUG: Got TrackingServer instance: $server")
        val shipment = server.getShipment(id)
        println("DEBUG: TrackingServer.getShipment returned: $shipment")
        if (shipment != null) {
            if (!activeTrackingShipments.contains(id)) {
                activeTrackingShipments.add(id)
                trackedShipmentsData[id] = shipment
                updateAllStateProperties()
                println("DEBUG: Shipment $id added to tracking")
            } else {
                println("DEBUG: Shipment $id already being tracked")
            }
            return true
        }
        println("DEBUG: Shipment $id NOT FOUND - returning false")
        return false
    }
    
    fun stopTracking() {
        simulator?.let { sim ->
            activeTrackingShipments.forEach { shipmentId ->
                sim.findShipment(shipmentId)?.removeObserver(this)
            }
        }
        
        activeTrackingShipments.clear()
        trackedShipmentsData.clear()
        clearAllStateProperties()
        
        println("Stopped tracking all shipments")
    }
    
    fun stopTrackingShipment(id: String) {
        simulator?.let { sim ->
            sim.findShipment(id)?.removeObserver(this)
        }
        
        activeTrackingShipments.remove(id)
        trackedShipmentsData.remove(id)
        updateAllStateProperties()
        
        println("Stopped tracking shipment: $id")
    }
    
    fun clearError() {
        viewHelperScope.launch {
            _errorMessage = null
        }
    }
    
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun cleanup() {
        viewHelperScope.cancel()
        stopTracking()
    }
    
    fun getTrackedShipments(): Map<String, Shipment> {
        return trackedShipmentsData.toMap()
    }
    
    fun isTrackingShipment(id: String): Boolean {
        return activeTrackingShipments.contains(id)
    }
    
    override fun onShipmentUpdate(shipmentId: String, update: ShippingUpdate) {
        if (activeTrackingShipments.contains(shipmentId)) {
            simulator?.let { sim ->
                val updatedShipment = sim.findShipment(shipmentId)
                if (updatedShipment != null) {
                    trackedShipmentsData[shipmentId] = createShipmentCopy(updatedShipment)
                    updateAllStateProperties()
                    
                    println("TrackerViewHelper: Updated tracking data for shipment $shipmentId -> ${update.newStatus}")
                }
            }
        }
    }
    
    override fun notifyIfShipmentDoesntExist() {
        println("UI Notification: Shipment doesn't exist")
    }
    
    fun shipmentExists(id: String): Boolean {
        println("DEBUG: shipmentExists called for ID: $id")
        val server = org.example.project.TrackingServer.getInstance()
        val exists = server.getShipment(id) != null
        println("DEBUG: shipmentExists result for $id: $exists")
        return exists
    }
    
    private fun createShipmentCopy(shipment: Shipment): Shipment {
        return ShipmentFactory.createShipment(
            type = shipment.shipmentType,
            status = shipment.status,
            id = shipment.id,
            creationDate = shipment.creationDate,
            expectedDeliveryDateTimestamp = shipment.expectedDeliveryDateTimestamp,
            currentLocation = shipment.currentLocation
        ).apply {
            // Copy over the mutable state
            notes.addAll(shipment.notes)
            updateHistory.addAll(shipment.updateHistory)
        }
    }
    
    private fun updateAllStateProperties() {
        _shipmentId = activeTrackingShipments.lastOrNull() ?: ""
        
        _shipmentTotes = trackedShipmentsData.values.map { shipment ->
            "${shipment.id}: ${shipment.status} at ${shipment.currentLocation}"
        }.toTypedArray()
        
        _shipmentUpdateHistory = trackedShipmentsData.values.flatMap { shipment ->
            shipment.updateHistory.map { update ->
                "${shipment.id}: ${formatUpdateDescription(update)}"
            }
        }.sortedByDescending { 
            trackedShipmentsData.values.flatMap { it.updateHistory }.maxOfOrNull { it.timestamp } ?: 0L
        }.toTypedArray()
        
        _expectedShipmentDeliveryDate = trackedShipmentsData.values.map { shipment ->
            "${shipment.id}: ${formatDate(shipment.expectedDeliveryDateTimestamp)}"
        }.toTypedArray()
        
        _shipmentStatus = trackedShipmentsData[_shipmentId]?.status ?: ""
    }
    
    private fun clearAllStateProperties() {
        _shipmentId = ""
        _shipmentTotes = arrayOf()
        _shipmentUpdateHistory = arrayOf()
        _expectedShipmentDeliveryDate = arrayOf()
        _shipmentStatus = ""
    }
    
    private fun formatUpdateDescription(update: ShippingUpdate): String {
        return if (update.previousStatus.isBlank()) {
            "Status set to ${update.newStatus} at ${formatDate(update.timestamp)}"
        } else {
            "${update.previousStatus} → ${update.newStatus} at ${formatDate(update.timestamp)}"
        }
    }
} 