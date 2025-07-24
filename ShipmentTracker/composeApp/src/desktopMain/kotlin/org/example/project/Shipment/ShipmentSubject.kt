package org.example.project.Shipment

import org.example.project.ShippingUpdate.ShippingUpdate

interface ShipmentSubject {
    fun addObserver(observer: ShipmentObserver)
    fun removeObserver(observer: ShipmentObserver)
    fun notifyObservers(shipment: Shipment, update: ShippingUpdate)
} 