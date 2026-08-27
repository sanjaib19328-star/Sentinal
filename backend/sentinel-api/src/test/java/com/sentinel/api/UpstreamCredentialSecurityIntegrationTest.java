package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.UpstreamAuthConfigRequest;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.UpstreamAuthType;
import com.sentinel.api.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UpstreamCredentialSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    private String userTokenA;
    private String userTokenB;

    @BeforeEach
    void setUp() throws Exception {
        userTokenA = registerAndLogin("user-a-" + System.currentTimeMillis() + "@sentinel.io");
        userTokenB = registerAndLogin("user-b-" + System.currentTimeMillis() + "@sentinel.io");
    }

    private String registerAndLogin(String email) throws Exception {
        RegisterRequest req = new RegisterRequest("User", email, "Password123!");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void testSecretProtectionAndEncryptionAtRest() throws Exception {
        String secretToken = "super-secret-confidential-upstream-token-777";

        String createPayload = """
            {
                "name": "Secured App",
                "description": "Confidential",
                "baseUrl": "http://127.0.0.1:9090",
                "upstreamAuth": {
                    "type": "BEARER_TOKEN",
                    "enabled": true,
                    "secret": "super-secret-confidential-upstream-token-777"
                }
            }
            """;

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + userTokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = appResult.getResponse().getContentAsString();
        // 1. Raw secret is NEVER in response body
        assertFalse(responseBody.contains(secretToken), "Raw secret must never be exposed in API responses");
        assertTrue(responseBody.contains("••••••••"), "Masked representation must be returned");

        long appId = objectMapper.readTree(responseBody).get("id").asLong();

        // 2. Database stores encrypted ciphertext, never plaintext
        Application app = applicationRepository.findById(appId).orElseThrow();
        assertNotNull(app.getUpstreamAuthConfigEncrypted());
        assertFalse(app.getUpstreamAuthConfigEncrypted().contains(secretToken), "Database must store encrypted ciphertext, not plaintext");

        // 3. User B cannot modify or delete User A's upstream auth
        String maliciousPayload = """
            {
                "type": "BEARER_TOKEN",
                "enabled": true,
                "secret": "hacker-token"
            }
            """;

        mockMvc.perform(put("/api/v1/applications/" + appId + "/upstream-auth")
                .header("Authorization", "Bearer " + userTokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPayload))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/applications/" + appId + "/upstream-auth")
                .header("Authorization", "Bearer " + userTokenB))
            .andExpect(status().isNotFound());
    }
}
