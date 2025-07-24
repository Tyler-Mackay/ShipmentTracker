package org.example.project.Shipment

import java.io.File
import java.io.IOException

class ShipmentDataParser {
    fun readFile(filePath: String): Array<String> {
        return try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.readLines().toTypedArray()
            } else {
                arrayOf()
            }
        } catch (e: IOException) {
            arrayOf()
        } catch (e: Exception) {
            arrayOf()
        }
    }
    
    fun parseUpdate(update: String): Array<String> {
        return try {
            if (update.isBlank()) {
                return arrayOf()
            }
            
            val components = update.split(",").map { it.trim() }
            
            if (components.size < 2) {
                return arrayOf()
            }
            
            val updateType = components[0]
            val shipmentId = components[1]
            val createdTimestamp = components.getOrNull(2) ?: ""
            val additionalData = components.getOrNull(3) ?: ""
            
            val normalizedUpdateType = normalizeUpdateType(updateType)
            
            val timestamp = if ((updateType.lowercase() == "shipped" || updateType.lowercase() == "delayed") && additionalData.isNotBlank()) {
                additionalData
            } else {
                createdTimestamp
            }
            
            val location = if (updateType.lowercase() == "location") additionalData else ""
            val notes = if (updateType.lowercase() == "noteadded") additionalData else ""
            
            arrayOf(shipmentId, normalizedUpdateType, timestamp, location, notes)
        } catch (e: Exception) {
            arrayOf()
        }
    }
    
    private fun normalizeUpdateType(updateType: String): String {
        return when (updateType.lowercase()) {
            "created" -> "Create"
            "shipped" -> "Shipped"
            "location" -> "Location"
            "delivered" -> "Delivered"
            "delayed" -> "Delayed"
            "lost" -> "Lost"
            "canceled" -> "Cancelled"
            "noteadded" -> "NoteAdded"
            else -> updateType.replaceFirstChar { it.uppercase() }
        }
    }
    
    fun isValidUpdateFormat(update: String): Boolean {
        if (update.isBlank()) return false
        
        val components = update.split(",")
        return components.size >= 2 && components[0].isNotBlank() && components[1].isNotBlank()
    }
    
    fun parseTimestamp(timestampStr: String): Long {
        return try {
            if (timestampStr.isBlank()) {
                System.currentTimeMillis()
            } else {
                timestampStr.toLong()
            }
        } catch (e: NumberFormatException) {
            System.currentTimeMillis()
        }
    }
    
    /**
     * Parse created shipment data from string format: created,shipmentId,shipmentType,timestamp
     */
    fun parseCreatedShipment(data: String): CreatedShipmentData {
        val parts = data.split(",").map { it.trim() }
        
        if (parts.size < 4) {
            throw IllegalArgumentException("Invalid created shipment format. Expected: created,shipmentId,shipmentType,timestamp")
        }
        
        val eventType = parts[0]
        val shipmentId = parts[1]
        val shipmentTypeStr = parts[2]
        val timestampStr = parts[3]
        
        if (eventType.lowercase() != "created") {
            throw IllegalArgumentException("Expected 'created' event type, got: $eventType")
        }
        
        val shipmentType = ShipmentFactory.parseShipmentType(shipmentTypeStr)
        val timestamp = parseTimestamp(timestampStr)
        
        return CreatedShipmentData(shipmentId, shipmentType, timestamp)
    }
    
    /**
     * Parse update data from string format
     */
    fun parseUpdateData(data: String): UpdateData {
        val parts = data.split(",").map { it.trim() }
        
        if (parts.size < 3) {
            throw IllegalArgumentException("Invalid update format. Expected at least: updateType,shipmentId,timestamp")
        }
        
        val updateType = normalizeUpdateType(parts[0])
        val shipmentId = parts[1]
        val timestamp = parseTimestamp(parts[2])
        val additionalData = if (parts.size > 3) parts.drop(3).joinToString(",") else null
        
        return UpdateData(updateType, shipmentId, timestamp, additionalData)
    }
}

/**
 * Data class for parsed created shipment information
 */
data class CreatedShipmentData(
    val id: String,
    val shipmentType: ShipmentType,
    val timestamp: Long
)

/**
 * Data class for parsed update information
 */
data class UpdateData(
    val updateType: String,
    val shipmentId: String,
    val timestamp: Long,
    val additionalData: String?
) 