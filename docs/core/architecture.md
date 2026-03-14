# IBMS — Architecture Reference

**Scope:** POC and MVP phases on OCI Free Tier

---

## Deployment Architecture

The POC and MVP run on a single OCI ARM Ampere A1 VM with all services managed via Docker Compose. This approach prioritizes simplicity — one machine, one deployment unit, one place to debug — while containerization preserves portability for future migration.

```plaintext
OCI Free Tier (ARM Ampere A1 — 4 cores, 24 GB RAM)
├── Docker Compose
│   ├── Spring Boot API        (8 GB heap)
│   ├── PostgreSQL 16          (4 GB)
│   ├── Redis 7                (2 GB)
│   └── Nginx                  (reverse proxy, SSL termination)
│
├── OCI Object Storage         (10 GB — document uploads)
├── OCI Block Storage          (200 GB — OS, app, database)
└── OCI Load Balancer          (10 Mbps — SSL/TLS termination)
```

Remaining memory (~10 GB) covers the operating system, Docker overhead, buffer cache, and headroom for load spikes.

---

## Memory Budget

| Component   | Allocation | Purpose                                     |
| ----------- | ---------- | ------------------------------------------- |
| Spring Boot | 8 GB       | JVM heap — generous to minimize GC pauses   |
| PostgreSQL  | 4 GB       | shared_buffers + work_mem + connections     |
| Redis       | 2 GB       | Cache + refresh token store + Redis Streams |
| Nginx       | ~128 MB    | Reverse proxy, static assets                |
| OS + Docker | ~2 GB      | Kernel, systemd, container runtime          |
| Headroom    | ~8 GB      | Buffer cache, spikes, maintenance tasks     |
| **Total**   | **24 GB**  |                                             |

---

## Storage Budget

| Type           | Capacity | Estimated Usage (MVP) | Headroom |
| -------------- | -------- | --------------------- | -------- |
| Block Storage  | 200 GB   | 80–100 GB             | ~100 GB  |
| Object Storage | 10 GB    | 5–7 GB                | 3–5 GB   |
| Database       | 40 GB    | 15–20 GB              | 20–25 GB |

Growth projection: the Free Tier comfortably supports up to ~150 active users before storage becomes a constraint.

---

## Network Architecture

```plaintext
Internet
  │
  ├── Meta Cloud API (outbound: send messages)
  │     POST https://graph.facebook.com/v21.0/{phone_id}/messages
  │
  ▼
OCI Load Balancer (10 Mbps, SSL/TLS termination)
  │
  ▼
Nginx (reverse proxy inside VM)
  │
  ├── /api/v1/*                → Spring Boot :8080  (JWT-protected)
  ├── /api/webhooks/whatsapp   → Spring Boot :8080  (HMAC-protected, public)
  ├── /actuator/health         → Spring Boot :8080  (public)
  └── /*                       → Angular static files (future)
  │
  ▲
  │
Meta Cloud API (inbound: webhook POST with message payloads)
```

The WhatsApp webhook endpoint is public (no JWT authentication) but protected by HMAC-SHA256 signature verification. Meta signs every webhook payload with a shared secret; the application validates this signature before processing.

During development, ngrok or cloudflared exposes the local webhook endpoint to the internet for Meta sandbox testing.

Outbound: 10 TB/month included in Free Tier — sufficient for any realistic MVP traffic pattern.

---

## Application Architecture

Package-by-feature organization inside `com.ibms`:

```plaintext
com.ibms
├── auth/              # JWT authentication (Sprint 1 — complete)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── broker/            # Broker CRUD (Sprint 2)
├── policy/            # Policy lifecycle (Sprint 3)
├── quote/             # Quote workflow (Sprint 4)
├── claim/             # Claims management (Sprint 5, foundation in WA-POC)
│   ├── model/         # Claim entity, ClaimType enum, ClaimStatus enum
│   └── repository/    # ClaimRepository
├── whatsapp/          # WhatsApp integration (WA-POC, evolves in Sprint 6)
│   ├── config/        # WhatsAppProperties
│   ├── client/        # WhatsAppClient (Meta Cloud API HTTP wrapper)
│   ├── webhook/       # WebhookController (message reception)
│   ├── conversation/  # ConversationEngine, ConversationSession, ConversationState
│   └── handler/       # WhatsAppMessageHandler (orchestration)
├── notification/      # Email + WhatsApp routing (Sprint 6)
└── common/
    ├── config/        # SecurityConfig, JwtProperties, JwtAuthenticationFilter
    └── exception/     # GlobalExceptionHandler
```

