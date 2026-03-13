# Insurance Broker Management System

![Status](https://img.shields.io/badge/status-active_development-blue)
![Version](https://img.shields.io/badge/version-0.1.0--SNAPSHOT-orange)
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
│   ├── api/        # Spring Boot 3.5 — REST API, domain logic, messaging
│   └── web/        # Angular — SPA client (in progress)
└── docs/
    └── adr/        # Architecture Decision Records
```

The backend follows a **package-by-feature** structure organized around insurance domain modules: `broker`, `policy`, `quote`, `claim`, `auth`, and `common`. This keeps domain logic cohesive and avoids the cross-cutting dependencies typical of package-by-layer approaches.

Infrastructure (Docker Compose, Terraform, CI/CD pipelines) lives in a separate repository with its own lifecycle.

---

## Tech Stack

**Backend**

- Java 17 + Spring Boot 3.5
- Spring Security (JWT, stateless)
- Spring Data JPA + PostgreSQL
- Spring AMQP + RabbitMQ
- Redis (distributed cache)
- Maven (build tooling)

**Frontend**

- Angular (in progress)
- Nginx (reverse proxy + static serving)

**Infrastructure** _(separate repo)_

- Oracle Cloud Infrastructure — Always Free Tier
- Docker + Docker Compose
- Terraform (IaC)
- GitHub Actions + GitLab CI

---

## Domain Modules

| Module   | Responsibility                                      |
| -------- | --------------------------------------------------- |
| `broker` | Brokerage and agent profile management              |
| `policy` | Policy lifecycle — issuance, renewal, cancellation  |
| `quote`  | Quote generation and comparison workflows           |
| `claim`  | Claims intake, tracking, and resolution             |
| `auth`   | Authentication, authorization, JWT issuance         |
| `common` | Shared utilities, exception handling, base entities |

---

## Getting Started

### Prerequisites

- Java 17 (`sdk use java 17` via SDKMAN)
- Docker + Docker Compose
- Maven 3.9+

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

| Component               | Status       |
| ----------------------- | ------------ |
| API scaffolding         | ✓ Complete   |
| Domain module structure | ✓ Complete   |
| JWT authentication      | ✓ Scaffolded |
| REST endpoints          | In progress  |
| Angular frontend        | Planned      |
| CI/CD pipelines         | Planned      |

---

## Architecture Decisions

Significant decisions are documented as ADRs in [`docs/adr/`](docs/adr/). This includes choices around monorepo structure, technology selection, and infrastructure strategy.
