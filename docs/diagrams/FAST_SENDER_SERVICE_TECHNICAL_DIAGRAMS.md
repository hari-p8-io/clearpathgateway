# Fast Sender Service - Technical Architecture & Exception Handling

## Service Responsibilities & Message Flow

```mermaid
graph TB
    subgraph "Internal Services"
        InwardProcessor[Fast Inward Processor]
        OutwardProcessor[Fast Outward Processor]
        KafkaInbound[Kafka Consumer<br/>fast.sender.outbound.messages<br/>fast.sender.response.messages]
    end
    
    subgraph "Fast Sender Service - Core Responsibilities"
        MessageConsumer[Multi-Source Message Consumer<br/>Reactive Kafka Listener]
        JsonToXmlTransformer[JSON → XML Transform Engine<br/>JAXB Marshalling + Validation]
        TransmissionManager[Transmission Manager<br/>HTTP/2 Client + Circuit Breaker]
        DeliveryTracker[Delivery Tracker<br/>Acknowledgment Processing]
        RetryOrchestrator[Retry Orchestrator<br/>Exponential Backoff Engine]
        IdempotencyRepo[Transmission Idempotency<br/>Duplicate Prevention]
        AuditLogger[Transmission Audit Logger<br/>Compliance Trail]
    end
    
    subgraph "Fallback & Exception Handling"
        CircuitBreaker[Circuit Breaker<br/>CPG Gateway Monitor]
        RetryQueue[Retry Queue<br/>Exponential Backoff<br/>Max 5 attempts]
        SpannerFallback[Spanner Fallback Store<br/>Failed Transmissions]
        S3DeadLetter[S3 Dead Letter Queue<br/>Manual Intervention]
        AlertingService[Alerting Service<br/>Ops Notifications]
    end
    
    subgraph "Data Layer"
        Redis[(Redis Cache<br/>Delivery Status<br/>TTL: 7 days)]
        Spanner[(Cloud Spanner<br/>Transmission History<br/>Audit Trail)]
        S3Bucket[(S3 Bucket<br/>Failed Messages<br/>Manual Recovery)]
    end
    
    subgraph "External Integration"
        CPGGateway[CPG Gateway<br/>HTTP/2 TLS 1.3<br/>Mutual Authentication]
        G3Host[G3 Host System]
        IBMMQ_OUT[IBM MQ Queue<br/>FAST.SENDER.OUTBOUND<br/>ISO 20022 XML Messages]
    end
    
    InwardProcessor --> KafkaInbound
    OutwardProcessor --> KafkaInbound
    KafkaInbound --> MessageConsumer
    MessageConsumer --> JsonToXmlTransformer
    JsonToXmlTransformer --> IdempotencyRepo
    IdempotencyRepo --> TransmissionManager
    TransmissionManager --> DeliveryTracker
    DeliveryTracker --> AuditLogger
    
    TransmissionManager --> CPGGateway
    CPGGateway --> G3Host
    G3Host --> IBMMQ_OUT
    
    %% Exception Flows
    JsonToXmlTransformer -.->|Transform Failed| CircuitBreaker
    TransmissionManager -.->|CPG Down| CircuitBreaker
    TransmissionManager -.->|Timeout| CircuitBreaker
    
    CircuitBreaker --> RetryQueue
    RetryQueue -.->|Retry Success| TransmissionManager
    RetryQueue -.->|Max Retries| SpannerFallback
    SpannerFallback -.->|Spanner Down| S3DeadLetter
    
    CircuitBreaker --> AlertingService
    SpannerFallback --> AlertingService
    S3DeadLetter --> AlertingService
    
    %% Data Flows
    IdempotencyRepo <--> Redis
    IdempotencyRepo <--> Spanner
    DeliveryTracker --> Spanner
    AuditLogger --> Spanner
    SpannerFallback --> S3Bucket
```

## Exception Handling & Retry Logic

