# Sentinel API Gateway Specification

## Ingress Route Routing Pattern

All external API traffic flows through Sentinel's gateway prefix:
```text
http://localhost:8080/api/v1/gateway/{path}
```

Sentinel identifies the destination target service via the `X-Sentinel-API-Key` request header:
1. Sentinel extracts `X-Sentinel-API-Key` and hashes it with SHA-256.
2. The key is matched against `api_keys` to resolve the associated `application_id`.
3. Sentinel queries the application's `base_url` (e.g. `http://localhost:9090`).
4. The remaining URI path and query string are forwarded downstream.

```text
Incoming:  GET /api/v1/gateway/users/123?include=profile
Header:    X-Sentinel-API-Key: sk_sentinel_xyz123
Target:    http://localhost:9090/users/123?include=profile
```

## Security & Safe Forwarding

- **Hop-by-Hop Headers Dropped**: `Connection`, `Keep-Alive`, `Proxy-Authenticate`, `Proxy-Authorization`, `TE`, `Trailer`, `Transfer-Encoding`, `Upgrade`, `Host`.
- **Internal Headers Stripped**: Any incoming header prefixed with `X-Sentinel-` (except authenticated key) or `X-Internal-` is stripped to prevent header injection.
- **Trace Headers Appended**:
  - `X-Request-Id`: Unique UUIDv4 assigned to every incoming request.
  - `X-Forwarded-For`: Original client IP.
  - `X-Forwarded-Proto`: Ingress protocol (`http` or `https`).
  - `X-RateLimit-Limit`: Maximum requests allowed in active quota window.
  - `X-RateLimit-Remaining`: Remaining request allowance.
  - `X-RateLimit-Reset`: UTC epoch timestamp when quota resets.
