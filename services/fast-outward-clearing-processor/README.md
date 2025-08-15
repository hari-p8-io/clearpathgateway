# Fast Outward Clearing Processor

## Overview

The Fast Outward Clearing Processor handles all outbound payment processing for Credit Transfer Outward (CTO) transactions. It coordinates with the liquidity service for authorization checks and manages the complete outward payment lifecycle.

## Responsibilities

### Core Functions
- **CTO Processing**: Handle outward credit transfer requests from PSP Global
- **Liquidity Authorization**: Coordinate with fast-liquidity-service for pre-payment checks
- **Message Construction**: Generate PACS.008 messages for G3 Host transmission
- **Payment Orchestration**: Manage payment workflow and state tracking
- **Response Processing**: Handle PACS.002 confirmations from G3 Host
- **Exception Handling**: Process failed payments and timeout scenarios
- **Hold and Retry**: Manage payments held due to insufficient liquidity

### Payment Types
- **Credit Transfer Outward (CTO)**: Bank sends money on behalf of customer
- **Bulk Payments**: Process multiple payments in batch
- **Priority Payments**: High-priority payment processing

## Architecture

### Input Sources
- PSP Global (payment requests)
- fast-sender-service (PACS.002 responses)
- Manual intervention interfaces

### Output Targets
- fast-liquidity-service (authorization requests)
- fast-sender-service (outbound PACS.008 messages)
- PSP Global (status updates)
- Exception queues

### Technology Stack
- **Framework**: Spring Boot 3.x with State Machine
- **Database**: Cloud Spanner (payment state tracking)
- **Messaging**: Kafka
- **Cache**: Redis (payment sessions)
- **Workflow**: Spring State Machine for payment orchestration

## Key Features

### Performance Requirements
- **Throughput**: 10+ TPS for outward payments
- **Latency**: <2 seconds for liquidity authorization
- **Availability**: 99.95% uptime
- **Success Rate**: >99.5% payment success (after liquidity approval)

### CTO Processing Flow
1. **Request Reception**: Receive payment request from PSP Global
2. **Initial Validation**: Validate payment details and format
3. **Liquidity Check**: Request authorization from fast-liquidity-service
4. **Authorization Decision**: 
   - **Approved**: Proceed with payment
   - **Insufficient Funds**: Hold payment for retry
   - **Rejected**: Return rejection to PSP Global
5. **Message Generation**: Create PACS.008 message
6. **Transmission**: Send to fast-sender-service
7. **Status Tracking**: Monitor payment until completion
8. **Final Update**: Update PSP Global with final status

### State Management
- **PENDING**: Initial payment state
- **LIQUIDITY_CHECK**: Awaiting liquidity authorization
- **AUTHORIZED**: Liquidity approved, ready to send
- **SENT**: Message sent to G3 Host
- **CONFIRMED**: Received PACS.002 acceptance
- **REJECTED**: Payment rejected
- **HELD**: Held due to insufficient funds
- **FAILED**: Technical failure

### Hold and Retry Logic
```yaml
hold_and_retry:
  max_hold_duration: "24h"
  retry_intervals: ["5m", "15m", "30m", "1h", "4h"]
  max_retry_attempts: 5
  priority_queue: true
```

## Configuration

### Environment Variables
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPANNER_INSTANCE=payment-gateway
SPANNER_DATABASE=outward-payments
PSP_GLOBAL_ENDPOINT=https://psp-global.anz.com/api
LIQUIDITY_SERVICE_URL=http://fast-liquidity-service:8080
SENDER_SERVICE_URL=http://fast-sender-service:8080
PAYMENT_TIMEOUT_MINUTES=30
HOLD_MAX_DURATION_HOURS=24
RETRY_QUEUE_SIZE=1000
```

### Business Rules
```yaml
validation:
  max_amount: 9999999.99
  min_amount: 0.01
  daily_limit: 50000000.00
  currency_codes: ["SGD", "USD"]
  
authorization:
  require_dual_approval: false
  high_value_threshold: 100000.00
  restricted_accounts: []
