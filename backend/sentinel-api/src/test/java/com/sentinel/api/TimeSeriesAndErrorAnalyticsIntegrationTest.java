package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApiKeyRepository;
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
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TimeSeriesAndErrorAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static HttpServer targetServer;
    private static int targetPort;

    @BeforeAll
    static void startServer() throws Exception {
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetServer.createContext("/ok", exchange -> {
            byte[] resp = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        targetServer.createContext("/error404", exchange -> {
            exchange.sendResponseHeaders(404, -1);
        });
        targetServer.createContext("/error500", exchange -> {
            exchange.sendResponseHeaders(500, -1);
        });
        targetServer.start();
        targetPort = targetServer.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (targetServer != null) {
            targetServer.stop(0);
        }
    }

    @BeforeEach
    void cleanDb() {
        requestLogRepository.deleteAll();
        apiEndpointRepository.deleteAll();
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest("Analytics User", email, "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, "password123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void analytics_timeseries_breakdown_errors_andGlobalDashboard() throws Exception {
        String token = getAuthToken("analytics_tester@example.com");

        CreateApplicationRequest appReq = new CreateApplicationRequest("Telemetry App", "Desc", "http://localhost:" + targetPort);
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Analytics Key", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // Send 2 OK requests to /ok
        mockMvc.perform(get("/api/v1/gateway/ok").header("X-Sentinel-API-Key", rawKey)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/gateway/ok").header("X-Sentinel-API-Key", rawKey)).andExpect(status().isOk());

        // Send 1 404 request to /error404
        mockMvc.perform(get("/api/v1/gateway/error404").header("X-Sentinel-API-Key", rawKey)).andExpect(status().isNotFound());

        // Send 1 500 request to /error500
        mockMvc.perform(get("/api/v1/gateway/error500").header("X-Sentinel-API-Key", rawKey)).andExpect(status().isInternalServerError());

        // 1. GET /api/v1/applications/{id}/analytics
        mockMvc.perform(get("/api/v1/applications/" + appId + "/analytics")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRequests").value(4))
            .andExpect(jsonPath("$.successCount").value(2))
            .andExpect(jsonPath("$.errorCount").value(2))
            .andExpect(jsonPath("$.status4xxCount").value(1))
            .andExpect(jsonPath("$.status5xxCount").value(1))
            .andExpect(jsonPath("$.p50LatencyMs").isNumber())
            .andExpect(jsonPath("$.p95LatencyMs").isNumber());

        // 2. GET /api/v1/applications/{id}/analytics/timeseries
        mockMvc.perform(get("/api/v1/applications/" + appId + "/analytics/timeseries?interval=minute")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(appId))
            .andExpect(jsonPath("$.interval").value("minute"))
            .andExpect(jsonPath("$.points", hasSize(greaterThanOrEqualTo(1))));

        // 3. GET /api/v1/applications/{id}/analytics/breakdown
        mockMvc.perform(get("/api/v1/applications/" + appId + "/analytics/breakdown")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.methodCounts.GET").value(4))
            .andExpect(jsonPath("$.statusClassCounts['2xx']").value(2))
            .andExpect(jsonPath("$.statusClassCounts['4xx']").value(1))
            .andExpect(jsonPath("$.statusClassCounts['5xx']").value(1))
            .andExpect(jsonPath("$.topApis", hasSize(greaterThanOrEqualTo(2))));

        // 4. GET /api/v1/applications/{id}/errors
        mockMvc.perform(get("/api/v1/applications/" + appId + "/errors")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalErrors").value(2))
            .andExpect(jsonPath("$.errorByStatusCode", hasSize(2)))
            .andExpect(jsonPath("$.errorLogs.content", hasSize(2)));

        // 5. GET /api/v1/dashboard/summary (Tenant Global Dashboard)
        mockMvc.perform(get("/api/v1/dashboard/summary")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalApplications").value(1))
            .andExpect(jsonPath("$.totalRequests").value(4))
            .andExpect(jsonPath("$.overallSuccessRate").value(50.0))
            .andExpect(jsonPath("$.overallErrorRate").value(50.0))
            .andExpect(jsonPath("$.recentErrors", hasSize(2)));
    }
}
