package com.example.order.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OrderItem(
    @JsonProperty("productId")
    val productId: String,
    @JsonProperty("quantity")
    val quantity: Int,
    @JsonProperty("price")
    val price: Double
)
