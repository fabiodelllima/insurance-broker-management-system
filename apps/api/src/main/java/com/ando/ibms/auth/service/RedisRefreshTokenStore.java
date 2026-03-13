package com.ando.ibms.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-backed implementation of {@link RefreshTokenStore}.
 *
 * <p>Each refresh token is stored as a key {@code refresh_token:{token}} with the user ID as value
 * and a TTL matching the token expiration. Deletion removes the key immediately, preventing reuse
 * after logout.
 */
@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;

    /** Creates the store with the given Redis template. */
    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void store(String token, UUID userId, long ttlMs) {
        redisTemplate
                .opsForValue()
                .set(KEY_PREFIX + token, userId.toString(), Duration.ofMillis(ttlMs));
    }

    @Override
    public boolean exists(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }

    @Override
    public void delete(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }
}
