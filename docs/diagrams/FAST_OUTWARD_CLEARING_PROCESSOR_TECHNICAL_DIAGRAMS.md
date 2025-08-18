# Fast Outward Clearing Processor - Technical Architecture & Exception Handling

## Service Responsibilities & Authorization Architecture

```mermaid
graph TB
    subgraph "External Initiators"
        PSPGlobal[PSP Global<br/>Payment Initiation<br/>REST API Calls]
        WebMethods[WebMethods<br/>Outward Payment Processing<br/>SOAP/REST Integration]
        OpsConsole[Operations Console<br/>Manual Payment Processing<br/>Web Interface]
    end
    
    subgraph "Fast Outward Clearing Processor - Core Responsibilities"
        RestAPIGateway[REST API Gateway<br/>OpenAPI 3.0 Contract<br/>OAuth2 + JWT Security]
        RequestValidator[Request Validator<br/>Schema + Business Validation<br/>Target: <300ms]
        RiskAssessmentEngine[Risk Assessment Engine<br/>AML/CFT + Sanctions<br/>Target: <800ms]
        ComplianceChecker[Compliance Checker<br/>Cross-border Rules<br/>Target: <400ms]
        AuthorizationManager[Authorization Manager<br/>Multi-stage Authorization<br/>Target: <500ms]
        PaymentOrchestrator[Payment Orchestrator<br/>Workflow Coordination<br/>Target: <600ms]
        SettlementCoordinator[Settlement Coordinator<br/>External System Integration<br/>Target: <1s]
        AuditLogger[Audit Logger<br/>Regulatory Compliance<br/>Real-time Logging]
    end
    
    subgraph "Exception Handling & Fallback"
        CircuitBreaker[Circuit Breaker<br/>External Service Monitor]
        TimeoutManager[Timeout Manager<br/>5s SLA Enforcement]
        AuthFallback[Authorization Fallback<br/>Manual Review Queue]
        ComplianceFallback[Compliance Fallback<br/>Enhanced Due Diligence]
        RetryOrchestrator[Retry Orchestrator<br/>Intelligent Backoff]
        AlertManager[Alert Manager<br/>Multi-channel Notifications]
    end
    
    subgraph "Data Layer"
        Redis[(Redis Cluster<br/>Authorization Cache<br/>Risk Scores<br/>< 5ms reads)]
        Spanner[(Cloud Spanner<br/>Payment State<br/>Authorization Trail<br/>ACID Transactions)]
        BigQuery[(BigQuery<br/>Risk Analytics<br/>ML Feature Store)]
        S3Compliance[(S3 Bucket<br/>Compliance Documents<br/>Encrypted Storage)]
    end
    
    subgraph "External Integration"
        LiquidityService[Liquidity Service<br/>Balance Authorization<br/>REST API]
        AvailabilityService[Availability Service<br/>Bank Status Check<br/>gRPC Client]
        ComplianceService[Compliance Service<br/>AML/CFT Screening<br/>External API]
        SenderService[Sender Service<br/>Message Transmission<br/>Kafka Producer]
        SettlementSystem[Settlement System<br/>G3 Settlement Interface<br/>External API]
        RiskManagement[Risk Management<br/>Enterprise Risk Platform<br/>REST API]
    end
    
    PSPGlobal --> RestAPIGateway
    WebMethods --> RestAPIGateway
    OpsConsole --> RestAPIGateway
    
    RestAPIGateway --> RequestValidator
    RequestValidator --> RiskAssessmentEngine
    RiskAssessmentEngine --> ComplianceChecker
    ComplianceChecker --> AuthorizationManager
    AuthorizationManager --> PaymentOrchestrator
    PaymentOrchestrator --> SettlementCoordinator
    SettlementCoordinator --> AuditLogger
    
    %% External Service Calls
    RiskAssessmentEngine --> ComplianceService
    AuthorizationManager --> LiquidityService
    AuthorizationManager --> AvailabilityService
    PaymentOrchestrator --> SenderService
    SettlementCoordinator --> SettlementSystem
    RiskAssessmentEngine --> RiskManagement
    
    %% Exception Flows
    RequestValidator -.->|Validation Timeout| CircuitBreaker
    RiskAssessmentEngine -.->|Risk Service Down| CircuitBreaker
    ComplianceChecker -.->|Compliance Timeout| CircuitBreaker
    AuthorizationManager -.->|Authorization Failed| CircuitBreaker
    SettlementCoordinator -.->|Settlement Error| CircuitBreaker
    
    CircuitBreaker --> TimeoutManager
    TimeoutManager --> RetryOrchestrator
    RetryOrchestrator -.->|Retry Success| PaymentOrchestrator
    RetryOrchestrator -.->|Max Retries| AuthFallback
    
    AuthFallback --> ComplianceFallback
    ComplianceFallback --> AlertManager
    TimeoutManager --> AlertManager
    
    %% Data Flows
    RiskAssessmentEngine <--> BigQuery
    AuthorizationManager <--> Redis
    PaymentOrchestrator <--> Spanner
    ComplianceChecker --> S3Compliance
```

