package org.example.project

data class Shipment(
    var status: String,
    val id: String,
    val notes: MutableList<String> = mutableListOf(),
    val updateHistory: MutableList<ShippingUpdate> = mutableListOf(),
    val expectedDeliveryDate: Long,
    var currentLocation: String
) : ShipmentSubject {
    
    private val observers: MutableList<ShipmentObserver> = mutableListOf()
    
    override fun addObserver(observer: ShipmentObserver) {
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }
    
    override fun removeObserver(observer: ShipmentObserver) {
        observers.remove(observer)
    }
    
    override fun notifyObservers(shipment: Shipment, update: ShippingUpdate) {
        observers.forEach { observer ->
            observer.onShipmentUpdate(shipment.id, update)
        }
    }
    
    fun addNote(note: String) {
        notes.add(note)
    }
    
    fun addUpdate(update: ShippingUpdate) {
        updateHistory.add(update)
        status = update.newStatus
        notifyObservers(this, update)
    }
    
    fun updateLocation(location: String) {
        currentLocation = location
    }
} 