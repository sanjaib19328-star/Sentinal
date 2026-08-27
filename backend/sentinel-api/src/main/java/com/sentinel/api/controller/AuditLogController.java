package com.sentinel.api.controller;

import com.sentinel.api.dto.AuditLogDto;
import com.sentinel.api.dto.PagedResponse;
import com.sentinel.api.model.AuditAction;
import com.sentinel.api.security.UserPrincipal;
import com.sentinel.api.service.AuditLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('USER')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AuditLogDto>> getAuditLogs(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) Long applicationId,
        @RequestParam(required = false) AuditAction action,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<AuditLogDto> result = auditLogService.getAuditLogs(principal.getId(), applicationId, action, pageable);
        return ResponseEntity.ok(result);
    }
}
