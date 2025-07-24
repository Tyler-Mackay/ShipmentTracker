package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.project.Tracker.TrackingClient
import org.example.project.Tracker.TrackingServer

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        runClientMode(args)
        return
    }
    
    val server = TrackingServer.getInstance()
    server.startFileWatcher()
    
    application {
        Window(
            onCloseRequest = {
                server.stopServer()
                exitApplication()
            },
            title = "ShipmentTracker",
        ) {
            App()
        }
    }
}

private fun runClientMode(args: Array<String>) {
    val input = args.joinToString(" ")
    
    try {
        val trackingClient = TrackingClient()
        val result = trackingClient.processInputSimple(input)
        println("SUCCESS: $result")
    } catch (e: Exception) {
        println("ERROR: ${e.message}")
    }
}