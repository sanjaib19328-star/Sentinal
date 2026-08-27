package com.sentinel.api.service;

import com.sentinel.api.dto.ApiKeyResponse;
import com.sentinel.api.dto.CreateApiKeyRequest;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.ApiKey;
import com.sentinel.api.model.Application;
import com.sentinel.api.repository.ApiKeyRepository;
import com.sentinel.api.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private AuditLogService auditLogService;

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(apiKeyRepository, applicationRepository, auditLogService);
    }

    @Test
    void generateRawApiKey_shouldHaveSentinelPrefixAndEntropy() {
        String key1 = apiKeyService.generateRawApiKey();
        String key2 = apiKeyService.generateRawApiKey();

        assertThat(key1).startsWith("sk_sentinel_");
        assertThat(key2).startsWith("sk_sentinel_");
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1.length()).isGreaterThan(40);
    }

    @Test
    void hashKey_shouldProduceConsistentSha256Hex() {
        String rawKey = "sk_sentinel_test1234567890";
        String hash1 = apiKeyService.hashKey(rawKey);
        String hash2 = apiKeyService.hashKey(rawKey);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // 256 bits = 64 hex chars
    }

    @Test
    void createApiKey_shouldSaveHashedKeyAndReturnRawKeyOnce() {
        CreateApiKeyRequest request = new CreateApiKeyRequest("Test Client", 120, null);

        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey toSave = invocation.getArgument(0);
            toSave.setId(1L);
            toSave.setCreatedAt(Instant.now());
            return toSave;
        });

        ApiKeyResponse response = apiKeyService.createApiKey(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Client");
        assertThat(response.getApiKey()).startsWith("sk_sentinel_");
        assertThat(response.getRateLimitPerMinute()).isEqualTo(120);
        assertThat(response.getWarning()).isNotNull();

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey savedEntity = captor.getValue();

        assertThat(savedEntity.getKeyHash()).isNotEqualTo(response.getApiKey());
        assertThat(savedEntity.getKeyHash()).isEqualTo(apiKeyService.hashKey(response.getApiKey()));
        assertThat(savedEntity.isActive()).isTrue();
    }

    @Test
    void createApplicationApiKey_whenApplicationOwned_shouldSaveWithAppId() {
        Long ownerId = 1L;
        Long appId = 100L;
        CreateApiKeyRequest request = new CreateApiKeyRequest("App Key", 60, null);

        Application app = new Application(ownerId, "App", "Desc", "http://localhost");
        app.setId(appId);

        when(applicationRepository.findByIdAndOwnerId(appId, ownerId)).thenReturn(Optional.of(app));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey toSave = invocation.getArgument(0);
            toSave.setId(10L);
            toSave.setCreatedAt(Instant.now());
            return toSave;
        });

        ApiKeyResponse response = apiKeyService.createApplicationApiKey(ownerId, appId, request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("App Key");
        assertThat(response.getApiKey()).startsWith("sk_sentinel_");

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo(appId);
    }

    @Test
    void createApplicationApiKey_whenApplicationNotOwned_shouldThrowNotFound() {
        Long ownerId = 1L;
        Long appId = 999L;
        CreateApiKeyRequest request = new CreateApiKeyRequest("App Key", 60, null);

        when(applicationRepository.findByIdAndOwnerId(appId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.createApplicationApiKey(ownerId, appId, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listApplicationApiKeys_whenOwned_shouldReturnKeysWithoutRawKey() {
        Long ownerId = 1L;
        Long appId = 100L;

        Application app = new Application(ownerId, "App", "Desc", "http://localhost");
        app.setId(appId);

        ApiKey key1 = new ApiKey(appId, "Key 1", "hash1", 60, null);
        key1.setId(1L);
        key1.setCreatedAt(Instant.now());

        when(applicationRepository.findByIdAndOwnerId(appId, ownerId)).thenReturn(Optional.of(app));
        when(apiKeyRepository.findByApplicationId(appId)).thenReturn(List.of(key1));

        List<ApiKeyResponse> responses = apiKeyService.listApplicationApiKeys(ownerId, appId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Key 1");
        assertThat(responses.get(0).getApiKey()).isNull(); // Never return raw key on list
    }

    @Test
    void validateKey_withValidActiveKey_shouldReturnApiKey() {
        String rawKey = "sk_sentinel_validkey123";
        String hash = apiKeyService.hashKey(rawKey);

        ApiKey apiKey = new ApiKey("Client", hash, 60, null);
        apiKey.setId(10L);
        apiKey.setActive(true);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

        Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
    }

    @Test
    void validateKey_withUnknownKey_shouldReturnEmpty() {
        String rawKey = "sk_sentinel_unknown";
        String hash = apiKeyService.hashKey(rawKey);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.empty());

        Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

        assertThat(result).isEmpty();
    }

    @Test
    void validateKey_withInactiveKey_shouldReturnEmpty() {
        String rawKey = "sk_sentinel_inactive";
        String hash = apiKeyService.hashKey(rawKey);

        ApiKey apiKey = new ApiKey("Client", hash, 60, null);
        apiKey.setId(11L);
        apiKey.setActive(false);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

        Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

        assertThat(result).isEmpty();
    }

    @Test
    void validateKey_withExpiredKey_shouldReturnEmpty() {
        String rawKey = "sk_sentinel_expired";
        String hash = apiKeyService.hashKey(rawKey);

        ApiKey apiKey = new ApiKey("Client", hash, 60, Instant.now().minus(1, ChronoUnit.DAYS));
        apiKey.setId(12L);
        apiKey.setActive(true);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

        Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

        assertThat(result).isEmpty();
    }

    @Test
    void validateKey_withNullOrBlank_shouldReturnEmpty() {
        assertThat(apiKeyService.validateKey(null)).isEmpty();
        assertThat(apiKeyService.validateKey("")).isEmpty();
        assertThat(apiKeyService.validateKey("   ")).isEmpty();
    }
}
