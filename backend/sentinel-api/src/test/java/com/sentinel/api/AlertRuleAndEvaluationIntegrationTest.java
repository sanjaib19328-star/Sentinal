package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateAlertRuleRequest;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.UpdateAlertRuleRequest;
import com.sentinel.api.model.AlertRuleType;
import com.sentinel.api.repository.AlertRepository;
import com.sentinel.api.repository.AlertRuleRepository;
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

import java.net.InetSocketAddress;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlertRuleAndEvaluationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static HttpServer targetServer;
    private static int targetPort;

    @BeforeAll
    static void startServer() throws Exception {
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetServer.createContext("/bad", exchange -> {
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
        alertRepository.deleteAll();
        alertRuleRepository.deleteAll();
        requestLogRepository.deleteAll();
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest("Alert User", email, "password123");
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
    void alertRulesCrud_andRealTimeErrorEvaluation_andAcknowledgeResolve() throws Exception {
        String token = getAuthToken("alert_eval_test@example.com");

        CreateApplicationRequest appReq = new CreateApplicationRequest("Alert Test App", "Desc", "http://localhost:" + targetPort);
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Alert Test Key", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // 1. Create Alert Rule: HIGH_ERROR_RATE threshold = 10.0%, window = 300s
        CreateAlertRuleRequest ruleReq = new CreateAlertRuleRequest(null, AlertRuleType.HIGH_ERROR_RATE, 10.0, 300, true);
        MvcResult ruleResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/alert-rules")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ruleReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("HIGH_ERROR_RATE"))
            .andExpect(jsonPath("$.threshold").value(10.0))
            .andReturn();
        long ruleId = objectMapper.readTree(ruleResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. GET /alert-rules
        mockMvc.perform(get("/api/v1/applications/" + appId + "/alert-rules")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        // 3. Send 500 error requests through gateway to trigger rule
        mockMvc.perform(get("/api/v1/gateway/bad").header("X-Sentinel-API-Key", rawKey)).andExpect(status().isInternalServerError());
        mockMvc.perform(get("/api/v1/gateway/bad").header("X-Sentinel-API-Key", rawKey)).andExpect(status().isInternalServerError());

        // 4. GET /alerts should show triggered ACTIVE alert
        MvcResult alertsResult = mockMvc.perform(get("/api/v1/applications/" + appId + "/alerts")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
            .andReturn();
        long alertId = objectMapper.readTree(alertsResult.getResponse().getContentAsString()).get(0).get("id").asLong();

        // 5. Acknowledge Alert -> ACKNOWLEDGED
        mockMvc.perform(post("/api/v1/alerts/" + alertId + "/acknowledge")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        // 6. Resolve Alert -> RESOLVED
        mockMvc.perform(post("/api/v1/alerts/" + alertId + "/resolve")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"))
            .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        // 7. Delete Alert Rule
        mockMvc.perform(delete("/api/v1/applications/" + appId + "/alert-rules/" + ruleId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }
}
