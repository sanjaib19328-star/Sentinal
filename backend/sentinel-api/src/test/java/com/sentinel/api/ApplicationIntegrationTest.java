package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.UpdateApplicationRequest;
import com.sentinel.api.model.ApplicationMetric;
import com.sentinel.api.model.ConnectionMode;
import com.sentinel.api.model.MetricType;
import com.sentinel.api.repository.ApplicationMetricRepository;
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

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationMetricRepository applicationMetricRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        applicationMetricRepository.deleteAll();
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
    void unauthenticatedAccess_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createApplication_withValidData_shouldReturn201WithObservationDefaults() throws Exception {
        String token = getAuthToken("Owner One", "owner1@example.com", "pass1234");

        CreateApplicationRequest request = new CreateApplicationRequest(
            "PixelVault",
            "Image security API",
            "https://pixelvault.example.com"
        );

        mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("PixelVault"))
            .andExpect(jsonPath("$.description").value("Image security API"))
            .andExpect(jsonPath("$.baseUrl").value("https://pixelvault.example.com"))
            .andExpect(jsonPath("$.connectionMode").value("OBSERVATION"))
            .andExpect(jsonPath("$.healthStatus").value("UNKNOWN"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void listApplications_shouldReturnOnlyOwnedApplications() throws Exception {
        String tokenUserA = getAuthToken("User A", "usera@example.com", "pass1234");
        String tokenUserB = getAuthToken("User B", "userb@example.com", "pass1234");

        // User A creates 2 apps
        mockMvc.perform(post("/api/v1/applications")
            .header("Authorization", "Bearer " + tokenUserA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateApplicationRequest("App A1", "Desc", "http://a1.com"))));

        mockMvc.perform(post("/api/v1/applications")
            .header("Authorization", "Bearer " + tokenUserA)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateApplicationRequest("App A2", "Desc", "http://a2.com"))));

        // User B creates 1 app
        mockMvc.perform(post("/api/v1/applications")
            .header("Authorization", "Bearer " + tokenUserB)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateApplicationRequest("App B1", "Desc", "http://b1.com"))));

        // User A lists applications -> 2 apps
        mockMvc.perform(get("/api/v1/applications")
                .header("Authorization", "Bearer " + tokenUserA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("App A1"))
            .andExpect(jsonPath("$[1].name").value("App A2"));

        // User B lists applications -> 1 app
        mockMvc.perform(get("/api/v1/applications")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("App B1"));
    }

    @Test
    void getApplication_whenOwned_shouldReturn200() throws Exception {
        String token = getAuthToken("User A", "usera_get@example.com", "pass1234");

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("My App", "Desc", "http://myapp.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(appId))
            .andExpect(jsonPath("$.name").value("My App"));
    }

    @Test
    void updateApplication_whenOwned_shouldUpdateSuccessfully() throws Exception {
        String token = getAuthToken("User A", "usera_update@example.com", "pass1234");

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("Original Name", "Desc", "http://original.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        UpdateApplicationRequest updateReq = new UpdateApplicationRequest(
            "Updated Name",
            "Updated Desc",
            "http://updated.com",
            false,
            ConnectionMode.OBSERVATION
        );

        mockMvc.perform(put("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.description").value("Updated Desc"))
            .andExpect(jsonPath("$.baseUrl").value("http://updated.com"))
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteApplication_whenOwned_shouldDeleteSuccessfully() throws Exception {
        String token = getAuthToken("User A", "usera_delete@example.com", "pass1234");

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("To Delete", "Desc", "http://todelete.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void crossUserIsolation_userBCannotAccessUserAApplication() throws Exception {
        String tokenUserA = getAuthToken("User A", "usera_iso@example.com", "pass1234");
        String tokenUserB = getAuthToken("User B", "userb_iso@example.com", "pass1234");

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("App A", "Desc", "http://a.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // User B GET -> 404
        mockMvc.perform(get("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        // User B PUT -> 404
        mockMvc.perform(put("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + tokenUserB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateApplicationRequest("Hacked", null, null, null, null))))
            .andExpect(status().isNotFound());

        // User B DELETE -> 404
        mockMvc.perform(delete("/api/v1/applications/" + appId)
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());
    }

    @Test
    void applicationStatusAndMetrics_shouldBeIsolatedPerUser() throws Exception {
        String tokenUserA = getAuthToken("User A", "usera_stat@example.com", "pass1234");
        String tokenUserB = getAuthToken("User B", "userb_stat@example.com", "pass1234");

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateApplicationRequest("Observability App", "Desc", "http://obs.com"))))
            .andExpect(status().isCreated())
            .andReturn();

        long appId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Add some metric
        applicationMetricRepository.save(new ApplicationMetric(appId, MetricType.REQUEST_COUNT, 150.0, Instant.now()));

        // User A get status -> 200
        mockMvc.perform(get("/api/v1/applications/" + appId + "/status")
                .header("Authorization", "Bearer " + tokenUserA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(appId))
            .andExpect(jsonPath("$.status").value("UNKNOWN"))
            .andExpect(jsonPath("$.connectionMode").value("OBSERVATION"));

        // User A get metrics -> 200
        mockMvc.perform(get("/api/v1/applications/" + appId + "/metrics")
                .header("Authorization", "Bearer " + tokenUserA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(appId))
            .andExpect(jsonPath("$.metrics", hasSize(1)))
            .andExpect(jsonPath("$.metrics[0].type").value("REQUEST_COUNT"))
            .andExpect(jsonPath("$.metrics[0].value").value(150.0));

        // User B get status -> 404
        mockMvc.perform(get("/api/v1/applications/" + appId + "/status")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());

        // User B get metrics -> 404
        mockMvc.perform(get("/api/v1/applications/" + appId + "/metrics")
                .header("Authorization", "Bearer " + tokenUserB))
            .andExpect(status().isNotFound());
    }
}
