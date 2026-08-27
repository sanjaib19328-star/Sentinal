package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.UpstreamAuthConfigRequest;
import com.sentinel.api.model.UpstreamAuthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UniversalOnboardingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "onboarding-user-" + System.currentTimeMillis() + "@sentinel.io";
        RegisterRequest registerRequest = new RegisterRequest("Onboarder", email, "Password123!");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        userToken = objectMapper.readTree(responseJson).get("token").asText();
    }

    @Test
    void testUniversalOnboardingLifecycleWithUpstreamAuth() throws Exception {
        // 1. Create Application with Bearer Upstream Auth
        String createPayload = """
            {
                "name": "Customer Microservice",
                "description": "Universal external REST API",
                "baseUrl": "http://127.0.0.1:9090",
                "upstreamAuth": {
                    "type": "BEARER_TOKEN",
                    "enabled": true,
                    "secret": "cust-secret-token"
                }
            }
            """;

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Customer Microservice"))
            .andExpect(jsonPath("$.upstreamAuth.type").value("BEARER_TOKEN"))
            .andExpect(jsonPath("$.upstreamAuth.configured").value(true))
            .andExpect(jsonPath("$.upstreamAuth.maskedSecret").value("••••••••"))
            .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Rotate / Update Upstream Auth to API_KEY_HEADER
        String updateAuthPayload = """
            {
                "type": "API_KEY_HEADER",
                "enabled": true,
                "headerName": "X-Customer-API-Key",
                "secret": "new-customer-api-key-999"
            }
            """;

        mockMvc.perform(put("/api/v1/applications/" + appId + "/upstream-auth")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateAuthPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("API_KEY_HEADER"))
            .andExpect(jsonPath("$.headerName").value("X-Customer-API-Key"))
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.maskedSecret").value("••••••••"));

        // 3. Disable Upstream Auth
        mockMvc.perform(delete("/api/v1/applications/" + appId + "/upstream-auth")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNoContent());

        // 4. Verify Application State shows auth disabled
        mockMvc.perform(get("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upstreamAuth.enabled").value(false));
    }
}
