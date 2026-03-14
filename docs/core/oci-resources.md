# IBMS — OCI Free Tier Resource Reference

- **Last updated:** 2026-03-13
- **Source:** Oracle Cloud Always Free documentation (verified February 2026)

---

## Compute

| Resource              | Specification                                   |
| --------------------- | ----------------------------------------------- |
| Shape                 | VM.Standard.A1.Flex (ARM)                       |
| Architecture          | ARM64 (Ampere Altra)                            |
| OCPUs                 | 4 (distributable across up to 4 instances)      |
| Memory                | 24 GB (distributable across up to 4 instances)  |
| Monthly compute hours | 3,000 OCPU-hours + 18,000 GB-hours              |
| Network bandwidth     | 4 Gbps per instance                             |
| AMD alternative       | 2x VM.Standard.E2.1.Micro (1/8 OCPU, 1 GB each) |

The 3,000 OCPU-hour / 18,000 GB-hour limits are designed so that a single 4-OCPU / 24-GB instance running 24x7 stays within bounds (4 × 744h = 2,976 hours; 24 × 744 = 17,856 GB-hours).

---

## Storage

| Resource                  | Capacity     | Notes                           |
| ------------------------- | ------------ | ------------------------------- |
| Block Volumes             | 200 GB total | Up to 5 volumes, combined limit |
| Object Storage Standard   | 10 GB        | S3-compatible API               |
| Object Storage Infrequent | 10 GB        | Lower retrieval frequency       |
| Archive Storage           | 10 GB        | Cold storage, retrieval delay   |

---

## Database

| Resource               | Specification                                     |
| ---------------------- | ------------------------------------------------- |
| Autonomous Database    | 2 instances × 20 GB each                          |
| Workload types         | Lakehouse, Transaction Processing, JSON, APEX     |
| Version                | Oracle 19c or Oracle AI Database 26ai             |
| Backups                | Not available on Always Free (requires paid tier) |
| Concurrent connections | ~3–6 simultaneous users (rate limited)            |

Note: IBMS uses PostgreSQL running in a Docker container on the ARM VM instead of Autonomous Database. The Autonomous DB allocation remains available as a secondary resource if needed.

---

## Network

| Resource               | Specification                      |
| ---------------------- | ---------------------------------- |
| Load Balancer          | 1 × Flexible LB, 10 Mbps bandwidth |
| Network Load Balancer  | 1 × Flexible NLB                   |
| Outbound data transfer | 10 TB/month                        |
| VCN                    | Unlimited                          |
| Public IPs             | 2 reserved ephemeral               |
| VCN Flow Logs          | Included                           |
| Site-to-Site VPN       | Included                           |

---

## Serverless

| Resource         | Specification                                                |
| ---------------- | ------------------------------------------------------------ |
| OCI Functions    | 2 million invocations/month                                  |
| Compute time     | 400,000 GB-seconds/month                                     |
| Runtime          | Fn Project (open-source, supports Java, Python, Go, Node.js) |
| Beyond free tier | $0.0000002 per invocation                                    |

OCI Functions is based on the open-source Fn Project, avoiding runtime lock-in. Currently not used in the IBMS architecture — Spring Boot handles all request processing directly. Potential future use cases: heavy PDF generation, document OCR processing, scheduled cleanup tasks. Reassess when isolated, bursty workloads emerge that don't justify running in the main process.

---

## Observability

| Resource                | Specification                              |
| ----------------------- | ------------------------------------------ |
| Monitoring              | Included (metrics, dashboards, queries)    |
| Logging                 | 10 GB/month ingestion                      |
| Notifications           | 1 million email deliveries/month (via ONS) |
| Alarms                  | Unlimited                                  |
| Application Performance | Included (APM, tracing)                    |

---

## Security

| Resource              | Specification                                    |
| --------------------- | ------------------------------------------------ |
| IAM                   | Included (users, groups, policies, compartments) |
| Vault                 | 20 key versions (secrets management)             |
| Bastions              | 5 concurrent sessions                            |
| Certificates          | Included (certificate management)                |
| Security Lists / NSGs | Included (firewall rules per subnet/VNIC)        |

---

## Services Not Used but Available

These services are included in the Free Tier but are not part of the current IBMS architecture. Listed for awareness and future reference.

| Service               | Free Tier Allocation | Potential IBMS Use                 |
| --------------------- | -------------------- | ---------------------------------- |
| OCI Functions         | 2M invocations/month | PDF generation, OCR, cron tasks    |
| Autonomous Database   | 2 × 20 GB            | Secondary/analytics DB if needed   |
| APEX                  | 744 hours/month      | Internal admin tools (low-code)    |
| Email Delivery        | Included             | Transactional email sending        |
| NoSQL Database        | Included             | Audit logs, event store            |
| Service Connector Hub | Included             | Event routing between OCI services |

---

## Limitations and Gotchas

Idle instances are stopped after 7 days of inactivity. Always Free Autonomous DBs are stopped after 7 days idle and permanently deleted after 90 days if not restarted. Block volume IOPS are lower than paid tiers. ARM instances may face "Out of Capacity" errors in high-demand regions during provisioning — retry or choose a different availability domain. The Load Balancer is limited to 10 Mbps — sufficient for MVP but a bottleneck at scale. Always Free resources can only be provisioned in the home region selected during account creation.
