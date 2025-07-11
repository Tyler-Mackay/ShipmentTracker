package org.example.project

/**
 * Helper class for managing the UI state and tracking operations
 * This class provides state management for the shipment tracking UI
 */
class TrackerViewHelper {
    // Properties as defined in the UML diagram
    private val activeTrackingShipments: MutableList<String> = mutableListOf()
    
    // State properties for UI (will be converted to Compose state later)
    private var _shipmentIds = arrayOf<String>()
    val shipmentIds: Array<String> get() = _shipmentIds
    
    private var _shipmentTotals = arrayOf<String>()
    val shipmentTotals: Array<String> get() = _shipmentTotals
    
    private var _shipmentUpdateHistory = arrayOf<String>()
    val shipmentUpdateHistory: Array<String> get() = _shipmentUpdateHistory
    
    private var _expectedShipmentDeliveryDate = arrayOf<String>()
    val expectedShipmentDeliveryDate: Array<String> get() = _expectedShipmentDeliveryDate
    
    private var _shipmentStatus = arrayOf<String>()
    val shipmentStatus: Array<String> get() = _shipmentStatus
    
    /**
     * Starts tracking a shipment by its ID
     * @param id The ID of the shipment to track
     */
    fun trackShipment(id: String) {
        if (!activeTrackingShipments.contains(id)) {
            activeTrackingShipments.add(id)
            updateShipmentIds()
        }
    }
    
    /**
     * Stops tracking all shipments
     */
    fun stopTracking() {
        activeTrackingShipments.clear()
        updateShipmentIds()
        clearAllStates()
    }
    
    /**
     * Stops tracking a specific shipment
     * @param id The ID of the shipment to stop tracking
     */
    fun stopTrackingShipment(id: String) {
        activeTrackingShipments.remove(id)
        updateShipmentIds()
    }
    
    /**
     * Updates the shipment totals state
     * @param totals Array of shipment totals to display
     */
    fun updateShipmentTotals(totals: Array<String>) {
        _shipmentTotals = totals
    }
    
    /**
     * Updates the shipment update history state
     * @param history Array of update history entries
     */
    fun updateShipmentUpdateHistory(history: Array<String>) {
        _shipmentUpdateHistory = history
    }
    
    /**
     * Updates the expected delivery dates state
     * @param dates Array of expected delivery dates
     */
    fun updateExpectedDeliveryDates(dates: Array<String>) {
        _expectedShipmentDeliveryDate = dates
    }
    
    /**
     * Updates the shipment status state
     * @param statuses Array of shipment statuses
     */
    fun updateShipmentStatus(statuses: Array<String>) {
        _shipmentStatus = statuses
    }
    
    /**
     * Gets the list of actively tracked shipment IDs
     * @return List of shipment IDs being tracked
     */
    fun getActiveTrackingShipments(): List<String> {
        return activeTrackingShipments.toList()
    }
    
    /**
     * Checks if a shipment is being tracked
     * @param id The shipment ID to check
     * @return True if the shipment is being tracked, false otherwise
     */
    fun isTrackingShipment(id: String): Boolean {
        return activeTrackingShipments.contains(id)
    }
    
    /**
     * Updates the shipment IDs state with currently tracked shipments
     */
    private fun updateShipmentIds() {
        _shipmentIds = activeTrackingShipments.toTypedArray()
    }
    
    /**
     * Clears all state arrays
     */
    private fun clearAllStates() {
        _shipmentIds = arrayOf()
        _shipmentTotals = arrayOf()
        _shipmentUpdateHistory = arrayOf()
        _expectedShipmentDeliveryDate = arrayOf()
        _shipmentStatus = arrayOf()
    }
} 