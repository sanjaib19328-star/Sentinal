package com.sentinel.api.service;

import com.sentinel.api.dto.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Test
    void checkRateLimit_whenFirstRequest_shouldSetExpiryAndAllow() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        RateLimitResult result = rateLimitService.checkRateLimit(1L, 100);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getLimit()).isEqualTo(100);
        assertThat(result.getRemaining()).isEqualTo(99);
        assertThat(result.getResetEpochSeconds()).isGreaterThan(0);
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void checkRateLimit_whenWithinLimit_shouldAllow() {
        when(valueOperations.increment(anyString())).thenReturn(50L);

        RateLimitResult result = rateLimitService.checkRateLimit(1L, 100);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(50);
    }

    @Test
    void checkRateLimit_whenExactlyAtLimit_shouldAllowWithZeroRemaining() {
        when(valueOperations.increment(anyString())).thenReturn(100L);

        RateLimitResult result = rateLimitService.checkRateLimit(1L, 100);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(0);
    }

    @Test
    void checkRateLimit_whenExceedsLimit_shouldDisallow() {
        when(valueOperations.increment(anyString())).thenReturn(101L);

        RateLimitResult result = rateLimitService.checkRateLimit(1L, 100);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isEqualTo(0);
        assertThat(result.getRetryAfterSeconds()).isGreaterThan(0);
    }
}
