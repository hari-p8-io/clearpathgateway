# Fast Inward Clearing Processor - Technical Architecture & Exception Handling

## Service Responsibilities & 4.5-Second SLA Compliance

```mermaid
graph TB
    subgraph "Message Sources"
        RouterKafka[Router Service<br/>Kafka Topic: fast.inward.payment.processing<br/>PACS.008, PACS.003, PACS.007, CAMT.056]
    end
    
    subgraph "Fast Inward Clearing Processor - Core Responsibilities"
        MessageConsumer[Kafka Message Consumer<br/>Reactive Processing<br/>SLA Timer Start]
        BusinessValidator[Business Rule Validator<br/>Configurable Rules Engine<br/>Target: <200ms]
        FraudDetectionEngine[ML Fraud Detection Engine<br/>Real-time Scoring<br/>Target: <500ms]
        AccountValidator[Account Validator<br/>VAM/MIDANZ Integration<br/>Target: <200ms]
        LiquidityClient[Liquidity Service Client<br/>gRPC/REST Calls<br/>Target: <200ms]
        PaymentExecutor[Payment Executor<br/>Core Banking Integration<br/>Target: <1.5s]
        WorkflowEngine[Payment Workflow Engine<br/>State Machine<br/>SLA Monitor]
        ResponseGenerator[Response Generator<br/>JSON→XML Transform<br/>Target: <100ms]
    end
    
    subgraph "SLA Management & Exception Handling"
        SLATracker[SLA Tracker<br/>4.5s Hard Limit<br/>Real-time Monitoring]
        CircuitBreaker[Circuit Breaker<br/>External Service Monitor]
        TimeoutManager[Timeout Manager<br/>Per-stage Timeouts]
        FallbackProcessor[Fallback Processor<br/>Degraded Mode Processing]
        RetryOrchestrator[Retry Orchestrator<br/>Exponential Backoff]
        AlertManager[Alert Manager<br/>SLA Breach Notifications]
    end
    
    subgraph "Data Layer"
        Redis[(Redis Cache<br/>Account Data<br/>Fraud Scores<br/>< 1ms reads)]
        Spanner[(Cloud Spanner<br/>Transaction State<br/>Audit Trail<br/>ACID Compliance)]
        MLModelStore[(ML Model Store<br/>Fraud Models<br/>Feature Store)]
    end
    
    subgraph "External Integration"
        VAMSystem[VAM Core Banking<br/>SGD Account Operations<br/>Circuit Breaker Protected]
        MIDANZSystem[MIDANZ System<br/>FX Account Operations<br/>Circuit Breaker Protected]
        LiquidityService[Liquidity Service<br/>Balance Authorization<br/>gRPC/REST API]
        SenderService[Sender Service<br/>Response Transmission<br/>Kafka Producer]
        AvailabilityService[Availability Service<br/>Bank Status Check<br/>gRPC Client]
    end
    
    RouterKafka --> MessageConsumer
    MessageConsumer --> SLATracker
    MessageConsumer --> BusinessValidator
    BusinessValidator --> FraudDetectionEngine
    FraudDetectionEngine --> AccountValidator
    AccountValidator --> LiquidityClient
    LiquidityClient --> PaymentExecutor
    PaymentExecutor --> WorkflowEngine
    WorkflowEngine --> ResponseGenerator
    ResponseGenerator --> SenderService
    
    %% SLA & Exception Flows
    SLATracker --> TimeoutManager
    TimeoutManager --> AlertManager
    
    BusinessValidator -.->|Rule Failure| CircuitBreaker
    FraudDetectionEngine -.->|Model Timeout| CircuitBreaker
    AccountValidator -.->|VAM/MIDANZ Down| CircuitBreaker
    LiquidityClient -.->|Liquidity Timeout| CircuitBreaker
    PaymentExecutor -.->|Core Banking Error| CircuitBreaker
    
    CircuitBreaker --> RetryOrchestrator
    RetryOrchestrator -.->|Retry Success| WorkflowEngine
    RetryOrchestrator -.->|Max Retries| FallbackProcessor
    
    FallbackProcessor --> AlertManager
    TimeoutManager --> FallbackProcessor
    
    %% Data Flows
    FraudDetectionEngine <--> MLModelStore
    AccountValidator <--> Redis
    WorkflowEngine <--> Spanner
    PaymentExecutor --> VAMSystem
    PaymentExecutor --> MIDANZSystem
    LiquidityClient --> LiquidityService
    AccountValidator --> AvailabilityService
```

