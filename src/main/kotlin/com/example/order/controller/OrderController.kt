package com.example.order.controller

import com.example.order.model.CreateOrderRequest
import com.example.order.model.Order
import com.example.order.model.OrderStatus
import com.example.order.model.toResponse
import com.example.order.repository.OrderRepository
import com.example.order.service.OrderOrchestrationService
import com.example.order.service.InventoryService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderRepository: OrderRepository,
    private val orchestrationService: OrderOrchestrationService,
    private val inventoryService: InventoryService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<Any> {
        return try {
            val order = Order(
                customerId = request.customerId,
                items = request.items,
                paymentMethod = request.paymentMethod
            )

            orderRepository.save(order)
            logger.info("Order created: ${order.orderId}")

            // Start Step Functions workflow
            try {
                orchestrationService.startOrderWorkflow(order)
                ResponseEntity.status(HttpStatus.CREATED).body(order.toResponse())
            } catch (e: Exception) {
                logger.error("Failed to start workflow for order ${order.orderId}", e)
                order.status = OrderStatus.FAILED
                orderRepository.update(order)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Failed to start order workflow"))
            }
        } catch (e: Exception) {
            logger.error("Error creating order", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/{orderId}")
    fun getOrderStatus(@PathVariable orderId: String): ResponseEntity<Any> {
        val order = orderRepository.findById(orderId)
            ?: return ResponseEntity.notFound().build()

        // Sync status from Step Functions and publish updates via WebSocket
        orchestrationService.syncAndPublishOrderStatus(order)

        return ResponseEntity.ok(order.toResponse())
    }

    @GetMapping
    fun listOrders(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Any> {
        val orders = if (status != null) {
            try {
                val orderStatus = OrderStatus.valueOf(status.uppercase())
                orderRepository.findByStatus(orderStatus)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Invalid status: $status"))
            }
        } else {
            orderRepository.findAll()
        }

        val paginated = orders
            .sortedByDescending { it.createdAt }
            .drop(page * size)
            .take(size)
            .map { it.toResponse() }

        return ResponseEntity.ok(mapOf(
            "orders" to paginated,
            "total" to orders.size,
            "page" to page,
            "size" to size
        ))
    }

    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(@PathVariable orderId: String): ResponseEntity<Any> {
        val order = orderRepository.findById(orderId)
            ?: return ResponseEntity.notFound().build()

        if (order.executionArn != null) {
            orchestrationService.stopExecution(order.executionArn!!)
        }

        // Release inventory if reserved
        inventoryService.releaseReservation(orderId)

        order.status = OrderStatus.CANCELLED
        orderRepository.update(order)

        logger.info("Order cancelled: $orderId")
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Any> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "order-processing-service"
        ))
    }
}
