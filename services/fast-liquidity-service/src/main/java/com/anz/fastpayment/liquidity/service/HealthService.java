package com.anz.fastpayment.liquidity.service;

import com.anz.fastpayment.liquidity.model.HealthResponse;

/**
 * Health Service Interface
 * 
 * Service for checking health status of the application and its dependencies
 */
public interface HealthService {

    /**
     * Check the health status of the service and its dependencies
     * 
     * @return Health response with overall status and dependency details
     */
    HealthResponse checkHealth();
}