```mermaid
flowchart TD
    Start([Message from Kafka]) --> Transform{JSON → XML<br/>Transformation}
    
    Transform -->|Success| IdempotencyCheck{Duplicate<br/>Transmission Check}
    Transform -->|Failed| TransformRetry{Transform Retry<br/>Count < 3?}
    TransformRetry -->|Yes| Transform
    TransformRetry -->|No| DeadLetter[Send to Dead Letter<br/>S3 + Alert Ops]
    
    IdempotencyCheck -->|New Message| PrepareTransmission[Prepare HTTP Request<br/>+ Set Headers]
    IdempotencyCheck -->|Duplicate| SkipTransmission[Skip + Log<br/>Return Cached Result]
    
    PrepareTransmission --> TransmitToCPG{Transmit to<br/>CPG Gateway}
    
    TransmitToCPG -->|Success 200| ProcessACK[Process ACK<br/>Update Status]
    TransmitToCPG -->|4xx Client Error| PermanentFailure[Log Error<br/>+ Alert Ops<br/>No Retry]
    TransmitToCPG -->|5xx Server Error| ServerError{CPG Server<br/>Error}
    TransmitToCPG -->|Timeout| TimeoutError{Connection<br/>Timeout}
    TransmitToCPG -->|Network Error| NetworkError{Network<br/>Failure}
    
    ServerError --> CheckRetryCount{Retry Count<br/>< 5?}
    TimeoutError --> CheckRetryCount
    NetworkError --> CheckRetryCount
    
    CheckRetryCount -->|Yes| CalculateBackoff[Calculate Exponential<br/>Backoff Delay<br/>Base: 1s, Max: 60s]
    CheckRetryCount -->|No| MaxRetriesExceeded[Max Retries Exceeded<br/>Store in Spanner]
    
    CalculateBackoff --> ScheduleRetry[Schedule Retry<br/>Background Task]
    ScheduleRetry -.->|After Delay| PrepareTransmission
    
    MaxRetriesExceeded --> SpannerStore{Store in<br/>Spanner Fallback}
    SpannerStore -->|Success| ManualQueue[Manual Intervention<br/>Queue + Alert]
    SpannerStore -->|Spanner Down| S3Emergency[Emergency S3<br/>Storage + Critical Alert]
    
    ProcessACK --> UpdateMetrics[Update Delivery<br/>Metrics + Audit]
    UpdateMetrics --> Success([Transmission<br/>Complete])
    
    PermanentFailure --> UpdateMetrics
    ManualQueue --> OperatorIntervention[Operator Reviews<br/>+ Manual Retry]
    S3Emergency --> OperatorIntervention
    
    style DeadLetter fill:#ffcccc
    style PermanentFailure fill:#ff9999
    style MaxRetriesExceeded fill:#ffffcc
    style S3Emergency fill:#ff6666
    style OperatorIntervention fill:#ffcc99
```

## API Contracts & Message Samples

### 1. Kafka Input Contracts

**Topic Configuration:**
```yaml
spring.cloud.stream:
  bindings:
    outbound-payment-input:
      destination: fast.sender.outbound.messages
      group: fast-sender-service
      consumer:
        max-attempts: 1
        enable-dlq: true
    response-message-input:
      destination: fast.sender.response.messages
      group: fast-sender-service
      consumer:
        max-attempts: 1
        enable-dlq: true
  kafka:
    bindings:
      outbound-payment-input:
        consumer:
          configuration:
            enable.auto.commit: false
            auto.offset.reset: earliest
            max.poll.records: 100
      response-message-input:
        consumer:
          configuration:
            enable.auto.commit: false
            auto.offset.reset: earliest
            max.poll.records: 50
```

