package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.OpenApiImportRequest;
import com.sentinel.api.model.DocumentationStatus;
import com.sentinel.api.model.User;
import com.sentinel.api.repository.ApiEndpointRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.UserRepository;
import com.sentinel.api.security.JwtService;
import com.sentinel.api.service.ApiDiscoveryService;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OpenApiImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    @Autowired
    private ApiDiscoveryService apiDiscoveryService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String jwtToken;
    private Long appId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        testUser = new User("OpenAPI User", "openapi-tester@sentinel.com", passwordEncoder.encode("Pass123!"));
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateToken(testUser);

        CreateApplicationRequest appReq = new CreateApplicationRequest("OpenAPI App", "Test App", "http://localhost:9090");
        String res = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        appId = objectMapper.readTree(res).get("id").asLong();
    }

    @Test
    void testOpenApiJsonImportAndDiscoveryTransition() throws Exception {
        String openApiJson = """
        {
          "openapi": "3.0.0",
          "info": {
            "title": "Sample API",
            "version": "1.0.0"
          },
          "paths": {
            "/users": {
              "get": {
                "summary": "Get all users",
                "description": "Returns list of users"
              },
              "post": {
                "summary": "Create user",
                "description": "Creates a new user profile"
              }
            },
            "/users/{id}": {
              "get": {
                "summary": "Get user by ID",
                "description": "Returns single user"
              },
              "delete": {
                "summary": "Delete user",
                "description": "Removes user record"
              }
            }
          }
        }
        """;

        OpenApiImportRequest req = new OpenApiImportRequest(openApiJson, null);

        mockMvc.perform(post("/api/v1/applications/" + appId + "/openapi/import")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endpointsImported", is(4)))
            .andExpect(jsonPath("$.totalDocumentedEndpoints", is(4)))
            .andExpect(jsonPath("$.endpoints", hasSize(4)));

        // Verify in database: status should be DOCUMENTED
        var ep = apiEndpointRepository.findByApplicationIdAndMethodAndNormalizedPath(appId, "GET", "/users");
        assertTrue(ep.isPresent());
        assertEquals(DocumentationStatus.DOCUMENTED, ep.get().getDocumentationStatus());
        assertEquals("Get all users", ep.get().getSummary());

        // Now simulate live gateway traffic to /users
        apiDiscoveryService.discoverOrUpdateEndpoint(appId, "GET", "/users");

        // Verify status transitioned to DOCUMENTED_AND_DISCOVERED
        var epUpdated = apiEndpointRepository.findByApplicationIdAndMethodAndNormalizedPath(appId, "GET", "/users");
        assertTrue(epUpdated.isPresent());
        assertEquals(DocumentationStatus.DOCUMENTED_AND_DISCOVERED, epUpdated.get().getDocumentationStatus());
    }

    @Test
    void testOpenApiSsrfProtectionBlocksLocalhostUrl() throws Exception {
        OpenApiImportRequest req = new OpenApiImportRequest(null, "http://127.0.0.1:8080/swagger.json");

        mockMvc.perform(post("/api/v1/applications/" + appId + "/openapi/import")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error", containsString("FORBIDDEN")));
    }
}
