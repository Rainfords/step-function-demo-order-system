# Order Processing System with AWS Step Functions

A Spring Boot demonstration of AWS Step Functions orchestrating an order processing workflow. Uses LocalStack for local development to enable rapid iteration without AWS costs.

## Overview

This system demonstrates how to build a sophisticated order processing pipeline using AWS Step Functions with Activity Tasks, where a Spring Boot backend handles the actual business logic.

**Workflow:** Order Creation → Validate Order → Reserve Inventory → Process Payment → Fulfill Order → Complete

## Architecture

### Activity Task Pattern

Instead of using Lambda functions, this implementation uses the **Activity Task** pattern:

1. Spring Boot services **poll** Step Functions for task assignments
2. Services **execute** business logic (validation, payment, fulfillment, etc.)
3. Services **report results** back to Step Functions
4. State machine progresses to next step or handles failures

This approach:
- ✅ Avoids Lambda cold starts and packaging complexity
- ✅ Keeps all logic in a single, testable Spring Boot application
- ✅ Enables local development with LocalStack
- ✅ Demonstrates enterprise orchestration patterns

### Component Diagram

```
┌─────────────────────────────────────────────────────┐
│           OrderController (REST API)                 │
└────────────────┬────────────────────────────────────┘
                 │
┌─────────────────▼────────────────────────────────────┐
│       OrderOrchestrationService                      │
│  (Starts/stops/monitors Step Functions executions)   │
└────────────┬──────────────────────────────┬──────────┘
             │                              │
    ┌────────▼─────────┐         ┌──────────▼─────┐
    │  LocalStack/AWS  │         │  OrderRepository│
    │  Step Functions  │         │  (In-Memory)    │
    │  Activities      │         └─────────────────┘
    └──────────────────┘
             │
    ┌────────┴──────────────────────────────┐
    │                                        │
┌───▼──────────────┐    ┌──────────────────▼──┐
│ Business Services│    │ Business Services    │
├──────────────────┤    ├─────────────────────┤
│ • Validation     │    │ • Inventory Reserve  │
│ • Payment        │    │ • Fulfillment        │
└──────────────────┘    └─────────────────────┘
```

## Prerequisites

- **Java 17+** - Download from [java.com](https://java.com)
- **Docker** - For running LocalStack
- **Git** - Version control

## Quick Start

### 1. Clone and Navigate to Project

```bash
cd ai-step-function-demo
```

### 2. Start LocalStack

This starts a local AWS Step Functions emulation service:

```bash
docker-compose up -d
```

Verify LocalStack is ready:
```bash
docker-compose logs localstack | grep "Ready"
```

### 3. Build the Application

```bash
./gradlew clean build
```

### 4. Run the Application

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

You should see:
```
Started OrderApplication in X seconds
```

Application is ready at: `http://localhost:8080`

### 5. Test the Order Flow

#### Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "items": [
      {
        "productId": "PROD-123",
        "quantity": 2,
        "price": 29.99
      }
    ],
    "paymentMethod": {
      "type": "CREDIT_CARD",
      "last4": "4242"
    }
  }'
```

Response (201 Created):
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "items": [...],
  "status": "VALIDATING",
  "executionArn": "arn:aws:states:us-east-1:000000000000:execution:order-550e8400-e29b-41d4-a716-446655440000:..."
}
```

#### Check Order Status

```bash
ORDER_ID="550e8400-e29b-41d4-a716-446655440000"
curl http://localhost:8080/api/orders/$ORDER_ID
```

#### List All Orders

```bash
curl http://localhost:8080/api/orders
```

Filter by status:
```bash
curl "http://localhost:8080/api/orders?status=COMPLETED"
```

#### Cancel an Order

```bash
curl -X POST http://localhost:8080/api/orders/$ORDER_ID/cancel
```

#### Health Check

```bash
curl http://localhost:8080/api/orders/health
```

## API Reference

### Endpoints

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| POST | `/api/orders` | Create new order | 201 Created |
| GET | `/api/orders` | List orders (paginated) | 200 OK |
| GET | `/api/orders/{orderId}` | Get order details | 200 OK or 404 |
| POST | `/api/orders/{orderId}/cancel` | Cancel order | 204 No Content |
| GET | `/api/orders/health` | Health check | 200 OK |

