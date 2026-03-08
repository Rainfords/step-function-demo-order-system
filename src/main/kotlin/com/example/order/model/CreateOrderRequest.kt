package com.example.order.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateOrderRequest(
    @JsonProperty("customerId")
    val customerId: String,
    @JsonProperty("items")
    val items: List<OrderItem>,
    @JsonProperty("paymentMethod")
    val paymentMethod: PaymentMethod
)
