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

    init {
        logger.info("ActivityWorker initialized with ARNs:")
        logger.info("  Validate: ${awsProperties.validateActivityArn}")
        logger.info("  Inventory: ${awsProperties.inventoryActivityArn}")
        logger.info("  Payment: ${awsProperties.paymentActivityArn}")
        logger.info("  Fulfillment: ${awsProperties.fulfillmentActivityArn}")
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
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

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
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

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
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
            objectMapper.writeValueAsString(result)
        }
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
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

    private fun pollAndExecute(
        activityArn: String,
        activityName: String,
        activeStatus: OrderStatus? = null,
        executor: (WorkflowInput) -> String
    ) {
        try {
            val request = GetActivityTaskRequest.builder()
                .activityArn(activityArn)
                .workerName("order-service-$activityName")
                .build()

            // logger.debug("Polling for $activityName tasks on ARN: $activityArn")
            val response = sfnClient.getActivityTask(request)

            // No task available
            if (response.taskToken() == null) {
                logger.trace("No task available for $activityName")
                return
            }

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
