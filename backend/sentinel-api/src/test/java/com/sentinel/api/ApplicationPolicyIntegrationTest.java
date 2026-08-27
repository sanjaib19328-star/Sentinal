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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationPolicyIntegrationTest {

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
    static void startServer() throws Exception {
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetServer.createContext("/data", exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }
            byte[] resp = "{\"ack\":true}".getBytes(StandardCharsets.UTF_8);
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
    static void stopServer() {
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
        RegisterRequest reg = new RegisterRequest("Policy User", email, "password123");
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
    void policyCrud_andMethodAndPayloadSizeRestrictions() throws Exception {
        String token = getAuthToken("policy_crud@example.com");

        // 1. Create App
        CreateApplicationRequest appReq = new CreateApplicationRequest("Policy CRUD App", "Desc", "http://localhost:" + targetPort);
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Policy Key", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // 2. Set App Policy: only "GET,POST" allowed, max body size = 50 bytes
        SavePolicyRequest policyReq = new SavePolicyRequest(true, 100, 60, null, null, 4000, 50L, null, "GET,POST");
        mockMvc.perform(put("/api/v1/applications/" + appId + "/policy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(policyReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowedMethods").value("GET,POST"))
            .andExpect(jsonPath("$.maxRequestBodyBytes").value(50));

        // 3. GET /policy returns saved policy
        mockMvc.perform(get("/api/v1/applications/" + appId + "/policy")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowedMethods").value("GET,POST"));

        // 4. Send GET request (allowed)
        mockMvc.perform(get("/api/v1/gateway/data")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk());

        // 5. Send DELETE request -> 405 Method Not Allowed
        mockMvc.perform(delete("/api/v1/gateway/data")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));

        // 6. Send POST request with small body (< 50 bytes) -> 200 OK
        mockMvc.perform(post("/api/v1/gateway/data")
                .header("X-Sentinel-API-Key", rawKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"small\":true}"))
            .andExpect(status().isOk());

        // 7. Send POST request with large body (> 50 bytes) -> 413 Payload Too Large
        String largeBody = "{\"data\":\"" + "A".repeat(100) + "\"}";
        mockMvc.perform(post("/api/v1/gateway/data")
                .header("X-Sentinel-API-Key", rawKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(largeBody))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.error").value("PAYLOAD_TOO_LARGE"));

        // 8. Delete policy
        mockMvc.perform(delete("/api/v1/applications/" + appId + "/policy")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        // 9. After policy deleted, DELETE is now permitted
        mockMvc.perform(delete("/api/v1/gateway/data")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk());
    }
}
