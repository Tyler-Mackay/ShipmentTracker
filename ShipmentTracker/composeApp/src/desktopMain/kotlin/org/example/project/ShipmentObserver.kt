package org.example.project

interface ShipmentObserver {
    fun onShipmentUpdate(shipmentId: String, update: ShippingUpdate)
} 