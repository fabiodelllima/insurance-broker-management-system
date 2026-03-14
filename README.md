# Insurance Broker Management System

![Status](https://img.shields.io/badge/status-active_development-blue)
![Version](https://img.shields.io/badge/version-0.1.0-green)
![Java](https://img.shields.io/badge/Java-17-red?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green?logo=springboot)
![Angular](https://img.shields.io/badge/Angular-TBD-red?logo=angular)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)

A fullstack CRM/ERP platform for insurance brokerages. Automates policy lifecycle management, client relationships, quoting, and claims processing — replacing fragmented spreadsheets and manual workflows with a unified, auditable system.

---

## Architecture Overview

```plaintext
insurance-broker-management-system/
├── apps/
│   ├── api/           # Spring Boot 3.5 — REST API, domain logic, messaging
│   └── web/           # Angular — SPA client (planned)
├── docs/
│   ├── core/          # Architecture, roadmap, sprints, risk matrix
│   ├── decisions/     # Architecture Decision Records (ADRs)
│   ├── guides/        # Setup guides and operational runbooks
│   └── requirements/  # Business requirements documents
├── config/            # Checkstyle, SpotBugs configuration
└── scripts/           # API test scripts (login, refresh, protected, etc.)
```

The backend follows a **package-by-feature** structure organized around insurance domain modules: `auth`, `broker`, `policy`, `quote`, `claim`, `whatsapp`, and `common`. This keeps domain logic cohesive and avoids the cross-cutting dependencies typical of package-by-layer approaches.

Infrastructure (Docker Compose, Terraform, CI/CD pipelines) lives in a separate repository with its own lifecycle.

---

## Tech Stack

### **Backend**

- Java 17 + Spring Boot 3.5
- Spring Security (JWT, stateless)
- Spring Data JPA + PostgreSQL
- Spring AMQP + RabbitMQ (planned)
- Redis (distributed cache, refresh token store)
- Maven (build tooling)

### **Frontend**

- Angular (planned)
- Nginx (reverse proxy + static serving)

### **Integrations**

- Meta WhatsApp Cloud API (conversational claim intake)

### **Infrastructure** _(separate repo)_

- Oracle Cloud Infrastructure — Always Free Tier
- Docker + Docker Compose
- Terraform (IaC)
- GitHub Actions + GitLab CI

---

## Domain Modules

| Module     | Responsibility                                        | Status      |
| ---------- | ----------------------------------------------------- | ----------- |
| `auth`     | Authentication, authorization, JWT issuance           | Complete    |
| `whatsapp` | WhatsApp Cloud API, conversational workflows          | In progress |
| `broker`   | Brokerage and agent profile management                | Planned     |
| `policy`   | Policy lifecycle — issuance, renewal, cancellation    | Planned     |
| `quote`    | Quote generation and comparison workflows             | Planned     |
| `claim`    | Claims intake, tracking, and resolution               | Foundation  |
| `common`   | Shared utilities, exception handling, security config | Complete    |

---

## Getting Started

### Prerequisites

- Java 17 (`sdk use java 17` via SDKMAN)
- Docker + Docker Compose
- Maven 3.9+

### Running tests

```bash
cd apps/api
./mvnw verify
```

Tests use Testcontainers — Docker must be running. No external infrastructure required.

### Running locally

```bash
# Start infrastructure services
cd infra && docker compose up -d

# Run the API
cd apps/api
./mvnw spring-boot:run
```

Health check: `GET http://localhost:8080/actuator/health`

---

## Development Status

This repository tracks the **MVP phase** of the project. Active development is happening on the `develop` branch following [Gitflow](https://nvie.com/posts/a-successful-git-branching-model/).

| Component                 | Status      | Sprint |
| ------------------------- | ----------- | ------ |
| API scaffolding           | ✓ Complete  | —      |
| Domain module structure   | ✓ Complete  | —      |
| Static analysis toolchain | ✓ Complete  | —      |
| JWT authentication        | ✓ Complete  | S1     |
| WhatsApp claim intake     | In progress | WA-POC |
| Broker CRUD               | Planned     | S2     |
| Policy lifecycle          | Planned     | S3     |
| Quote workflow            | Planned     | S4     |
| Claim module (full)       | Planned     | S5     |
| Notifications             | Planned     | S6     |
| Angular frontend          | Planned     | S7–S10 |
| CI/CD pipelines           | Planned     | I-3    |

---

## Architecture Decisions

Significant decisions are documented as ADRs in [`docs/decisions/`](docs/decisions/):

| ADR     | Title                                        | Status   |
| ------- | -------------------------------------------- | -------- |
| ADR-001 | Fullstack Monorepo for MVP Phase             | Accepted |
| ADR-002 | Static Analysis and Code Formatting          | Accepted |
| ADR-003 | WhatsApp Integration Strategy                | Accepted |
| ADR-004 | WhatsApp Conversational POC — Prioritization | Accepted |

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.
