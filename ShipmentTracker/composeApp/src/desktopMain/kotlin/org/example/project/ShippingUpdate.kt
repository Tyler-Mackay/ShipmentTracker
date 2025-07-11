package org.example.project

/**
 * Represents a shipping update with status change information
 */
data class ShippingUpdate(
    val previousStatus: String,
    val newStatus: String,
    val timestamp: Long
) 