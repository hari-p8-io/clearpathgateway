# Fast Liquidity Service

Multi-region liquidity management service for APEAFAST-SG ClearPath Gateway. Provides real-time balance management, authorization checks, and transaction processing for Singapore G3 and Hong Kong FPS payment schemes.

## Overview

The Fast Liquidity Service is a critical component of the APEAFAST-SG payment gateway, responsible for:
- Real-time liquidity balance checking and authorization
- Multi-country payment scheme support (SG=G3, HK=FPS)
- ISO 20022 message processing (PACS/CAMT)
- Net debit cap monitoring and enforcement
- Transaction history and audit trails

## Architecture

### Tech Stack
- **Java 21** with Virtual Threads (Project Loom)
- **Spring Boot 3.2.1** with WebFlux
- **Google Cloud Spanner** for transactional data
- **Redis** for caching and session management
- **Apache Kafka** for event streaming
- **Docker** for containerization

### Key Features
- **High Performance**: Virtual Threads for improved concurrency
- **Multi-Region**: Supports Singapore G3 and Hong Kong FPS
- **Real-time**: Sub-second balance checks and updates
- **Resilient**: Circuit breakers and retry mechanisms
- **Observable**: Comprehensive monitoring and tracing

## API Documentation

The service exposes RESTful APIs documented with OpenAPI 3.0.3 specification:

### Core Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/liquidity/balance/check` | POST | Check balance authorization |
| `/liquidity/balance/update` | POST | Update participant balance |
| `/liquidity/balance/{participantId}` | GET | Get participant balance |
| `/health` | GET | Service health check |

### API Documentation
- **Swagger UI**: `http://localhost:8084/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8084/api-docs`
- **Spec File**: [openapi.yaml](api/openapi.yaml)

## Quick Start

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose
- Redis (for caching)
- Kafka (for messaging)
- Google Cloud Spanner (or emulator)

### Local Development

1. **Start Infrastructure**:
   ```bash
   cd ../../
   docker-compose up -d redis kafka spanner-emulator
   ```

2. **Run the Service**:
   ```bash
   mvn spring-boot:run
   ```

3. **Test the API**:
   ```bash
   curl -X POST http://localhost:8084/liquidity/balance/check \
     -H "Content-Type: application/json" \
     -d '{
       "countryCode": "SG",
       "currency": "SGD",
       "amount": "-50000.00",
       "transactionType": "DEBIT"
     }'
   ```

### Docker

1. **Build Image**:
   ```bash
   docker build -t fast-liquidity-service .
   ```

2. **Run Container**:
   ```bash
   docker run -p 8084:8084 \
     -e SPRING_PROFILES_ACTIVE=docker \
     fast-liquidity-service
   ```

## Configuration

### Application Profiles
- **local**: Local development with emulators
- **docker**: Docker environment
- **production**: Production environment

### Key Configuration Properties
```yaml
server:
  port: 8084

liquidity:
  supported-schemes:
    SG: G3
    HK: FPS
  thresholds:
    warning-percentage: 0.8
    critical-percentage: 0.95
  net-debit-cap:
    default-limit: 5000000.00
    monitoring-enabled: true
```

## Payment Schemes

### Singapore G3
- **Regulator**: Monetary Authority of Singapore (MAS)
- **Operating Hours**: 24/7
- **Currency**: SGD
- **Settlement**: Real-time gross settlement (RTGS)

### Hong Kong FPS
- **Regulator**: Hong Kong Monetary Authority (HKMA)
- **Operating Hours**: 24/7
- **Currency**: HKD
- **Settlement**: Real-time gross settlement (RTGS)

## Net Debit Cap Management

The service enforces net debit caps to manage liquidity risk:

- **Real-time Monitoring**: Continuous tracking of participant positions
- **Threshold Alerts**: Configurable warning and critical thresholds
- **Automatic Rejection**: Transactions exceeding limits are automatically rejected
- **Compliance Reporting**: Audit trails for regulatory compliance

## Message Types Supported

| Message Type | Description | Flow |
|--------------|-------------|------|
| pacs.008.001.08 | Customer Credit Transfer | Outbound |
| pacs.002.001.10 | Payment Status Report | Inbound |
| pacs.003.001.07 | Customer Direct Debit | Outbound |
| camt.054.001.08 | Bank to Customer Debit/Credit Notification | Inbound |
| camt.052.001.08 | Bank to Customer Account Report | Inbound |
| camt.053.001.08 | Bank to Customer Statement | Inbound |

## Monitoring & Observability

### Health Checks
- **Application Health**: `/health`
- **Dependency Status**: Database, Kafka, Redis, Spanner
- **Custom Metrics**: Balance thresholds, transaction rates

### Metrics
- **Prometheus**: `http://localhost:8084/actuator/prometheus`
- **Custom Metrics**: 
  - `liquidity.balance.checks.total`
  - `liquidity.balance.updates.total`
  - `liquidity.netdebitcap.utilization`

### Logging
- **Structured Logging**: JSON format with correlation IDs
- **Log Levels**: Configurable per package
- **Audit Trail**: Complete transaction history

## Security

### Authentication
- **JWT Tokens**: Service-to-service authentication
- **API Keys**: External client authentication
- **Mutual TLS**: Transport layer security (infrastructure level)

### Authorization
- **Role-based Access**: Different access levels
- **Participant Isolation**: Cross-participant access prevention
- **Audit Logging**: All access logged and monitored

## Error Handling

### Error Codes
| Code | Description |
|------|-------------|
| `INSUFFICIENT_FUNDS` | Not enough liquidity |
| `NET_DEBIT_CAP_EXCEEDED` | Exceeds net debit cap |
| `DUPLICATE_TRANSACTION` | Transaction already processed |
| `INVALID_PARTICIPANT` | Unknown participant |
| `SCHEME_NOT_SUPPORTED` | Unsupported payment scheme |

### Retry Logic
- **Exponential Backoff**: For transient failures
- **Circuit Breakers**: Fail-fast for degraded services
- **Dead Letter Queues**: For failed messages

## Development

### Project Structure
```
src/
├── main/java/com/anz/fastpayment/liquidity/
│   ├── controller/          # REST API controllers
│   ├── service/            # Business logic interfaces
│   ├── service/impl/       # Business logic implementations
│   ├── model/              # Request/Response models
│   ├── repository/         # Data access layer
│   └── config/             # Configuration classes
├── main/resources/
│   ├── application.yml     # Application configuration
│   └── db/migration/       # Database migrations
└── test/java/              # Unit and integration tests
```

### Building
```bash
# Clean build
mvn clean compile

# Run tests
mvn test

# Package
mvn package

# Build Docker image
mvn jib:dockerBuild
```

### Testing
```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Test with Testcontainers
mvn test -Dspring.profiles.active=test
```

## Deployment

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `local` |
| `SPANNER_INSTANCE_ID` | Spanner instance | - |
| `SPANNER_DATABASE` | Spanner database | - |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `REDIS_HOST` | Redis host | `localhost` |
| `JWT_SECRET` | JWT signing key | - |

### Health Checks
- **Readiness**: Service is ready to accept traffic
- **Liveness**: Service is running and healthy
- **Startup**: Service has completed initialization

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Submit a pull request

### Code Standards
- **Java 21** language features
- **Spring Boot** best practices
- **OpenAPI 3.0** for API documentation
- **JUnit 5** for testing
- **SLF4J** for logging

## License

Proprietary - ANZ Banking Group Limited

---

For more information, contact the APEAFAST-SG team at [apeafast-sg@anz.com](mailto:apeafast-sg@anz.com)