## 4.5-Second SLA Breakdown & Exception Handling

```mermaid
flowchart TD
    Start([Message Received<br/>SLA Timer Start]) --> ValidateMessage{Message<br/>Validation}
    
    ValidateMessage -->|Valid| BusinessRules{Business Rules<br/>Validation<br/>Budget: 200ms}
    ValidateMessage -->|Invalid| MessageError[Log Invalid Message<br/>Generate NACK<br/>< 50ms]
    MessageError --> SLACompliant([SLA: ✓ < 4.5s])
    
    BusinessRules -->|Pass| FraudCheck{Fraud Detection<br/>ML Scoring<br/>Budget: 500ms}
    BusinessRules -->|Fail| RejectPayment[Reject Payment<br/>Business Rule Violation<br/>< 100ms]
    RejectPayment --> SLACompliant
    
    FraudCheck -->|Low Risk| AccountValidation{Account Validation<br/>VAM/MIDANZ<br/>Budget: 200ms}
    FraudCheck -->|High Risk| FraudReject[Reject Payment<br/>Fraud Detection<br/>< 100ms]
    FraudCheck -->|Timeout| FraudTimeout{Fraud Timeout<br/>Elapsed < 3s?}
    
    FraudTimeout -->|Yes| FraudFallback[Use Rule-based<br/>Fallback Scoring<br/>< 100ms]
    FraudTimeout -->|No| SLABreach[SLA Breach<br/>Emergency Processing]
    FraudFallback --> AccountValidation
    
    AccountValidation -->|Valid| LiquidityCheck{Liquidity Check<br/>Authorization<br/>Budget: 200ms}
    AccountValidation -->|Invalid| AccountReject[Reject Payment<br/>Invalid Account<br/>< 100ms]
    AccountValidation -->|Timeout| AccountTimeout{Account Timeout<br/>Elapsed < 3.5s?}
    
    AccountTimeout -->|Yes| AccountFallback[Use Cached<br/>Account Data<br/>< 50ms]
    AccountTimeout -->|No| SLABreach
    AccountFallback --> LiquidityCheck
    
    LiquidityCheck -->|Approved| ExecutePayment{Payment Execution<br/>Core Banking<br/>Budget: 1.5s}
    LiquidityCheck -->|Rejected| LiquidityReject[Reject Payment<br/>Insufficient Funds<br/>< 100ms]
    LiquidityCheck -->|Timeout| LiquidityTimeout{Liquidity Timeout<br/>Elapsed < 4s?}
    
    LiquidityTimeout -->|Yes| LiquidityFallback[Emergency<br/>Authorization<br/>< 100ms]
    LiquidityTimeout -->|No| SLABreach
    LiquidityFallback --> ExecutePayment
    
    ExecutePayment -->|Success| GenerateResponse{Response Generation<br/>JSON→XML<br/>Budget: 100ms}
    ExecutePayment -->|Failed| PaymentError[Payment Execution<br/>Failed<br/>< 100ms]
    ExecutePayment -->|Timeout| PaymentTimeout{Payment Timeout<br/>Elapsed < 4.4s?}
    
    PaymentTimeout -->|Yes| PaymentFallback[Asynchronous<br/>Processing<br/>< 100ms]
    PaymentTimeout -->|No| SLABreach
    
    GenerateResponse -->|Success| SendResponse[Send to<br/>Sender Service<br/>< 50ms]
    GenerateResponse -->|Timeout| ResponseTimeout{Response Timeout<br/>Elapsed < 4.45s?}
    
    ResponseTimeout -->|Yes| CachedResponse[Use Cached<br/>Response Template<br/>< 50ms]
    ResponseTimeout -->|No| SLABreach
    
    SendResponse --> CheckSLA{Total Time<br/>< 4.5s?}
    CachedResponse --> CheckSLA
    PaymentFallback --> CheckSLA
    
    CheckSLA -->|Yes| SLACompliant
    CheckSLA -->|No| SLABreach
    
    SLABreach --> EmergencyMode[Emergency Mode<br/>• Alert Operations<br/>• Log SLA Breach<br/>• Initiate Investigation]
    EmergencyMode --> SLAViolation([SLA: ✗ > 4.5s<br/>Manual Investigation])
    
    FraudReject --> SLACompliant
    AccountReject --> SLACompliant
    LiquidityReject --> SLACompliant
    PaymentError --> SLACompliant
    
    style SLABreach fill:#ff6666
    style SLAViolation fill:#ff3333
    style EmergencyMode fill:#ffcc99
    style SLACompliant fill:#ccffcc
```

