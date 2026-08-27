# Sentinel Testing Strategy & Harness Specification

This document details the comprehensive testing structure, test automation suites, test datasets, and verification procedures for the Sentinel API Management & Gateway Platform.

---

## 1. Test Suite Organization

```
Sentinel/
├── backend/sentinel-api/src/test/java/com/sentinel/api/
│   ├── AlertRuleAndEvaluationIntegrationTest.java
│   ├── ApiKeyLifecycleIntegrationTest.java
│   ├── ApiKeyOwnershipIntegrationTest.java
│   ├── ApiTestConsoleIntegrationTest.java
│   ├── ApplicationIntegrationTest.java
│   ├── ApplicationPolicyIntegrationTest.java
│   ├── AuditLogIntegrationTest.java
│   ├── AuthIntegrationTest.java
│   ├── CircuitBreakerIntegrationTest.java
│   ├── ConsumerAnalyticsIntegrationTest.java
│   ├── GatewayForwardingIntegrationTest.java
│   ├── GatewaySecurityIntegrationTest.java
│   ├── GlobalApiCatalogIntegrationTest.java
│   ├── GlobalRequestExplorerIntegrationTest.java
│   ├── ObservationAndMetricsIntegrationTest.java
│   ├── OpenApiImportIntegrationTest.java
│   ├── ProductionSecurityIntegrationTest.java
│   ├── RateLimitPolicyIntegrationTest.java
│   ├── RequestIdIntegrationTest.java
│   ├── SecurityHardeningIntegrationTest.java
│   ├── SystemHealthIntegrationTest.java
│   ├── TenantIsolationPhase2IntegrationTest.java
│   ├── TimeSeriesAndErrorAnalyticsIntegrationTest.java
│   └── service/
│       ├── ApiKeyServiceTest.java
│       ├── CircuitBreakerServiceTest.java
│       ├── PathNormalizerTest.java
│       └── RateLimitServiceTest.java
├── tests/
│   ├── fixtures/
│   │   ├── valid_petstore_openapi.json
│   │   ├── valid_ecommerce_openapi.yaml
│   ├── security/
│   │   ├── test_api_key_security.py
│   │   ├── test_tenant_isolation.py
│   │   ├── test_ssrf_and_headers.py
│   │   └── test_error_sanitization.py
│   ├── performance/
│   │   └── test_load_performance.py
│   └── failure/
│       ├── test_circuit_breaker_and_recovery.py
│       └── test_target_failure_recovery.py
└── scripts/
    ├── final_acceptance_test.py
    └── verify_phase4_e2e.py
```

---

## 2. Test Execution Commands

### Backend Automated Test Suite
```bash
cd backend/sentinel-api
.\mvnw.cmd test
```

### Security & Hardening Suite
```bash
python tests/security/test_api_key_security.py
python tests/security/test_tenant_isolation.py
python tests/security/test_ssrf_and_headers.py
python tests/security/test_error_sanitization.py
```

### Fault Tolerance & Recovery Suite
```bash
python tests/failure/test_circuit_breaker_and_recovery.py
python tests/failure/test_target_failure_recovery.py
```

### Performance & Load Benchmark
```bash
python tests/performance/test_load_performance.py
```

### 30-Step Comprehensive Acceptance Test
```bash
python scripts/final_acceptance_test.py
```
