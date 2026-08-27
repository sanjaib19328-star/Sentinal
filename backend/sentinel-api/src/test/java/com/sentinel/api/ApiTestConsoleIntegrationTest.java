package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.ApiTestConsoleRequest;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApiTestConsoleIntegrationTest {

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
    private Long apiKeyId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Start mock target HTTP server
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetPort = targetServer.getAddress().getPort();
        targetServer.createContext("/api/hello", exchange -> {
            byte[] resp = "{\"greeting\":\"Hello from Target Server!\"}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("X-Target-Custom-Header", "Validated");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        targetServer.start();

        testUser = new User("Console User", "console-tester@sentinel.com", passwordEncoder.encode("Pass123!"));
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateToken(testUser);

        CreateApplicationRequest appReq = new CreateApplicationRequest("Console App", "Console App", "http://localhost:" + targetPort);
        String res = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        appId = objectMapper.readTree(res).get("id").asLong();

        // Create Key
        String keyRes = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApiKeyRequest("Console Key", 100))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        apiKeyId = objectMapper.readTree(keyRes).get("id").asLong();
    }

    @AfterEach
    void tearDown() {
        if (targetServer != null) {
            targetServer.stop(0);
        }
    }

    @Test
    void testExecuteTestConsoleRequest() throws Exception {
        ApiTestConsoleRequest consoleReq = new ApiTestConsoleRequest(
            apiKeyId,
            "GET",
            "/api/hello",
            Map.of("lang", "en"),
            Map.of("Accept", "application/json"),
            null
        );

        mockMvc.perform(post("/api/v1/applications/" + appId + "/apis/test-console")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(consoleReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode", is(200)))
            .andExpect(jsonPath("$.requestId", notNullValue()))
            .andExpect(jsonPath("$.responseBody", containsString("Hello from Target Server!")))
            .andExpect(jsonPath("$.rateLimitLimit", is(100)));
    }
}