## API Contracts & Message Processing

### 1. Kafka Input Contract (from Router)

**Topic Configuration:**
```yaml
spring.cloud.stream:
  bindings:
    inward-payment-input:
      destination: fast.inward.payment.processing
      group: fast-inward-clearing-processor
      consumer:
        max-attempts: 1  # No retries - SLA critical
        enable-dlq: true
        batch-mode: false  # Individual message processing
  kafka:
    bindings:
      inward-payment-input:
        consumer:
          configuration:
            enable.auto.commit: false
            auto.offset.reset: latest
            max.poll.records: 1  # Process one at a time for SLA
            session.timeout.ms: 30000
            heartbeat.interval.ms: 3000
```

**Sample PACS.008 Input (Credit Transfer Inward):**
```json
{
  "messageType": "PACS_008",
  "messageVersion": "13",
  "groupHeader": {
    "messageId": "CTI20241201001",
    "creationDateTime": "2024-12-01T10:30:00.000Z",
    "numberOfTransactions": "1",
    "controlSum": 75000.00,
    "instructingAgent": {
      "financialInstitutionIdentification": {
        "bicfi": "DBSSSGSG"
      }
    },
    "instructedAgent": {
      "financialInstitutionIdentification": {
        "bicfi": "OCBCSGSG"
      }
    }
  },
  "transactionInformation": [
    {
      "paymentIdentification": {
        "instructionId": "INS-CTI-20241201001",
        "endToEndId": "E2E-CTI-20241201001",
        "txId": "TXN-CTI-20241201001",
        "uetr": "12345678-1234-1234-1234-123456789012"
      },
      "interbankSettlementAmount": {
        "value": 75000.00,
        "currency": "SGD"
      },
      "creditor": {
        "name": "TechStart Pte Ltd",
        "postalAddress": {
          "country": "SG"
        }
      },
      "creditorAccount": {
        "identification": {
          "iban": "SG77OCBC0000000012345678"
        }
      },
      "debtor": {
        "name": "Global Enterprises Ltd",
        "postalAddress": {
          "country": "SG"
        }
      },
      "debtorAccount": {
        "identification": {
          "iban": "SG12DBSS0000000087654321"
        }
      },
      "remittanceInformation": {
        "unstructured": "Invoice payment INV-2024-001"
      }
    }
  ],
  "processingMetadata": {
    "receivedTimestamp": 1733051400000,
    "sourceService": "fast-router-service",
    "targetService": "fast-inward-clearing-processor",
    "correlationId": "CORR-CTI-20241201-001",
    "traceId": "TRACE-CTI-20241201-001",
    "idempotencyKey": "IDEM-CTI20241201001",
    "slaStartTimestamp": 1733051400000
  }
}
```

