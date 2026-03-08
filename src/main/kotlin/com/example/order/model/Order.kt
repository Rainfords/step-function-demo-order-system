package com.example.order.model

import java.time.LocalDateTime
import java.util.UUID

data class Order(
    val orderId: String = UUID.randomUUID().toString(),
    val customerId: String,
    val items: List<OrderItem>,
    val paymentMethod: PaymentMethod,
    var status: OrderStatus = OrderStatus.PENDING,
    var executionArn: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
