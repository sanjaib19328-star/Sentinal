package com.sentinel.api.service;

import com.sentinel.api.dto.ApiPolicyDto;
import com.sentinel.api.dto.SavePolicyRequest;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.ApiPolicy;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.AuditAction;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApiPolicyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ApiPolicyService {

    private final ApiPolicyRepository apiPolicyRepository;
    private final ApplicationRepository applicationRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final AuditLogService auditLogService;

    public ApiPolicyService(
        ApiPolicyRepository apiPolicyRepository,
        ApplicationRepository applicationRepository,
        ApiEndpointRepository apiEndpointRepository,
        AuditLogService auditLogService
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.applicationRepository = applicationRepository;
        this.apiEndpointRepository = apiEndpointRepository;
        this.auditLogService = auditLogService;
    }

    public ApiPolicyDto getApplicationPolicy(Long ownerId, Long applicationId) {
        verifyApplicationOwnership(ownerId, applicationId);

        return apiPolicyRepository.findByApplicationIdAndApiEndpointIdIsNull(applicationId)
            .map(this::mapToDto)
            .orElseGet(() -> createDefaultPolicyDto(applicationId, null));
    }

    @Transactional
    public ApiPolicyDto saveApplicationPolicy(Long ownerId, Long applicationId, SavePolicyRequest request) {
        verifyApplicationOwnership(ownerId, applicationId);

        ApiPolicy policy = apiPolicyRepository.findByApplicationIdAndApiEndpointIdIsNull(applicationId)
            .orElseGet(() -> {
                ApiPolicy p = new ApiPolicy();
                p.setApplicationId(applicationId);
                p.setApiEndpointId(null);
                return p;
            });

        boolean isNew = policy.getId() == null;
        applyRequest(policy, request);
        ApiPolicy saved = apiPolicyRepository.save(policy);

        auditLogService.record(
            ownerId,
            applicationId,
            isNew ? AuditAction.POLICY_CREATED : AuditAction.POLICY_UPDATED,
            "APPLICATION_POLICY",
            String.valueOf(saved.getId()),
            "Rate limit: " + saved.getRateLimit() + "/" + saved.getRateWindowSeconds() + "s, Quota: " + saved.getQuotaLimit() + ", CB: " + saved.isCircuitBreakerEnabled(),
            null
        );

        return mapToDto(saved);
    }

    @Transactional
    public void deleteApplicationPolicy(Long ownerId, Long applicationId) {
        verifyApplicationOwnership(ownerId, applicationId);

        apiPolicyRepository.findByApplicationIdAndApiEndpointIdIsNull(applicationId)
            .ifPresent(policy -> {
                apiPolicyRepository.delete(policy);
                auditLogService.record(
                    ownerId,
                    applicationId,
                    AuditAction.POLICY_DELETED,
                    "APPLICATION_POLICY",
                    String.valueOf(policy.getId()),
                    "Application policy deleted",
                    null
                );
            });
    }

    public ApiPolicyDto getEndpointPolicy(Long ownerId, Long applicationId, Long apiEndpointId) {
        verifyApplicationOwnership(ownerId, applicationId);
        verifyEndpointBelongsToApp(applicationId, apiEndpointId);

        return apiPolicyRepository.findByApplicationIdAndApiEndpointId(applicationId, apiEndpointId)
            .map(this::mapToDto)
            .orElseGet(() -> createDefaultPolicyDto(applicationId, apiEndpointId));
    }

    @Transactional
    public ApiPolicyDto saveEndpointPolicy(Long ownerId, Long applicationId, Long apiEndpointId, SavePolicyRequest request) {
        verifyApplicationOwnership(ownerId, applicationId);
        verifyEndpointBelongsToApp(applicationId, apiEndpointId);

        ApiPolicy policy = apiPolicyRepository.findByApplicationIdAndApiEndpointId(applicationId, apiEndpointId)
            .orElseGet(() -> {
                ApiPolicy p = new ApiPolicy();
                p.setApplicationId(applicationId);
                p.setApiEndpointId(apiEndpointId);
                return p;
            });

        boolean isNew = policy.getId() == null;
        applyRequest(policy, request);
        ApiPolicy saved = apiPolicyRepository.save(policy);

        auditLogService.record(
            ownerId,
            applicationId,
            isNew ? AuditAction.POLICY_CREATED : AuditAction.POLICY_UPDATED,
            "ENDPOINT_POLICY",
            String.valueOf(saved.getId()),
            "Endpoint " + apiEndpointId + " rate limit: " + saved.getRateLimit() + "/" + saved.getRateWindowSeconds() + "s",
            null
        );

        return mapToDto(saved);
    }

    @Transactional
    public void deleteEndpointPolicy(Long ownerId, Long applicationId, Long apiEndpointId) {
        verifyApplicationOwnership(ownerId, applicationId);
        verifyEndpointBelongsToApp(applicationId, apiEndpointId);

        apiPolicyRepository.findByApplicationIdAndApiEndpointId(applicationId, apiEndpointId)
            .ifPresent(policy -> {
                apiPolicyRepository.delete(policy);
                auditLogService.record(
                    ownerId,
                    applicationId,
                    AuditAction.POLICY_DELETED,
                    "ENDPOINT_POLICY",
                    String.valueOf(policy.getId()),
                    "Endpoint policy deleted for endpoint " + apiEndpointId,
                    null
                );
            });
    }

    // Direct lookup methods for gateway fast-path
    public Optional<ApiPolicy> findAppPolicy(Long applicationId) {
        if (applicationId == null) return Optional.empty();
        return apiPolicyRepository.findByApplicationIdAndApiEndpointIdIsNull(applicationId);
    }

    public Optional<ApiPolicy> findEndpointPolicy(Long applicationId, Long endpointId) {
        if (applicationId == null || endpointId == null) return Optional.empty();
        return apiPolicyRepository.findByApplicationIdAndApiEndpointId(applicationId, endpointId);
    }

    private void applyRequest(ApiPolicy policy, SavePolicyRequest req) {
        if (req.getEnabled() != null) policy.setEnabled(req.getEnabled());
        if (req.getRateLimit() != null) policy.setRateLimit(req.getRateLimit());
        if (req.getRateWindowSeconds() != null) policy.setRateWindowSeconds(req.getRateWindowSeconds());
        if (req.getQuotaLimit() != null) policy.setQuotaLimit(req.getQuotaLimit());
        if (req.getQuotaWindowSeconds() != null) policy.setQuotaWindowSeconds(req.getQuotaWindowSeconds());
        if (req.getTimeoutMs() != null) policy.setTimeoutMs(req.getTimeoutMs());
        if (req.getMaxRequestBodyBytes() != null) policy.setMaxRequestBodyBytes(req.getMaxRequestBodyBytes());
        if (req.getMaxResponseBodyBytes() != null) policy.setMaxResponseBodyBytes(req.getMaxResponseBodyBytes());
        if (req.getAllowedMethods() != null) policy.setAllowedMethods(req.getAllowedMethods());
        if (req.getRetryCount() != null) policy.setRetryCount(req.getRetryCount());
        if (req.getRetryDelayMs() != null) policy.setRetryDelayMs(req.getRetryDelayMs());
        if (req.getRetryNonIdempotent() != null) policy.setRetryNonIdempotent(req.getRetryNonIdempotent());
        if (req.getCircuitBreakerEnabled() != null) policy.setCircuitBreakerEnabled(req.getCircuitBreakerEnabled());
        if (req.getCircuitFailureThreshold() != null) policy.setCircuitFailureThreshold(req.getCircuitFailureThreshold());
        if (req.getCircuitRecoveryTimeoutSeconds() != null) policy.setCircuitRecoveryTimeoutSeconds(req.getCircuitRecoveryTimeoutSeconds());
    }

    private Application verifyApplicationOwnership(Long ownerId, Long applicationId) {
        return applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }

    private ApiEndpoint verifyEndpointBelongsToApp(Long applicationId, Long endpointId) {
        return apiEndpointRepository.findById(endpointId)
            .filter(ep -> ep.getApplicationId().equals(applicationId))
            .orElseThrow(() -> new ResourceNotFoundException("API endpoint not found"));
    }

    public ApiPolicyDto mapToDto(ApiPolicy p) {
        return new ApiPolicyDto(
            p.getId(),
            p.getApplicationId(),
            p.getApiEndpointId(),
            p.isEnabled(),
            p.getRateLimit(),
            p.getRateWindowSeconds(),
            p.getQuotaLimit(),
            p.getQuotaWindowSeconds(),
            p.getTimeoutMs(),
            p.getMaxRequestBodyBytes(),
            p.getMaxResponseBodyBytes(),
            p.getAllowedMethods(),
            p.getRetryCount(),
            p.getRetryDelayMs(),
            p.isRetryNonIdempotent(),
            p.isCircuitBreakerEnabled(),
            p.getCircuitFailureThreshold(),
            p.getCircuitRecoveryTimeoutSeconds(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }

    private ApiPolicyDto createDefaultPolicyDto(Long applicationId, Long endpointId) {
        ApiPolicyDto dto = new ApiPolicyDto();
        dto.setApplicationId(applicationId);
        dto.setApiEndpointId(endpointId);
        dto.setEnabled(true);
        dto.setRateLimit(60);
        dto.setRateWindowSeconds(60);
        dto.setTimeoutMs(5000);
        dto.setRetryCount(0);
        dto.setRetryDelayMs(100);
        dto.setRetryNonIdempotent(false);
        dto.setCircuitBreakerEnabled(true);
        dto.setCircuitFailureThreshold(5);
        dto.setCircuitRecoveryTimeoutSeconds(15);
        return dto;
    }
}
