package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.model.User;
import com.sentinel.api.repository.UserRepository;
import com.sentinel.api.security.JwtService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ConsumerAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private HttpServer targetServer;
    private int targetPort;
    private User testUser;
    private String jwtToken;
    private Long appId;
    private Long key1Id;
    private Long key2Id;
    private String rawKey1;
    private String rawKey2;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Start mock target HTTP server
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetPort = targetServer.getAddress().getPort();
        targetServer.createContext("/data", exchange -> {
            byte[] resp = "{\"status\":\"ok\"}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        targetServer.createContext("/bad", exchange -> {
            byte[] resp = "{\"error\":\"bad_request\"}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        targetServer.start();

        testUser = new User("Consumer User", "consumer-tester@sentinel.com", passwordEncoder.encode("Pass123!"));
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateToken(testUser);

        CreateApplicationRequest appReq = new CreateApplicationRequest("Consumer App", "Consumer App", "http://localhost:" + targetPort);
        String res = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        appId = objectMapper.readTree(res).get("id").asLong();

        // Create Key 1
        String keyRes1 = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApiKeyRequest("Key Alpha", 100))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        key1Id = objectMapper.readTree(keyRes1).get("id").asLong();
        rawKey1 = objectMapper.readTree(keyRes1).get("apiKey").asText();

        // Create Key 2
        String keyRes2 = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApiKeyRequest("Key Beta", 100))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        key2Id = objectMapper.readTree(keyRes2).get("id").asLong();
        rawKey2 = objectMapper.readTree(keyRes2).get("apiKey").asText();
    }

    @AfterEach
    void tearDown() {
        if (targetServer != null) {
            targetServer.stop(0);
        }
    }

    @Test
    void testConsumerKeyAnalyticsAggregation() throws Exception {
        // Send 3 requests with Key 1 (2 success, 1 bad request)
        mockMvc.perform(get("/api/v1/gateway/data").header("X-Sentinel-API-Key", rawKey1))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/gateway/data").header("X-Sentinel-API-Key", rawKey1))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/gateway/bad").header("X-Sentinel-API-Key", rawKey1))
            .andExpect(status().isBadRequest());

        // Send 1 request with Key 2
        mockMvc.perform(get("/api/v1/gateway/data").header("X-Sentinel-API-Key", rawKey2))
            .andExpect(status().isOk());

        // Query key 1 analytics
        mockMvc.perform(get("/api/v1/applications/" + appId + "/keys/" + key1Id + "/analytics")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiKeyId", is(key1Id.intValue())))
            .andExpect(jsonPath("$.keyName", is("Key Alpha")))
            .andExpect(jsonPath("$.totalRequests", is(3)))
            .andExpect(jsonPath("$.successRequests", is(2)))
            .andExpect(jsonPath("$.errorRequests", is(1)))
            .andExpect(jsonPath("$.count4xx", is(1)))
            .andExpect(jsonPath("$.topEndpoints", hasSize(2)));

        // Query application consumers list
        mockMvc.perform(get("/api/v1/applications/" + appId + "/consumers")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].apiKeyId", is(key1Id.intValue()))) // Key 1 has 3 requests
            .andExpect(jsonPath("$[1].apiKeyId", is(key2Id.intValue()))); // Key 2 has 1 request

        // Query global top consumers
        mockMvc.perform(get("/api/v1/analytics/consumers/top")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }
}
