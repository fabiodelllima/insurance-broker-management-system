package com.ibms.auth.service;

import java.util.UUID;

/**
 * Abstraction for refresh token persistence and invalidation.
 *
 * <p>Implementations may use Redis, in-memory storage, or any other backing store. Tokens are
 * stored with a TTL and can be individually deleted (logout) or checked for existence (refresh
 * validation).
 */
public interface RefreshTokenStore {

    /** Stores a refresh token associated with a user, expiring after {@code ttlMs}. */
    void store(String token, UUID userId, long ttlMs);

    /** Returns {@code true} if the token exists and has not expired. */
    boolean exists(String token);

    /** Removes a single refresh token (logout). */
    void delete(String token);
}
