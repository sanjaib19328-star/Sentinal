package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.HealthStatus;
import com.sentinel.api.model.MetricType;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationMetricRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import com.sentinel.api.repository.UserRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ObservationAndMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ApplicationMetricRepository applicationMetricRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static HttpServer testTargetServer;
    private static int targetPort;

    @BeforeAll
    static void startTargetServer() throws Exception {
        testTargetServer = HttpServer.create(new InetSocketAddress(0), 0);
        testTargetServer.createContext("/health", exchange -> {
            byte[] response = "{\"status\":\"UP\"}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        });
        testTargetServer.start();
        targetPort = testTargetServer.getAddress().getPort();
    }

    @AfterAll
    static void stopTargetServer() {
        if (testTargetServer != null) {
            testTargetServer.stop(0);
        }
    }

    @BeforeEach
    void cleanDb() {
        requestLogRepository.deleteAll();
        applicationMetricRepository.deleteAll();
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String name, String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest(name, email, password);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void newApplication_shouldHaveUnknownHealthAndEmptyMetrics_strictlyNoFakeData() throws Exception {
        String token = getAuthToken("Owner Test", "owner_nofake@example.com", "pass1234");

        CreateApplicationRequest request = new CreateApplicationRequest(
            "CleanApp",
            "No fake data test app",
            "http://localhost:" + targetPort + "/health"
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.healthStatus").value("UNKNOWN"))
            .andReturn();

        long appId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // 1. Check status is honestly UNKNOWN before any observation
        mockMvc.perform(get("/api/v1/applications/" + appId + "/status")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNKNOWN"));

        // 2. Check metrics is honestly empty
        mockMvc.perform(get("/api/v1/applications/" + appId + "/metrics")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(appId))
            .andExpect(jsonPath("$.metrics", empty()));

        // 3. Check requests is honestly empty
        mockMvc.perform(get("/api/v1/applications/" + appId + "/requests")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", empty()))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void connectionTest_againstReachableTarget_shouldMeasureRealLatencyAndReturnHealthy() throws Exception {
        String token = getAuthToken("Owner Reachable", "owner_reachable@example.com", "pass1234");

        CreateApplicationRequest request = new CreateApplicationRequest(
            "ReachableApp",
            "Observing live target",
            "http://localhost:" + targetPort + "/health"
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Perform real connection test
        mockMvc.perform(post("/api/v1/applications/" + appId + "/connection-test")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(appId))
            .andExpect(jsonPath("$.reachable").value(true))
            .andExpect(jsonPath("$.status").value("HEALTHY"))
            .andExpect(jsonPath("$.latencyMs").isNumber());

        // Verify status is now updated to HEALTHY
        mockMvc.perform(get("/api/v1/applications/" + appId + "/status")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("HEALTHY"))
            .andExpect(jsonPath("$.lastSeenAt").isNotEmpty());

        // Verify connection test causes ZERO telemetry pollution (empty metrics)
        mockMvc.perform(get("/api/v1/applications/" + appId + "/metrics")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metrics", empty()));
    }

    @Test
    void connectionTest_againstUnreachableTarget_shouldReturnUnavailableSafely() throws Exception {
        String token = getAuthToken("Owner Unreachable", "owner_unreach@example.com", "pass1234");

        // Target port 59998 has nothing listening
        CreateApplicationRequest request = new CreateApplicationRequest(
            "DeadApp",
            "Observing unreachable target",
            "http://127.0.0.1:59998/nonexistent"
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Perform connection test against dead endpoint - must not crash Sentinel
        mockMvc.perform(post("/api/v1/applications/" + appId + "/connection-test")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(appId))
            .andExpect(jsonPath("$.reachable").value(false))
            .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.latencyMs").doesNotExist());

        // Verify status is now UNAVAILABLE
        mockMvc.perform(get("/api/v1/applications/" + appId + "/status")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNAVAILABLE"));
    }

    @Test
    void realGatewayTraffic_shouldProduceRealLogsAndRealCalculatedMetrics() throws Exception {
        String token = getAuthToken("Owner Traffic", "owner_traffic@example.com", "pass1234");

        // Create application
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("TrafficApp", "Desc", "http://traffic.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // Create API Key for Application
        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("TrafficKey", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();

        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();
        long keyId = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("id").asLong();

        // Send 3 real gateway requests with this application key
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/gateway/test")
                    .header("X-Sentinel-Api-Key", rawKey))
                .andExpect(status().isOk());
        }

        // Add 1 error request log manually associated with key
        requestLogRepository.save(new RequestLog(
            "err-req-123",
            keyId,
            "POST",
            "/api/v1/gateway/test",
            500,
            120L,
            "127.0.0.1"
        ));

        // 1. Verify requests endpoint returns all 4 real logs with pagination
        mockMvc.perform(get("/api/v1/applications/" + appId + "/requests?page=0&size=10")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(4))
            .andExpect(jsonPath("$.content", hasSize(4)))
            .andExpect(jsonPath("$.content[0].requestId").isNotEmpty())
            .andExpect(jsonPath("$.content[0].method").value("POST"))
            .andExpect(jsonPath("$.content[0].latencyMs").isNumber());

        // 2. Verify metrics endpoint computes real metrics from actual records
        mockMvc.perform(get("/api/v1/applications/" + appId + "/metrics")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(appId))
            .andExpect(jsonPath("$.metrics", hasSize(4)))
            .andExpect(jsonPath("$.metrics[0].type").value("REQUEST_COUNT"))
            .andExpect(jsonPath("$.metrics[0].value").value(4.0))
            .andExpect(jsonPath("$.metrics[1].type").value("SUCCESS_COUNT"))
            .andExpect(jsonPath("$.metrics[1].value").value(3.0))
            .andExpect(jsonPath("$.metrics[2].type").value("ERROR_COUNT"))
            .andExpect(jsonPath("$.metrics[2].value").value(1.0))
            .andExpect(jsonPath("$.metrics[3].type").value("AVG_LATENCY"))
            .andExpect(jsonPath("$.metrics[3].value").isNumber());
    }

    @Test
    void crossUserIsolation_onPhase4ObservationEndpoints() throws Exception {
        String tokenUserA = getAuthToken("User A", "usera_phase4@example.com", "pass1234");
        String tokenUserB = getAuthToken("User B", "userb_phase4@example.com", "pass1234");

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("Private App", "Desc", "http://private.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // User B connection-test -> 404
        mockMvc.perform(post("/api/v1/applications/" + appId + "/connection-test")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        // User B requests -> 404
        mockMvc.perform(get("/api/v1/applications/" + appId + "/requests")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        // User B metrics -> 404
        mockMvc.perform(get("/api/v1/applications/" + appId + "/metrics")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        // User B status -> 404
        mockMvc.perform(get("/api/v1/applications/" + appId + "/status")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());
    }
}
