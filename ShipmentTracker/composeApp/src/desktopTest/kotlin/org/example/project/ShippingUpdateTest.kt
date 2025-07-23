package org.example.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

class ShippingUpdateTest {

    @Test
    fun testShippingUpdateCreation() {
        val update = ShippingUpdate(
            previousStatus = "Created",
            newStatus = "Shipped",
            timestamp = 1234567890L
        )
        
        assertEquals("Created", update.previousStatus)
        assertEquals("Shipped", update.newStatus)
        assertEquals(1234567890L, update.timestamp)
    }

    @Test
    fun testShippingUpdateWithEmptyPreviousStatus() {
        val update = ShippingUpdate(
            previousStatus = "",
            newStatus = "Created",
            timestamp = 1234567890L
        )
        
        assertEquals("", update.previousStatus)
        assertEquals("Created", update.newStatus)
        assertEquals(1234567890L, update.timestamp)
    }

    @Test
    fun testShippingUpdateEquality() {
        val update1 = ShippingUpdate("Created", "Shipped", 1234567890L)
        val update2 = ShippingUpdate("Created", "Shipped", 1234567890L)
        val update3 = ShippingUpdate("Created", "Delivered", 1234567890L)
        
        assertEquals(update1, update2)
        assertNotEquals(update1, update3)
        assertEquals(update1.hashCode(), update2.hashCode())
    }

    @Test
    fun testShippingUpdateToString() {
        val update = ShippingUpdate("Created", "Shipped", 1234567890L)
        val toString = update.toString()
        
        assertTrue(toString.contains("Created"))
        assertTrue(toString.contains("Shipped"))
        assertTrue(toString.contains("1234567890"))
    }

    @Test
    fun testShippingUpdateCopy() {
        val original = ShippingUpdate("Created", "Shipped", 1234567890L)
        val copy = original.copy()
        val modifiedCopy = original.copy(newStatus = "Delivered")
        
        assertEquals(original, copy)
        assertEquals("Created", modifiedCopy.previousStatus)
        assertEquals("Delivered", modifiedCopy.newStatus)
        assertEquals(1234567890L, modifiedCopy.timestamp)
    }

    @Test
    fun testShippingUpdateWithCurrentTimestamp() {
        val currentTime = System.currentTimeMillis()
        val update = ShippingUpdate("Created", "Shipped", currentTime)
        
        assertEquals(currentTime, update.timestamp)
        assertTrue(update.timestamp > 0)
    }

    @Test
    fun testShippingUpdateProperties() {
        val update = ShippingUpdate("In Transit", "Delivered", 9876543210L)
        
        assertEquals("In Transit", update.previousStatus)
        assertEquals("Delivered", update.newStatus)
        assertEquals(9876543210L, update.timestamp)
    }
} 