package com.ando.ibms.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ando.ibms.TestcontainersConfig;
import com.ando.ibms.auth.model.Role;
import com.ando.ibms.auth.model.User;
import com.ando.ibms.auth.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

/**
 * Integration tests for {@link UserRepository}.
 *
 * <p>Uses a Testcontainers PostgreSQL instance with Flyway migrations applied automatically. Each
 * test runs inside a transaction that is rolled back after completion, keeping tests isolated.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        savedUser =
                userRepository.save(
                        User.builder()
                                .email("broker@example.com")
                                .password("$2a$10$hashedpassword")
                                .role(Role.BROKER)
                                .build());
    }

    @Test
    void save_shouldPersistUserAndGenerateId() {
        assertThat(savedUser.getId()).isNotNull();
    }

    @Test
    void save_shouldPopulateTimestamps() {
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isEqualTo(savedUser.getUpdatedAt());
    }

    @Test
    void save_shouldUpdateTimestampOnModification() {
        Instant originalUpdatedAt = savedUser.getUpdatedAt();
        savedUser.setRole(Role.ADMIN);
        User updated = userRepository.saveAndFlush(savedUser);

        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void save_shouldRejectDuplicateEmail() {
        User duplicate =
                User.builder()
                        .email("broker@example.com")
                        .password("$2a$10$otherpassword")
                        .role(Role.OPERATOR)
                        .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByEmail_shouldReturnUserWhenExists() {
        Optional<User> found = userRepository.findByEmail("broker@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.BROKER);
    }

    @Test
    void findByEmail_shouldReturnEmptyWhenNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_shouldReturnTrueWhenExists() {
        assertThat(userRepository.existsByEmail("broker@example.com")).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalseWhenNotFound() {
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }
}
