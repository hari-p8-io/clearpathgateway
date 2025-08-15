# Fast Availability Service

## Overview

The Fast Availability Service manages participant bank status tracking and availability notifications. It monitors the operational status of all participating banks in the payment network and ensures service availability information is accurately maintained and distributed.

## Responsibilities

### Core Functions
- **Bank Status Tracking**: Monitor operational status of all participant banks
- **Availability Updates**: Process real-time availability status changes
- **Service Interruption Management**: Handle planned and unplanned service interruptions
- **Status Broadcasting**: Distribute availability information to relevant services
- **Historical Tracking**: Maintain availability history for reporting and analysis
- **Manual Override Support**: Allow authorized manual status changes
- **SNM Message Processing**: Handle Settlement Network Management messages

### Bank Status Types
- **AVAILABLE**: Bank fully operational for all payment types
- **RESTRICTED**: Limited operations (specific payment types unavailable)
- **UNAVAILABLE**: Bank temporarily offline
- **MAINTENANCE**: Planned maintenance window
- **SUSPENDED**: Regulatory or operational suspension

## Architecture

### Input Sources
- fast-router-service (SNM messages from G3 Host)
- Manual operator interfaces
- Scheduled maintenance systems
- External monitoring systems

### Output Targets
- All payment processing services (status notifications)
- Operational dashboards
- Regulatory reporting systems
- Customer notification systems

### Technology Stack
- **Framework**: Spring Boot 3.x
- **Database**: Cloud Spanner (status tracking)
- **Cache**: Redis (real-time status cache)
- **Messaging**: Kafka (status event broadcasting)
- **Monitoring**: Custom health check integrations

## Key Features

### Performance Requirements
- **Response Time**: <100ms for status queries
- **Availability**: 99.99% uptime
- **Update Latency**: <5 seconds for status propagation
- **Data Consistency**: Real-time status accuracy across all services

### Status Management Flow
1. **Status Change Detection**: Receive status update (automated or manual)
2. **Validation**: Verify status change is valid and authorized
3. **Impact Assessment**: Determine effect on ongoing payments
4. **Status Update**: Update central status repository
5. **Notification Broadcast**: Notify all dependent services
6. **Historical Logging**: Record status change for audit
7. **Dashboard Update**: Refresh operational displays

### SNM Message Handling
- **Bank Sign-on**: Process bank coming online
- **Bank Sign-off**: Handle bank going offline
- **Status Notifications**: Parse availability status messages
- **Settlement Messages**: Track settlement-related status changes

## Configuration

### Environment Variables
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPANNER_INSTANCE=payment-gateway
SPANNER_DATABASE=bank-availability
REDIS_HOST=localhost
REDIS_PORT=6379
STATUS_CACHE_TTL_SECONDS=300
NOTIFICATION_TIMEOUT_MS=5000
SNM_TOPIC=bank-status-messages
DEFAULT_STATUS_CHECK_INTERVAL=60
```

### Bank Configuration
```yaml
banks:
  ANZ_SG:
    bank_code: "ANZBSGSG"
    name: "ANZ Singapore"
    payment_types: ["CTI", "CTO", "DDI"]
    maintenance_window: "02:00-04:00"
    
  DBS_SG:
    bank_code: "DBSSSGSG"
    name: "DBS Singapore"
    payment_types: ["CTI", "CTO", "DDI"]
    maintenance_window: "01:00-03:00"
    
status_definitions:
  AVAILABLE:
    description: "Fully operational"
    allow_payments: true
    
  RESTRICTED:
    description: "Limited operations"
    allow_payments: ["CTI"]  # Only inward payments
    
  UNAVAILABLE:
    description: "Temporarily offline"
    allow_payments: false
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

### Get Bank Status
```http
GET /api/v1/banks/{bankCode}/status
Response: 200 OK
{
  "bankCode": "ANZBSGSG",
  "status": "AVAILABLE",
  "lastUpdated": "2024-01-15T10:00:00Z",
  "allowedPaymentTypes": ["CTI", "CTO", "DDI"],
  "nextMaintenanceWindow": "2024-01-16T02:00:00Z"
}
```

