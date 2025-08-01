package org.example.project.ShippingUpdate

import org.example.project.Shipment.Shipment

class ShippedUpdateStrategy : UpdateProcessorStrategy {
    override fun processUpdate(shipment: Shipment, timestamp: Long): ShippingUpdate {
        val previousStatus = shipment.status
        val newStatus = "Shipped"
        
        return ShippingUpdate(
            previousStatus = previousStatus,
            newStatus = newStatus,
            timestamp = timestamp
        )
    }
} 