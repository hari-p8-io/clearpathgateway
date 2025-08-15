# Shared Common Library

## Overview

This library contains common utilities, DTOs, and configurations shared across all Fast Payment Gateway services.

## Contents

- **Common DTOs**: Shared data transfer objects for payment messages
- **Security Utilities**: Authentication and authorization helpers
- **Configuration**: Common configuration classes
- **Constants**: System-wide constants and enums
- **Utilities**: Helper classes and functions

## Usage

Include this library as a dependency in your service:

```xml
<dependency>
    <groupId>com.anz.fastpayment</groupId>
    <artifactId>fast-payment-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Key Components

### Payment Message DTOs
- `PaymentMessage`: Base payment message structure
- `PACS008Message`: Credit transfer initiation
- `PACS003Message`: Direct debit initiation
- `PACS002Message`: Payment status report

### Common Enums
- `PaymentStatus`: Payment processing status
- `MessageType`: ISO 20022 message types
- `Currency`: Supported currency codes
- `BankStatus`: Bank availability status

### Security
- `JwtUtil`: JWT token utilities
- `EncryptionUtil`: Data encryption helpers
- `AuditHelper`: Audit logging utilities

### Configuration
- `KafkaConfig`: Common Kafka configuration
- `SecurityConfig`: Security configuration
- `MetricsConfig`: Monitoring configuration