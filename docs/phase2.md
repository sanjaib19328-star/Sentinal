# Sentinel Phase 2 — Advanced API Management, Traffic Policies & Observability

## Overview

Sentinel Phase 2 extends Sentinel from a lightweight observation platform into an enterprise-ready, high-throughput **API Management and Traffic Control Platform**.

Phase 2 preserves all Phase 1 capabilities (real gateway proxying, dynamic API auto-discovery, SHA-256 scoped key management, and non-intrusive health probes) while adding:

1. **Multi-Level Rate Limiting & Quotas** (API Key $\to$ Application Policy $\to$ Endpoint Policy $\to$ Quotas)
2. **Configurable Traffic Policies** (`ApiPolicy` with allowed HTTP methods, max request body size limits, custom timeout ms, and IP whitelisting)
3. **End-to-End Request ID Tracing** (`X-Request-Id` client preservation, generation, target propagation, response delivery, and persistence)
4. **Time-Series Telemetry & Traffic Breakdowns** (interval bucketing by minute/hour/day, P50/P95/P99 latency percentiles, and HTTP method/status class distributions)
5. **Dedicated Error Analytics & Anomaly Detection** (error rate gauges, breakdown by status code and endpoint, filterable error stream)
6. **Real-Time Alert Rules & Alert Lifecycle** (`HIGH_ERROR_RATE`, `HIGH_LATENCY`, `API_UNAVAILABLE`, `EXCESSIVE_429` evaluation with `ACTIVE` $\to$ `ACKNOWLEDGED` $\to$ `RESOLVED` states)
7. **Management-Plane Audit Logging** (immutable trail of application, key, policy, and rule changes with zero raw secret leakage)
8. **Tenant-Isolated Command Center** (global aggregation across tenant services)
9. **Full 9-Tab Frontend Control Plane** (Overview, APIs, Requests, Metrics, Errors, Policies, Alerts, API Keys, Audit)

---

## Architectural & Database Migrations

- `V8__create_api_policies_and_quotas.sql`: creates `api_policies` table for application and endpoint traffic constraints.
- `V9__create_audit_logs.sql`: creates `audit_logs` table for management actions.
- `V10__create_alert_rules_and_alerts.sql`: creates `alert_rules` and `alerts` tables.

---

## Gateway Request Flow & Policy Enforcement

When a request arrives at `/api/v1/gateway/**`:

```text
Client Request
      │
      ▼
1. Extract or Generate Request ID (X-Request-Id)
      │
      ▼
2. Authenticate API Key via SHA-256 Hash
      │
      ▼
3. Auto-Discover Endpoint (Path Normalization, e.g. /users/{id})
      │
      ▼
4. Fetch Application Policy & Endpoint Policy
      │
      ├── Check Allowed HTTP Methods (405 Method Not Allowed if invalid)
      ├── Check Max Request Body Size (413 Payload Too Large if exceeded)
      │
      ▼
5. Evaluate Multi-Level Rate Limiting & Quotas (Atomic Redis Counters)
      ├── API Key limit (req / min)
      ├── Application Policy limit & Quota (req / window)
      └── Endpoint Policy limit & Quota (req / window)
      │
      ├── [If Throttled] Return 429 Too Many Requests with:
      │   - X-Request-Id
      │   - X-RateLimit-Limit
      │   - X-RateLimit-Remaining: 0
      │   - X-RateLimit-Reset
      │   - Retry-After
      │   - Error JSON indicating throttled policy
      │
      ▼
6. Forward Request to Target Backend with custom Timeout (timeoutMs)
      ├── Propagate X-Request-Id and client headers
      └── Stream response body & headers
      │
      ▼
7. Persist RequestLog (with Request ID, Latency, Status Code, IP)
      │
      ▼
8. Real-Time Alert Evaluation (if error rate or latency exceeds active rule threshold)
```

---

## Testing & Quality Assurance

- **Backend Integration Tests**: 61 automated integration and unit tests passing via `.\mvnw.cmd test`
- **Frontend Build**: TypeScript build validated with 0 errors via `npm run build`
- **Security & Tenant Isolation**: Verified cross-user tenant boundary enforcement across all endpoints.
