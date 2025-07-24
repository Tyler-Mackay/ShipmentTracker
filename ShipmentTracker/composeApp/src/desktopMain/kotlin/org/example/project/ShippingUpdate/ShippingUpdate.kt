package org.example.project.ShippingUpdate

data class ShippingUpdate(
    val previousStatus: String,
    val newStatus: String,
    val timestamp: Long
) 