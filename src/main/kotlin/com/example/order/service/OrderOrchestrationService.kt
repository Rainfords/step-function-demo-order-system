package com.example.order.service

import com.example.order.config.AwsProperties
import com.example.order.model.Order
import com.example.order.model.OrderStatus
import com.example.order.model.OrderStatusUpdate
import com.example.order.model.workflow.WorkflowInput
import com.example.order.repository.OrderRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sfn.SfnClient
import software.amazon.awssdk.services.sfn.model.DescribeExecutionRequest
import software.amazon.awssdk.services.sfn.model.ExecutionStatus
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest
import software.amazon.awssdk.services.sfn.model.StopExecutionRequest

@Service
class OrderOrchestrationService(
    private val sfnClient: SfnClient,
    private val awsProperties: AwsProperties,
    private val orderRepository: OrderRepository,
    private val objectMapper: ObjectMapper,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun startOrderWorkflow(order: Order): String {
        try {
            val workflowInput = WorkflowInput(
                orderId = order.orderId,
                customerId = order.customerId,
                items = order.items,
                paymentMethod = order.paymentMethod
            )

            val inputJson = objectMapper.writeValueAsString(workflowInput)

            val request = StartExecutionRequest.builder()
                .stateMachineArn(awsProperties.stateMachineArn)
                .name("order-${order.orderId}")
                .input(inputJson)
                .build()

            val response = sfnClient.startExecution(request)
            val executionArn = response.executionArn()

            logger.info("Started Step Functions execution for order ${order.orderId}: $executionArn")

            // Update order with execution ARN
            order.executionArn = executionArn
            order.status = OrderStatus.VALIDATING
            orderRepository.update(order)

            return executionArn
        } catch (e: Exception) {
            logger.error("Failed to start order workflow for order ${order.orderId}", e)
            throw e
        }
    }

    fun getExecutionStatus(executionArn: String): ExecutionStatus? {
        return try {
            val request = DescribeExecutionRequest.builder()
                .executionArn(executionArn)
                .build()

            val response = sfnClient.describeExecution(request)
            response.status()
        } catch (e: Exception) {
            logger.warn("Failed to get execution status for $executionArn", e)
            null
        }
    }

    fun stopExecution(executionArn: String): Boolean {
        return try {
            val request = StopExecutionRequest.builder()
                .executionArn(executionArn)
                .build()

            sfnClient.stopExecution(request)
            logger.info("Stopped execution: $executionArn")
            true
        } catch (e: Exception) {
            logger.error("Failed to stop execution: $executionArn", e)
            false
        }
    }

    fun mapExecutionStatusToOrderStatus(executionStatus: ExecutionStatus?): OrderStatus {
        return when (executionStatus) {
            ExecutionStatus.RUNNING -> OrderStatus.VALIDATING
            ExecutionStatus.SUCCEEDED -> OrderStatus.COMPLETED
            ExecutionStatus.FAILED -> OrderStatus.FAILED
            ExecutionStatus.TIMED_OUT -> OrderStatus.FAILED
            ExecutionStatus.ABORTED -> OrderStatus.CANCELLED
            else -> OrderStatus.PENDING
        }
    }

    fun syncAndPublishOrderStatus(order: Order) {
        if (order.executionArn == null) return

        try {
            val executionStatus = getExecutionStatus(order.executionArn!!)
            val newStatus = mapExecutionStatusToOrderStatus(executionStatus)

            if (newStatus != order.status && newStatus == OrderStatus.COMPLETED) {
                order.status = newStatus
                orderRepository.update(order)

                // Publish completion status via WebSocket
                val statusUpdate = OrderStatusUpdate(order.orderId, newStatus)
                messagingTemplate.convertAndSend("/topic/orders/${order.orderId}/status", statusUpdate)
                logger.info("Published COMPLETED status for order ${order.orderId} via WebSocket")
            }
        } catch (e: Exception) {
            logger.warn("Failed to sync execution status for order ${order.orderId}", e)
        }
    }
}
