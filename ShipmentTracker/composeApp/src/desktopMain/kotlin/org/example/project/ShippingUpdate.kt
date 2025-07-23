package org.example.project

data class ShippingUpdate(
    val previousStatus: String,
    val newStatus: String,
    val timestamp: Long
) 