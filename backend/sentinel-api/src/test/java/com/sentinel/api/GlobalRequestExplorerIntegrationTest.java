package com.sentinel.api;

import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.User;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.UserRepository;
import com.sentinel.api.security.JwtService;
import com.sentinel.api.service.RequestLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class GlobalRequestExplorerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private RequestLoggingService requestLoggingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String token;
    private User testUser;
    private Application app;
    private ApiKey apiKey;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.currentTimeMillis());
        testUser = new User();
        testUser.setName("Explorer Tester");
        testUser.setEmail("explorer-" + unique + "@sentinel.com");
        testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser = userRepository.save(testUser);
        token = jwtService.generateToken(testUser);

        app = new Application();
        app.setName("Explorer Microservice " + unique);
        app.setBaseUrl("http://localhost:9090");
        app.setOwnerId(testUser.getId());
        app = applicationRepository.save(app);

        apiKey = new ApiKey();
        apiKey.setName("Test Explorer Key");
        apiKey.setKeyHash("hash-" + unique);
        apiKey.setApplicationId(app.getId());
        apiKey.setActive(true);
        apiKey.setCreatedAt(Instant.now());
        apiKey.setRateLimitPerMinute(100);
        apiKey = apiKeyRepository.save(apiKey);
    }

    @Test
    void testGlobalRequestExplorerFilteringAndPagination() throws Exception {
        // Log diverse requests
        requestLoggingService.logRequest("req-200-1", app.getId(), apiKey.getId(), null, "GET", "/api/v1/users", "/api/v1/users", 200, 30L, "192.168.1.1");
        requestLoggingService.logRequest("req-200-2", app.getId(), apiKey.getId(), null, "POST", "/api/v1/users", "/api/v1/users", 201, 45L, "192.168.1.2");
        requestLoggingService.logRequest("req-404-1", app.getId(), apiKey.getId(), null, "GET", "/api/v1/missing", "/api/v1/missing", 404, 15L, "192.168.1.3");
        requestLoggingService.logRequest("req-500-1", app.getId(), apiKey.getId(), null, "GET", "/api/v1/crash", "/api/v1/crash", 500, 80L, "192.168.1.4");

        // 1. Fetch all requests
        mockMvc.perform(get("/api/v1/requests")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(4))))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(4)));

        // 2. Filter by statusClass 5xx
        mockMvc.perform(get("/api/v1/requests")
                .param("statusClass", "5xx")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].requestId", is("req-500-1")))
            .andExpect(jsonPath("$.content[0].statusCode", is(500)));

        // 3. Filter by Method POST
        mockMvc.perform(get("/api/v1/requests")
                .param("method", "POST")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].requestId", is("req-200-2")))
            .andExpect(jsonPath("$.content[0].statusCode", is(201)));

        // 4. Search query by Path or RequestId
        mockMvc.perform(get("/api/v1/requests")
                .param("search", "crash")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].path", is("/api/v1/crash")));
    }
}
