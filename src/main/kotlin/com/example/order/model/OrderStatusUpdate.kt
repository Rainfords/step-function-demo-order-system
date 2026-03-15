package com.example.order.model

import java.time.Instant

data class OrderStatusUpdate(
    val orderId: String,
    val status: OrderStatus,
    val timestamp: Instant = Instant.now()
)
