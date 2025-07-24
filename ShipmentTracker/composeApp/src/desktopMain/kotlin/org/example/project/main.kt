package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main(args: Array<String>) {
    // If command line arguments are provided, run in client mode
    if (args.isNotEmpty()) {
        runClientMode(args)
        return
    }
    
    // Start the TrackingServer with simple file-based communication
    println("DEBUG: About to start TrackingServer...")
    val server = TrackingServer.getInstance()
    println("DEBUG: TrackingServer instance obtained")
    
    // Use simple file-based communication instead of HTTP
    server.startFileWatcher()
    println("DEBUG: TrackingServer file watcher started")
    
    // Run the GUI application
    application {
        Window(
            onCloseRequest = {
                // Stop the server when closing the application
                server.stopServer()
                exitApplication()
            },
            title = "ShipmentTracker",
        ) {
            App()
        }
    }
}

/**
 * Run the tracking client in command line mode
 */
private fun runClientMode(args: Array<String>) {
    val input = args.joinToString(" ")
    
    try {
        println("DEBUG: Starting client mode with input: $input")
        
        // Initialize the tracking client
        val trackingClient = TrackingClient()
        println("DEBUG: TrackingClient created (using simple file communication)")
        
        // Process the web input using simple file-based communication
        val result = trackingClient.processInputSimple(input)
        println("DEBUG: Processing result: $result")
        
        // Output the result (this will be captured by the web server)
        println("SUCCESS: $result")
        
    } catch (e: Exception) {
        println("ERROR: ${e.message}")
        e.printStackTrace()
    }
}