**Sample PACS.003 Input (Direct Debit Inward):**
```json
{
  "messageType": "PACS_003",
  "messageVersion": "11",
  "groupHeader": {
    "messageId": "DDI20241201001",
    "creationDateTime": "2024-12-01T14:15:00.000Z",
    "numberOfTransactions": "1",
    "controlSum": 1200.00,
    "instructingAgent": {
      "financialInstitutionIdentification": {
        "bicfi": "UOBSSGSG"
      }
    },
    "instructedAgent": {
      "financialInstitutionIdentification": {
        "bicfi": "DBSSSGSG"
      }
    }
  },
  "transactionInformation": [
    {
      "paymentIdentification": {
        "instructionId": "INS-DDI-20241201001",
        "endToEndId": "E2E-DDI-20241201001",
        "txId": "TXN-DDI-20241201001"
      },
      "interbankSettlementAmount": {
        "value": 1200.00,
        "currency": "SGD"
      },
      "directDebitTransactionInformation": {
        "mandateRelatedInformation": {
          "mandateId": "MANDATE-001",
          "dateOfSignature": "2024-01-15"
        },
        "preNotificationId": "PRENOT-001",
        "preNotificationDate": "2024-11-25"
      },
      "creditor": {
        "name": "Utility Company Pte Ltd",
        "postalAddress": {
          "country": "SG"
        }
      },
      "creditorAccount": {
        "identification": {
          "iban": "SG99UOBS0000000098765432"
        }
      },
      "debtor": {
        "name": "John Consumer",
        "postalAddress": {
          "country": "SG"
        }
      },
      "debtorAccount": {
        "identification": {
          "iban": "SG44DBSS0000000013579246"
        }
      }
    }
  ],
  "processingMetadata": {
    "receivedTimestamp": 1733065200000,
    "sourceService": "fast-router-service",
    "targetService": "fast-inward-clearing-processor",
    "correlationId": "CORR-DDI-20241201-001",
    "slaStartTimestamp": 1733065200000
  }
}
```

### 2. External Service Integration APIs

**VAM Core Banking Integration:**
```java
@FeignClient(name = "vam-service", url = "${vam.service.url}")
public interface VAMServiceClient {
    
    @PostMapping("/api/v1/accounts/validate")
    @TimeLimiter(name = "vam-service", fallbackMethod = "validateAccountFallback")
    Mono<AccountValidationResponse> validateAccount(
        @RequestBody AccountValidationRequest request,
        @RequestHeader("X-Correlation-ID") String correlationId,
        @RequestHeader("X-Timeout-Ms") String timeoutMs);
    
    @PostMapping("/api/v1/payments/credit")
    @TimeLimiter(name = "vam-service", fallbackMethod = "executeCreditFallback")
    Mono<PaymentExecutionResponse> executeCredit(
        @RequestBody CreditTransferRequest request,
        @RequestHeader("X-Correlation-ID") String correlationId);
    
    @PostMapping("/api/v1/payments/debit")
    @TimeLimiter(name = "vam-service", fallbackMethod = "executeDebitFallback")  
    Mono<PaymentExecutionResponse> executeDebit(
        @RequestBody DirectDebitRequest request,
        @RequestHeader("X-Correlation-ID") String correlationId);
}
```

**VAM Request/Response Samples:**
```json
// Account Validation Request
{
  "accountNumber": "SG77OCBC0000000012345678",
  "accountType": "CURRENT",
  "currency": "SGD",
  "validationType": "EXISTENCE_AND_STATUS",
  "correlationId": "CORR-CTI-20241201-001"
}

// Account Validation Response
{
  "accountNumber": "SG77OCBC0000000012345678",
  "accountName": "TechStart Pte Ltd",
  "accountStatus": "ACTIVE",
  "accountType": "CURRENT",
  "currency": "SGD",
  "isValid": true,
  "validationTimestamp": "2024-12-01T10:30:00.156Z",
  "responseTimeMs": 156
}

// Credit Transfer Request
{
  "transactionId": "TXN-CTI-20241201001",
  "amount": 75000.00,
  "currency": "SGD",
  "creditAccount": "SG77OCBC0000000012345678",
  "debitAccount": "SG12DBSS0000000087654321",
  "remittanceInfo": "Invoice payment INV-2024-001",
  "correlationId": "CORR-CTI-20241201-001",
  "urgency": "HIGH"
}

// Payment Execution Response
{
  "transactionId": "TXN-CTI-20241201001",
  "executionStatus": "COMPLETED",
  "systemReference": "VAM-REF-20241201001",
  "executionTimestamp": "2024-12-01T10:30:01.234Z",
  "newBalance": 125000.00,
  "processingTimeMs": 987
}
```

### 3. Liquidity Service Integration

**gRPC Proto for Liquidity Check:**
```protobuf
service LiquidityService {
  rpc CheckLiquidityBalance(LiquidityCheckRequest) returns (LiquidityCheckResponse);
}

message LiquidityCheckRequest {
  string participant_id = 1;
  double amount = 2;
  string currency = 3;
  string transaction_type = 4;
  string reference = 5;
  string correlation_id = 6;
  int32 timeout_ms = 7;
}

message LiquidityCheckResponse {
  bool approved = 1;
  string participant_id = 2;
  double requested_amount = 3;
  double available_balance = 4;
  string approval_reference = 5;
  string rejection_reason = 6;
  google.protobuf.Timestamp response_timestamp = 7;
  int32 processing_time_ms = 8;
}
```

