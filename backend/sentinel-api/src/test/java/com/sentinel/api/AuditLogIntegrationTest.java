package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.SavePolicyRequest;
import com.sentinel.api.dto.UpdateApiKeyRequest;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApiPolicyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.AuditLogRepository;
import com.sentinel.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogIntegrationTest {

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        auditLogRepository.deleteAll();
        apiPolicyRepository.deleteAll();
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest("Audit User", email, "password123");
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
    void managementActions_shouldGenerateAuditLogs_withoutLeakingSecrets() throws Exception {
        String token = getAuthToken("audit_test@example.com");

        // 1. Create App -> APPLICATION_CREATED
        CreateApplicationRequest appReq = new CreateApplicationRequest("Audited App", "Desc", "http://localhost:8080");
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Create Key -> API_KEY_CREATED
        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Audit Key", 60, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long keyId = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("id").asLong();
        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // 3. Update Key -> API_KEY_UPDATED
        mockMvc.perform(put("/api/v1/applications/" + appId + "/keys/" + keyId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateApiKeyRequest("Renamed Key", 120, true))))
            .andExpect(status().isOk());

        // 4. Create Policy -> POLICY_CREATED
        mockMvc.perform(put("/api/v1/applications/" + appId + "/policy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SavePolicyRequest(true, 50, 60, null, null, 5000, null, null, null))))
            .andExpect(status().isOk());

        // 5. Query GET /api/v1/audit-logs
        MvcResult auditResult = mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(4))))
            .andExpect(jsonPath("$.content[?(@.action == 'APPLICATION_CREATED')]").exists())
            .andExpect(jsonPath("$.content[?(@.action == 'API_KEY_CREATED')]").exists())
            .andExpect(jsonPath("$.content[?(@.action == 'POLICY_CREATED')]").exists())
            .andReturn();

        String auditResponse = auditResult.getResponse().getContentAsString();
        // VERIFY: Raw secret is NEVER in audit log metadata or response
        assertThat(auditResponse).doesNotContain(rawKey);
    }
}
