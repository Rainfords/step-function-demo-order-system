package com.example.order.service

import com.example.order.model.Order
import org.springframework.stereotype.Service
import kotlin.random.Random

data class PaymentResult(
    val success: Boolean,
    val transactionId: String? = null,
    val error: String? = null
)

@Service
class PaymentService {
    private val processedPayments = mutableSetOf<String>()

    fun processPayment(order: Order): PaymentResult {
        // Calculate total amount
        val totalAmount = order.items.sumOf { it.price * it.quantity }

        // Simulate occasional payment failures (10% failure rate for demo)
        if (Random.nextInt(100) < 10) {
            return PaymentResult(
                success = false,
                error = "Payment processing failed (simulated failure)"
            )
        }

        // Simulate payment processing
        val transactionId = "TXN-${order.orderId}-${System.currentTimeMillis()}"
        processedPayments.add(transactionId)

        return PaymentResult(
            success = true,
            transactionId = transactionId
        )
    }

    fun refundPayment(orderId: String, transactionId: String): Boolean {
        return processedPayments.remove(transactionId)
    }
}
