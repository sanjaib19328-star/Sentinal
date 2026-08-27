# Sentinel — Intelligent API Gateway, Observability & Security Platform

Sentinel is a lightweight, non-blocking API Gateway, Observability, and Security Control platform designed to connect, monitor, protect, and analyze backend applications with zero friction.

```text
                        CLIENT / TRAFFIC
                               │
                               │  Header: X-Sentinel-API-Key: sk_sentinel_...
                               ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           SENTINEL PLATFORM                            │
│                                                                        │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ JWT Authentication    │  │ Gateway Routing & Forwarding          │  │
│  │ & Tenant Isolation    │  │ (High Performance & Non-Blocking)     │  │
│  └───────────────────────┘  └───────────────────────────────────────┘  │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ Automatic API         │  │ Redis Sliding-Window Rate Limiting    │  │
│  │ Discovery & Catalog   │  │ & Policy Enforcement                  │  │
│  └───────────────────────┘  └───────────────────────────────────────┘  │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ Real-Time Telemetry,  │  │ Sentinel AI Assistant                 │  │
│  │ Metrics & Error Logs  │  │ (Live Grounded Telemetry Diagnosis)   │  │
│  └───────────────────────┘  └───────────────────────────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    │ Proxied Requests / Health Checks
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        YOUR APPLICATION BACKEND                        │
│                                                                        │
│        (Hosted: e.g. https://api.mycompany.com                         │
│         or Local: e.g. http://localhost:5000)                          │
│                                                                        │
│   GET /api/v1/health             POST /api/v1/orders                   │
│   GET /api/v1/users              DELETE /api/v1/items/{id}             │
│   ... and 1,000+ automatically discovered routes                       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Key Features

* **One Connection $\rightarrow$ One API Key $\rightarrow$ All APIs**: Connect an entire application once using a single Sentinel developer key. Protect and meter 10, 100, or 1,000+ endpoints without per-route key configuration.
* **Automatic API Discovery**: Auto-detects OpenAPI/Swagger specifications (`/openapi.json`, `/v3/api-docs`, etc.) or discovers live routes dynamically from runtime gateway traffic.
* **Normalized Route Catalog**: Dynamic resource IDs (UUIDs, hashes, numeric IDs, file extensions) are collapsed into parameterized templates (e.g. `/api/v1/images/{id}/download`), avoiding catalog duplication.
* **Non-Blocking Observability**: Sentinel operates out-of-band for health probing and adds negligible sub-millisecond overhead during gateway proxying.
* **Redis Sliding-Window Rate Limiter**: High-throughput distributed rate limiting with real-time token tracking and standard HTTP headers (`RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset`).
* **Strict Multi-Tenant Isolation**: Complete database and telemetry isolation partitioned by tenant ownership.
* **Sentinel AI Assistant**: Gemini-grounded intelligent assistant capable of querying live telemetry, error metrics, and API catalog state via deterministic tool calling.

---

## Application Connection Workflow

Connecting a backend to Sentinel requires only **3 simple inputs**:

```text
Applications Page
       ↓
[ + Import Application ]
       ↓
┌──────────────────────────────────────────────┐
│  Application Name: PixelVault-Clean          │
│  Backend URL:      https://api.mycompany.com │
│  Sentinel API Key: sk_sentinel_... (optional)│
└──────────────────────────────────────────────┘
       ↓
[ Import Application ]
       ↓
✓ Connection Established
✓ Backend Health Verified (HEALTHY)
✓ Automatic API Discovery Completed
       ↓
Discovered APIs populated in API Catalog
(Gateway URL & Sentinel API Key displayed with 1-click Copy)
```

The same identical workflow connects:
* **Hosted Backends** (e.g. `https://pixelvault-clean-api.onrender.com` or `https://api.mycompany.com`)
* **Local Backends** (e.g. `http://localhost:5000` or `http://127.0.0.1:8000`)

---

## Quick Start

### 1. Prerequisites
* **Java 21 LTS**
* **Node.js 20+** & **npm**
* **Docker & Docker Compose**

### 2. Start Supporting Services (MySQL & Redis)
```powershell
docker compose up -d
```
* **MySQL 8.4**: Port `3307` (`sentinel-mysql`)
* **Redis 7**: Port `6379` (`sentinel-redis`)

### 3. Start Backend API
```powershell
cd backend\sentinel-api
.\mvnw.cmd clean package -DskipTests
java -jar target\sentinel-api-0.0.1-SNAPSHOT.jar
```
Backend starts on `http://localhost:8080`.

Verify health:
```powershell
curl.exe http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 4. Start Frontend Dashboard
```powershell
cd frontend
npm install
npm run dev
```
Dashboard opens on `http://localhost:5173`.

---

## Routing Traffic Through the Gateway

Once an application is imported, route requests through the Sentinel Gateway by prefixing paths with `/api/v1/gateway` and providing the Sentinel API Key:

```bash
# Example: Querying your application through Sentinel
curl -X GET "http://localhost:8080/api/v1/gateway/api/v1/images" \
  -H "X-Sentinel-API-Key: sk_sentinel_your_app_key_here"
```

Sentinel automatically:
1. Validates the API key hash in MySQL/cache.
2. Applies Redis rate limits.
3. Forwards the request and headers to your configured application backend.
4. Captures latency, status codes, and request logs for dashboard analytics.
5. Updates route discovery in the API Catalog.

---

## Architecture & Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.4, Spring Data JPA, Spring Security |
| **Database** | MySQL 8.4 with Flyway Schema Migrations |
| **Caching & Rate Limiting** | Redis 7 (Sliding-window counter & token bucket) |
| **Frontend** | React 18, TypeScript, Vite, Vanilla CSS Design System, Lucide Icons |
| **AI Layer** | Google Gemini API (gemini-2.5-flash) with Spring Tool Execution Pipeline |
| **Containerization** | Docker, Docker Compose |

---

## License

This project is licensed under the Apache License 2.0.
