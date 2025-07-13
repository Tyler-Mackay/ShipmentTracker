package org.example.project

/**
 * Observer interface for receiving shipment update notifications
 * Implementations of this interface will be notified when shipments are updated
 */
interface ShipmentObserver {
    /**
     * Called when a shipment update occurs
     * @param shipmentId The ID of the shipment that was updated
     * @param update The shipping update that occurred
     */
    fun onShipmentUpdate(shipmentId: String, update: ShippingUpdate)
} 