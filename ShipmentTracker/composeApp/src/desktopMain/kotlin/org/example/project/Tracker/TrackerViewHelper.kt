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
import org.example.project.Tracker.SimulatorMode
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
        // Create a TrackingSimulator in network mode (uses TrackingServer as its data source)
        val trackingSimulator = TrackingSimulator()
        trackingSimulator.setMode(SimulatorMode.NETWORK_BASED)
        trackingSimulator.setParser(ShipmentDataParser())
        trackingSimulator.setUpdater(ShipmentUpdater())
        
        setSimulator(trackingSimulator)
        
        // Don't load from file - we get data from TrackingServer instead
        createSampleShipments()
    }
    
    /**
     * Start automatic refresh of tracked shipments every 2 seconds
     */
    private fun startAutoRefresh() {
        viewHelperScope.launch {
            while (true) {
                try {
                    if (activeTrackingShipments.isNotEmpty()) {
                        refreshTrackedShipments()
                    }
                    delay(10000) // Refresh every 10 seconds (reduced spam)
                } catch (e: Exception) {
                    // Ignore cancellation exceptions - they're normal during shutdown
                    if (e.message?.contains("cancelled") != true) {
                        println("Auto-refresh error: ${e.message}")
                    }
                }
            }
        }
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
        simulator?.let { sim ->
            val shipment = sim.findShipment(id)
            if (shipment != null) {
                if (!activeTrackingShipments.contains(id)) {
                    activeTrackingShipments.add(id)
                    trackedShipmentsData[id] = createShipmentCopy(shipment)
                    
                    shipment.addObserver(this)
                    updateAllStateProperties()
                    forceUIUpdate()  // 🔥 Trigger UI update when new shipment added
                    
                    println("🎯 Started tracking shipment $id - UI should update!")
                    return true
                }
            }
        }
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
    
    /**
     * Refresh all tracked shipments from the server
     * Call this to update the UI when shipments have been modified on the server
     */
    fun refreshTrackedShipments() {
        println("DEBUG: refreshTrackedShipments called")
        val server = org.example.project.TrackingServer.getInstance()
        
        // Refresh each tracked shipment from the server
        activeTrackingShipments.forEach { shipmentId ->
            val oldShipment = trackedShipmentsData[shipmentId]
            val updatedShipment = server.getShipment(shipmentId)
            if (updatedShipment != null) {
                // Only update if there's an actual change
                val hasChanged = oldShipment?.status != updatedShipment.status || 
                               oldShipment?.currentLocation != updatedShipment.currentLocation
                
                if (hasChanged) {
                    println("🔄 Manual refresh detected change for $shipmentId: ${oldShipment?.status} -> ${updatedShipment.status}")
                    trackedShipmentsData[shipmentId] = updatedShipment
                } else if (oldShipment !== updatedShipment) {
                    // Force update even if no changes detected
                    trackedShipmentsData[shipmentId] = updatedShipment
                }
            }
        }
        
        updateAllStateProperties()
        forceUIUpdate()  // Force Compose recomposition
        println("DEBUG: Finished refreshing tracked shipments")
    }

    fun stopTrackingShipment(id: String) {
        simulator?.let { sim ->
            sim.findShipment(id)?.removeObserver(this)
        }
        
        activeTrackingShipments.remove(id)
        trackedShipmentsData.remove(id)
        updateAllStateProperties()
        forceUIUpdate()  // 🔥 Trigger UI update when shipment removed
        
        println("🛑 Stopped tracking shipment: $id - UI should update!")
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
        println("DEBUG: getTrackedShipments called, returning: ${trackedShipmentsData.keys}")
        return trackedShipmentsData.toMap()
    }
    
    // Add a state that triggers UI recomposition
    private var _trackedShipmentsState = mutableStateOf(emptyMap<String, Shipment>())
    val trackedShipmentsState = _trackedShipmentsState
    
    // Add a version counter to force updates even when objects are the same
    private var _updateVersion = mutableStateOf(0)
    val updateVersion = _updateVersion
    
    // Force UI update by updating the observable state
    private fun forceUIUpdate() {
        println("DEBUG: forceUIUpdate called on thread: ${Thread.currentThread().name}")
        
        // Simple approach: clear and rebuild state to guarantee change detection
        _trackedShipmentsState.value = emptyMap()  // Clear first
        _updateVersion.value = _updateVersion.value + 1  // Increment version
        _trackedShipmentsState.value = trackedShipmentsData.toMap()  // Set new value
        
        println("DEBUG: UI state updated with ${_trackedShipmentsState.value.size} shipments, version: ${_updateVersion.value}")
        
        // Debug: Show current shipment statuses
        _trackedShipmentsState.value.forEach { (id, shipment) ->
            println("DEBUG: UI state contains - ID: $id, Status: ${shipment.status}, Location: ${shipment.currentLocation}")
        }
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
                    forceUIUpdate()  // 🔥 This triggers Compose recomposition!
                    
                    println("🔔 TrackerViewHelper: Observer received update for shipment $shipmentId -> ${update.newStatus}")
                    println("🎨 UI should now automatically update!")
                }
            }
        }
    }
    
    override fun notifyIfShipmentDoesntExist() {
        println("UI Notification: Shipment doesn't exist")
    }
    
    fun shipmentExists(id: String): Boolean {
        return simulator?.findShipment(id) != null
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
        println("DEBUG: updateAllStateProperties called")
        println("DEBUG: activeTrackingShipments: $activeTrackingShipments")
        println("DEBUG: trackedShipmentsData keys: ${trackedShipmentsData.keys}")
        
        _shipmentId = activeTrackingShipments.lastOrNull() ?: ""
        println("DEBUG: Set _shipmentId to: $_shipmentId")
        
        _shipmentTotes = trackedShipmentsData.values.map { shipment ->
            "${shipment.id}: ${shipment.status} at ${shipment.currentLocation}"
        }.toTypedArray()
        println("DEBUG: Set _shipmentTotes to: ${_shipmentTotes.contentToString()}")
        
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
        println("DEBUG: Set _shipmentStatus to: $_shipmentStatus")
        println("DEBUG: updateAllStateProperties completed")
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