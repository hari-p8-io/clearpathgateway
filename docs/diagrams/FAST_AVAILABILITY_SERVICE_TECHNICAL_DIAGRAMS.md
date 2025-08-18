# Fast Availability Service - Technical Architecture & Exception Handling

## Service Responsibilities & gRPC Integration Architecture

```mermaid
graph TB
    subgraph "gRPC Clients"
        RouterService[Fast Router Service<br/>gRPC Client<br/>Availability Checks]
        InwardProcessor[Fast Inward Processor<br/>gRPC Client<br/>Participant Status]
        OutwardProcessor[Fast Outward Processor<br/>gRPC Client<br/>Bank Validation]
        LiquidityService[Fast Liquidity Service<br/>gRPC Client<br/>Status Verification]
        OpsDashboard[Operations Dashboard<br/>Web gRPC Client<br/>Management Interface]
    end
    
    subgraph "Fast Availability Service - Core Responsibilities"
        gRPCServer[gRPC Server<br/>High-Performance RPC<br/>Protocol Buffers]
        StatusManager[Status Manager<br/>Real-time State Machine]
        HealthMonitor[Health Monitor<br/>Continuous Monitoring<br/>30s intervals]
        EventProcessor[Event Processor<br/>Status Change Handler]
        MaintenanceScheduler[Maintenance Scheduler<br/>Planned Downtime Manager]
        NotificationEngine[Notification Engine<br/>Event Broadcasting]
        MetricsCollector[Metrics Collector<br/>Performance Tracking]
    end
    
    subgraph "Fallback & Exception Handling"
        CircuitBreaker[Circuit Breaker<br/>Bank Connectivity Monitor]
        StatusFallback[Status Fallback Logic<br/>Last Known Good State]
        HealthFallback[Health Check Fallback<br/>Degraded Monitoring]
        EmergencyOverride[Emergency Override<br/>Manual Status Control]
        AlertingService[Multi-Channel Alerting<br/>PagerDuty + Slack + Email]
    end
    
    subgraph "Data Layer"
        Redis[(Redis Cluster<br/>Real-time Status<br/>< 10ms reads)]
        Spanner[(Cloud Spanner<br/>Status History<br/>Audit Trail)]
        EventStore[(Event Store<br/>Status Changes<br/>Event Sourcing)]
        ConfigDB[(Configuration DB<br/>Bank Details<br/>Maintenance Windows)]
    end
    
    subgraph "External Bank Monitoring"
        BankAPIs[Bank Public APIs<br/>Health Endpoints<br/>HTTP/HTTPS]
        NetworkMonitor[Network Connectivity<br/>Ping + Traceroute<br/>TCP Connect Tests]
        ServiceTests[Service-level Tests<br/>Synthetic Transactions<br/>Business Logic Validation]
    end
    
    RouterService -.->|gRPC Call| gRPCServer
    InwardProcessor -.->|gRPC Call| gRPCServer
    OutwardProcessor -.->|gRPC Call| gRPCServer
    LiquidityService -.->|gRPC Call| gRPCServer
    OpsDashboard -.->|gRPC-Web| gRPCServer
    
    gRPCServer --> StatusManager
    StatusManager --> HealthMonitor
    HealthMonitor --> EventProcessor
    EventProcessor --> MaintenanceScheduler
    MaintenanceScheduler --> NotificationEngine
    NotificationEngine --> MetricsCollector
    
    HealthMonitor --> BankAPIs
    HealthMonitor --> NetworkMonitor
    HealthMonitor --> ServiceTests
    
    %% Exception Flows
    HealthMonitor -.->|Bank Unreachable| CircuitBreaker
    StatusManager -.->|Status Conflict| StatusFallback
    HealthMonitor -.->|Monitor Failure| HealthFallback
    
    CircuitBreaker --> StatusFallback
    StatusFallback --> EmergencyOverride
    HealthFallback --> AlertingService
    
    %% Data Flows
    StatusManager <--> Redis
    StatusManager <--> Spanner
    EventProcessor --> EventStore
    MaintenanceScheduler <--> ConfigDB
```

## Exception Handling & Status Continuity

