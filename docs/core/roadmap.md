# IBMS — Project Roadmap

- **Last updated:** 2026-03-10
- **Current phase:** MVP
- **Repository:** insurance-broker-management-system

---

## Overview

This roadmap tracks the evolution of the Insurance Broker Management System from its current scaffolded state through a fully operational MVP. It is divided into two parallel tracks: **Application** (backend API + frontend SPA) and **Infrastructure** (OCI provisioning, CI/CD, observability).

Infrastructure work depends on OCI account creation. Until then, all development runs against the local Docker Compose stack.

---

## Application Track

### Phase 1 — Backend Core (Sprints 1–4)

The backend is developed module by module, following the domain structure already established in `apps/api/`. Each module ships with unit and integration tests before the next begins.

**Auth** establishes the security foundation. No other module can be properly tested end-to-end without working JWT issuance and validation. This is the entry point.

**Broker** is the central aggregate of the system — every policy, quote, and claim belongs to a broker. It must exist before the downstream modules can reference it.

**Policy** represents the core business entity of an insurance brokerage. It has the most complex lifecycle (issuance → active → renewal → cancellation) and will drive most of the domain modeling work.

**Quote** feeds into policy creation. A quote is the starting point of the sales workflow and must integrate with both broker and policy modules.

- [ ] Sprint 1 — `auth`: JWT issuance, login endpoint, refresh token, token validation filter
- [ ] Sprint 2 — `broker`: Broker CRUD, pagination, search, integration tests
- [ ] Sprint 3 — `policy`: Policy lifecycle, status transitions, domain events
- [ ] Sprint 4 — `quote`: Quote generation, status workflow, policy conversion

### Phase 2 — Backend Extended (Sprints 5–6)

**Claim** closes the policy lifecycle loop, handling the intake and tracking of insurance claims. It is the most operationally complex module, with its own status machine and document requirements.

**Notifications** is a cross-cutting concern — email and WhatsApp messaging triggered by domain events from policy, quote, and claim modules. It is implemented last to avoid coupling with unstable domain models.

- [ ] Sprint 5 — `claim`: Claim intake, status machine, document attachment
- [ ] Sprint 6 — `notification`: Email (SMTP), WhatsApp integration, async processing via Redis Streams

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

## Migration Triggers

This monorepo will be split into separate repositories when any of the following conditions are met:

- API and frontend require independent deployment cadences
- Team grows beyond 2–3 developers
- Component-level access control becomes necessary

See [ADR-001](adr/ADR-001-fullstack-monorepo.md) for full rationale.
