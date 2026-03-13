-- Test-only seed data. This migration lives under src/test/resources
-- and is only picked up by Flyway during test execution.
-- Password is BCrypt hash of "password123"
INSERT INTO users (id, email, password, role, created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'admin@ibms.test',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    NOW(),
    NOW()
);