## Multi-stage Authorization Flow & Exception Handling

```mermaid
flowchart TD
    Start([Payment Request<br/>PSP Global/WebMethods]) --> ValidateRequest{Request Schema<br/>Validation<br/>Budget: 300ms}
    
    ValidateRequest -->|Valid| RiskAssessment{Risk Assessment<br/>AML/CFT Screening<br/>Budget: 800ms}
    ValidateRequest -->|Invalid| BadRequest[400 Bad Request<br/>Validation Errors<br/>< 50ms]
    BadRequest --> RequestComplete([Request Completed])
    
    RiskAssessment -->|Low Risk| ComplianceCheck{Compliance Check<br/>Cross-border Rules<br/>Budget: 400ms}
    RiskAssessment -->|High Risk| HighRiskReview[Queue for<br/>Manual Review<br/>< 100ms]
    RiskAssessment -->|Timeout| RiskTimeout{Risk Timeout<br/>Elapsed < 1.5s?}
    
    RiskTimeout -->|Yes| RiskFallback[Use Cached<br/>Risk Profile<br/>< 100ms]
    RiskTimeout -->|No| SLABreach[5s SLA Breach<br/>Emergency Processing]
    RiskFallback --> ComplianceCheck
    
    ComplianceCheck -->|Compliant| LiquidityAuth{Liquidity<br/>Authorization<br/>Budget: 500ms}
    ComplianceCheck -->|Non-compliant| ComplianceReject[Reject Payment<br/>Compliance Violation<br/>< 100ms]
    ComplianceCheck -->|Timeout| ComplianceTimeout{Compliance Timeout<br/>Elapsed < 2.5s?}
    
    ComplianceTimeout -->|Yes| ComplianceFallback[Enhanced Due<br/>Diligence Review<br/>< 200ms]
    ComplianceTimeout -->|No| SLABreach
    ComplianceFallback --> LiquidityAuth
    
    LiquidityAuth -->|Authorized| BankAvailability{Bank Availability<br/>Check<br/>Budget: 200ms}
    LiquidityAuth -->|Rejected| LiquidityReject[Reject Payment<br/>Insufficient Liquidity<br/>< 100ms]
    LiquidityAuth -->|Timeout| LiquidityTimeout{Liquidity Timeout<br/>Elapsed < 3.5s?}
    
    LiquidityTimeout -->|Yes| LiquidityFallback[Emergency<br/>Pre-authorization<br/>< 100ms]
    LiquidityTimeout -->|No| SLABreach
    LiquidityFallback --> BankAvailability
    
    BankAvailability -->|Available| FinalAuthorization{Final Authorization<br/>Aggregate Decision<br/>Budget: 300ms}
    BankAvailability -->|Unavailable| BankUnavailable[Reject Payment<br/>Bank Unavailable<br/>< 100ms]
    BankAvailability -->|Timeout| AvailabilityTimeout{Availability Timeout<br/>Elapsed < 4s?}
    
    AvailabilityTimeout -->|Yes| AvailabilityFallback[Assume Available<br/>+ Alert Ops<br/>< 100ms]
    AvailabilityTimeout -->|No| SLABreach
    AvailabilityFallback --> FinalAuthorization
    
    FinalAuthorization -->|Approved| PaymentOrchestration{Payment Orchestration<br/>Settlement Coordination<br/>Budget: 800ms}
    FinalAuthorization -->|Rejected| FinalReject[Reject Payment<br/>Authorization Denied<br/>< 100ms]
    FinalAuthorization -->|Timeout| AuthTimeout{Auth Timeout<br/>Elapsed < 4.5s?}
    
    AuthTimeout -->|Yes| AuthFallback[Manual Authorization<br/>Queue + Alert<br/>< 200ms]
    AuthTimeout -->|No| SLABreach
    
    PaymentOrchestration -->|Success| SendToTransmission[Send to<br/>Sender Service<br/>< 200ms]
    PaymentOrchestration -->|Failed| OrchestrationError[Orchestration Failed<br/>System Error<br/>< 100ms]
    PaymentOrchestration -->|Timeout| OrchestrationTimeout{Orchestration Timeout<br/>Elapsed < 4.8s?}
    
    OrchestrationTimeout -->|Yes| AsyncProcessing[Asynchronous<br/>Processing<br/>< 200ms]
    OrchestrationTimeout -->|No| SLABreach
    
    SendToTransmission --> CheckSLA{Total Time<br/>< 5s?}
    AsyncProcessing --> CheckSLA
    AuthFallback --> ManualQueue[Manual Review<br/>Queue]
    
    CheckSLA -->|Yes| SLACompliant([SLA: ✓ < 5s<br/>Payment Authorized])
    CheckSLA -->|No| SLABreach
    
    SLABreach --> EmergencyEscalation[Emergency Escalation<br/>• Critical Alert<br/>• Ops Investigation<br/>• Executive Notification]
    EmergencyEscalation --> SLAViolation([SLA: ✗ > 5s<br/>Manual Intervention])
    
    HighRiskReview --> ManualQueue
    ComplianceReject --> RequestComplete
    LiquidityReject --> RequestComplete
    BankUnavailable --> RequestComplete
    FinalReject --> RequestComplete
    OrchestrationError --> RequestComplete
    
    style SLABreach fill:#ff6666
    style SLAViolation fill:#ff3333
    style EmergencyEscalation fill:#ffcc99
    style SLACompliant fill:#ccffcc
    style ManualQueue fill:#ffffcc
```

