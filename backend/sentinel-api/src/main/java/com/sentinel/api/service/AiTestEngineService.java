package com.sentinel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.AiTestMissingInputDto;
import com.sentinel.api.dto.AiTestPlanDto;
import com.sentinel.api.dto.AiTestRunReportDto;
import com.sentinel.api.dto.AiTestSessionDto;
import com.sentinel.api.dto.AiTestStepDto;
import com.sentinel.api.dto.AiTestStepResultDto;
import com.sentinel.api.dto.ApiTestConsoleRequest;
import com.sentinel.api.dto.ApiTestConsoleResultDto;
import com.sentinel.api.dto.BulkApiCheckRequest;
import com.sentinel.api.dto.BulkApiCheckResponse;
import com.sentinel.api.dto.BulkApiEndpointResultDto;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiTestEngineService {

    private static final Logger log = LoggerFactory.getLogger(AiTestEngineService.class);
    private static final ObjectMapper MAPPER = com.fasterxml.jackson.databind.json.JsonMapper.builder()
        .findAndAddModules()
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_-]+)\\}");

    // Standard 1x1 transparent PNG fallback if test image is provided or needed
    private static final String DEFAULT_TEST_IMAGE_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    private final ApplicationRepository applicationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiEndpointRepository apiEndpointRepository;
    private final ApiTestConsoleService apiTestConsoleService;

    // Resumable, waitable test sessions in memory (persisted across modal open/close)
    private final Map<String, AiTestSessionDto> activeSessions = new ConcurrentHashMap<>();

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

    /**
     * Constructs a dependency-aware Directed Acyclic Graph (DAG) of execution steps for the target application.
     */
    public AiTestPlanDto generateTestPlan(Long ownerId, Long applicationId) {
        Application app = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        List<ApiEndpoint> endpoints = apiEndpointRepository.findByApplicationId(applicationId);
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

        // First pass: identify steps and find producer endpoints (e.g. Upload, Auth, Creation)
        String uploadStepId = null;
        String cleanStepId = null;
        String authStepId = null;

        for (ApiEndpoint ep : endpoints) {
            String method = ep.getMethod().toUpperCase(Locale.ROOT);
            String path = ep.getNormalizedPath();
            String lowerPath = path.toLowerCase(Locale.ROOT);
            String stepId = "step_" + method.toLowerCase() + "_" + path.replaceAll("[^a-zA-Z0-9]", "_");

            AiTestStepDto step = new AiTestStepDto();
            step.setStepId(stepId);
            step.setMethod(method);
            step.setPath(path);
            step.setName(method + " " + path);
            step.setDescription(ep.getSummary() != null ? ep.getSummary() : ep.getDescription());

            // 1. Identify Producer: Multipart File Upload
            if (lowerPath.contains("upload") || (method.equals("POST") && !lowerPath.contains("{") && (lowerPath.contains("image") || lowerPath.contains("file")))) {
                step.setMultipart(true);
                step.setMultipartFieldName("file");
                step.setRequestContentType("multipart/form-data");
                step.getProducedVariables().add("image_id");
                step.getExtractedVariables().put("image_id", "response.body.image_id");
                step.setLevel(1);
                uploadStepId = stepId;
            }
            // 2. Identify Producer: Authentication
            else if (lowerPath.contains("/auth/login") || lowerPath.contains("/auth/token") || lowerPath.contains("/oauth/token")) {
                step.getProducedVariables().add("token");
                step.getExtractedVariables().put("token", "response.body.token");
                step.setLevel(1);
                authStepId = stepId;
            }
            // 3. Root / Probe / Health Check (Level 0)
            else if (path.equals("/") || lowerPath.contains("health") || lowerPath.contains("actuator")) {
                step.setLevel(0);
            }

            // Check if destructive/state-changing
            boolean isDestructive = method.equals("DELETE") ||
                lowerPath.contains("clean") ||
                lowerPath.contains("reset") ||
                lowerPath.contains("purge") ||
                lowerPath.contains("clear") ||
                lowerPath.contains("delete");
            step.setDestructive(isDestructive);
            step.setRequiresApproval(isDestructive);

            if (lowerPath.contains("clean")) {
                cleanStepId = stepId;
            }

            steps.add(step);
        }

        // Second pass: assign consumer dependencies based on variables and business semantics
        for (AiTestStepDto step : steps) {
            String lowerPath = step.getPath().toLowerCase(Locale.ROOT);
            Matcher matcher = PATH_VAR_PATTERN.matcher(step.getPath());
            while (matcher.find()) {
                String varName = matcher.group(1);
                step.getParameterMappings().put(varName, "{" + varName + "}");
                step.getRequiredVariables().add(varName);

                if (varName.equalsIgnoreCase("image_id") && uploadStepId != null && !uploadStepId.equals(step.getStepId())) {
                    if (!step.getDependsOnStepIds().contains(uploadStepId)) {
                        step.getDependsOnStepIds().add(uploadStepId);
                    }
                }
            }

            // Assign DAG levels and specific semantic dependencies for image pipeline:
            // Upload -> Analyze -> Clean -> Download; and Upload -> Report
            if (lowerPath.contains("upload")) {
                step.setLevel(1);
            } else if (lowerPath.contains("analyze")) {
                step.setLevel(2);
                if (uploadStepId != null && !step.getDependsOnStepIds().contains(uploadStepId)) {
                    step.getDependsOnStepIds().add(uploadStepId);
                }
            } else if (lowerPath.contains("clean")) {
                step.setLevel(3);
                if (uploadStepId != null && !step.getDependsOnStepIds().contains(uploadStepId)) {
                    step.getDependsOnStepIds().add(uploadStepId);
                }
                step.getProducedVariables().add("cleaned_image_id");
                step.getExtractedVariables().put("cleaned_image_id", "response.body.image_id");
            } else if (lowerPath.contains("download")) {
                step.setLevel(4);
                // Download requires image_id AND a successful clean operation
                if (cleanStepId != null && !step.getDependsOnStepIds().contains(cleanStepId)) {
                    step.getDependsOnStepIds().add(cleanStepId);
                } else if (uploadStepId != null && !step.getDependsOnStepIds().contains(uploadStepId)) {
                    step.getDependsOnStepIds().add(uploadStepId);
                }
            } else if (lowerPath.contains("report") || lowerPath.contains("summary")) {
                step.setLevel(5);
                // Report requires image_id from upload, independent of clean
                if (uploadStepId != null && !step.getDependsOnStepIds().contains(uploadStepId)) {
                    step.getDependsOnStepIds().add(uploadStepId);
                }
            } else if (step.getLevel() == 0 && (step.getPath().equals("/") || lowerPath.contains("health"))) {
                step.setLevel(0);
            } else if (step.getLevel() == 0) {
                step.setLevel(2);
            }
        }

        // Topological Sort (Level 0 -> Level 1 -> Level 2 -> Level 3 -> Level 4 -> Level 5)
        steps.sort(Comparator.comparingInt(AiTestStepDto::getLevel)
            .thenComparing(s -> s.getMethod().equals("GET") ? 0 : 1)
            .thenComparing(AiTestStepDto::getPath));

        // Pre-Execution Input Discovery
        List<AiTestMissingInputDto> missingInputs = discoverMissingInputs(app, steps, Collections.emptyMap());
        plan.setMissingInputs(missingInputs);
        if (!missingInputs.isEmpty()) {
            plan.setStatus("WAITING_FOR_INPUT");
        } else {
            plan.setStatus("READY");
        }

        plan.setSteps(steps);
        plan.setTotalStepsPlanned(steps.size());
        plan.setSummary("Discovered " + endpoints.size() + " endpoints. Built dependency DAG with " + steps.size() + " ordered execution steps.");
        return plan;
    }

    /**
     * Inspects the test plan and detects genuinely missing inputs (files, keys, external variables) before execution.
     */
    public List<AiTestMissingInputDto> discoverMissingInputs(Application app, List<AiTestStepDto> steps, Map<String, String> providedInputs) {
        List<AiTestMissingInputDto> missing = new ArrayList<>();

        // 1. Check Multipart file requirement
        for (AiTestStepDto step : steps) {
            if (step.isMultipart()) {
                boolean hasFile = providedInputs.containsKey("file_base64") && !providedInputs.get("file_base64").isBlank();
                if (!hasFile) {
                    AiTestMissingInputDto fileInput = new AiTestMissingInputDto(
                        "file_base64",
                        "FILE",
                        step.getPath(),
                        step.getMethod(),
                        step.getMethod() + " " + step.getPath() + " requires a multipart image file. Please provide the test image."
                    );
                    missing.add(fileInput);
                    step.setRequiresInput(true);
                    step.setMissingInputType("FILE");
                    step.setMissingInputPrompt(fileInput.getPrompt());
                }
            }
        }

        // 2. Check API Key requirement
        List<ApiKey> keys = apiKeyRepository.findByApplicationId(app.getId());
        boolean hasActiveKey = keys.stream().anyMatch(ApiKey::isActive);
        if (!hasActiveKey && !providedInputs.containsKey("apiKey")) {
            missing.add(new AiTestMissingInputDto(
                "apiKey",
                "API_KEY",
                app.getBaseUrl(),
                "ALL",
                "No active API Key found for " + app.getName() + ". Please provide or generate an active Sentinel API key."
            ));
        }

        // 3. Check for external path variables not produced by upstream endpoints
        List<String> allProducedVars = new ArrayList<>();
        for (AiTestStepDto step : steps) {
            allProducedVars.addAll(step.getProducedVariables());
        }

        for (AiTestStepDto step : steps) {
            for (String requiredVar : step.getRequiredVariables()) {
                if (!allProducedVars.contains(requiredVar) && !providedInputs.containsKey(requiredVar)) {
                    missing.add(new AiTestMissingInputDto(
                        requiredVar,
                        "VARIABLE",
                        step.getPath(),
                        step.getMethod(),
                        step.getMethod() + " " + step.getPath() + " requires parameter '{" + requiredVar + "}' which is not produced by any upstream step."
                    ));
                    step.setRequiresInput(true);
                    step.setMissingInputType("VARIABLE");
                    step.setMissingInputPrompt("Requires value for {" + requiredVar + "}");
                }
            }
        }

        return missing;
    }

    /**
     * Executes the autonomous AI test run against the live Sentinel gateway with variable propagation and strict dependency checks.
     */
    public AiTestRunReportDto executeAiTestRun(Long ownerId, RunAiTestRequest request) {
        if (request.getApplicationId() == null) {
            throw new BadRequestException("Application ID is required for AI test run");
        }

        Long applicationId = request.getApplicationId();
        Application app = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        ApiKey apiKey = resolveApiKey(applicationId, request.getApiKeyId());
        AiTestPlanDto plan = generateTestPlan(ownerId, applicationId);

        Map<String, String> runtimeVariables = new HashMap<>(request.getInitialContext());

        // File Context
        if (request.getFileBase64() != null && !request.getFileBase64().isBlank()) {
            runtimeVariables.put("file_base64", request.getFileBase64());
            runtimeVariables.put("file_name", request.getFileName() != null ? request.getFileName() : "sentinel_test_image.png");
            runtimeVariables.put("file_content_type", request.getFileContentType() != null ? request.getFileContentType() : "image/png");
        } else if (!runtimeVariables.containsKey("file_base64")) {
            // Default sample image if user didn't attach a specific file
            runtimeVariables.put("file_base64", DEFAULT_TEST_IMAGE_PNG);
            runtimeVariables.put("file_name", "sentinel_test_image.png");
            runtimeVariables.put("file_content_type", "image/png");
        }

        long runStartTime = System.currentTimeMillis();
        String runId = "run_" + UUID.randomUUID().toString().substring(0, 8);

        AiTestRunReportDto report = new AiTestRunReportDto();
        report.setRunId(runId);
        report.setApplicationId(applicationId);
        report.setApplicationName(app.getName());
        report.setTotalSteps(plan.getSteps().size());

        List<AiTestStepResultDto> results = new ArrayList<>();
        Map<String, Boolean> stepSuccess = new HashMap<>();
        Map<String, String> stepErrors = new HashMap<>();

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

            String lowerPath = step.getPath().toLowerCase(Locale.ROOT);

            // 1. Dependency Prerequisite Check
            boolean dependencyFailed = false;
            String failedPrereqReason = null;

            for (String prereqId : step.getDependsOnStepIds()) {
                if (stepSuccess.containsKey(prereqId) && !stepSuccess.get(prereqId)) {
                    dependencyFailed = true;
                    if (lowerPath.contains("download")) {
                        failedPrereqReason = "Blocked because the required clean operation failed.";
                    } else {
                        failedPrereqReason = "Blocked because upstream prerequisite step failed: " + stepErrors.getOrDefault(prereqId, "Prerequisite failure");
                    }
                    break;
                }
            }

            if (dependencyFailed) {
                result.setBlocked(true);
                result.setSkipped(true);
                result.setPassed(false);
                result.setStatus(412); // Precondition Failed / Blocked by Dependency
                result.setExecutionStatus("BLOCKED");
                result.setBlockedReason(failedPrereqReason);
                result.setError(failedPrereqReason);
                result.setResponseSummary(failedPrereqReason);
                blockedCount++;
                stepSuccess.put(step.getStepId(), false);
                stepErrors.put(step.getStepId(), failedPrereqReason);
                results.add(result);
                continue;
            }

            // 2. Required Variable Availability Check
            boolean missingVariable = false;
            String missingVarName = null;

            for (String requiredVar : step.getRequiredVariables()) {
                String val = runtimeVariables.get(requiredVar);
                if (val == null || val.isBlank()) {
                    missingVariable = true;
                    missingVarName = requiredVar;
                    break;
                }
            }

            if (missingVariable) {
                String reason = "Required variable '" + missingVarName + "' was not produced by upload.";
                result.setBlocked(true);
                result.setSkipped(true);
                result.setPassed(false);
                result.setStatus(412);
                result.setExecutionStatus("BLOCKED");
                result.setBlockedReason(reason);
                result.setError("Blocked: " + reason);
                result.setResponseSummary("Could not execute step because required runtime variable '" + missingVarName + "' was not generated by previous steps.");
                blockedCount++;
                stepSuccess.put(step.getStepId(), false);
                stepErrors.put(step.getStepId(), reason);
                results.add(result);
                continue;
            }

            // 3. Safety Guardrail: State-Changing / Destructive Confirmation
            if (step.isDestructive() && !request.isApproveDestructiveOperations()) {
                result.setRequiresApproval(true);
                result.setBlocked(true);
                result.setPassed(false);
                result.setStatus(428); // Precondition Required / Awaiting Confirmation
                result.setExecutionStatus("REQUIRES_CONFIRMATION");
                result.setBlockedReason("Awaiting user confirmation before executing state-changing operation " + step.getMethod() + " " + step.getPath());
                result.setError("Operation paused: Destructive endpoint requires explicit human approval.");
                result.setResponseSummary("Awaiting user confirmation before executing " + step.getMethod() + " " + step.getPath());
                pendingApprovalCount++;
                stepSuccess.put(step.getStepId(), false);
                stepErrors.put(step.getStepId(), "Requires confirmation");
                results.add(result);
                continue;
            }

            // 4. Resolve Path Variables with Actual Runtime Values
            String resolvedPath = step.getPath();
            Matcher matcher = PATH_VAR_PATTERN.matcher(step.getPath());
            while (matcher.find()) {
                String varName = matcher.group(1);
                String val = runtimeVariables.get(varName);
                if (val != null) {
                    resolvedPath = resolvedPath.replace("{" + varName + "}", val);
                    result.getInputsUsed().put(varName, val);
                }
            }
            result.setResolvedPath(resolvedPath);

            // 5. Prepare ApiTestConsoleRequest
            ApiTestConsoleRequest consoleReq = new ApiTestConsoleRequest();
            consoleReq.setApiKeyId(apiKey.getId());
            consoleReq.setMethod(step.getMethod());
            consoleReq.setPath(resolvedPath);

            if (step.isMultipart()) {
                consoleReq.setBinaryBodyBase64(runtimeVariables.get("file_base64"));
                consoleReq.setFileName(runtimeVariables.get("file_name"));
                consoleReq.setFileContentType(runtimeVariables.get("file_content_type"));
                consoleReq.setFileFieldName(step.getMultipartFieldName() != null ? step.getMultipartFieldName() : "file");
                result.getInputsUsed().put("file", runtimeVariables.get("file_name"));
            } else if (step.getMethod().equals("POST") || step.getMethod().equals("PUT") || step.getMethod().equals("PATCH")) {
                if (step.getRequestBodyTemplate() != null) {
                    String body = step.getRequestBodyTemplate();
                    for (Map.Entry<String, String> entry : runtimeVariables.entrySet()) {
                        body = body.replace("{" + entry.getKey() + "}", entry.getValue());
                    }
                    consoleReq.setBody(body);
                } else {
                    consoleReq.setBody("{}");
                }
            }

            // Inject Bearer Token if available
            String authToken = runtimeVariables.get("token");
            if (authToken == null) authToken = runtimeVariables.get("accessToken");
            if (authToken == null) authToken = runtimeVariables.get("access_token");
            if (authToken != null && !authToken.isBlank() && !resolvedPath.contains("/auth/login")) {
                Map<String, String> headers = consoleReq.getHeaders() != null ? new HashMap<>(consoleReq.getHeaders()) : new HashMap<>();
                headers.put("Authorization", "Bearer " + authToken);
                consoleReq.setHeaders(headers);
                result.getInputsUsed().put("Authorization", "Bearer ••••••••");
            }

            // 6. Execute Live Request via Sentinel Gateway
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
                    result.setExecutionStatus("FAILED");
                    String errMsg = "HTTP " + statusCode;
                    if (testResult.getResponseBody() != null && !testResult.getResponseBody().isBlank()) {
                        String snippet = testResult.getResponseBody();
                        if (snippet.length() > 200) snippet = snippet.substring(0, 197) + "...";
                        errMsg += ": " + snippet;
                    }
                    result.setError(errMsg);
                    result.setResponseSummary(testResult.getResponseBody());
                    stepSuccess.put(step.getStepId(), false);
                    stepErrors.put(step.getStepId(), errMsg);
                } else {
                    passedCount++;
                    result.setExecutionStatus("PASSED");
                    result.setResponseSummary(testResult.getResponseBody());
                    stepSuccess.put(step.getStepId(), true);

                    // 7. Extract Actual Output Variables from Response
                    extractAndRememberOutputs(testResult.getResponseBody(), runtimeVariables, result);
                }
            } catch (Exception e) {
                log.warn("Step execution failed: {}", e.getMessage());
                result.setPassed(false);
                result.setStatus(500);
                result.setExecutionStatus("FAILED");
                result.setError(e.getMessage());
                failedCount++;
                stepSuccess.put(step.getStepId(), false);
                stepErrors.put(step.getStepId(), e.getMessage());
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
        report.setRememberedContext(runtimeVariables);

        if (failedCount == 0 && blockedCount == 0 && pendingApprovalCount == 0) {
            report.setOverallStatus("PASSED");
            report.setExecutiveSummary("All " + passedCount + " API tests passed successfully with average latency " + String.format("%.1f", report.getAvgLatencyMs()) + "ms.");
        } else if (pendingApprovalCount > 0 && failedCount == 0 && blockedCount == 0) {
            report.setOverallStatus("NEEDS_APPROVAL");
            report.setExecutiveSummary("Executed " + passedCount + " safe tests. " + pendingApprovalCount + " destructive action(s) require explicit user approval.");
        } else {
            report.setOverallStatus("PARTIAL");
            report.setExecutiveSummary("Test suite completed: " + passedCount + " passed, " + failedCount + " failed, " + blockedCount + " blocked steps.");
            report.setFailureAnalysis(generateFailureAnalysis(results));
        }

        // Update active session cache with last report
        String sessionKey = ownerId + "_" + applicationId;
        AiTestSessionDto session = activeSessions.computeIfAbsent(sessionKey, k -> new AiTestSessionDto());
        session.setSessionId("sess_" + UUID.randomUUID().toString().substring(0, 8));
        session.setApplicationId(applicationId);
        session.setApplicationName(app.getName());
        session.setStatus(report.getOverallStatus());
        session.setStatusMessage(report.getExecutiveSummary());
        session.setPlan(plan);
        session.setLastReport(report);
        session.setUpdatedAt(System.currentTimeMillis());

        return report;
    }

    /**
     * Retrieves or initializes a waitable AI Test Session for an application.
     */
    public AiTestSessionDto getOrCreateSession(Long ownerId, Long applicationId) {
        String sessionKey = ownerId + "_" + applicationId;
        return activeSessions.computeIfAbsent(sessionKey, k -> {
            AiTestPlanDto plan = generateTestPlan(ownerId, applicationId);
            AiTestSessionDto session = new AiTestSessionDto();
            session.setSessionId("sess_" + UUID.randomUUID().toString().substring(0, 8));
            session.setApplicationId(applicationId);
            session.setApplicationName(plan.getApplicationName());
            session.setPlan(plan);
            session.setMissingInputs(plan.getMissingInputs());
            session.setStatus(plan.getStatus());
            session.setStatusMessage(plan.getMissingInputs().isEmpty() ? "Ready to run test suite" : "Sentinel needs a few inputs before this test can continue.");
            return session;
        });
    }

    /**
     * Provides missing input (file, API key, parameter) to resume a waiting session.
     */
    public AiTestSessionDto provideSessionInput(Long ownerId, Long applicationId, String inputKey, String inputValue, String fileBase64, String fileName, String fileContentType) {
        String sessionKey = ownerId + "_" + applicationId;
        AiTestSessionDto session = getOrCreateSession(ownerId, applicationId);

        if (inputKey != null && inputValue != null) {
            session.getProvidedInputs().put(inputKey, inputValue);
        }

        if (fileBase64 != null && !fileBase64.isBlank()) {
            session.setFileBase64(fileBase64);
            session.setFileName(fileName != null ? fileName : "sentinel_test_image.png");
            session.setFileContentType(fileContentType != null ? fileContentType : "image/png");
            session.getProvidedInputs().put("file_base64", fileBase64);
            session.getProvidedInputs().put("file_name", session.getFileName());
        }

        // Re-analyze missing inputs
        Application app = applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        List<AiTestMissingInputDto> remainingMissing = discoverMissingInputs(app, session.getPlan().getSteps(), session.getProvidedInputs());
        session.setMissingInputs(remainingMissing);

        if (remainingMissing.isEmpty()) {
            session.setStatus("READY");
            session.setStatusMessage("All required inputs received. Ready to execute test plan.");
        } else {
            session.setStatus("WAITING_FOR_INPUT");
            session.setStatusMessage("Sentinel still requires " + remainingMissing.size() + " input(s) before this test can continue.");
        }
        session.setUpdatedAt(System.currentTimeMillis());
        return session;
    }

    /**
     * Explicitly cancels and clears an active test session.
     */
    public void cancelSession(Long ownerId, Long applicationId) {
        String sessionKey = ownerId + "_" + applicationId;
        activeSessions.remove(sessionKey);
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
                        fieldName.equalsIgnoreCase("imageId") ||
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
                        if (fieldName.equalsIgnoreCase("imageId")) {
                            rememberedContext.put("image_id", strVal);
                        }
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
                  .append(" (").append(step.getExecutionStatus() != null ? step.getExecutionStatus() : "FAILED").append(")")
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
        List<ApiEndpoint> existing = apiEndpointRepository.findByApplicationIdOrderByLastSeenAtDesc(app.getId());
        if (!existing.isEmpty()) {
            return existing;
        }
        list.add(new ApiEndpoint(app.getId(), "GET", "/"));
        list.add(new ApiEndpoint(app.getId(), "GET", "/health"));
        list.add(new ApiEndpoint(app.getId(), "GET", "/api/health"));
        list.add(new ApiEndpoint(app.getId(), "GET", "/actuator/health"));
        return list;
    }

    public BulkApiCheckResponse executeBulkApiCheck(Long ownerId, BulkApiCheckRequest request) {
        Application app = applicationRepository.findByIdAndOwnerId(request.getApplicationId(), ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        List<ApiEndpoint> allEndpoints = apiEndpointRepository.findByApplicationId(app.getId());
        if (allEndpoints.isEmpty()) {
            allEndpoints = generateFallbackEndpoints(app);
        }

        if (request.getEndpointIds() != null && !request.getEndpointIds().isEmpty()) {
            java.util.Set<Long> filterIds = new java.util.HashSet<>(request.getEndpointIds());
            allEndpoints = allEndpoints.stream()
                .filter(ep -> filterIds.contains(ep.getId()))
                .collect(java.util.stream.Collectors.toList());
        }

        int totalEndpoints = allEndpoints.size();
        int batchSize = Math.max(1, Math.min(request.getBatchSize(), 50));
        int totalBatches = totalEndpoints > 0 ? (int) Math.ceil((double) totalEndpoints / batchSize) : 1;
        int batchIndex = Math.max(0, Math.min(request.getBatchIndex(), Math.max(0, totalBatches - 1)));

        int fromIndex = batchIndex * batchSize;
        int toIndex = Math.min(fromIndex + batchSize, totalEndpoints);

        List<ApiEndpoint> batchEndpoints = fromIndex < totalEndpoints
            ? allEndpoints.subList(fromIndex, toIndex)
            : Collections.emptyList();

        ApiKey apiKey = null;
        try {
            apiKey = resolveApiKey(app.getId(), null);
        } catch (Exception e) {
            log.warn("No active API key for bulk check of app {}", app.getId());
        }

        List<BulkApiEndpointResultDto> results = new ArrayList<>();
        int validCount = 0;
        int warningCount = 0;
        int errorCount = 0;

        for (ApiEndpoint ep : batchEndpoints) {
            BulkApiEndpointResultDto res = new BulkApiEndpointResultDto();
            res.setEndpointId(ep.getId());
            res.setMethod(ep.getMethod());
            res.setPath(ep.getNormalizedPath());
            res.setHasRequestBody(ep.getMethod().equalsIgnoreCase("POST") || ep.getMethod().equalsIgnoreCase("PUT") || ep.getMethod().equalsIgnoreCase("PATCH"));

            boolean hasPathVars = ep.getNormalizedPath().contains("{") && ep.getNormalizedPath().contains("}");
            int pathVarCount = 0;
            if (hasPathVars) {
                Matcher matcher = PATH_VAR_PATTERN.matcher(ep.getNormalizedPath());
                while (matcher.find()) pathVarCount++;
            }
            res.setParametersCount(pathVarCount);

            if (hasPathVars) {
                res.setStatus("REQUIRES_INPUT");
                res.setResponseValidity("Specification Valid");
                res.setDetectedProblems("Requires dynamic path parameter(s) for live execution");
                res.setRecommendation("Provide path parameter values or execute via workflow with preceding creator step");
                warningCount++;
            } else if (apiKey != null && ep.getMethod().equalsIgnoreCase("GET")) {
                try {
                    ApiTestConsoleRequest testReq = new ApiTestConsoleRequest();
                    testReq.setApiKeyId(apiKey.getId());
                    testReq.setMethod("GET");
                    testReq.setPath(ep.getNormalizedPath());
                    ApiTestConsoleResultDto probeRes = apiTestConsoleService.executeTest(ownerId, app.getId(), testReq);

                    res.setStatusCode(probeRes.getStatusCode());
                    res.setLatencyMs(probeRes.getLatencyMs());
                    if (probeRes.getStatusCode() >= 200 && probeRes.getStatusCode() < 400) {
                        res.setStatus("VALID");
                        res.setResponseValidity("HTTP " + probeRes.getStatusCode() + " (" + probeRes.getLatencyMs() + "ms)");
                        res.setRecommendation("Endpoint verified reachable and responding normally");
                        validCount++;
                    } else if (probeRes.getStatusCode() == 401 || probeRes.getStatusCode() == 403) {
                        res.setStatus("WARNING");
                        res.setResponseValidity("Authentication Required");
                        res.setDetectedProblems("Upstream backend rejected request with HTTP " + probeRes.getStatusCode());
                        res.setRecommendation("Configure Upstream Authentication headers or token in Sentinel");
                        warningCount++;
                    } else if (probeRes.getStatusCode() == 404) {
                        res.setStatus("WARNING");
                        res.setResponseValidity("Route Not Found (404)");
                        res.setDetectedProblems("Backend returned 404 for this route");
                        res.setRecommendation("Verify route path in application backend");
                        warningCount++;
                    } else {
                        res.setStatus("ERROR");
                        res.setResponseValidity("HTTP " + probeRes.getStatusCode() + " Server Error");
                        res.setDetectedProblems("Upstream returned error status " + probeRes.getStatusCode());
                        res.setRecommendation("Inspect application backend error logs");
                        errorCount++;
                    }
                } catch (Exception ex) {
                    res.setStatus("ERROR");
                    res.setResponseValidity("Connection Error");
                    res.setDetectedProblems(ex.getMessage());
                    res.setRecommendation("Check backend connectivity and health");
                    errorCount++;
                }
            } else {
                res.setStatus("VALID");
                res.setResponseValidity("Documented Specification Ready");
                res.setRecommendation(res.isHasRequestBody() ? "Ready for payload testing" : "Ready for execution");
                validCount++;
            }

            results.add(res);
        }

        BulkApiCheckResponse response = new BulkApiCheckResponse();
        response.setApplicationId(app.getId());
        response.setBatchIndex(batchIndex);
        response.setBatchSize(batchSize);
        response.setTotalBatches(totalBatches);
        response.setTotalEndpoints(totalEndpoints);
        response.setCompletedCount(toIndex);
        response.setValidCount(validCount);
        response.setWarningCount(warningCount);
        response.setErrorCount(errorCount);
        response.setLastBatch(batchIndex >= totalBatches - 1);
        response.setResults(results);

        return response;
    }
}
