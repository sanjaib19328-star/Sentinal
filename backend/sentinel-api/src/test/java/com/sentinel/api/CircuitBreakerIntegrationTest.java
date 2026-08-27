package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.SavePolicyRequest;
import com.sentinel.api.model.User;
import com.sentinel.api.repository.UserRepository;
import com.sentinel.api.security.JwtService;
import com.sentinel.api.service.CircuitBreakerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CircuitBreakerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String jwtToken;
    private Long appId;
    private String rawApiKey;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        testUser = new User("Circuit User", "circuit-breaker@sentinel.com", passwordEncoder.encode("Pass123!"));
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateToken(testUser);

        // Target pointing to an unreachable port
        CreateApplicationRequest appReq = new CreateApplicationRequest("Unreachable App", "Down service", "http://127.0.0.1:49999");
        String res = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        appId = objectMapper.readTree(res).get("id").asLong();

        // Configure policy with failure threshold = 2, recovery timeout = 10s
        SavePolicyRequest policyReq = new SavePolicyRequest();
        policyReq.setEnabled(true);
        policyReq.setRateLimit(100);
        policyReq.setCircuitBreakerEnabled(true);
        policyReq.setCircuitFailureThreshold(2);
        policyReq.setCircuitRecoveryTimeoutSeconds(10);

        mockMvc.perform(put("/api/v1/applications/" + appId + "/policy")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(policyReq)))
            .andExpect(status().isOk());

        // Create API key
        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("CB Key", 100);
        String keyRes = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        rawApiKey = objectMapper.readTree(keyRes).get("apiKey").asText();
        circuitBreakerService.reset(appId);
    }

    @Test
    void testCircuitBreakerTripsToOpenAfterConsecutiveFailures() throws Exception {
        // Attempt 1: Target unreachable -> returns 502 Bad Gateway
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawApiKey))
            .andExpect(status().isBadGateway());

        // Attempt 2: Target unreachable -> reaches threshold of 2 failures -> trips circuit
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawApiKey))
            .andExpect(status().isBadGateway());

        // Attempt 3: Circuit is now OPEN -> should fast-fail with 503 SERVICE_UNAVAILABLE and error CIRCUIT_BREAKER_OPEN
        mockMvc.perform(get("/api/v1/gateway/items")
                .header("X-Sentinel-API-Key", rawApiKey))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error", is("CIRCUIT_BREAKER_OPEN")));

        // Verify status endpoint
        mockMvc.perform(get("/api/v1/applications/" + appId + "/circuit-breaker")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state", is("OPEN")))
            .andExpect(jsonPath("$.consecutiveFailures", is(2)))
            .andExpect(jsonPath("$.enabled", is(true)));
    }
}
