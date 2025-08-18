# ISO 20022 to Unified JSON Mapping Resources

## Overview

This directory contains the complete set of resources for transforming ISO 20022 XML payment messages to the unified JSON format used across the APEAFAST-SG ClearPath Gateway microservices.

## Files in this Directory

### 1. `unified-payment-message-schema.json`
**Purpose**: JSON Schema definition for the unified payment message format  
**Usage**: Use this schema to validate transformed JSON messages  
**Key Features**:
- Supports all four message types (PACS_008, PACS_003, PACS_007, CAMT_056)
- Comprehensive validation rules and data type constraints
- Message-type-specific required fields using conditional schemas
- Pattern validation for identifiers (BIC, IBAN, UETR, etc.)

### 2. `iso20022-to-unified-json-mapping.md`
**Purpose**: Comprehensive human-readable mapping documentation  
**Usage**: Reference guide for developers implementing transformations  
**Key Features**:
- Detailed field-by-field mapping tables
- Data type conversion guidelines
- Enumeration value mappings
- Usage examples for each message type
- Error handling and validation guidance

### 3. `transformation-config.json`
**Purpose**: Machine-readable transformation configuration  
**Usage**: Configuration file for automated transformation utilities  
**Key Features**:
- Structured transformation rules for each message type
- Reusable transformation templates
- Data type conversion specifications
- Validation rules and business logic constraints

### 4. `README.md`
**Purpose**: This file - documentation guide for the mapping resources

## Supported Message Types

| ISO 20022 Message | Unified Type | Description | Use Case |
|-------------------|--------------|-------------|----------|
| `pacs.008.001.13` | `PACS_008` | FI To FI Customer Credit Transfer | Customer credit transfers, payments |
| `pacs.003.001.11` | `PACS_003` | FI To FI Customer Direct Debit | Direct debits, collections |
| `pacs.007.001.13` | `PACS_007` | FI To FI Payment Reversal | Payment reversals, returns |
| `camt.056.001.11` | `CAMT_056` | FI To FI Payment Cancellation Request | Payment cancellations |

## Quick Start Guide

### 1. Validate JSON Messages

Use the unified schema to validate transformed messages:

```javascript
// Node.js example
const Ajv = require('ajv');
const addFormats = require('ajv-formats');
const schema = require('./unified-payment-message-schema.json');

const ajv = new Ajv();
addFormats(ajv);
const validate = ajv.compile(schema);

const isValid = validate(transformedMessage);
if (!isValid) {
    console.log(validate.errors);
}
```

### 2. Transform XML to JSON

Use the transformation config for automated conversion:

```java
// Java example using Jackson
ObjectMapper mapper = new ObjectMapper();
TransformationConfig config = mapper.readValue(
    new File("transformation-config.json"), 
    TransformationConfig.class
);

// Apply transformation rules based on message type
UnifiedPaymentMessage result = transformer.transform(xmlMessage, config);
```

### 3. Reference Field Mappings

Consult the mapping documentation for specific field transformations:

```xml
<!-- XML Input -->
<IntrBkSttlmAmt Ccy="SGD">1000.00</IntrBkSttlmAmt>
```

```json
// JSON Output
{
  "interbankSettlementAmount": {
    "value": 1000.00,
    "currency": "SGD"
  }
}
```

## Implementation Guidelines

### Message Type Detection

Detect the message type from the XML root element:

```java
public MessageType detectMessageType(Document xmlDoc) {
    Element root = xmlDoc.getDocumentElement();
    Element firstChild = (Element) root.getFirstChild();
    
    switch (firstChild.getLocalName()) {
        case "FIToFICstmrCdtTrf": return MessageType.PACS_008;
        case "FIToFICstmrDrctDbt": return MessageType.PACS_003;
        case "FIToFIPmtRvsl": return MessageType.PACS_007;
        case "FIToFIPmtCxlReq": return MessageType.CAMT_056;
        default: throw new UnsupportedMessageTypeException();
    }
}
```

### Transformation Pipeline

Recommended transformation approach:

1. **Parse XML**: Convert XML to DOM or object model
2. **Detect Type**: Identify message type from root element
3. **Load Config**: Get transformation rules for the message type
4. **Transform**: Apply field mappings and data conversions
5. **Validate**: Check against unified JSON schema
6. **Output**: Return validated unified JSON message

### Error Handling

Handle common transformation errors:

```java
public class TransformationException extends Exception {
    public enum ErrorType {
        UNSUPPORTED_MESSAGE_TYPE,
        MISSING_REQUIRED_FIELD,
        INVALID_DATA_FORMAT,
        SCHEMA_VALIDATION_FAILED
    }
    
    private final ErrorType errorType;
    private final String fieldPath;
    private final Object sourceValue;
}
```

### Data Type Conversions

Key conversion patterns:

```java
// Amount conversion
public Amount convertAmount(Element amountElement) {
    return Amount.builder()
        .value(new BigDecimal(amountElement.getTextContent()))
        .currency(amountElement.getAttribute("Ccy"))
        .build();
}

// Date conversion
public String convertDate(String isoDate) {
    return LocalDate.parse(isoDate).toString(); // Already ISO format
}

// DateTime conversion
public String convertDateTime(String isoDateTime) {
    return OffsetDateTime.parse(isoDateTime).toString();
}
```

## Business Rules and Validation

### Mandatory Fields

Each message type has specific required fields:

- **PACS_008**: Group header, transaction info with creditor/debtor details
- **PACS_003**: Group header, transaction info with direct debit details
- **PACS_007**: Group header, reversal amounts and reasons
- **CAMT_056**: Group header, transaction info, case information

