package com.sentinel.api.controller;

import com.sentinel.api.dto.AlertDto;
import com.sentinel.api.dto.AlertRuleDto;
import com.sentinel.api.dto.CreateAlertRuleRequest;
import com.sentinel.api.dto.UpdateAlertRuleRequest;
import com.sentinel.api.security.UserPrincipal;
import com.sentinel.api.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasRole('USER')")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/api/v1/applications/{applicationId}/alerts")
    public ResponseEntity<List<AlertDto>> listAlerts(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId
    ) {
        List<AlertDto> alerts = alertService.listApplicationAlerts(principal.getId(), applicationId);
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/api/v1/applications/{applicationId}/alert-rules")
    public ResponseEntity<List<AlertRuleDto>> listAlertRules(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId
    ) {
        List<AlertRuleDto> rules = alertService.listAlertRules(principal.getId(), applicationId);
        return ResponseEntity.ok(rules);
    }

    @PostMapping("/api/v1/applications/{applicationId}/alert-rules")
    public ResponseEntity<AlertRuleDto> createAlertRule(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @Valid @RequestBody CreateAlertRuleRequest request
    ) {
        AlertRuleDto rule = alertService.createAlertRule(principal.getId(), applicationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PutMapping("/api/v1/applications/{applicationId}/alert-rules/{ruleId}")
    public ResponseEntity<AlertRuleDto> updateAlertRule(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @PathVariable Long ruleId,
        @RequestBody UpdateAlertRuleRequest request
    ) {
        AlertRuleDto rule = alertService.updateAlertRule(principal.getId(), applicationId, ruleId, request);
        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/api/v1/applications/{applicationId}/alert-rules/{ruleId}")
    public ResponseEntity<Void> deleteAlertRule(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @PathVariable Long ruleId
    ) {
        alertService.deleteAlertRule(principal.getId(), applicationId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/alerts/{alertId}/acknowledge")
    public ResponseEntity<AlertDto> acknowledgeAlert(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long alertId
    ) {
        AlertDto alert = alertService.acknowledgeAlert(principal.getId(), alertId);
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/api/v1/alerts/{alertId}/resolve")
    public ResponseEntity<AlertDto> resolveAlert(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long alertId
    ) {
        AlertDto alert = alertService.resolveAlert(principal.getId(), alertId);
        return ResponseEntity.ok(alert);
    }
}