**Sample Unified JSON Input (PACS.008):**
```json
{
  "messageType": "PACS_008",
  "messageVersion": "13",
  "groupHeader": {
    "messageId": "MSG20241201002",
    "creationDateTime": "2024-12-01T15:30:00.000Z",
    "numberOfTransactions": "1",
    "controlSum": 2500.00,
    "totalInterbankSettlementAmount": {
      "value": 2500.00,
      "currency": "SGD"
    },
    "interbankSettlementDate": "2024-12-01",
    "instructingAgent": {
      "financialInstitutionIdentification": {
        "bicfi": "OCBCSGSG"
      }
    },
    "instructedAgent": {
      "financialInstitutionIdentification": {
        "bicfi": "UOBSSGSG"
      }
    }
  },
  "transactionInformation": [
    {
      "paymentIdentification": {
        "instructionId": "INS20241201002",
        "endToEndId": "E2E20241201002",
        "txId": "TXN20241201002",
        "uetr": "87654321-4321-4321-4321-210987654321"
      },
      "interbankSettlementAmount": {
        "value": 2500.00,
        "currency": "SGD"
      },
      "debtor": {
        "name": "ACME Corp Pte Ltd",
        "postalAddress": {
          "country": "SG"
        }
      },
      "debtorAccount": {
        "identification": {
          "iban": "SG44OCBC0000000098765432"
        }
      },
      "creditor": {
        "name": "TechStart Pte Ltd",
        "postalAddress": {
          "country": "SG"
        }
      },
      "creditorAccount": {
        "identification": {
          "iban": "SG77UOBS0000000013579246"
        }
      },
      "remittanceInformation": {
        "unstructured": "Invoice INV-2024-001"
      }
    }
  ],
  "processingMetadata": {
    "receivedTimestamp": 1733065800000,
    "processedTimestamp": 1733065802500,
    "sourceService": "fast-outward-clearing-processor",
    "targetService": "fast-sender-service",
    "correlationId": "CORR-20241201-002",
    "traceId": "TRACE-20241201-002",
    "idempotencyKey": "IDEM-MSG20241201002"
  }
}
```

**Sample Unified JSON Input (PACS.002 - Status Report):**
```json
{
  "messageType": "PACS_002",
  "messageVersion": "15",
  "groupHeader": {
    "messageId": "STS20241201001",
    "creationDateTime": "2024-12-01T16:00:00.000Z",
    "instructingAgent": {
      "financialInstitutionIdentification": {
        "bicfi": "OCBCSGSG"
      }
    },
    "instructedAgent": {
      "financialInstitutionIdentification": {
        "bicfi": "DBSSSGSG"
      }
    }
  },
  "originalGroupInformation": {
    "originalMessageId": "MSG20241201001",
    "originalMessageNameId": "pacs.008.001.13",
    "originalCreationDateTime": "2024-12-01T10:30:00.000Z"
  },
  "transactionInformationAndStatus": [
    {
      "statusId": "STS001",
      "originalInstructionId": "INS20241201001",
      "originalEndToEndId": "E2E20241201001",
      "originalTransactionId": "TXN20241201001",
      "transactionStatus": "ACCP",
      "acceptanceDateTime": "2024-12-01T15:59:30.000Z"
    }
  ],
  "processingMetadata": {
    "receivedTimestamp": 1733067600000,
    "sourceService": "fast-inward-clearing-processor",
    "targetService": "fast-sender-service",
    "correlationId": "CORR-STS-20241201-001",
    "traceId": "TRACE-STS-20241201-001",
    "idempotencyKey": "IDEM-STS20241201001"
  }
}
```

### 2. CPG Gateway HTTP API

**Configuration:**
```yaml
cpg:
  gateway:
    base-url: https://cpg.g3.anz.com.sg
    timeout:
      connect: 5000ms
      read: 30000ms
      write: 10000ms
    http2:
      enabled: true
      max-concurrent-streams: 100
    ssl:
      key-store: classpath:certs/fast-sender-keystore.p12
      trust-store: classpath:certs/cpg-truststore.p12
      mutual-auth: required
    retry:
      max-attempts: 3
      backoff:
        initial-interval: 1000ms
        multiplier: 2.0
        max-interval: 60000ms
```

**HTTP Request Sample:**
```http
POST /api/v1/messages/send HTTP/2
Host: cpg.g3.anz.com.sg
Content-Type: application/xml
X-Correlation-ID: CORR-20241201-002
X-Message-Type: PACS_008
X-Transmission-ID: TRANS-20241201-002
X-Source-System: FAST-SENDER-SERVICE
X-Target-System: G3-HOST
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Length: 2847

<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.13">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>MSG20241201002</MsgId>
      <CreDtTm>2024-12-01T15:30:00.000Z</CreDtTm>
      <!-- ... full XML content ... -->
    </GrpHdr>
    <!-- ... transaction details ... -->
  </FIToFICstmrCdtTrf>
</Document>
```