## REST API Contracts for PSP Global & WebMethods

### 1. Payment Initiation API

```yaml
openapi: 3.0.3
info:
  title: Fast Outward Clearing Processor API
  version: 1.0.0
  description: Outward payment authorization and processing for PSP Global and WebMethods

paths:
  /api/v1/outward-payments/initiate:
    post:
      summary: Initiate outward payment
      description: 5-second SLA for complete authorization process
      operationId: initiateOutwardPayment
      security:
        - OAuth2: [payment:initiate]
        - ApiKey: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/OutwardPaymentRequest'
            examples:
              pspGlobalRequest:
                summary: PSP Global Credit Transfer
                value:
                  requestId: "PSP-OUT-20241201-001"
                  paymentType: "CREDIT_TRANSFER"
                  amount: 250000.00
                  currency: "SGD"
                  urgency: "HIGH"
                  originator:
                    name: "ANZ Bank Singapore"
                    account: "SG12ANZB0000000012345678"
                    bic: "ANZBSGSG"
                  beneficiary:
                    name: "Global Trading Corp"
                    account: "SG99OCBC0000000087654321"
                    bic: "OCBCSGSG"
                    country: "SG"
                  remittanceInfo: "Trade finance settlement TF-2024-001"
                  regulatoryInfo:
                    purposeCode: "TRADE"
                    reportingFlag: true
                  pspGlobalCorrelationId: "PSP-CORR-20241201-001"
              webMethodsRequest:
                summary: WebMethods Cross-border Payment
                value:
                  requestId: "WM-OUT-20241201-001"
                  paymentType: "CROSS_BORDER_TRANSFER"
                  amount: 100000.00
                  currency: "USD"
                  urgency: "NORMAL"
                  originator:
                    name: "Singapore Export Co"
                    account: "SG44DBSS0000000098765432"
                    bic: "DBSSSGSG"
                  beneficiary:
                    name: "US Import LLC"
                    account: "US12CITI0000000013579246"
                    bic: "CITIUS33"
                    country: "US"
                  exchangeRate: 1.3456
                  remittanceInfo: "Export payment INV-EXP-2024-001"
                  regulatoryInfo:
                    purposeCode: "GOODS"
                    reportingFlag: true
                    crossBorderInfo:
                      sourceCountry: "SG"
                      destinationCountry: "US"
                      tradeReference: "TRADE-2024-001"
                  webMethodsTransactionId: "WM-TXN-20241201-001"
      responses:
        200:
          description: Payment authorized and processing initiated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OutwardPaymentResponse'
              examples:
                authorized:
                  summary: Payment Authorized
                  value:
                    paymentId: "OUT-PAY-20241201-001"
                    requestId: "PSP-OUT-20241201-001"
                    status: "AUTHORIZED"
                    authorizationReference: "AUTH-20241201-001"
                    authorizationTimestamp: "2024-12-01T16:30:00.234Z"
                    estimatedSettlementTime: "2024-12-01T16:32:00.000Z"
                    processingTimeMs: 3456
                    riskScore: 0.15
                    riskLevel: "LOW"
                    liquidityStatus: "APPROVED"
                    complianceStatus: "CLEARED"
                    trackingInfo:
                      correlationId: "CORR-OUT-20241201-001"
                      traceId: "TRACE-OUT-20241201-001"
                rejected:
                  summary: Payment Rejected
                  value:
                    paymentId: "OUT-PAY-20241201-002"
                    requestId: "PSP-OUT-20241201-002"
                    status: "REJECTED"
                    rejectionReason: "HIGH_RISK_DETECTED"
                    rejectionCode: "RISK_001"
                    rejectionDetails: "Sanctions screening flagged beneficiary"
                    processingTimeMs: 1234
                    riskScore: 0.89
                    riskLevel: "HIGH"
                    reviewRequired: true
                    reviewReference: "REV-20241201-001"
        400:
          description: Invalid request
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        401:
          description: Unauthorized
        403:
          description: Forbidden - insufficient permissions
        422:
          description: Business validation failed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                liquidityRejection:
                  summary: Insufficient Liquidity
                  value:
                    error: "INSUFFICIENT_LIQUIDITY"
                    message: "Payment amount exceeds available liquidity"
                    details:
                      requestedAmount: 1000000.00
                      availableBalance: 750000.00
                      participantId: "ANZBSGSG"
                complianceRejection:
                  summary: Compliance Violation
                  value:
                    error: "COMPLIANCE_VIOLATION"
                    message: "Cross-border payment exceeds regulatory limit"
                    details:
                      violationType: "DAILY_LIMIT_EXCEEDED"
                      currentDailyTotal: 9500000.00
                      dailyLimit: 10000000.00
                      requestedAmount: 750000.00
        503:
          description: Service temporarily unavailable

  /api/v1/outward-payments/{paymentId}/status:
    get:
      summary: Get payment status
      parameters:
        - name: paymentId
          in: path
          required: true
          schema:
            type: string
        - name: includeDetailedStatus
          in: query
          schema:
            type: boolean
            default: false
      responses:
        200:
          description: Payment status
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentStatusResponse'
              example:
                paymentId: "OUT-PAY-20241201-001"
                currentStatus: "SETTLING"
                statusHistory:
                  - status: "AUTHORIZED"
                    timestamp: "2024-12-01T16:30:00.234Z"
                  - status: "TRANSMITTED"
                    timestamp: "2024-12-01T16:30:01.567Z"
                  - status: "SETTLING"
                    timestamp: "2024-12-01T16:31:30.123Z"
                estimatedCompletion: "2024-12-01T16:32:00.000Z"
                transmissionReference: "TRANS-20241201-001"
                settlementReference: "SETTLE-20241201-001"

  /api/v1/outward-payments/{paymentId}/cancel:
    post:
      summary: Cancel pending payment
      parameters:
        - name: paymentId
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CancellationRequest'
            example:
              cancellationReason: "CUSTOMER_REQUEST"
              operatorId: "ops.user@anz.com"
              cancellationReference: "CANCEL-20241201-001"
      responses:
        200:
          description: Cancellation processed
        409:
          description: Payment cannot be cancelled (already settled)

components:
  schemas:
    OutwardPaymentRequest:
      type: object
      required:
        - requestId
        - paymentType
        - amount
        - currency
        - originator
        - beneficiary
      properties:
        requestId:
          type: string
          maxLength: 35
          description: Unique request identifier from initiating system
        paymentType:
          type: string
          enum: [CREDIT_TRANSFER, CROSS_BORDER_TRANSFER, URGENT_PAYMENT]
        amount:
          type: number
          minimum: 0.01
          maximum: 999999999.99
        currency:
          type: string
          enum: [SGD, USD, EUR, GBP, JPY, AUD]
        urgency:
          type: string
          enum: [HIGH, NORMAL, LOW]
          default: NORMAL
        originator:
          $ref: '#/components/schemas/PaymentParty'
        beneficiary:
          $ref: '#/components/schemas/PaymentParty'
        exchangeRate:
          type: number
          description: Exchange rate for cross-currency payments
        remittanceInfo:
          type: string
          maxLength: 1000
        regulatoryInfo:
          $ref: '#/components/schemas/RegulatoryInfo'
        pspGlobalCorrelationId:
          type: string
          description: PSP Global correlation ID
        webMethodsTransactionId:
          type: string
          description: WebMethods transaction ID
        
    PaymentParty:
      type: object
      required:
        - name
        - account
        - bic
      properties:
        name:
          type: string
          maxLength: 140
        account:
          type: string
          maxLength: 34
        bic:
          type: string
          pattern: '^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$'
        country:
          type: string
          pattern: '^[A-Z]{2}$'
        address:
          $ref: '#/components/schemas/Address'
        
    RegulatoryInfo:
      type: object
      properties:
        purposeCode:
          type: string
          enum: [TRADE, SERVICES, INVESTMENT, PERSONAL, OTHER]
        reportingFlag:
          type: boolean
          default: false
        crossBorderInfo:
          type: object
          properties:
            sourceCountry:
              type: string
            destinationCountry:
              type: string
            tradeReference:
              type: string
```

