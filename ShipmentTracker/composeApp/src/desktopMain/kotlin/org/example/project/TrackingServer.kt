            package org.example.project

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpExchange
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.example.project.Shipment.*
import org.example.project.ShippingUpdate.ShippingUpdate

/**
 * TrackingServer - Singleton server that manages all shipments and provides HTTP API
 * Based on UML design with singleton pattern and network communication
 */
class TrackingServer private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: TrackingServer? = null
        
        fun getInstance(): TrackingServer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrackingServer().also { INSTANCE = it }
            }
        }
    }
    
    // Core components as per UML
    private val shipments: MutableMap<String, Shipment> = mutableMapOf()
    private val parser: ShipmentDataParser = ShipmentDataParser()
    private val updater: ShipmentUpdater = ShipmentUpdater()
    private val factory: ShipmentFactory = ShipmentFactory
    
    private var server: HttpServer? = null
    
    /**
     * Start the HTTP server
     */
    fun startServer(port: Int = 8080) {
        try {
            println("DEBUG: Attempting to start TrackingServer on port $port")
            server = HttpServer.create(InetSocketAddress(port), 0)
            println("DEBUG: HTTP server created successfully")
            
            // Create shipment endpoint
            server?.createContext("/api/shipments/create") { exchange ->
                handleCreateShipment(exchange)
            }
            
            // Update shipment endpoint
            server?.createContext("/api/shipments/update") { exchange ->
                handleUpdateShipment(exchange)
            }
            
            // Get shipment endpoint
            server?.createContext("/api/shipments") { exchange ->
                handleGetShipment(exchange)
            }
            
            server?.executor = Executors.newFixedThreadPool(10)
            server?.start()
            
            println("✅ TrackingServer started successfully on port $port")
            println("Available endpoints:")
            println("  POST /api/shipments/create")
            println("  POST /api/shipments/update") 
            println("  GET  /api/shipments/{id}")
        } catch (e: Exception) {
            println("❌ Failed to start TrackingServer: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Stop the HTTP server
     */
    fun stopServer() {
        server?.stop(0)
        println("TrackingServer stopped")
    }
    
    private fun handleCreateShipment(exchange: HttpExchange) {
        println("DEBUG: handleCreateShipment called")
        println("DEBUG: Request method: ${exchange.requestMethod}")
        
        if (exchange.requestMethod != "POST") {
            println("DEBUG: Method not allowed, sending 405")
            sendResponse(exchange, 405, """{"error": "Method not allowed"}""")
            return
        }
        
        try {
            val requestBody = exchange.requestBody.bufferedReader().readText()
            println("DEBUG: Request body: $requestBody")
            
            val data = parseJsonData(requestBody)
            println("DEBUG: Parsed data: $data")
            
            val request = CreateShipmentRequest(data)
            println("DEBUG: Created CreateShipmentRequest")
            
            val response = createShipment(request)
            println("DEBUG: createShipment returned: ${response.success}, ${response.message}")
            
            val jsonResponse = """
                {
                    "success": ${response.success},
                    "message": "${response.message}",
                    "shipmentData": ${if (response.shipmentData != null) shipmentToJson(response.shipmentData) else "null"},
                    "abnormality": "${response.abnormality}"
                }
            """.trimIndent()
            
            println("DEBUG: Sending response: $jsonResponse")
            sendResponse(exchange, 200, jsonResponse)
            println("DEBUG: Response sent successfully")
        } catch (e: Exception) {
            println("DEBUG: Exception in handleCreateShipment: ${e.message}")
            e.printStackTrace()
            sendResponse(exchange, 400, """{"error": "Failed to create shipment: ${e.message}"}""")
        }
    }
    
    private fun handleUpdateShipment(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            sendResponse(exchange, 405, """{"error": "Method not allowed"}""")
            return
        }
        
        try {
            val requestBody = exchange.requestBody.bufferedReader().readText()
            val data = parseJsonData(requestBody)
            val request = UpdateShipmentRequest(data)
            val response = updateShipment(request)
            
            val jsonResponse = """
                {
                    "success": ${response.success},
                    "message": "${response.message}",
                    "shipmentData": ${if (response.shipmentData != null) shipmentToJson(response.shipmentData) else "null"},
                    "abnormality": "${response.abnormality}"
                }
            """.trimIndent()
            
            sendResponse(exchange, 200, jsonResponse)
        } catch (e: Exception) {
            sendResponse(exchange, 400, """{"error": "Failed to update shipment: ${e.message}"}""")
        }
    }
    
    private fun handleGetShipment(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            sendResponse(exchange, 405, """{"error": "Method not allowed"}""")
            return
        }
        
        try {
            val path = exchange.requestURI.path
            val id = path.split("/").lastOrNull() ?: throw IllegalArgumentException("Missing shipment ID")
            val response = getShipment(id)
            
            val jsonResponse = """
                {
                    "success": ${response != null},
                    "message": "Shipment found",
                    "shipmentData": ${if (response != null) shipmentToJson(response) else "null"},
                    "abnormality": "${response?.abnormality ?: ""}"
                }
            """.trimIndent()
            
            sendResponse(exchange, if (response != null) 200 else 404, jsonResponse)
        } catch (e: Exception) {
            sendResponse(exchange, 400, """{"error": "Failed to get shipment: ${e.message}"}""")
        }
    }
    
    private fun sendResponse(exchange: HttpExchange, statusCode: Int, response: String) {
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type")
        
        exchange.sendResponseHeaders(statusCode, response.length.toLong())
        val outputStream: OutputStream = exchange.responseBody
        outputStream.write(response.toByteArray())
        outputStream.close()
    }
    
    private fun parseJsonData(jsonBody: String): String {
        // Simple JSON parsing for {"data": "value"} format
        val dataPattern = """"data"\s*:\s*"([^"]*)"""".toRegex()
        val match = dataPattern.find(jsonBody)
        return match?.groupValues?.get(1) ?: throw IllegalArgumentException("Invalid JSON format")
    }
    
    private fun shipmentToJson(shipment: Shipment): String {
        return """
            {
                "status": "${shipment.status}",
                "id": "${shipment.id}",
                "shipmentType": "${shipment.shipmentType.name}",
                "creationDate": ${shipment.creationDate},
                "notes": [${shipment.notes.joinToString(",") { "\"$it\"" }}],
                "expectedDeliveryDateTimestamp": ${shipment.expectedDeliveryDateTimestamp},
                "currentLocation": "${shipment.currentLocation}",
                "isAbnormal": ${shipment.isAbnormal},
                "abnormality": "${shipment.abnormality}"
            }
        """.trimIndent()
    }
    
    // UML Methods Implementation
    
    fun createShipment(request: CreateShipmentRequest): CreateShipmentResponse {
        return try {
            // Parse the shipment data from the request
            val shipmentData = parser.parseCreatedShipment(request.data)
            
            // Create the appropriate shipment type using factory
            val shipment = factory.createShipment(
                type = shipmentData.shipmentType,
                status = "created",
                id = shipmentData.id,
                creationDate = shipmentData.timestamp,
                expectedDeliveryDateTimestamp = shipmentData.timestamp + (7 * 24 * 60 * 60 * 1000), // Default 7 days
                currentLocation = ""
            )
            
            // Validate delivery date
            val validation = shipment.validateDeliveryDate()
            
            // Store the shipment
            shipments[shipment.id] = shipment
            println("DEBUG: Shipment stored in map. Map now contains: ${shipments.keys}")
            println("DEBUG: Shipment ${shipment.id} details: ${shipment.status}, type: ${shipment.shipmentType}")
            
            CreateShipmentResponse(
                success = true,
                message = "Shipment ${shipment.id} created successfully",
                shipmentData = shipment,
                abnormality = if (!validation.isValid) validation.message else ""
            )
        } catch (e: Exception) {
            CreateShipmentResponse(
                success = false,
                message = "Failed to create shipment: ${e.message}",
                shipmentData = null,
                abnormality = ""
            )
        }
    }
    
    fun updateShipment(request: UpdateShipmentRequest): UpdateShipmentResponse {
        return try {
            // Parse the update data
            val updateData = parser.parseUpdateData(request.data)
            
            // Get the existing shipment
            val shipment = shipments[updateData.shipmentId]
                ?: throw IllegalArgumentException("Shipment ${updateData.shipmentId} not found")
            
            // Process the update using the updater
            val shippingUpdate = updater.processUpdate(updateData.updateType, shipment, updateData.timestamp)
            
            // Apply the update to the shipment
            println("DEBUG: Updating shipment ${shipment.id} with ${updateData.updateType}")
            println("DEBUG: Shipment status before update: ${shipment.status}")
            shipment.addUpdate(shippingUpdate)
            println("DEBUG: Shipment status after update: ${shipment.status}")
            
            // TODO: Notify observers that shipment was updated
            // This should trigger TrackerViewHelper.onShipmentUpdate if it's observing this shipment
            println("DEBUG: Shipment ${shipment.id} updated - observers should be notified")
            
            // Handle special update types
            when (updateData.updateType.lowercase()) {
                "location" -> updateData.additionalData?.let { 
                    println("DEBUG: Updating location to: $it")
                    shipment.updateLocation(it) 
                }
                "noteadded" -> updateData.additionalData?.let { 
                    println("DEBUG: Adding note: $it")
                    shipment.addNote(it) 
                }
            }
            
            UpdateShipmentResponse(
                success = true,
                message = "Shipment ${shipment.id} updated successfully with ${updateData.updateType}",
                shipmentData = shipment,
                abnormality = if (shipment.isAbnormal) shipment.abnormality else ""
            )
        } catch (e: Exception) {
            UpdateShipmentResponse(
                success = false,
                message = "Failed to update shipment: ${e.message}",
                shipmentData = null,
                abnormality = ""
            )
        }
    }
    
    fun getShipment(id: String): Shipment? {
        println("DEBUG: getShipment called for ID: $id")
        println("DEBUG: Available shipments in map: ${shipments.keys}")
        val result = shipments[id]
        println("DEBUG: getShipment result for $id: ${if (result != null) "FOUND" else "NOT FOUND"}")
        return result
    }
    
    fun trackShipment(id: String): Boolean {
        return shipments.containsKey(id)
    }
    
    fun stopTracking(id: String): Boolean {
        // For now, this just indicates if the shipment exists
        // In a more complex system, this might remove active tracking/subscriptions
        return shipments.containsKey(id)
    }

    /**
     * Start simple file-based communication (alternative to HTTP)
     */
    fun startFileWatcher() {
        val watcherThread = Thread {
            println("🔧 Starting simple file-based communication watcher...")
            
            while (true) {
                try {
                    val requestFile = java.io.File("client_request.txt")
                    
                    if (requestFile.exists()) {
                        println("DEBUG: Found client request file")
                        val requestContent = requestFile.readText()
                        println("DEBUG: Request content: $requestContent")
                        
                        // Process the request
                        val response = processFileRequest(requestContent)
                        println("DEBUG: Generated response: $response")
                        
                        // Write response
                        val responseFile = java.io.File("server_response.txt")
                        responseFile.writeText(response)
                        println("DEBUG: Response written to file")
                        
                        // Delete request file to indicate it's been processed
                        requestFile.delete()
                        println("DEBUG: Request file deleted")
                    }
                    
                    Thread.sleep(50) // Check every 50ms
                } catch (e: Exception) {
                    println("DEBUG: Error in file watcher: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        watcherThread.isDaemon = true  // Make it a daemon thread so it won't prevent app shutdown
        watcherThread.start()
    }
    
    private fun processFileRequest(requestContent: String): String {
        return try {
            when {
                requestContent.startsWith("CREATE:") -> {
                    println("📂 Processing CREATE request")
                    val data = requestContent.removePrefix("CREATE:")
                    val request = CreateShipmentRequest(data)
                    val response = createShipment(request)
                    
                    "✅ ${response.message}"
                }
                
                requestContent.startsWith("UPDATE:") -> {
                    println("📂 Processing UPDATE request")
                    val data = requestContent.removePrefix("UPDATE:")
                    println("📂 Parsed data: $data")
                    val request = UpdateShipmentRequest(data)
                    println("📂 About to call updateShipment()")
                    val response = updateShipment(request)
                    println("📂 updateShipment() returned: ${response.message}")
                    
                    "✅ ${response.message}"
                }
                
                else -> {
                    "❌ Unknown request format: $requestContent"
                }
            }
        } catch (e: Exception) {
            println("📂 Exception in processFileRequest: ${e.message}")
            e.printStackTrace()
            "❌ Error processing request: ${e.message}"
        }
    }
}

// Response classes as per UML
abstract class TrackingServerResponse(
    val success: Boolean,
    val message: String,
    val shipmentData: Shipment?,
    val abnormality: String
)

class CreateShipmentResponse(
    success: Boolean,
    message: String,
    shipmentData: Shipment?,
    abnormality: String
) : TrackingServerResponse(success, message, shipmentData, abnormality)

class UpdateShipmentResponse(
    success: Boolean,
    message: String,
    shipmentData: Shipment?,
    abnormality: String
) : TrackingServerResponse(success, message, shipmentData, abnormality)

class GetShipmentResponse(
    success: Boolean,
    message: String,
    shipmentData: Shipment?,
    abnormality: String
) : TrackingServerResponse(success, message, shipmentData, abnormality) 