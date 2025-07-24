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
    
    fun stopServer() {
        server?.stop(0)
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
                message = "Shipment ${shipment.id} ${updateData.updateType.lowercase()} successfully!",
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
                    val shipmentId = response.shipmentData?.id ?: "?"
                    "Shipment $shipmentId created successfully!"
                }
                
                requestContent.startsWith("UPDATE:") -> {
                    val data = requestContent.removePrefix("UPDATE:")
                    val request = UpdateShipmentRequest(data)
                    val response = updateShipment(request)
                    val shipmentId = response.shipmentData?.id ?: "?"
                    val updateType = (response.shipmentData?.status ?: "update").lowercase()
                    "Shipment $shipmentId $updateType successfully!"
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