### 2. WebMethods SOAP Integration

**SOAP Request Sample:**
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:out="http://anz.com/fastpayment/outward/v1">
  <soapenv:Header>
    <wm:CorrelationID>WM-CORR-20241201-001</wm:CorrelationID>
    <wm:TransactionID>WM-TXN-20241201-001</wm:TransactionID>
    <wm:Timeout>5000</wm:Timeout>
  </soapenv:Header>
  <soapenv:Body>
    <out:InitiateOutwardPaymentRequest>
      <requestId>WM-OUT-20241201-001</requestId>
      <paymentType>CROSS_BORDER_TRANSFER</paymentType>
      <amount>100000.00</amount>
      <currency>USD</currency>
      <urgency>NORMAL</urgency>
      <originator>
        <name>Singapore Export Co</name>
        <account>SG44DBSS0000000098765432</account>
        <bic>DBSSSGSG</bic>
      </originator>
      <beneficiary>
        <name>US Import LLC</name>
        <account>US12CITI0000000013579246</account>
        <bic>CITIUS33</bic>
        <country>US</country>
      </beneficiary>
      <exchangeRate>1.3456</exchangeRate>
      <remittanceInfo>Export payment INV-EXP-2024-001</remittanceInfo>
      <regulatoryInfo>
        <purposeCode>GOODS</purposeCode>
        <reportingFlag>true</reportingFlag>
        <crossBorderInfo>
          <sourceCountry>SG</sourceCountry>
          <destinationCountry>US</destinationCountry>
          <tradeReference>TRADE-2024-001</tradeReference>
        </crossBorderInfo>
      </regulatoryInfo>
      <webMethodsTransactionId>WM-TXN-20241201-001</webMethodsTransactionId>
    </out:InitiateOutwardPaymentRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

