package com.sentinel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.AiTestPlanDto;
import com.sentinel.api.dto.AiTestRunReportDto;
import com.sentinel.api.dto.AiTestStepDto;
import com.sentinel.api.dto.AiTestStepResultDto;
import com.sentinel.api.dto.ApiTestConsoleRequest;
import com.sentinel.api.dto.ApiTestConsoleResultDto;
import com.sentinel.api.dto.RunAiTestRequest;
import com.sentinel.api.exception.BadRequestException;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.Application;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiTestEngineService {

    private static final Logger log = LoggerFactory.getLogger(AiTestEngineService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_-]+)\\}");

    // Standard 1x1 transparent PNG fallback if test image is needed and user didn't attach one
    private static final String DEFAULT_TEST_IMAGE_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    private final ApplicationRepository applicationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final ApiTestConsoleService apiTestConsoleService;

    public AiTestEngineService(
        ApplicationRepository applicationRepository,
        ApiKeyRepository apiKeyRepository,
        ApiEndpointRepository apiEndpointRepository,
        ApiTestConsoleService apiTestConsoleService
    ) {
        this.applicationRepository = applicationRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiEndpointRepository = apiEndpointRepository;
        this.apiTestConsoleService = apiTestConsoleService;
    }

    public AiTestPlanDto generateTestPlan(Long ownerId, Long applicationId) {
        Application app = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        List<ApiEndpoint> endpoints = apiEndpointRepository.findByApplicationId(applicationId);

        // Fallback default endpoints for known patterns if DB catalog is empty
        if (endpoints.isEmpty()) {
            endpoints = generateFallbackEndpoints(app);
        }

        AiTestPlanDto plan = new AiTestPlanDto();
        plan.setPlanId("plan_" + UUID.randomUUID().toString().substring(0, 8));
        plan.setApplicationId(applicationId);
        plan.setApplicationName(app.getName());
        plan.setTitle("Autonomous AI Test Plan for " + app.getName());
        plan.setTotalEndpointsDiscovered(endpoints.size());

        List<AiTestStepDto> steps = new ArrayList<>();
        String creatorStepId = null;

        for (ApiEndpoint ep : endpoints) {
            AiTestStepDto step = new AiTestStepDto();
            String method = ep.getMethod().toUpperCase(Locale.ROOT);
            String path = ep.getNormalizedPath();
            String stepId = "step_" + method.toLowerCase() + "_" + path.replaceAll("[^a-zA-Z0-9]", "_");
            step.setStepId(stepId);
            step.setMethod(method);
            step.setPath(path);
            step.setName(method + " " + path);
            step.setDescription(ep.getSummary() != null ? ep.getSummary() : ep.getDescription());

            // Check if destructive
            boolean isDestructive = method.equals("DELETE") ||
                path.toLowerCase().contains("reset") ||
                path.toLowerCase().contains("purge") ||
                path.toLowerCase().contains("clear") ||
                path.toLowerCase().contains("delete");
            step.setDestructive(isDestructive);
            step.setRequiresApproval(isDestructive);

            // Check multipart
            if (path.toLowerCase().contains("upload") || path.toLowerCase().contains("image")) {
                if (method.equals("POST") || method.equals("PUT")) {
                    step.setMultipart(true);
                    step.setMultipartFieldName("file");
                    step.setRequestContentType("multipart/form-data");
                    step.getExtractedVariables().put("image_id", "response.body.image_id");
                    creatorStepId = stepId;
                }
            } else if (method.equals("POST") && (path.contains("user") || path.contains("item") || path.contains("order"))) {
                step.getExtractedVariables().put("id", "response.body.id");
                creatorStepId = stepId;
            }

            // Check path variables
            Matcher matcher = PATH_VAR_PATTERN.matcher(path);
            while (matcher.find()) {
                String varName = matcher.group(1);
                step.getParameterMappings().put(varName, "{" + varName + "}");
                if (creatorStepId != null && !creatorStepId.equals(stepId)) {
                    step.getDependsOnStepIds().add(creatorStepId);
                }
            }

            steps.add(step);
        }

        // Sort steps: Root/Health GETs -> Creation POSTs -> Dependent GETs/POSTs -> Destructive
        steps.sort(Comparator.comparingInt(s -> {
            if (s.isDestructive()) return 100;
            if (s.getPath().equals("/") || s.getPath().contains("health")) return 1;
            if (s.isMultipart() || (s.getMethod().equals("POST") && s.getDependsOnStepIds().isEmpty())) return 10;
            if (!s.getDependsOnStepIds().isEmpty()) return 30;
            return 20;
        }));

        plan.setSteps(steps);
        plan.setTotalStepsPlanned(steps.size());
        plan.setSummary("Discovered " + endpoints.size() + " endpoints. Built dependency graph with " + steps.size() + " sequential execution steps.");
        return plan;
    }

    public AiTestRunReportDto executeAiTestRun(Long ownerId, RunAiTestRequest request) {
        if (request.getApplicationId() == null) {
            throw new BadRequestException("Application ID is required for AI test run");
        }

        Long applicationId = request.getApplicationId();
        Application app = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Resolve API key
        ApiKey apiKey = resolveApiKey(applicationId, request.getApiKeyId());

        AiTestPlanDto plan = generateTestPlan(ownerId, applicationId);
        Map<String, String> rememberedContext = new HashMap<>(request.getInitialContext());

        // Retain file in memory if provided
        if (request.getFileBase64() != null && !request.getFileBase64().isBlank()) {
            rememberedContext.put("file_base64", request.getFileBase64());
            rememberedContext.put("file_name", request.getFileName() != null ? request.getFileName() : "uploaded_test_file.png");
            rememberedContext.put("file_content_type", request.getFileContentType() != null ? request.getFileContentType() : "image/png");
        } else {
            // Default sample image for upload workflows
            rememberedContext.putIfAbsent("file_base64", DEFAULT_TEST_IMAGE_PNG);
            rememberedContext.putIfAbsent("file_name", "sentinel_test_image.png");
            rememberedContext.putIfAbsent("file_content_type", "image/png");
        }

        long runStartTime = System.currentTimeMillis();
        String runId = "run_" + UUID.randomUUID().toString().substring(0, 8);

        AiTestRunReportDto report = new AiTestRunReportDto();
        report.setRunId(runId);
        report.setApplicationId(applicationId);
        report.setApplicationName(app.getName());
        report.setTotalSteps(plan.getSteps().size());

        List<AiTestStepResultDto> results = new ArrayList<>();
        int passedCount = 0;
        int failedCount = 0;
        int blockedCount = 0;
        int pendingApprovalCount = 0;
        long totalLatency = 0;

        for (AiTestStepDto step : plan.getSteps()) {
            AiTestStepResultDto result = new AiTestStepResultDto();
            result.setStepId(step.getStepId());
            result.setName(step.getName());
            result.setMethod(step.getMethod());
            result.setEndpoint(step.getPath());

            // 1. Destructive safety checkpoint
            if (step.isDestructive() && !request.isApproveDestructiveOperations()) {
                result.setRequiresApproval(true);
                result.setPassed(false);
                result.setBlocked(true);
                result.setStatus(428); // Precondition Required
                result.setError("Operation paused: Destructive endpoint requires explicit human approval.");
                result.setResponseSummary("Awaiting user confirmation before executing " + step.getMethod() + " " + step.getPath());
                pendingApprovalCount++;
                results.add(result);
                continue;
            }

            // 2. Resolve Path Variables from Remembered Context
            String resolvedPath = step.getPath();
            boolean hasMissingParam = false;
            String missingParamName = null;

            Matcher matcher = PATH_VAR_PATTERN.matcher(step.getPath());
            while (matcher.find()) {
                String varName = matcher.group(1);
                String val = rememberedContext.get(varName);
                if (val != null && !val.isBlank()) {
                    resolvedPath = resolvedPath.replace("{" + varName + "}", val);
                    result.getInputsUsed().put(varName, val);
                } else {
                    hasMissingParam = true;
                    missingParamName = varName;
                    break;
                }
            }

            if (hasMissingParam) {
                result.setBlocked(true);
                result.setPassed(false);
                result.setStatus(412); // Precondition Failed
                result.setError("Blocked: Missing required upstream parameter {" + missingParamName + "}.");
                result.setResponseSummary("Could not execute step because {" + missingParamName + "} was not generated by previous steps.");
                blockedCount++;
                results.add(result);
                continue;
            }

            result.setResolvedPath(resolvedPath);

            // 3. Prepare ApiTestConsoleRequest
            ApiTestConsoleRequest consoleReq = new ApiTestConsoleRequest();
            consoleReq.setApiKeyId(apiKey.getId());
            consoleReq.setMethod(step.getMethod());
            consoleReq.setPath(resolvedPath);

            if (step.isMultipart()) {
                consoleReq.setBinaryBodyBase64(rememberedContext.get("file_base64"));
                consoleReq.setFileName(rememberedContext.get("file_name"));
                consoleReq.setFileContentType(rememberedContext.get("file_content_type"));
                consoleReq.setFileFieldName(step.getMultipartFieldName() != null ? step.getMultipartFieldName() : "file");
                result.getInputsUsed().put("file", rememberedContext.get("file_name"));
            } else if (step.getMethod().equals("POST") || step.getMethod().equals("PUT")) {
                if (step.getRequestBodyTemplate() != null) {
                    String body = step.getRequestBodyTemplate();
                    for (Map.Entry<String, String> entry : rememberedContext.entrySet()) {
                        body = body.replace("{" + entry.getKey() + "}", entry.getValue());
                    }
                    consoleReq.setBody(body);
                } else {
                    consoleReq.setBody("{}");
                }
            }

            // Automatic Bearer Token Authorization Injection
            String authToken = rememberedContext.get("token");
            if (authToken == null) authToken = rememberedContext.get("accessToken");
            if (authToken == null) authToken = rememberedContext.get("access_token");
            if (authToken == null) authToken = rememberedContext.get("jwt");

            if (authToken != null && !authToken.isBlank() && !resolvedPath.contains("/auth/login") && !resolvedPath.contains("/auth/register")) {
                Map<String, String> headers = consoleReq.getHeaders() != null ? new HashMap<>(consoleReq.getHeaders()) : new HashMap<>();
                headers.put("Authorization", "Bearer " + authToken);
                consoleReq.setHeaders(headers);
                result.getInputsUsed().put("Authorization", "Bearer ••••••••");
            }

            // 4. Execute via Sentinel Gateway Pipeline!
            try {
                ApiTestConsoleResultDto testResult = apiTestConsoleService.executeTest(ownerId, applicationId, consoleReq);
                int statusCode = testResult.getStatusCode();
                result.setStatus(statusCode);
                result.setLatencyMs(testResult.getLatencyMs());
                result.setRequestId(testResult.getRequestId());
                totalLatency += testResult.getLatencyMs();

                boolean isOk = statusCode >= 200 && statusCode < 400;
                result.setPassed(isOk);

                if (!isOk) {
                    failedCount++;
                    String errMsg = "HTTP " + statusCode;
                    if (testResult.getResponseBody() != null && !testResult.getResponseBody().isBlank()) {
                        String bodySnippet = testResult.getResponseBody();
                        if (bodySnippet.length() > 200) bodySnippet = bodySnippet.substring(0, 197) + "...";
                        errMsg += ": " + bodySnippet;
                    }
                    result.setError(errMsg);
                    result.setResponseSummary(testResult.getResponseBody());
                } else {
                    passedCount++;
                    result.setResponseSummary(testResult.getResponseBody());

                    // 5. Extract output variables from response for reuse
                    extractAndRememberOutputs(testResult.getResponseBody(), rememberedContext, result);
                }
            } catch (Exception e) {
                log.warn("Step execution failed: {}", e.getMessage());
                result.setPassed(false);
                result.setStatus(500);
                result.setError(e.getMessage());
                failedCount++;
            }

            results.add(result);
        }

        long duration = System.currentTimeMillis() - runStartTime;
        report.setPassedSteps(passedCount);
        report.setFailedSteps(failedCount);
        report.setBlockedSteps(blockedCount);
        report.setPendingApprovalSteps(pendingApprovalCount);
        report.setTotalDurationMs(duration);
        report.setAvgLatencyMs(results.isEmpty() ? 0 : (double) totalLatency / results.size());
        report.setStepResults(results);
        report.setRememberedContext(rememberedContext);

        if (failedCount == 0 && blockedCount == 0 && pendingApprovalCount == 0) {
            report.setOverallStatus("PASSED");
            report.setExecutiveSummary("All " + passedCount + " API tests passed successfully with an average latency of " + String.format("%.1f", report.getAvgLatencyMs()) + "ms.");
        } else if (pendingApprovalCount > 0 && failedCount == 0) {
            report.setOverallStatus("NEEDS_APPROVAL");
            report.setExecutiveSummary("Executed " + passedCount + " safe tests. " + pendingApprovalCount + " destructive action(s) require explicit user approval.");
        } else {
            report.setOverallStatus("PARTIAL");
            report.setExecutiveSummary("Test suite completed with " + passedCount + " passed, " + failedCount + " failed, and " + blockedCount + " blocked steps.");
            report.setFailureAnalysis(generateFailureAnalysis(results));
        }

        return report;
    }

    private void extractAndRememberOutputs(String responseBody, Map<String, String> rememberedContext, AiTestStepResultDto result) {
        if (responseBody == null || responseBody.isBlank()) return;
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            extractJsonFields(root, "", rememberedContext, result);
        } catch (Exception e) {
            log.debug("Response body is not JSON: {}", e.getMessage());
        }
    }

    private void extractJsonFields(JsonNode node, String prefix, Map<String, String> rememberedContext, AiTestStepResultDto result) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode val = field.getValue();

                if (val.isValueNode()) {
                    String strVal = val.asText();
                    if (fieldName.equalsIgnoreCase("image_id") ||
                        fieldName.equalsIgnoreCase("id") ||
                        fieldName.equalsIgnoreCase("token") ||
                        fieldName.equalsIgnoreCase("accessToken") ||
                        fieldName.equalsIgnoreCase("access_token") ||
                        fieldName.equalsIgnoreCase("jwt") ||
                        fieldName.equalsIgnoreCase("bearerToken") ||
                        fieldName.equalsIgnoreCase("authToken") ||
                        fieldName.equalsIgnoreCase("eventId") ||
                        fieldName.equalsIgnoreCase("bookingId") ||
                        fieldName.equalsIgnoreCase("userId") ||
                        fieldName.equalsIgnoreCase("key") ||
                        fieldName.equalsIgnoreCase("taskId")) {

                        rememberedContext.put(fieldName, strVal);
                        // Also store normalized "token" key if this is an auth token
                        if (fieldName.equalsIgnoreCase("accessToken") ||
                            fieldName.equalsIgnoreCase("access_token") ||
                            fieldName.equalsIgnoreCase("jwt") ||
                            fieldName.equalsIgnoreCase("authToken")) {
                            rememberedContext.put("token", strVal);
                        }
                        result.getOutputsExtracted().put(fieldName, strVal);
                        log.info("AI Test Engine extracted and remembered: {} = {}", fieldName, strVal);
                    }
                } else if (val.isObject()) {
                    extractJsonFields(val, prefix + fieldName + ".", rememberedContext, result);
                }
            }
        }
    }

    private String generateFailureAnalysis(List<AiTestStepResultDto> results) {
        StringBuilder sb = new StringBuilder();
        for (AiTestStepResultDto step : results) {
            if (!step.isPassed() && step.getError() != null) {
                sb.append("• ").append(step.getMethod()).append(" ").append(step.getEndpoint())
                  .append(" failed with status ").append(step.getStatus())
                  .append(": ").append(step.getError()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private ApiKey resolveApiKey(Long applicationId, Long preferredApiKeyId) {
        if (preferredApiKeyId != null) {
            return apiKeyRepository.findByIdAndApplicationId(preferredApiKeyId, applicationId)
                .filter(ApiKey::isActive)
                .orElseThrow(() -> new BadRequestException("Specified API key is invalid or revoked"));
        }

        List<ApiKey> keys = apiKeyRepository.findByApplicationId(applicationId);
        return keys.stream()
            .filter(ApiKey::isActive)
            .findFirst()
            .orElseThrow(() -> new BadRequestException("No active API Key found for this application. Please create a developer API key first."));
    }

    private List<ApiEndpoint> generateFallbackEndpoints(Application app) {
        List<ApiEndpoint> list = new ArrayList<>();
        // Check if endpoints are already registered in the API catalog
        List<ApiEndpoint> existing = apiEndpointRepository.findByApplicationIdOrderByLastSeenAtDesc(app.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        // Standard default probe endpoints
        list.add(new ApiEndpoint(app.getId(), "GET", "/"));
        list.add(new ApiEndpoint(app.getId(), "GET", "/health"));
        list.add(new ApiEndpoint(app.getId(), "GET", "/api/health"));
        list.add(new ApiEndpoint(app.getId(), "GET", "/actuator/health"));
        return list;
    }
}
