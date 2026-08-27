package com.sentinel.api.service;

import com.sentinel.api.dto.AlertDto;
import com.sentinel.api.dto.AlertRuleDto;
import com.sentinel.api.dto.CreateAlertRuleRequest;
import com.sentinel.api.dto.UpdateAlertRuleRequest;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.Alert;
import com.sentinel.api.model.AlertRule;
import com.sentinel.api.model.AlertRuleType;
import com.sentinel.api.model.AlertSeverity;
import com.sentinel.api.model.AlertStatus;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.AuditAction;
import com.sentinel.api.model.HealthStatus;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.AlertRepository;
import com.sentinel.api.repository.AlertRuleRepository;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final ApplicationRepository applicationRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final RequestLogRepository requestLogRepository;
    private final AuditLogService auditLogService;

    public AlertService(
        AlertRuleRepository alertRuleRepository,
        AlertRepository alertRepository,
        ApplicationRepository applicationRepository,
        ApiEndpointRepository apiEndpointRepository,
        RequestLogRepository requestLogRepository,
        AuditLogService auditLogService
    ) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertRepository = alertRepository;
        this.applicationRepository = applicationRepository;
        this.apiEndpointRepository = apiEndpointRepository;
        this.requestLogRepository = requestLogRepository;
        this.auditLogService = auditLogService;
    }

    public List<AlertRuleDto> listAlertRules(Long ownerId, Long applicationId) {
        verifyApplicationOwnership(ownerId, applicationId);
        return alertRuleRepository.findAllByApplicationId(applicationId).stream()
            .map(this::mapRuleToDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public AlertRuleDto createAlertRule(Long ownerId, Long applicationId, CreateAlertRuleRequest request) {
        verifyApplicationOwnership(ownerId, applicationId);
        if (request.getApiEndpointId() != null) {
            verifyEndpointBelongsToApp(applicationId, request.getApiEndpointId());
        }

        AlertRule rule = new AlertRule();
        rule.setApplicationId(applicationId);
        rule.setApiEndpointId(request.getApiEndpointId());
        rule.setType(request.getType());
        rule.setThreshold(request.getThreshold());
        rule.setEvaluationWindowSeconds(request.getEvaluationWindowSeconds() != null ? request.getEvaluationWindowSeconds() : 300);
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        AlertRule saved = alertRuleRepository.save(rule);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.ALERT_RULE_CREATED,
            "ALERT_RULE",
            String.valueOf(saved.getId()),
            "Created rule " + saved.getType() + " with threshold " + saved.getThreshold(),
            null
        );

        return mapRuleToDto(saved);
    }

    @Transactional
    public AlertRuleDto updateAlertRule(Long ownerId, Long applicationId, Long ruleId, UpdateAlertRuleRequest request) {
        verifyApplicationOwnership(ownerId, applicationId);
        AlertRule rule = alertRuleRepository.findByIdAndApplicationId(ruleId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found"));

        if (request.getType() != null) rule.setType(request.getType());
        if (request.getThreshold() != null) rule.setThreshold(request.getThreshold());
        if (request.getEvaluationWindowSeconds() != null) rule.setEvaluationWindowSeconds(request.getEvaluationWindowSeconds());
        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());

        AlertRule updated = alertRuleRepository.save(rule);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.ALERT_RULE_UPDATED,
            "ALERT_RULE",
            String.valueOf(updated.getId()),
            "Updated rule " + updated.getType() + ", enabled=" + updated.isEnabled(),
            null
        );

        return mapRuleToDto(updated);
    }

    @Transactional
    public void deleteAlertRule(Long ownerId, Long applicationId, Long ruleId) {
        verifyApplicationOwnership(ownerId, applicationId);
        AlertRule rule = alertRuleRepository.findByIdAndApplicationId(ruleId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found"));

        alertRuleRepository.delete(rule);

        auditLogService.record(
            ownerId,
            applicationId,
            AuditAction.ALERT_RULE_DELETED,
            "ALERT_RULE",
            String.valueOf(ruleId),
            "Deleted alert rule " + rule.getType(),
            null
        );
    }

    public List<AlertDto> listApplicationAlerts(Long ownerId, Long applicationId) {
        verifyApplicationOwnership(ownerId, applicationId);
        // Run real-time evaluation before returning alerts
        evaluateAlertsForApplication(applicationId);
        return alertRepository.findAllByApplicationIdOrderByTriggeredAtDesc(applicationId).stream()
            .map(this::mapAlertToDto)
            .collect(Collectors.toList());
    }

    public List<AlertDto> listActiveAlertsForOwner(Long ownerId) {
        List<Application> userApps = applicationRepository.findAllByOwnerId(ownerId);
        List<Long> appIds = userApps.stream().map(Application::getId).collect(Collectors.toList());
        if (appIds.isEmpty()) {
            return List.of();
        }

        // Evaluate all user's apps
        for (Long appId : appIds) {
            evaluateAlertsForApplication(appId);
        }

        return alertRepository.findTop10ByApplicationIdInAndStatusOrderByTriggeredAtDesc(appIds, AlertStatus.ACTIVE).stream()
            .map(this::mapAlertToDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public AlertDto acknowledgeAlert(Long ownerId, Long alertId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        verifyApplicationOwnership(ownerId, alert.getApplicationId());

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        Alert saved = alertRepository.save(alert);

        auditLogService.record(
            ownerId,
            saved.getApplicationId(),
            AuditAction.ALERT_ACKNOWLEDGED,
            "ALERT",
            String.valueOf(saved.getId()),
            "Acknowledged alert: " + saved.getMessage(),
            null
        );

        return mapAlertToDto(saved);
    }

    @Transactional
    public AlertDto resolveAlert(Long ownerId, Long alertId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        verifyApplicationOwnership(ownerId, alert.getApplicationId());

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        Alert saved = alertRepository.save(alert);

        auditLogService.record(
            ownerId,
            saved.getApplicationId(),
            AuditAction.ALERT_RESOLVED,
            "ALERT",
            String.valueOf(saved.getId()),
            "Resolved alert: " + saved.getMessage(),
            null
        );

        return mapAlertToDto(saved);
    }

    /**
     * Evaluates alert rules against real persisted telemetry and updates active alerts.
     */
    @Transactional
    public void evaluateAlertsForApplication(Long applicationId) {
        List<AlertRule> rules = alertRuleRepository.findAllByApplicationId(applicationId).stream()
            .filter(AlertRule::isEnabled)
            .collect(Collectors.toList());

        if (rules.isEmpty()) return;

        Application app = applicationRepository.findById(applicationId).orElse(null);
        if (app == null) return;

        Instant now = Instant.now().plusSeconds(2);

        for (AlertRule rule : rules) {
            int window = rule.getEvaluationWindowSeconds() > 0 ? rule.getEvaluationWindowSeconds() : 300;
            Instant windowStart = now.minusSeconds(window + 4);

            List<RequestLog> windowLogs;
            if (rule.getApiEndpointId() != null) {
                windowLogs = requestLogRepository.findByEndpointIdAndTimestampBetween(rule.getApiEndpointId(), windowStart, now);
            } else {
                windowLogs = requestLogRepository.findByApplicationIdAndTimestampBetween(applicationId, windowStart, now);
            }

            boolean triggerCondition = false;
            String message = "";
            AlertSeverity severity = AlertSeverity.WARNING;

            switch (rule.getType()) {
                case HIGH_ERROR_RATE:
                    if (!windowLogs.isEmpty()) {
                        long total = windowLogs.size();
                        long errors = windowLogs.stream().filter(r -> r.getStatusCode() >= 400).count();
                        double errorRate = ((double) errors / total) * 100.0;
                        if (errorRate > rule.getThreshold()) {
                            triggerCondition = true;
                            severity = errorRate > 50.0 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
                            message = String.format("High error rate detected: %.1f%% errors (threshold: %.1f%%) over last %ds (%d requests).",
                                errorRate, rule.getThreshold(), window, total);
                        }
                    }
                    break;

                case HIGH_LATENCY:
                    if (!windowLogs.isEmpty()) {
                        List<Long> latencies = windowLogs.stream()
                            .map(RequestLog::getLatencyMs)
                            .sorted()
                            .collect(Collectors.toList());
                        int p95Index = (int) Math.ceil(0.95 * latencies.size()) - 1;
                        double p95 = latencies.get(Math.max(0, p95Index));
                        if (p95 > rule.getThreshold()) {
                            triggerCondition = true;
                            severity = p95 > (rule.getThreshold() * 2) ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
                            message = String.format("High P95 latency detected: %.0f ms (threshold: %.0f ms) over last %ds.",
                                p95, rule.getThreshold(), window);
                        }
                    }
                    break;

                case API_UNAVAILABLE:
                    if (app.getHealthStatus() == HealthStatus.UNAVAILABLE) {
                        triggerCondition = true;
                        severity = AlertSeverity.CRITICAL;
                        message = "Application target is UNAVAILABLE / unreachable during health checks.";
                    } else if (!windowLogs.isEmpty()) {
                        long count502 = windowLogs.stream().filter(r -> r.getStatusCode() == 502 || r.getStatusCode() == 504).count();
                        if (count502 >= rule.getThreshold()) {
                            triggerCondition = true;
                            severity = AlertSeverity.CRITICAL;
                            message = String.format("Application returned %d gateway failure errors (502/504) in the last %ds.", count502, window);
                        }
                    }
                    break;

                case EXCESSIVE_429:
                    if (!windowLogs.isEmpty()) {
                        long count429 = windowLogs.stream().filter(r -> r.getStatusCode() == 429).count();
                        if (count429 >= rule.getThreshold()) {
                            triggerCondition = true;
                            severity = AlertSeverity.WARNING;
                            message = String.format("Excessive rate-limiting: %d requests throttled (429) in the last %ds.", count429, window);
                        }
                    }
                    break;

                case QUOTA_APPROACHING:
                    // Handled if quota counters in Redis are tracked
                    break;
            }

            Optional<Alert> existingActiveAlert = alertRepository.findFirstByAlertRuleIdAndStatus(rule.getId(), AlertStatus.ACTIVE);

            if (triggerCondition) {
                if (existingActiveAlert.isEmpty()) {
                    Alert newAlert = new Alert(rule.getId(), applicationId, rule.getApiEndpointId(), severity, message);
                    alertRepository.save(newAlert);
                } else {
                    Alert existing = existingActiveAlert.get();
                    existing.setMessage(message);
                    existing.setSeverity(severity);
                    alertRepository.save(existing);
                }
            } else {
                // If condition cleared, resolve active alert
                if (existingActiveAlert.isPresent()) {
                    Alert active = existingActiveAlert.get();
                    active.setStatus(AlertStatus.RESOLVED);
                    active.setResolvedAt(Instant.now());
                    alertRepository.save(active);
                }
            }
        }
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

    public AlertRuleDto mapRuleToDto(AlertRule r) {
        return new AlertRuleDto(
            r.getId(),
            r.getApplicationId(),
            r.getApiEndpointId(),
            r.getType(),
            r.getThreshold(),
            r.getEvaluationWindowSeconds(),
            r.isEnabled(),
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }

    public AlertDto mapAlertToDto(Alert a) {
        return new AlertDto(
            a.getId(),
            a.getAlertRuleId(),
            a.getApplicationId(),
            a.getApiEndpointId(),
            a.getStatus(),
            a.getSeverity(),
            a.getMessage(),
            a.getTriggeredAt(),
            a.getResolvedAt()
        );
    }
}
