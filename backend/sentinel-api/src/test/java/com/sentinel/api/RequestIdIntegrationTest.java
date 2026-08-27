package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.model.RequestLog;
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

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequestIdIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static HttpServer targetServer;
    private static int targetPort;
    private static final AtomicReference<String> receivedRequestId = new AtomicReference<>();

    @BeforeAll
    static void startServer() throws Exception {
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetServer.createContext("/trace", exchange -> {
            String forwardedReqId = exchange.getRequestHeaders().getFirst("X-Request-Id");
            receivedRequestId.set(forwardedReqId);

            byte[] resp = "{\"traced\":true}".getBytes(StandardCharsets.UTF_8);
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
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        receivedRequestId.set(null);
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest("Trace User", email, "password123");
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
    void requestId_whenProvidedByClient_shouldBePreservedForwardedAndLogged() throws Exception {
        String token = getAuthToken("client_trace@example.com");

        CreateApplicationRequest appReq = new CreateApplicationRequest("Trace App", "Desc", "http://localhost:" + targetPort);
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Trace Key", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        String customRequestId = "custom-client-req-998877";

        // Send request with X-Request-Id
        mockMvc.perform(get("/api/v1/gateway/trace")
                .header("X-Sentinel-API-Key", rawKey)
                .header("X-Request-Id", customRequestId))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Request-Id", customRequestId));

        // Verify target received the exact custom request ID
        assertThat(receivedRequestId.get()).isEqualTo(customRequestId);

        // Verify request_logs persisted the exact custom request ID
        RequestLog log = requestLogRepository.findByRequestId(customRequestId).get(0);
        assertThat(log.getRequestId()).isEqualTo(customRequestId);
        assertThat(log.getPath()).isEqualTo("/trace");
    }

    @Test
    void requestId_whenMissing_shouldBeGeneratedForwardedAndLogged() throws Exception {
        String token = getAuthToken("generated_trace@example.com");

        CreateApplicationRequest appReq = new CreateApplicationRequest("Gen App", "Desc", "http://localhost:" + targetPort);
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();
        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Gen Key", 100, null);
        MvcResult keyResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andReturn();
        String rawKey = objectMapper.readTree(keyResult.getResponse().getContentAsString()).get("apiKey").asText();

        // Send request without X-Request-Id
        MvcResult result = mockMvc.perform(get("/api/v1/gateway/trace")
                .header("X-Sentinel-API-Key", rawKey))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-Id"))
            .andReturn();

        String generatedId = result.getResponse().getHeader("X-Request-Id");
        assertThat(generatedId).isNotBlank();

        // Verify target received generated ID
        assertThat(receivedRequestId.get()).isEqualTo(generatedId);

        // Verify log contains generated ID
        RequestLog log = requestLogRepository.findByRequestId(generatedId).get(0);
        assertThat(log).isNotNull();
    }
}