```mermaid
flowchart TD
    Start([gRPC Status Request]) --> ValidateRequest{Request<br/>Validation}
    
    ValidateRequest -->|Valid| CheckCache{Redis Status<br/>Cache Available?}
    ValidateRequest -->|Invalid| InvalidRequest[gRPC INVALID_ARGUMENT<br/>+ Error Details]
    
    CheckCache -->|Hit + Fresh| ServeFromCache[Serve from Cache<br/>< 10ms response]
    CheckCache -->|Miss/Stale| CheckDatabase{Spanner<br/>Available?}
    CheckCache -->|Redis Down| CheckDatabase
    
    CheckDatabase -->|Available| QueryStatus[Query Current Status<br/>+ Health Metrics]
    CheckDatabase -->|Down| FallbackMode{Last Known<br/>State Available?}
    
    QueryStatus --> ValidateStatus{Status<br/>Coherence Check}
    ValidateStatus -->|Coherent| UpdateCache[Update Cache<br/>+ Return Status]
    ValidateStatus -->|Conflicted| ResolveConflict[Conflict Resolution<br/>Latest Timestamp Wins]
    
    ResolveConflict --> UpdateCache
    UpdateCache --> Success([gRPC OK<br/>Status Returned])
    
    FallbackMode -->|Yes| LastKnownState[Return Last Known State<br/>+ DEGRADED flag]
    FallbackMode -->|No| UnknownState[Return UNKNOWN Status<br/>+ UNAVAILABLE gRPC]
    
    LastKnownState --> DegradedSuccess([gRPC OK<br/>Degraded Response])
    UnknownState --> ServiceUnavailable([gRPC UNAVAILABLE<br/>Service Degraded])
    
    %% Health Check Path
    HealthCheck([Periodic Health Check]) --> TestConnectivity{Bank Connectivity<br/>Test}
    TestConnectivity -->|Success| TestServices{Service Health<br/>Test}
    TestConnectivity -->|Failed| MarkUnavailable[Mark Bank<br/>UNAVAILABLE]
    
    TestServices -->|Healthy| MarkAvailable[Mark Bank<br/>AVAILABLE]
    TestServices -->|Degraded| MarkDegraded[Mark Bank<br/>DEGRADED]
    TestServices -->|Failed| MarkUnavailable
    
    MarkAvailable --> PublishEvent[Publish Status<br/>Change Event]
    MarkDegraded --> PublishEvent
    MarkUnavailable --> PublishEvent
    
    PublishEvent --> UpdateAllSources[Update Redis<br/>+ Spanner + EventStore]
    UpdateAllSources --> NotifySubscribers[Notify gRPC<br/>Subscribers]
    
    %% Emergency Override Path
    EmergencyOverride([Manual Override]) --> ValidateCredentials{Operations<br/>Authentication}
    ValidateCredentials -->|Valid| ForceStatusChange[Force Status<br/>Change + Audit]
    ValidateCredentials -->|Invalid| Unauthorized[gRPC PERMISSION_DENIED]
    
    ForceStatusChange --> CriticalAlert[Send Critical Alert<br/>Manual Override Active]
    CriticalAlert --> UpdateAllSources
    
    style InvalidRequest fill:#ffcccc
    style ServiceUnavailable fill:#ff9999
    style UnknownState fill:#ffffcc
    style CriticalAlert fill:#ff6666
    style Unauthorized fill:#ff3333
```

## gRPC Service Definition & Contracts

### 1. Protocol Buffer Definition

