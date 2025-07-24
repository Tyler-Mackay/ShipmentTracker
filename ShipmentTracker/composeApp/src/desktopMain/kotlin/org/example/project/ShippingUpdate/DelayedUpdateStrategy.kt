package org.example.project.ShippingUpdate

import org.example.project.Shipment.Shipment

class DelayedUpdateStrategy : UpdateProcessorStrategy {
    override fun processUpdate(shipment: Shipment, timestamp: Long): ShippingUpdate {
        val previousStatus = shipment.status
        val newStatus = "Delayed"
        
        return ShippingUpdate(
            previousStatus = previousStatus,
            newStatus = newStatus,
            timestamp = timestamp
        )
    }
} 