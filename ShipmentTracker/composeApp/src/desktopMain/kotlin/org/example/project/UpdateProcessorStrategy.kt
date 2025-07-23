package org.example.project

interface UpdateProcessorStrategy {
    fun processUpdate(shipment: Shipment, timestamp: Long): ShippingUpdate
} 