package com.sentinel.api.controller;

import com.sentinel.api.dto.GlobalRequestLogDto;
import com.sentinel.api.dto.PagedResponse;
import com.sentinel.api.security.UserPrincipal;
import com.sentinel.api.service.GlobalRequestLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/requests")
public class GlobalRequestController {

    private final GlobalRequestLogService globalRequestLogService;

    public GlobalRequestController(GlobalRequestLogService globalRequestLogService) {
        this.globalRequestLogService = globalRequestLogService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<GlobalRequestLogDto>> getGlobalRequests(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) Long applicationId,
        @RequestParam(required = false) Long apiKeyId,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) Integer statusCode,
        @RequestParam(required = false) String statusClass,
        @RequestParam(required = false) String requestId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        PagedResponse<GlobalRequestLogDto> response = globalRequestLogService.getGlobalRequestLogs(
            principal.getId(),
            applicationId,
            apiKeyId,
            method,
            statusCode,
            statusClass,
            requestId,
            search,
            from,
            to,
            page,
            size
        );
        return ResponseEntity.ok(response);
    }
}
