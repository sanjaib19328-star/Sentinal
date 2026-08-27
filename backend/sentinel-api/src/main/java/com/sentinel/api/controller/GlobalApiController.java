package com.sentinel.api.controller;

import com.sentinel.api.dto.GlobalApiEndpointDto;
import com.sentinel.api.model.DocumentationStatus;
import com.sentinel.api.security.UserPrincipal;
import com.sentinel.api.service.GlobalApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/apis")
public class GlobalApiController {

    private final GlobalApiService globalApiService;

    public GlobalApiController(GlobalApiService globalApiService) {
        this.globalApiService = globalApiService;
    }

    @GetMapping
    public ResponseEntity<List<GlobalApiEndpointDto>> listGlobalApis(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long applicationId,
        @RequestParam(required = false) String method,
        @RequestParam(required = false) DocumentationStatus documentationStatus,
        @RequestParam(required = false) Boolean deprecated,
        @RequestParam(required = false, defaultValue = "requests") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        List<GlobalApiEndpointDto> result = globalApiService.listGlobalApis(
            principal.getId(),
            search,
            applicationId,
            method,
            documentationStatus,
            deprecated,
            sortBy,
            sortDir
        );
        return ResponseEntity.ok(result);
    }
}
