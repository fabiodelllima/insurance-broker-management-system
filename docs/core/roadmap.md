# IBMS — Project Roadmap

- **Current phase:** MVP
- **Repository:** insurance-broker-management-system

---

## Overview

This roadmap tracks the evolution of the Insurance Broker Management System from its current scaffolded state through a fully operational MVP. It is divided into two parallel tracks: **Application** (backend API + frontend SPA) and **Infrastructure** (OCI provisioning, CI/CD, observability).

Infrastructure work depends on OCI account creation. Until then, all development runs against the local Docker Compose stack.

---

## Application Track

### Phase 0 — Foundation (Sprint 1)

Auth establishes the security foundation. No other module can be properly tested end-to-end without working JWT issuance and validation.

- [x] Sprint 1 — `auth`: JWT issuance, login, refresh, logout, token validation filter

### Phase 0.5 — WhatsApp POC (Sprint WA-POC)

Inserted ahead of the backend core modules to demonstrate tangible system value to stakeholders. The financier requires evidence that the system solves a real workflow problem before committing further investment. A conversational claim intake via WhatsApp — replacing the manual process brokers perform daily — provides that evidence.

This sprint builds a vertical slice: Meta Cloud API integration, conversation engine, and a simplified claim entity. The code is structured to evolve into Sprints 5 (Claim) and 6 (Notifications) without rewrite. See ADR-004 for full rationale.

- [ ] Sprint WA-POC — WhatsApp claim intake: Meta Cloud API, conversation engine, claim persistence

### Phase 1 — Backend Core (Sprints 2–4)

The backend is developed module by module, following the domain structure already established in `apps/api/`. Each module ships with unit and integration tests before the next begins.

**Broker** is the central aggregate of the system — every policy, quote, and claim belongs to a broker. It must exist before the downstream modules can reference it.

**Policy** represents the core business entity of an insurance brokerage. It has the most complex lifecycle (issuance → active → renewal → cancellation) and will drive most of the domain modeling work.

**Quote** feeds into policy creation. A quote is the starting point of the sales workflow and must integrate with both broker and policy modules.

- [ ] Sprint 2 — `broker`: Broker CRUD, pagination, search, integration tests
- [ ] Sprint 3 — `policy`: Policy lifecycle, status transitions, domain events
- [ ] Sprint 4 — `quote`: Quote generation, status workflow, policy conversion

### Phase 2 — Backend Extended (Sprints 5–6)

**Claim** builds on the foundation established in WA-POC — the simplified entity gains a proper Policy FK, document attachments, status machine, and REST endpoints.

**Notifications** absorbs the WhatsApp client from WA-POC into the full notification pipeline: Redis Streams, domain event routing, email channel, Resilience4j circuit breakers, and dead letter handling.

- [ ] Sprint 5 — `claim`: Full claim entity, status machine, document attachment, REST API
- [ ] Sprint 6 — `notification`: Email (SMTP), WhatsApp promotion to NotificationChannel, async processing via Redis Streams

### Phase 3 — Frontend (Sprints 7–10)

Angular scaffolding begins only after the backend API is stable enough to be consumed. The frontend is developed feature by feature, mirroring the backend module order.

- [ ] Sprint 7 — Scaffold + Auth: Angular project setup, routing, HTTP interceptors, login page
- [ ] Sprint 8 — Broker + Policy: Broker management views, policy lifecycle UI, status indicators
- [ ] Sprint 9 — Quote + Claim: Quote creation flow, claim intake form, document upload
- [ ] Sprint 10 — Dashboard + Polish: Operational dashboard, notifications UI, responsive layout

---

## Infrastructure Track

Infrastructure work is gated on OCI account creation. Local development uses Docker Compose throughout.

### Phase 1 — OCI Provisioning

- [ ] Terraform foundation: provider config, versions, remote state on Object Storage
- [ ] Network module: VCN, subnets, security lists, internet gateway
- [ ] Compute module: ARM Ampere A1 instance, cloud-init bootstrap
- [ ] Storage module: block volume (200 GB), Object Storage bucket
- [ ] Environments: `poc/` and `mvp/` consuming shared modules

### Phase 2 — CI/CD

- [ ] GitHub Actions: build + test on PR, Docker image build and push
- [ ] Deploy pipeline: SSH deploy to OCI VM, approval gate for `main`
- [ ] GitLab CI: mirror pipelines for redundancy
- [ ] Branch protection: rules for `main` and `develop` on both remotes

### Phase 3 — Observability

- [ ] Metrics stack: Prometheus + Grafana, Spring Actuator, node-exporter, cAdvisor
- [ ] Dashboards: CPU, RAM, storage, request latency, error rate
- [ ] Alerting: thresholds aligned with OCI Free Tier limits (CPU > 80%, RAM > 20 GB, storage > 180 GB)
- [ ] Operations: automated PostgreSQL backup to Object Storage, log rotation, disaster recovery runbook

---

## Timeline Overview

```plaintext
Sprint 1 (Auth)         ████████████████  2026-03-11 → 2026-03-24  [COMPLETE]
Sprint WA-POC           ░░░░░░░░░░░░░░░░  2026-03-25 → 2026-04-07  ← current
Sprint 2 (Broker)       ░░░░░░░░░░░░░░░░  2026-04-08 → 2026-04-21
Sprint 3 (Policy)       ░░░░░░░░░░░░░░░░  2026-04-22 → 2026-05-05
Sprint 4 (Quote)        ░░░░░░░░░░░░░░░░  2026-05-06 → 2026-05-19
Sprint 5 (Claim)        ░░░░░░░░░░░░░░░░  2026-05-20 → 2026-06-02
Sprint 6 (Notification) ░░░░░░░░░░░░░░░░  2026-06-03 → 2026-06-16
Sprint 7 (Angular)      ░░░░░░░░░░░░░░░░  2026-06-17 → 2026-06-30
Sprint 8 (Broker UI)    ░░░░░░░░░░░░░░░░  2026-07-01 → 2026-07-14
Sprint 9 (Claim UI)     ░░░░░░░░░░░░░░░░  2026-07-15 → 2026-07-28
Sprint 10 (Dashboard)   ░░░░░░░░░░░░░░░░  2026-07-29 → 2026-08-11
```

Inserting WA-POC shifts all subsequent sprints by 2 weeks. Total project timeline extends from ~20 weeks to ~22 weeks. The trade-off is justified by early stakeholder validation (see ADR-004).

---

## Migration Triggers

This monorepo will be split into separate repositories when any of the following conditions are met:

- API and frontend require independent deployment cadences
- Team grows beyond 2–3 developers
- Component-level access control becomes necessary

See [ADR-001](../decisions/ADR-001-fullstack-monorepo.md) for full rationale.

---

- **Last updated:** 2026-03-14
