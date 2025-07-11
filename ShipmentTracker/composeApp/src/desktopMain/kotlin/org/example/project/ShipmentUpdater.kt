package org.example.project

/**
 * Handles updating shipments using different update processor strategies
 */
class ShipmentUpdater {
    // Map of update types to their corresponding processor strategies
    private val processors: MutableMap<String, UpdateProcessorStrategy> = mutableMapOf()
    
    init {
        // Initialize the processors map with all the available strategies
        processors["Create"] = CreateUpdateStrategy()
        processors["Shipped"] = ShippedUpdateStrategy()
        processors["Location"] = LocationUpdateStrategy()
        processors["Delivered"] = DeliveredUpdateStrategy()
        processors["Delayed"] = DelayedUpdateStrategy()
        processors["Lost"] = LostUpdateStrategy()
        processors["Cancelled"] = CancelledUpdateStrategy()
        processors["NoteAdded"] = NoteAddedUpdateStrategy()
    }
    
    /**
     * Processes an update for a shipment using the appropriate strategy
     * @param updateType The type of update to process
     * @param shipment The shipment to update
     * @param timestamp The timestamp of the update
     * @return A ShippingUpdate object representing the processed update
     */
    fun processUpdate(updateType: String, shipment: Shipment, timestamp: Long): ShippingUpdate {
        val processor = processors[updateType]
            ?: throw IllegalArgumentException("Unknown update type: $updateType")
        
        return processor.processUpdate(shipment, timestamp)
    }
    
    /**
     * Adds a custom processor strategy for a specific update type
     * @param updateType The type of update
     * @param processor The processor strategy to use
     */
    fun addProcessor(updateType: String, processor: UpdateProcessorStrategy) {
        processors[updateType] = processor
    }
    
    /**
     * Gets all available update types
     * @return A set of all available update types
     */
    fun getAvailableUpdateTypes(): Set<String> {
        return processors.keys
    }
    
    /**
     * Checks if a processor exists for the given update type
     * @param updateType The update type to check
     * @return True if a processor exists, false otherwise
     */
    fun hasProcessor(updateType: String): Boolean {
        return processors.containsKey(updateType)
    }
} 