```protobuf
syntax = "proto3";

package fast.availability.v1;

import "google/protobuf/timestamp.proto";
import "google/protobuf/duration.proto";

option java_package = "com.anz.fastpayment.availability.grpc";
option java_multiple_files = true;

// Fast Availability Service
service AvailabilityService {
  // Get current status of a single participant
  rpc GetParticipantStatus(GetParticipantStatusRequest) returns (GetParticipantStatusResponse);
  
  // Get status of multiple participants (batch)
  rpc GetMultipleParticipantStatus(GetMultipleParticipantStatusRequest) returns (GetMultipleParticipantStatusResponse);
  
  // Subscribe to status changes (streaming)
  rpc SubscribeStatusChanges(SubscribeStatusChangesRequest) returns (stream StatusChangeEvent);
  
  // Update participant status (operations only)
  rpc UpdateParticipantStatus(UpdateParticipantStatusRequest) returns (UpdateParticipantStatusResponse);
  
  // Schedule maintenance window
  rpc ScheduleMaintenance(ScheduleMaintenanceRequest) returns (ScheduleMaintenanceResponse);
  
  // Get service health
  rpc GetServiceHealth(GetServiceHealthRequest) returns (GetServiceHealthResponse);
}

// Messages
message GetParticipantStatusRequest {
  string participant_id = 1; // BIC code (e.g., "OCBCSGSG")
  bool include_health_metrics = 2;
  bool include_maintenance_info = 3;
}

message GetParticipantStatusResponse {
  string participant_id = 1;
  ParticipantStatus status = 2;
  google.protobuf.Timestamp last_updated = 3;
  google.protobuf.Timestamp cache_timestamp = 4;
  HealthMetrics health_metrics = 5;
  MaintenanceInfo maintenance_info = 6;
  bool is_degraded_response = 7; // True if from fallback
}

message GetMultipleParticipantStatusRequest {
  repeated string participant_ids = 1;
  bool include_health_metrics = 2;
  bool include_maintenance_info = 3;
}

message GetMultipleParticipantStatusResponse {
  repeated ParticipantStatusInfo participants = 1;
  google.protobuf.Timestamp response_timestamp = 2;
  int32 total_participants = 3;
  int32 available_participants = 4;
  int32 degraded_participants = 5;
  int32 unavailable_participants = 6;
}

message SubscribeStatusChangesRequest {
  repeated string participant_ids = 1; // Empty = all participants
  repeated ParticipantStatus status_filter = 2; // Only these statuses
  string subscriber_id = 3;
}

message StatusChangeEvent {
  string event_id = 1;
  string participant_id = 2;
  ParticipantStatus previous_status = 3;
  ParticipantStatus new_status = 4;
  google.protobuf.Timestamp change_timestamp = 5;
  StatusChangeReason reason = 6;
  string additional_info = 7;
}

message UpdateParticipantStatusRequest {
  string participant_id = 1;
  ParticipantStatus new_status = 2;
  string reason = 3;
  string operator_id = 4;
  bool force_override = 5; // Emergency override
  google.protobuf.Duration duration = 6; // For temporary status
}

message UpdateParticipantStatusResponse {
  bool success = 1;
  string update_id = 2;
  google.protobuf.Timestamp effective_timestamp = 3;
  string previous_status = 4;
  string error_message = 5;
}

// Enums
enum ParticipantStatus {
  UNKNOWN = 0;
  AVAILABLE = 1;
  DEGRADED = 2;
  MAINTENANCE = 3;
  UNAVAILABLE = 4;
  SUSPENDED = 5;
}

enum StatusChangeReason {
  UNKNOWN_REASON = 0;
  HEALTH_CHECK_PASSED = 1;
  HEALTH_CHECK_FAILED = 2;
  NETWORK_CONNECTIVITY_LOST = 3;
  NETWORK_CONNECTIVITY_RESTORED = 4;
  PLANNED_MAINTENANCE = 5;
  UNPLANNED_OUTAGE = 6;
  MANUAL_OVERRIDE = 7;
  SYSTEM_STARTUP = 8;
  SYSTEM_SHUTDOWN = 9;
}

// Complex Types
message ParticipantStatusInfo {
  string participant_id = 1;
  ParticipantStatus status = 2;
  google.protobuf.Timestamp last_updated = 3;
  HealthMetrics health_metrics = 4;
  MaintenanceInfo maintenance_info = 5;
}

message HealthMetrics {
  double response_time_ms = 1;
  double availability_percentage = 2;
  int32 consecutive_failures = 3;
  google.protobuf.Timestamp last_health_check = 4;
  repeated HealthCheckResult recent_checks = 5;
}

message HealthCheckResult {
  google.protobuf.Timestamp timestamp = 1;
  bool success = 2;
  double response_time_ms = 3;
  string error_message = 4;
  string check_type = 5; // "CONNECTIVITY", "SERVICE", "BUSINESS"
}

message MaintenanceInfo {
  bool is_in_maintenance = 1;
  google.protobuf.Timestamp maintenance_start = 2;
  google.protobuf.Timestamp maintenance_end = 3;
  string maintenance_reason = 4;
  string maintenance_id = 5;
}
```

