# IBMS — Sprint Planning

**Sprint duration:** 2 weeks

**Methodology:** Gitflow — feature branches per story, merged into `develop` via PR

---

## Current Status

- [x] Monorepo structure
- [x] Docker Compose (local stack)
- [x] Spring Boot scaffold
- [x] Package-by-feature structure
- [x] SecurityConfig (JWT)
- [x] GlobalExceptionHandler
- [x] HealthCheckController
- [x] Static analysis toolchain (google-java-format, Checkstyle, SpotBugs) — ADR-002
- [x] Git hooks (pre-commit formatting, pre-push verify)
- [x] EditorConfig
- [x] Auth module complete (Sprint 1) — merged to `main` as v0.1.0
- [ ] WhatsApp conversational POC — Sprint WA-POC (next)
- [ ] Angular scaffold — Sprint 7
- [ ] OCI account — Infrastructure Track

---

## Sprint 1 — Auth Module ✓

- **Period:** 2026-03-11 → 2026-03-24
- **Status:** Complete — merged to `main`, tagged `v0.1.0`
- **Tests:** 42 passing (11 unit + 31 integration)

### Stories

- [x] **S1-00 — Test infrastructure enablement**
      Testcontainers (PostgreSQL), `application-test.yml`, pre-push hook with Docker check.

- [x] **S1-01 — User entity and repository**
      `User` JPA entity, `Role` enum, `UserRepository`, Flyway `V1__create_users_table.sql`.

- [x] **S1-02 — JWT token service**
      `JwtTokenService`, `JwtProperties` (`security.jwt.*`).

- [x] **S1-03 — JWT filter and security chain**
      `JwtAuthenticationFilter`, `SecurityConfig` (stateless, CSRF off), `UserDetailsServiceImpl`, BCrypt `PasswordEncoder`, `AuthenticationManager`.

- [x] **S1-04 — Login endpoint**
      `POST /api/v1/auth/login`, `AuthService.login()`, `LoginRequest`/`AuthResponse` DTOs, seed `V100__test_seed_users.sql`.

- [x] **S1-05 — Refresh token endpoint**
      `POST /api/v1/auth/refresh`, `AuthService.refresh()`, `RefreshRequest` DTO.

- [x] **S1-06 — Logout endpoint**
      `RefreshTokenStore` interface, `RedisRefreshTokenStore` (`@ConditionalOnBean`), `InMemoryRefreshTokenStore` (`@TestConfiguration`), `POST /api/v1/auth/logout` → 204.

### Acceptance Criteria

- [x] All endpoints return correct HTTP status codes for valid and invalid inputs
- [x] Invalid credentials return `401`, expired tokens return `401`, missing token returns `401`
- [x] Integration tests pass for all happy paths and primary error scenarios
- [ ] No secrets hardcoded — all via environment variables (partial: test seeds use hardcoded values)

---

## Sprint WA-POC — WhatsApp Claim Intake

- **Period:** 2026-03-25 → 2026-04-07
- **Goal:** Stakeholder demo — client opens an insurance claim via WhatsApp conversation; system collects structured data and persists a claim record with protocol number.
- **Branch:** `feature/wa-poc-claim-intake`
- **ADR:** ADR-004
- **Requirements:** BR-001

### Pre-requisites (non-code)

- [ ] Meta Business Account created
- [ ] App created in Meta for Developers dashboard
- [ ] WhatsApp sandbox activated with test phone number
- [ ] Webhook URL accessible from internet (ngrok or cloudflared for dev)

### Stories

- [ ] **WA-01 — Meta Cloud API infrastructure**
      `WhatsAppProperties` configuration class (token, phone number ID, verify token, app secret). `WhatsAppClient` — `RestClient` wrapper for sending text messages via Graph API `POST /{phone_id}/messages`. `WebhookController` — public endpoint `GET /api/webhooks/whatsapp` for Meta verification challenge, `POST /api/webhooks/whatsapp` for inbound message reception. HMAC-SHA256 signature validation on POST payloads. `SecurityConfig` update to permit webhook endpoint without JWT. Unit tests for client and webhook signature verification.

- [ ] **WA-02 — Conversation engine**
      `ConversationState` enum (IDLE, AWAITING_POLICY, AWAITING_TYPE, AWAITING_DATE, AWAITING_DESCRIPTION, COMPLETED). `ConversationSession` — holds phone number, current state, collected data, timestamps. `ConversationStore` interface + `InMemoryConversationStore` implementation (ConcurrentHashMap). `ConversationEngine` — receives message text + session, validates input per state, advances state machine, returns response text. Input validation: occurrence type must be 1–5, date must be valid and not future, description max 500 chars. Handle "cancelar" keyword at any state. Handle unknown messages (help text). Session timeout: 30 minutes of inactivity. Unit tests covering full happy path, invalid inputs at each step, cancellation, timeout, and unknown messages.

