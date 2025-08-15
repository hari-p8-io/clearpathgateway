package com.anz.fastpayment.liquidity.service;

import com.anz.fastpayment.liquidity.model.*;

/**
 * Liquidity Service Interface
 * 
 * Core business logic interface for liquidity management operations
 */
public interface LiquidityService {

    /**
     * Check if a proposed transaction would be authorized based on current liquidity
     * 
     * @param request Balance check request containing transaction details
     * @return Balance check response with authorization decision
     */
    BalanceCheckResponse checkBalance(BalanceCheckRequest request);

    /**
     * Update participant balance based on payment transaction
     * 
     * @param request Balance update request containing transaction details
     * @return Balance update response with transaction results
     */
    BalanceUpdateResponse updateBalance(BalanceUpdateRequest request);

    /**
     * Get current balance information for a participant
     * 
     * @param participantId Participant identifier
     * @param currency Currency code
     * @param countryCode Country code to determine payment scheme
     * @return Current balance information
     */
    ParticipantBalanceResponse getParticipantBalance(String participantId, String currency, String countryCode);
}