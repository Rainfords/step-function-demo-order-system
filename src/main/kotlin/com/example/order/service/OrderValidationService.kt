package com.example.order.service

import com.example.order.model.Order
import org.springframework.stereotype.Service

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList()
)

@Service
class OrderValidationService {

    fun validateOrder(order: Order): ValidationResult {
        val errors = mutableListOf<String>()

        if (order.customerId.isBlank()) {
            errors.add("Customer ID cannot be empty")
        }

        if (order.items.isEmpty()) {
            errors.add("Order must contain at least one item")
        }

        for ((index, item) in order.items.withIndex()) {
            if (item.productId.isBlank()) {
                errors.add("Item $index: Product ID cannot be empty")
            }
            if (item.quantity <= 0) {
                errors.add("Item $index: Quantity must be positive")
            }
            if (item.price <= 0) {
                errors.add("Item $index: Price must be positive")
            }
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors
        )
    }
}