### Request/Response Examples

#### Create Order Request

```json
{
  "customerId": "CUST-001",
  "items": [
    {
      "productId": "PROD-123",
      "quantity": 2,
      "price": 29.99
    }
  ],
  "paymentMethod": {
    "type": "CREDIT_CARD",
    "last4": "4242"
  }
}
```

#### Order Response

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-001",
  "items": [
    {
      "productId": "PROD-123",
      "quantity": 2,
      "price": 29.99
    }
  ],
  "status": "COMPLETED",
  "paymentMethod": {
    "type": "CREDIT_CARD",
    "last4": "4242"
  },
  "executionArn": "arn:aws:states:us-east-1:000000000000:execution:order-550e8400...",
  "createdAt": "2026-03-07T10:30:00",
  "updatedAt": "2026-03-07T10:30:05"
}
```

## Order Status Workflow

```
PENDING
  ↓
VALIDATING ──(validation fails)──→ FAILED
  ↓
RESERVED ──(inventory shortage)──→ FAILED
  ↓
PAID ──(payment fails)──→ FAILED
  ↓
FULFILLED
  ↓
COMPLETED

CANCELLED (from any state via POST /cancel)
```

## Development

### Project Structure

```
├── src/main/kotlin/com/example/order/
│   ├── OrderApplication.kt              # Spring Boot entry point
│   ├── config/
│   │   └── AwsConfig.kt                # AWS SDK configuration
│   ├── controller/
│   │   └── OrderController.kt          # REST endpoints
│   ├── service/
│   │   ├── OrderValidationService.kt   # Validation logic
│   │   ├── InventoryService.kt         # Inventory management
│   │   ├── PaymentService.kt           # Payment processing
│   │   ├── FulfillmentService.kt       # Order fulfillment
│   │   └── OrderOrchestrationService.kt # Step Functions integration
│   ├── model/
│   │   ├── Order.kt, OrderStatus.kt
│   │   └── workflow/WorkflowInput.kt
│   └── repository/
│       └── OrderRepository.kt           # In-memory storage
├── src/test/kotlin/com/example/order/
│   ├── service/                         # Service tests
│   └── controller/                      # Controller tests
├── src/main/resources/
│   ├── application.yml                  # Base config
│   └── application-local.yml            # LocalStack config
├── docker-compose.yml
├── localstack/init-aws.sh               # LocalStack initialization
└── stepfunctions/order-workflow.json    # State machine definition
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests OrderValidationServiceTest

# Run with coverage report
./gradlew test --info
```

### Adding Features

To add a new service or modify existing logic:

1. **Update Models** - Add/modify data classes in `model/`
2. **Update Service** - Implement business logic
3. **Write Tests** - Add tests in `src/test/`
4. **Update Controller** - Expose via REST API if needed
5. **Test Locally** - Use curl commands to verify

Example: Adding inventory level endpoint:

```kotlin
// In InventoryService
fun getAvailableQuantity(productId: String): Int =
    inventory.getOrDefault(productId, 0)

// In OrderController
@GetMapping("/inventory/{productId}")
fun getInventory(@PathVariable productId: String): ResponseEntity<Int> {
    return ResponseEntity.ok(inventoryService.getAvailableQuantity(productId))
}

// Test it
curl http://localhost:8080/api/inventory/PROD-123
```

## LocalStack Management

### View LocalStack Logs

```bash
docker-compose logs -f localstack
```

### List State Machines

```bash
docker-compose exec localstack awslocal stepfunctions list-state-machines
```

### List Executions

```bash
docker-compose exec localstack awslocal stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-east-1:000000000000:stateMachine:OrderProcessingWorkflow
```

### Describe Execution

```bash
docker-compose exec localstack awslocal stepfunctions describe-execution \
  --execution-arn arn:aws:states:us-east-1:000000000000:execution:order-123...
```

### Get Execution History

```bash
docker-compose exec localstack awslocal stepfunctions get-execution-history \
  --execution-arn arn:aws:states:us-east-1:000000000000:execution:order-123...
