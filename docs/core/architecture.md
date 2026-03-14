# IBMS — Architecture Reference

- **Last updated:** 2026-03-13
- **Scope:** POC and MVP phases on OCI Free Tier

---

## Deployment Architecture

The POC and MVP run on a single OCI ARM Ampere A1 VM with all services managed via Docker Compose. This approach prioritizes simplicity — one machine, one deployment unit, one place to debug — while containerization preserves portability for future migration.

```
OCI Free Tier (ARM Ampere A1 — 4 cores, 24 GB RAM)
├── Docker Compose
│   ├── Spring Boot API        (8 GB heap)
│   ├── PostgreSQL 16          (4 GB)
│   ├── Redis 7                (2 GB)
│   └── Nginx                  (reverse proxy, SSL termination)
│
├── OCI Object Storage         (10 GB — document uploads)
├── OCI Block Storage           (200 GB — OS, app, database)
└── OCI Load Balancer           (10 Mbps — SSL/TLS termination)
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

```
Internet
  │
  ▼
OCI Load Balancer (10 Mbps, SSL/TLS termination)
  │
  ▼
Nginx (reverse proxy inside VM)
  │
  ├── /api/v1/*          → Spring Boot :8080
  ├── /actuator/health   → Spring Boot :8080
  └── /*                 → Angular static files (future)
```

Outbound: 10 TB/month included in Free Tier — sufficient for any realistic MVP traffic pattern.

---

## Application Architecture

Package-by-feature organization inside `com.ibms`:

```
com.ibms
├── auth/          # JWT authentication (Sprint 1 — complete)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── broker/        # Broker CRUD (Sprint 2)
├── policy/        # Policy lifecycle (Sprint 3)
├── quote/         # Quote workflow (Sprint 4)
├── claim/         # Claims management (Sprint 5)
├── notification/  # Email + WhatsApp (Sprint 6)
└── common/
    ├── config/    # SecurityConfig, JwtProperties, JwtAuthenticationFilter
    └── exception/ # GlobalExceptionHandler
```

---

## Async Processing

Notification delivery and other async workloads use Redis Streams instead of a dedicated message broker. Redis is already in the stack for caching and refresh token storage; Streams adds pub/sub semantics without an additional container competing for memory.

```
Domain Event (e.g., PolicyActivated)
  → DomainEventPublisher writes to Redis Stream
    → NotificationConsumer (consumer group) reads events
      → NotificationRouter dispatches to channel
        → WhatsAppChannel (Meta Cloud API via RestClient)
        → EmailChannel (Spring Mail via SMTP)
```

Failed deliveries after 3 retries move to a dead letter stream. Admin endpoint `GET /api/v1/admin/notifications/failed` exposes inspection.

---

## Portability Strategy

Every technology choice targets portability over OCI-specific features:

| Layer    | Technology            | Lock-in risk |
| -------- | --------------------- | ------------ |
| Compute  | Docker containers     | None         |
| Database | PostgreSQL (standard) | None         |
| Cache    | Redis (standard)      | None         |
| Storage  | S3-compatible API     | Low          |
| IaC      | Terraform             | None         |
| CI/CD    | GitHub Actions        | None         |

Estimated migration effort if leaving OCI: 2–3 weeks, under R$ 5,000, zero code rewrite.

---

## Infrastructure Roadmap

```
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
