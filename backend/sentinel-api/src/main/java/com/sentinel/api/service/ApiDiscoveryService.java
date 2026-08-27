package com.sentinel.api.service;

import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.DocumentationStatus;
import com.sentinel.api.repository.ApiEndpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class ApiDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ApiDiscoveryService.class);

    private final ApiEndpointRepository apiEndpointRepository;

    public ApiDiscoveryService(ApiEndpointRepository apiEndpointRepository) {
        this.apiEndpointRepository = apiEndpointRepository;
    }

    @Transactional
    public ApiEndpoint discoverOrUpdateEndpoint(Long applicationId, String method, String normalizedPath) {
        String upperMethod = method != null ? method.toUpperCase() : "GET";
        String path = normalizedPath != null ? normalizedPath : "/";

        // 1. Direct match on normalizedPath
        Optional<ApiEndpoint> existingOpt = apiEndpointRepository
            .findByApplicationIdAndMethodAndNormalizedPath(applicationId, upperMethod, path);

        if (existingOpt.isPresent()) {
            ApiEndpoint endpoint = existingOpt.get();
            endpoint.setLastSeenAt(Instant.now());
            if (endpoint.getDocumentationStatus() == DocumentationStatus.DOCUMENTED) {
                endpoint.setDocumentationStatus(DocumentationStatus.DOCUMENTED_AND_DISCOVERED);
                log.info("API endpoint for app {}: {} {} transitioned from DOCUMENTED to DOCUMENTED_AND_DISCOVERED", applicationId, upperMethod, path);
            }
            return apiEndpointRepository.save(endpoint);
        }

        // 2. Pattern match against existing documented endpoints with path variables (e.g. {image_id}, {id}, {param})
        java.util.List<ApiEndpoint> appEndpoints = apiEndpointRepository.findByApplicationId(applicationId);
        for (ApiEndpoint ep : appEndpoints) {
            if (ep.getMethod().equalsIgnoreCase(upperMethod) && matchesPathTemplate(ep.getNormalizedPath(), path)) {
                ep.setLastSeenAt(Instant.now());
                if (ep.getDocumentationStatus() == DocumentationStatus.DOCUMENTED) {
                    ep.setDocumentationStatus(DocumentationStatus.DOCUMENTED_AND_DISCOVERED);
                    log.info("API endpoint for app {}: {} {} transitioned to DOCUMENTED_AND_DISCOVERED via pattern match for {}", applicationId, ep.getMethod(), ep.getNormalizedPath(), path);
                }
                return apiEndpointRepository.save(ep);
            }
        }

        // 3. Create new DISCOVERED endpoint
        try {
            ApiEndpoint newEndpoint = new ApiEndpoint(applicationId, upperMethod, path);
            newEndpoint.setDocumentationStatus(DocumentationStatus.DISCOVERED);
            ApiEndpoint saved = apiEndpointRepository.save(newEndpoint);
            log.info("Discovered new API endpoint for app {}: {} {}", applicationId, upperMethod, path);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent request already inserted the same endpoint
            return apiEndpointRepository
                .findByApplicationIdAndMethodAndNormalizedPath(applicationId, upperMethod, path)
                .map(endpoint -> {
                    endpoint.setLastSeenAt(Instant.now());
                    if (endpoint.getDocumentationStatus() == DocumentationStatus.DOCUMENTED) {
                        endpoint.setDocumentationStatus(DocumentationStatus.DOCUMENTED_AND_DISCOVERED);
                    }
                    return apiEndpointRepository.save(endpoint);
                })
                .orElseGet(() -> new ApiEndpoint(applicationId, upperMethod, path));
        }
    }

    private boolean matchesPathTemplate(String templatePath, String actualPath) {
        if (templatePath == null || actualPath == null) return false;
        String[] tSegments = templatePath.split("/");
        String[] aSegments = actualPath.split("/");
        if (tSegments.length != aSegments.length) return false;

        for (int i = 0; i < tSegments.length; i++) {
            String t = tSegments[i];
            String a = aSegments[i];
            if (t.startsWith("{") && t.endsWith("}")) {
                if (a.isEmpty()) return false;
            } else if (!t.equalsIgnoreCase(a)) {
                return false;
            }
        }
        return true;
    }
}