```

### Stop LocalStack

```bash
docker-compose down
```

### Clean LocalStack Data

```bash
docker-compose down -v
```

## Troubleshooting

### Application won't start

**Error**: `Connection refused` to localhost:4566

**Solution**: Ensure LocalStack is running
```bash
docker-compose ps  # Should show localstack as "Up"
docker-compose logs localstack
```

### Orders created but status stuck in VALIDATING

**Possible causes**:
1. LocalStack not properly initialized
2. State machine not deployed

**Solution**:
```bash
docker-compose restart localstack
# Wait 10 seconds for init script to run
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Payment failures (expected for demo)

The PaymentService has a 10% failure rate to demonstrate error handling. This is intentional!

```kotlin
if (Random.nextInt(100) < 10) {
    return PaymentResult(success = false, error = "Payment processing failed")
}
```

To test success scenario, adjust the percentage in `PaymentService.kt`.

### Port 8080 already in use

**Error**: `Address already in use: bind`

**Solution**:
```bash
# Find process on port 8080
lsof -i :8080

# Kill it
kill -9 <PID>

# Or run on different port
./gradlew bootRun --args='--spring.profiles.active=local --server.port=8081'
```

## Configuration

### Spring Profiles

- **`local`** (default) - Uses LocalStack on localhost:4566
- **Production** - Uses AWS credentials for real AWS Step Functions

### application-local.yml

```yaml
aws:
  endpoint: http://localhost:4566
  region: us-east-1
  stepfunctions:
    state-machine-arn: arn:aws:states:us-east-1:000000000000:stateMachine:OrderProcessingWorkflow
```

### Environment Variables

For production deployment:
```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret
```

## Step Functions State Machine

The state machine orchestrates 4 activities in sequence:

```
ValidateOrder
    ↓
ReserveInventory
    ↓
ProcessPayment
    ↓
FulfillOrder
    ↓
OrderComplete (Success) or OrderFailed (Error)
```

Each step has:
- **Retry Logic**: 2 retries with exponential backoff for transient failures
- **Error Handling**: Catches all exceptions, transitions to failure state
- **Result Path**: Passes results to next step via JSON path

See `stepfunctions/order-workflow.json` for complete definition.

## Next Steps

### To Extend This Demo:

1. **Database Integration** - Replace OrderRepository with Spring Data JPA + PostgreSQL
2. **Real AWS** - Switch from LocalStack to actual AWS Step Functions
3. **Activity Workers** - Implement background threads that actually poll activities
4. **Advanced Workflows** - Add parallel processing, conditional logic, retries
5. **Monitoring** - Add CloudWatch integration for execution tracking
6. **API Authentication** - Add Spring Security for production deployment

## Architecture Notes

### Why Activity Tasks over Lambda?

| Aspect | Activity Tasks | Lambda |
|--------|----------------|--------|
| Complexity | Low - use existing Spring service | Medium - separate deployment |
| Local Testing | Easy with LocalStack | Requires SAM/LocalStack |
| Latency | Lower (no cold starts) | Higher (cold starts) |
| Cost | Cheaper (pay for execution) | More expensive |
| Monitoring | Standard Spring logging | CloudWatch logs |

### Thread Safety

- **OrderRepository** uses `ConcurrentHashMap` for thread-safe operations
- **InventoryService** uses concurrent operations for reservations
- **All services are stateless** except repository

### In-Memory Storage

For this demo, we use ConcurrentHashMap. For production:

```kotlin
@Repository
interface OrderRepository : JpaRepository<Order, String>

@Entity
data class OrderEntity(
    @Id val orderId: String,
    // ... fields
)
```

## Contributing

When making changes:

1. Write tests first (TDD approach)
2. Ensure all tests pass: `./gradlew test`
3. Follow Kotlin conventions
4. Update CLAUDE.md if structure changes
5. Commit with clear messages

## License

This project is a demonstration for educational purposes.

## Support

For issues or questions:
1. Check CLAUDE.md for architecture details
2. Review test cases in `src/test/`
3. Examine Spring Boot logs: `./gradlew bootRun`
4. Verify LocalStack is running: `docker-compose ps`
