# Sentinel Frontend — Observability & Security Control Dashboard

The official web frontend for the **Sentinel API Gateway & Security Layer**.

Sentinel provides an external, non-blocking observability dashboard where engineering teams can register, probe, monitor, and manage independent microservices and applications.

---

## Architectural Philosophy

* **External & Independent**: The user's application does **not** rely on Sentinel for core runtime execution. If Sentinel is offline or disconnected, the user application continues to serve traffic normally.
* **Strict No Fake Data Policy**:
  - Newly registered applications start with `UNKNOWN` status, `0` request logs, and `[]` empty metrics.
  - All charts, status badges, and request tables render **only real data** received from the Sentinel backend.
  - No synthetic/random/mock telemetry is generated.
* **Light Theme by Design**: Clean developer-first interface with high contrast, clear visual hierarchies, and distinct status indicators:
  - `HEALTHY` (Green / Emerald)
  - `DEGRADED` (Amber / Yellow)
  - `UNAVAILABLE` (Red / Rose)
  - `UNKNOWN` (Slate / Gray)

---

## Key Features

1. **Authentication & User Management**:
   - Secure registration and login with JWT token persistence.
   - Session auto-restoration and centralized 401 unauthorized handling.
2. **Applications Registry**:
   - Register new services with target Base URL in non-blocking `OBSERVATION` mode.
   - Search and filter by status (`HEALTHY`, `DEGRADED`, `UNAVAILABLE`, `UNKNOWN`).
   - Edit application metadata and delete application with confirmation dialogs.
3. **Real Health Observation & Latency Probes**:
   - On-demand "Test Connection Now" probe calling `POST /api/v1/applications/:id/connection-test`.
   - Real measured HTTP response time and exact status message.
4. **Real Request Telemetry Logs**:
   - Paginated table showing real gateway requests (`requestId`, `method`, `path`, `statusCode`, `latencyMs`, `clientIp`, `timestamp`).
5. **Real Metrics Aggregation**:
   - Dynamic aggregated metric cards (`REQUEST_COUNT`, `SUCCESS_COUNT`, `ERROR_COUNT`, `AVG_LATENCY`, `HEALTH_CHECK`).
   - Persisted metric event logs table.
6. **Scoped API Key Management**:
   - Generate application-scoped API keys with configurable Redis rate limits.
   - One-time raw API key display with secure copy button and prominent security alert.

---

## Getting Started

### Prerequisites
* Node.js 18+
* Sentinel Backend running on `http://localhost:8080`

### Installation
```bash
cd frontend
npm install
```

### Development Server
```bash
npm run dev
```
The application will be accessible at `http://localhost:5173`.

### Production Build
```bash
npm run build
```
Generates optimized static assets in the `dist/` directory with zero TypeScript compiler errors.
