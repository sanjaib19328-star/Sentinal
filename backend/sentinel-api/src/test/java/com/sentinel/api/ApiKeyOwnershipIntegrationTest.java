package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String name, String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest(name, email, password);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void createAndListApplicationApiKey_whenOwned_shouldSucceed() throws Exception {
        String token = getAuthToken("Owner App", "owner_key@example.com", "pass1234");

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("App With Key", "Desc", "http://keyapp.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyRequest = new CreateApiKeyRequest("Production Key", 120, null);

        // Create key under application
        mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("Production Key"))
            .andExpect(jsonPath("$.apiKey").value(org.hamcrest.Matchers.startsWith("sk_sentinel_")))
            .andExpect(jsonPath("$.rateLimitPerMinute").value(120));

        // List keys under application
        mockMvc.perform(get("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Production Key"))
            .andExpect(jsonPath("$[0].apiKey").doesNotExist());
    }

    @Test
    void createAndListApplicationApiKey_whenNotOwned_shouldReturn404() throws Exception {
        String tokenUserA = getAuthToken("User A", "usera_key@example.com", "pass1234");
        String tokenUserB = getAuthToken("User B", "userb_key@example.com", "pass1234");

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("App A", "Desc", "http://a.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        CreateApiKeyRequest keyRequest = new CreateApiKeyRequest("Hacker Key", 500, null);

        // User B tries to create key for User A's app -> 404
        mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + tokenUserB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyRequest)))
            .andExpect(status().isNotFound());

        // User B tries to list keys for User A's app -> 404
        mockMvc.perform(get("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());
    }
}
