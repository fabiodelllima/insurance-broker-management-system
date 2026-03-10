# ADR-001: Fullstack Monorepo for MVP Phase

**Date:** 2026-03-10

**Status:** Accepted

---

## Context

The Insurance Broker Management System requires a backend API (Spring Boot), a frontend SPA (Angular), and shared documentation. At MVP stage, the team is a single developer and the components evolve together — API contracts, domain models, and UI screens change in lockstep.

The question was whether to maintain separate repositories per component from the start, or to consolidate into a monorepo for the initial phases.

---

## Decision

Adopt a **fullstack monorepo** for the MVP phase, with the following structure:

```plaintext
insurance-broker-management-system/
├── apps/
│   ├── api/    # Spring Boot backend
│   └── web/    # Angular frontend
└── docs/
    └── adr/
```

Infrastructure (Terraform, Docker Compose, CI/CD) remains in a **separate repository** (`ibms-infra`) due to its independent lifecycle, operational sensitivity, and distinct access control requirements.

---

## Alternatives Considered

**Polyrepo from day one** — one repository per component (`ibms-api`, `ibms-web`). Standard practice at scale, but introduces coordination overhead (synchronized PRs, cross-repo dependency management, duplicated CI configuration) that adds no value when a single developer owns all components and they change together.

**Monorepo including infrastructure** — consolidate API, frontend, and infra into one repo. Rejected because infrastructure has a different change cadence, contains operationally sensitive configuration, and benefits from separate access controls. Mixing application code and infrastructure in the same repository conflates two distinct concerns.

---

## Rationale

At MVP scale, the cost of coordination between separate repos exceeds the benefit. A monorepo allows atomic commits that span API and frontend changes, a single pull request to review a feature end-to-end, and a unified history that tells the story of the product's evolution.

The historical commits from the original `ibms-api` repository were migrated into `apps/api/` using `git filter-repo`, preserving full commit history under the correct path. This means the monorepo's history accurately reflects the development timeline from the first scaffold.

The decision is explicitly scoped to the MVP phase. When the frontend matures and independent deployment cadences emerge, splitting into separate repositories becomes the natural next step.

---

## Consequences

**Positive**

- Atomic commits across API and frontend
- Single repository for portfolio visibility
- Full commit history preserved from `ibms-api` inception
- Simplified local development setup

**Negative**

- CI/CD pipelines require path filtering to avoid unnecessary builds (e.g., API pipeline should not trigger on frontend-only changes)
- Future repository split will require `git filter-repo` or subtree operations

**Migration trigger**
Split into separate repositories when any of the following conditions are met:

- API and frontend require independent deployment cadences
- Team size grows beyond 2-3 developers
- Component-level access control becomes necessary
