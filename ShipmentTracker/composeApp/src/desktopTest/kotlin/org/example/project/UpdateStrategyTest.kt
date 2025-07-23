package org.example.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

class UpdateStrategyTest {

    private lateinit var testShipment: Shipment

    @BeforeTest
    fun setup() {
        testShipment = Shipment(
            status = "Created",
            id = "SHIP001",
            expectedDeliveryDate = 1234567890L,
            currentLocation = "Warehouse A"
        )
    }

    @Test
    fun testCreateUpdateStrategy() {
        val strategy = CreateUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567891L)
        
        assertEquals("", result.previousStatus)
        assertEquals("Created", result.newStatus)
        assertEquals(1234567891L, result.timestamp)
    }

    @Test
    fun testShippedUpdateStrategy() {
        testShipment.status = "Created"
        val strategy = ShippedUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567892L)
        
        assertEquals("Created", result.previousStatus)
        assertEquals("Shipped", result.newStatus)
        assertEquals(1234567892L, result.timestamp)
    }

    @Test
    fun testLocationUpdateStrategy() {
        testShipment.status = "Shipped"
        val strategy = LocationUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567893L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("In Transit", result.newStatus)
        assertEquals(1234567893L, result.timestamp)
    }

    @Test
    fun testDeliveredUpdateStrategy() {
        testShipment.status = "In Transit"
        val strategy = DeliveredUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567894L)
        
        assertEquals("In Transit", result.previousStatus)
        assertEquals("Delivered", result.newStatus)
        assertEquals(1234567894L, result.timestamp)
    }

    @Test
    fun testDelayedUpdateStrategy() {
        testShipment.status = "Shipped"
        val strategy = DelayedUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567895L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("Delayed", result.newStatus)
        assertEquals(1234567895L, result.timestamp)
    }

    @Test
    fun testLostUpdateStrategy() {
        testShipment.status = "In Transit"
        val strategy = LostUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567896L)
        
        assertEquals("In Transit", result.previousStatus)
        assertEquals("Lost", result.newStatus)
        assertEquals(1234567896L, result.timestamp)
    }

    @Test
    fun testCancelledUpdateStrategy() {
        testShipment.status = "Created"
        val strategy = CancelledUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567897L)
        
        assertEquals("Created", result.previousStatus)
        assertEquals("Cancelled", result.newStatus)
        assertEquals(1234567897L, result.timestamp)
    }

    @Test
    fun testNoteAddedUpdateStrategy() {
        testShipment.status = "Shipped"
        val strategy = NoteAddedUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567898L)
        
        assertEquals("Shipped", result.previousStatus)
        assertEquals("Shipped", result.newStatus) // Status should remain unchanged
        assertEquals(1234567898L, result.timestamp)
    }

    @Test
    fun testCreateUpdateWithEmptyPreviousStatus() {
        val strategy = CreateUpdateStrategy()
        val result = strategy.processUpdate(testShipment, 1234567890L)
        
        assertEquals("", result.previousStatus)
        assertEquals("Created", result.newStatus)
    }

    @Test
    fun testAllStrategiesImplementInterface() {
        val strategies: List<UpdateProcessorStrategy> = listOf(
            CreateUpdateStrategy(),
            ShippedUpdateStrategy(),
            LocationUpdateStrategy(),
            DeliveredUpdateStrategy(),
            DelayedUpdateStrategy(),
            LostUpdateStrategy(),
            CancelledUpdateStrategy(),
            NoteAddedUpdateStrategy()
        )
        
        strategies.forEach { strategy ->
            val result = strategy.processUpdate(testShipment, 1234567890L)
            assertEquals(1234567890L, result.timestamp)
        }
    }

    @Test
    fun testStrategiesWithDifferentShipmentStates() {
        val shipmentStates = listOf("Created", "Shipped", "In Transit", "Delayed", "Lost", "Cancelled", "Delivered")
        val strategies = mapOf(
            "Create" to CreateUpdateStrategy(),
            "Shipped" to ShippedUpdateStrategy(),
            "Location" to LocationUpdateStrategy(),
            "Delivered" to DeliveredUpdateStrategy(),
            "Delayed" to DelayedUpdateStrategy(),
            "Lost" to LostUpdateStrategy(),
            "Cancelled" to CancelledUpdateStrategy(),
            "NoteAdded" to NoteAddedUpdateStrategy()
        )
        
        shipmentStates.forEach { state ->
            testShipment.status = state
            strategies.forEach { (strategyName, strategy) ->
                val result = strategy.processUpdate(testShipment, 1234567890L)
                assertEquals(state, result.previousStatus, "Failed for $strategyName with state $state")
                assertEquals(1234567890L, result.timestamp)
            }
        }
    }

    @Test
    fun testStrategiesWithZeroTimestamp() {
        val strategies = listOf(
            CreateUpdateStrategy(),
            ShippedUpdateStrategy(),
            LocationUpdateStrategy()
        )
        
        strategies.forEach { strategy ->
            val result = strategy.processUpdate(testShipment, 0L)
            assertEquals(0L, result.timestamp)
        }
    }

    @Test
    fun testStrategiesWithNegativeTimestamp() {
        val strategies = listOf(
            DeliveredUpdateStrategy(),
            DelayedUpdateStrategy(),
            LostUpdateStrategy()
        )
        
        strategies.forEach { strategy ->
            val result = strategy.processUpdate(testShipment, -1L)
            assertEquals(-1L, result.timestamp)
        }
    }

    @Test
    fun testStrategyResultConsistency() {
        val strategy = ShippedUpdateStrategy()
        
        // Multiple calls should produce consistent results
        val result1 = strategy.processUpdate(testShipment, 1234567890L)
        val result2 = strategy.processUpdate(testShipment, 1234567890L)
        
        assertEquals(result1.previousStatus, result2.previousStatus)
        assertEquals(result1.newStatus, result2.newStatus)
        assertEquals(result1.timestamp, result2.timestamp)
    }

    @Test
    fun testExpectedStatusTransitions() {
        val transitions = mapOf(
            CreateUpdateStrategy() to "Created",
            ShippedUpdateStrategy() to "Shipped",
            LocationUpdateStrategy() to "In Transit",
            DeliveredUpdateStrategy() to "Delivered",
            DelayedUpdateStrategy() to "Delayed",
            LostUpdateStrategy() to "Lost",
            CancelledUpdateStrategy() to "Cancelled",
            NoteAddedUpdateStrategy() to testShipment.status // Should preserve current status
        )
        
        transitions.forEach { (strategy, expectedNewStatus) ->
            val result = strategy.processUpdate(testShipment, 1234567890L)
            assertEquals(expectedNewStatus, result.newStatus, "Failed for ${strategy.javaClass.simpleName}")
        }
    }

    @Test
    fun testNoteAddedPreservesStatus() {
        val testStatuses = listOf("Created", "Shipped", "In Transit", "Delayed", "Delivered", "Lost", "Cancelled")
        val strategy = NoteAddedUpdateStrategy()
        
        testStatuses.forEach { status ->
            testShipment.status = status
            val result = strategy.processUpdate(testShipment, 1234567890L)
            
            assertEquals(status, result.previousStatus)
            assertEquals(status, result.newStatus) // Should preserve the status
        }
    }

    @Test
    fun testCreateAlwaysHasEmptyPreviousStatus() {
        val testStatuses = listOf("Created", "Shipped", "In Transit", "Delayed", "Delivered")
        val strategy = CreateUpdateStrategy()
        
        testStatuses.forEach { status ->
            testShipment.status = status
            val result = strategy.processUpdate(testShipment, 1234567890L)
            
            assertEquals("", result.previousStatus) // Should always be empty for Create
            assertEquals("Created", result.newStatus)
        }
    }

    @Test
    fun testTimestampPreservation() {
        val timestamps = listOf(0L, 1L, 1234567890L, Long.MAX_VALUE, -1L)
        val strategy = ShippedUpdateStrategy()
        
        timestamps.forEach { timestamp ->
            val result = strategy.processUpdate(testShipment, timestamp)
            assertEquals(timestamp, result.timestamp)
        }
    }
} 