# Fast Router Service

## Overview

The Fast Router Service is the entry point for all messages from the CPG/G3 Host system. It handles message reception, initial validation, parsing, and routing decisions based on message type.

## Responsibilities

### Core Functions
- **Message Reception**: Receive messages from CPG/G3 Host
- **Initial Validation**: Basic message format and structure validation
- **Message Parsing**: Parse incoming messages and extract routing information
- **Routing Decisions**: Determine target service based on message type and content
- **Duplicate Detection**: Identify and handle duplicate messages
- **Message Enrichment**: Add correlation IDs, timestamps, and audit trail information
- **MUID Generation**: Generate Message Unit IDs for tracking

### Message Types Handled
- **PACS.008**: Credit Transfer Initiation (routes to fast-inward-clearing-processor)
- **PACS.003**: Direct Debit Initiation (routes to fast-inward-clearing-processor)  
- **CAMT.056**: Cancelation Request (routes to appropriate processor)
- **PACS.007**: Reversal Request (routes to appropriate processor)
- **SNM Messages**: Settlement and Bank Status messages (routes to fast-availability-service)

## Architecture

### Input Sources
- CPG (Clearing Payment Gateway)
- G3 Host integration

### Output Targets
- fast-inward-clearing-processor (for inbound payments)
- fast-availability-service (for bank status updates)
- Exception queues (for invalid messages)

### Technology Stack
- **Framework**: Spring Boot 3.x
- **Messaging**: Kafka Producer/Consumer
- **Database**: Redis (for duplicate detection cache)
- **Monitoring**: Micrometer metrics, Zipkin tracing

## Key Features

### Performance Requirements
- **SLA**: Sub-second routing decisions
- **Throughput**: 20+ TPS message routing
- **Availability**: 99.95% uptime

### Message Processing Flow
1. **Receive**: Accept message from CPG/G3 Host
2. **Validate**: Basic structure and format validation
3. **Parse**: Extract message type and routing information
4. **Duplicate Check**: Check against recent message cache
5. **Enrich**: Add MUID, correlation ID, timestamp
6. **Route**: Send to appropriate downstream service
7. **Audit**: Log processing details for compliance

### Error Handling
- Invalid message format → Exception queue
- Duplicate detection → Idempotent response
- Downstream service unavailable → Retry with exponential backoff
- Timeout → Dead letter queue

## Configuration

### Environment Variables
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
CPG_ENDPOINT=https://cpg.anz.com/api
G3_HOST_ENDPOINT=https://g3host.anz.com/api
MESSAGE_TIMEOUT_MS=4500
DUPLICATE_CACHE_TTL_MINUTES=60
```

### Message Routing Rules
```yaml
routing:
  PACS.008: fast-inward-clearing-processor
  PACS.003: fast-inward-clearing-processor
  CAMT.056: fast-inward-clearing-processor
  PACS.007: fast-inward-clearing-processor
  SNM.*: fast-availability-service
  default: exception-queue
```

## APIs

### Health Check
```http
GET /health
Response: 200 OK
{
  "status": "UP",
  "components": {
    "kafka": "UP",
    "redis": "UP",
    "cpg": "UP"
  }
}
```

### Metrics
```http
GET /actuator/metrics
```

## Monitoring

### Key Metrics
- `router.messages.received.total` - Total messages received
- `router.messages.routed.total` - Total messages successfully routed
- `router.messages.failed.total` - Total failed messages
- `router.processing.duration.seconds` - Processing time distribution
- `router.duplicate.detected.total` - Duplicate messages detected

### Alerts
- Processing time > 1 second
- Error rate > 1%
- Duplicate rate > 5%
- Downstream service connectivity issues

## Development

### Local Setup
```bash
cd services/fast-router-service
./mvnw spring-boot:run
```

### Testing
```bash
./mvnw test
./mvnw integration-test
```

### Docker
```bash
docker build -t fast-router-service .
docker run -p 8080:8080 fast-router-service
```