**SOAP Response Sample:**
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Header>
    <wm:CorrelationID>WM-CORR-20241201-001</wm:CorrelationID>
    <wm:ResponseTime>3456</wm:ResponseTime>
  </soapenv:Header>
  <soapenv:Body>
    <out:InitiateOutwardPaymentResponse>
      <paymentId>OUT-PAY-20241201-001</paymentId>
      <requestId>WM-OUT-20241201-001</requestId>
      <status>AUTHORIZED</status>
      <authorizationReference>AUTH-20241201-001</authorizationReference>
      <authorizationTimestamp>2024-12-01T16:30:00.234Z</authorizationTimestamp>
      <estimatedSettlementTime>2024-12-01T16:32:00.000Z</estimatedSettlementTime>
      <processingTimeMs>3456</processingTimeMs>
      <riskScore>0.15</riskScore>
      <riskLevel>LOW</riskLevel>
      <liquidityStatus>APPROVED</liquidityStatus>
      <complianceStatus>CLEARED</complianceStatus>
      <trackingInfo>
        <correlationId>CORR-OUT-20241201-001</correlationId>
        <traceId>TRACE-OUT-20241201-001</traceId>
      </trackingInfo>
      <webMethodsTransactionId>WM-TXN-20241201-001</webMethodsTransactionId>
    </out:InitiateOutwardPaymentResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

