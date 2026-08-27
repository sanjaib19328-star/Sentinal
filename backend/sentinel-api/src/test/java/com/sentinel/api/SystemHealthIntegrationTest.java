package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.model.User;
import com.sentinel.api.repository.UserRepository;
import com.sentinel.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SystemHealthIntegrationTest {

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

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        testUser = new User("Health User", "health-tester@sentinel.com", passwordEncoder.encode("Pass123!"));
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateToken(testUser);

        CreateApplicationRequest appReq = new CreateApplicationRequest("Health App", "Test Health", "http://localhost:9090");
        mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated());
    }

    @Test
    void testSystemHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/system/health")
                .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.controlPlaneStatus", is("UP")))
            .andExpect(jsonPath("$.mysql.status", is("UP")))
            .andExpect(jsonPath("$.mysql.latencyMs", greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.redis.status", is("UP")))
            .andExpect(jsonPath("$.redis.latencyMs", greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.targetApplications", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.targetApplications[0].circuitState", is("CLOSED")));
    }
}
