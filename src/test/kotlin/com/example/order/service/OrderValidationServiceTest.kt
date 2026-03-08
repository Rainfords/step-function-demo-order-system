package com.example.order.service

import com.example.order.model.Order
import com.example.order.model.OrderItem
import com.example.order.model.PaymentMethod
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class OrderValidationServiceTest {
    private lateinit var service: OrderValidationService

    @BeforeEach
    fun setup() {
        service = OrderValidationService()
    }

    @Test
    fun `should validate valid order`() {
        val order = Order(
            customerId = "CUST-001",
            items = listOf(
                OrderItem("PROD-001", 2, 29.99),
                OrderItem("PROD-002", 1, 49.99)
            ),
            paymentMethod = PaymentMethod("CREDIT_CARD", "4242")
        )

        val result = service.validateOrder(order)

        assertTrue(result.valid)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `should fail validation for empty customer ID`() {
        val order = Order(
            customerId = "",
            items = listOf(OrderItem("PROD-001", 1, 29.99)),
            paymentMethod = PaymentMethod("CREDIT_CARD", "4242")
        )

        val result = service.validateOrder(order)

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("Customer ID") })
    }

    @Test
    fun `should fail validation for empty items`() {
        val order = Order(
            customerId = "CUST-001",
            items = emptyList(),
            paymentMethod = PaymentMethod("CREDIT_CARD", "4242")
        )

        val result = service.validateOrder(order)

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("at least one item") })
    }

    @Test
    fun `should fail validation for negative quantity`() {
        val order = Order(
            customerId = "CUST-001",
            items = listOf(OrderItem("PROD-001", -1, 29.99)),
            paymentMethod = PaymentMethod("CREDIT_CARD", "4242")
        )

        val result = service.validateOrder(order)

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("Quantity") })
    }
}
