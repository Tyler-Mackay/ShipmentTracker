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
     * Expected format: "SHIPMENT_ID,UPDATE_TYPE,TIMESTAMP,LOCATION,NOTES"
     * @param update The update string to parse
     * @return Array of parsed components from the update string
     */
    fun parseUpdate(update: String): Array<String> {
        return try {
            if (update.isBlank()) {
                println("Warning: Empty update string provided")
                return arrayOf()
            }
            
            // Split by comma and trim whitespace
            val components = update.split(",").map { it.trim() }
            
            // Validate minimum required components (ID and UPDATE_TYPE)
            if (components.size < 2) {
                println("Warning: Invalid update format. Expected at least ID and UPDATE_TYPE: $update")
                return arrayOf()
            }
            
            // Ensure we have at least 5 components, filling with empty strings if needed
            val paddedComponents = components.toMutableList()
            while (paddedComponents.size < 5) {
                paddedComponents.add("")
            }
            
            paddedComponents.toTypedArray()
        } catch (e: Exception) {
            println("Error parsing update string '$update': ${e.message}")
            arrayOf()
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