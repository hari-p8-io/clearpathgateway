package com.anz.fastpayment.liquidity.controller;

import com.anz.fastpayment.liquidity.model.*;
import com.anz.fastpayment.liquidity.service.LiquidityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Liquidity Management Controller
 * 
 * Provides REST endpoints for liquidity balance management including:
 * - Balance checking and authorization
 * - Balance updates from payment transactions
 * - Participant balance retrieval
 * 
 * Supports Singapore G3 and Hong Kong FPS payment schemes.
 */
@RestController
@RequestMapping("/liquidity")
@Validated
@Tag(name = "Liquidity Management", description = "Core liquidity management operations for balance checking and updates")
public class LiquidityController {

    private static final Logger logger = LoggerFactory.getLogger(LiquidityController.class);

    private final LiquidityService liquidityService;

    @Autowired
    public LiquidityController(LiquidityService liquidityService) {
        this.liquidityService = liquidityService;
    }

    @Operation(
        summary = "Check liquidity balance and authorization",
        description = """
            Check current liquidity balance and determine if a proposed transaction amount 
            would be authorized or rejected. Returns current balance and authorization status
            for the requested amount without actually updating the balance.
            
            Country code automatically determines the payment scheme:
            - SG: Singapore G3 (MAS regulated)
            - HK: Hong Kong FPS
            
            This endpoint supports both debit and credit authorization checks with
            real-time net debit cap compliance verification.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Balance check completed successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = BalanceCheckResponse.class),
                examples = {
                    @ExampleObject(
                        name = "authorized_transaction",
                        summary = "Transaction authorized",
                        value = """
                            {
                              "status": "SUCCESS",
                              "authorized": true,
                              "currentBalance": "1500000.00",
                              "projectedBalance": "1450000.00",
                              "currency": "SGD",
                              "minimumBalance": "0.00",
                              "availableAmount": "1500000.00",
                              "timestamp": "2025-01-15T10:30:00.123Z",
                              "requestId": "CHK-20250115-103000-001",
                              "warnings": []
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Currency or country not supported",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping(value = "/balance/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BalanceCheckResponse> checkBalance(
        @Valid @RequestBody BalanceCheckRequest request) {
        
        logger.info("Received balance check request for country: {}, currency: {}, amount: {}", 
                   request.getCountryCode(), request.getCurrency(), request.getAmount());
        
        BalanceCheckResponse response = liquidityService.checkBalance(request);
        
        logger.info("Balance check completed - authorized: {}, requestId: {}", 
                   response.isAuthorized(), response.getRequestId());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Update liquidity balances",
        description = """
            Process payment messages (PACS.008, PACS.002, CAMT.054, etc.) and update 
            participant balances accordingly. Country code automatically determines 
            the payment scheme (SG=G3, HK=FPS). 
            
            Supports various transaction types:
            - DEBIT: Outbound payments (reduce balance)
            - CREDIT: Inbound payments (increase balance)
            - RESERVE: Hold funds for pending transactions
            - RELEASE: Release previously reserved funds
            
            All updates are atomic and include comprehensive audit trails.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Balance updated successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = BalanceUpdateResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid transaction data or insufficient funds",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Duplicate transaction or balance conflict",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping(value = "/balance/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BalanceUpdateResponse> updateBalance(
        @Valid @RequestBody BalanceUpdateRequest request) {
        
        logger.info("Received balance update request for participant: {}, messageId: {}, amount: {}", 
                   request.getParticipantId(), request.getMessageId(), request.getAmount());
        
        BalanceUpdateResponse response = liquidityService.updateBalance(request);
        
        logger.info("Balance update completed - status: {}, transactionId: {}", 
                   response.getStatus(), response.getTransactionId());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get current balance for participant",
        description = """
            Retrieve the current liquidity balance for a specific participant.
            Includes available balance, reserved amounts, and balance history.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Current balance information",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ParticipantBalanceResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Participant not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping(value = "/balance/{participantId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParticipantBalanceResponse> getParticipantBalance(
        @Parameter(description = "Participant identifier (e.g., ANZBSGSG, DBSSSGSG)", example = "ANZBSGSG")
        @PathVariable 
        @Pattern(regexp = "^[A-Z]{8}$", message = "Participant ID must be 8 uppercase letters")
        String participantId,
        
        @Parameter(description = "Currency code", example = "SGD")
        @RequestParam 
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
        String currency,
        
        @Parameter(description = "Country code to determine payment scheme", example = "SG")
        @RequestParam 
        @Pattern(regexp = "^(SG|HK)$", message = "Country code must be SG or HK")
        String countryCode) {
        
        logger.info("Received participant balance request for: {}, currency: {}, country: {}", 
                   participantId, currency, countryCode);
        
        ParticipantBalanceResponse response = liquidityService.getParticipantBalance(
            participantId, currency, countryCode);
        
        logger.info("Retrieved balance for participant: {}, current balance: {}", 
                   participantId, response.getCurrentBalance());
        
        return ResponseEntity.ok(response);
    }
}