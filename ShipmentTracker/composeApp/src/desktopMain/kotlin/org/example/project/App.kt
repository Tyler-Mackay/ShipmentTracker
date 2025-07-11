package org.example.project

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val viewModel = remember { ShipmentTrackerViewModel() }
    
    // Clean up when composable leaves composition
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.cleanup()
        }
    }
    
    MaterialTheme {
        ShipmentTrackerScreen(viewModel)
    }
}

@Composable
fun ShipmentTrackerScreen(viewModel: ShipmentTrackerViewModel) {
    var trackedShipments by remember { mutableStateOf(viewModel.trackedShipments) }
    var errorMessage by remember { mutableStateOf(viewModel.errorMessage) }
    var isLoading by remember { mutableStateOf(viewModel.isLoading) }
    
    // Update UI when viewModel state changes
    LaunchedEffect(viewModel) {
        while (true) {
            delay(100) // Check for updates every 100ms
            trackedShipments = viewModel.trackedShipments
            errorMessage = viewModel.errorMessage
            isLoading = viewModel.isLoading
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "📦 Shipment Tracker",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Input Section
        ShipmentInputSection(
            onTrackShipment = viewModel::toggleTracking,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onClearError = viewModel::clearError
        )
        
        // Tracked Shipments Section
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
                        onStopTracking = { viewModel.stopTracking(shipmentId) },
                        formatDate = viewModel::formatDate
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
    
    // Clear error when user starts typing
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
                placeholder = { Text("Enter shipment ID (e.g., SHIP001)") },
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
        
        // Error message
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
        
        // Helper text
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
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📦 ${shipment.id}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Status: ${shipment.status}",
                        fontSize = 14.sp,
                        color = getStatusColor(shipment.status)
                    )
                }
                
                // Stop tracking button
                IconButton(
                    onClick = onStopTracking,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("❌", fontSize = 16.sp)
                }
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Shipment details
                    DetailRow("📍 Location", shipment.currentLocation)
                    DetailRow("📅 Expected Delivery", formatDate(shipment.expectedDeliveryDate))
                    DetailRow("📝 Notes", "${shipment.notes.size} notes")
                    DetailRow("🔄 Updates", "${shipment.updateHistory.size} updates")
                    
                    // Recent updates
                    if (shipment.updateHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recent Updates:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        shipment.updateHistory.takeLast(3).forEach { update ->
                            Text(
                                text = "• ${update.previousStatus} → ${update.newStatus}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                    
                    // Live update indicator
                    LiveUpdateIndicator()
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LiveUpdateIndicator() {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            isVisible = !isVisible
        }
    }
    
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "🟢",
                fontSize = 8.sp
            )
        }
        Text(
            text = " Live updates (every second)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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