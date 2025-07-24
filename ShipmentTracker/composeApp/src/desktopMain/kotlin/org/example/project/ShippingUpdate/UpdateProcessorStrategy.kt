package org.example.project.ShippingUpdate

import org.example.project.Shipment.Shipment

interface UpdateProcessorStrategy {
    fun processUpdate(shipment: Shipment, timestamp: Long): ShippingUpdate
} 