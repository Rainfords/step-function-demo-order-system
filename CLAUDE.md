# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ai-step-function-demo** is a Spring Boot-based backend service demonstrating AWS Step Functions orchestration for an order processing workflow. It uses LocalStack for local development, enabling rapid iteration without AWS costs.

The system orchestrates a complete order processing pipeline:
**Order Creation → Validate Order → Reserve Inventory → Process Payment → Fulfill Order → Complete**

Current Status: Fully functional Spring Boot application with Step Functions integration, LocalStack support, and comprehensive test coverage.

## Tech Stack

- **Framework**: Spring Boot 3.2.3
- **Language**: Kotlin 1.9.23
- **Build Tool**: Gradle 8.5
- **AWS SDK**: AWS SDK v2 (Step Functions, STS)
- **Local Development**: LocalStack (Step Functions emulation)
- **Testing**: JUnit 5, Mockk, TestContainers
- **IDE**: IntelliJ IDEA

## Quick Start

### Prerequisites
- Java 17+
- Gradle 8.5+ (wrapper included)
- Docker (for LocalStack)

### 1. Start LocalStack
```bash
docker-compose up -d
docker-compose logs -f localstack  # Monitor startup
```

### 2. Build the Application
```bash
./gradlew clean build
```

### 3. Run the Application
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

The application will start on `http://localhost:8080`

## Project Structure

```
src/
  main/
    kotlin/
      com/example/order/
        OrderApplication.kt           # Spring Boot entry point
        config/
          AwsConfig.kt               # AWS SDK configuration
        controller/
          OrderController.kt         # REST API endpoints
        service/
          OrderValidationService.kt  # Order validation logic
          InventoryService.kt        # Inventory management
          PaymentService.kt          # Payment processing
          FulfillmentService.kt      # Order fulfillment
          OrderOrchestrationService.kt # Step Functions orchestration
        model/
          Order.kt, OrderStatus.kt, OrderItem.kt, PaymentMethod.kt
          CreateOrderRequest.kt, OrderResponse.kt
          workflow/
            WorkflowInput.kt         # Step Functions workflow input
        repository/
          OrderRepository.kt         # In-memory order storage
  test/
    kotlin/
      com/example/order/
        service/                     # Service unit tests
        controller/                  # Controller tests
  resources/
    application.yml                  # Main configuration
    application-local.yml            # LocalStack configuration

docker-compose.yml                   # LocalStack setup
localstack/
  init-aws.sh                        # LocalStack initialization
stepfunctions/
  order-workflow.json                # State machine definition
```

## Architecture Overview

### Core Patterns

1. **Activity Tasks Pattern**: Spring Boot services poll Step Functions for task assignments, execute business logic, and report results back
2. **Service Layer**: Encapsulates domain logic (validation, inventory, payment, fulfillment)
3. **Orchestration Service**: Manages Step Functions execution lifecycle (start, status, stop)
4. **Repository Layer**: In-memory order storage (easily replaceable with JPA + database)
5. **REST API**: Thin controller layer delegating to services

### Key Components

- **OrderOrchestrationService**: Manages Step Functions execution (start workflow, query status, stop execution)
- **Business Services**: OrderValidationService, InventoryService, PaymentService, FulfillmentService (mock implementations for demo)
- **OrderRepository**: Thread-safe in-memory storage using ConcurrentHashMap
- **AwsConfig**: Configures SfnClient to connect to LocalStack or AWS

### Design Decisions

- **LocalStack**: Full Step Functions emulation for local development without AWS costs
- **In-Memory State**: Suitable for demo; easily replaceable with Spring Data JPA + PostgreSQL
- **Activity Tasks**: Avoids Lambda deployment complexity; Spring services poll for work
- **Mock Services**: Focus on orchestration patterns rather than business logic complexity

## Common Commands

```bash
# Build without tests (fast iteration)
./gradlew build -x test

# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests OrderValidationServiceTest

# Start application (requires LocalStack running)
./gradlew bootRun --args='--spring.profiles.active=local'

# Check dependencies
./gradlew dependencies

# View gradle wrapper version
cat gradle/wrapper/gradle-wrapper.properties
```

## Configuration

- **Profiles**: `local` (LocalStack), default (AWS)
- **application.yml**: Base configuration (port 8080, logging levels)
- **application-local.yml**: LocalStack endpoint and Step Functions ARNs
- **Environment Variables**: Use for sensitive data (AWS keys in production)
- **Logging**: Structured logging via Spring Boot defaults (Logback)

## API Endpoints

### Order Management

**POST /api/orders** - Create Order
- Creates new order and starts Step Functions workflow
- Request body: `{ customerId, items: [{productId, quantity, price}], paymentMethod: {type, last4} }`
- Returns: 201 Created with OrderResponse

**GET /api/orders/{orderId}** - Get Order Status
- Fetches order and syncs status from Step Functions
- Returns: 200 OK with OrderResponse, 404 if not found

**GET /api/orders** - List Orders
- Query params: `status` (filter), `page` (default 0), `size` (default 20)
- Returns: 200 OK with paginated orders

**POST /api/orders/{orderId}/cancel** - Cancel Order
- Stops Step Functions execution, releases inventory
- Returns: 204 No Content

**GET /api/orders/health** - Health Check
- Returns: 200 OK with status

## Testing Strategy

- **Unit Tests**: Test services in isolation (OrderValidationService, InventoryService, etc.)
- **Controller Tests**: Test API endpoints with MockMvc
- **Integration Tests**: Use TestContainers with LocalStack (optional, currently using mocks)
- **Coverage Target**: Maintain >80% coverage for services and controllers

## Important Notes

- **Controller Layer**: Keep endpoints thin—delegate to services
- **Service Layer**: Contains all business logic and Step Functions orchestration
- **Thread Safety**: OrderRepository uses ConcurrentHashMap for thread-safe operations
- **Error Handling**: Services return result objects with success/error status
- **Mocking**: PaymentService has 10% failure rate to demonstrate error handling
- **LocalStack Initialization**: State machine and activities are created via init-aws.sh on container start

## File Locations

- **Main Application**: `src/main/kotlin/com/example/order/OrderApplication.kt`
- **Controllers**: `src/main/kotlin/com/example/order/controller/`
- **Services**: `src/main/kotlin/com/example/order/service/`
- **Models**: `src/main/kotlin/com/example/order/model/`
- **Repository**: `src/main/kotlin/com/example/order/repository/`
- **Configuration**: `src/main/kotlin/com/example/order/config/`
- **Tests**: `src/test/kotlin/com/example/order/`
- **AWS Config**: `src/main/resources/application-local.yml`
- **Step Functions State Machine**: `stepfunctions/order-workflow.json`
- **LocalStack Setup**: `docker-compose.yml`, `localstack/init-aws.sh`
