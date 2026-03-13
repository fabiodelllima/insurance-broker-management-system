-- Test-only seed data. This migration lives under src/test/resources
-- and is only picked up by Flyway during test execution.
-- Password is BCrypt hash of "password123"
INSERT INTO users (id, email, password, role, created_at, updated_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'admin@ibms.test',
    '$2b$10$uE7FrPHzLsobeQ/.V8KE/.g3dtCx.QYLsxMUwYIZu2.xOQ7YMco4S',
    'ADMIN',
    NOW(),
    NOW()
);
