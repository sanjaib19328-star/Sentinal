package com.sentinel.api.service;

import com.sentinel.api.dto.AiTestMissingInputDto;
import com.sentinel.api.dto.AiTestPlanDto;
import com.sentinel.api.dto.AiTestRunReportDto;
import com.sentinel.api.dto.AiTestSessionDto;
import com.sentinel.api.dto.AiTestStepDto;
import com.sentinel.api.dto.AiTestStepResultDto;
import com.sentinel.api.dto.ApiTestConsoleRequest;
import com.sentinel.api.dto.ApiTestConsoleResultDto;
import com.sentinel.api.dto.RunAiTestRequest;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.Application;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiTestEngineServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiEndpointRepository apiEndpointRepository;

    @Mock
    private ApiTestConsoleService apiTestConsoleService;

    private AiTestEngineService aiTestEngineService;

    private Application testApp;
    private ApiKey testApiKey;

    @BeforeEach
    void setUp() {
        aiTestEngineService = new AiTestEngineService(
            applicationRepository,
            apiKeyRepository,
            apiEndpointRepository,
            apiTestConsoleService
        );

        testApp = new Application();
        testApp.setId(995L);
        testApp.setName("PixelVault-Clean");
        testApp.setBaseUrl("http://localhost:5000");

        testApiKey = new ApiKey();
        testApiKey.setId(10L);
        testApiKey.setApplicationId(995L);
        testApiKey.setActive(true);
        testApiKey.setKeyHash("test-key-hash");

        lenient().when(applicationRepository.findByIdAndOwnerId(eq(995L), any())).thenReturn(Optional.of(testApp));
        lenient().when(apiKeyRepository.findByApplicationId(eq(995L))).thenReturn(List.of(testApiKey));
    }

    private List<ApiEndpoint> createPixelVaultEndpoints() {
        List<ApiEndpoint> list = new ArrayList<>();
        list.add(new ApiEndpoint(995L, "POST", "/api/v1/images/{image_id}/clean"));
        list.add(new ApiEndpoint(995L, "GET", "/api/v1/images/{image_id}/download"));
        list.add(new ApiEndpoint(995L, "GET", "/"));
        list.add(new ApiEndpoint(995L, "POST", "/api/v1/images/{image_id}/analyze"));
        list.add(new ApiEndpoint(995L, "POST", "/api/v1/images/upload"));
        list.add(new ApiEndpoint(995L, "GET", "/api/v1/health"));
        list.add(new ApiEndpoint(995L, "GET", "/api/v1/images/{image_id}/report"));
        return list;
    }

    @Test
    void testPixelVaultCleanDependencyDagOrder() {
        when(apiEndpointRepository.findByApplicationId(995L)).thenReturn(createPixelVaultEndpoints());

        AiTestPlanDto plan = aiTestEngineService.generateTestPlan(1L, 995L);

        assertNotNull(plan);
        assertEquals(7, plan.getSteps().size());

        List<String> stepNames = new ArrayList<>();
        for (AiTestStepDto step : plan.getSteps()) {
            stepNames.add(step.getMethod() + " " + step.getPath());
        }

        // Expected topological order:
        // 1. GET /
        // 2. GET /api/v1/health
        // 3. POST /api/v1/images/upload
        // 4. POST /api/v1/images/{image_id}/analyze
        // 5. POST /api/v1/images/{image_id}/clean
        // 6. GET /api/v1/images/{image_id}/download
        // 7. GET /api/v1/images/{image_id}/report
        assertEquals("GET /", stepNames.get(0));
        assertEquals("GET /api/v1/health", stepNames.get(1));
        assertEquals("POST /api/v1/images/upload", stepNames.get(2));
        assertEquals("POST /api/v1/images/{image_id}/analyze", stepNames.get(3));
        assertEquals("POST /api/v1/images/{image_id}/clean", stepNames.get(4));
        assertEquals("GET /api/v1/images/{image_id}/download", stepNames.get(5));
        assertEquals("GET /api/v1/images/{image_id}/report", stepNames.get(6));
    }

    @Test
    void testVariablePropagationAndSuccessfulPipeline() {
        when(apiEndpointRepository.findByApplicationId(995L)).thenReturn(createPixelVaultEndpoints());

        // Mock gateway executions:
        // 1. GET / -> 200
        // 2. GET /api/v1/health -> 200
        // 3. POST /api/v1/images/upload -> 200 with {"image_id": "img_998877"}
        // 4. POST /api/v1/images/img_998877/analyze -> 200
        // 5. POST /api/v1/images/img_998877/clean -> 200
        // 6. GET /api/v1/images/img_998877/download -> 200
        // 7. GET /api/v1/images/img_998877/report -> 200
        when(apiTestConsoleService.executeTest(any(), eq(995L), any(ApiTestConsoleRequest.class)))
            .thenAnswer(invocation -> {
                ApiTestConsoleRequest req = invocation.getArgument(2);
                ApiTestConsoleResultDto res = new ApiTestConsoleResultDto();
                res.setStatusCode(200);
                res.setLatencyMs(45);
                res.setRequestId("req-test");

                if (req.getPath().equals("/api/v1/images/upload")) {
                    res.setResponseBody("{\"image_id\": \"img_998877\", \"status\": \"uploaded\"}");
                } else {
                    res.setResponseBody("{\"status\": \"ok\", \"path\": \"" + req.getPath() + "\"}");
                }
                return res;
            });

        RunAiTestRequest request = new RunAiTestRequest();
        request.setApplicationId(995L);
        request.setApproveDestructiveOperations(true); // Approve clean

        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(1L, request);

        assertNotNull(report);
        assertEquals("PASSED", report.getOverallStatus());
        assertEquals(7, report.getPassedSteps());
        assertEquals(0, report.getFailedSteps());
        assertEquals(0, report.getBlockedSteps());
        assertEquals("img_998877", report.getRememberedContext().get("image_id"));

        // Verify that path variables were resolved to actual image_id
        assertEquals("/api/v1/images/img_998877/analyze", report.getStepResults().get(3).getResolvedPath());
        assertEquals("/api/v1/images/img_998877/clean", report.getStepResults().get(4).getResolvedPath());
        assertEquals("/api/v1/images/img_998877/download", report.getStepResults().get(5).getResolvedPath());
        assertEquals("/api/v1/images/img_998877/report", report.getStepResults().get(6).getResolvedPath());
    }

    @Test
    void testCleanFailureBlocksDownloadButAllowsReport() {
        when(apiEndpointRepository.findByApplicationId(995L)).thenReturn(createPixelVaultEndpoints());

        when(apiTestConsoleService.executeTest(any(), eq(995L), any(ApiTestConsoleRequest.class)))
            .thenAnswer(invocation -> {
                ApiTestConsoleRequest req = invocation.getArgument(2);
                ApiTestConsoleResultDto res = new ApiTestConsoleResultDto();
                res.setLatencyMs(40);
                res.setRequestId("req-test");

                if (req.getPath().equals("/api/v1/images/upload")) {
                    res.setStatusCode(200);
                    res.setResponseBody("{\"image_id\": \"img_12345\"}");
                } else if (req.getPath().contains("clean")) {
                    res.setStatusCode(500);
                    res.setResponseBody("{\"error\": \"Cleaning filter failed\"}");
                } else {
                    res.setStatusCode(200);
                    res.setResponseBody("{\"status\": \"ok\"}");
                }
                return res;
            });

        RunAiTestRequest request = new RunAiTestRequest();
        request.setApplicationId(995L);
        request.setApproveDestructiveOperations(true);

        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(1L, request);

        assertNotNull(report);
        // Upload (PASSED), Analyze (PASSED), Clean (FAILED), Download (BLOCKED), Report (PASSED)
        assertEquals(5, report.getPassedSteps());
        assertEquals(1, report.getFailedSteps());
        assertEquals(1, report.getBlockedSteps());

        AiTestStepResultDto downloadResult = report.getStepResults().get(5);
        assertEquals("GET", downloadResult.getMethod());
        assertTrue(downloadResult.getEndpoint().contains("download"));
        assertEquals("BLOCKED", downloadResult.getExecutionStatus());
        assertTrue(downloadResult.isBlocked());
        assertTrue(downloadResult.isSkipped());
        assertEquals("Blocked because the required clean operation failed.", downloadResult.getBlockedReason());

        AiTestStepResultDto reportResult = report.getStepResults().get(6);
        assertEquals("PASSED", reportResult.getExecutionStatus());
        assertTrue(reportResult.isPassed());
    }

    @Test
    void testUploadFailureBlocksAllDependentSteps() {
        when(apiEndpointRepository.findByApplicationId(995L)).thenReturn(createPixelVaultEndpoints());

        when(apiTestConsoleService.executeTest(any(), eq(995L), any(ApiTestConsoleRequest.class)))
            .thenAnswer(invocation -> {
                ApiTestConsoleRequest req = invocation.getArgument(2);
                ApiTestConsoleResultDto res = new ApiTestConsoleResultDto();
                res.setLatencyMs(40);
                res.setRequestId("req-test");

                if (req.getPath().equals("/api/v1/images/upload")) {
                    res.setStatusCode(500);
                    res.setResponseBody("{\"error\": \"Storage unavailable\"}");
                } else {
                    res.setStatusCode(200);
                    res.setResponseBody("{\"status\": \"ok\"}");
                }
                return res;
            });

        RunAiTestRequest request = new RunAiTestRequest();
        request.setApplicationId(995L);
        request.setApproveDestructiveOperations(true);

        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(1L, request);

        assertNotNull(report);
        assertEquals(2, report.getPassedSteps()); // Root and Health
        assertEquals(1, report.getFailedSteps()); // Upload failed
        assertEquals(4, report.getBlockedSteps()); // Analyze, Clean, Download, Report blocked
    }

    @Test
    void testMissingInputDetectionAndSessionWorkflow() {
        when(apiEndpointRepository.findByApplicationId(995L)).thenReturn(createPixelVaultEndpoints());

        // Initial session creation without file
        AiTestSessionDto session = aiTestEngineService.getOrCreateSession(1L, 995L);
        assertNotNull(session);
        assertEquals("WAITING_FOR_INPUT", session.getStatus());
        assertFalse(session.getMissingInputs().isEmpty());

        // Provide missing file input
        AiTestSessionDto updatedSession = aiTestEngineService.provideSessionInput(
            1L, 995L, "file_base64", "dummyBase64Data", "dummyBase64Data", "test.png", "image/png"
        );
        assertNotNull(updatedSession);
        assertEquals("READY", updatedSession.getStatus());
        assertTrue(updatedSession.getMissingInputs().isEmpty());
    }
}
