package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.RequestLog;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.RequestLogRepository;
import com.sentinel.api.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewaySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        requestLogRepository.deleteAll();
        apiKeyRepository.deleteAll();
    }

    @Test
    void publicEndpoints_shouldBeAccessibleWithoutApiKey() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void createApiKeyEndpoint_shouldGenerateKeyWithoutApiKeyHeader() throws Exception {
        CreateApiKeyRequest request = new CreateApiKeyRequest("Test Client", 100, null);

        mockMvc.perform(post("/api/v1/keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.apiKey").value(org.hamcrest.Matchers.startsWith("sk_sentinel_")))
            .andExpect(jsonPath("$.rateLimitPerMinute").value(100))
            .andExpect(jsonPath("$.warning").isNotEmpty())
            .andExpect(jsonPath("$.keyHash").doesNotExist());
    }

    @Test
    void gatewayEndpoint_whenMissingApiKeyHeader_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/gateway/test"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("Valid Sentinel API key required"));
    }

    @Test
    void gatewayEndpoint_whenInvalidApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", "sk_sentinel_invalid_random_key"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("Valid Sentinel API key required"));
    }

    @Test
    void gatewayEndpoint_whenInactiveApiKey_shouldReturn401() throws Exception {
        String rawKey = apiKeyService.generateRawApiKey();
        ApiKey apiKey = new ApiKey("Inactive Client", apiKeyService.hashKey(rawKey), 60, null);
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);

        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void gatewayEndpoint_whenExpiredApiKey_shouldReturn401() throws Exception {
        String rawKey = apiKeyService.generateRawApiKey();
        ApiKey apiKey = new ApiKey("Expired Client", apiKeyService.hashKey(rawKey), 60, Instant.now().minus(2, ChronoUnit.DAYS));
        apiKey.setActive(true);
        apiKeyRepository.save(apiKey);

        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void gatewayEndpoint_whenValidApiKey_shouldReturn200AndRecordLog() throws Exception {
        var keyResponse = apiKeyService.createApiKey(new CreateApiKeyRequest("Valid Client", 100, null));
        String rawKey = keyResponse.getApiKey();

        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-Id"))
            .andExpect(header().string("X-RateLimit-Limit", "100"))
            .andExpect(header().exists("X-RateLimit-Remaining"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Sentinel gateway request accepted"))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andExpect(jsonPath("$.apiKeyId").value(keyResponse.getId()));

        List<RequestLog> logs = requestLogRepository.findByApiKeyId(keyResponse.getId());
        assertThat(logs).hasSize(1);
        RequestLog log = logs.get(0);
        assertThat(log.getStatusCode()).isEqualTo(200);
        assertThat(log.getMethod()).isEqualTo("POST");
        assertThat(log.getPath()).isEqualTo("/api/v1/gateway/test");
        assertThat(log.getLatencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void gatewayEndpoint_whenRateLimitExceeded_shouldReturn429AndRecordLog() throws Exception {
        // Create key with rate limit of 2 per minute
        var keyResponse = apiKeyService.createApiKey(new CreateApiKeyRequest("Rate Limited Client", 2, null));
        String rawKey = keyResponse.getApiKey();

        // 1st request -> 200
        mockMvc.perform(post("/api/v1/gateway/test").header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk());

        // 2nd request -> 200
        mockMvc.perform(post("/api/v1/gateway/test").header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk());

        // 3rd request -> 429
        mockMvc.perform(post("/api/v1/gateway/test").header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.message").value("Rate limit exceeded"));

        List<RequestLog> logs = requestLogRepository.findByApiKeyId(keyResponse.getId());
        assertThat(logs).hasSize(3);
        assertThat(logs.get(2).getStatusCode()).isEqualTo(429);
    }
}
