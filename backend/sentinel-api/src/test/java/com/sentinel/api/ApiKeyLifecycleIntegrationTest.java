package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.UpdateApiKeyRequest;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import com.sentinel.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyLifecycleIntegrationTest {

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

    @BeforeEach
    void cleanDb() {
        requestLogRepository.deleteAll();
        apiKeyRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String getAuthToken(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest("Lifecycle User", email, "password123");
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
    void completeApiKeyLifecycle_generate_list_edit_revoke_regenerate_delete() throws Exception {
        String token = getAuthToken("key_owner@example.com");

        // 1. Create Application
        CreateApplicationRequest appReq = new CreateApplicationRequest("Key Lifecycle App", "Desc", "http://localhost:8080");
        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. GENERATE API Key (Must return raw key ONCE and never hash)
        CreateApiKeyRequest keyReq = new CreateApiKeyRequest("Production Key", 60, null);
        MvcResult createResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("Production Key"))
            .andExpect(jsonPath("$.apiKey", containsString("sk_sentinel_")))
            .andExpect(jsonPath("$.maskedKey", containsString("sk_sentinel_••••")))
            .andExpect(jsonPath("$.rateLimitPerMinute").value(60))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.warning").isNotEmpty())
            .andReturn();

        long keyId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();
        String initialRawKey = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("apiKey").asText();

        // Verify gateway test accepts this key
        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", initialRawKey))
            .andExpect(status().isOk());

        // 3. LIST Keys (Must NOT return raw secret)
        mockMvc.perform(get("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].apiKey").value(nullValue()))
            .andExpect(jsonPath("$[0].maskedKey", containsString("sk_sentinel_••••")))
            .andExpect(jsonPath("$[0].active").value(true));

        // 4. EDIT Key (Update name and rate limit)
        UpdateApiKeyRequest updateReq = new UpdateApiKeyRequest("Renamed Prod Key", 120, true);
        mockMvc.perform(put("/api/v1/applications/" + appId + "/keys/" + keyId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed Prod Key"))
            .andExpect(jsonPath("$.rateLimitPerMinute").value(120))
            .andExpect(jsonPath("$.apiKey").value(nullValue()));

        // 5. REVOKE Key (Soft revocation)
        mockMvc.perform(post("/api/v1/applications/" + appId + "/keys/" + keyId + "/revoke")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.revokedAt").isNotEmpty());

        // Revoked key MUST immediately return 401
        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", initialRawKey))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        // 6. REGENERATE Key
        MvcResult regenResult = mockMvc.perform(post("/api/v1/applications/" + appId + "/keys/" + keyId + "/regenerate")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.apiKey", containsString("sk_sentinel_")))
            .andExpect(jsonPath("$.revokedAt").value(nullValue()))
            .andReturn();

        String newRawKey = objectMapper.readTree(regenResult.getResponse().getContentAsString()).get("apiKey").asText();
        assertThat(newRawKey).isNotEqualTo(initialRawKey);

        // Old key STILL fails (401)
        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", initialRawKey))
            .andExpect(status().isUnauthorized());

        // New key WORKS (200)
        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", newRawKey))
            .andExpect(status().isOk());

        // 7. DELETE Key
        mockMvc.perform(delete("/api/v1/applications/" + appId + "/keys/" + keyId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        // Deleted key fails (401)
        mockMvc.perform(post("/api/v1/gateway/test")
                .header("X-Sentinel-API-Key", newRawKey))
            .andExpect(status().isUnauthorized());

        // List is now empty
        mockMvc.perform(get("/api/v1/applications/" + appId + "/keys")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void multiTenantCrossApplicationIsolation_cannotAccessAnotherUsersKeys() throws Exception {
        String tokenUserA = getAuthToken("usera_keys@example.com");
        String tokenUserB = getAuthToken("userb_keys@example.com");

        // User A creates app and key
        MvcResult appResultA = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("App A", "Desc", "http://localhost:8080"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appIdA = objectMapper.readTree(appResultA.getResponse().getContentAsString()).get("id").asLong();

        MvcResult keyResultA = mockMvc.perform(post("/api/v1/applications/" + appIdA + "/keys")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApiKeyRequest("Key A", 60, null))))
            .andExpect(status().isCreated())
            .andReturn();

        long keyIdA = objectMapper.readTree(keyResultA.getResponse().getContentAsString()).get("id").asLong();

        // User B attempts to access/modify User A's keys -> 404
        mockMvc.perform(get("/api/v1/applications/" + appIdA + "/keys")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/applications/" + appIdA + "/keys/" + keyIdA)
                .header("Authorization", "Bearer " + tokenUserB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateApiKeyRequest("Hacked Name", 10, true))))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/applications/" + appIdA + "/keys/" + keyIdA + "/revoke")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/applications/" + appIdA + "/keys/" + keyIdA + "/regenerate")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/applications/" + appIdA + "/keys/" + keyIdA)
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());
    }
}