**Liquidity Check Implementation:**
```java
@Service
public class LiquidityCheckService {
    
    private final LiquidityServiceGrpc.LiquidityServiceBlockingStub liquidityStub;
    
    @CircuitBreaker(name = "liquidity-service", fallbackMethod = "liquidityCheckFallback")
    @TimeLimiter(name = "liquidity-service")
    public Mono<LiquidityResult> checkLiquidity(PaymentMessage payment, String correlationId) {
        return Mono.fromCallable(() -> {
            LiquidityCheckRequest request = LiquidityCheckRequest.newBuilder()
                .setParticipantId(extractParticipantId(payment))
                .setAmount(payment.getAmount())
                .setCurrency(payment.getCurrency())
                .setTransactionType(payment.getMessageType().name())
                .setReference(payment.getTransactionId())
                .setCorrelationId(correlationId)
                .setTimeoutMs(200)  // 200ms SLA budget
                .build();
            
            LiquidityCheckResponse response = liquidityStub
                .withDeadlineAfter(200, TimeUnit.MILLISECONDS)
                .checkLiquidityBalance(request);
            
            return LiquidityResult.builder()
                .approved(response.getApproved())
                .availableBalance(response.getAvailableBalance())
                .approvalReference(response.getApprovalReference())
                .processingTime(response.getProcessingTimeMs())
                .build();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofMillis(250)); // Buffer for network overhead
    }
    
    public Mono<LiquidityResult> liquidityCheckFallback(PaymentMessage payment, String correlationId, Exception ex) {
        // Emergency liquidity approval for small amounts
        if (payment.getAmount() <= 10000.00) {
            return Mono.just(LiquidityResult.builder()
                .approved(true)
                .approvalReference("EMERGENCY-FALLBACK")
                .fallbackUsed(true)
                .build());
        }
        
        return Mono.just(LiquidityResult.builder()
            .approved(false)
            .rejectionReason("LIQUIDITY_SERVICE_UNAVAILABLE")
            .fallbackUsed(true)
            .build());
    }
}
```

## Fraud Detection & ML Model Integration

```mermaid
graph TB
    subgraph "ML Fraud Detection Pipeline"
        FeatureExtractor[Feature Extractor<br/>Real-time Features<br/>< 50ms]
        ModelInference[ML Model Inference<br/>TensorFlow Serving<br/>< 300ms]
        RuleEngine[Rule Engine<br/>Business Rules<br/>< 100ms]
        ScoreAggregator[Score Aggregator<br/>Weighted Combination<br/>< 50ms]
    end
    
    subgraph "Fraud Models"
        TransactionModel[Transaction Model<br/>Amount, Velocity, Pattern]
        AccountModel[Account Model<br/>History, Behavior, Risk]
        NetworkModel[Network Model<br/>IP, Device, Location]
        EntityModel[Entity Model<br/>Customer, Merchant, Bank]
    end
    
    subgraph "Feature Store"
        RealTimeFeatures[Real-time Features<br/>Redis Cache<br/>TTL: 1 hour]
        HistoricalFeatures[Historical Features<br/>BigQuery ML<br/>Batch Computed]
        StaticFeatures[Static Features<br/>Cloud SQL<br/>Reference Data]
    end
    
    subgraph "Fraud Decision"
        ScoreThreshold{Fraud Score<br/>>= 0.7?}
        RiskLevel[Risk Level<br/>LOW/MEDIUM/HIGH/CRITICAL]
        FraudDecision[Fraud Decision<br/>APPROVE/REVIEW/REJECT]
        AuditLog[Fraud Audit Log<br/>Regulatory Compliance]
    end
    
    FeatureExtractor --> ModelInference
    ModelInference --> ScoreAggregator
    RuleEngine --> ScoreAggregator
    ScoreAggregator --> ScoreThreshold
    
    ModelInference --> TransactionModel
    ModelInference --> AccountModel
    ModelInference --> NetworkModel
    ModelInference --> EntityModel
    
    FeatureExtractor --> RealTimeFeatures
    FeatureExtractor --> HistoricalFeatures
    FeatureExtractor --> StaticFeatures
    
    ScoreThreshold --> RiskLevel
    RiskLevel --> FraudDecision
    FraudDecision --> AuditLog
```

