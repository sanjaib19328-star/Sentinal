package com.sentinel.api.service;

import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.RequestLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestLoggingService {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingService.class);

    private final RequestLogRepository requestLogRepository;

    public RequestLoggingService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Transactional
    public RequestLog logRequest(
        String requestId,
        Long apiKeyId,
        String method,
        String path,
        int statusCode,
        long latencyMs,
        String clientIp
    ) {
        return logRequest(requestId, null, apiKeyId, null, method, path, PathNormalizer.normalize(path), statusCode, latencyMs, clientIp);
    }

    @Transactional
    public RequestLog logRequest(
        String requestId,
        Long applicationId,
        Long apiKeyId,
        Long endpointId,
        String method,
        String path,
        String normalizedPath,
        int statusCode,
        long latencyMs,
        String clientIp
    ) {
        RequestLog requestLog = new RequestLog(
            requestId,
            applicationId,
            apiKeyId,
            endpointId,
            method,
            path,
            normalizedPath,
            statusCode,
            latencyMs,
            clientIp
        );

        RequestLog saved = requestLogRepository.save(requestLog);
        log.info("RequestLog recorded: requestId={}, appId={}, keyId={}, endpointId={}, method={}, path={}, normPath={}, status={}, latencyMs={}",
            requestId, applicationId, apiKeyId, endpointId, method, path, normalizedPath, statusCode, latencyMs);

        return saved;
    }
}
