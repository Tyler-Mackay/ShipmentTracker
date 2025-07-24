package org.example.project.Tracker

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.example.project.Shipment.*

/**
 * TrackingServer - Singleton server that manages all shipments and provides HTTP API
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
    
    private val shipments: MutableMap<String, Shipment> = mutableMapOf()
    private val parser: ShipmentDataParser = ShipmentDataParser()
    private val updater: ShipmentUpdater = ShipmentUpdater()
    private val factory: ShipmentFactory = ShipmentFactory
    
    private var server: HttpServer? = null
    
    fun startServer(port: Int = 8080) {
        try {
            server = HttpServer.create(InetSocketAddress(port), 0)
            
            server?.createContext("/api/shipments/create") { exchange ->
                handleCreateShipment(exchange)
            }
            
            server?.createContext("/api/shipments/update") { exchange ->
                handleUpdateShipment(exchange)
            }
            
            server?.createContext("/api/shipments") { exchange ->
                handleGetShipment(exchange)
            }
            
            server?.executor = Executors.newFixedThreadPool(10)
            server?.start()
        } catch (e: Exception) {
            // Handle server startup error silently
        }
    }
    
    fun stopServer() {
        server?.stop(0)
    }
    
    private fun handleCreateShipment(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            sendResponse(exchange, 405, """{"error": "Method not allowed"}""")
            return
        }
        
        try {
            val requestBody = exchange.requestBody.bufferedReader().readText()
            val data = parseJsonData(requestBody)
            val request = CreateShipmentRequest(data)
            val response = createShipment(request)
            
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
    
    fun createShipment(request: CreateShipmentRequest): CreateShipmentResponse {
        return try {
            val shipmentData = parser.parseCreatedShipment(request.data)
            
            val shipment = factory.createShipment(
                type = shipmentData.shipmentType,
                status = "created",
                id = shipmentData.id,
                creationDate = shipmentData.timestamp,
                expectedDeliveryDateTimestamp = shipmentData.timestamp + (7 * 24 * 60 * 60 * 1000),
                currentLocation = ""
            )
            
            val validation = shipment.validateDeliveryDate()
            shipments[shipment.id] = shipment
            
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
            val updateData = parser.parseUpdateData(request.data)
            
            val shipment = shipments[updateData.shipmentId]
                ?: throw IllegalArgumentException("Shipment ${updateData.shipmentId} not found")
            
            val shippingUpdate = updater.processUpdate(updateData.updateType, shipment, updateData.timestamp)
            val updateTypeLower = updateData.updateType.lowercase()
            var estimatedDeliveryTimestamp: Long? = null
            if (updateTypeLower == "create" || updateTypeLower == "shipped" || updateTypeLower == "delayed") {
                // For these types, the estimated delivery timestamp is the additionalData field
                estimatedDeliveryTimestamp = updateData.additionalData?.toLongOrNull()
            }
            shipment.addUpdate(shippingUpdate, estimatedDeliveryTimestamp, updateTypeLower)
            
            when (updateData.updateType.lowercase()) {
                "location" -> updateData.additionalData?.let { 
                    shipment.updateLocation(it) 
                }
                "noteadded" -> updateData.additionalData?.let { 
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
        return shipments[id]
    }
    
    fun trackShipment(id: String): Boolean {
        return shipments.containsKey(id)
    }
    
    fun stopTracking(id: String): Boolean {
        return shipments.containsKey(id)
    }

    fun startFileWatcher() {
        val watcherThread = Thread {
            while (true) {
                try {
                    val requestFile = java.io.File("client_request.txt")
                    
                    if (requestFile.exists()) {
                        val requestContent = requestFile.readText()
                        val response = processFileRequest(requestContent)
                        
                        val responseFile = java.io.File("server_response.txt")
                        responseFile.writeText(response)
                        requestFile.delete()
                    }
                    
                    Thread.sleep(50)
                } catch (e: Exception) {
                    // Handle file watcher error silently
                }
            }
        }
        watcherThread.isDaemon = true
        watcherThread.start()
    }
    
    private fun processFileRequest(requestContent: String): String {
        return try {
            when {
                requestContent.startsWith("CREATE:") -> {
                    val data = requestContent.removePrefix("CREATE:")
                    val request = CreateShipmentRequest(data)
                    val response = createShipment(request)
                    "✅ ${response.message}"
                }
                
                requestContent.startsWith("UPDATE:") -> {
                    val data = requestContent.removePrefix("UPDATE:")
                    val request = UpdateShipmentRequest(data)
                    val response = updateShipment(request)
                    "✅ ${response.message}"
                }
                
                else -> {
                    "❌ Unknown request format: $requestContent"
                }
            }
        } catch (e: Exception) {
            "❌ Error processing request: ${e.message}"
        }
    }
}

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