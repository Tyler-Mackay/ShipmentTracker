package org.example.project

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for managing shipment tracking state with coroutines
 */
class ShipmentTrackerViewModel : UI {
    private val simulator = TrackingSimulator()
    private var _trackedShipments by mutableStateOf(mapOf<String, Shipment>())
    val trackedShipments: Map<String, Shipment> get() = _trackedShipments
    
    private var _errorMessage by mutableStateOf<String?>(null)
    val errorMessage: String? get() = _errorMessage
    
    private var _isLoading by mutableStateOf(false)
    val isLoading: Boolean get() = _isLoading
    
    // Version counter to force recomposition
    private var _uiVersion by mutableStateOf(0)
    val uiVersion: Int get() = _uiVersion
    
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        // Initialize the simulator with dependencies
        simulator.setParser(ShipmentDataParser())
        simulator.setUpdater(ShipmentUpdater())
        simulator.setViewHelper(TrackerViewHelper())
        
        // Add observer for real-time updates - only update UI when tracked shipments are updated
        simulator.addObserver(object : ShipmentObserver {
            override fun onShipmentUpdate(shipmentId: String, update: ShippingUpdate) {
                // Only refresh if this specific shipment is being tracked
                if (_trackedShipments.containsKey(shipmentId)) {
                    println("Refreshing UI for tracked shipment update: $shipmentId -> ${update.newStatus}")
                    // Use coroutine to update UI on main thread
                    viewModelScope.launch {
                        refreshTrackedShipments()
                    }
                }
            }
        })
        
        // Load shipments from test.txt file
        loadShipmentsFromFile()
    }
    
    /**
     * Loads shipments from the test.txt file line by line with 1-second delays
     */
    private fun loadShipmentsFromFile() {
        viewModelScope.launch {
            try {
                // Read all lines from the file first
                val parser = ShipmentDataParser()
                val lines = parser.readFile("test.txt")
                
                if (lines.isEmpty()) {
                    println("No data found in test.txt, falling back to sample data")
                    createSampleShipments()
                    return@launch
                }
                
                println("Starting live file processing - ${lines.size} lines to process")
                
                // Process each line with a 1-second delay
                for ((index, line) in lines.withIndex()) {
                    if (line.isNotBlank()) {
                        try {
                            simulator.processFileUpdate(line)
                            println("Processed line ${index + 1}/${lines.size}: $line")
                        } catch (e: Exception) {
                            println("Error processing line ${index + 1}: $line - ${e.message}")
                        }
                    }
                    
                    // Wait 1 second before processing the next line
                    if (index < lines.size - 1) {
                        delay(1000)
                    }
                }
                
                println("Live file processing completed")
                
            } catch (e: Exception) {
                println("Error loading shipments from test.txt: ${e.message}")
                // Fall back to creating sample shipments if file loading fails
                createSampleShipments()
            }
        }
    }
    
    /**
     * Creates sample shipments for demonstration (fallback)
     */
    private fun createSampleShipments() {
        val sampleShipments = listOf(
            Shipment(
                status = "Created",
                id = "SHIP001",
                expectedDeliveryDate = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000),
                currentLocation = "Warehouse A"
            ),
            Shipment(
                status = "Shipped",
                id = "SHIP002", 
                expectedDeliveryDate = System.currentTimeMillis() + (5 * 24 * 60 * 60 * 1000),
                currentLocation = "Distribution Center"
            ),
            Shipment(
                status = "In Transit",
                id = "SHIP003",
                expectedDeliveryDate = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000),
                currentLocation = "Highway 101"
            )
        )
        
        sampleShipments.forEach { shipment ->
            simulator.addShipment(shipment)
        }
    }
    
    /**
     * Refreshes all tracked shipments and triggers UI update
     */
    private fun refreshTrackedShipments() {
        // Force UI update by creating a completely new map with fresh shipment data
        val newTrackedShipments = mutableMapOf<String, Shipment>()
        
        _trackedShipments.keys.forEach { shipmentId ->
            val freshShipment = simulator.findShipment(shipmentId)
            if (freshShipment != null) {
                // Create a completely new Shipment instance with current data
                val updatedShipment = Shipment(
                    status = freshShipment.status,
                    id = freshShipment.id,
                    expectedDeliveryDate = freshShipment.expectedDeliveryDate,
                    currentLocation = freshShipment.currentLocation,
                    notes = freshShipment.notes.toMutableList(),
                    updateHistory = freshShipment.updateHistory.toMutableList()
                )
                newTrackedShipments[shipmentId] = updatedShipment
            }
        }
        
        // Force state update with completely new map
        _trackedShipments = newTrackedShipments.toMap()
        
        // Force UI recomposition by incrementing version
        _uiVersion++
        
        println("UI force-refreshed for ${newTrackedShipments.size} tracked shipments (version: $_uiVersion)")
    }
    
    /**
     * Toggles tracking for a shipment ID - starts tracking if not tracked, stops if already tracked
     */
    fun toggleTracking(shipmentId: String) {
        val trimmedId = shipmentId.trim()
        if (trimmedId.isBlank()) {
            _errorMessage = "Please enter a shipment ID"
            return
        }
        
        if (_trackedShipments.containsKey(trimmedId)) {
            // Stop tracking if already being tracked
            println("Stopping tracking for shipment: $trimmedId")
            stopTracking(trimmedId)
        } else {
            // Start tracking if not currently tracked
            println("Starting tracking for shipment: $trimmedId")
            startTracking(trimmedId)
        }
    }
    
    /**
     * Starts tracking a shipment
     */
    private fun startTracking(shipmentId: String) {
        viewModelScope.launch {
            _isLoading = true
            _errorMessage = null
            
            val shipment = simulator.findShipment(shipmentId)
            if (shipment == null) {
                _errorMessage = "Shipment ID '$shipmentId' not found"
                _isLoading = false
                notifyIfShipmentDoesntExist()
                return@launch
            }
            
            // Create a new shipment instance for tracking
            val trackingShipment = Shipment(
                status = shipment.status,
                id = shipment.id,
                expectedDeliveryDate = shipment.expectedDeliveryDate,
                currentLocation = shipment.currentLocation,
                notes = shipment.notes.toMutableList(),
                updateHistory = shipment.updateHistory.toMutableList()
            )
            
            // Add to tracked shipments
            _trackedShipments = _trackedShipments + (shipmentId to trackingShipment)
            
            // Force UI update
            _uiVersion++
            
            println("Now tracking shipment: $shipmentId")
            
            _isLoading = false
        }
    }
    
    /**
     * Stops tracking a shipment
     */
    fun stopTracking(shipmentId: String) {
        viewModelScope.launch {
            // Remove from tracked shipments
            _trackedShipments = _trackedShipments - shipmentId
            
            // Force UI update
            _uiVersion++
            
            println("Stopped tracking shipment: $shipmentId")
        }
    }
    
    /**
     * Clears error message
     */
    fun clearError() {
        viewModelScope.launch {
            _errorMessage = null
        }
    }
    
    /**
     * Formats timestamp to readable date
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Cleans up resources
     */
    fun cleanup() {
        viewModelScope.cancel()
    }
    
    override fun notifyIfShipmentDoesntExist() {
        // UI notification is handled through the error message state
        println("UI Notification: Shipment doesn't exist")
    }
} 