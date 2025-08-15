# Fast Liquidity Service

## Overview

The Fast Liquidity Service is the core liquidity management component that handles real-time liquidity tracking, net debit cap monitoring, and authorization decisions for both outward payments and direct debit transactions. It ensures compliance with regulatory requirements and optimizes liquidity utilization.

## Responsibilities

### Core Functions
- **Real-time Liquidity Tracking**: Continuous balance calculation and monitoring
- **Net Debit Cap Management**: Monitor and enforce regulatory net debit limits
- **Authorization Engine**: Pre-transaction liquidity authorization decisions
- **DDI Impact Tracking**: Track how direct debits affect available liquidity
- **Settlement Integration**: Twice-daily settlement cycle support
- **Hold and Release**: Manage liquidity reservations for pending payments
- **Threshold Management**: Configurable warning levels and alerts
- **Cross-border Optimization**: Multi-currency liquidity management

### Key Concepts
- **Net Debit Cap**: Maximum negative position allowed with central bank
- **Available Liquidity**: Current available funds for outward payments
- **Reserved Liquidity**: Funds held for pending transactions
- **DDI Credit**: Liquidity gained from customer direct debit collections

## Architecture

### Input Sources
- fast-outward-clearing-processor (authorization requests)
- fast-inward-clearing-processor (DDI and CTI updates)
- Settlement systems (twice-daily settlements)
- Core banking systems (account updates)

### Output Targets
- Authorization responses to payment processors
- Liquidity alerts and notifications
- Regulatory reporting systems
- Operational dashboards

### Technology Stack
- **Framework**: Spring Boot 3.x with WebFlux (reactive)
- **Database**: Cloud Spanner (ACID transactions for liquidity)
- **Cache**: Redis (real-time balance cache)
- **Messaging**: Kafka (liquidity events)
- **Analytics**: BigQuery (liquidity reporting)

## Key Features

### Performance Requirements
- **Response Time**: <200ms for authorization decisions
- **Throughput**: 50+ TPS authorization requests
- **Availability**: 99.99% uptime (critical for payment flow)
- **Consistency**: ACID compliance for all liquidity operations

### Liquidity Calculation Model
```
Available Liquidity = Opening Balance 
                    + Inward Payments (CTI)
                    + Direct Debit Collections (DDI)
                    + Settlement Credits
                    - Outward Payments (CTO)
                    - Reserved for Pending Payments
                    - Regulatory Buffer
```

### Authorization Flow
1. **Receive Request**: Authorization request from payment processor
2. **Calculate Impact**: Determine liquidity impact of transaction
3. **Check Availability**: Verify sufficient liquidity exists
4. **Regulatory Check**: Ensure net debit cap compliance
5. **Reserve Funds**: Hold liquidity for approved transactions
6. **Respond**: Return authorization decision
7. **Track State**: Update liquidity position

### DDI Special Handling
- **Customer Debit**: DDI increases bank's available liquidity
- **Pre-authorization**: Check customer account balance before DDI
- **Real-time Impact**: Immediate liquidity position update
- **Settlement Timing**: Consider settlement cycle effects

## Configuration

### Environment Variables
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPANNER_INSTANCE=payment-gateway
SPANNER_DATABASE=liquidity
REDIS_HOST=localhost
REDIS_PORT=6379
NET_DEBIT_CAP_SGD=500000000.00
REGULATORY_BUFFER_SGD=50000000.00
SETTLEMENT_TIMES=["09:00", "16:00"]
WARNING_THRESHOLD=0.8
CRITICAL_THRESHOLD=0.9
```

### Net Debit Cap Configuration
```yaml
net_debit_cap:
  SGD: 500000000.00  # 500M SGD limit
  USD: 100000000.00  # 100M USD limit
  
thresholds:
  warning: 0.8       # 80% utilization warning
  critical: 0.9      # 90% utilization critical alert
  emergency: 0.95    # 95% emergency procedures
  
buffers:
  regulatory: 50000000.00  # Required regulatory buffer
  operational: 25000000.00 # Operational safety buffer
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
    "redis": "UP",
    "kafka": "UP"
  }
}
```

### Authorize Payment
```http
POST /api/v1/liquidity/authorize
Content-Type: application/json

