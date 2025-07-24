package org.example.project

/**
 * TrackingClient - Handles client-side logic for shipment tracking
 * Based on UML design, this class manages communication with tracking server
 * and processes user input from the web interface
 */
class TrackingClient {
    private var serverConnection: ServerConnection? = null
    
    /**
     * Initialize connection to the tracking server
     */
    fun initializeConnection(serverUrl: String, port: Int): Boolean {
        return try {
            serverConnection = ServerConnection(serverUrl, port)
            serverConnection?.connect() ?: false
        } catch (e: Exception) {
            println("Failed to connect to server: ${e.message}")
            false
        }
    }
    
    /**
     * Send create request to server
     * @param data The shipment data to create
     * @return Boolean indicating success
     */
    fun sendCreateRequest(data: String): Boolean {
        return try {
            serverConnection?.sendCreateRequest(data) ?: false
        } catch (e: Exception) {
            println("Failed to send create request: ${e.message}")
            false
        }
    }
    
    /**
     * Send update request to server  
     * @param data The update data
     * @return Boolean indicating success
     */
    fun sendUpdateRequest(data: String): Boolean {
        return try {
            serverConnection?.sendUpdateRequest(data) ?: false
        } catch (e: Exception) {
            println("Failed to send update request: ${e.message}")
            false
        }
    }
    
    /**
     * Parse user input from the web interface into a structured request
     * @param input Raw input string from web textbox (comma-separated simulation format)
     * @return ClientRequest object
     */
    fun parseUserInput(input: String): ClientRequest {
        val trimmedInput = input.trim()
        
        // Check if input is in comma-separated simulation format
        if (trimmedInput.contains(",")) {
            return parseSimulationLine(trimmedInput)
        }
        
        // Parse the input string to determine request type and extract data (legacy format)
        return when {
            input.startsWith("CREATE:") -> {
                val data = input.removePrefix("CREATE:").trim()
                CreateShipmentRequest(data)
            }
            input.startsWith("UPDATE:") -> {
                val data = input.removePrefix("UPDATE:").trim()
                UpdateShipmentRequest(data)
            }
            input.startsWith("TRACK:") -> {
                val data = input.removePrefix("TRACK:").trim()
                TrackShipmentRequest(data)
            }
            else -> {
                // Default to tracking if no prefix specified
                TrackShipmentRequest(input.trim())
            }
        }
    }
    
    /**
     * Parse comma-separated simulation line 
     * Format for created: eventType,shipmentId,shipmentType,timestamp
     * Format for others: eventType,shipmentId,timestamp[,additionalData]
     * @param line Comma-separated line from simulation format
     * @return ClientRequest object
     */
    private fun parseSimulationLine(line: String): ClientRequest {
        val parts = line.split(",")
        
        if (parts.size < 3) {
            throw IllegalArgumentException("Invalid simulation format")
        }
        
        val eventType = parts[0].trim().lowercase()
        val shipmentId = parts[1].trim()
        
        // Handle different formats based on event type
        return when (eventType) {
            "created" -> {
                // Format: created,shipmentId,shipmentType,timestamp
                if (parts.size < 4) {
                    throw IllegalArgumentException("Created events require format: created,shipmentId,shipmentType,timestamp")
                }
                val shipmentType = parts[2].trim()
                val timestamp = parts[3].trim()
                CreateShipmentRequest(formatCreatedShipmentData(eventType, shipmentId, shipmentType, timestamp))
            }
            "shipped", "delayed", "location", "noteadded", "lost", "canceled", "delivered" -> {
                // Format: eventType,shipmentId,timestamp[,additionalData]
                val timestamp = parts[2].trim()
                val additionalData = if (parts.size > 3) parts.drop(3).joinToString(",").trim() else null
                UpdateShipmentRequest(formatShipmentData(eventType, shipmentId, timestamp, additionalData))
            }
            else -> TrackShipmentRequest(shipmentId)
        }
    }
    
    /**
     * Format shipment data for processing
     */
    private fun formatShipmentData(eventType: String, shipmentId: String, timestamp: String, additionalData: String?): String {
        return buildString {
            append("$eventType,$shipmentId,$timestamp")
            if (additionalData != null) {
                append(",$additionalData")
            }
        }
    }
    