### 2. Java gRPC Client Implementation

```java
@Service
@Slf4j
public class AvailabilityServiceClient {

    private final AvailabilityServiceGrpc.AvailabilityServiceBlockingStub blockingStub;
    private final AvailabilityServiceGrpc.AvailabilityServiceStub asyncStub;
    private final MeterRegistry meterRegistry;
    
    @Value("${availability.service.timeout:100ms}")
    private Duration timeout;

    public Mono<ParticipantStatus> getParticipantStatus(String participantId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        return Mono.fromCallable(() -> {
            GetParticipantStatusRequest request = GetParticipantStatusRequest.newBuilder()
                .setParticipantId(participantId)
                .setIncludeHealthMetrics(false) // Fast call
                .build();
            
            try {
                GetParticipantStatusResponse response = blockingStub
                    .withDeadlineAfter(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .getParticipantStatus(request);
                
                sample.stop(Timer.builder("grpc.availability.call.duration")
                    .tag("method", "getParticipantStatus")
                    .tag("status", "success")
                    .register(meterRegistry));
                
                return response.getStatus();
                
            } catch (StatusRuntimeException e) {
                sample.stop(Timer.builder("grpc.availability.call.duration")
                    .tag("method", "getParticipantStatus")
                    .tag("status", "error")
                    .tag("error.code", e.getStatus().getCode().name())
                    .register(meterRegistry));
                
                log.warn("gRPC call failed for participant {}: {}", participantId, e.getMessage());
                
                // Fallback based on error type
                return switch (e.getStatus().getCode()) {
                    case UNAVAILABLE, DEADLINE_EXCEEDED -> ParticipantStatus.UNKNOWN;
                    case NOT_FOUND -> ParticipantStatus.UNAVAILABLE;
                    default -> throw new AvailabilityServiceException("gRPC call failed", e);
                };
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(timeout.plusMillis(50)) // Buffer for network overhead
        .onErrorReturn(ParticipantStatus.UNKNOWN);
    }

    public Flux<StatusChangeEvent> subscribeToStatusChanges(List<String> participantIds) {
        return Flux.create(sink -> {
            SubscribeStatusChangesRequest request = SubscribeStatusChangesRequest.newBuilder()
                .addAllParticipantIds(participantIds)
                .setSubscriberId("fast-router-service-" + UUID.randomUUID().toString())
                .build();
            
            StreamObserver<StatusChangeEvent> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(StatusChangeEvent event) {
                    sink.next(event);
                }
                
                @Override
                public void onError(Throwable t) {
                    log.error("Status change subscription failed", t);
                    sink.error(t);
                }
                
                @Override
                public void onCompleted() {
                    sink.complete();
                }
            };
            
            asyncStub.subscribeStatusChanges(request, responseObserver);
        });
    }
}
```

### 3. Sample gRPC Calls & Responses

**Router Service Status Check:**
```java
// Router Service calling Availability Service
GetParticipantStatusRequest request = GetParticipantStatusRequest.newBuilder()
    .setParticipantId("OCBCSGSG")
    .setIncludeHealthMetrics(false)
    .setIncludeMaintenanceInfo(false)
    .build();

// Response
GetParticipantStatusResponse response = GetParticipantStatusResponse.newBuilder()
    .setParticipantId("OCBCSGSG")
    .setStatus(ParticipantStatus.AVAILABLE)
    .setLastUpdated(Timestamp.newBuilder()
        .setSeconds(1733067600)
        .setNanos(500000000)
        .build())
    .setCacheTimestamp(Timestamp.newBuilder()
        .setSeconds(1733067700)
        .setNanos(125000000)
        .build())
    .setIsDegradedResponse(false)
    .build();
```

