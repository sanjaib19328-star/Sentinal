# Sentinel Architecture Overview

Sentinel is an enterprise-grade API Management, Observability, and Gateway Platform designed to unify disparate microservices under centralized policy enforcement, real-time telemetry, and developer self-service.

```
+-----------------------------------------------------------------------------+
|                           API Client / Consumer                             |
+-----------------------------------------------------------------------------+
                                      │
                                      ▼ [HTTP Request + X-Sentinel-API-Key]
+-----------------------------------------------------------------------------+
|                            SENTINEL API GATEWAY                             |
|                                                                             |
|  1. Security Filters: Header Sanitization (Strip internal, Drop hop-by-hop) |
|  2. Authentication: SHA-256 Hashed Scoped API Key Validation               |
|  3. Dynamic Routing & Auto-Discovery: Normalize URI Path Parameters        |
|  4. Traffic Policies: Method Whitelisting, Body Size, Backend Timeout       |
|  5. Rate Limiter & Sliding Quota Engine (Redis Distributed Token Bucket)    |
|  6. Circuit Breaker & Safe Retry Machine (Closed -> Open -> Half-Open)      |
|  7. Observability Logging: X-Request-Id Distributed Tracing & Latency Timer |
+-----------------------------------------------------------------------------+
                                      │
                                      ▼ [Forwarded Clean HTTP Request]
+-----------------------------------------------------------------------------+
|                     Downstream Target Application Microservice              |
+-----------------------------------------------------------------------------+
                                      │
                                      ▼ [HTTP Response Payload]
+-----------------------------------------------------------------------------+
|                       SENTINEL CONTROL & DATA PLANE                         |
|                                                                             |
|  - MySQL 8.4: Relational Persistence (Users, Apps, Keys, Logs, Policies)   |
|  - Redis 7.x: Sub-millisecond distributed counters & quota windows          |
|  - Analytics & Alerting Engine: Real-time latency percentiles & alerts      |
|  - React Control Console: Developer Test Console & Global Catalog           |
+-----------------------------------------------------------------------------+
```

## Subsystem Breakdown

1. **Gateway Ingress Pipeline**:
   - High-performance non-blocking proxying using Java HTTP Client.
   - Enforces 16KB header size limits and configurable payload boundaries.
   - Attaches `X-Request-Id`, `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-RateLimit-*` response headers.

2. **Policy & Quota Engine**:
   - Hierarchical rate limits: Global Policy $\to$ Application Policy $\to$ Endpoint Policy $\to$ Consumer Key limits.
   - Atomic Redis operations ensuring consistency across multi-node gateway clusters.

3. **Circuit Breaker Machine**:
   - Interface-backed reliability layer isolating degraded downstream microservices.
   - Fast-fails with `503 Service Unavailable` (`"CIRCUIT_BREAKER_OPEN"`) to prevent cascading resource starvation.
