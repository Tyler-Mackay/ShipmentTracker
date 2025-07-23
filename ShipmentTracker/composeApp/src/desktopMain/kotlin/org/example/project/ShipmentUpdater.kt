package org.example.project

class ShipmentUpdater {
    private val processors: MutableMap<String, UpdateProcessorStrategy> = mutableMapOf()
    
    init {
        processors["Create"] = CreateUpdateStrategy()
        processors["Shipped"] = ShippedUpdateStrategy()
        processors["Location"] = LocationUpdateStrategy()
        processors["Delivered"] = DeliveredUpdateStrategy()
        processors["Delayed"] = DelayedUpdateStrategy()
        processors["Lost"] = LostUpdateStrategy()
        processors["Cancelled"] = CancelledUpdateStrategy()
        processors["NoteAdded"] = NoteAddedUpdateStrategy()
    }
    
    fun processUpdate(updateType: String, shipment: Shipment, timestamp: Long): ShippingUpdate {
        val processor = processors[updateType]
            ?: throw IllegalArgumentException("Unknown update type: $updateType")
        
        return processor.processUpdate(shipment, timestamp)
    }
} 