**Batch Status Check for Outward Processor:**
```java
GetMultipleParticipantStatusRequest request = GetMultipleParticipantStatusRequest.newBuilder()
    .addParticipantIds("OCBCSGSG")
    .addParticipantIds("DBSSSGSG")
    .addParticipantIds("UOBSSGSG")
    .setIncludeHealthMetrics(true)
    .build();

// Response
GetMultipleParticipantStatusResponse response = GetMultipleParticipantStatusResponse.newBuilder()
    .addParticipants(ParticipantStatusInfo.newBuilder()
        .setParticipantId("OCBCSGSG")
        .setStatus(ParticipantStatus.AVAILABLE)
        .setHealthMetrics(HealthMetrics.newBuilder()
            .setResponseTimeMs(45.5)
            .setAvailabilityPercentage(99.95)
            .setConsecutiveFailures(0)
            .build())
        .build())
    .addParticipants(ParticipantStatusInfo.newBuilder()
        .setParticipantId("DBSSSGSG")
        .setStatus(ParticipantStatus.DEGRADED)
        .setHealthMetrics(HealthMetrics.newBuilder()
            .setResponseTimeMs(1250.0)
            .setAvailabilityPercentage(95.2)
            .setConsecutiveFailures(2)
            .build())
        .build())
    .setTotalParticipants(3)
    .setAvailableParticipants(2)
    .setDegradedParticipants(1)
    .setUnavailableParticipants(0)
    .build();
```

## Performance Monitoring & gRPC Metrics

```mermaid
graph TB
    subgraph "gRPC Performance Metrics"
        CallLatency[gRPC Call Latency<br/>P50: <50ms<br/>P95: <100ms<br/>P99: <200ms]
        CallVolume[Call Volume<br/>Target: 1000 RPS<br/>Peak: 2000 RPS<br/>Concurrent: 500]
        ErrorRates[gRPC Error Rates<br/>UNAVAILABLE: <1%<br/>DEADLINE_EXCEEDED: <0.5%<br/>INTERNAL: <0.1%]
    end
    
    subgraph "Health Check Performance"
        BankResponse[Bank Response Times<br/>Target: <3s<br/>Timeout: 10s<br/>Retry: 3 attempts]
        CheckFrequency[Health Check Frequency<br/>Normal: 30s<br/>Degraded: 10s<br/>Failed: 60s]
        StatusPropagation[Status Propagation<br/>Cache Update: <1s<br/>gRPC Notification: <2s<br/>Event Store: <5s]
    end
    
    subgraph "Circuit Breaker States"
        Healthy[HEALTHY<br/>Success Rate >95%<br/>Response Time <3s]
        Degraded[DEGRADED<br/>Success Rate 80-95%<br/>Increased Monitoring]
        Failed[FAILED<br/>Success Rate <80%<br/>Mark Unavailable]
    end
    
    CallLatency --> CheckFrequency
    BankResponse --> StatusPropagation
    
    Healthy -->|Degraded Performance| Degraded
    Degraded -->|Continued Failure| Failed
    Failed -->|Recovery Detected| Healthy
```

## Technology Stack & gRPC Configuration

```yaml
gRPC Server Configuration:
  grpc:
    server:
      port: 9090
      max-inbound-message-size: 4MB
      max-inbound-metadata-size: 8KB
      keep-alive-time: 30s
      keep-alive-timeout: 5s
      keep-alive-without-calls: true
      max-connection-idle: 60s
      max-connection-age: 120s
      max-connection-age-grace: 30s
      permit-keep-alive-time: 10s
      permit-keep-alive-without-calls: true

Health Monitoring:
  availability:
    health-check:
      interval: 30s
      timeout: 10s
      retry-attempts: 3
      retry-delay: 5s
    banks:
      - participant-id: OCBCSGSG
        health-url: https://api.ocbc.com.sg/health
        service-url: https://api.ocbc.com.sg/payments/status
        network-test: true
      - participant-id: DBSSSGSG
        health-url: https://api.dbs.com.sg/health
        service-url: https://api.dbs.com.sg/payments/status
        network-test: true

Circuit Breaker:
  resilience4j:
    circuitbreaker:
      instances:
        bank-health-check:
          failure-rate-threshold: 20
          slow-call-rate-threshold: 50
          slow-call-duration-threshold: 5s
          wait-duration-in-open-state: 60s
          sliding-window-size: 20
          minimum-number-of-calls: 10

JVM Configuration:
  memory: "-Xms2g -Xmx4g"
  gc: "-XX:+UseZGC"
  virtual-threads: "--enable-preview -XX:+UseVirtualThreads"
  grpc-netty: "-Dio.netty.allocator.type=pooled"
```