    /**
     * Format created shipment data with shipment type
     */
    private fun formatCreatedShipmentData(eventType: String, shipmentId: String, shipmentType: String, timestamp: String): String {
        return "$eventType,$shipmentId,$shipmentType,$timestamp"
    }
    
    /**
     * Display response from server to the web interface
     * @param response Server response to display
     */
    fun displayResponse(response: ServerResponse) {
        // This would send the response back to the web interface
        // For now, we'll print it (later we'll integrate with web API)
        println("Server Response: ${response.message}")
        println("Status: ${response.status}")
        println("Data: ${response.data}")
    }
    
    /**
     * Process input from web interface
     * This is the main entry point called by the web server
     */
    fun processWebInput(input: String): String {
        return try {
            val request = parseUserInput(input)
            
            when (request) {
                is CreateShipmentRequest -> {
                    val success = sendCreateRequest(request.data)
                    if (success) {
                        val parts = request.data.split(",")
                        if (parts.size >= 4) {
                            val shipmentId = parts[1]
                            val shipmentType = parts[2].uppercase()
                            val timestamp = parts[3]
                            "✅ $shipmentType shipment $shipmentId created successfully at ${formatTimestamp(timestamp)}"
                        } else {
                            "✅ Create request sent successfully"
                        }
                    } else "❌ Failed to send create request"
                }
                is UpdateShipmentRequest -> {
                    val success = sendUpdateRequest(request.data)
                    if (success) {
                        val parts = request.data.split(",")
                                                if (parts.size >= 3) {
                            val eventType = parts[0].replaceFirstChar { it.uppercase() }
                            val shipmentId = parts[1]
                            val additionalInfo = if (parts.size > 3) " - ${parts.drop(3).joinToString(",")}" else ""
                            "✅ $eventType update for shipment $shipmentId processed$additionalInfo"
                        } else {
                            "✅ Update request sent successfully"
                        }
                    } else "❌ Failed to send update request"
                }
                is TrackShipmentRequest -> {
                    val success = sendCreateRequest("TRACK:${request.data}")
                    if (success) "🔍 Tracking request sent for: ${request.data}"
                    else "❌ Failed to send tracking request"
                }
                else -> "❓ Unknown request type processed"
            }
        } catch (e: Exception) {
            "❌ Error processing request: ${e.message}"
        }
    }
    
    /**
     * Alternative simple communication using file system
     */
    fun processInputSimple(input: String): String {
        try {
            println("DEBUG: Processing input with simple file method: $input")
            
            // Parse the input
            val parsedRequest = parseUserInput(input)
            println("DEBUG: Parsed request type: ${parsedRequest.type}")
            
            when (parsedRequest) {
                is CreateShipmentRequest -> {
                    // Write request to file
                    val requestFile = java.io.File("client_request.txt")
                    requestFile.writeText("CREATE:${parsedRequest.data}")
                    
                    // Wait for response file
                    val responseFile = java.io.File("server_response.txt")
                    var attempts = 0
                    while (!responseFile.exists() && attempts < 50) {
                        Thread.sleep(100)  // Wait 100ms
                        attempts++
                    }
                    
                    return if (responseFile.exists()) {
                        val response = responseFile.readText()
                        responseFile.delete() // Clean up
                        requestFile.delete() // Clean up
                        "✅ Shipment created successfully! Response: $response"
                    } else {
                        requestFile.delete() // Clean up
                        "❌ Timeout waiting for server response"
                    }
                }
                
                is UpdateShipmentRequest -> {
                    // Write request to file
                    val requestFile = java.io.File("client_request.txt")
                    requestFile.writeText("UPDATE:${parsedRequest.data}")
                    
                    // Wait for response file
                    val responseFile = java.io.File("server_response.txt")
                    var attempts = 0
                    while (!responseFile.exists() && attempts < 50) {
                        Thread.sleep(100)  // Wait 100ms
                        attempts++
                    }
                    
                    return if (responseFile.exists()) {
                        val response = responseFile.readText()
                        responseFile.delete() // Clean up
                        requestFile.delete() // Clean up
                        "✅ Shipment updated successfully! Response: $response"
                    } else {
                        requestFile.delete() // Clean up
                        "❌ Timeout waiting for server response"
                    }
                }
                
                is TrackShipmentRequest -> {
                    return "🔍 Tracking feature not implemented in simple mode"
                }
            }
            // Add this line to satisfy the compiler
            return "❌ Unknown request type in processInputSimple"
        } catch (e: Exception) {
            println("DEBUG: Exception in processInputSimple: ${e.message}")
            e.printStackTrace()
            return "❌ Error processing input: ${e.message}"
        }
    }
    
