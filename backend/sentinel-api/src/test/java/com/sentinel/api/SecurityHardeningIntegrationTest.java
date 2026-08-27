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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityHardeningIntegrationTest {

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
    private String rawApiKey;
    private final AtomicBoolean internalHeaderReceived = new AtomicBoolean(false);

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        // Start mock target HTTP server that inspects incoming headers
        targetServer = HttpServer.create(new InetSocketAddress(0), 0);
        targetPort = targetServer.getAddress().getPort();
        targetServer.createContext("/inspect", exchange -> {
            // Check if any internal header leaked
            boolean leaked = exchange.getRequestHeaders().keySet().stream()
                .anyMatch(h -> h.toLowerCase().startsWith("x-sentinel-internal") || h.toLowerCase().startsWith("x-internal"));
            internalHeaderReceived.set(leaked);

            byte[] resp = "{\"inspected\":true}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        targetServer.start();

        testUser = new User("Security User", "security-tester@sentinel.com", passwordEncoder.encode("Pass123!"));
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateToken(testUser);

        CreateApplicationRequest appReq = new CreateApplicationRequest("Security App", "Sec App", "http://localhost:" + targetPort);
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
                .content(objectMapper.writeValueAsString(new CreateApiKeyRequest("Sec Key", 100))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        rawApiKey = objectMapper.readTree(keyRes).get("apiKey").asText();
    }

    @AfterEach
    void tearDown() {
        if (targetServer != null) {
            targetServer.stop(0);
        }
    }

    @Test
    void testInternalHeadersAreStrippedBeforeForwarding() throws Exception {
        internalHeaderReceived.set(false);

        mockMvc.perform(get("/api/v1/gateway/inspect")
                .header("X-Sentinel-API-Key", rawApiKey)
                .header("X-Sentinel-Internal-Admin", "true")
                .header("X-Internal-Token", "fake-secret-999"))
            .andExpect(status().isOk());

        assertFalse(internalHeaderReceived.get(), "Internal header must be stripped by Sentinel gateway before reaching target");
    }

    @Test
    void testOversizedHeadersAreRejected() throws Exception {
        // Build 18 KB header string (> 16 KB limit)
        String hugeValue = "A".repeat(18 * 1024);

        mockMvc.perform(get("/api/v1/gateway/inspect")
                .header("X-Sentinel-API-Key", rawApiKey)
                .header("X-Custom-Large", hugeValue))
            .andExpect(status().is(431));
    }
}
