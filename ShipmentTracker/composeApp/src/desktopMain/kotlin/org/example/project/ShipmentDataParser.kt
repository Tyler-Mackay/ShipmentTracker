package org.example.project

import java.io.File
import java.io.IOException

class ShipmentDataParser {
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
    
    fun parseUpdate(update: String): Array<String> {
        return try {
            if (update.isBlank()) {
                println("Warning: Empty update string provided")
                return arrayOf()
            }
            
            val components = update.split(",").map { it.trim() }
            
            if (components.size < 2) {
                println("Warning: Invalid update format. Expected at least UPDATE_TYPE and SHIPMENT_ID: $update")
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
            println("Error parsing update string '$update': ${e.message}")
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
            println("Warning: Invalid timestamp format '$timestampStr', using current time")
            System.currentTimeMillis()
        }
    }
} 