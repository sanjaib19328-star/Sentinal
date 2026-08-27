package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.SavePolicyRequest;
import com.sentinel.api.model.ApiEndpoint;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApiPolicyRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiPolicyRepository apiPolicyRepository;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static HttpServer targetServer;
    private static int targetPort;

    @BeforeAll
    static void startTargetServer() throws Exception {
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetServer.createContext("/items", exchange -> {
            byte[] resp = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        targetServer.start();
        targetPort = targetServer.getAddress().getPort();
    }

    @AfterAll
    static void stopTargetServer() {
        if (targetServer != null) {
            targetServer.stop(0);
        }
    }

    @BeforeEach
    void cleanDb() {
        requestLogRepository.deleteAll();
        apiPolicyRepository.deleteAll();
        apiEndpointRepository.deleteAll();
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest("Rate Limit User", email, "password123");
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
    void multiLevelRateLimiting_endpointPolicyMoreRestrictiveThanAppOrKey() throws Exception {
        String token = getAuthToken("multilevel@example.com");

        // 1. Create App
        CreateApplicationRequest appReq = new CreateApplicationRequest("MultiLevel App", "Desc", "http://localhost:" + targetPort);
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Create Key with high limit (100 req/min)
        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Generous Key", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // 3. Send 1 request to discover GET /items
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-Id"))
            .andExpect(header().exists("X-RateLimit-Limit"));

        ApiEndpoint endpoint = apiEndpointRepository.findByApplicationIdOrderByLastSeenAtDesc(appId).get(0);

        // 4. Attach a restrictive Endpoint Policy: rateLimit = 2 requests / 60 seconds
        SavePolicyRequest epPolicyReq = new SavePolicyRequest(true, 2, 60, null, null, 5000, null, null, null);
        mockMvc.perform(put("/api/v1/applications/" + appId + "/apis/" + endpoint.getId() + "/policy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(epPolicyReq)))
            .andExpect(status().isOk());

        // 5. Send request 1 against endpoint policy (allowed, remaining=1)
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Remaining", "1"));

        // 6. Send request 2 against endpoint policy (allowed, remaining=0)
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Remaining", "0"));

        // 7. Send request 3 against endpoint policy (exceeded -> 429 Too Many Requests)
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(header().string("X-RateLimit-Remaining", "0"))
            .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.message", containsString("ENDPOINT_POLICY")));
    }

    @Test
    void applicationQuota_whenExhausted_shouldReturn429() throws Exception {
        String token = getAuthToken("quota_test@example.com");

        CreateApplicationRequest appReq = new CreateApplicationRequest("Quota App", "Desc", "http://localhost:" + targetPort);
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Key", 50, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // Configure App Policy with small quota = 2 requests total
        SavePolicyRequest appPolicyReq = new SavePolicyRequest(true, 50, 60, 2, 86400, 5000, null, null, null);
        mockMvc.perform(put("/api/v1/applications/" + appId + "/policy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appPolicyReq)))
            .andExpect(status().isOk());

        // Req 1 (allowed)
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk());

        // Req 2 (allowed)
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk());

        // Req 3 (quota exceeded -> 429)
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message", containsString("APPLICATION_QUOTA")));
    }
}
