package com.sentinel.api.controller;

import com.sentinel.api.dto.ApiPolicyDto;
import com.sentinel.api.dto.SavePolicyRequest;
import com.sentinel.api.security.UserPrincipal;
import com.sentinel.api.service.ApiPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}")
@PreAuthorize("hasRole('USER')")
public class PolicyController {

    private final ApiPolicyService apiPolicyService;

    public PolicyController(ApiPolicyService apiPolicyService) {
        this.apiPolicyService = apiPolicyService;
    }

    @GetMapping("/policy")
    public ResponseEntity<ApiPolicyDto> getApplicationPolicy(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId
    ) {
        ApiPolicyDto policy = apiPolicyService.getApplicationPolicy(principal.getId(), applicationId);
        return ResponseEntity.ok(policy);
    }

    @PutMapping("/policy")
    public ResponseEntity<ApiPolicyDto> saveApplicationPolicy(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @RequestBody SavePolicyRequest request
    ) {
        ApiPolicyDto policy = apiPolicyService.saveApplicationPolicy(principal.getId(), applicationId, request);
        return ResponseEntity.ok(policy);
    }

    @DeleteMapping("/policy")
    public ResponseEntity<Void> deleteApplicationPolicy(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId
    ) {
        apiPolicyService.deleteApplicationPolicy(principal.getId(), applicationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/apis/{apiId}/policy")
    public ResponseEntity<ApiPolicyDto> getEndpointPolicy(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @PathVariable Long apiId
    ) {
        ApiPolicyDto policy = apiPolicyService.getEndpointPolicy(principal.getId(), applicationId, apiId);
        return ResponseEntity.ok(policy);
    }

    @PutMapping("/apis/{apiId}/policy")
    public ResponseEntity<ApiPolicyDto> saveEndpointPolicy(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @PathVariable Long apiId,
        @RequestBody SavePolicyRequest request
    ) {
        ApiPolicyDto policy = apiPolicyService.saveEndpointPolicy(principal.getId(), applicationId, apiId, request);
        return ResponseEntity.ok(policy);
    }

    @DeleteMapping("/apis/{apiId}/policy")
    public ResponseEntity<Void> deleteEndpointPolicy(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @PathVariable Long apiId
    ) {
        apiPolicyService.deleteEndpointPolicy(principal.getId(), applicationId, apiId);
        return ResponseEntity.noContent().build();
    }
}
