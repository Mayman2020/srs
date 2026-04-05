# Performance Risks Register  
## Government Administrative Communications System

| Document control | |
|------------------|---|
| **Source SRS** | `SRS_نظام_الاتصالات_الادارية.docx` (NFR-100, NFR-200) |
| **Version** | 1.0 |

### SRS targets (acceptance baseline)

| ID | Metric | Target |
|----|--------|--------|
| NFR-101 | Transaction response time | ≤ 3 seconds |
| NFR-102 | List query (10,000 records) | ≤ 5 seconds |
| NFR-103 | Concurrent users | 5,000 |
| NFR-104 | Correspondence volume | 50,000+ |
| NFR-105 | Retention | 10 years |
| NFR-201 | Availability | 99.9% |

---

## Risk register

| Risk ID | Area | Risk description | Likelihood | Impact | Mitigation | Owner | SRS |
|---------|------|------------------|------------|--------|------------|-------|-----|
| PERF-R01 | Database | Unindexed filters on correspondence list (`status`, `type`, `created_at`, `department_id`) cause full scans as volume → 50k+. | M | H | Composite indexes; EXPLAIN review; read replicas for reporting | DBA / Dev | NFR-102 |
| PERF-R02 | Database | N+1 queries loading correspondence + attachments + workflow in detail view. | H | M | Fetch joins / DTO projections; batch APIs | Dev | NFR-101 |
| PERF-R03 | Camunda | High task completion rate saturates job executor thread pool. | M | H | Tune `corePoolSize` / `maxPoolSize`; horizontal scaling story | Platform | NFR-103 |
| PERF-R04 | Camunda | Large BPMN with many parallel branches increases engine overhead per instance. | L | M | Simplify model; async where possible | BA / Dev | FR-203 |
| PERF-R05 | File I/O | Local disk attachment store on shared NFS with high latency. | M | H | Move to object storage (S3/MinIO); CDN for static | Infra | FR-102 |
| PERF-R06 | File I/O | Synchronous antivirus scan blocks upload thread. | M | M | Async scan + status; bounded queue | Security | §14 |
| PERF-R07 | Search | Full-text (FR-502) on PostgreSQL `LIKE` only at scale. | H | H | Elasticsearch/OpenSearch; async indexing | Arch | FR-502 |
| PERF-R08 | Reports | Dashboard aggregates (`/reports/*`, `/dashboard`) compute on every request. | M | H | Materialized views / cache (Redis) with TTL; pre-aggregate nightly | Dev | FR-601 |
| PERF-R09 | Export | Excel export loads full dataset into memory. | M | H | Streaming writer; cursor-based queries; async job + download link | Dev | FR-705 |
| PERF-R10 | JWT | Per-request DB lookup for user/role without cache. | M | M | Short-lived cache with invalidation on permission change | Dev | — |
| PERF-R11 | API | Chatty Angular app (many sequential HTTP calls per screen). | M | M | BFF aggregate endpoints; parallel `forkJoin` | FE | NFR-101 |
| PERF-R12 | Network | Large JWT or huge cookies increase latency on every call. | L | M | Slim claims; permission summary hashed server-side | Sec | — |
| PERF-R13 | Frontend | Unvirtualized table for 10k rows in browser. | H | M | Server paging + virtual scroll | FE | NFR-102 |
| PERF-R14 | Growth | 10-year retention (NFR-105) → billion-row audit / attachment metadata. | M | H | Partitioning by year; archival cold storage; TTL policy | DBA | NFR-105 |
| PERF-R15 | Integrations | Synchronous AD/LDAP/SMS on login or notification path. | M | H | Timeout + circuit breaker; async notification queue | Dev | §12 |
| PERF-R16 | Backup | RPO 1h with heavy write load — backup window pressure. | M | H | WAL archiving / PITR; storage IOPS sizing | Infra | NFR-203 |

---

## Load testing profile (recommended)

| Scenario | Mix % | Notes |
|----------|-------|-------|
| Login + dashboard | 25 | Warm JWT cache |
| List correspondence (filtered) | 35 | Primary NFR-102 |
| Open detail + attachments | 15 | Attachment download separate |
| Workflow action | 15 | Camunda hot path |
| Report export | 10 | PERF-R09 stress |

**Ramp**: 0 → 5000 virtual users over 30–60 min; soak 2–4 h for leaks.

---

## Monitoring KPIs (production)

| KPI | Alert threshold (example) |
|-----|---------------------------|
| API P95 latency | > 2.5s (warn), > 3.5s (crit) vs NFR-101 |
| Error rate 5xx | > 0.1% 5 min window |
| DB CPU | > 70% sustained |
| Camunda incidents/hour | > 0 business hours |
| Disk usage attachments | > 80% |

---

## Review

| Frequency | Action |
|-----------|--------|
| Each major release | Re-run load test; update risk scores |
| Architecture change | Add rows (e.g., new integration) |