**HTTP Response Sample:**
```http
HTTP/2 200 OK
Content-Type: application/json
X-Correlation-ID: CORR-20241201-002
X-G3-Message-ID: G3-MSG-20241201-002
X-Processing-Time: 245ms

{
  "status": "ACCEPTED",
  "messageId": "MSG20241201002",
  "g3MessageId": "G3-MSG-20241201-002",
  "transmissionTimestamp": "2024-12-01T15:30:00.245Z",
  "acknowledgment": {
    "code": "ACCP",
    "description": "Message accepted for processing"
  },
  "routing": {
    "targetQueue": "FAST.G3.OUTBOUND.PACS008",
    "estimatedProcessingTime": "2024-12-01T15:30:02.000Z"
  }
}
```

### 3. IBM MQ Output Contract

**Queue Configuration:**
```yaml
ibm.mq:
  output-queues:
    outbound-messages:
      name: FAST.SENDER.OUTBOUND
      put-enabled: true
      get-enabled: false
      message-persistence: PERSISTENT
      message-priority: 5
      expiry: 86400000  # 24 hours
      
  connection:
    queue-manager: QM_FAST_OUTBOUND
    channel: FAST.SENDER.CHANNEL
    connection-name: mq-out.anz.com.sg(1414)
    user: fast_sender_user
    ssl-cipher-suite: TLS_RSA_WITH_AES_256_CBC_SHA256
```

**MQ Message Sample:**
```xml
<!-- MQ Message Headers -->
MQMD (MQ Message Descriptor):
  Format: MQFMT_STRING
  Priority: 5
  Persistence: MQPER_PERSISTENT
  MsgType: MQMT_DATAGRAM
  Expiry: 86400000
  CorrelId: CORR-20241201-002
  ReplyToQ: FAST.SENDER.REPLY
  
MQRFH2 (Rules and Formatting Header):
  NameValueCCSID: 1208
  usr:
    MessageType: PACS_008
    TransmissionID: TRANS-20241201-002
    SourceSystem: FAST-SENDER-SERVICE
    ProcessingTimestamp: 2024-12-01T15:30:00.500Z

<!-- XML Payload -->
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.13">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>MSG20241201002</MsgId>
      <CreDtTm>2024-12-01T15:30:00.000Z</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <CtrlSum>2500.00</CtrlSum>
      <!-- ... complete XML structure ... -->
    </GrpHdr>
    <!-- ... transaction information ... -->
  </FIToFICstmrCdtTrf>
</Document>
```

### 4. Internal Monitoring APIs

**Transmission Status API:**
```yaml
paths:
  /api/v1/sender/transmissions/{transmissionId}:
    get:
      summary: Get transmission status
      parameters:
        - name: transmissionId
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          content:
            application/json:
              schema:
                type: object
                properties:
                  transmissionId:
                    type: string
                  messageId:
                    type: string
                  status:
                    type: string
                    enum: [PENDING, TRANSMITTED, ACKNOWLEDGED, FAILED, RETRY_SCHEDULED]
                  attempts:
                    type: integer
                  lastAttemptTime:
                    type: string
                    format: date-time
                  nextRetryTime:
                    type: string
                    format: date-time
                  deliveryReceipt:
                    type: object
                  errorDetails:
                    type: string
                example:
                  transmissionId: "TRANS-20241201-002"
                  messageId: "MSG20241201002"
                  status: "ACKNOWLEDGED"
                  attempts: 1
                  lastAttemptTime: "2024-12-01T15:30:00.500Z"
                  deliveryReceipt:
                    g3MessageId: "G3-MSG-20241201-002"
                    acknowledgmentCode: "ACCP"
                    processingTime: "245ms"

  /api/v1/sender/retry/{messageId}:
    post:
      summary: Manual retry for failed transmission
      parameters:
        - name: messageId
          in: path
          required: true
          schema:
            type: string
      responses:
        202:
          content:
            application/json:
              schema:
                type: object
                properties:
                  retryId:
                    type: string
                  messageId:
                    type: string
                  scheduledTime:
                    type: string
                    format: date-time
                example:
                  retryId: "RETRY-20241201-001"
                  messageId: "MSG20241201002"
                  scheduledTime: "2024-12-01T15:35:00.000Z"
```

