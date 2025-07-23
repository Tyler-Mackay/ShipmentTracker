package org.example.project

interface ShipmentSubject {
    fun addObserver(observer: ShipmentObserver)
    fun removeObserver(observer: ShipmentObserver)
    fun notifyObservers(shipment: Shipment, update: ShippingUpdate)
} 