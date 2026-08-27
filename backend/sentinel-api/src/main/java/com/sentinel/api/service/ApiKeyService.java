package com.sentinel.api.service;

import com.sentinel.api.dto.ApiKeyResponse;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.UpdateApiKeyRequest;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.AuditAction;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    private static final String KEY_PREFIX = "sk_sentinel_";
    private static final int RANDOM_BYTES_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;
    private final AuditLogService auditLogService;

    public ApiKeyService(
        ApiKeyRepository apiKeyRepository,
        ApplicationRepository applicationRepository,
        AuditLogService auditLogService
    ) {
        this.apiKeyRepository = apiKeyRepository;
        this.applicationRepository = applicationRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ApiKeyResponse createApiKey(CreateApiKeyRequest request) {
        String rawKey = generateRawApiKey();
        String keyHash = hashKey(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setName(request.getName());
        apiKey.setKeyHash(keyHash);
        apiKey.setActive(true);
        apiKey.setRateLimitPerMinute(request.getRateLimitPerMinute());
        apiKey.setExpiresAt(request.getExpiresAt());

        ApiKey saved = apiKeyRepository.save(apiKey);

        return new ApiKeyResponse(
            saved.getId(),
            saved.getName(),
            rawKey,
            maskKey(saved.getKeyHash()),
            saved.getRateLimitPerMinute(),
            saved.isActive(),
            saved.getCreatedAt(),
            saved.getExpiresAt(),
            saved.getRevokedAt(),
            "Store this API key securely. It will not be shown again."
        );
    }

    @Transactional
    public ApiKeyResponse createApplicationApiKey(Long ownerId, Long applicationId, CreateApiKeyRequest request) {
        // Validate application ownership
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        String rawKey = generateRawApiKey();
        String keyHash = hashKey(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setApplicationId(applicationId);
        apiKey.setName(request.getName());
        apiKey.setKeyHash(keyHash);
        apiKey.setActive(true);
        apiKey.setRateLimitPerMinute(request.getRateLimitPerMinute());
        apiKey.setExpiresAt(request.getExpiresAt());

        ApiKey saved = apiKeyRepository.save(apiKey);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.API_KEY_CREATED,
            "API_KEY",
            String.valueOf(saved.getId()),
            "Created key '" + saved.getName() + "' with limit " + saved.getRateLimitPerMinute() + " req/min",
            null
        );

        return new ApiKeyResponse(
            saved.getId(),
            saved.getName(),
            rawKey,
            maskKey(saved.getKeyHash()),
            saved.getRateLimitPerMinute(),
            saved.isActive(),
            saved.getCreatedAt(),
            saved.getExpiresAt(),
            saved.getRevokedAt(),
            "Store this API key securely. It will not be shown again."
        );
    }

    public List<ApiKeyResponse> listApplicationApiKeys(Long ownerId, Long applicationId) {
        // Validate application ownership
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        return apiKeyRepository.findByApplicationId(applicationId).stream()
            .map(key -> new ApiKeyResponse(
                key.getId(),
                key.getName(),
                null, // Raw key is only shown once at creation/regeneration time
                maskKey(key.getKeyHash()),
                key.getRateLimitPerMinute(),
                key.isActive(),
                key.getCreatedAt(),
                key.getExpiresAt(),
                key.getRevokedAt(),
                null
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public ApiKeyResponse updateApplicationApiKey(Long ownerId, Long applicationId, Long keyId, UpdateApiKeyRequest request) {
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApiKey key = apiKeyRepository.findByIdAndApplicationId(keyId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("API Key not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            key.setName(request.getName().trim());
        }
        if (request.getRateLimitPerMinute() != null && request.getRateLimitPerMinute() > 0) {
            key.setRateLimitPerMinute(request.getRateLimitPerMinute());
        }
        if (request.getActive() != null) {
            key.setActive(request.getActive());
            if (!request.getActive() && key.getRevokedAt() == null) {
                key.setRevokedAt(Instant.now());
            } else if (request.getActive()) {
                key.setRevokedAt(null);
            }
        }

        ApiKey updated = apiKeyRepository.save(key);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.API_KEY_UPDATED,
            "API_KEY",
            String.valueOf(updated.getId()),
            "Updated key '" + updated.getName() + "', active=" + updated.isActive() + ", limit=" + updated.getRateLimitPerMinute(),
            null
        );

        return new ApiKeyResponse(
            updated.getId(),
            updated.getName(),
            null,
            maskKey(updated.getKeyHash()),
            updated.getRateLimitPerMinute(),
            updated.isActive(),
            updated.getCreatedAt(),
            updated.getExpiresAt(),
            updated.getRevokedAt(),
            null
        );
    }

    @Transactional
    public ApiKeyResponse revokeApplicationApiKey(Long ownerId, Long applicationId, Long keyId) {
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApiKey key = apiKeyRepository.findByIdAndApplicationId(keyId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("API Key not found"));

        key.setActive(false);
        key.setRevokedAt(Instant.now());
        ApiKey updated = apiKeyRepository.save(key);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.API_KEY_REVOKED,
            "API_KEY",
            String.valueOf(updated.getId()),
            "Revoked key '" + updated.getName() + "'",
            null
        );

        return new ApiKeyResponse(
            updated.getId(),
            updated.getName(),
            null,
            maskKey(updated.getKeyHash()),
            updated.getRateLimitPerMinute(),
            updated.isActive(),
            updated.getCreatedAt(),
            updated.getExpiresAt(),
            updated.getRevokedAt(),
            "API key has been revoked and can no longer authenticate."
        );
    }

    @Transactional
    public ApiKeyResponse regenerateApplicationApiKey(Long ownerId, Long applicationId, Long keyId) {
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApiKey key = apiKeyRepository.findByIdAndApplicationId(keyId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("API Key not found"));

        String newRawKey = generateRawApiKey();
        String newKeyHash = hashKey(newRawKey);

        key.setKeyHash(newKeyHash);
        key.setActive(true);
        key.setRevokedAt(null);
        ApiKey updated = apiKeyRepository.save(key);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.API_KEY_REGENERATED,
            "API_KEY",
            String.valueOf(updated.getId()),
            "Regenerated secret for key '" + updated.getName() + "'",
            null
        );

        return new ApiKeyResponse(
            updated.getId(),
            updated.getName(),
            newRawKey,
            maskKey(updated.getKeyHash()),
            updated.getRateLimitPerMinute(),
            updated.isActive(),
            updated.getCreatedAt(),
            updated.getExpiresAt(),
            updated.getRevokedAt(),
            "Store this regenerated API key securely. It will not be shown again."
        );
    }

    @Transactional
    public void deleteApplicationApiKey(Long ownerId, Long applicationId, Long keyId) {
        applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApiKey key = apiKeyRepository.findByIdAndApplicationId(keyId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("API Key not found"));

        apiKeyRepository.delete(key);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.API_KEY_DELETED,
            "API_KEY",
            String.valueOf(keyId),
            "Deleted key '" + key.getName() + "'",
            null
        );
    }

    public Optional<ApiKey> validateKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }

        String keyHash = hashKey(rawKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

        if (apiKeyOpt.isEmpty()) {
            return Optional.empty();
        }

        ApiKey apiKey = apiKeyOpt.get();
        if (!apiKey.isActive()) {
            return Optional.empty();
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        return Optional.of(apiKey);
    }

    public String generateRawApiKey() {
        byte[] randomBytes = new byte[RANDOM_BYTES_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return KEY_PREFIX + HexFormat.of().formatHex(randomBytes);
    }

    public String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String maskKey(String keyHash) {
        if (keyHash == null || keyHash.length() < 8) {
            return "sk_sentinel_••••••••";
        }
        return "sk_sentinel_••••" + keyHash.substring(keyHash.length() - 4);
    }
}
