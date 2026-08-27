package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.UpstreamAuthType;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.security.CredentialEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpstreamAuthGatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CredentialEncryptionService encryptionService;

    private String userToken;
    private Long appId;
    private String consumerApiKey;

    @BeforeEach
    void setUp() throws Exception {
        String email = "gateway-auth-" + System.currentTimeMillis() + "@sentinel.io";
        RegisterRequest req = new RegisterRequest("Gateway Auth User", email, "Password123!");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk())
            .andReturn();

        userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        // Create App
        String createPayload = """
            {
                "name": "Upstream Auth App",
                "description": "Gateway test",
                "baseUrl": "http://127.0.0.1:9090",
                "upstreamAuth": {
                    "type": "BEARER_TOKEN",
                    "enabled": true,
                    "secret": "valid-upstream-bearer-token"
                }
            }
            """;

        MvcResult appRes = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

        appId = objectMapper.readTree(appRes.getResponse().getContentAsString()).get("id").asLong();

        // Create Consumer API Key
        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Consumer Key", 500);
        MvcResult keyRes = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();

        consumerApiKey = objectMapper.readTree(keyRes.getResponse().getContentAsString()).get("apiKey").asText();
        assertNotNull(consumerApiKey);
    }

    @Test
    void testConsumerKeyAuthenticatesAndUpstreamAuthIsConfigured() throws Exception {
        // Verify key works against backward-compatible test endpoint
        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", consumerApiKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify application entity has encrypted bearer token
        Application app = applicationRepository.findById(appId).orElseThrow();
        assertEquals(UpstreamAuthType.BEARER_TOKEN, app.getUpstreamAuthType());
        assertTrue(app.isUpstreamAuthEnabled());
        assertNotNull(app.getUpstreamAuthConfigEncrypted());
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
