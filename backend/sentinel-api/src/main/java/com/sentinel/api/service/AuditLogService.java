package com.sentinel.api.service;

import com.sentinel.api.dto.AuditLogDto;
import com.sentinel.api.dto.PagedResponse;
import com.sentinel.api.model.AuditAction;
import com.sentinel.api.model.AuditLog;
import com.sentinel.api.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(Long userId, Long applicationId, AuditAction action, String resourceType, String resourceId, String metadata, String ipAddress) {
        AuditLog log = new AuditLog(userId, applicationId, action, resourceType, resourceId, metadata, ipAddress);
        auditLogRepository.save(log);
    }

    public PagedResponse<AuditLogDto> getAuditLogs(Long userId, Long applicationId, AuditAction action, Pageable pageable) {
        Page<AuditLog> page;

        if (applicationId != null && action != null) {
            page = auditLogRepository.findByUserIdAndApplicationIdAndAction(userId, applicationId, action, pageable);
        } else if (applicationId != null) {
            page = auditLogRepository.findByUserIdAndApplicationId(userId, applicationId, pageable);
        } else if (action != null) {
            page = auditLogRepository.findByUserIdAndAction(userId, action, pageable);
        } else {
            page = auditLogRepository.findByUserId(userId, pageable);
        }

        List<AuditLogDto> dtos = page.getContent().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());

        return new PagedResponse<>(
            dtos,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    public AuditLogDto mapToDto(AuditLog log) {
        return new AuditLogDto(
            log.getId(),
            log.getUserId(),
            log.getApplicationId(),
            log.getAction(),
            log.getResourceType(),
            log.getResourceId(),
            log.getMetadata(),
            log.getIpAddress(),
            log.getCreatedAt()
        );
    }
}
