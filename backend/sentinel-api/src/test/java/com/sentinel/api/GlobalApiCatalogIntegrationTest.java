package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.OpenApiImportRequest;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.User;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.UserRepository;
import com.sentinel.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class GlobalApiCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private User testUser;
    private Application app1;
    private Application app2;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.currentTimeMillis());
        testUser = new User();
        testUser.setName("Catalog Tester");
        testUser.setEmail("catalog-" + unique + "@sentinel.com");
        testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser = userRepository.save(testUser);
        token = jwtService.generateToken(testUser);

        app1 = new Application();
        app1.setName("Payment App " + unique);
        app1.setBaseUrl("http://localhost:9091");
        app1.setOwnerId(testUser.getId());
        app1 = applicationRepository.save(app1);

        app2 = new Application();
        app2.setName("User App " + unique);
        app2.setBaseUrl("http://localhost:9092");
        app2.setOwnerId(testUser.getId());
        app2 = applicationRepository.save(app2);
    }

    @Test
    void testGlobalApiCatalogCrossApplicationSearchAndFilter() throws Exception {
        // Import OpenAPI spec for App1
        String spec1 = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Payments", "version": "1.0" },
              "paths": {
                "/payments/charge": {
                  "post": { "summary": "Process credit card charge" }
                },
                "/payments/refund": {
                  "post": { "summary": "Refund transaction", "deprecated": true }
                }
              }
            }
            """;
        mockMvc.perform(post("/api/v1/applications/" + app1.getId() + "/openapi/import")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OpenApiImportRequest(spec1, null))))
            .andExpect(status().isOk());

        // Import OpenAPI spec for App2
        String spec2 = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Users", "version": "1.0" },
              "paths": {
                "/users/list": {
                  "get": { "summary": "List all active users" }
                }
              }
            }
            """;
        mockMvc.perform(post("/api/v1/applications/" + app2.getId() + "/openapi/import")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OpenApiImportRequest(spec2, null))))
            .andExpect(status().isOk());

        // 1. Query all global APIs across apps
        mockMvc.perform(get("/api/v1/apis")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].normalizedPath", hasItems("/payments/charge", "/payments/refund", "/users/list")));

        // 2. Search query filter
        mockMvc.perform(get("/api/v1/apis")
                .param("search", "charge")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].normalizedPath", is("/payments/charge")))
            .andExpect(jsonPath("$[0].applicationName", is(app1.getName())));

        // 3. Filter by Method
        mockMvc.perform(get("/api/v1/apis")
                .param("method", "GET")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].normalizedPath", is("/users/list")));

        // 4. Filter by Deprecated
        mockMvc.perform(get("/api/v1/apis")
                .param("deprecated", "true")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].normalizedPath", is("/payments/refund")));

        // 5. Filter by Application
        mockMvc.perform(get("/api/v1/apis")
                .param("applicationId", String.valueOf(app2.getId()))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].normalizedPath", is("/users/list")));
    }
}