### Cross-Field Validation

Implement business rule validation:

```java
public void validateBusinessRules(UnifiedPaymentMessage message) {
    // Currency consistency
    validateCurrencyConsistency(message);
    
    // Date logic
    validateDateSequence(message);
    
    // Amount precision
    validateAmountPrecision(message);
    
    // Reference consistency
    validateReferenceConsistency(message);
}
```

### Amount Precision

All monetary amounts must have at most 5 decimal places as per ISO 20022:

```java
public void validateAmountPrecision(BigDecimal amount) {
    if (amount.scale() > 5) {
        throw new ValidationException("Amount precision exceeds 5 decimal places");
    }
}
```

## Performance Considerations

### Transformation Optimization

1. **Cache Schemas**: Load and cache JSON schemas at startup
2. **Reuse Parsers**: Create DOM parsers once and reuse
3. **Stream Processing**: For large files, use streaming XML parsers
4. **Parallel Processing**: Transform multiple messages in parallel
5. **Memory Management**: Clear intermediate objects to prevent memory leaks

### Example Performance-Optimized Transformer

```java
@Service
public class OptimizedMessageTransformer {
    
    private final JsonSchema schema;
    private final TransformationConfig config;
    private final DocumentBuilderFactory factory;
    
    @PostConstruct
    public void initialize() {
        // Pre-load and cache resources
        this.schema = loadSchema();
        this.config = loadTransformationConfig();
        this.factory = DocumentBuilderFactory.newInstance();
        this.factory.setNamespaceAware(true);
    }
    
    public UnifiedPaymentMessage transform(InputStream xmlInput) {
        // Use cached resources for transformation
        Document doc = factory.newDocumentBuilder().parse(xmlInput);
        return applyTransformation(doc, config);
    }
}
```

## Testing

### Unit Test Examples

```java
@Test
public void testPacs008Transformation() {
    // Arrange
    String xml = loadTestXml("pacs008-sample.xml");
    
    // Act
    UnifiedPaymentMessage result = transformer.transform(xml);
    
    // Assert
    assertEquals("PACS_008", result.getMessageType());
    assertNotNull(result.getGroupHeader().getMessageId());
    assertTrue(validator.isValid(result));
}

@Test
public void testAmountConversion() {
    // Test amount with currency attribute transformation
    Element amountElement = createAmountElement("1000.50", "SGD");
    Amount result = transformer.convertAmount(amountElement);
    
    assertEquals(new BigDecimal("1000.50"), result.getValue());
    assertEquals("SGD", result.getCurrency());
}
```

### Integration Test Examples

```java
@Test
public void testEndToEndTransformation() {
    // Load real ISO 20022 message samples
    List<String> sampleMessages = loadSampleMessages();
    
    for (String xml : sampleMessages) {
        UnifiedPaymentMessage result = transformer.transform(xml);
        
        // Validate against schema
        assertTrue(schemaValidator.isValid(result));
        
        // Validate business rules
        assertTrue(businessRuleValidator.isValid(result));
        
        // Test round-trip if reverse transformation exists
        if (reverseTransformer != null) {
            String reconstructed = reverseTransformer.transform(result);
            assertXMLEqual(xml, reconstructed);
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **Missing Namespace Handling**
   - Ensure XML parsers are namespace-aware
   - Use proper namespace URIs in XPath expressions

2. **Currency Attribute Extraction**
   - Amount elements have currency as XML attributes
   - Use `getAttribute("Ccy")` not text content

3. **Optional Field Handling**
   - Many ISO 20022 fields are optional
   - Check for null/empty before transformation

4. **Array Field Processing**
   - Some fields can have multiple occurrences
   - Handle both single and multiple values correctly

5. **Date Format Variations**
   - ISO 20022 supports both date and datetime
   - Detect format and convert appropriately

### Debug Mode

Enable debug logging for transformation details:

```java
@Component
@Slf4j
public class DebugTransformer {
    
    public UnifiedPaymentMessage transform(String xml) {
        log.debug("Starting transformation for XML: {}", xml.substring(0, 200));
        
        try {
            UnifiedPaymentMessage result = doTransform(xml);
            log.debug("Transformation successful. Result type: {}", result.getMessageType());
            return result;
        } catch (Exception e) {
            log.error("Transformation failed for XML: {}", xml, e);
            throw e;
        }
    }
}
```

## Version Compatibility

### Schema Versioning

The unified schema supports multiple ISO 20022 versions:

- PACS.008: Version 13 (latest)
- PACS.003: Version 11 
- PACS.007: Version 13
- CAMT.056: Version 11

### Backward Compatibility

When updating schemas:

1. Maintain backward compatibility for existing fields
2. Add new optional fields for new requirements
3. Use semantic versioning for schema updates
4. Provide migration guides for breaking changes

## Contributing

### Adding New Message Types

1. Update `unified-payment-message-schema.json` with new message type
2. Add mapping documentation in `iso20022-to-unified-json-mapping.md`
3. Extend `transformation-config.json` with transformation rules
4. Create unit and integration tests
5. Update this README with the new message type

### Updating Existing Mappings

1. Review impact on existing transformations
2. Update all three files consistently
3. Test with real message samples
4. Document any breaking changes
5. Update version numbers appropriately

## Support and Documentation

For additional support:

- Review the comprehensive mapping documentation
- Check the JSON schema for validation rules
- Examine the transformation config for implementation details
- Refer to ISO 20022 official documentation for field definitions
- Contact the APEAFAST-SG development team for specific questions

## License

This mapping documentation and configuration is proprietary to ANZ Banking Group and is intended for use within the APEAFAST-SG ClearPath Gateway project only.