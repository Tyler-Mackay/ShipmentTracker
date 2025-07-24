package org.example.project

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.Shipment.Shipment
import org.example.project.Tracker.TrackerViewHelper
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val trackerViewHelper = remember { 
        TrackerViewHelper().apply {
            initialize() 
        } 
    }
    
    DisposableEffect(trackerViewHelper) {
        onDispose {
            trackerViewHelper.cleanup()
        }
    }
    
    MaterialTheme {
        ShipmentTrackerScreen(trackerViewHelper)
    }
}

@Composable
fun ShipmentTrackerScreen(trackerViewHelper: TrackerViewHelper) {
    // Observe the Compose state for automatic recomposition
    val trackedShipments by trackerViewHelper.trackedShipmentsState
    val updateVersion by trackerViewHelper.updateVersion  // Force recomposition when version changes
    val errorMessage = trackerViewHelper.errorMessage
    val isLoading = trackerViewHelper.isLoading
    
    // Debug: Print when recomposition happens
    println("🎨 ShipmentTrackerScreen recomposition - tracked: ${trackedShipments.size}, version: $updateVersion")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📦 Shipment Tracker",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        ShipmentInputSection(
            onTrackShipment = trackerViewHelper::toggleTracking,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onClearError = trackerViewHelper::clearError
        )
        
        if (trackedShipments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Tracked Shipments (${trackedShipments.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trackedShipments.entries.toList()) { (shipmentId, shipment) ->
                    ShipmentCard(
                        shipment = shipment,
                        onStopTracking = { trackerViewHelper.stopTrackingShipment(shipmentId) },
                        formatDate = trackerViewHelper::formatDate
                    )
                }
            }
        }
    }
}

@Composable
fun ShipmentInputSection(
    onTrackShipment: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit
) {
    var shipmentId by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(shipmentId) {
        if (errorMessage != null) {
            onClearError()
        }
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = shipmentId,
                onValueChange = { shipmentId = it },
                label = { Text("Shipment ID") },
                placeholder = { Text("Enter shipment ID (e.g., s10000)") },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onTrackShipment(shipmentId)
                        shipmentId = ""
                    }
                ),
                singleLine = true,
                isError = errorMessage != null
            )
            
            Button(
                onClick = {
                    onTrackShipment(shipmentId)
                    shipmentId = ""
                },
                enabled = !isLoading && shipmentId.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Track")
                }
            }
        }
        
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "❌ $message",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        
        Text(
            text = "💡 Tip: Enter the same ID again to stop tracking it",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ShipmentCard(
    shipment: Shipment,
    onStopTracking: () -> Unit,
    formatDate: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Tracking shipment: ${shipment.id}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onStopTracking,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("❌", fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Status: ${shipment.status}",
                fontSize = 16.sp,
                color = getStatusColor(shipment.status)
            )
            
            Text(
                text = "Location: ${shipment.currentLocation}",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Text(
                text = "Expected Delivery: ${formatDate(shipment.expectedDeliveryDateTimestamp)}",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            if (shipment.updateHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Status Updates:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                shipment.updateHistory.forEach { update ->
                    val statusText = if (update.previousStatus.isBlank()) {
                        "Shipment status set to ${update.newStatus} at ${formatDate(update.timestamp)}"
                    } else {
                        "Shipment went from ${update.previousStatus} to ${update.newStatus} at ${formatDate(update.timestamp)}"
                    }
                    
                    Text(
                        text = statusText,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Notes:",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            if (shipment.notes.isNotEmpty()) {
                shipment.notes.forEach { note ->
                    Text(
                        text = note,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "created" -> Color(0xFF2196F3)
        "shipped" -> Color(0xFF4CAF50)
        "in transit" -> Color(0xFFFF9800)
        "delivered" -> Color(0xFF4CAF50)
        "delayed" -> Color(0xFFFF5722)
        "cancelled" -> Color(0xFFf44336)
        "lost" -> Color(0xFFf44336)
        else -> MaterialTheme.colorScheme.onSurface
    }
}