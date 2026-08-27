package com.sentinel.api.service;

import com.sentinel.api.dto.RateLimitResult;
import com.sentinel.api.model.ApiPolicy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RateLimitService {

    private static final String RATE_LIMIT_PREFIX = "sentinel:rate-limit:";
    private static final String APP_RATE_LIMIT_PREFIX = "sentinel:rate-limit:app:";
    private static final String ENDPOINT_RATE_LIMIT_PREFIX = "sentinel:rate-limit:endpoint:";
    private static final String QUOTA_PREFIX = "sentinel:quota:";
    private static final Duration WINDOW_TTL = Duration.ofSeconds(75);

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RateLimitResult checkRateLimit(Long apiKeyId, int limitPerMinute) {
        return checkWindowLimit(RATE_LIMIT_PREFIX + apiKeyId, limitPerMinute, 60, "API_KEY");
    }

    public RateLimitResult checkWindowLimit(String keyPrefix, int limit, int windowSeconds, String throttledBy) {
        if (limit <= 0) {
            return RateLimitResult.allow(Long.MAX_VALUE, Long.MAX_VALUE, Instant.now().getEpochSecond() + 60);
        }
        int effectiveWindow = windowSeconds > 0 ? windowSeconds : 60;
        long currentEpochSecond = Instant.now().getEpochSecond();
        long windowIndex = currentEpochSecond / effectiveWindow;
        long resetEpochSeconds = (windowIndex + 1) * effectiveWindow;
        long retryAfterSeconds = Math.max(1, resetEpochSeconds - currentEpochSecond);

        String redisKey = keyPrefix + ":" + windowIndex;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(effectiveWindow + 15));
        }

        long count = currentCount != null ? currentCount : 1;
        boolean allowed = count <= limit;
        long remaining = Math.max(0, limit - count);

        if (!allowed) {
            return RateLimitResult.deny(limit, resetEpochSeconds, retryAfterSeconds, throttledBy);
        }

        return new RateLimitResult(true, limit, remaining, resetEpochSeconds, 0, throttledBy);
    }

    public RateLimitResult checkQuota(String keyPrefix, int quotaLimit, int quotaWindowSeconds, String throttledBy) {
        if (quotaLimit <= 0) {
            return RateLimitResult.allow(Long.MAX_VALUE, Long.MAX_VALUE, Instant.now().getEpochSecond() + 60);
        }
        int effectiveWindow = quotaWindowSeconds > 0 ? quotaWindowSeconds : 86400; // default 1 day
        long currentEpochSecond = Instant.now().getEpochSecond();
        long windowIndex = currentEpochSecond / effectiveWindow;
        long resetEpochSeconds = (windowIndex + 1) * effectiveWindow;
        long retryAfterSeconds = Math.max(1, resetEpochSeconds - currentEpochSecond);

        String redisKey = QUOTA_PREFIX + keyPrefix + ":" + windowIndex;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(effectiveWindow + 60));
        }

        long count = currentCount != null ? currentCount : 1;
        boolean allowed = count <= quotaLimit;
        long remaining = Math.max(0, quotaLimit - count);

        if (!allowed) {
            return RateLimitResult.deny(quotaLimit, resetEpochSeconds, retryAfterSeconds, throttledBy);
        }

        return new RateLimitResult(true, quotaLimit, remaining, resetEpochSeconds, 0, throttledBy);
    }

    /**
     * Multi-level rate limit and quota evaluation:
     * 1. API Key level limit
     * 2. Application-level policy limit and quota (if configured & enabled)
     * 3. Endpoint-level policy limit and quota (if configured & enabled)
     *
     * Enforces the most restrictive applicable policy.
     */
    public RateLimitResult evaluateMultiLevel(
        Long apiKeyId,
        int keyLimitPerMinute,
        Long applicationId,
        ApiPolicy appPolicy,
        Long endpointId,
        ApiPolicy endpointPolicy
    ) {
        long minLimit = keyLimitPerMinute;
        long minRemaining = Long.MAX_VALUE;
        long targetResetEpoch = Instant.now().getEpochSecond() + 60;

        // 1. Check API Key Rate Limit
        if (apiKeyId != null && keyLimitPerMinute > 0) {
            RateLimitResult keyRes = checkRateLimit(apiKeyId, keyLimitPerMinute);
            if (!keyRes.isAllowed()) {
                return keyRes;
            }
            minLimit = Math.min(minLimit, keyRes.getLimit());
            minRemaining = Math.min(minRemaining, keyRes.getRemaining());
            targetResetEpoch = keyRes.getResetEpochSeconds();
        }

        // 2. Check Application Policy Rate Limit & Quota
        if (appPolicy != null && appPolicy.isEnabled()) {
            if (appPolicy.getRateLimit() > 0) {
                RateLimitResult appRes = checkWindowLimit(
                    APP_RATE_LIMIT_PREFIX + applicationId,
                    appPolicy.getRateLimit(),
                    appPolicy.getRateWindowSeconds(),
                    "APPLICATION_POLICY"
                );
                if (!appRes.isAllowed()) {
                    return appRes;
                }
                minLimit = Math.min(minLimit, appRes.getLimit());
                minRemaining = Math.min(minRemaining, appRes.getRemaining());
                targetResetEpoch = Math.min(targetResetEpoch, appRes.getResetEpochSeconds());
            }

            if (appPolicy.getQuotaLimit() != null && appPolicy.getQuotaLimit() > 0) {
                int qWindow = appPolicy.getQuotaWindowSeconds() != null ? appPolicy.getQuotaWindowSeconds() : 86400;
                RateLimitResult appQuotaRes = checkQuota(
                    "app:" + applicationId,
                    appPolicy.getQuotaLimit(),
                    qWindow,
                    "APPLICATION_QUOTA"
                );
                if (!appQuotaRes.isAllowed()) {
                    return appQuotaRes;
                }
                minRemaining = Math.min(minRemaining, appQuotaRes.getRemaining());
            }
        }

        // 3. Check Endpoint Policy Rate Limit & Quota
        if (endpointPolicy != null && endpointPolicy.isEnabled() && endpointId != null) {
            if (endpointPolicy.getRateLimit() > 0) {
                RateLimitResult epRes = checkWindowLimit(
                    ENDPOINT_RATE_LIMIT_PREFIX + endpointId,
                    endpointPolicy.getRateLimit(),
                    endpointPolicy.getRateWindowSeconds(),
                    "ENDPOINT_POLICY"
                );
                if (!epRes.isAllowed()) {
                    return epRes;
                }
                minLimit = Math.min(minLimit, epRes.getLimit());
                minRemaining = Math.min(minRemaining, epRes.getRemaining());
                targetResetEpoch = Math.min(targetResetEpoch, epRes.getResetEpochSeconds());
            }

            if (endpointPolicy.getQuotaLimit() != null && endpointPolicy.getQuotaLimit() > 0) {
                int qWindow = endpointPolicy.getQuotaWindowSeconds() != null ? endpointPolicy.getQuotaWindowSeconds() : 86400;
                RateLimitResult epQuotaRes = checkQuota(
                    "endpoint:" + endpointId,
                    endpointPolicy.getQuotaLimit(),
                    qWindow,
                    "ENDPOINT_QUOTA"
                );
                if (!epQuotaRes.isAllowed()) {
                    return epQuotaRes;
                }
                minRemaining = Math.min(minRemaining, epQuotaRes.getRemaining());
            }
        }

        return new RateLimitResult(true, minLimit, minRemaining == Long.MAX_VALUE ? minLimit - 1 : minRemaining, targetResetEpoch, 0, null);
    }
}
