package org.example.project.ShippingUpdate

import org.example.project.Shipment.Shipment

class NoteAddedUpdateStrategy : UpdateProcessorStrategy {
    override fun processUpdate(shipment: Shipment, timestamp: Long): ShippingUpdate {
        val previousStatus = shipment.status
        val newStatus = shipment.status // Preserve current status when adding notes
        
        return ShippingUpdate(
            previousStatus = previousStatus,
            newStatus = newStatus,
            timestamp = timestamp
        )
    }
} 