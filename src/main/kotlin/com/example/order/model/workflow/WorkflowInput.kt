package com.example.order.model.workflow

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.example.order.model.OrderItem
import com.example.order.model.PaymentMethod

@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowInput(
    @JsonProperty("orderId")
    val orderId: String,
    @JsonProperty("customerId")
    val customerId: String,
    @JsonProperty("items")
    val items: List<OrderItem>,
    @JsonProperty("paymentMethod")
    val paymentMethod: PaymentMethod
)
