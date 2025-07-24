package org.example.project.ShippingUpdate

import org.example.project.Shipment.Shipment

class LostUpdateStrategy : UpdateProcessorStrategy {
    override fun processUpdate(shipment: Shipment, timestamp: Long): ShippingUpdate {
        val previousStatus = shipment.status
        val newStatus = "Lost"
        
        return ShippingUpdate(
            previousStatus = previousStatus,
            newStatus = newStatus,
            timestamp = timestamp
        )
    }
} 