package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApiEndpointRepository;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayForwardingIntegrationTest {

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
    private ApplicationMetricRepository applicationMetricRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static HttpServer targetServer;
    private static int targetPort;

    @BeforeAll
    static void startMockTargetServer() throws Exception {
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);

        // GET /users
        targetServer.createContext("/users", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/users") && "GET".equalsIgnoreCase(method)) {
                byte[] resp = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            } else if (path.matches("^/users/[0-9]+$") && "GET".equalsIgnoreCase(method)) {
                String id = path.substring("/users/".length());
                byte[] resp = ("{\"id\":" + id + ",\"name\":\"User " + id + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            } else if (path.matches("^/users/[0-9]+$") && "PUT".equalsIgnoreCase(method)) {
                try (InputStream is = exchange.getRequestBody()) {
                    byte[] body = is.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                }
            } else if (path.matches("^/users/[0-9]+$") && "DELETE".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });

        // POST /payments
        targetServer.createContext("/payments", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (InputStream is = exchange.getRequestBody()) {
                    byte[] body = is.readAllBytes();
                    String reqBody = new String(body, StandardCharsets.UTF_8);
                    byte[] resp = "{\"paymentId\":\"pay_12345\",\"status\":\"COMPLETED\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(201, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        targetServer.start();
        targetPort = targetServer.getAddress().getPort();
    }

    @AfterAll
    static void stopMockTargetServer() {
        if (targetServer != null) {
            targetServer.stop(0);
        }
    }

    @BeforeEach
    void cleanDb() {
        requestLogRepository.deleteAll();
        apiEndpointRepository.deleteAll();
        applicationMetricRepository.deleteAll();
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest("Test User", email, "password123");
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
    void realGatewayForwarding_andAutomaticApiDiscovery() throws Exception {
        String token = getAuthToken("gateway_tester@example.com");

        // 1. Create Application with base URL pointing to target server
        CreateApplicationRequest appReq = new CreateApplicationRequest(
            "Test Target App",
            "Target application for real gateway forwarding",
            "http://localhost:" + targetPort
        );

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Create Scoped API Key
        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Gateway Test Key", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();

        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // 3. Send GET /users
        mockMvc.perform(get("/api/v1/gateway/users")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-Id"))
            .andExpect(header().string("X-RateLimit-Limit", "100"))
            .andExpect(jsonPath("$[0].name").value("Alice"));

        // 4. Send GET /users/123 & GET /users/456 (dynamic path normalization test)
        mockMvc.perform(get("/api/v1/gateway/users/123")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(123))
            .andExpect(jsonPath("$.name").value("User 123"));

        mockMvc.perform(get("/api/v1/gateway/users/456")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(456))
            .andExpect(jsonPath("$.name").value("User 456"));

        // 5. Send POST /payments
        mockMvc.perform(post("/api/v1/gateway/payments")
                .header("X-Sentinel-API-Key", rawKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 150.00, \"currency\": \"USD\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value("pay_12345"))
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 6. Send PUT /users/123
        mockMvc.perform(put("/api/v1/gateway/users/123")
                .header("X-Sentinel-API-Key", rawKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Updated Bob\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Bob"));

        // 7. Send DELETE /users/123
        mockMvc.perform(delete("/api/v1/gateway/users/123")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isNoContent());

        // 8. Verify Discovered Endpoints in Database
        List<ApiEndpoint> discovered = apiEndpointRepository.findByApplicationIdOrderByLastSeenAtDesc(appId);
        // Should have discovered 4 distinct endpoint templates: GET /users, GET /users/{id}, POST /payments, PUT /users/{id}, DELETE /users/{id}
        assertThat(discovered).hasSize(5);

        // Verify that GET /users/{id} was consolidated rather than separate entries for 123 and 456
        long userDetailsEndpoints = discovered.stream()
            .filter(e -> "GET".equals(e.getMethod()) && "/users/{id}".equals(e.getNormalizedPath()))
            .count();
        assertThat(userDetailsEndpoints).isEqualTo(1);

        // 9. Verify API Catalog endpoint GET /api/v1/applications/{id}/apis
        mockMvc.perform(get("/api/v1/applications/" + appId + "/apis")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)));

        // 10. Verify Per-API Analytics for GET /users/{id}
        ApiEndpoint userDetailsEp = discovered.stream()
            .filter(e -> "GET".equals(e.getMethod()) && "/users/{id}".equals(e.getNormalizedPath()))
            .findFirst()
            .orElseThrow();

        mockMvc.perform(get("/api/v1/applications/" + appId + "/apis/" + userDetailsEp.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endpointId").value(userDetailsEp.getId()))
            .andExpect(jsonPath("$.method").value("GET"))
            .andExpect(jsonPath("$.normalizedPath").value("/users/{id}"))
            .andExpect(jsonPath("$.totalRequests").value(2))
            .andExpect(jsonPath("$.successCount").value(2))
            .andExpect(jsonPath("$.errorCount").value(0))
            .andExpect(jsonPath("$.successRate").value(100.0))
            .andExpect(jsonPath("$.p50LatencyMs").isNumber())
            .andExpect(jsonPath("$.recentRequests", hasSize(2)));

        // 11. Verify Per-API Request Logs
        mockMvc.perform(get("/api/v1/applications/" + appId + "/apis/" + userDetailsEp.getId() + "/requests")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].method").value("GET"));
    }

    @Test
    void gateway_whenTargetUnavailable_shouldReturn502AndKeepSentinelAlive() throws Exception {
        String token = getAuthToken("fail_safe_tester@example.com");

        // Target URL points to dead port
        CreateApplicationRequest appReq = new CreateApplicationRequest(
            "Dead Target App",
            "Target on closed port",
            "http://127.0.0.1:59997"
        );

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Dead App Key", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();

        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // Send gateway request to dead target -> 502 Bad Gateway
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error").value("BAD_GATEWAY"))
            .andExpect(jsonPath("$.message", containsString("Target application unreachable")));

        // Verify request was logged with 502 status
        List<RequestLog> logs = requestLogRepository.findAllByApplicationIdOrderByTimestampDesc(appId);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getStatusCode()).isEqualTo(502);

        // Sentinel's own health check must be completely UP
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
