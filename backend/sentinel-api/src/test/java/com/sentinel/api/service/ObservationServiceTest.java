package com.sentinel.api.service;

import com.sentinel.api.dto.ConnectionTestResponse;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.HealthStatus;
import com.sentinel.api.repository.ApplicationMetricRepository;
import com.sentinel.api.repository.ApplicationRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ObservationServiceTest {

    private ApplicationRepository applicationRepository;
    private ApplicationMetricRepository applicationMetricRepository;
    private ObservationService observationService;

    private static HttpServer testHttpServer;
    private static int serverPort;

    @BeforeAll
    static void startServer() throws Exception {
        testHttpServer = HttpServer.create(new InetSocketAddress(0), 0);

        // 200 OK probe endpoint
        testHttpServer.createContext("/health", exchange -> {
            byte[] response = "{\"status\":\"UP\"}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        });

        // 503 Service Unavailable probe endpoint
        testHttpServer.createContext("/error/health", exchange -> {
            byte[] response = "{\"status\":\"DOWN\"}".getBytes();
            exchange.sendResponseHeaders(503, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        });

        testHttpServer.start();
        serverPort = testHttpServer.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (testHttpServer != null) {
            testHttpServer.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        applicationMetricRepository = mock(ApplicationMetricRepository.class);
        observationService = new ObservationService(applicationRepository, applicationMetricRepository);
    }

    @Test
    void testZeroRegisteredApplications_PerformsZeroRequests() {
        when(applicationRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

        List<ConnectionTestResponse> responses = observationService.observeAllActiveApplications();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(applicationRepository, times(1)).findAllByActiveTrue();
        verifyNoInteractions(applicationMetricRepository);
    }

    @Test
    void testZeroApplicationsForOwner_PerformsZeroRequests() {
        when(applicationRepository.findAllByOwnerIdAndActiveTrue(123L)).thenReturn(Collections.emptyList());

        List<ConnectionTestResponse> responses = observationService.observeApplicationsForOwner(123L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(applicationRepository, times(1)).findAllByOwnerIdAndActiveTrue(123L);
        verifyNoInteractions(applicationMetricRepository);
    }

    @Test
    void testNullOrBlankBaseUrl_ReturnsUnknownWithoutExternalRequest() {
        Application app = new Application();
        app.setId(10L);
        app.setBaseUrl("   ");

        ConnectionTestResponse response = observationService.testConnection(app);

        assertNotNull(response);
        assertEquals(10L, response.getApplicationId());
        assertFalse(response.isReachable());
        assertEquals(HealthStatus.UNKNOWN, response.getStatus());
        assertEquals("No base URL configured for application", response.getMessage());
        verify(applicationRepository, times(1)).save(app);
        verifyNoInteractions(applicationMetricRepository);
    }

    @Test
    void testHealthyApplication_ProbesAndReturnsHealthy() {
        Application app = new Application();
        app.setId(1L);
        app.setBaseUrl("http://localhost:" + serverPort);

        ConnectionTestResponse response = observationService.testConnection(app);

        assertNotNull(response);
        assertEquals(1L, response.getApplicationId());
        assertTrue(response.isReachable());
        assertEquals(HealthStatus.HEALTHY, response.getStatus());
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getMessage().startsWith("HEALTHY:"));
        verify(applicationRepository, times(1)).save(app);
        verify(applicationMetricRepository, times(1)).save(any());
    }

    @Test
    void testHttpErrorApplication_ReturnsDegradedWithStatusCode() {
        Application app = new Application();
        app.setId(2L);
        app.setBaseUrl("http://localhost:" + serverPort + "/error/health");

        ConnectionTestResponse response = observationService.testConnection(app);

        assertNotNull(response);
        assertEquals(2L, response.getApplicationId());
        assertFalse(response.isReachable());
        assertEquals(HealthStatus.DEGRADED, response.getStatus());
        assertEquals(503, response.getStatusCode());
        assertTrue(response.getMessage().startsWith("HTTP_ERROR:"));
        verify(applicationRepository, times(1)).save(app);
    }

    @Test
    void testUnreachableHost_ReturnsUnreachableFailSafe() {
        Application app = new Application();
        app.setId(3L);
        app.setBaseUrl("http://127.0.0.1:59999"); // Closed port

        ConnectionTestResponse response = observationService.testConnection(app);

        assertNotNull(response);
        assertEquals(3L, response.getApplicationId());
        assertFalse(response.isReachable());
        assertEquals(HealthStatus.UNAVAILABLE, response.getStatus());
        assertTrue(response.getMessage().startsWith("UNREACHABLE:") || response.getMessage().startsWith("TIMEOUT:"));
        verify(applicationRepository, times(1)).save(app);
    }

    @Test
    void testMultipleApplications_OneFailureDoesNotAffectOthers() {
        Application goodApp = new Application();
        goodApp.setId(101L);
        goodApp.setBaseUrl("http://localhost:" + serverPort);

        Application badApp = new Application();
        badApp.setId(102L);
        badApp.setBaseUrl("http://127.0.0.1:59999");

        when(applicationRepository.findAllByActiveTrue()).thenReturn(List.of(goodApp, badApp));

        List<ConnectionTestResponse> responses = observationService.observeAllActiveApplications();

        assertEquals(2, responses.size());
        assertEquals(101L, responses.get(0).getApplicationId());
        assertTrue(responses.get(0).isReachable());
        assertEquals(HealthStatus.HEALTHY, responses.get(0).getStatus());

        assertEquals(102L, responses.get(1).getApplicationId());
        assertFalse(responses.get(1).isReachable());
        assertEquals(HealthStatus.UNAVAILABLE, responses.get(1).getStatus());
    }
}
