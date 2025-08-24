# 🚀 Kafka + Avro Setup Guide

## 📋 **What This Guide Contains**
- Docker infrastructure setup
- Maven dependencies
- Application configuration
- Avro schema files
- Basic usage commands
- **Testing with Playwright**

## 🐳 **Docker Infrastructure**

### Start Services
```bash
docker-compose up -d
```

**Services:**
- Zookeeper (port 2181)
- Kafka (port 9092) 
- Schema Registry (port 8081)

## 📦 **Maven Dependencies**

### Required Dependencies
```xml
<!-- Avro -->
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.11.3</version>
</dependency>

<!-- Confluent Kafka Avro Serializer -->
<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-avro-serializer</artifactId>
    <version>7.3.0</version>
</dependency>

<dependency>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-schema-registry-client</artifactId>
    <version>7.3.0</version>
</dependency>
```

### Confluent Repository
```xml
<repository>
    <id>confluent</id>
    <url>https://packages.confluent.io/maven/</url>
</repository>
```

## ⚙️ **Application Configuration**

### application.yml
```yaml
spring:
  kafka:
    producer:
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      properties:
        schema.registry.url: http://localhost:8081
    consumer:
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: http://localhost:8081
        specific.avro.reader: false
```

## 📋 **Avro Schema Files**

### Location
```
src/main/resources/avro/
├── UnifiedPaymentMessage.avsc
└── ProcessedTransactionMessage.avsc
```

## 🧪 **Testing with Playwright**

### Test Package Location
```
playwright/
├── package.json          # Dependencies and scripts
├── playwright.config.ts  # Playwright configuration
├── tsconfig.json         # TypeScript configuration
├── tests/                # Test files
│   └── kafka-avro.spec.ts
├── README.md             # Test package documentation
└── run-tests.sh         # Test runner script
```

### Quick Test Setup
```bash
cd playwright
npm install
npx playwright install
npm run docker:up
npm test
```

### Test Coverage
- ✅ Kafka connectivity validation
- ✅ Avro message serialization/deserialization
- ✅ Schema Registry integration
- ✅ Message production and consumption
- ✅ Error handling scenarios

## 🚀 **Usage Commands**

### Build Service
```bash
cd services/fast-inward-clearing-processor
mvn clean compile
```

### Run Service
```bash
mvn spring-boot:run
```

### Check Topics
```bash
kafka-topics --list --bootstrap-server localhost:9092
```

### Monitor Schema Registry
```
http://localhost:8081
```

## 📊 **Kafka Topics**
- **Input**: `transactions.incoming`
- **Output**: `transactions.processed`
- **DLQ**: `transactions.dlq`

## ✅ **What You Get**
- Kafka producer with Avro serialization
- Kafka consumer with Avro deserialization
- Schema validation and evolution
- Production-ready configuration
- **Comprehensive test suite** with Playwright
