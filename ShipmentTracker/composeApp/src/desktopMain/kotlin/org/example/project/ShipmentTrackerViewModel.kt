package org.example.project

import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for managing shipment tracking state with coroutines
 */
class ShipmentTrackerViewModel : UI {
    private val simulator = TrackingSimulator()
    private var _trackedShipments = mapOf<String, Shipment>()
    val trackedShipments: Map<String, Shipment> get() = _trackedShipments
    
    private var _errorMessage: String? = null
    val errorMessage: String? get() = _errorMessage
    
    private var _isLoading = false
    val isLoading: Boolean get() = _isLoading
    
    private val updateJobs = mutableMapOf<String, Job>()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        // Initialize the simulator with dependencies
        simulator.setParser(ShipmentDataParser())
        simulator.setUpdater(ShipmentUpdater())
        simulator.setViewHelper(TrackerViewHelper())
        
        // Add observer for real-time updates
        simulator.addObserver(object : ShipmentObserver {
            override fun onShipmentUpdate(update: ShippingUpdate) {
                // Update the tracked shipments when changes occur
                refreshTrackedShipments()
            }
        })
        
        // Create some sample shipments for demonstration
        createSampleShipments()
    }
    
    /**
     * Creates sample shipments for demonstration
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
     * Toggles tracking for a shipment ID
     */
    fun toggleTracking(shipmentId: String) {
        val trimmedId = shipmentId.trim()
        if (trimmedId.isBlank()) {
            _errorMessage = "Please enter a shipment ID"
            return
        }
        
        if (_trackedShipments.containsKey(trimmedId)) {
            // Stop tracking
            stopTracking(trimmedId)
        } else {
            // Start tracking
            startTracking(trimmedId)
        }
    }
    
    /**
     * Starts tracking a shipment
     */
    private fun startTracking(shipmentId: String) {
        _isLoading = true
        _errorMessage = null
        
        val shipment = simulator.findShipment(shipmentId)
        if (shipment == null) {
            _errorMessage = "Shipment ID '$shipmentId' not found"
            _isLoading = false
            notifyIfShipmentDoesntExist()
            return
        }
        
        // Add to tracked shipments
        _trackedShipments = _trackedShipments + (shipmentId to shipment)
        
        // Start coroutine for real-time updates (max once per second)
        val updateJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // Update every second
                
                // Get fresh shipment data
                val freshShipment = simulator.findShipment(shipmentId)
                if (freshShipment != null) {
                    _trackedShipments = _trackedShipments + (shipmentId to freshShipment)
                }
            }
        }
        
        updateJobs[shipmentId] = updateJob
        _isLoading = false
    }
    
    /**
     * Stops tracking a shipment
     */
    fun stopTracking(shipmentId: String) {
        // Cancel the update job
        updateJobs[shipmentId]?.cancel()
        updateJobs.remove(shipmentId)
        
        // Remove from tracked shipments
        _trackedShipments = _trackedShipments - shipmentId
    }
    
    /**
     * Refreshes all tracked shipments
     */
    private fun refreshTrackedShipments() {
        val currentTracked = _trackedShipments
        val refreshedShipments = currentTracked.mapNotNull { (id, _) ->
            val freshShipment = simulator.findShipment(id)
            if (freshShipment != null) id to freshShipment else null
        }.toMap()
        
        _trackedShipments = refreshedShipments
    }
    
    /**
     * Clears error message
     */
    fun clearError() {
        _errorMessage = null
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
        updateJobs.values.forEach { it.cancel() }
        updateJobs.clear()
        viewModelScope.cancel()
    }
    
    override fun notifyIfShipmentDoesntExist() {
        // UI notification is handled through the error message state
        println("UI Notification: Shipment doesn't exist")
    }
} 