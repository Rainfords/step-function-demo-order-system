package com.example.order.service

import com.example.order.model.Order
import org.springframework.stereotype.Service
import java.time.LocalDate

data class FulfillmentResult(
    val fulfilled: Boolean,
    val trackingNumber: String? = null,
    val estimatedDelivery: LocalDate? = null,
    val error: String? = null
)

@Service
class FulfillmentService {
    private val fulfillments = mutableSetOf<String>()

    fun fulfillOrder(order: Order): FulfillmentResult {
        val trackingNumber = "TRK-${order.orderId}-${System.currentTimeMillis()}"
        val estimatedDelivery = LocalDate.now().plusDays(5)

        fulfillments.add(trackingNumber)

        return FulfillmentResult(
            fulfilled = true,
            trackingNumber = trackingNumber,
            estimatedDelivery = estimatedDelivery
        )
    }

    fun cancelFulfillment(trackingNumber: String): Boolean {
        return fulfillments.remove(trackingNumber)
    }
}
