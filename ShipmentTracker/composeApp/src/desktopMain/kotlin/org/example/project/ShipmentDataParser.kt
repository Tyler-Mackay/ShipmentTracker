package org.example.project

import java.io.File
import java.io.IOException

/**
 * Handles parsing of shipment data from files and update strings
 */
class ShipmentDataParser {
    /**
     * Reads a file and returns its contents as an array of strings
     * @param filePath The path to the file to read
     * @return Array of strings representing the file contents
     */
    fun readFile(filePath: String): Array<String> {
        return try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.readLines().toTypedArray()
            } else {
                println("Warning: File not found or is not a valid file: $filePath")
                arrayOf()
            }
        } catch (e: IOException) {
            println("Error reading file $filePath: ${e.message}")
            arrayOf()
        } catch (e: Exception) {
            println("Unexpected error reading file $filePath: ${e.message}")
            arrayOf()
        }
    }
    
    /**
     * Parses an update string into its components
     * Expected format from test.txt: "UPDATE_TYPE,SHIPMENT_ID,TIMESTAMP,[ADDITIONAL_DATA]"
     * Returns components in order: [SHIPMENT_ID, UPDATE_TYPE, TIMESTAMP, LOCATION, NOTES]
     * @param update The update string to parse
     * @return Array of parsed components reordered to match expected format
     */
    fun parseUpdate(update: String): Array<String> {
        return try {
            if (update.isBlank()) {
                println("Warning: Empty update string provided")
                return arrayOf()
            }
            
            // Split by comma and trim whitespace
            val components = update.split(",").map { it.trim() }
            
            // Validate minimum required components (UPDATE_TYPE and SHIPMENT_ID)
            if (components.size < 2) {
                println("Warning: Invalid update format. Expected at least UPDATE_TYPE and SHIPMENT_ID: $update")
                return arrayOf()
            }
            
            // Extract components from test.txt format
            val updateType = components[0]
            val shipmentId = components[1]
            val createdTimestamp = components.getOrNull(2) ?: ""
            val additionalData = components.getOrNull(3) ?: ""
            
            // Convert updateType to match the processor strategy names
            val normalizedUpdateType = normalizeUpdateType(updateType)
            
            // Determine the correct timestamp based on update type
            val timestamp = if ((updateType.lowercase() == "shipped" || updateType.lowercase() == "delayed") && additionalData.isNotBlank()) {
                additionalData // For shipped and delayed updates, use the 4th component as timestamp
            } else {
                createdTimestamp // For other updates, use the 3rd component
            }
            
            // Reorder to match expected format: [SHIPMENT_ID, UPDATE_TYPE, TIMESTAMP, LOCATION, NOTES]
            val location = if (updateType.lowercase() == "location") additionalData else ""
            val notes = if (updateType.lowercase() == "noteadded") additionalData else ""
            
            arrayOf(shipmentId, normalizedUpdateType, timestamp, location, notes)
        } catch (e: Exception) {
            println("Error parsing update string '$update': ${e.message}")
            arrayOf()
        }
    }
    
    /**
     * Normalizes update type from test.txt format to match processor strategy names
     * @param updateType The update type from test.txt
     * @return Normalized update type
     */
    private fun normalizeUpdateType(updateType: String): String {
        return when (updateType.lowercase()) {
            "created" -> "Create"
            "shipped" -> "Shipped"
            "location" -> "Location"
            "delivered" -> "Delivered"
            "delayed" -> "Delayed"
            "lost" -> "Lost"
            "canceled" -> "Cancelled"  // Handle spelling difference
            "noteadded" -> "NoteAdded"
            else -> updateType.replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * Validates if an update string has the correct format
     * @param update The update string to validate
     * @return True if the format is valid, false otherwise
     */
    fun isValidUpdateFormat(update: String): Boolean {
        if (update.isBlank()) return false
        
        val components = update.split(",")
        return components.size >= 2 && components[0].isNotBlank() && components[1].isNotBlank()
    }
    
    /**
     * Parses a timestamp string to Long
     * @param timestampStr The timestamp string to parse
     * @return The parsed timestamp as Long, or current time if parsing fails
     */
    fun parseTimestamp(timestampStr: String): Long {
        return try {
            if (timestampStr.isBlank()) {
                System.currentTimeMillis()
            } else {
                timestampStr.toLong()
            }
        } catch (e: NumberFormatException) {
            println("Warning: Invalid timestamp format '$timestampStr', using current time")
            System.currentTimeMillis()
        }
    }
} 