import urllib.request
import urllib.error
import json
import time

BASE_URL = "http://127.0.0.1:8080"

def post_json(path, data, token=None, headers=None):
    url = f"{BASE_URL}{path}"
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    if headers:
        for k, v in headers.items():
            req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, {"raw": body}

def get_json(path, token=None, headers=None):
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(url, method="GET")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    if headers:
        for k, v in headers.items():
            req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, {"raw": body}

def put_json(path, data, token=None):
    url = f"{BASE_URL}{path}"
    body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="PUT")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, {"raw": body}

def delete_req(path, token=None):
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(url, method="DELETE")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, {}
    except urllib.error.HTTPError as e:
        return e.code, {}

def run_phase4_e2e():
    print("================================================================================")
    print("           SENTINEL PHASE 4 COMPREHENSIVE E2E REAL-WORLD VALIDATION             ")
    print("================================================================================")

    # 1. User Registration & Authentication
    email = f"sentinel-admin-{int(time.time())}@sentinel.io"
    password = "ProductionPassword123!"
    print(f"\n[Step 1] Registering Management User: {email}...")
    st, reg = post_json("/api/v1/auth/register", {"name": "Sentinel Operations Admin", "email": email, "password": password})
    assert st == 201, f"Registration failed: {st} {reg}"

    st, login = post_json("/api/v1/auth/login", {"email": email, "password": password})
    assert st == 200, f"Login failed: {st} {login}"
    token = login["token"]
    print(" -> User authenticated successfully. Bearer JWT acquired.")

    # 2. Register Target Microservice Application
    print("\n[Step 2] Registering Downstream Target Microservice (http://127.0.0.1:9090)...")
    st, app = post_json("/api/v1/applications", {
        "name": "E-Commerce Core Microservice",
        "description": "Downstream user management, orders, and product catalog service",
        "baseUrl": "http://127.0.0.1:9090"
    }, token=token)
    assert st == 201, f"App registration failed: {st} {app}"
    app_id = app["id"]
    print(f" -> Target Application provisioned. ID: {app_id}")

    # 3. Connection Probing & Live Health Check
    print("\n[Step 3] Probing Target Health Endpoint...")
    st, probe = post_json(f"/api/v1/applications/{app_id}/connection-test", None, token=token)
    assert st == 200 and probe["reachable"] is True, f"Connection probe failed: {probe}"
    print(f" -> Target Reachable: {probe['reachable']} (Round-trip Latency: {probe['latencyMs']}ms)")

    # 4. Import OpenAPI Specification
    print("\n[Step 4] Importing OpenAPI 3.0 Specification into API Catalog...")
    spec_json = {
        "openapi": "3.0.0",
        "info": { "title": "E-Commerce API", "version": "1.0.0" },
        "paths": {
            "/users": {
                "get": { "summary": "List all active users" },
                "post": { "summary": "Create user account" }
            },
            "/users/{id}": {
                "get": { "summary": "Get user by ID" },
                "put": { "summary": "Update user" },
                "delete": { "summary": "Delete user" }
            },
            "/orders": {
                "get": { "summary": "List orders" },
                "post": { "summary": "Place order" }
            },
            "/products": {
                "get": { "summary": "List products" }
            }
        }
    }
    st, oas = post_json(f"/api/v1/applications/{app_id}/openapi/import", {"specContent": json.dumps(spec_json)}, token=token)
    assert st == 200, f"OpenAPI import failed: {st} {oas}"
    print(f" -> Imported {oas['endpointsImported']} OpenAPI endpoints. Total documented: {oas.get('totalDocumentedEndpoints', oas.get('endpointsImported'))}")

    # 5. Verify Global API Catalog
    print("\n[Step 5] Querying Cross-Application Global API Catalog (GET /api/v1/apis)...")
    st, global_apis = get_json("/api/v1/apis", token=token)
    assert st == 200, f"Global API Catalog fetch failed: {st} {global_apis}"
    print(f" -> Global Catalog contains {len(global_apis)} endpoints.")
    for a in global_apis:
        print(f"    - {a['method']} {a['normalizedPath']} [{a['documentationStatus']}] - App: {a['applicationName']}")

    # 6. Generate Scoped API Key
    print("\n[Step 6] Generating Scoped API Key with Rate Limit...")
    st, key_res = post_json(f"/api/v1/applications/{app_id}/keys", {
        "name": "E2E Automated Client Key",
        "rateLimitPerMinute": 50
    }, token=token)
    assert st == 201, f"API key generation failed: {st} {key_res}"
    api_key = key_res["apiKey"]
    key_id = key_res["id"]
    print(f" -> API Key generated: ID={key_id}, Prefix={api_key[:14]}... (Masked: {key_res['maskedKey']})")

    # 7. Execute Developer Test Console
    print("\n[Step 7] Testing via Developer API Console (POST /api/v1/applications/{id}/apis/test-console)...")
    st, console_res = post_json(f"/api/v1/applications/{app_id}/apis/test-console", {
        "apiKeyId": key_id,
        "method": "GET",
        "path": "/users",
        "headers": { "Accept": "application/json" }
    }, token=token)
    assert st == 200, f"Test console execution failed: {st} {console_res}"
    print(f" -> Console Result: Status={console_res['statusCode']}, Latency={console_res['latencyMs']}ms, TraceId={console_res['requestId']}")
    print(f"    Response: {console_res['responseBody']}")

    # 8. Send Gateway Ingress Traffic Across Multiple APIs
    print("\n[Step 8] Sending Live Gateway Traffic across User, Order, and Product APIs...")
    test_endpoints = [
        ("GET", "/api/v1/gateway/users"),
        ("POST", "/api/v1/gateway/users", {"name": "Charlie Tester", "email": "charlie@sentinel.io"}),
        ("GET", "/api/v1/gateway/users/1"),
        ("GET", "/api/v1/gateway/orders"),
        ("GET", "/api/v1/gateway/products"),
    ]
    for m, p, *rest in test_endpoints:
        if m == "GET":
            s, b = get_json(p, headers={"X-Sentinel-API-Key": api_key})
        else:
            s, b = post_json(p, rest[0], headers={"X-Sentinel-API-Key": api_key})
        print(f" -> {m} {p} -> HTTP {s} OK")
        assert s in [200, 201], f"Gateway request {m} {p} failed: {s} {b}"

    # 9. Verify Dynamic Discovery & Status Transition
    print("\n[Step 9] Verifying Auto-Discovery & Status Transition to DOCUMENTED_AND_DISCOVERED...")
    st, updated_apis = get_json(f"/api/v1/applications/{app_id}/apis", token=token)
    user_api = next(e for e in updated_apis if e["method"] == "GET" and e["normalizedPath"] == "/users")
    print(f" -> GET /users status is: {user_api['documentationStatus']} (Traffic: {user_api['totalRequests']} reqs, AvgLat: {user_api['avgLatencyMs']}ms)")
    assert user_api["documentationStatus"] == "DOCUMENTED_AND_DISCOVERED"

    # 10. Global Request Explorer Querying
    print("\n[Step 10] Querying Global Request Observability Engine (GET /api/v1/requests)...")
    st, req_logs = get_json("/api/v1/requests?size=10", token=token)
    assert st == 200, f"Request Explorer fetch failed: {st} {req_logs}"
    print(f" -> Total recorded gateway requests: {req_logs['totalElements']}")
    first_log = req_logs["content"][0]
    print(f"    Latest Log: TraceId={first_log['requestId']}, {first_log['method']} {first_log['path']} -> {first_log['statusCode']} ({first_log['latencyMs']}ms)")

    # 11. Consumer Key Analytics
    print("\n[Step 11] Inspecting Consumer Key Analytics & Latency Percentiles...")
    st, key_analytics = get_json(f"/api/v1/applications/{app_id}/keys/{key_id}/analytics", token=token)
    assert st == 200, f"Consumer analytics failed: {st} {key_analytics}"
    print(f" -> Consumer Key Analytics: Total={key_analytics['totalRequests']}, Success={key_analytics['successRequests']}, AvgLat={key_analytics['avgLatencyMs']}ms, P95={key_analytics['p95LatencyMs']}ms")

    # 12. Rate Limit & Throttling Enforcement
    print("\n[Step 12] Enforcing Rate Limit Policy & 429 Throttling...")
    # Set low rate limit
    st, pol = put_json(f"/api/v1/applications/{app_id}/policy", {
        "enabled": True,
        "rateLimit": 3,
        "rateLimitWindowSeconds": 60,
        "circuitBreakerEnabled": True,
        "circuitFailureThreshold": 3,
        "circuitRecoveryTimeoutSeconds": 10
    }, token=token)
    assert st == 200, f"Policy update failed: {st} {pol}"

    throttled = False
    for i in range(6):
        s, b = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
        if s == 429:
            throttled = True
            print(f" -> Request {i+1}: HTTP 429 RATE_LIMITED enforced by {b.get('message', 'Sentinel')}")
            break
        else:
            print(f" -> Request {i+1}: HTTP {s} OK")
    assert throttled, "Expected HTTP 429 rate limit throttle was not triggered"

    # Reset rate limit for subsequent tests
    put_json(f"/api/v1/applications/{app_id}/policy", {
        "enabled": True,
        "rateLimit": 500,
        "rateLimitWindowSeconds": 60,
        "circuitBreakerEnabled": True,
        "circuitFailureThreshold": 3,
        "circuitRecoveryTimeoutSeconds": 10
    }, token=token)

    # 13. API Key Lifecycle: Regeneration & Revocation
    print("\n[Step 13] Verifying API Key Lifecycle (Regenerate & Immediate Old Secret Invalidation)...")
    st, regen_key = post_json(f"/api/v1/applications/{app_id}/keys/{key_id}/regenerate", None, token=token)
    assert st == 200, f"Key regeneration failed: {st} {regen_key}"
    new_api_key = regen_key["apiKey"]
    print(f" -> Key regenerated with new secret: {new_api_key[:14]}...")

    # Old key MUST fail with 401
    s_old, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
    print(f" -> Old API Key request result: HTTP {s_old} (Expected 401)")
    assert s_old == 401

    # New key MUST succeed with 200
    s_new, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": new_api_key})
    print(f" -> New API Key request result: HTTP {s_new} (Expected 200)")
    assert s_new == 200

    # 14. Circuit Breaker & 503 Fast-Fail Protection
    print("\n[Step 14] Testing Circuit Breaker Downstream Protection (Trip to OPEN & 503 Fast-Fail)...")
    for i in range(3):
        s_fail, _ = get_json("/api/v1/gateway/fail/500", headers={"X-Sentinel-API-Key": new_api_key})
        print(f" -> Injected Failure {i+1}: HTTP {s_fail}")

    s_cb, cb_resp = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": new_api_key})
    print(f" -> Circuit Breaker Active Response: HTTP {s_cb}, ErrorCode={cb_resp.get('error')}")
    assert s_cb == 503 and cb_resp.get("error") == "CIRCUIT_BREAKER_OPEN"

    # 15. System Health Telemetry
    print("\n[Step 15] Checking Subsystem Health Observability (GET /api/v1/analytics/system/health)...")
    st, health = get_json("/api/v1/analytics/system/health", token=token)
    assert st == 200, f"System health fetch failed: {st} {health}"
    print(f" -> Control Plane: {health['controlPlaneStatus']}")
    print(f" -> MySQL 8.4: {health['mysql']['status']} ({health['mysql']['latencyMs']}ms round-trip)")
    print(f" -> Redis 7.x: {health['redis']['status']} ({health['redis']['latencyMs']}ms round-trip)")
    print(f" -> Gateway Stats: Total={health['gateway']['totalRequests']}, ErrorRate={health['gateway']['errorRate']}%, AvgLat={health['gateway']['avgLatencyMs']}ms")

    print("\n================================================================================")
    print("        ALL 15 PHASE 4 VALIDATION SUITES PASSED FLAWLESSLY WITH REAL DATA       ")
    print("================================================================================")

if __name__ == "__main__":
    run_phase4_e2e()
