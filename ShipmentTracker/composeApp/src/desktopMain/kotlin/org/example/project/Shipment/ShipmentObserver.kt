package org.example.project.Shipment

import org.example.project.ShippingUpdate.ShippingUpdate

interface ShipmentObserver {
    fun onShipmentUpdate(shipmentId: String, update: ShippingUpdate)
} 