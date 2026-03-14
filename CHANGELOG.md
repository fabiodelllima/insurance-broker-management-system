# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- WhatsApp conversational claim intake via Meta Cloud API (Sprint WA-POC)
- `whatsapp` module: WebhookController, WhatsAppClient, ConversationEngine
- `claim` module foundation: Claim entity, ClaimRepository, Flyway migration
- ADR-004: WhatsApp Conversational POC — Prioritization and Approach
- BR-001: WhatsApp Claim Intake business requirements
- WhatsApp sandbox setup guide

---

## [0.1.0] — 2026-03-24

### Added

- **Auth module** — complete JWT authentication flow
  - `User` entity with BCrypt password hashing and `Role` enum
  - `JwtTokenService` for token generation, parsing, and validation
  - `JwtAuthenticationFilter` integrated into Spring Security filter chain
  - `POST /api/v1/auth/login` — email/password authentication returning JWT pair
  - `POST /api/v1/auth/refresh` — access token renewal via refresh token
  - `POST /api/v1/auth/logout` — refresh token invalidation (204 No Content)
  - `RefreshTokenStore` interface with `RedisRefreshTokenStore` and test-only `InMemoryRefreshTokenStore`
  - Flyway migration `V1__create_users_table.sql`
  - Test seed `V100__test_seed_users.sql`
- **Test infrastructure** — Testcontainers (PostgreSQL) for integration tests
- **Static analysis** — google-java-format (AOSP), Checkstyle, SpotBugs
- **Git hooks** — pre-commit (format + checkstyle), pre-push (full verify)
- **Documentation** — ADR-001 (monorepo), ADR-002 (static analysis), ADR-003 (WhatsApp strategy)
- 42 tests (11 unit + 31 integration), all passing

### Infrastructure

- Project scaffold: Spring Boot 3.5, Java 17, Maven, package-by-feature
- Docker Compose local stack: PostgreSQL 16, Redis 7, RabbitMQ, Nginx
- Dual-remote Git: GitLab (origin) + GitHub (github)
- EditorConfig for cross-editor consistency

---

[Unreleased]: https://github.com/delimafabio/insurance-broker-management-system/compare/v0.1.0...develop
[0.1.0]: https://github.com/delimafabio/insurance-broker-management-system/releases/tag/v0.1.0
