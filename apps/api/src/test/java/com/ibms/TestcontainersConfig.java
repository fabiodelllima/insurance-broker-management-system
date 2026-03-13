package com.ibms;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers configuration for integration tests.
 *
 * <p>Provides a PostgreSQL container with {@link ServiceConnection} so Spring Boot auto-configures
 * the datasource and Flyway against the ephemeral database. Reuse this configuration via
 * {@code @Import(TestcontainersConfig.class)} in any test that needs a real database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