- [ ] **WA-03 — Claim entity and persistence**
      `Claim` entity (id UUID, protocol, policy_number, claim_type enum, occurrence_date, description, reporter_phone, status, created_at, updated_at). `ClaimType` enum (COLLISION, THEFT, FIRE, NATURAL_EVENT, OTHER). `ClaimStatus` enum (OPENED — single status for POC). `ClaimRepository` extending `JpaRepository`. Flyway migration `V2__create_claims_table.sql`. Protocol generation: `SIN-YYYY-NNNN` (year + zero-padded sequential from DB sequence). Integration tests with Testcontainers: save, find by protocol, find by reporter phone.

- [ ] **WA-04 — End-to-end orchestration**
      `WhatsAppMessageHandler` — receives parsed webhook message, loads/creates session via `ConversationStore`, delegates to `ConversationEngine`, persists `Claim` when conversation completes, sends response via `WhatsAppClient`. Idempotency: deduplicate by Meta message ID to prevent duplicate claims from webhook retries. Spring wiring: component scan, dependency injection, configuration binding. Integration test of full flow (webhook POST in → engine → claim persisted → response out) with `WhatsAppClient` mocked via `@MockBean`.

- [ ] **WA-05 — Sandbox validation and documentation**
      Create `application-sandbox.yml` profile with Meta sandbox credentials placeholder. Test full flow manually: send message from personal WhatsApp → receive conversation → complete claim → verify in database. Document sandbox setup procedure in `docs/guides/whatsapp-sandbox-setup.md`. Document ngrok/cloudflared configuration for webhook exposure. Update architecture diagrams.

### Acceptance Criteria

- [ ] Sending "sinistro" to the WhatsApp number starts the claim intake conversation
- [ ] Invalid input at any step produces an error message and re-asks the question
- [ ] Sending "cancelar" at any step aborts the conversation
- [ ] Completing the conversation creates a `Claim` record in PostgreSQL
- [ ] Confirmation message includes protocol number in `SIN-YYYY-NNNN` format
- [ ] Duplicate webhook deliveries do not create duplicate claims
- [ ] Webhook endpoint validates HMAC signature and rejects unsigned requests
- [ ] Unknown messages receive a help response listing available commands
- [ ] All unit and integration tests pass via `./mvnw verify`

---

## Sprint 2 — Broker Module

- **Period:** 2026-04-08 → 2026-04-21
- **Goal:** Broker CRUD with pagination and search. Broker is the central aggregate — policy, quote, and claim all reference it.

### Stories

- [ ] **S2-01 — Broker entity and repository**
      Create `Broker` JPA entity: `id`, `name`, `document` (CNPJ), `email`, `phone`, `address`, `active`, `createdAt`, `updatedAt`. Implement `BrokerRepository` with custom query for search by name or document.

- [ ] **S2-02 — Broker DTOs and mapper**
      Create `BrokerRequest`, `BrokerResponse`, and `BrokerSummaryResponse` DTOs. Implement mapper (manual or MapStruct). Validate `@NotBlank`, `@Email`, CNPJ format on request DTO.

- [ ] **S2-03 — Broker service**
      Implement `BrokerService` with `create`, `update`, `findById`, `findAll` (paginated), `search`, `deactivate`. Throw domain-specific exceptions (`BrokerNotFoundException`, `DuplicateDocumentException`) handled by `GlobalExceptionHandler`.

- [ ] **S2-04 — Broker controller**
      Implement `BrokerController` with endpoints:
  - `POST /api/v1/brokers`
  - `GET /api/v1/brokers` (paginated, filterable)
  - `GET /api/v1/brokers/{id}`
  - `PUT /api/v1/brokers/{id}`
  - `DELETE /api/v1/brokers/{id}` (soft delete — sets `active = false`)

- [ ] **S2-05 — Integration tests**
      Integration tests for all endpoints covering: valid creation, duplicate document, not found, deactivation, pagination parameters.

### Acceptance Criteria

- [ ] Authenticated requests only (`Authorization: Bearer <token>`)
- [ ] Pagination defaults: `page=0`, `size=20`, sorted by `name` ASC
- [ ] Soft delete — records are never physically removed
- [ ] CNPJ validation on create and update

---

## Sprint 3 — Policy Module

