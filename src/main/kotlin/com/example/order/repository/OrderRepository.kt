package com.example.order.repository

import com.example.order.model.Order
import com.example.order.model.OrderStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class OrderRepository {
    private val orders = ConcurrentHashMap<String, Order>()
    private val executionArnToOrderId = ConcurrentHashMap<String, String>()

    fun save(order: Order): Order {
        orders[order.orderId] = order
        val arn = order.executionArn
        if (arn != null) {
            executionArnToOrderId[arn] = order.orderId
        }
        return order
    }

    fun findById(orderId: String): Order? = orders[orderId]

    fun findAll(): List<Order> = orders.values.toList()

    fun findByStatus(status: OrderStatus): List<Order> =
        orders.values.filter { it.status == status }

    fun findByExecutionArn(executionArn: String): Order? {
        val orderId = executionArnToOrderId[executionArn] ?: return null
        return orders[orderId]
    }

    fun delete(orderId: String): Boolean {
        val order = orders.remove(orderId) ?: return false
        val arn = order.executionArn
        if (arn != null) {
            executionArnToOrderId.remove(arn)
        }
        return true
    }

    fun update(order: Order): Order {
        orders[order.orderId] = order
        val arn = order.executionArn
        if (arn != null) {
            executionArnToOrderId[arn] = order.orderId
        }
        return order
    }
}