## Performance Monitoring & Circuit Breaker

```mermaid
graph TB
    subgraph "Performance Metrics"
        TransmissionLatency[Transmission Latency<br/>P50: <500ms<br/>P95: <1s<br/>P99: <2s]
        ThroughputMetrics[Throughput Metrics<br/>• messages/second<br/>• bytes/second<br/>• concurrent transmissions]
        ErrorRates[Error Rates<br/>• 4xx client errors<br/>• 5xx server errors<br/>• timeout errors<br/>• network errors]
    end
    
    subgraph "Circuit Breaker Logic"
        HealthCheck[CPG Health Check<br/>Endpoint: /health<br/>Interval: 30s]
        ThresholdMonitor[Threshold Monitor<br/>Failure Rate: >50%<br/>Response Time: >5s<br/>Consecutive Failures: >10]
        StateManager[Circuit State Manager<br/>CLOSED → OPEN → HALF_OPEN]
    end
    
    subgraph "Alerting Thresholds"
        Warning[WARNING<br/>• Error Rate >5%<br/>• Latency P95 >1s<br/>• Queue Depth >100]
        Critical[CRITICAL<br/>• Error Rate >20%<br/>• Circuit Breaker Open<br/>• Queue Depth >500]
        Emergency[EMERGENCY<br/>• All CPG Endpoints Down<br/>• S3 Fallback Active<br/>• Manual Intervention Required]
    end
    
    TransmissionLatency --> ThresholdMonitor
    ErrorRates --> ThresholdMonitor
    ThresholdMonitor --> StateManager
    HealthCheck --> StateManager
    
    StateManager -->|Open Circuit| Warning
    StateManager -->|High Error Rate| Critical
    StateManager -->|Total Failure| Emergency
```

## Technology Stack & Dependencies

```yaml
Spring Boot Configuration:
  spring.application.name: fast-sender-service
  spring.profiles.active: production
  
  # Virtual Threads (Java 21)
  spring.threads.virtual.enabled: true
  
  # Kafka Configuration
  spring.cloud.stream.kafka.binder.brokers: kafka-cluster:9092
  spring.cloud.stream.kafka.binder.configuration:
    security.protocol: SASL_SSL
    sasl.mechanism: SCRAM-SHA-512
    ssl.truststore.location: /app/config/kafka-truststore.jks
  
  # HTTP Client Configuration
  spring.webflux.http2.enabled: true
  
  # Actuator Endpoints
  management.endpoints.web.exposure.include: health,metrics,prometheus,info
  management.endpoint.health.show-details: always

Circuit Breaker Configuration:
  resilience4j.circuitbreaker.instances.cpg-gateway:
    failure-rate-threshold: 50
    wait-duration-in-open-state: 30s
    sliding-window-size: 10
    minimum-number-of-calls: 5
    permitted-number-of-calls-in-half-open-state: 3
    
  resilience4j.retry.instances.transmission:
    max-attempts: 5
    wait-duration: 1s
    exponential-backoff-multiplier: 2.0
    retry-exceptions:
      - java.net.SocketTimeoutException
      - java.net.ConnectException
      - org.springframework.web.reactive.function.client.WebClientResponseException$InternalServerError

JVM Settings:
  memory: "-Xms2g -Xmx4g"
  gc: "-XX:+UseZGC -XX:+UnlockExperimentalVMOptions"
  virtual-threads: "-XX:+UseVirtualThreads --enable-preview"
  monitoring: "-XX:+FlightRecorder -XX:StartFlightRecording=duration=0s,filename=app.jfr"
```