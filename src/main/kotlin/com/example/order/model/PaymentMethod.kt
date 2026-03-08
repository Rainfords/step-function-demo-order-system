package com.example.order.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PaymentMethod(
    @JsonProperty("type")
    val type: String,
    @JsonProperty("last4")
    val last4: String
)
