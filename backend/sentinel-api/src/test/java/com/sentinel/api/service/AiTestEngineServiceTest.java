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
        request.setFileBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        request.setFileName("test_upload.png");
        request.setFileContentType("image/png");

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
        request.setFileBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        request.setFileName("test_upload.png");
        request.setFileContentType("image/png");

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
        request.setFileBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        request.setFileName("test_upload.png");
        request.setFileContentType("image/png");

        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(1L, request);

        assertNotNull(report);
        assertEquals(2, report.getPassedSteps()); // Root and Health
        assertEquals(1, report.getFailedSteps()); // Upload failed
        assertEquals(4, report.getBlockedSteps()); // Analyze, Clean, Download, Report blocked
    }

    @Test
    void testMultipartStepWithoutFileIsBlockedWithoutFallback() {
        when(apiEndpointRepository.findByApplicationId(995L)).thenReturn(createPixelVaultEndpoints());

        // Do NOT provide fileBase64 in request
        RunAiTestRequest request = new RunAiTestRequest();
        request.setApplicationId(995L);
        request.setApproveDestructiveOperations(true);

        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(1L, request);

        assertNotNull(report);
        // Step 2 is POST /api/v1/images/upload (multipart) -> must be BLOCKED because no image was provided
        AiTestStepResultDto uploadResult = report.getStepResults().get(2);
        assertTrue(uploadResult.isBlocked(), "Multipart step must be blocked when no image is provided");
        assertEquals("BLOCKED", uploadResult.getExecutionStatus());
        assertEquals(422, uploadResult.getStatus());
        assertTrue(uploadResult.getBlockedReason().contains("requires an uploaded image/file"));
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

    @Test
    void testMultipartUploadStepCreatesMultipartRequestWithDecodedBytes() {
        when(apiEndpointRepository.findByApplicationId(995L)).thenReturn(createPixelVaultEndpoints());

        List<ApiTestConsoleRequest> capturedRequests = new ArrayList<>();
        when(apiTestConsoleService.executeTest(any(), eq(995L), any(ApiTestConsoleRequest.class)))
            .thenAnswer(invocation -> {
                ApiTestConsoleRequest req = invocation.getArgument(2);
                capturedRequests.add(req);
                ApiTestConsoleResultDto res = new ApiTestConsoleResultDto();
                res.setStatusCode(200);
                res.setLatencyMs(35);
                res.setRequestId("req-mp-test");
                if (req.getPath().equals("/api/v1/images/upload")) {
                    res.setResponseBody("{\"image_id\": \"img_vault_123\"}");
                } else {
                    res.setResponseBody("{\"status\": \"ok\"}");
                }
                return res;
            });

        // Valid 1x1 PNG base64
        String rawBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String dataUrlBase64 = "data:image/png;base64," + rawBase64;

        RunAiTestRequest request = new RunAiTestRequest();
        request.setApplicationId(995L);
        request.setApproveDestructiveOperations(true);
        request.setFileBase64(dataUrlBase64);
        request.setFileName("forensic_evidence.png");
        request.setFileContentType("image/png");

        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(1L, request);

        assertNotNull(report);
        assertEquals("PASSED", report.getOverallStatus());

        // Find the captured request for /api/v1/images/upload
        ApiTestConsoleRequest uploadReq = capturedRequests.stream()
            .filter(r -> r.getPath().equals("/api/v1/images/upload"))
            .findFirst()
            .orElse(null);

        assertNotNull(uploadReq, "Multipart upload request must be dispatched");
        assertEquals("POST", uploadReq.getMethod());
        assertEquals("file", uploadReq.getFileFieldName());
        assertEquals("forensic_evidence.png", uploadReq.getFileName());
        assertEquals("image/png", uploadReq.getFileContentType());
        assertEquals(dataUrlBase64, uploadReq.getBinaryBodyBase64());

        // Validate multipart binary body construction with decoded bytes
        byte[] decodedBytes = java.util.Base64.getDecoder().decode(rawBase64);
        String boundary = "----TestBoundary123456";
        byte[] multipartPayload = ApiTestConsoleService.buildMultipartFormData(
            decodedBytes,
            uploadReq.getFileFieldName(),
            uploadReq.getFileName(),
            uploadReq.getFileContentType(),
            boundary
        );

        String multipartString = new String(multipartPayload, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(multipartString.contains("Content-Disposition: form-data; name=\"file\"; filename=\"forensic_evidence.png\""));
        assertTrue(multipartString.contains("Content-Type: image/png"));
        assertTrue(multipartString.contains(boundary));
        assertTrue(multipartPayload.length > decodedBytes.length);

        // Verify non-multipart step 0 (GET /) and step 1 (GET /api/v1/health) remained clean
        ApiTestConsoleRequest rootReq = capturedRequests.stream()
            .filter(r -> r.getPath().equals("/"))
            .findFirst()
            .orElse(null);
        assertNotNull(rootReq);
        assertEquals("GET", rootReq.getMethod());
        assertTrue(rootReq.getBinaryBodyBase64() == null || rootReq.getBinaryBodyBase64().isBlank());

        ApiTestConsoleRequest healthReq = capturedRequests.stream()
            .filter(r -> r.getPath().equals("/api/v1/health"))
            .findFirst()
            .orElse(null);
        assertNotNull(healthReq);
        assertEquals("GET", healthReq.getMethod());
        assertTrue(healthReq.getBinaryBodyBase64() == null || healthReq.getBinaryBodyBase64().isBlank());
    }

    @Test
    void testMultipartFallbackFromActiveSessionWhenRequestFileBase64Empty() {
        when(apiEndpointRepository.findByApplicationId(995L)).thenReturn(createPixelVaultEndpoints());

        // Seed session with image
        aiTestEngineService.provideSessionInput(
            1L, 995L, "file_base64", "c2Vzc2lvbkJhc2U2NA==", "c2Vzc2lvbkJhc2U2NA==", "session_photo.png", "image/png"
        );

        List<ApiTestConsoleRequest> capturedRequests = new ArrayList<>();
        when(apiTestConsoleService.executeTest(any(), eq(995L), any(ApiTestConsoleRequest.class)))
            .thenAnswer(invocation -> {
                ApiTestConsoleRequest req = invocation.getArgument(2);
                capturedRequests.add(req);
                ApiTestConsoleResultDto res = new ApiTestConsoleResultDto();
                res.setStatusCode(200);
                res.setLatencyMs(25);
                res.setRequestId("req-session-test");
                if (req.getPath().equals("/api/v1/images/upload")) {
                    res.setResponseBody("{\"image_id\": \"img_from_session\"}");
                } else {
                    res.setResponseBody("{\"status\": \"ok\"}");
                }
                return res;
            });

        // Request with null file fields
        RunAiTestRequest emptyReq = new RunAiTestRequest();
        emptyReq.setApplicationId(995L);
        emptyReq.setApproveDestructiveOperations(true);

        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(1L, emptyReq);

        assertNotNull(report);
        assertEquals("PASSED", report.getOverallStatus());

        ApiTestConsoleRequest uploadReq = capturedRequests.stream()
            .filter(r -> r.getPath().equals("/api/v1/images/upload"))
            .findFirst()
            .orElse(null);

        assertNotNull(uploadReq);
        assertEquals("c2Vzc2lvbkJhc2U2NA==", uploadReq.getBinaryBodyBase64());
        assertEquals("session_photo.png", uploadReq.getFileName());
    }
}