## Risk Assessment & Compliance Engine

```mermaid
graph TB
    subgraph "Risk Assessment Engine"
        AMLScreening[AML Screening<br/>OFAC, UN, EU Lists<br/>Target: <300ms]
        SanctionsCheck[Sanctions Check<br/>Real-time API<br/>Target: <200ms]
        PEPScreening[PEP Screening<br/>Politically Exposed Persons<br/>Target: <200ms]
        CountryRisk[Country Risk Assessment<br/>Cross-border Analysis<br/>Target: <100ms]
    end
    
    subgraph "Compliance Framework"
        CrossBorderRules[Cross-border Rules<br/>Regulatory Limits<br/>MAS Guidelines]
        TradingLimits[Trading Limits<br/>Daily/Monthly Caps<br/>Real-time Tracking]
        ReportingRules[Reporting Rules<br/>CTR/STR Thresholds<br/>Automated Filing]
        DocumentationReqs[Documentation Requirements<br/>Trade Finance<br/>KYC Verification]
    end
    
    subgraph "Decision Matrix"
        RiskScoring[Risk Scoring<br/>Weighted Algorithm<br/>0.0 - 1.0 Scale]
        ThresholdMatrix[Threshold Matrix<br/>• 0.0-0.3: Auto Approve<br/>• 0.3-0.7: Enhanced Review<br/>• 0.7-1.0: Reject/Manual]
        BusinessDecision[Business Decision<br/>APPROVE/REVIEW/REJECT]
        ComplianceDocGen[Compliance Documentation<br/>Automated Generation]
    end
    
    subgraph "Fallback & Override"
        ManualReview[Manual Review Queue<br/>High-risk Payments<br/>Compliance Officer]
        EmergencyOverride[Emergency Override<br/>Senior Management<br/>Critical Payments]
        AuditTrail[Audit Trail<br/>All Decisions<br/>Regulatory Compliance]
    end
    
    AMLScreening --> RiskScoring
    SanctionsCheck --> RiskScoring
    PEPScreening --> RiskScoring
    CountryRisk --> RiskScoring
    
    CrossBorderRules --> ThresholdMatrix
    TradingLimits --> ThresholdMatrix
    ReportingRules --> ThresholdMatrix
    DocumentationReqs --> ThresholdMatrix
    
    RiskScoring --> ThresholdMatrix
    ThresholdMatrix --> BusinessDecision
    BusinessDecision --> ComplianceDocGen
    
    BusinessDecision -.->|REVIEW| ManualReview
    ManualReview -.->|Escalation| EmergencyOverride
    BusinessDecision --> AuditTrail
    ManualReview --> AuditTrail
    EmergencyOverride --> AuditTrail
```

## Performance Monitoring & 5-Second SLA Management