---

## WhatsApp Integration Architecture

The WhatsApp module follows the same interface abstraction pattern used throughout the project (see `RefreshTokenStore` in auth). External dependencies are accessed through interfaces, with concrete implementations injected by Spring.

### Message Flow — Inbound (WhatsApp → System)

```plaintext
Meta Cloud API
  │  POST /api/webhooks/whatsapp
  │  (JSON payload, X-Hub-Signature-256 header)
  ▼
WebhookController
  │  1. Validate HMAC signature
  │  2. Extract message text and sender phone
  ▼
WhatsAppMessageHandler
  │  3. Load or create ConversationSession for phone number
  │  4. Delegate to ConversationEngine
  ▼
ConversationEngine
  │  5. Evaluate current state + user input
  │  6. Advance state machine
  │  7. If COMPLETED: persist Claim via ClaimRepository
  │  8. Return response message text
  ▼
WhatsAppMessageHandler
  │  9. Send response via WhatsAppClient
  ▼
WhatsAppClient
  │  POST https://graph.facebook.com/v21.0/{phone_id}/messages
  ▼
Meta Cloud API → User's WhatsApp
```

### Message Flow — Outbound (System → WhatsApp)

In the POC, outbound messages are only sent as replies within the conversation flow. In Sprint 6, the `NotificationChannel` interface will enable proactive outbound notifications triggered by domain events.

```plaintext
Sprint 6 architecture (future):

Domain Event (e.g., PolicyActivated)
  => DomainEventPublisher writes to Redis Stream
    => NotificationConsumer reads event
      => NotificationRouter dispatches to channel
        => WhatsAppChannel (wraps WhatsAppClient + Resilience4j)
        => EmailChannel (Spring Mail via SMTP)
```

### Conversation State Management

During the POC, conversation sessions are stored in a `ConcurrentHashMap` behind a `ConversationStore` interface. This is sufficient for single-instance deployment and POC-level concurrency (10 simultaneous conversations).

In Sprint 6, `RedisConversationStore` replaces the in-memory implementation, enabling session persistence across restarts and multi-instance deployment. The pattern mirrors `RefreshTokenStore` → `RedisRefreshTokenStore`.

---

## Async Processing

Notification delivery and other async workloads use Redis Streams instead of a dedicated message broker. Redis is already in the stack for caching and refresh token storage; Streams adds pub/sub semantics without an additional container competing for memory.

```plaintext
Domain Event (e.g., PolicyActivated)
  => DomainEventPublisher writes to Redis Stream
    => NotificationConsumer (consumer group) reads events
      => NotificationRouter dispatches to channel
        => WhatsAppChannel (Meta Cloud API via RestClient)
        => EmailChannel (Spring Mail via SMTP)
```

Failed deliveries after 3 retries move to a dead letter stream. Admin endpoint `GET /api/v1/admin/notifications/failed` exposes inspection.

---

## Portability Strategy

Every technology choice targets portability over OCI-specific features:

| Layer    | Technology            | Lock-in risk                 |
| -------- | --------------------- | ---------------------------- |
| Compute  | Docker containers     | None                         |
| Database | PostgreSQL (standard) | None                         |
| Cache    | Redis (standard)      | None                         |
| Storage  | S3-compatible API     | Low                          |
| IaC      | Terraform             | None                         |
| CI/CD    | GitHub Actions        | None                         |
| WhatsApp | Meta Cloud API        | Low (BSP fallback available) |

Estimated migration effort if leaving OCI: 2–3 weeks, under R$ 5,000, zero code rewrite.

---

## Infrastructure Roadmap

```plaintext
Month 0–1: POC
├── OCI Free Tier (100% free)
├── Single VM, Docker Compose
└── Goal: Technical validation

Month 1–7: MVP
├── OCI Free Tier (100% free)
├── Same infrastructure as POC
├── Added: monitoring, CI/CD, backups
└── Goal: Market validation
```

Transition to paid infrastructure triggered by conditions defined in `risk-matrix.md`.

---

- **Last updated:** 2026-03-14
