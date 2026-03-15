package com.example.order.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sfn.SfnClient
import software.amazon.awssdk.services.sfn.model.GetActivityTaskRequest
import software.amazon.awssdk.services.sfn.model.SendTaskFailureRequest
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest
import com.example.order.model.Order
import com.example.order.model.OrderStatus
import com.example.order.model.OrderStatusUpdate
import com.example.order.model.workflow.WorkflowInput
import com.example.order.config.AwsProperties
import com.example.order.repository.OrderRepository

@EnableAsync
@Service
class ActivityWorker(
    private val sfnClient: SfnClient,
    private val awsProperties: AwsProperties,
    private val validationService: OrderValidationService,
    private val inventoryService: InventoryService,
    private val paymentService: PaymentService,
    private val fulfillmentService: FulfillmentService,
    private val objectMapper: ObjectMapper,
    private val orderRepository: OrderRepository,
    private val messagingTemplate: SimpMessagingTemplate,
    @Value("\${app.simulation.activity-delay-ms:0}") private val activityDelayMs: Long
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // Track idle time for each activity to implement exponential backoff
    private val activityIdleTime = mutableMapOf<String, Long>()

    init {
        logger.info("ActivityWorker initialized with ARNs:")
        logger.info("  Validate: ${awsProperties.validateActivityArn}")
        logger.info("  Inventory: ${awsProperties.inventoryActivityArn}")
        logger.info("  Payment: ${awsProperties.paymentActivityArn}")
        logger.info("  Fulfillment: ${awsProperties.fulfillmentActivityArn}")
        logger.info("  Release: ${awsProperties.releaseActivityArn}")
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    @Async
    fun validateOrderWorker() {
        pollAndExecute(
            activityArn = awsProperties.validateActivityArn,
            activityName = "ValidateOrder",
            activeStatus = OrderStatus.VALIDATING
        ) { input ->
            val order = Order(
                orderId = input.orderId,
                customerId = input.customerId,
                items = input.items,
                paymentMethod = input.paymentMethod
            )
            val result = validationService.validateOrder(order)
            objectMapper.writeValueAsString(result)
        }
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    @Async
    fun reserveInventoryWorker() {
        pollAndExecute(
            activityArn = awsProperties.inventoryActivityArn,
            activityName = "ReserveInventory",
            activeStatus = OrderStatus.RESERVED
        ) { input ->
            val order = Order(
                orderId = input.orderId,
                customerId = input.customerId,
                items = input.items,
                paymentMethod = input.paymentMethod
            )
            val result = inventoryService.reserveInventory(order)
            objectMapper.writeValueAsString(result)
        }
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    @Async
    fun processPaymentWorker() {
        pollAndExecute(
            activityArn = awsProperties.paymentActivityArn,
            activityName = "ProcessPayment",
            activeStatus = OrderStatus.PAID
        ) { input ->
            val order = Order(
                orderId = input.orderId,
                customerId = input.customerId,
                items = input.items,
                paymentMethod = input.paymentMethod
            )
            val result = paymentService.processPayment(order)
            if (!result.success) {
                throw RuntimeException(result.error ?: "Payment declined")
            }
            objectMapper.writeValueAsString(result)
        }
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    @Async
    fun fulfillOrderWorker() {
        pollAndExecute(
            activityArn = awsProperties.fulfillmentActivityArn,
            activityName = "FulfillOrder",
            activeStatus = OrderStatus.FULFILLED
        ) { input ->
            val order = Order(
                orderId = input.orderId,
                customerId = input.customerId,
                items = input.items,
                paymentMethod = input.paymentMethod
            )
            val result = fulfillmentService.fulfillOrder(order)
            objectMapper.writeValueAsString(result)
        }
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    @Async
    fun releaseInventoryWorker() {
        pollAndExecute(
            activityArn = awsProperties.releaseActivityArn,
            activityName = "ReleaseInventory",
            activeStatus = OrderStatus.FAILED
        ) { input ->
            val released = inventoryService.releaseReservation(input.orderId)
            logger.info("Released inventory for order ${input.orderId}: $released")
            objectMapper.writeValueAsString(mapOf("released" to released, "orderId" to input.orderId))
        }
    }

    private fun shouldSkipPoll(activityName: String): Boolean {
        val lastIdleTime = activityIdleTime[activityName] ?: return false
        val timeSinceLastIdle = System.currentTimeMillis() - lastIdleTime
        // Exponential backoff: skip polls for increasing duration (max 30 seconds)
        val backoffMs = minOf((Math.pow(2.0, (timeSinceLastIdle / 5000).toDouble()) * 1000).toLong(), 30000L)
        return timeSinceLastIdle < backoffMs
    }

    private fun pollAndExecute(
        activityArn: String,
        activityName: String,
        activeStatus: OrderStatus? = null,
        executor: (WorkflowInput) -> String
    ) {
        try {
            // Skip polling if we've been idle (exponential backoff)
            if (shouldSkipPoll(activityName)) {
                logger.trace("Skipping poll for $activityName (backoff)")
                return
            }

            val request = GetActivityTaskRequest.builder()
                .activityArn(activityArn)
                .workerName("order-service-$activityName")
                .build()

            // logger.debug("Polling for $activityName tasks on ARN: $activityArn")
            val response = sfnClient.getActivityTask(request)

            // No task available - mark as idle
            if (response.taskToken() == null) {
                logger.trace("No task available for $activityName")
                activityIdleTime[activityName] = System.currentTimeMillis()
                return
            }

            // Task found - reset idle time
            activityIdleTime.remove(activityName)

            logger.info("Received $activityName task: ${response.taskToken()}")
            logger.info("Task input: ${response.input()}")

            val input = objectMapper.readValue(response.input(), WorkflowInput::class.java)
            val taskToken = response.taskToken()

            // Update order status immediately when task is received
            if (activeStatus != null) {
                val order = orderRepository.findById(input.orderId)
                if (order != null) {
                    order.status = activeStatus
                    orderRepository.update(order)
                    logger.info("Updated order ${input.orderId} status to $activeStatus")

                    // Publish status update via WebSocket
                    val statusUpdate = OrderStatusUpdate(input.orderId, activeStatus)
                    messagingTemplate.convertAndSend("/topic/orders/${input.orderId}/status", statusUpdate)
                }
            }

            try {
                logger.info("Executing $activityName for order ${input.orderId}")
                val output = executor(input)
                logger.info("$activityName completed for order ${input.orderId}")
                logger.info("Task output: $output")

                if (activityDelayMs > 0) {
                    logger.info("Applying simulated delay of ${activityDelayMs}ms for $activityName")
                    Thread.sleep(activityDelayMs)
                }

                sfnClient.sendTaskSuccess(
                    SendTaskSuccessRequest.builder()
                        .taskToken(taskToken)
                        .output(output)
                        .build()
                )
            } catch (e: Exception) {
                logger.error("$activityName failed for order ${input.orderId}", e)
                sfnClient.sendTaskFailure(
                    SendTaskFailureRequest.builder()
                        .taskToken(taskToken)
                        .error("ActivityTaskFailed")
                        .cause(e.message ?: "Unknown error")
                        .build()
                )
            }
        } catch (e: Exception) {
            logger.error("Error polling $activityName activity: ${e.message}", e)
        }
    }
}
