package com.example.order.service

import com.example.order.model.Order
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

data class ReservationResult(
    val reserved: Boolean,
    val reservationId: String? = null,
    val error: String? = null
)

@Service
class InventoryService {
    // Mock inventory: product ID -> available quantity
    private val inventory = ConcurrentHashMap<String, Int>()
    // Track reservations: reservation ID -> product ID -> quantity
    private val reservations = ConcurrentHashMap<String, Map<String, Int>>()

    init {
        // Initialize with some products
        inventory["PROD-001"] = 100
        inventory["PROD-002"] = 50
        inventory["PROD-003"] = 25
        inventory["PROD-123"] = 1000
    }

    fun reserveInventory(order: Order): ReservationResult {
        // Check if all items are available
        for (item in order.items) {
            val available = inventory.getOrDefault(item.productId, 0)
            if (available < item.quantity) {
                return ReservationResult(
                    reserved = false,
                    error = "Insufficient inventory for product ${item.productId}. Available: $available, Requested: ${item.quantity}"
                )
            }
        }

        // Reserve inventory
        val reservationId = "RES-${order.orderId}"
        val reservedItems = mutableMapOf<String, Int>()

        for (item in order.items) {
            inventory.compute(item.productId) { _, current ->
                (current ?: 0) - item.quantity
            }
            reservedItems[item.productId] = item.quantity
        }

        reservations[reservationId] = reservedItems

        return ReservationResult(
            reserved = true,
            reservationId = reservationId
        )
    }

    fun releaseReservation(orderId: String): Boolean {
        val reservationId = "RES-$orderId"
        val items = reservations.remove(reservationId) ?: return false

        for ((productId, quantity) in items) {
            inventory.compute(productId) { _, current ->
                (current ?: 0) + quantity
            }
        }

        return true
    }

    fun getInventory(productId: String): Int = inventory.getOrDefault(productId, 0)
}
