package org.example.project

/**
 * Represents a shipment with all its tracking information
 */
data class Shipment(
    var status: String,
    val id: String,
    val notes: MutableList<String> = mutableListOf(),
    val updateHistory: MutableList<ShippingUpdate> = mutableListOf(),
    val expectedDeliveryDate: Long,
    var currentLocation: String
) {
    /**
     * Adds a note to the shipment
     */
    fun addNote(note: String) {
        notes.add(note)
    }
    
    /**
     * Adds an update to the shipment history and updates the status
     */
    fun addUpdate(update: ShippingUpdate) {
        updateHistory.add(update)
        // Update the current status to the new status from the update
        status = update.newStatus
    }
    
    /**
     * Updates the current location of the shipment
     */
    fun updateLocation(location: String) {
        currentLocation = location
    }
    
    /**
     * Gets the latest update from the history
     */
    fun getLatestUpdate(): ShippingUpdate? {
        return updateHistory.lastOrNull()
    }
    
    /**
     * Gets all updates of a specific type
     */
    fun getUpdatesByType(statusType: String): List<ShippingUpdate> {
        return updateHistory.filter { it.newStatus == statusType }
    }
    
    /**
     * Checks if the shipment has a specific status
     */
    fun hasStatus(statusType: String): Boolean {
        return status == statusType
    }
} 