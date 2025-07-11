package org.example.project

/**
 * UI interface for handling user interface operations
 * This interface defines the contract for UI components to notify about shipment status
 */
interface UI {
    /**
     * Notifies the UI if a shipment doesn't exist
     * This method should be called when a user tries to access a non-existent shipment
     */
    fun notifyIfShipmentDoesntExist()
} 