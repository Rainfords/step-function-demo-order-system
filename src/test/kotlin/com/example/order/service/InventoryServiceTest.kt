package com.example.order.service

import com.example.order.model.Order
import com.example.order.model.OrderItem
import com.example.order.model.PaymentMethod
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class InventoryServiceTest {
    private lateinit var service: InventoryService

    @BeforeEach
    fun setup() {
        service = InventoryService()
    }

    @Test
    fun `should reserve inventory successfully`() {
        val order = Order(
            orderId = "ORD-001",
            customerId = "CUST-001",
            items = listOf(OrderItem("PROD-001", 5, 29.99)),
            paymentMethod = PaymentMethod("CREDIT_CARD", "4242")
        )

        val result = service.reserveInventory(order)

        assertTrue(result.reserved)
        assertNotNull(result.reservationId)
    }

    @Test
    fun `should fail reservation for insufficient inventory`() {
        val order = Order(
            orderId = "ORD-002",
            customerId = "CUST-001",
            items = listOf(OrderItem("PROD-001", 5000, 29.99)),
            paymentMethod = PaymentMethod("CREDIT_CARD", "4242")
        )

        val result = service.reserveInventory(order)

        assertFalse(result.reserved)
        assertTrue(result.error?.contains("Insufficient inventory") ?: false)
    }

    @Test
    fun `should release inventory reservation`() {
        val orderId = "ORD-003"
        val order = Order(
            orderId = orderId,
            customerId = "CUST-001",
            items = listOf(OrderItem("PROD-002", 3, 49.99)),
            paymentMethod = PaymentMethod("CREDIT_CARD", "4242")
        )

        service.reserveInventory(order)
        val initialInventory = service.getInventory("PROD-002")

        val released = service.releaseReservation(orderId)

        assertTrue(released)
        assertEquals(initialInventory + 3, service.getInventory("PROD-002"))
    }
}
