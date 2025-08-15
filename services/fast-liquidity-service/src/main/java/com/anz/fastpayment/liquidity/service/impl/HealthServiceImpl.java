package com.anz.fastpayment.liquidity.service.impl;

import com.anz.fastpayment.liquidity.model.HealthResponse;
import com.anz.fastpayment.liquidity.service.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health Service Implementation
 * 
 * Provides health check functionality for the liquidity service
 * 
 * TODO: Implement actual health checks for Spanner, Kafka, Redis
 * This is a basic implementation for initial setup
 */
@Service
public class HealthServiceImpl implements HealthService {

    private static final Logger logger = LoggerFactory.getLogger(HealthServiceImpl.class);

    @Value("${spring.application.name:fast-liquidity-service}")
    private String applicationName;

    @Value("${server.port:8084}")
    private String serverPort;

    private final Instant startTime = Instant.now();

    @Override
    public HealthResponse checkHealth() {
        logger.debug("Performing health check");

        HealthResponse response = new HealthResponse();

        try {
            // Check individual services
            Map<String, String> serviceStatuses = checkServiceHealth();
            response.setServices(serviceStatuses);

            // Determine overall status
            String overallStatus = determineOverallStatus(serviceStatuses);
            response.setStatus(overallStatus);

            // Set metadata
            response.setVersion(getApplicationVersion());
            response.setUptime(calculateUptime());

            // Add errors if any services are down
            if (HealthResponse.Status.DEGRADED.equals(overallStatus) || 
                HealthResponse.Status.DOWN.equals(overallStatus)) {
                response.setErrors(getServiceErrors(serviceStatuses));
            }

            logger.debug("Health check completed - status: {}", overallStatus);

        } catch (Exception e) {
            logger.error("Error during health check", e);
            response.setStatus(HealthResponse.Status.DOWN);
            response.setErrors(List.of("Health check failed: " + e.getMessage()));
        }

        return response;
    }

    private Map<String, String> checkServiceHealth() {
        Map<String, String> serviceStatuses = new HashMap<>();

        // TODO: Implement actual health checks
        // For now, returning mock statuses
        serviceStatuses.put(HealthResponse.ServiceNames.DATABASE, checkDatabaseHealth());
        serviceStatuses.put(HealthResponse.ServiceNames.MESSAGE_QUEUE, checkKafkaHealth());
        serviceStatuses.put(HealthResponse.ServiceNames.REDIS, checkRedisHealth());
        serviceStatuses.put(HealthResponse.ServiceNames.SPANNER, checkSpannerHealth());

        return serviceStatuses;
    }

    private String checkDatabaseHealth() {
        // TODO: Implement actual database health check
        return HealthResponse.Status.UP;
    }

    private String checkKafkaHealth() {
        // TODO: Implement actual Kafka health check
        return HealthResponse.Status.UP;
    }

    private String checkRedisHealth() {
        // TODO: Implement actual Redis health check
        return HealthResponse.Status.UP;
    }

    private String checkSpannerHealth() {
        // TODO: Implement actual Spanner health check
        return HealthResponse.Status.UP;
    }

    private String determineOverallStatus(Map<String, String> serviceStatuses) {
        long downServices = serviceStatuses.values().stream()
                .filter(HealthResponse.Status.DOWN::equals)
                .count();

        if (downServices == 0) {
            return HealthResponse.Status.UP;
        } else if (downServices < serviceStatuses.size()) {
            return HealthResponse.Status.DEGRADED;
        } else {
            return HealthResponse.Status.DOWN;
        }
    }

    private List<String> getServiceErrors(Map<String, String> serviceStatuses) {
        return serviceStatuses.entrySet().stream()
                .filter(entry -> HealthResponse.Status.DOWN.equals(entry.getValue()))
                .map(entry -> entry.getKey() + " service is down")
                .toList();
    }

    private String getApplicationVersion() {
        // Try to get version from manifest or return default
        Package pkg = getClass().getPackage();
        String version = pkg != null ? pkg.getImplementationVersion() : null;
        return version != null ? version : "21.0.0-apeafast-SNAPSHOT";
    }

    private String calculateUptime() {
        Duration uptime = Duration.between(startTime, Instant.now());
        long hours = uptime.toHours();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}