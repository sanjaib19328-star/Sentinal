package com.sentinel.api.service;

import com.sentinel.api.dto.PreparedUpstreamRequest;
import com.sentinel.api.dto.UpstreamAuthConfigRequest;
import com.sentinel.api.dto.UpstreamAuthConfigResponse;
import com.sentinel.api.exception.BadRequestException;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.UpstreamAuthType;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.security.CredentialEncryptionService;
import com.sentinel.api.security.SsrfProtectionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UpstreamAuthenticationServiceTest {

    private ApplicationRepository applicationRepository;
    private CredentialEncryptionService encryptionService;
    private SsrfProtectionValidator ssrfValidator;
    private AuditLogService auditLogService;
    private UpstreamAuthenticationService upstreamAuthService;

    @BeforeEach
    void setUp() {
        applicationRepository = Mockito.mock(ApplicationRepository.class);
        encryptionService = new CredentialEncryptionService("dev-encryption-key-for-tests-32b");
        ssrfValidator = Mockito.mock(SsrfProtectionValidator.class);
        auditLogService = Mockito.mock(AuditLogService.class);

        upstreamAuthService = new UpstreamAuthenticationService(
            applicationRepository,
            encryptionService,
            ssrfValidator,
            auditLogService
        );
    }

    @Test
    void testValidateConfigurationValidation() {
        // Bearer missing secret
        UpstreamAuthConfigRequest reqBearer = new UpstreamAuthConfigRequest();
        reqBearer.setType(UpstreamAuthType.BEARER_TOKEN);
        assertThrows(BadRequestException.class, () -> upstreamAuthService.validateConfiguration(reqBearer));

        // API Key Header missing headerName
        UpstreamAuthConfigRequest reqHdr = new UpstreamAuthConfigRequest();
        reqHdr.setType(UpstreamAuthType.API_KEY_HEADER);
        reqHdr.setSecret("secret");
        assertThrows(BadRequestException.class, () -> upstreamAuthService.validateConfiguration(reqHdr));

        // Basic Auth missing password
        UpstreamAuthConfigRequest reqBasic = new UpstreamAuthConfigRequest();
        reqBasic.setType(UpstreamAuthType.BASIC_AUTH);
        reqBasic.setUsername("admin");
        assertThrows(BadRequestException.class, () -> upstreamAuthService.validateConfiguration(reqBasic));
    }

    @Test
    void testConfigureAndMaskBearerToken() {
        Application app = new Application(1L, "App", "Desc", "http://localhost:9090");
        app.setId(100L);
        when(applicationRepository.findByIdAndOwnerId(100L, 1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        UpstreamAuthConfigRequest req = new UpstreamAuthConfigRequest();
        req.setType(UpstreamAuthType.BEARER_TOKEN);
        req.setSecret("super-secret-token-xyz");

        UpstreamAuthConfigResponse res = upstreamAuthService.configureAuthentication(1L, 100L, req);

        assertEquals(UpstreamAuthType.BEARER_TOKEN, res.getType());
        assertTrue(res.isEnabled());
        assertTrue(res.isConfigured());
        assertEquals("••••••••", res.getMaskedSecret());
        assertNull(res.getHeaderName());
    }

    @Test
    void testPrepareTargetRequestBearerToken() {
        Application app = new Application(1L, "App", "Desc", "http://api.example.com");
        app.setId(100L);
        app.setUpstreamAuthType(UpstreamAuthType.BEARER_TOKEN);
        app.setUpstreamAuthEnabled(true);
        app.setUpstreamAuthConfigEncrypted(encryptionService.encrypt("{\"type\":\"BEARER_TOKEN\",\"secret\":\"token-123\"}"));

        PreparedUpstreamRequest prepared = upstreamAuthService.prepareTargetRequest("http://api.example.com", "/users", "page=1", app);

        assertEquals("http://api.example.com/users?page=1", prepared.getTargetUri().toString());
        assertEquals("Bearer token-123", prepared.getHeaders().get("Authorization"));
    }

    @Test
    void testPrepareTargetRequestApiKeyQuery() {
        Application app = new Application(1L, "App", "Desc", "http://api.example.com");
        app.setId(100L);
        app.setUpstreamAuthType(UpstreamAuthType.API_KEY_QUERY);
        app.setUpstreamAuthEnabled(true);
        app.setUpstreamAuthConfigEncrypted(encryptionService.encrypt("{\"type\":\"API_KEY_QUERY\",\"queryParamName\":\"api_key\",\"secret\":\"secret-query-token\"}"));

        // With existing query params
        PreparedUpstreamRequest prepared = upstreamAuthService.prepareTargetRequest("http://api.example.com", "/users", "filter=active", app);
        assertEquals("http://api.example.com/users?filter=active&api_key=secret-query-token", prepared.getTargetUri().toString());

        // Without existing query params
        PreparedUpstreamRequest prepared2 = upstreamAuthService.prepareTargetRequest("http://api.example.com", "/users", null, app);
        assertEquals("http://api.example.com/users?api_key=secret-query-token", prepared2.getTargetUri().toString());
    }
}
