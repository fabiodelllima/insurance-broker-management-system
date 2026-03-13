# IBMS — Sprint Planning

- **Last updated:** 2026-03-13
- **Sprint duration:** 2 weeks
- **Methodology:** Gitflow — feature branches per story, merged into `develop` via PR

---

## Current Status

- [x] Monorepo structure
- [x] Docker Compose (local stack)
- [x] Spring Boot scaffold
- [x] Package-by-feature structure
- [x] SecurityConfig (JWT skeleton)
- [x] GlobalExceptionHandler
- [x] HealthCheckController
- [x] Static analysis toolchain (google-java-format, Checkstyle, SpotBugs) — ADR-002
- [x] Git hooks (pre-commit formatting, pre-push verify)
- [x] EditorConfig
- [ ] Angular scaffold — Sprint 7
- [ ] OCI account — Infrastructure Track

---

## Sprint 1 — Auth Module

**Period:** 2026-03-11 → 2026-03-24
**Goal:** Fully operational JWT authentication. All subsequent API endpoints depend on this.

### Stories

- [x] **S1-00 — Test infrastructure enablement**
      Configure `IbmsApiApplicationTests` to use Testcontainers (PostgreSQL) so that `./mvnw verify` passes without an external database. Create `application-test.yml` profile if needed. Adjust pre-push hook to account for Docker requirement or skip integration tests locally and rely on CI.

- [ ] **S1-01 — User entity and repository**
      Create the `User` JPA entity with fields `id`, `email`, `password` (BCrypt), `role`, `createdAt`, `updatedAt`. Implement `UserRepository` extending `JpaRepository`. Write unit tests for repository layer.

- [ ] **S1-02 — JWT token service**
      Implement `JwtTokenService` responsible for token generation, parsing, and validation. Token claims must include `sub` (user ID), `email`, `role`, and `exp`. Externalize secret and expiration via `application.yml` environment variables.

- [ ] **S1-03 — JWT filter and security chain**
      Implement `JwtAuthenticationFilter` extending `OncePerRequestFilter`. Wire it into `SecurityConfig` to validate tokens on every request. Public routes: `/actuator/health`, `/api/v1/auth/**`.

- [ ] **S1-04 — Login endpoint**
      Implement `POST /api/v1/auth/login` accepting `{ email, password }`. Returns `{ accessToken, refreshToken, expiresIn }`. Write integration test using `@SpringBootTest` and `MockMvc`.

- [ ] **S1-05 — Refresh token endpoint**
      Implement `POST /api/v1/auth/refresh` accepting `{ refreshToken }`. Returns new `accessToken`. Persist refresh tokens in Redis with TTL.

- [ ] **S1-06 — Logout endpoint**
      Implement `POST /api/v1/auth/logout`. Invalidate refresh token in Redis. Return `204 No Content`.

### Acceptance Criteria

- [ ] All endpoints return correct HTTP status codes for valid and invalid inputs
- [ ] Invalid credentials return `401`, expired tokens return `401`, missing token returns `401`
- [ ] Integration tests pass for all happy paths and primary error scenarios
- [ ] No secrets hardcoded — all via environment variables

---

## Sprint 2 — Broker Module

**Period:** 2026-03-25 → 2026-04-07
**Goal:** Broker CRUD with pagination and search. Broker is the central aggregate — policy, quote, and claim all reference it.

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

**Period:** 2026-04-08 → 2026-04-21
**Goal:** Policy lifecycle management with status transitions. Most complex domain entity in the system.

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

**Period:** 2026-04-22 → 2026-05-05
**Goal:** Quote generation workflow with conversion to policy.

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

**Period:** 2026-05-06 → 2026-05-19
**Goal:** Claims intake and tracking with document attachment support.

### Stories

- [ ] **S5-01 — Claim entity and status machine**
      Create `Claim` entity with status: `OPENED → UNDER_REVIEW → APPROVED → REJECTED → CLOSED`. Link to `Policy`. Fields: `occurrenceDate`, `description`, `claimType`, `estimatedValue`, `finalValue`.

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

- [ ] Claims can only be opened against `ACTIVE` policies
- [ ] `finalValue` is required when closing an approved claim
- [ ] Document storage abstracted behind an interface for future OCI migration

---

## Sprint 6 — Notifications

**Period:** 2026-05-20 → 2026-06-02
**Goal:** Async email and WhatsApp notifications triggered by domain events.

### Stories

- [ ] **S6-01 — Redis Streams producer**
      Implement `DomainEventPublisher` that writes events to Redis Streams. Events: `PolicyActivated`, `PolicyRenewed`, `PolicyCancelled`, `QuoteConverted`, `ClaimOpened`, `ClaimClosed`.

- [ ] **S6-02 — Notification consumer**
      Implement Redis Streams consumer group reading from the events stream. Route events to the appropriate notification handler.

- [ ] **S6-03 — Email notifications**
      Implement SMTP email sender via Spring Mail. Templates for: policy activation, renewal reminder, claim status update. Externalize SMTP credentials via environment variables.

- [ ] **S6-04 — WhatsApp notifications**
      Implement WhatsApp sender via HTTP client against WhatsApp Business API. Implement circuit breaker (Resilience4j) to prevent API failures from affecting the main application flow.

- [ ] **S6-05 — Dead letter handling**
      Failed notifications after 3 retries move to a dead letter stream. Implement admin endpoint `GET /api/v1/admin/notifications/failed` to inspect failures.

### Acceptance Criteria

- [ ] Notification failures never affect the originating transaction
- [ ] All credentials externalized via environment variables
- [ ] Dead letter stream inspectable via admin endpoint

---

## Sprint 7 — Angular Scaffold + Auth UI

**Period:** 2026-06-03 → 2026-06-16
**Goal:** Angular project running, routing configured, login page consuming the real API.

### Stories

- [ ] **S7-01 — Angular scaffold in apps/web**
      Generate Angular project with `ng new`. Configure standalone components, routing, and HTTP client. Set up proxy configuration for local development against `apps/api` at `localhost:8080`.

- [ ] **S7-02 — Auth service and interceptor**
      Implement `AuthService` handling login, logout, and token storage (memory + sessionStorage). Implement `JwtInterceptor` attaching `Authorization` header to all API requests. Implement `AuthGuard` protecting routes.

- [ ] **S7-03 — Login page**
      Implement login form with email/password fields, validation, error display, and loading state. On success, redirect to dashboard.

- [ ] **S7-04 — Token refresh flow**
      Implement automatic token refresh via HTTP interceptor — intercept `401` responses, attempt refresh, retry original request. On refresh failure, redirect to login.

### Acceptance Criteria

- [ ] Login with valid credentials navigates to dashboard
- [ ] Login with invalid credentials shows error message
- [ ] Protected routes redirect unauthenticated users to login
- [ ] Token refresh is transparent to the user

---

## Sprint 8 — Broker and Policy UI

**Period:** 2026-06-17 → 2026-06-30

- [ ] Broker list with pagination and search
- [ ] Broker detail and edit forms
- [ ] Policy list filterable by status and date range
- [ ] Policy detail with status timeline

---

## Sprint 9 — Quote and Claim UI

**Period:** 2026-07-01 → 2026-07-14

- [ ] Quote creation wizard
- [ ] Quote-to-policy conversion flow
- [ ] Claim intake form
- [ ] Claim status tracking view
- [ ] Document upload component

---

## Sprint 10 — Dashboard and Polish

**Period:** 2026-07-15 → 2026-07-28

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