```

## APIs

### Health Check
```http
GET /health
Response: 200 OK
{
  "status": "UP",
  "components": {
    "spanner": "UP",
    "kafka": "UP",
    "liquidity-service": "UP",
    "sender-service": "UP"
  }
}
```

### Submit Payment (from PSP Global)
```http
POST /api/v1/payments/submit
Content-Type: application/json

{
  "paymentId": "PAY123456",
  "amount": "1000.00",
  "currency": "SGD",
  "debtorAccount": "12345678",
  "creditorAccount": "87654321",
  "creditorName": "John Doe",
  "remittanceInfo": "Invoice 12345",
  "priority": "NORMAL"
}
```

### Payment Status
```http
GET /api/v1/payments/{paymentId}/status
Response: 200 OK
{
  "paymentId": "PAY123456",
  "status": "CONFIRMED",
  "amount": "1000.00",
  "submittedAt": "2024-01-15T10:00:00Z",
  "authorizedAt": "2024-01-15T10:00:01Z",
  "sentAt": "2024-01-15T10:00:02Z",
  "confirmedAt": "2024-01-15T10:00:05Z"
}
```

### Held Payments
```http
GET /api/v1/payments/held
Response: 200 OK
{
  "heldPayments": [
    {
      "paymentId": "PAY123457",
      "amount": "5000.00",
      "heldSince": "2024-01-15T09:30:00Z",
      "nextRetry": "2024-01-15T09:35:00Z",
      "reason": "INSUFFICIENT_LIQUIDITY"
    }
  ]
}
```

## Monitoring

### Key Metrics
- `outward.payments.submitted.total` - Total payments submitted
- `outward.payments.authorized.total` - Liquidity authorized payments
- `outward.payments.sent.total` - Payments sent to G3 Host
- `outward.payments.confirmed.total` - Confirmed payments
- `outward.payments.held.total` - Payments currently held
- `outward.processing.duration.seconds` - End-to-end processing time

### Business Metrics
- Daily payment volume and value
- Liquidity authorization success rate
- Average hold time for insufficient funds
- Payment confirmation rate from G3 Host

### Critical Alerts
- Liquidity service unavailable
- High payment failure rate
- Hold queue approaching capacity
- Payment processing time SLA breach
- G3 Host confirmation delays

## Liquidity Integration

### Authorization Flow
1. Send liquidity check request to fast-liquidity-service
2. Include payment amount, currency, and account details
3. Receive authorization response:
   - **AUTHORIZED**: Proceed with payment
   - **INSUFFICIENT**: Hold payment
   - **REJECTED**: Permanent rejection
4. For held payments, periodically retry authorization

### Liquidity Request Format
```json
{
  "paymentId": "PAY123456",
  "amount": "1000.00",
  "currency": "SGD",
  "debtorAccount": "12345678",
  "requestId": "REQ789012",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

## Error Handling

### Liquidity Service Errors
- **Service Unavailable**: Hold payment for retry
- **Timeout**: Default to hold for manual review
- **Invalid Response**: Reject payment

### G3 Host Response Errors
- **PACS.002 Reject**: Update payment status and notify PSP Global
- **No Response**: Retry after timeout
- **Technical Error**: Route to exception queue

### State Recovery
- **Service Restart**: Recover in-flight payments from database
- **Corruption Detection**: Validate payment state consistency
- **Manual Intervention**: Operator interface for problem resolution

## Development

### Local Setup
```bash
cd services/fast-outward-clearing-processor
./mvnw spring-boot:run -Dspring.profiles.active=local
```

### Testing
```bash
# Unit tests
./mvnw test

# Integration tests with liquidity service mock
./mvnw integration-test

# State machine tests
./mvnw test -Dtest=StateMachineTest

# Load testing
./scripts/load-test.sh --payments=100 --concurrent=10
```

### Mock Dependencies
```bash
# Start mock liquidity service
docker run -p 8081:8080 liquidity-service-mock:latest

# Start mock sender service  
docker run -p 8082:8080 sender-service-mock:latest
```