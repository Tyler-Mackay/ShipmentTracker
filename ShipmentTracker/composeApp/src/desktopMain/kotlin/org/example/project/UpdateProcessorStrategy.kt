package org.example.project

/**
 * Strategy interface for processing different types of shipment updates
 */
interface UpdateProcessorStrategy {
    /**
     * Processes an update for a shipment
     * @param shipment The shipment to update
     * @param timestamp The timestamp of the update
     * @return A ShippingUpdate object representing the change
     */
    fun processUpdate(shipment: Shipment, timestamp: Long): ShippingUpdate
} 