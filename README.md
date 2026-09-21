# Sentinel — Intelligent API Gateway, Observability & Security Platform

[![Live Frontend](https://img.shields.io/badge/Live%20Demo-Sentinel%20Dashboard-blue?style=for-the-badge&logo=render)](https://sentinel-frontend-cdmv.onrender.com)
[![Live Backend API](https://img.shields.io/badge/Live%20Gateway-sentinel--api-brightgreen?style=for-the-badge&logo=render)](https://sentinel-api-v33r.onrender.com)
[![Database](https://img.shields.io/badge/Database-Aiven%20MySQL-orange?style=for-the-badge&logo=mysql)](https://aiven.io)
[![Cache & Rate Limit](https://img.shields.io/badge/Cache-Upstash%20Redis-red?style=for-the-badge&logo=redis)](https://upstash.com)
[![AI Engine](https://img.shields.io/badge/AI%20Assistant-Google%20Gemini-purple?style=for-the-badge&logo=google)](https://ai.google.dev)

Sentinel is a production-ready, high-performance API Gateway, Observability, and Security Control platform designed to connect, monitor, protect, and analyze backend applications with zero friction.

---

## 🌐 Live Deployments & Cloud Infrastructure

| Service | Hosting / Provider | Live URL / Endpoint | Details |
| :--- | :--- | :--- | :--- |
| **Frontend Dashboard** | Render | [`https://sentinel-frontend-cdmv.onrender.com`](https://sentinel-frontend-cdmv.onrender.com) | React 18, TypeScript, Vite, Responsive Dark Design System |
| **Backend Gateway & API** | Render | [`https://sentinel-api-v33r.onrender.com`](https://sentinel-api-v33r.onrender.com) | Spring Boot 3.4, Java 21 LTS, Actuator, Gateway Forwarding |
| **Cloud Database** | Aiven | Managed Cloud MySQL 8.4 | Multi-tenant isolation, SSL/TLS, Flyway migrations |
| **Distributed Cache / Rate Limiting** | Upstash | Serverless Redis (TLS/SSL) | Sliding-window rate limiter, token tracking, low latency |
| **AI Assistant Engine** | Google | Gemini API (`gemini-2.5-flash`) | Live telemetry diagnosis, root-cause analysis, tool calling |

---

## 📐 System Architecture

```text
                                CLIENT / USER TRAFFIC
                                          │
                                          │  Header: X-Sentinel-API-Key: sk_sentinel_...
                                          ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                 SENTINEL CLOUD PLATFORM                                   │
│  [Frontend UI: Render]  ◄──────────────────────────────────►  [Backend Gateway: Render]   │
│                                                                                           │
│  ┌─────────────────────────────────┐           ┌───────────────────────────────────────┐  │
│  │   JWT Auth & Multi-Tenancy      │           │   Non-Blocking Gateway Forwarding     │  │
│  │   Tenant & Workspace Isolation  │           │   Sub-millisecond Proxy Pipeline      │  │
│  └─────────────────────────────────┘           └───────────────────────────────────────┘  │
│  ┌─────────────────────────────────┐           ┌───────────────────────────────────────┐  │
│  │   Auto API Discovery & Catalog  │           │   Distributed Rate Limiter            │  │
│  │   Swagger / Live Route Parser   │           │   Sliding-Window & Token Bucket       │  │
│  └─────────────────────────────────┘           └───────────────────────────────────────┘  │
│  ┌─────────────────────────────────┐           ┌───────────────────────────────────────┐  │
│  │   Live Telemetry & Logs Engine  │           │   Sentinel AI Assistant (Gemini)      │  │
│  │   p50/p95/p99 Latency & RPS     │           │   Grounded Telemetry Tool Calling     │  │
│  └─────────────────────────────────┘           └───────────────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────────────┬─────────────────────┘
                        │                                             │
      State & Data      ▼                                             ▼  Telemetry & Cache
┌───────────────────────────────────────┐                 ┌─────────────────────────────────┐
│          Aiven Cloud MySQL            │                 │       Upstash Cloud Redis       │
│  (Apps, Keys, Users, Logs, Catalogs)  │                 │  (Sliding-Window Rates, Tokens) │
└───────────────────────────────────────┘                 └─────────────────────────────────┘
                        │
                        │ Proxied Traffic & Health Probing
                        ▼
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                 YOUR APPLICATION BACKEND                                  │
│                                                                                           │
│        (Hosted: e.g. https://api.mycompany.com  or  Local: http://localhost:5000)        │
│                                                                                           │
│   GET /api/v1/health             POST /api/v1/orders               PUT /api/v1/settings   │
│   GET /api/v1/users              DELETE /api/v1/items/{id}         GET /api/v1/analytics  │
│   ... and 1,000+ automatically discovered & protected routes                              │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## ✨ Key Features

* **One Connection $\rightarrow$ One API Key $\rightarrow$ All APIs**: Connect your backend application once using a single Sentinel developer key. Protect, throttle, and observe hundreds or thousands of endpoints without individual route configuration.
* **Zero-Friction Automatic API Discovery**: Automatically parses OpenAPI/Swagger specifications (`/openapi.json`, `/v3/api-docs`, `/swagger.json`) or dynamically captures and registers active routes directly from live traffic.
* **Normalized Route Catalog**: Dynamic parameters (UUIDs, hashes, numeric IDs, file extensions) are collapsed into parameterized templates (e.g. `/api/v1/items/{id}/download`), avoiding duplicate clutter in your catalog.
* **Distributed Sliding-Window Rate Limiting**: High-throughput distributed rate limiting backed by **Upstash Redis**, returning standard RFC headers (`RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset`, `429 Too Many Requests`).
* **Real-Time Observability & Telemetry**: Monitor real-time throughput (RPS), status code breakdowns (2xx, 4xx, 5xx), error spikes, and response latency percentiles (p50, p95, p99).
* **Live Request Explorer**: Full forensic request logs capturing timestamp, method, endpoint, HTTP status, execution latency, and client IPs with instant filtering.
* **Sentinel AI Assistant (Gemini-Powered)**: Conversational assistant capable of querying live telemetry, error logs, and catalog status via deterministic tool calling to provide automated root-cause diagnostics.
* **Multi-Tenant Security & Isolation**: Secure password hashing with BCrypt, cryptographic API key storage (SHA-256), JWT token authentication, and SSRF prevention.

---

## 🚀 Real-Time Gateway Usage

### 1. Connecting Your Application (30 Seconds)

1. Open the [Sentinel Dashboard](https://sentinel-frontend-cdmv.onrender.com).
2. Navigate to **Applications** $\rightarrow$ Click **[+ Import Application]**.
3. Provide:
   * **Application Name**: (e.g. `Store-Backend`)
   * **Backend Base URL**: (e.g. `https://api.mycompany.com` or local tunnel)
4. Sentinel immediately verifies backend health and generates your **Sentinel API Key** (`sk_sentinel_...`).

---

### 2. Live Request Routing Examples

Route any request through the Sentinel Gateway by pointing to the Gateway URL and attaching your `X-Sentinel-API-Key` header:

#### A. cURL Example (Production Gateway)
```bash
# Query an endpoint through the live Render Sentinel Gateway
curl -X GET "https://sentinel-api-v33r.onrender.com/api/v1/gateway/api/v1/products" \
  -H "X-Sentinel-API-Key: sk_sentinel_your_api_key_here" \
  -H "Content-Type: application/json"
```

#### B. JavaScript / Fetch Example
```javascript
const response = await fetch('https://sentinel-api-v33r.onrender.com/api/v1/gateway/api/v1/orders', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Sentinel-API-Key': 'sk_sentinel_your_api_key_here'
  },
  body: JSON.stringify({ productId: 'prod_123', quantity: 2 })
});

const data = await response.json();
console.log('Gateway Response:', data);
```

#### C. Python (Requests) Example
```python
import requests

url = "https://sentinel-api-v33r.onrender.com/api/v1/gateway/api/v1/users"
headers = {
    "X-Sentinel-API-Key": "sk_sentinel_your_api_key_here"
}

response = requests.get(url, headers=headers)
print("Status Code:", response.status_code)
print("RateLimit-Remaining:", response.headers.get("RateLimit-Remaining"))
print("Data:", response.json())
```

---

### 3. Rate Limit Enforcement Headers

When requests pass through Sentinel, Upstash Redis tracks token consumption and attaches real-time rate limit headers to the response:

```http
HTTP/1.1 200 OK
Content-Type: application/json
RateLimit-Limit: 100
RateLimit-Remaining: 94
RateLimit-Reset: 42
X-Sentinel-Gateway-Latency: 18ms
```

When rate limits are exceeded, Sentinel intercepts the request before reaching your backend:

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Retry-After: 35

{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded for application. Please retry after reset window."
}
```

---

## 🛠️ Local Development Setup

### 1. Prerequisites
* **Java 21 LTS**
* **Node.js 20+** & **npm**
* **Docker & Docker Compose** (for local MySQL & Redis)

### 2. Clone Repository
```bash
git clone https://github.com/sanjaib19328-star/Sentinal.git
cd Sentinel
```

### 3. Start Local Infrastructure
```powershell
docker compose up -d
```
* **MySQL 8.4**: Port `3307` (`sentinel-mysql`)
* **Redis 7**: Port `6379` (`sentinel-redis`)

### 4. Start Backend API
```powershell
cd backend\sentinel-api
.\mvnw.cmd clean package -DskipTests
java -jar target\sentinel-api-0.0.1-SNAPSHOT.jar
```
Backend starts at `http://localhost:8080`.

Verify health check:
```powershell
curl.exe http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 5. Start Frontend Dashboard
```powershell
cd frontend
npm install
npm run dev
```
Dashboard opens at `http://localhost:5173`.

---

## ⚙️ Environment Variables Reference

| Variable | Description | Example / Default |
| :--- | :--- | :--- |
| `DB_URL` | JDBC MySQL Connection URL | `jdbc:mysql://<host>:<port>/<db>?sslmode=require` |
| `DB_USERNAME` | MySQL database username | `avnadmin` / `sentinel` |
| `DB_PASSWORD` | MySQL database password | `******` |
| `REDIS_HOST` | Redis hostname (Upstash / Local) | `<name>.upstash.io` / `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password / token | `******` |
| `REDIS_SSL_ENABLED` | Enable SSL/TLS for Redis (required for Upstash) | `true` (prod) / `false` (local) |
| `JWT_SECRET` | 256-bit secret key for JWT signing | `your-secure-256-bit-secret-key` |
| `JWT_EXPIRATION` | JWT token expiration time in ms | `86400000` (24 Hours) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed origins | `https://sentinel-frontend-cdmv.onrender.com` |
| `GEMINI_API_KEY` | Google Gemini API Key for AI Assistant | `AIzaSy...` |
| `GATEWAY_TIMEOUT_MS` | Max upstream proxy request timeout in ms | `5000` |

---

## 📊 Tech Stack Overview

| Layer | Technologies Used |
| :--- | :--- |
| **Frontend** | React 18, TypeScript, Vite, Vanilla CSS Design System, Lucide Icons |
| **Backend & Gateway** | Java 21, Spring Boot 3.4, Spring Data JPA, Spring Security, Spring Actuator |
| **Database** | Aiven Cloud MySQL 8.4 / Local MySQL with Flyway DB Migrations |
| **Caching & Limiting** | Upstash Serverless Redis / Local Redis 7 (Sliding Window & Token Bucket) |
| **AI Telemetry Assistant** | Google Gemini (`gemini-2.5-flash`) with Deterministic Tool Execution |
| **Cloud Deployment** | Render (Web Services for Frontend & Backend) |
| **Containerization** | Docker, Docker Compose |

---

## 📄 License

This project is licensed under the **Apache License 2.0**.