    /**
     * Format timestamp for display
     */
    private fun formatTimestamp(timestamp: String): String {
        return try {
            val millis = timestamp.toLong()
            val date = java.util.Date(millis)
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)
        } catch (e: Exception) {
            timestamp
        }
    }
}

/**
 * Represents a connection to the tracking server
 */
class ServerConnection(private val serverUrl: String, private val port: Int) {
    private var connected = false
    private val baseUrl = "http://$serverUrl:$port"
    
    fun connect(): Boolean {
        return try {
            // Test connection by actually trying to reach the server
            val testConnection = java.net.URL(baseUrl).openConnection() as java.net.HttpURLConnection
            testConnection.requestMethod = "GET"
            testConnection.connectTimeout = 5000
            testConnection.readTimeout = 5000
            
            val responseCode = testConnection.responseCode
            testConnection.disconnect()
            
            if (responseCode > 0) {
                connected = true
                println("Connected to server at $serverUrl:$port")
                true
            } else {
                println("Server not responding at $serverUrl:$port")
                false
            }
        } catch (e: Exception) {
            println("Failed to connect to server: ${e.message}")
            false
        }
    }
    
    fun sendCreateRequest(data: String): Boolean {
        if (!connected) return false
        
        return try {
            val jsonPayload = """{"data": "$data"}"""
            val response = makeHttpRequest("POST", "$baseUrl/api/shipments/create", jsonPayload)
            println("Create request sent: $data")
            println("Server response: $response")
            true
        } catch (e: Exception) {
            println("Failed to send create request: ${e.message}")
            false
        }
    }
    
    fun sendUpdateRequest(data: String): Boolean {
        if (!connected) return false
        
        return try {
            val jsonPayload = """{"data": "$data"}"""
            val response = makeHttpRequest("POST", "$baseUrl/api/shipments/update", jsonPayload)
            println("Update request sent: $data")
            println("Server response: $response")
            true
        } catch (e: Exception) {
            println("Failed to send update request: ${e.message}")
            false
        }
    }
    
    private fun makeHttpRequest(method: String, url: String, jsonPayload: String): String {
        return try {
            println("DEBUG: Making $method request to: $url")
            println("DEBUG: Payload: $jsonPayload")
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = method
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            println("DEBUG: Connection configured, sending data...")
            
            // Send request body
            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray())
            }
            
            println("DEBUG: Data sent, reading response...")
            
            // Read response
            val responseCode = connection.responseCode
            println("DEBUG: Response code: $responseCode")
            
            val inputStream = if (responseCode >= 400) {
                connection.errorStream
            } else {
                connection.inputStream
            }
            
            val response = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            println("DEBUG: Response: $response")
            response
        } catch (e: Exception) {
            println("DEBUG: HTTP request exception: ${e.message}")
            e.printStackTrace()
            throw Exception("HTTP request failed: ${e.message}")
        }
    }
    
    fun disconnect() {
        connected = false
        println("Disconnected from server")
    }
}

/**
 * Base class for client requests as shown in UML
 */
abstract class ClientRequest(
    val type: RequestType,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Request types
 */
enum class RequestType {
    CREATE,
    UPDATE, 
    TRACK
}

/**
 * Create shipment request
 */
class CreateShipmentRequest(data: String) : 
    ClientRequest(RequestType.CREATE, data)

/**
 * Update shipment request  
 */
class UpdateShipmentRequest(data: String) : 
    ClientRequest(RequestType.UPDATE, data)

/**
 * Track shipment request
 */
class TrackShipmentRequest(data: String) : 
    ClientRequest(RequestType.TRACK, data)

/**
 * Server response structure
 */
data class ServerResponse(
    val status: String,
    val message: String,  
    val data: String? = null
) 