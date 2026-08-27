package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
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
public class ProductionSecurityIntegrationTest {

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

    private User user1;
    private User user2;
    private String token1;
    private String token2;
    private Application appUser1;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.currentTimeMillis());

        user1 = new User();
        user1.setName("Alice Security");
        user1.setEmail("alice-sec-" + unique + "@sentinel.com");
        user1.setPasswordHash(passwordEncoder.encode("Password123!"));
        user1 = userRepository.save(user1);
        token1 = jwtService.generateToken(user1);

        user2 = new User();
        user2.setName("Bob Attacker");
        user2.setEmail("bob-att-" + unique + "@sentinel.com");
        user2.setPasswordHash(passwordEncoder.encode("Password123!"));
        user2 = userRepository.save(user2);
        token2 = jwtService.generateToken(user2);

        appUser1 = new Application();
        appUser1.setName("Alice App " + unique);
        appUser1.setBaseUrl("http://localhost:9090");
        appUser1.setOwnerId(user1.getId());
        appUser1 = applicationRepository.save(appUser1);
    }

    @Test
    void testUnauthenticatedManagementApiReturnsStructuredError() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    void testTenantIsolationCrossUserAccessForbidden() throws Exception {
        // User 2 attempts to fetch or modify User 1's application
        mockMvc.perform(get("/api/v1/applications/" + appUser1.getId())
                .header("Authorization", "Bearer " + token2))
            .andExpect(status().isNotFound());
    }

    @Test
    void testApiKeySecretNeverReturnedInListing() throws Exception {
        // Create key for appUser1
        mockMvc.perform(post("/api/v1/applications/" + appUser1.getId() + "/keys")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApiKeyRequest("SecKey", 60, null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.apiKey", startsWith("sk_sentinel_"))); // Returned once on creation

        // When listing keys, raw apiKey MUST be null or omitted
        mockMvc.perform(get("/api/v1/applications/" + appUser1.getId() + "/keys")
                .header("Authorization", "Bearer " + token1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].apiKey", nullValue()))
            .andExpect(jsonPath("$[0].maskedKey", containsString("••••")));
    }
}
