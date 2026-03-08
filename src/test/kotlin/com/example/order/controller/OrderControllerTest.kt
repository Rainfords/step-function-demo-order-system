package com.example.order.controller

import com.example.order.model.CreateOrderRequest
import com.example.order.model.OrderItem
import com.example.order.model.PaymentMethod
import com.example.order.repository.OrderRepository
import com.example.order.service.InventoryService
import com.example.order.service.OrderOrchestrationService
import io.mockk.mockk
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest

@WebMvcTest(OrderController::class)
class OrderControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper
) {

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun orderRepository(): OrderRepository = OrderRepository()

        @Bean
        @Primary
        fun inventoryService(): InventoryService = InventoryService()

        @Bean
        @Primary
        fun orchestrationService(): OrderOrchestrationService {
            val mockOrchestrationService = mockk<OrderOrchestrationService>()
            every { mockOrchestrationService.startOrderWorkflow(any()) } returns "arn:aws:states:us-east-1:000000000000:execution:test"
            return mockOrchestrationService
        }
    }

    @Test
    fun `should create order successfully`() {
        val request = CreateOrderRequest(
            customerId = "CUST-001",
            items = listOf(OrderItem("PROD-123", 2, 29.99)),
            paymentMethod = PaymentMethod("CREDIT_CARD", "4242")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$.orderId").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.customerId").value("CUST-001"))
    }

    @Test
    fun `should return 404 for non-existent order`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/orders/non-existent")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `should list orders`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/orders")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.orders").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.total").exists())
    }

    @Test
    fun `should get health status`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/orders/health")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }
}
