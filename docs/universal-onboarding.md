# Sentinel Universal Application Onboarding & Upstream Authentication

## 1. Architectural Overview

Sentinel is a **Universal API Management & Governance Platform** designed to onboard and govern **any** externally deployed HTTP/HTTPS REST API.

Sentinel cleanly decouples consumer authentication from upstream application authentication using a strict **Two-Layer Credential Architecture**:

```
[API Consumer / Client Application]
                │
                │ Layer 1: Sentinel Consumer API Key
                │ Header: `X-Sentinel-API-Key: sk_sentinel_...`
                ▼
┌──────────────────────────────────────────────────────────┐
│                   SENTINEL GATEWAY                      │
│                                                          │
│  1. Authenticate Consumer Key (SHA-256 MySQL Hash)       │
│  2. Enforce Multi-Level Rate Limits & Redis Quotas       │
│  3. Sanitize Consumer Headers (Strip Injected Upstream)  │
│  4. Decrypt Upstream Secret (AES-256-GCM)                │
│  5. Pre-Compute Target URI & Upstream Headers            │
│  6. Dynamic API Discovery & Normalization                │
│  7. Telemetry & Analytics Capture                        │
└──────────────────────────────────────────────────────────┘
                │
                │ Layer 2: Customer Upstream Authentication
                │ (Bearer Token / API Key Header / Query / Basic / Custom)
                ▼
[Customer's Existing Deployed API / Microservice]
```

---

## 2. Two-Layer Authentication Separation

| Layer | Purpose | Credentials | Storage & Handling |
| :--- | :--- | :--- | :--- |
| **Layer 1: Consumer $\to$ Sentinel** | Controls client access, rate limiting, and consumption quotas. | `X-Sentinel-API-Key: sk_sentinel_<32-hex>` | SHA-256 hashed at rest in `api_keys` table. Raw key only displayed once upon generation. |
| **Layer 2: Sentinel $\to$ Upstream API** | Authenticates Sentinel against customer's existing backend. | Customer Upstream Secrets (`NONE`, `BEARER_TOKEN`, `API_KEY_HEADER`, `API_KEY_QUERY`, `BASIC_AUTH`, `CUSTOM_HEADER`). | Encrypted at rest using **AES-256-GCM** in `applications.upstream_auth_config_encrypted`. Masked (`••••••••`) in all API responses. |

> [!IMPORTANT]
> Sentinel **never exposes** the upstream application's credentials to consumers. Consumers cannot bypass or spoof upstream credentials because Sentinel sanitizes incoming headers before forwarding.

---

## 3. Supported Upstream Authentication Strategies

Sentinel supports 6 universal authentication schemes:

1. **`NONE`**: Public upstream APIs requiring no upstream credentials.
2. **`BEARER_TOKEN`**: Standard OAuth2 / JWT bearer tokens forwarded as `Authorization: Bearer <secret>`.
3. **`API_KEY_HEADER`**: API keys forwarded in a custom header (e.g., `X-API-Key`, `X-App-Key`, `api-key`).
4. **`API_KEY_QUERY`**: API keys appended as query parameters (e.g., `?apiKey=<secret>` or `&token=<secret>`). Existing query parameters from the consumer request are preserved.
5. **`BASIC_AUTH`**: HTTP Basic Authentication forwarded as `Authorization: Basic <base64(user:pass)>`.
6. **`CUSTOM_HEADER`**: Arbitrary custom security headers (e.g., `X-Internal-Secret: <value>`).

---

## 4. Universal Onboarding Wizard (5 Steps)

When registering an application in the Sentinel UI or API:

1. **Step 1: Application Information**: Provide application name, base upstream URL (e.g. `https://api.mycompany.com`), and description.
2. **Step 2: Upstream Authentication**: Select the authentication scheme and supply the upstream secret/credentials.
3. **Step 3: Discovery & Governance Mode**: Configure non-intrusive traffic observation and automated path normalization.
4. **Step 4: Live Zero-Pollution Connection Test**: Sentinel probes the upstream application and verifies connectivity without polluting consumer analytics, request logs, or discovery catalogs.
5. **Step 5: Completion & Gateway Route**: Sentinel generates the consumer entrypoint `/api/v1/gateway/*` and provides instructions for generating consumer API keys.

---

## 5. Upstream Credential Rotation

Sentinel supports zero-downtime upstream credential rotation via `PUT /api/v1/applications/{id}/upstream-auth`:

- If a backend secret is rotated on the customer's infrastructure, updating Sentinel immediately propagates to all subsequent gateway requests without needing to re-issue consumer keys.
- If upstream authentication needs to be disabled or switched to public, `DELETE /api/v1/applications/{id}/upstream-auth` safely removes upstream credentials.

---

## 6. Zero-Pollution Probing & SSRF Protection

- **Zero-Pollution Connection Probing (`POST /api/v1/applications/{id}/connection-test`)**:
  - Tests connectivity directly with the upstream application.
  - Logs a dedicated audit event `UPSTREAM_CONNECTION_TESTED`.
  - Generates **zero** false request logs, zero false metrics, and zero fake API catalog endpoints.
- **SSRF Protection**:
  - Validates all external URLs (including OpenAPI spec import) against loopback, link-local, private IP addresses (RFC 1918), and restricted internal hostnames.
