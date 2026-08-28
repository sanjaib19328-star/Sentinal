package com.sentinel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.ApiEndpointDto;
import com.sentinel.api.dto.ApplicationStatusResponse;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.DocumentationStatus;
import com.sentinel.api.model.HealthStatus;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.RequestLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolExecutionServiceTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SystemHealthService systemHealthService;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private ApiCatalogService apiCatalogService;

    @Mock
    private RequestLogRepository requestLogRepository;

    private ToolExecutionService toolExecutionService;
    private final ObjectMapper mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
        .findAndAddModules()
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    @BeforeEach
    void setUp() {
        toolExecutionService = new ToolExecutionService(
            applicationService,
            applicationRepository,
            systemHealthService,
            analyticsService,
            apiCatalogService,
            requestLogRepository
        );
    }

    @Test
    void testListApplicationsExecution() throws Exception {
        Application app = new Application();
        app.setId(1L);
        app.setName("Billing API");
        app.setBaseUrl("https://api.billing.internal");
        app.setHealthStatus(HealthStatus.HEALTHY);

        when(applicationRepository.findByOwnerId(10L)).thenReturn(List.of(app));

        String json = toolExecutionService.executeTool("list_applications", Collections.emptyMap(), 10L, null);
        assertNotNull(json);

        JsonNode root = mapper.readTree(json);
        assertEquals("sentinel_database", root.path("source").asText());
        assertEquals(1, root.path("totalCount").asInt());
        assertEquals("Billing API", root.path("applications").get(0).path("name").asText());
    }

    @Test
    void testGetApiCatalogWithExplicitAppId() throws Exception {
        ApiEndpointDto ep = new ApiEndpointDto(
            100L, 1L, "GET", "/api/v1/invoices", DocumentationStatus.DOCUMENTED,
            "Get Invoices", "Retrieve invoices list", null, null, null,
            false, Instant.now(), Instant.now(), 45L, 0L, 12.5, 100.0
        );

        when(apiCatalogService.listApplicationEndpoints(10L, 1L)).thenReturn(List.of(ep));

        Map<String, Object> args = new HashMap<>();
        args.put("applicationId", 1L);

        String json = toolExecutionService.executeTool("get_api_catalog", args, 10L, null);
        assertNotNull(json);

        JsonNode root = mapper.readTree(json);
        assertEquals(1, root.path("totalEndpoints").asInt());
        assertEquals("/api/v1/invoices", root.path("endpoints").get(0).path("normalizedPath").asText());
    }

    @Test
    void testGetApiCatalogWithoutAppIdAggregatesAllApps() throws Exception {
        Application app1 = new Application();
        app1.setId(1L);
        app1.setName("Payment Gateway");

        Application app2 = new Application();
        app2.setId(2L);
        app2.setName("User Service");

        when(applicationRepository.findByOwnerId(10L)).thenReturn(List.of(app1, app2));

        ApiEndpointDto ep1 = new ApiEndpointDto(
            101L, 1L, "POST", "/v1/charge", DocumentationStatus.DOCUMENTED,
            "Create Charge", null, null, null, null, false, null, null, 10L, 0L, 50.0, 100.0
        );
        ApiEndpointDto ep2 = new ApiEndpointDto(
            102L, 2L, "GET", "/v1/users", DocumentationStatus.DOCUMENTED,
            "List Users", null, null, null, null, false, null, null, 20L, 0L, 30.0, 100.0
        );

        when(apiCatalogService.listApplicationEndpoints(10L, 1L)).thenReturn(List.of(ep1));
        when(apiCatalogService.listApplicationEndpoints(10L, 2L)).thenReturn(List.of(ep2));

        // When user prompt says "give list of apis" and no applicationId is passed
        String json = toolExecutionService.executeTool("get_api_catalog", Collections.emptyMap(), 10L, null);
        assertNotNull(json);

        JsonNode root = mapper.readTree(json);
        assertEquals(2, root.path("totalApplications").asInt());
        assertEquals(2, root.path("totalEndpoints").asInt());
        assertEquals("Payment Gateway", root.path("applications").get(0).path("applicationName").asText());
    }

    @Test
    void testGetApplicationHealth() throws Exception {
        ApplicationStatusResponse status = new ApplicationStatusResponse(1L, HealthStatus.HEALTHY, Instant.now(), com.sentinel.api.model.ConnectionMode.OBSERVATION);
        when(applicationService.getApplicationStatus(10L, 1L)).thenReturn(status);

        Map<String, Object> args = Map.of("applicationId", 1);
        String json = toolExecutionService.executeTool("get_application_health", args, 10L, null);
        assertNotNull(json);

        JsonNode root = mapper.readTree(json);
        assertEquals("HEALTHY", root.path("healthStatus").asText());
    }
}
