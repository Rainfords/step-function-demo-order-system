package com.example.order.model

enum class OrderStatus {
    PENDING,
    VALIDATING,
    RESERVED,
    PAID,
    FULFILLED,
    COMPLETED,
    CANCELLED,
    FAILED
}