{
  "paymentId": "PAY123456",
  "amount": "1000000.00",
  "currency": "SGD",
  "type": "CTO",
  "debtorAccount": "12345678",
  "priority": "NORMAL"
}

Response: 200 OK
{
  "authorizationId": "AUTH789012",
  "decision": "AUTHORIZED",
  "availableLiquidity": "495000000.00",
  "utilizationRatio": 0.75,
  "expiresAt": "2024-01-15T10:15:00Z"
}
```

### Current Liquidity Position
```http
GET /api/v1/liquidity/position
Response: 200 OK
{
  "currency": "SGD",
  "availableLiquidity": "495000000.00",
  "netDebitCap": "500000000.00",
  "utilizationRatio": 0.75,
  "reservedAmount": "25000000.00",
  "pendingPayments": 45,
  "lastUpdated": "2024-01-15T10:00:00Z"
}
```

### Release Authorization
```http
POST /api/v1/liquidity/release/{authorizationId}
Response: 200 OK
{
  "authorizationId": "AUTH789012",
  "status": "RELEASED",
  "releasedAmount": "1000000.00"
}
```

## Monitoring

### Key Metrics
- `liquidity.available.amount` - Current available liquidity
- `liquidity.utilization.ratio` - Net debit cap utilization
- `liquidity.authorizations.total` - Total authorization requests
- `liquidity.authorizations.approved.total` - Approved authorizations
- `liquidity.authorizations.rejected.total` - Rejected authorizations
- `liquidity.response.duration.seconds` - Authorization response time
- `liquidity.reserved.amount` - Currently reserved liquidity

### Business Metrics
- Peak liquidity utilization
- Daily authorization volume
- Rejection rate by reason
- Settlement impact analysis
- DDI vs CTO liquidity flow

### Critical Alerts
- Net debit cap utilization > 80%
- Available liquidity below operational threshold
- Authorization service response time > 200ms
- High rejection rate (>5%)
- Settlement processing delays
- Redis cache connectivity issues

## Liquidity Management

### Real-time Updates
- **CTI Receipt**: Increase available liquidity immediately
- **DDI Processing**: Increase available liquidity (customer pays bank)
- **CTO Authorization**: Reserve liquidity for pending payment
- **CTO Confirmation**: Deduct from available liquidity
- **Payment Failure**: Release reserved liquidity

### Settlement Integration
```yaml
settlement:
  morning:
    time: "09:00"
    type: "bilateral"
    currencies: ["SGD"]
    
  afternoon:
    time: "16:00" 
    type: "multilateral"
    currencies: ["SGD", "USD"]
```

### Hold and Retry Support
- **Insufficient Funds**: Return rejection with retry recommendation
- **Temporary Hold**: Reserve partial liquidity for high-priority payments
- **Queue Management**: Priority-based authorization for held payments
- **Liquidity Recovery**: Automatic retry when liquidity improves

## Risk Management

### Regulatory Compliance
- **MAS Requirements**: Singapore central bank regulations
- **Net Debit Cap**: Real-time monitoring and enforcement
- **Reporting**: Automated regulatory report generation
- **Audit Trail**: Complete liquidity movement tracking

### Risk Controls
- **Concentration Limits**: Per-counterparty exposure limits
- **Velocity Checks**: Unusual liquidity movement detection
- **Circuit Breakers**: Automatic stops for anomalous activity
- **Manual Overrides**: Authorized operator interventions

## Development

### Local Setup
```bash
cd services/fast-liquidity-service
./mvnw spring-boot:run -Dspring.profiles.active=local
```

### Testing
```bash
# Unit tests
./mvnw test

# Integration tests with Spanner
./mvnw integration-test

# Liquidity simulation tests
./mvnw test -Dtest=LiquiditySimulationTest

# Performance tests
./scripts/performance-test.sh --concurrent=50 --duration=300s
```

### Liquidity Simulation
```bash
# Simulate payment flows
./scripts/simulate-liquidity.sh --scenario=peak-load
./scripts/simulate-liquidity.sh --scenario=settlement-cycle
```