```mermaid
graph TB
    subgraph "SLA Performance Dashboard"
        SLAMetrics[5-Second SLA Metrics<br/>• Compliance Rate: 99.2%<br/>• Average Processing: 3.1s<br/>• P95 Processing: 4.7s<br/>• P99 Processing: 4.9s]
        
        StageBreakdown[Authorization Stage Breakdown<br/>• Request Validation: 289ms<br/>• Risk Assessment: 756ms<br/>• Compliance Check: 378ms<br/>• Liquidity Auth: 445ms<br/>• Final Authorization: 267ms<br/>• Orchestration: 634ms<br/>• Response Generation: 178ms]
        
        ErrorTracking[Error & Rejection Tracking<br/>• Validation Errors: 0.3%<br/>• Risk Rejections: 2.1%<br/>• Compliance Violations: 0.8%<br/>• Liquidity Rejections: 1.2%<br/>• System Errors: 0.2%]
    end
    
    subgraph "Business Intelligence"
        PaymentVolume[Payment Volume Analytics<br/>• Daily Volume: 2,500 payments<br/>• Peak TPS: 12 payments/sec<br/>• Average Amount: $125,000<br/>• Cross-border: 35%]
        
        RiskDistribution[Risk Score Distribution<br/>• Low Risk (0.0-0.3): 78%<br/>• Medium Risk (0.3-0.7): 19%<br/>• High Risk (0.7-1.0): 3%]
        
        ComplianceMetrics[Compliance Metrics<br/>• Manual Reviews: 2.8%<br/>• CTR Filings: 0.2%<br/>• STR Reports: 0.05%<br/>• Override Rate: 0.1%]
    end
    
    subgraph "Operational Alerting"
        SLAWarning[SLA WARNING<br/>• Processing >4s<br/>• Success Rate <98%<br/>• Queue Depth >50]
        
        SLACritical[SLA CRITICAL<br/>• Processing >4.5s<br/>• Success Rate <95%<br/>• External Service Down]
        
        BusinessCritical[BUSINESS CRITICAL<br/>• High Risk Spike<br/>• Compliance Violation<br/>• Manual Override Required]
    end
    
    SLAMetrics --> SLAWarning
    StageBreakdown --> SLACritical
    ErrorTracking --> BusinessCritical
    
    PaymentVolume --> SLAWarning
    RiskDistribution --> BusinessCritical
    ComplianceMetrics --> BusinessCritical
```

## Technology Stack & Security Configuration

```yaml
Application Configuration:
  spring:
    application:
      name: fast-outward-clearing-processor
    security:
      oauth2:
        resourceserver:
          jwt:
            issuer-uri: https://auth.anz.com.sg/oauth2
            jwk-set-uri: https://auth.anz.com.sg/.well-known/jwks.json
    
    webflux:
      # High-performance reactive stack
      max-connections: 1000
      connection-timeout: 30s
    
    # Virtual threads for authorization performance  
    threads:
      virtual:
        enabled: true

Security Configuration:
  oauth2:
    client:
      registration:
        psp-global:
          client-id: psp-global-client
          client-secret: ${PSP_GLOBAL_SECRET}
          scope: payment:initiate,payment:status
        webmethods:
          client-id: webmethods-client
          client-secret: ${WEBMETHODS_SECRET}
          scope: payment:initiate,payment:cancel
  
  api-security:
    rate-limiting:
      psp-global: 100/minute
      webmethods: 200/minute
      operations: 50/minute
    
    encryption:
      field-level: true
      algorithm: AES-256-GCM
      key-rotation: 90-days

Business Rules Configuration:
  authorization:
    sla:
      target-duration: 5000ms
      warning-threshold: 4000ms
      critical-threshold: 4500ms
    
    limits:
      single-payment-max: 10000000.00  # $10M SGD
      daily-participant-limit: 50000000.00  # $50M SGD
      cross-border-daily: 25000000.00  # $25M SGD
    
    risk-thresholds:
      auto-approve: 0.3
      manual-review: 0.7
      auto-reject: 0.9

Circuit Breaker Configuration:
  resilience4j:
    circuitbreaker:
      instances:
        liquidity-service:
          failure-rate-threshold: 10
          slow-call-rate-threshold: 20
          slow-call-duration-threshold: 500ms
          wait-duration-in-open-state: 30s
        compliance-service:
          failure-rate-threshold: 5
          slow-call-rate-threshold: 15
          slow-call-duration-threshold: 800ms
          wait-duration-in-open-state: 60s
        risk-management:
          failure-rate-threshold: 15
          slow-call-rate-threshold: 25
          slow-call-duration-threshold: 1000ms
          wait-duration-in-open-state: 45s

JVM Configuration:
  memory: "-Xms6g -Xmx12g"
  gc: "-XX:+UseZGC -XX:MaxGCPauseMillis=100"
  virtual-threads: "--enable-preview -XX:+UseVirtualThreads"
  security: "-Djava.security.properties=/app/config/java.security"
  monitoring: "-XX:+FlightRecorder -XX:StartFlightRecording=duration=0s"
```