- **Period:** 2026-04-22 → 2026-05-05
- **Goal:** Policy lifecycle management with status transitions. Most complex domain entity in the system.

### Stories

- [ ] **S3-01 — Policy entity and status machine**
      Create `Policy` JPA entity with lifecycle status: `DRAFT → ACTIVE → PENDING_RENEWAL → RENEWED → CANCELLED`. Map `@Enumerated` status field. Define valid transitions as domain logic in the entity or a dedicated `PolicyStatusTransition` class.

- [ ] **S3-02 — Policy DTOs and mapper**
      Create `PolicyRequest`, `PolicyResponse`, `PolicySummaryResponse`. Include nested `BrokerSummaryResponse`. Validate required fields, date ranges (`startDate` must be before `endDate`), and positive premium values.

- [ ] **S3-03 — Policy service**
      Implement `PolicyService` with `create`, `activate`, `renew`, `cancel`, `findById`, `findAllByBroker` (paginated). Enforce status transition rules — invalid transitions throw `InvalidPolicyStatusTransitionException`.

- [ ] **S3-04 — Policy controller**
      Implement `PolicyController`:
  - `POST /api/v1/policies`
  - `GET /api/v1/policies` (paginated, filterable by broker, status, date range)
  - `GET /api/v1/policies/{id}`
  - `PUT /api/v1/policies/{id}`
  - `PATCH /api/v1/policies/{id}/activate`
  - `PATCH /api/v1/policies/{id}/renew`
  - `PATCH /api/v1/policies/{id}/cancel`

- [ ] **S3-05 — Integration tests**
      Cover: creation, valid transitions, invalid transitions (expect `409`), filtering by status and broker, date range queries.

### Acceptance Criteria

- [ ] Invalid status transitions return `409 Conflict` with descriptive message
- [ ] Policy cannot be created without an existing active broker
- [ ] All date fields use ISO 8601 format

---

## Sprint 4 — Quote Module

- **Period:** 2026-05-06 → 2026-05-19
- **Goal:** Quote generation workflow with conversion to policy.

### Stories

- [ ] **S4-01 — Quote entity**
      Create `Quote` JPA entity with status: `DRAFT → SENT → ACCEPTED → REJECTED → CONVERTED`. Fields: `broker`, `clientName`, `clientDocument`, `coverageType`, `premium`, `validUntil`, `notes`.

- [ ] **S4-02 — Quote service and controller**
      Implement CRUD + status transitions. Key endpoint: `PATCH /api/v1/quotes/{id}/convert` — converts an accepted quote into a `DRAFT` policy, linking the two entities.

- [ ] **S4-03 — Quote-to-policy conversion**
      The conversion must be atomic — quote status updates to `CONVERTED` and policy is created in the same transaction. If policy creation fails, quote status must not change.

- [ ] **S4-04 — Integration tests**
      Cover: quote creation, acceptance flow, rejection flow, conversion to policy, attempt to convert non-accepted quote (expect `409`).

### Acceptance Criteria

- [ ] Quote conversion is atomic (transactional)
- [ ] Expired quotes (`validUntil` in the past) cannot be accepted
- [ ] Converted quotes are immutable

---

## Sprint 5 — Claim Module

- **Period:** 2026-05-20 → 2026-06-02
- **Goal:** Full claims module building on WA-POC foundation. Adds policy FK, document attachments, status machine, and REST API.

### Stories

- [ ] **S5-01 — Claim entity evolution**
      Migrate POC `Claim` entity: add `Policy` FK (nullable — preserves POC records), add `estimatedValue`, `finalValue`. Expand `ClaimStatus`: `OPENED → UNDER_REVIEW → APPROVED → REJECTED → CLOSED`. Flyway migration to alter existing table.

- [ ] **S5-02 — Claim service and controller**
      Implement CRUD + status transitions:
  - `POST /api/v1/claims`
  - `GET /api/v1/claims` (filterable by policy, status, date)
  - `PATCH /api/v1/claims/{id}/review`
  - `PATCH /api/v1/claims/{id}/approve`
  - `PATCH /api/v1/claims/{id}/reject`
  - `PATCH /api/v1/claims/{id}/close`

- [ ] **S5-03 — Document attachment**
      Implement `POST /api/v1/claims/{id}/documents` accepting multipart upload. Store files in local filesystem (POC) with path recorded in DB. Prepare abstraction for OCI Object Storage migration.

- [ ] **S5-04 — Integration tests**
      Cover: claim creation against valid policy, invalid policy (expect `404`), status transitions, invalid transitions, document upload.

