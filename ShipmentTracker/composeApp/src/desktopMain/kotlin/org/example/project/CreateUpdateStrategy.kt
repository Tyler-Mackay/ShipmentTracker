package org.example.project

/**
 * Strategy for processing shipment creation updates
 */
class CreateUpdateStrategy : UpdateProcessorStrategy {
    override fun processUpdate(shipment: Shipment, timestamp: Long): ShippingUpdate {
        val previousStatus = "" // No previous status for new shipments
        val newStatus = "Created"
        
        return ShippingUpdate(
            previousStatus = previousStatus,
            newStatus = newStatus,
            timestamp = timestamp
        )
    }
} 