### Get All Bank Statuses
```http
GET /api/v1/banks/status
Response: 200 OK
{
  "banks": [
    {
      "bankCode": "ANZBSGSG",
      "status": "AVAILABLE",
      "lastUpdated": "2024-01-15T10:00:00Z"
    },
    {
      "bankCode": "DBSSSGSG", 
      "status": "MAINTENANCE",
      "lastUpdated": "2024-01-15T09:45:00Z",
      "estimatedReturn": "2024-01-15T11:00:00Z"
    }
  ],
  "timestamp": "2024-01-15T10:01:00Z"
}
```

### Update Bank Status (Manual)
```http
PUT /api/v1/banks/{bankCode}/status
Content-Type: application/json
Authorization: Bearer {operatorToken}

{
  "status": "MAINTENANCE",
  "reason": "Scheduled system upgrade",
  "estimatedDuration": "2h",
  "operatorId": "OP12345",
  "allowedPaymentTypes": []
}
```

### Bank Status History
```http
GET /api/v1/banks/{bankCode}/history?from=2024-01-01&to=2024-01-15
Response: 200 OK
{
  "bankCode": "ANZBSGSG",
  "statusHistory": [
    {
      "status": "MAINTENANCE",
      "startTime": "2024-01-15T02:00:00Z",
      "endTime": "2024-01-15T04:00:00Z",
      "reason": "Scheduled maintenance",
      "duration": "2h"
    }
  ]
}
```

## Monitoring

### Key Metrics
- `availability.banks.total` - Total number of banks
- `availability.banks.available.total` - Banks currently available
- `availability.banks.unavailable.total` - Banks currently unavailable
- `availability.status.changes.total` - Total status changes
- `availability.notification.duration.seconds` - Notification propagation time
- `availability.uptime.ratio` - Per-bank uptime percentage

### Business Metrics
- Network availability percentage
- Average bank downtime per month
- Status change frequency
- Maintenance window compliance
- Payment impact from bank outages

### Critical Alerts
- Multiple banks unavailable simultaneously
- Bank status notification failures
- Extended maintenance windows
- Unplanned outages
- Status propagation delays

## Event Processing

### Status Change Events
```json
{
  "eventType": "BANK_STATUS_CHANGED",
  "bankCode": "ANZBSGSG",
  "previousStatus": "AVAILABLE",
  "newStatus": "MAINTENANCE",
  "timestamp": "2024-01-15T10:00:00Z",
  "reason": "Scheduled maintenance",
  "operatorId": "SYSTEM",
  "estimatedDuration": "2h"
}
```

### SNM Message Processing
- **Parse**: Extract status information from SNM messages
- **Validate**: Verify message authenticity and format
- **Map**: Convert SNM codes to internal status values
- **Update**: Apply status changes to bank records
- **Broadcast**: Notify dependent services

## Integration Points

### Payment Services Integration
- **fast-router-service**: Route messages only to available banks
- **fast-inward-clearing-processor**: Check sender bank availability
- **fast-outward-clearing-processor**: Verify recipient bank status
- **fast-sender-service**: Queue messages for unavailable banks

### Operational Integration
- **Monitoring Dashboards**: Real-time network status display
- **Alerting Systems**: Automated incident notifications
- **Reporting**: Daily/monthly availability reports
- **Change Management**: Integration with planned maintenance systems

## Error Handling

### Status Update Failures
- **Database Error**: Retry with exponential backoff
- **Notification Failure**: Queue for retry, alert operations
- **Invalid Status**: Reject with detailed error message
- **Authorization Error**: Log security event, reject update

### Recovery Procedures
- **Service Restart**: Reload bank status from persistent storage
- **Cache Inconsistency**: Rebuild cache from authoritative source
- **Message Queue Backup**: Process queued notifications in order
- **Manual Recovery**: Operator tools for status correction

## Development

### Local Setup
```bash
cd services/fast-availability-service
./mvnw spring-boot:run -Dspring.profiles.active=local
```

### Testing
```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw integration-test

# SNM message processing tests
./mvnw test -Dtest=SNMProcessingTest

# Status propagation tests
./scripts/test-status-propagation.sh
```

### Mock Bank Status
```bash
# Simulate bank status changes
./scripts/simulate-bank-status.sh --bank=ANZBSGSG --status=MAINTENANCE --duration=2h
```