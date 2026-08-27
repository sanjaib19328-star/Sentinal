# Sentinel Security Architecture & Hardening

## Key Security Controls

1. **One-Way Cryptographic Key Hashing**:
   - API keys are generated as cryptographically secure random tokens prefixed with `sk_sentinel_`.
   - The plaintext key is returned strictly once to the client upon creation/regeneration.
   - The database only stores the SHA-256 hash. Plaintext secrets are never persisted or logged.

2. **Strict Multi-Tenant Isolation**:
   - Every control plane resource (application, API endpoint, key, policy, alert, log) is scoped to the authenticated user ID (`owner_id`).
   - Cross-tenant lookups and operations are strictly prevented and rejected with `404 NOT_FOUND` or `403 FORBIDDEN`.

3. **SSRF Protection on OpenAPI Import**:
   - URL imports validate target hostnames and IPs against private and local ranges:
     - Loopback (`127.0.0.1`, `::1`)
     - RFC1918 Private ranges (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`)
     - Link-Local (`169.254.0.0/16`)
     - Internal domains (`.local`, `.internal`, `.lan`, `localhost`)
   - Restricts spec download payloads to 10MB and enforces strict 10s timeouts.

4. **Header Injection & Hop-by-Hop Protection**:
   - All internal headers (`X-Sentinel-*`, `X-Internal-*`) are stripped from incoming client requests.
   - Hop-by-hop HTTP/1.1 transport headers are dropped before proxy forwarding.
   - Header payload ceiling of 16KB enforced.

5. **Safe-by-Default Retries**:
   - Automatic retry is enabled only for idempotent HTTP methods (`GET`, `HEAD`, `OPTIONS`).
   - Mutations (`POST`, `PUT`, `PATCH`, `DELETE`) are never retried unless explicitly configured in the application's policy.