## Performance Monitoring & SLA Tracking

```mermaid
graph TB
    subgraph "SLA Monitoring Dashboard"
        SLAMetrics[SLA Metrics<br/>• 4.5s Compliance: 99.5%<br/>• Average Processing: 2.8s<br/>• P95 Processing: 4.1s<br/>• P99 Processing: 4.4s]
        
        StageBreakdown[Stage Breakdown<br/>• Business Rules: 145ms<br/>• Fraud Detection: 425ms<br/>• Account Validation: 178ms<br/>• Liquidity Check: 156ms<br/>• Payment Execution: 1.2s<br/>• Response Generation: 89ms]
        
        ErrorTracking[Error Tracking<br/>• Validation Errors: 0.2%<br/>• Fraud Rejections: 1.1%<br/>• Account Errors: 0.1%<br/>• Liquidity Rejections: 0.8%<br/>• Execution Failures: 0.3%]
    end
    
    subgraph "Alerting Thresholds"
        Warning[WARNING<br/>• Processing Time >3.5s<br/>• Error Rate >2%<br/>• SLA Compliance <98%]
        
        Critical[CRITICAL<br/>• Processing Time >4.2s<br/>• Error Rate >5%<br/>• SLA Compliance <95%]
        
        Emergency[EMERGENCY<br/>• Processing Time >4.5s<br/>• SLA Breach Detected<br/>• External Service Down]
    end
    
    subgraph "Circuit Breaker States"
        HealthyServices[ALL HEALTHY<br/>Normal Processing<br/>Full Feature Set]
        
        DegradedServices[SOME DEGRADED<br/>Fallback Processing<br/>Reduced Features]
        
        CriticalServices[CRITICAL STATE<br/>Emergency Processing<br/>Manual Oversight]
    end
    
    SLAMetrics --> Warning
    StageBreakdown --> Critical
    ErrorTracking --> Emergency
    
    HealthyServices -->|Service Degradation| DegradedServices
    DegradedServices -->|Critical Failure| CriticalServices
    CriticalServices -->|Service Recovery| HealthyServices
```

## Technology Stack & Configuration

```yaml
Application Configuration:
  spring:
    application:
      name: fast-inward-clearing-processor
    kafka:
      consumer:
        group-id: fast-inward-clearing-processor
        enable-auto-commit: false
        auto-offset-reset: latest
        max-poll-records: 1  # SLA-critical: one message at a time
    
    # Virtual Threads for SLA performance
    threads:
      virtual:
        enabled: true
    
    cloud:
      circuitbreaker:
        resilience4j:
          enabled: true

SLA Configuration:
  sla:
    inward-payment:
      target-duration: 4500ms
      warning-threshold: 3500ms
      critical-threshold: 4200ms
      timeout-stages:
        business-validation: 200ms
        fraud-detection: 500ms
        account-validation: 200ms
        liquidity-check: 200ms
        payment-execution: 1500ms
        response-generation: 100ms

Circuit Breaker Configuration:
  resilience4j:
    circuitbreaker:
      instances:
        vam-service:
          failure-rate-threshold: 20
          slow-call-rate-threshold: 30
          slow-call-duration-threshold: 1000ms
          wait-duration-in-open-state: 30s
        fraud-detection:
          failure-rate-threshold: 10
          slow-call-rate-threshold: 20
          slow-call-duration-threshold: 500ms
          wait-duration-in-open-state: 15s
        liquidity-service:
          failure-rate-threshold: 5
          slow-call-rate-threshold: 10
          slow-call-duration-threshold: 200ms
          wait-duration-in-open-state: 10s

JVM Optimization:
  memory: "-Xms4g -Xmx8g"
  gc: "-XX:+UseZGC -XX:MaxGCPauseMillis=50"
  virtual-threads: "--enable-preview -XX:+UseVirtualThreads"
  performance: "-XX:+AlwaysPreTouch -XX:+UseTransparentHugePages"
  monitoring: "-XX:+FlightRecorder -XX:StartFlightRecording=duration=0s"
```