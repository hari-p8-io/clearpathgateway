# Environment Variables Configuration

This document describes the environment variables used by the Clear Path Gateway services.

## Kafka Configuration

### KAFKA_BOOTSTRAP_SERVERS
- **Description**: Kafka bootstrap servers connection string
- **Default**: `kafka:29092` (for Docker Compose)
- **Example**: `kafka:29092` or `localhost:9092` (for local development)
- **Used by**: All services that produce/consume Kafka messages

### SCHEMA_REGISTRY_URL
- **Description**: URL for the Avro Schema Registry
- **Default**: `http://schema-registry:8081` (for Docker Compose)
- **Example**: `http://localhost:8081` (for local development)
- **Used by**: All services that produce/consume Avro messages

## Service Configuration

### SPRING_PROFILES_ACTIVE
- **Description**: Spring Boot profile to activate
- **Default**: `local`
- **Options**: `local`, `docker`, `production`
- **Used by**: All Spring Boot services

### DATABASE_URL
- **Description**: Database connection URL
- **Default**: `jdbc:postgresql://postgres:5432/payment_gateway`
- **Used by**: Services that require database access

### DATABASE_USERNAME
- **Description**: Database username
- **Default**: `dev_user`
- **Used by**: Services that require database access

### DATABASE_PASSWORD
- **Description**: Database password
- **Default**: `dev_password`
- **Used by**: Services that require database access

## Redis Configuration

### REDIS_HOST
- **Description**: Redis server hostname
- **Default**: `redis` (for Docker Compose), `localhost` (for local development)
- **Used by**: Services that use Redis for caching

### REDIS_PORT
- **Description**: Redis server port
- **Default**: `6379`
- **Used by**: Services that use Redis for caching

## External Service URLs

### FAST_ROUTER_SERVICE_URL
- **Description**: URL for the Fast Router Service
- **Default**: `http://fast-router-service:8080`
- **Used by**: Services that communicate with the router

### FAST_LIQUIDITY_SERVICE_URL
- **Description**: URL for the Fast Liquidity Service
- **Default**: `http://fast-liquidity-service:8080`
- **Used by**: Services that require liquidity checks

### G3_HOST_ENDPOINT
- **Description**: G3 Host system endpoint
- **Default**: `http://localhost:8443/g3host` (local), `https://g3host.anz.com/api` (production)
- **Used by**: Services that communicate with G3 Host

### CPG_ENDPOINT
- **Description**: CPG Gateway endpoint
- **Default**: `http://localhost:8444/cpg` (local), `https://cpg.anz.com/gateway` (production)
- **Used by**: Services that communicate with CPG

## Security Configuration

### JWT_SECRET
- **Description**: Secret key for JWT token signing
- **Default**: `dev-secret-key-change-in-production`
- **Used by**: Services that handle authentication

### MTLS_ENABLED
- **Description**: Enable mutual TLS authentication
- **Default**: `false`
- **Used by**: Services that support mTLS

## SLA Configuration

### INWARD_PAYMENT_SLA_SECONDS
- **Description**: SLA timeout for inward payment processing
- **Default**: `4.5`
- **Used by**: Inward clearing processor

### OUTWARD_PAYMENT_SLA_SECONDS
- **Description**: SLA timeout for outward payment processing
- **Default**: `2.0`
- **Used by**: Outward clearing processor

## Liquidity Configuration

### NET_DEBIT_CAP_SGD
- **Description**: Net debit cap for SGD transactions
- **Default**: `500000000.00`
- **Used by**: Liquidity service

### REGULATORY_BUFFER_SGD
- **Description**: Regulatory buffer for SGD transactions
- **Default**: `50000000.00`
- **Used by**: Liquidity service

## Environment-Specific Configuration

### Local Development
```bash
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SCHEMA_REGISTRY_URL=http://localhost:8081
export REDIS_HOST=localhost
export DATABASE_URL=jdbc:postgresql://localhost:5432/payment_gateway
```

### Docker Compose
```bash
export KAFKA_BOOTSTRAP_SERVERS=kafka:29092
export SCHEMA_REGISTRY_URL=http://schema-registry:8081
export REDIS_HOST=redis
export DATABASE_URL=jdbc:postgresql://postgres:5432/payment_gateway
```

### Production
```bash
export KAFKA_BOOTSTRAP_SERVERS=kafka-cluster:9092
export SCHEMA_REGISTRY_URL=https://schema-registry.company.com
export REDIS_HOST=redis-cluster.company.com
export DATABASE_URL=jdbc:postgresql://db-cluster.company.com:5432/payment_gateway
```

## Configuration Files

The environment variables are configured in:
- `docker-compose.yml` - For Docker Compose environment
- `scripts/dev-setup.sh` - Generates `.env.template`
- `services/*/src/main/resources/application.yml` - Service-specific configuration
- `services/*/k8s/deployment.yaml` - Kubernetes deployment configuration

## Notes

- All services now use environment variables for Kafka and Schema Registry configuration
- The default values are set for Docker Compose environment
- Local development requires setting appropriate localhost values
- Production deployments should override all sensitive configuration values
- Schema Registry URL is required for all services that handle Avro messages
