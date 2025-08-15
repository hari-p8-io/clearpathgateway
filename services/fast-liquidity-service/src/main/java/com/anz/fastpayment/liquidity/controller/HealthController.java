package com.anz.fastpayment.liquidity.controller;

import com.anz.fastpayment.liquidity.model.HealthResponse;
import com.anz.fastpayment.liquidity.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health Check Controller
 * 
 * Provides health status endpoint for monitoring service availability 
 * and dependencies including database, message queues, and external services.
 */
@RestController
@Tag(name = "Health", description = "Service health and monitoring endpoints")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    private final HealthService healthService;

    @Autowired
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @Operation(
        summary = "Service health check",
        description = """
            Health check endpoint for monitoring service availability and dependencies.
            Returns status of database connections, message queues, and overall service health.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Service is healthy",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HealthResponse.class),
                examples = {
                    @ExampleObject(
                        name = "healthy_service",
                        summary = "All systems operational",
                        value = """
                            {
                              "status": "UP",
                              "timestamp": "2025-01-15T10:30:00.123Z",
                              "services": {
                                "database": "UP",
                                "messageQueue": "UP",
                                "redis": "UP",
                                "spanner": "UP"
                              },
                              "version": "21.0.0-apeafast-SNAPSHOT",
                              "uptime": "72h 35m 42s"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "503", 
            description = "Service is unhealthy",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = HealthResponse.class),
                examples = {
                    @ExampleObject(
                        name = "degraded_service",
                        summary = "Database connection issues",
                        value = """
                            {
                              "status": "DEGRADED",
                              "timestamp": "2025-01-15T10:30:00.123Z",
                              "services": {
                                "database": "DOWN",
                                "messageQueue": "UP",
                                "redis": "UP",
                                "spanner": "DOWN"
                              },
                              "version": "21.0.0-apeafast-SNAPSHOT",
                              "uptime": "72h 35m 42s",
                              "errors": [
                                "Database connection timeout",
                                "Spanner instance unavailable"
                              ]
                            }
                            """
                    )
                }
            )
        )
    })
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthResponse> getHealth() {
        logger.debug("Health check requested");
        
        HealthResponse healthResponse = healthService.checkHealth();
        
        logger.debug("Health check completed - status: {}", healthResponse.getStatus());
        
        // Return 503 if service is DOWN or DEGRADED
        if ("DOWN".equals(healthResponse.getStatus()) || "DEGRADED".equals(healthResponse.getStatus())) {
            return ResponseEntity.status(503).body(healthResponse);
        }
        
        return ResponseEntity.ok(healthResponse);
    }
}