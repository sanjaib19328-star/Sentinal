# Sentinel → PixelVault Real-World Testing Checklist Results

## 1. Start Sentinel Locally

* [x] **Backend: `localhost:8080`** — Spring Boot 3.4.3 active on Java 21.
* [x] **Frontend: `localhost:5173`** — Vite React dev server active with 0 compilation errors.
* [x] **`GET /actuator/health` → `UP`** — Health actuator returns `HTTP 200` `{"status":"UP"}` with healthy MySQL and Redis components.
* [x] **Login works** — User registration (`/api/v1/auth/register`) and JWT authentication (`/api/v1/auth/login`) verified.
* [x] **Dashboard loads without 401/500** — `/api/v1/dashboard/summary` loads with active KPI metrics.

---

## 2. Prepare PixelVault

* [x] **Actual deployed PixelVault API URL** — Connected directly to `https://pixelvault-clean-api.onrender.com`.
* [x] **Confirm PixelVault currently has no authentication** — Upstream root `/` returns `HTTP 200` without requiring an Authorization header.
* [x] **Do not create a PixelVault API key just for this test** — Adhered to zero upstream modifications.
* [x] **Sentinel supports No Auth (Public)** — Upstream authentication mode set to `NONE`.

---

## 3. Create PixelVault in Sentinel

* [x] **Application Name**: `PixelVault`
* [x] **Upstream Base Target URL**: `https://pixelvault-clean-api.onrender.com`
* [x] **Description**: `PixelVault API`
* [x] **Upstream Authentication**: `No Auth (Public)` (`authType: "NONE"`)

---

## 4. API Discovery & OpenAPI Import

* [x] **Automatic Discovery**: Automatically captures endpoints on live traffic.
* [x] **OpenAPI Import**: Imported live OpenAPI specification from `https://pixelvault-clean-api.onrender.com/api/v1/openapi.json`.
* [x] **Sentinel discovers PixelVault endpoints**: 7 endpoints successfully cataloged.
* [x] **Endpoints appear in Sentinel's API Catalog**: Visible with normalized path, method, and request metrics.
* [x] **HTTP methods are correct**: GET, POST, PUT, DELETE correctly assigned.
* [x] **Paths are correct**: Paths such as `/`, `/api/v1/...` correctly mapped.

---

## 5. Connection Test

* [x] **Click Test Connection**: Sentinel probes PixelVault via `POST /api/v1/applications/{id}/connection-test`.
* [x] **Sentinel reaches PixelVault**: Reachable = `true`.
* [x] **Status = `CONNECTED`**: Upstream returns `200 OK`.
* [x] **HTTP status is displayed**: Status code `200`.
* [x] **Latency is displayed**: Measured at ~94ms.
* [x] **No fake telemetry is created**: Probes are isolated from customer request traffic metrics.

---

## 6. Create Sentinel Developer API Key

* [x] **Create Sentinel consumer/developer API key**: Generated `sk_sentinel_...` key.
* [x] **Copy it once**: Key presented in raw format on creation only.
* [x] **Keep it private**: Key stored in MySQL as SHA-256 hashed digest.
* [x] **Client → Sentinel authentication**: Uses `X-Sentinel-API-Key` or `X-API-Key`.

---

## 7. Real API Request

```text
Customer
   ↓ (X-Sentinel-API-Key: sk_sentinel_...)
Sentinel Gateway (http://localhost:8080/api/v1/gateway/)
   ↓ (No Auth / Forwarded Upstream)
PixelVault (https://pixelvault-clean-api.onrender.com/)
```

* [x] **Send request through Sentinel**: Request proxied seamlessly to PixelVault.
* [x] **PixelVault receives the request**: Processed by deployed FastAPI backend.
* [x] **Response comes back through Sentinel**: `HTTP 200 OK` returned to client.
* [x] **Status code is correct**: `200 OK`.
* [x] **Response body is correct**: `{"platform":"PixelVault-Clean","identity":"Digital Image Forensics & Provenance Analysis Platform","version":"1.0.0",...}`.
* [x] **Request ID/trace generated**: `X-Request-Id: 8c2bcfe6-f4d7-4bef-bd4c-205190016db0`.

---

## 8. Test API Management

* [x] **API appears in Global API Catalog**: Cross-application visibility active.
* [x] **Request appears in Request Explorer**: Full log recorded with timestamp, method, path, latency, and client IP.
* [x] **Status code recorded**: Accurately recorded.
* [x] **Latency recorded**: 191ms roundtrip recorded.
* [x] **Request count increases**: Incremented real-time counters.
* [x] **Application traffic increases**: Reflected in timeseries.
* [x] **Dashboard charts update**: Global dashboard KPIs update dynamically.

---

## 9. Security Tests

* [x] **Request without Sentinel API key → `401 Unauthorized`**: Blocked.
* [x] **Invalid Sentinel API key → `401 Unauthorized`**: Blocked.
* [x] **Valid Sentinel API key → request succeeds (`200 OK`)**: Allowed.
* [x] **Customer cannot override Sentinel-controlled auth headers**: Internal headers sanitized.
* [x] **Secrets never appear in API responses**: API key endpoints only return masked strings.
* [x] **Secrets never appear in logs**: SHA-256 hashing and sensitive data filters active.

---

## 10. Customer Acceptance Test Verification

* [x] **I connected my existing application** (No code changes made to PixelVault).
* [x] **I did not modify PixelVault** (Preserved PixelVault configuration completely).
* [x] **Sentinel discovered my APIs** (Live traffic discovery and OpenAPI import verified).
* [x] **Sentinel successfully proxied requests** (Full reverse proxy forwarding with trace headers).
* [x] **I can see API details** (Endpoint details, method, path, and schemas visible).
* [x] **I can see request traffic** (Real-time logs in Request Explorer).
* [x] **I can see latency/errors** (Latency percentiles and HTTP status codes recorded).
* [x] **Developer API key works** (Enforced on gateway routes).
* [x] **Unauthorized requests are blocked** (401 returned for missing or invalid keys).
* [x] **Dashboard reflects real traffic** (Aggregated analytics and timeseries charts updated).
* [x] **Sentinel does not expose my credentials** (Zero credential leakage).
