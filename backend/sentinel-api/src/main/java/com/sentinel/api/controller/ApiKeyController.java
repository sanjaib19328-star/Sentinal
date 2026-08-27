package com.sentinel.api.controller;

import com.sentinel.api.dto.ApiKeyResponse;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development & Administration endpoint for generating Sentinel API keys.
 * Note: This endpoint is for Phase 2 development and testing. It must be secured
 * by appropriate administrative authentication before production deployment.
 */
@RestController
@RequestMapping("/api/v1/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyResponse response = apiKeyService.createApiKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
