package com.ibms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that verifies the Spring application context loads successfully.
 *
 * <p>Uses Testcontainers for PostgreSQL and disables Redis/RabbitMQ autoconfiguration via the
 * {@code test} profile.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, com.ibms.InMemoryRefreshTokenStore.class})
class IbmsApiApplicationTests {

    @Test
    void contextLoads() {}
}
