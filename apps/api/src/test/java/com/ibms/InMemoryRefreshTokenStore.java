package com.ibms;

import com.ibms.auth.service.RefreshTokenStore;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link RefreshTokenStore} for integration tests.
 *
 * <p>Replaces the Redis-backed store when the {@code test} profile is active, since Redis
 * autoconfiguration is excluded in that profile.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InMemoryRefreshTokenStore {

    @Bean
    @Primary
    RefreshTokenStore refreshTokenStore() {
        return new RefreshTokenStore() {
            private final Map<String, String> store = new ConcurrentHashMap<>();

            @Override
            public void store(String token, UUID userId, long ttlMs) {
                store.put(token, userId.toString());
            }

            @Override
            public boolean exists(String token) {
                return store.containsKey(token);
            }

            @Override
            public void delete(String token) {
                store.remove(token);
            }
        };
    }
}
