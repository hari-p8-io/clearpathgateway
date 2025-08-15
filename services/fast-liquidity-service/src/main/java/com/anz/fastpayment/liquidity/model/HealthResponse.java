package com.anz.fastpayment.liquidity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Health Response
 * 
 * Response object for health check operations
 */
@Schema(description = "Health status response")
public class HealthResponse {

    @Schema(description = "Overall service health status", 
            example = "UP", allowableValues = {"UP", "DOWN", "DEGRADED"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Health check timestamp", example = "2025-01-15T10:30:00.123Z")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(description = "Status of individual service dependencies")
    @JsonProperty("services")
    private Map<String, String> services;

    @Schema(description = "Service version", example = "21.0.0-apeafast-SNAPSHOT")
    @JsonProperty("version")
    private String version;

    @Schema(description = "Service uptime", example = "72h 35m 42s")
    @JsonProperty("uptime")
    private String uptime;

    @Schema(description = "Error messages if any services are down")
    @JsonProperty("errors")
    private List<String> errors;

    // Constructors
    public HealthResponse() {
        this.timestamp = Instant.now();
    }

    public HealthResponse(String status) {
        this();
        this.status = status;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getServices() {
        return services;
    }

    public void setServices(Map<String, String> services) {
        this.services = services;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    /**
     * Health status constants
     */
    public static final class Status {
        public static final String UP = "UP";
        public static final String DOWN = "DOWN";
        public static final String DEGRADED = "DEGRADED";
    }

    /**
     * Service dependency constants
     */
    public static final class ServiceNames {
        public static final String DATABASE = "database";
        public static final String MESSAGE_QUEUE = "messageQueue";
        public static final String REDIS = "redis";
        public static final String SPANNER = "spanner";
    }
}