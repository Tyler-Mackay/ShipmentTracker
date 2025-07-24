package org.example.project.Tracker

/**
 * TrackingClient - Handles client-side logic for shipment tracking
 */
class TrackingClient {
    private var serverConnection: ServerConnection? = null
    
    fun initializeConnection(serverUrl: String, port: Int): Boolean {
        return try {
            serverConnection = ServerConnection(serverUrl, port)
            serverConnection?.connect() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    fun sendCreateRequest(data: String): Boolean {
        return try {
            serverConnection?.sendCreateRequest(data) ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    fun sendUpdateRequest(data: String): Boolean {
        return try {
            serverConnection?.sendUpdateRequest(data) ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    fun parseUserInput(input: String): ClientRequest {
        val trimmedInput = input.trim()
        
        if (trimmedInput.contains(",")) {
            return parseSimulationLine(trimmedInput)
        }
        
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
                TrackShipmentRequest(input.trim())
            }
        }
    }
    
    private fun parseSimulationLine(line: String): ClientRequest {
        val parts = line.split(",")
        
        if (parts.size < 3) {
            throw IllegalArgumentException("Invalid simulation format")
        }
        
        val eventType = parts[0].trim().lowercase()
        val shipmentId = parts[1].trim()
        
        return when (eventType) {
            "created" -> {
                if (parts.size < 4) {
                    throw IllegalArgumentException("Created events require format: created,shipmentId,shipmentType,timestamp")
                }
                val shipmentType = parts[2].trim()
                val timestamp = parts[3].trim()
                CreateShipmentRequest(formatCreatedShipmentData(eventType, shipmentId, shipmentType, timestamp))
            }
            "shipped", "delayed", "location", "noteadded", "lost", "canceled", "delivered" -> {
                val timestamp = parts[2].trim()
                val additionalData = if (parts.size > 3) parts.drop(3).joinToString(",").trim() else null
                UpdateShipmentRequest(formatShipmentData(eventType, shipmentId, timestamp, additionalData))
            }
            else -> TrackShipmentRequest(shipmentId)
        }
    }
    
    private fun formatShipmentData(eventType: String, shipmentId: String, timestamp: String, additionalData: String?): String {
        return buildString {
            append("$eventType,$shipmentId,$timestamp")
            if (additionalData != null) {
                append(",$additionalData")
            }
        }
    }
    
    private fun formatCreatedShipmentData(eventType: String, shipmentId: String, shipmentType: String, timestamp: String): String {
        return "$eventType,$shipmentId,$shipmentType,$timestamp"
    }
    
    fun processInputSimple(input: String): String {
        try {
            val parsedRequest = parseUserInput(input)
            
            when (parsedRequest) {
                is CreateShipmentRequest -> {
                    val requestFile = java.io.File("client_request.txt")
                    requestFile.writeText("CREATE:${parsedRequest.data}")
                    
                    val responseFile = java.io.File("server_response.txt")
                    var attempts = 0
                    while (!responseFile.exists() && attempts < 50) {
                        Thread.sleep(100)
                        attempts++
                    }
                    
                    return if (responseFile.exists()) {
                        val response = responseFile.readText()
                        responseFile.delete()
                        requestFile.delete()
                        response
                    } else {
                        requestFile.delete()
                        "❌ Timeout waiting for server response"
                    }
                }
                
                is UpdateShipmentRequest -> {
                    val requestFile = java.io.File("client_request.txt")
                    requestFile.writeText("UPDATE:${parsedRequest.data}")
                    
                    val responseFile = java.io.File("server_response.txt")
                    var attempts = 0
                    while (!responseFile.exists() && attempts < 50) {
                        Thread.sleep(100)
                        attempts++
                    }
                    
                    return if (responseFile.exists()) {
                        val response = responseFile.readText()
                        responseFile.delete()
                        requestFile.delete()
                        response
                    } else {
                        requestFile.delete()
                        "❌ Timeout waiting for server response"
                    }
                }
                
                is TrackShipmentRequest -> {
                    return "🔍 Tracking feature not implemented in simple mode"
                }
            }
            return "❌ Unknown request type in processInputSimple"
        } catch (e: Exception) {
            return "❌ Error processing input: ${e.message}"
        }
    }

}

class ServerConnection(serverUrl: String, port: Int) {
    private var connected = false
    private val baseUrl = "http://$serverUrl:$port"
    
    fun connect(): Boolean {
        return try {
            val testConnection = java.net.URL(baseUrl).openConnection() as java.net.HttpURLConnection
            testConnection.requestMethod = "GET"
            testConnection.connectTimeout = 5000
            testConnection.readTimeout = 5000
            
            val responseCode = testConnection.responseCode
            testConnection.disconnect()
            
            if (responseCode > 0) {
                connected = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun sendCreateRequest(data: String): Boolean {
        if (!connected) return false
        
        return try {
            val jsonPayload = """{"data": "$data"}"""
            makeHttpRequest("POST", "$baseUrl/api/shipments/create", jsonPayload)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun sendUpdateRequest(data: String): Boolean {
        if (!connected) return false
        
        return try {
            val jsonPayload = """{"data": "$data"}"""
            makeHttpRequest("POST", "$baseUrl/api/shipments/update", jsonPayload)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun makeHttpRequest(method: String, url: String, jsonPayload: String): String {
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = method
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray())
            }
            
            val responseCode = connection.responseCode
            
            val inputStream = if (responseCode >= 400) {
                connection.errorStream
            } else {
                connection.inputStream
            }
            
            inputStream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            throw Exception("HTTP request failed: ${e.message}")
        }
    }
}

abstract class ClientRequest(
    val type: RequestType,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RequestType {
    CREATE,
    UPDATE, 
    TRACK
}

class CreateShipmentRequest(data: String) : 
    ClientRequest(RequestType.CREATE, data)

class UpdateShipmentRequest(data: String) : 
    ClientRequest(RequestType.UPDATE, data)

class TrackShipmentRequest(data: String) : 
    ClientRequest(RequestType.TRACK, data)