# Fast Inward Clearing Processor

## Overview

The Fast Inward Clearing Processor handles all inbound payment processing for Credit Transfer Inward (CTI) and Direct Debit Inward (DDI) transactions. This service must meet the critical 4.5-second SLA requirement for payment processing.

## Responsibilities

### Core Functions
- **CTI Processing**: Handle PACS.008 Credit Transfer Initiation messages
- **DDI Processing**: Handle PACS.003 Direct Debit Initiation messages
- **Account Validation**: Verify recipient account details via VAM/MIDANZ
- **Business Rule Validation**: Apply payment scheme rules and fraud checks
- **Liquidity Management**: Coordinate with fast-liquidity-service for DDI authorization
- **Response Generation**: Create PACS.002 Payment Status Reports
- **Exception Handling**: Process CAMT.056 cancelations and PACS.007 reversals

### Payment Types
- **Credit Transfer Inward (CTI)**: Customer receives money
- **Direct Debit Inward (DDI)**: Customer account is debited

## Architecture

### Input Sources
- fast-router-service (routed messages)
- PSP APEA FAST Business Services

### Output Targets
- fast-sender-service (for responses)
- fast-liquidity-service (for liquidity updates)
- VAM/MIDANZ (for account operations)
- Audit/Event streams

### Technology Stack
- **Framework**: Spring Boot 3.x with WebFlux (reactive)
- **Database**: Cloud Spanner (transactional consistency)
- **Messaging**: Kafka
- **Cache**: Redis (for session state)
- **Monitoring**: Micrometer, Jaeger tracing

## Key Features

### Performance Requirements
- **SLA**: 4.5 seconds end-to-end processing
- **Throughput**: 15+ TPS for inward payments
- **Availability**: 99.95% uptime
- **Success Rate**: >99.9% transaction success

### CTI Processing Flow
1. **Message Validation**: Validate PACS.008 message structure
2. **Account Lookup**: Verify recipient account via VAM
3. **Business Rules**: Apply scheme-specific validation
4. **Account Credit**: Post credit to customer account
5. **Response**: Generate PACS.002 acceptance response
6. **Notification**: Trigger customer notification
7. **Audit**: Complete audit trail logging

### DDI Processing Flow
1. **Message Validation**: Validate PACS.003 message structure
2. **Account Verification**: Check customer account exists and is active
3. **Liquidity Check**: Verify sufficient customer balance
4. **Authorization**: Pre-authorization check via fast-liquidity-service
5. **Account Debit**: Process customer account debit
6. **Liquidity Update**: Update net debit cap (increases bank liquidity)
7. **Response**: Generate PACS.002 response (accept/reject)
8. **Settlement**: Include in settlement cycle

### Timer Management
- **SLA Timer**: 4.5-second countdown from message receipt
- **Timeout Detection**: Automatic timeout handling
- **Performance Monitoring**: Real-time SLA compliance tracking

## Configuration

### Environment Variables
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPANNER_INSTANCE=payment-gateway
SPANNER_DATABASE=transactions
VAM_ENDPOINT=https://vam.anz.com/api
MIDANZ_ENDPOINT=https://midanz.anz.com/api
SLA_TIMEOUT_MS=4500
MAX_RETRY_ATTEMPTS=3
CIRCUIT_BREAKER_THRESHOLD=5
```

### Business Rules
```yaml
validation:
  max_amount: 999999.99
  min_amount: 0.01
  blocked_accounts: []
  restricted_currencies: []
  
fraud_checks:
  enabled: true
  threshold_amount: 10000.00
  velocity_checks: true
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
    "vam": "UP",
    "midanz": "UP"
  }
}
```

### Process Payment (Internal)
```http
POST /api/v1/payments/process
Content-Type: application/json

{
  "messageType": "PACS.008",
  "messageId": "MSG123456",
  "amount": "1000.00",
  "currency": "SGD",
  "debtorAccount": "12345678",
  "creditorAccount": "87654321"
}
```

## Monitoring

### Key Metrics
- `processor.payments.processed.total` - Total payments processed
- `processor.payments.success.total` - Successful payments
- `processor.payments.failed.total` - Failed payments
- `processor.sla.compliance.ratio` - SLA compliance percentage
- `processor.processing.duration.seconds` - Processing time distribution
- `processor.account.validation.duration.seconds` - Account validation time

### Critical Alerts
- SLA breach (>4.5 seconds processing time)
- Error rate > 0.1%
- Account service connectivity issues
- Liquidity service timeouts
- Circuit breaker activated

### Business Metrics
- Daily transaction volume
- Peak TPS achieved
- Account validation success rate
- Liquidity authorization success rate

## Error Handling

### Timeout Scenarios
- **Account Service Timeout**: Return temporary reject with retry
- **Liquidity Service Timeout**: Hold payment for manual review
- **Database Timeout**: Return technical reject

### Retry Logic
- **Transient Errors**: Exponential backoff (max 3 attempts)
- **Account Unavailable**: Hold and retry every 30 seconds
- **System Errors**: Dead letter queue for manual intervention

## Development

### Local Setup
```bash
cd services/fast-inward-clearing-processor
./mvnw spring-boot:run -Dspring.profiles.active=local
```

### Testing
```bash
# Unit tests
./mvnw test

# Integration tests with test containers
./mvnw integration-test

# Performance tests
./mvnw test -Dtest=PerformanceTest
```

### Load Testing
```bash
# Test SLA compliance under load
./scripts/load-test.sh --tps=20 --duration=300s
```