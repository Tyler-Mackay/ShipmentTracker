package org.example.project.Shipment

import org.example.project.ShippingUpdate.ShippingUpdate

/**
 * Abstract base class for all shipment types
 * Based on UML design with different shipment types and abnormality rules
 */
abstract class Shipment(
    var status: String,
    val id: String,
    val shipmentType: ShipmentType,
    val creationDate: Long,
    val notes: MutableList<String> = mutableListOf(),
    val updateHistory: MutableList<ShippingUpdate> = mutableListOf(),
    val expectedDeliveryDateTimestamp: Long,
    var currentLocation: String = "",
    var isAbnormal: Boolean = false,
    var abnormality: String = ""
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
    
    /**
     * Validate delivery date based on shipment type rules
     * Returns ValidationResult indicating if delivery timing is acceptable
     */
    abstract fun validateDeliveryDate(): ValidationResult
    
    fun markAbnormal(abnormality: String) {
        this.isAbnormal = true
        this.abnormality = abnormality
    }
    
    fun clearAbnormality() {
        this.isAbnormal = false
        this.abnormality = ""
    }
}

/**
 * Enumeration of shipment types
 */
enum class ShipmentType {
    STANDARD,
    EXPRESS, 
    OVERNIGHT,
    BULK
}

/**
 * Result of delivery date validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

/**
 * Standard shipment with no special delivery time constraints
 */
class StandardShipment(
    status: String,
    id: String,
    creationDate: Long,
    expectedDeliveryDateTimestamp: Long,
    currentLocation: String = ""
) : Shipment(status, id, ShipmentType.STANDARD, creationDate, mutableListOf(), mutableListOf(), expectedDeliveryDateTimestamp, currentLocation) {
    
    override fun validateDeliveryDate(): ValidationResult {
        // Standard shipments have no special timing constraints
        return ValidationResult(true, "Standard shipment delivery timing is acceptable")
    }
}

/**
 * Express shipment with <= 3 day delivery requirement
 */
class ExpressShipment(
    status: String,
    id: String,
    creationDate: Long,
    expectedDeliveryDateTimestamp: Long,
    currentLocation: String = ""
) : Shipment(status, id, ShipmentType.EXPRESS, creationDate, mutableListOf(), mutableListOf(), expectedDeliveryDateTimestamp, currentLocation) {
    
    override fun validateDeliveryDate(): ValidationResult {
        val deliveryTimeMs = expectedDeliveryDateTimestamp - creationDate
        val deliveryTimeDays = deliveryTimeMs / (1000 * 60 * 60 * 24)
        
        return if (deliveryTimeDays <= 3) {
            ValidationResult(true, "Express shipment delivery timing is acceptable (<= 3 days)")
        } else {
            markAbnormal("<= 3 day delivery requirement violated")
            ValidationResult(false, "Express shipment abnormality: delivery time exceeds 3 days")
        }
    }
}

/**
 * Overnight shipment with next day delivery requirement
 */
class OvernightShipment(
    status: String,
    id: String,
    creationDate: Long,
    expectedDeliveryDateTimestamp: Long,
    currentLocation: String = ""
) : Shipment(status, id, ShipmentType.OVERNIGHT, creationDate, mutableListOf(), mutableListOf(), expectedDeliveryDateTimestamp, currentLocation) {
    
    override fun validateDeliveryDate(): ValidationResult {
        val deliveryTimeMs = expectedDeliveryDateTimestamp - creationDate
        val deliveryTimeDays = deliveryTimeMs / (1000 * 60 * 60 * 24)
        
        return if (deliveryTimeDays <= 1) {
            ValidationResult(true, "Overnight shipment delivery timing is acceptable (next day)")
        } else {
            markAbnormal("next day delivery requirement violated")
            ValidationResult(false, "Overnight shipment abnormality: delivery time exceeds 1 day")
        }
    }
}

/**
 * Bulk shipment with > 3 day delivery allowance
 */
class BulkShipment(
    status: String,
    id: String,
    creationDate: Long,
    expectedDeliveryDateTimestamp: Long,
    currentLocation: String = ""
) : Shipment(status, id, ShipmentType.BULK, creationDate, mutableListOf(), mutableListOf(), expectedDeliveryDateTimestamp, currentLocation) {
    
    override fun validateDeliveryDate(): ValidationResult {
        val deliveryTimeMs = expectedDeliveryDateTimestamp - creationDate
        val deliveryTimeDays = deliveryTimeMs / (1000 * 60 * 60 * 24)
        
        return if (deliveryTimeDays > 3) {
            ValidationResult(true, "Bulk shipment delivery timing is acceptable (> 3 days)")
        } else {
            markAbnormal("> 3 day delivery requirement violated")
            ValidationResult(false, "Bulk shipment abnormality: delivery time is too fast (should be > 3 days)")
        }
    }
}

/**
 * Factory for creating shipments based on type
 */
object ShipmentFactory {
    fun createShipment(
        type: ShipmentType,
        status: String,
        id: String,
        creationDate: Long,
        expectedDeliveryDateTimestamp: Long,
        currentLocation: String = ""
    ): Shipment {
        return when (type) {
            ShipmentType.STANDARD -> StandardShipment(status, id, creationDate, expectedDeliveryDateTimestamp, currentLocation)
            ShipmentType.EXPRESS -> ExpressShipment(status, id, creationDate, expectedDeliveryDateTimestamp, currentLocation)
            ShipmentType.OVERNIGHT -> OvernightShipment(status, id, creationDate, expectedDeliveryDateTimestamp, currentLocation)
            ShipmentType.BULK -> BulkShipment(status, id, creationDate, expectedDeliveryDateTimestamp, currentLocation)
        }
    }
    
    fun parseShipmentType(typeString: String): ShipmentType {
        return when (typeString.lowercase()) {
            "standard" -> ShipmentType.STANDARD
            "express" -> ShipmentType.EXPRESS
            "overnight" -> ShipmentType.OVERNIGHT
            "bulk" -> ShipmentType.BULK
            else -> throw IllegalArgumentException("Unknown shipment type: $typeString")
        }
    }
} 