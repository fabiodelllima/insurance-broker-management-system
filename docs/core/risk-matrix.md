# IBMS — Risk Matrix and Transition Triggers

**Scope:** OCI Free Tier operation during POC and MVP

---

## Operational Health Indicators

These metrics confirm the infrastructure is performing within acceptable bounds. Monitor continuously via OCI Monitoring and Spring Actuator.

| Metric              | Healthy  | Warning    | Critical        |
| ------------------- | -------- | ---------- | --------------- |
| CPU usage (avg)     | < 70%    | 70–85%     | > 85% sustained |
| RAM usage           | < 20 GB  | 20–22 GB   | > 22 GB         |
| Storage usage       | < 160 GB | 160–180 GB | > 180 GB        |
| Request latency p95 | < 300 ms | 300–500 ms | > 500 ms        |
| Error rate          | < 0.5%   | 0.5–1.0%   | > 1.0%          |
| Availability        | > 99%    | 97–99%     | < 97%           |

**Action protocol:** Warning triggers investigation and optimization. Critical sustained for 7+ days triggers the contingency plan below.

---

## Technical Risks

| Risk                                    | Probability   | Impact | Mitigation                                              |
| --------------------------------------- | ------------- | ------ | ------------------------------------------------------- |
| Free Tier limits hit before MVP end     | Low (15%)     | Medium | Proactive monitoring, optimization pipeline             |
| ARM performance inadequate for workload | Very Low (5%) | Medium | Benchmark on real workload before production commitment |
| OCI downtime                            | Low (10%)     | High   | Automated backups, versioned data, documented restore   |
| OCI changes Free Tier policy            | Very Low (5%) | High   | Portable architecture, migration plan documented        |
| Testcontainers flaky in CI              | Medium (25%)  | Low    | Pre-pull images, retry logic, fallback to `-DskipTests` |

---

## Financial Risks

| Risk                                  | Probability  | Impact | Mitigation                                              |
| ------------------------------------- | ------------ | ------ | ------------------------------------------------------- |
| Growth faster than projected          | Medium (30%) | Medium | Code optimization, aggressive caching                   |
| Hidden costs (egress, API calls)      | Low (15%)    | Low    | Billing monitoring, alerts on any non-zero charge       |
| Free Tier insufficient before revenue | Low (20%)    | Medium | Budget reassessment, cost-benefit analysis              |
| WhatsApp API costs exceed projection  | Very Low     | Low    | Volume is <500 msg/month; even 10x growth is under $100 |

---

## WhatsApp Integration Risks

| Risk                                        | Probability   | Impact | Mitigation                                               |
| ------------------------------------------- | ------------- | ------ | -------------------------------------------------------- |
| Meta Business Account verification delay    | Medium (25%)  | Medium | Start process early; sandbox works without verification  |
| Template approval rejected by Meta          | Low (15%)     | Low    | Follow official guidelines; re-submit with adjustments   |
| Webhook endpoint unreachable (ngrok/tunnel) | Medium (30%)  | Low    | Document fallback setup; deploy to OCI for stable URL    |
| Meta API rate limiting                      | Very Low (5%) | Low    | POC volume far below limits; implement backoff if needed |
| Conversation state lost on app restart      | Certain       | Low    | Known POC limitation; Redis-backed store in Sprint 6     |
| WhatsApp number banned                      | Very Low (5%) | High   | Strictly transactional messages; follow Meta policies    |
| Meta deprecates sandbox features            | Very Low      | Medium | Sandbox is established tooling; unlikely in short term   |
| Webhook HMAC bypass attempt                 | Low (10%)     | High   | Validate signature on every request; reject unsigned     |

---

## Contingency Plan

When critical thresholds are sustained, execute in order — each layer resolves most cases without reaching the next.

### Layer 1 — Optimization (zero cost)

Execute before any spending decision:

- Audit slow queries, add missing indexes, eliminate N+1 patterns
- Increase Redis cache TTLs, add cache for frequently accessed data
- Enable gzip compression, optimize images, implement lazy loading
- Review connection pool sizing (HikariCP, PostgreSQL max_connections)
- Garbage collection tuning (G1GC parameters, heap sizing)

### Layer 2 — Reassessment

If optimization is insufficient:

- Cost-benefit analysis of OCI paid tier vs migration to another provider
- Validate whether current revenue justifies infrastructure spend
- Compare: OCI paid add-ons vs AWS/Azure equivalent pricing
- Decision: scale vertically on OCI, or migrate

---

## Transition Triggers

Reassess cloud provider choice when any of the following conditions are met:

| Trigger                                | Projected timeline |
| -------------------------------------- | ------------------ |
| Simultaneous active users > 150        | Month 9–12         |
| Total storage usage > 180 GB           | Month 10–14        |
| Need for OCI-unavailable services      | Unpredictable      |
| Monthly revenue > R$ 50,000            | Post-MVP           |
| CPU sustained > 85% after optimization | Variable           |

---

## Benchmark Acceptance Criteria

### POC Phase

- Load test: 50 simultaneous users → p95 latency < 500 ms
- Stress test: 100 requests/second → error rate < 1%
- Availability: 95% during business hours

### MVP Phase

- Load test: 100 simultaneous users → p95 latency < 300 ms
- Stress test: 200 requests/second → error rate < 0.5%
- Availability: 99% (24x7)

Run benchmarks before each phase transition. Tools: k6, Gatling, or Apache JMeter.

---

- **Last updated:** 2026-03-14