### Acceptance Criteria

- [ ] Claims can only be opened against `ACTIVE` policies (via REST; WhatsApp intake remains text-based)
- [ ] `finalValue` is required when closing an approved claim
- [ ] Document storage abstracted behind an interface for future OCI migration
- [ ] Existing POC claims (with null policy FK) remain queryable

---

## Sprint 6 — Notifications

- **Period:** 2026-06-03 → 2026-06-16
- **Goal:** Async email and WhatsApp notifications triggered by domain events. Absorbs WA-POC WhatsApp client into full notification pipeline.

### Stories

- [ ] **S6-01 — Redis Streams producer**
      Implement `DomainEventPublisher` that writes events to Redis Streams. Events: `PolicyActivated`, `PolicyRenewed`, `PolicyCancelled`, `QuoteConverted`, `ClaimOpened`, `ClaimClosed`.

- [ ] **S6-02 — Notification consumer**
      Implement Redis Streams consumer group reading from the events stream. Route events to the appropriate notification handler.

- [ ] **S6-03 — Email notifications**
      Implement SMTP email sender via Spring Mail. Templates for: policy activation, renewal reminder, claim status update. Externalize SMTP credentials via environment variables.

- [ ] **S6-04 — WhatsApp notification channel**
      Promote `WhatsAppClient` from WA-POC to `WhatsAppChannel` implementing `NotificationChannel` interface. Add Resilience4j circuit breaker and retry with exponential backoff. Replace `InMemoryConversationStore` with `RedisConversationStore`.

- [ ] **S6-05 — Dead letter handling**
      Failed notifications after 3 retries move to a dead letter stream. Implement admin endpoint `GET /api/v1/admin/notifications/failed` to inspect failures.

### Acceptance Criteria

- [ ] Notification failures never affect the originating transaction
- [ ] All credentials externalized via environment variables
- [ ] Dead letter stream inspectable via admin endpoint
- [ ] WhatsApp conversation sessions survive application restart (Redis-backed)

---

## Sprint 7 — Angular Scaffold + Auth UI

- **Period:** 2026-06-17 → 2026-06-30
- **Goal:** Angular project running, routing configured, login page consuming the real API.

### Stories

- [ ] **S7-01 — Angular scaffold in apps/web**
- [ ] **S7-02 — Auth service and interceptor**
- [ ] **S7-03 — Login page**
- [ ] **S7-04 — Token refresh flow**

---

## Sprint 8 — Broker and Policy UI

**Period:** 2026-07-01 → 2026-07-14

- [ ] Broker list with pagination and search
- [ ] Broker detail and edit forms
- [ ] Policy list filterable by status and date range
- [ ] Policy detail with status timeline

---

## Sprint 9 — Quote and Claim UI

**Period:** 2026-07-15 → 2026-07-28

- [ ] Quote creation wizard
- [ ] Quote-to-policy conversion flow
- [ ] Claim intake form
- [ ] Claim status tracking view
- [ ] Document upload component

---

## Sprint 10 — Dashboard and Polish

**Period:** 2026-07-29 → 2026-08-11

- [ ] Operational dashboard with key metrics (active policies, open claims, pending quotes)
- [ ] Responsive layout
- [ ] Notification history view
- [ ] End-to-end QA pass

---

## Infrastructure Sprints

These sprints run independently and are gated on OCI account creation.

### Sprint I-1 — Terraform Foundation

- [ ] Provider config and versions
- [ ] Remote state on Object Storage
- [ ] Network module (VCN, subnets, security lists, internet gateway)
- [ ] Compute module (ARM Ampere A1, cloud-init)

### Sprint I-2 — Terraform Environments

- [ ] `poc/` environment consuming shared modules
- [ ] `mvp/` environment consuming shared modules
- [ ] Storage module (block volume, Object Storage bucket)
- [ ] Variables, outputs, tfvars.example

### Sprint I-3 — CI/CD

- [ ] GitHub Actions: build + test on PR
- [ ] Docker image build and push to registry
- [ ] SSH deploy to OCI VM with approval gate for `main`
- [ ] GitLab CI mirror pipelines
- [ ] Branch protection rules on both remotes

### Sprint I-4 — Observability

- [ ] Prometheus + Grafana stack
- [ ] Spring Actuator, node-exporter, cAdvisor
- [ ] Operational dashboards
- [ ] Alerting thresholds (CPU, RAM, storage, latency)
- [ ] Automated PostgreSQL backup to Object Storage
- [ ] Log rotation and disaster recovery runbook

---

**Last updated:** 2026-03-14
