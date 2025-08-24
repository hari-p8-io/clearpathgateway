# Avro Refactoring for Fast Inward Clearing Processor

## Overview

This document describes the refactoring of the Fast Inward Clearing Processor service to use Apache Avro for message serialization and deserialization instead of JSON/POJO objects.

## Changes Made

### 1. Avro Schema Files

Created two Avro schema files in `src/main/resources/avro/`:

- **`UnifiedPaymentMessage.avsc`**: Schema for incoming payment messages following the provided format
- **`ProcessedTransactionMessage.avsc`**: Schema for processed transaction messages

### 2. Maven Dependencies

Updated `pom.xml` to include:

```xml
<!-- Avro Dependencies -->
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.11.3</version>
</dependency>
```

Added Avro Maven plugin for generating Avro classes:

```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.11.3</version>
    <executions>
        <execution>
            <goals>
                <goal>schema</goal>
            </goals>
            <phase>generate-sources</phase>
            <configuration>
                <sourceDirectory>${project.basedir}/src/main/resources/avro</sourceDirectory>
                <outputDirectory>${project.basedir}/src/main/java</outputDirectory>
                <stringType>String</stringType>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 3. Kafka Configuration Updates

#### `KafkaConfig.java`

- **Consumer Factory**: Updated to use `KafkaAvroDeserializer`
- **Producer Factory**: Updated to use `KafkaAvroSerializer`
- **Schema Registry**: Added configuration for schema registry URL
- **GenericRecord**: Changed from specific POJO types to `GenericRecord` for flexibility

Key changes:
```java
// Consumer
props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, 
    "io.confluent.kafka.serializers.KafkaAvroDeserializer");
props.put("schema.registry.url", schemaRegistryUrl);
props.put("specific.avro.reader", "false");

// Producer
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
    "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("schema.registry.url", schemaRegistryUrl);
```

#### `application.yml`

Updated Kafka configuration:
```yaml
spring:
  kafka:
    producer:
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
      properties:
        schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}
    consumer:
      value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
      properties:
        schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}
        specific.avro.reader: false

app:
  kafka:
    schema-registry:
      url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}
```

### 4. Service Interface Updates

#### `ClearingProcessorService.java`

Added new methods for Avro processing:
- `processAvroTransaction(GenericRecord avroMessage)`
- `validateAvroTransaction(GenericRecord avroMessage)`
- `enrichAvroTransaction(GenericRecord avroMessage)`
- `applyBusinessRulesToAvro(ProcessedTransactionMessage processedMessage)`

### 5. Consumer Updates

#### `TransactionConsumer.java`

- **Message Type**: Changed from `TransactionMessage` to `GenericRecord`
- **Avro Processing**: Updated to handle Avro messages
- **Conversion**: Uses `AvroConverter` utility for POJO ↔ Avro conversions

### 6. Utility Class

#### `AvroConverter.java`

New utility class providing:
- `convertToTransactionMessage(GenericRecord avroRecord)`: Convert Avro to POJO
- `convertToAvroRecord(ProcessedTransactionMessage processedMessage)`: Convert POJO to Avro
- `extractTransactionId(GenericRecord avroRecord)`: Extract transaction ID from Avro
- `extractAmount(GenericRecord avroRecord)`: Extract amount from Avro

## Benefits of Avro Refactoring

### 1. **Schema Evolution**
- Backward and forward compatibility
- Runtime schema validation
- Schema registry support

### 2. **Performance**
- Binary serialization (smaller message size)
- Faster serialization/deserialization
- Reduced network overhead

### 3. **Type Safety**
- Compile-time schema validation
- Runtime type checking
- Reduced runtime errors

### 4. **Interoperability**
- Language-agnostic schemas
- Multiple language support
- Standardized message format

## Configuration Requirements

### Environment Variables

```bash
# Schema Registry URL (default: http://localhost:8081)
export SCHEMA_REGISTRY_URL=http://your-schema-registry:8081

# Kafka Bootstrap Servers
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Schema Registry

The service expects a Confluent Schema Registry running at the configured URL. The schema registry will:
- Store and version Avro schemas
- Provide schema compatibility checking
- Enable schema evolution

## Usage Examples

### Producing Avro Messages

```java
// Create Avro message
GenericRecord avroRecord = createAvroMessage();

// Send to Kafka
kafkaTemplate.send("transactions.incoming", "key", avroRecord);
```

### Consuming Avro Messages

```java
@KafkaListener(topics = "transactions.incoming")
public void consumeTransaction(ConsumerRecord<String, GenericRecord> record) {
    GenericRecord avroMessage = record.value();
    
    // Process Avro message
    ProcessedTransactionMessage result = processAvroMessage(avroMessage);
}
```

## Migration Notes

### 1. **Existing POJOs**
- Original POJO classes are preserved for backward compatibility
- New Avro-based methods are added alongside existing ones
- Gradual migration path available

### 2. **Message Format**
- Incoming messages must conform to `UnifiedPaymentMessage` schema
- Output messages use `ProcessedTransactionMessage` schema
- Schema validation occurs at runtime

### 3. **Testing**
- Update unit tests to use Avro schemas
- Mock Avro `GenericRecord` objects
- Test schema validation and conversion logic

## Next Steps

### 1. **Schema Registry Setup**
- Deploy Confluent Schema Registry
- Configure schema compatibility rules
- Set up schema versioning strategy

### 2. **Schema Evolution**
- Plan schema changes carefully
- Test backward/forward compatibility
- Update consumer applications

### 3. **Monitoring**
- Monitor schema registry metrics
- Track schema compatibility issues
- Monitor Avro serialization performance

### 4. **Documentation**
- Update API documentation
- Document schema evolution rules
- Provide migration guides for consumers

## Troubleshooting

### Common Issues

1. **Schema Registry Connection**
   - Verify schema registry URL
   - Check network connectivity
   - Validate schema registry health

2. **Schema Compatibility**
   - Check schema version compatibility
   - Verify schema evolution rules
   - Review schema validation errors

3. **Serialization Errors**
   - Validate Avro schema syntax
   - Check data type compatibility
   - Review schema registry logs

### Debug Mode

Enable debug logging for Avro operations:
```yaml
logging:
  level:
    com.anz.fastpayment.inward.util.AvroConverter: DEBUG
    org.apache.avro: DEBUG
```

## Conclusion

The Avro refactoring provides a robust, performant, and scalable foundation for message processing in the Fast Inward Clearing Processor service. The schema-driven approach ensures message consistency and enables seamless schema evolution as business requirements change.
