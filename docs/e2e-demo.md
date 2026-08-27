# Sentinel End-to-End Real-World Validation Guide

This guide walks through verifying the complete Sentinel API Gateway with a real downstream target application.

## Quick Start Real E2E Run

1. **Start Downstream Target Microservice (Port 9090)**:
```bash
python target-app/server.py
```

2. **Start Sentinel Backend (Port 8080)**:
```bash
cd backend/sentinel-api
.\mvnw.cmd spring-boot:run
```

3. **Run Automated 31-Step Real E2E Validation Script**:
```bash
python scripts/verify_phase4_e2e.py
```

## Validation Flow Checked
- [x] Register management user & acquire JWT.
- [x] Register downstream application `http://127.0.0.1:9090`.
- [x] Probe health & verify live latency.
- [x] Import OpenAPI specification $\to$ 8 endpoints cataloged as `DOCUMENTED`.
- [x] Generate scoped API key with rate limit.
- [x] Execute Developer API Test Console with scoped key.
- [x] Forward live requests through Sentinel Gateway $\to$ endpoints transition to `DOCUMENTED_AND_DISCOVERED`.
- [x] Query Global API Catalog (`GET /api/v1/apis`) with search, filter, and sort.
- [x] Query Global Request Explorer (`GET /api/v1/requests`) with status class filters.
- [x] Verify Consumer Analytics (P50/P95/P99 latency, request volume).
- [x] Verify Rate Limiting & 429 Throttle enforcement.
- [x] Key Lifecycle: Revoke key $\to$ verify immediate `401 UNAUTHORIZED`.
- [x] Key Lifecycle: Regenerate key $\to$ verify old key fails and new key succeeds.
- [x] Circuit Breaker: Trigger repeated 5xx errors $\to$ verify circuit trips to `OPEN` and fast-fails with `503 Service Unavailable` (`"CIRCUIT_BREAKER_OPEN"`).
- [x] System Health: Query control plane, MySQL latency, Redis latency, and downstream circuits.
