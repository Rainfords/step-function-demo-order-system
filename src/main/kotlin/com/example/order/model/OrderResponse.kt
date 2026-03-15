package com.example.order.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class OrderResponse(
    @JsonProperty("orderId")
    val orderId: String,
    @JsonProperty("customerId")
    val customerId: String,
    @JsonProperty("items")
    val items: List<OrderItem>,
    @JsonProperty("paymentMethod")
    val paymentMethod: PaymentMethod,
    @JsonProperty("status")
    val status: OrderStatus,
    @JsonProperty("executionArn")
    val executionArn: String?,
    @JsonProperty("failureReason")
    val failureReason: String?,
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime,
    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime
)

fun Order.toResponse() = OrderResponse(
    orderId = orderId,
    customerId = customerId,
    items = items,
    paymentMethod = paymentMethod,
    status = status,
    executionArn = executionArn,
    failureReason = failureReason,
    createdAt = createdAt,
    updatedAt = updatedAt
)
