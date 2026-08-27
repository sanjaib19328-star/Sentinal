package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateAlertRuleRequest;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.SavePolicyRequest;
import com.sentinel.api.model.AlertRuleType;
import com.sentinel.api.repository.AlertRepository;
import com.sentinel.api.repository.AlertRuleRepository;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApiPolicyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.AuditLogRepository;
import com.sentinel.api.repository.RequestLogRepository;
import com.sentinel.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationPhase2IntegrationTest {

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
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        auditLogRepository.deleteAll();
        alertRepository.deleteAll();
        alertRuleRepository.deleteAll();
        apiPolicyRepository.deleteAll();
        requestLogRepository.deleteAll();
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest("User", email, "password123");
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
    void userBCannotAccessUserAsPoliciesAlertsAuditLogsOrAnalytics() throws Exception {
        String tokenUserA = getAuthToken("tenant_a@example.com");
        String tokenUserB = getAuthToken("tenant_b@example.com");

        // User A creates an application, policy, and alert rule
        MvcResult appResultA = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("App A", "Desc", "http://localhost:8080"))))
            .andExpect(status().isCreated())
            .andReturn();
        long appIdA = objectMapper.readTree(appResultA.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/applications/" + appIdA + "/policy")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SavePolicyRequest(true, 100, 60, null, null, 5000, null, null, null))))
            .andExpect(status().isOk());

        MvcResult ruleResultA = mockMvc.perform(post("/api/v1/applications/" + appIdA + "/alert-rules")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateAlertRuleRequest(null, AlertRuleType.HIGH_ERROR_RATE, 20.0, 300, true))))
            .andExpect(status().isCreated())
            .andReturn();
        long ruleIdA = objectMapper.readTree(ruleResultA.getResponse().getContentAsString()).get("id").asLong();

        // 1. User B tries to get/update/delete User A's policy -> 404
        mockMvc.perform(get("/api/v1/applications/" + appIdA + "/policy")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/applications/" + appIdA + "/policy")
                .header("Authorization", "Bearer " + tokenUserB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SavePolicyRequest(true, 10, 60, null, null, 5000, null, null, null))))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/applications/" + appIdA + "/policy")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        // 2. User B tries to view/modify User A's alert rules -> 404
        mockMvc.perform(get("/api/v1/applications/" + appIdA + "/alert-rules")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/applications/" + appIdA + "/alert-rules/" + ruleIdA)
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        // 3. User B tries to view User A's analytics -> 404
        mockMvc.perform(get("/api/v1/applications/" + appIdA + "/analytics")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/applications/" + appIdA + "/analytics/timeseries")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        // 4. User B checks their own audit logs -> sees 0 logs of User A
        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));

        // 5. User B checks global dashboard summary -> sees 0 apps
        mockMvc.perform(get("/api/v1/dashboard/summary")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalApplications").value(0));
    }
}
