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

def run_e2e():
    print("==================================================")
    print("SENTINEL PHASE 3 LIVE E2E VERIFICATION")
    print("==================================================")

    # 1. Register / Login
    email = f"p3-user-{int(time.time())}@sentinel.com"
    pwd = "Password123!"
    print(f"\n[1] Registering user: {email}...")
    st, res = post_json("/api/v1/auth/register", {"name": "Phase 3 Tester", "email": email, "password": pwd})
    assert st == 201, f"Registration failed: {st} {res}"
    
    st, login_res = post_json("/api/v1/auth/login", {"email": email, "password": pwd})
    assert st == 200, f"Login failed: {st} {login_res}"
    token = login_res["token"]
    print(f" -> Logged in successfully. Token acquired.")

    # 2. Register target app
    print("\n[2] Registering Target Application (http://127.0.0.1:9090)...")
    st, app = post_json("/api/v1/applications", {
        "name": "Live Target Microservice",
        "description": "Downstream user & order service",
        "baseUrl": "http://127.0.0.1:9090"
    }, token=token)
    assert st == 201, f"App creation failed: {st} {app}"
    app_id = app["id"]
    print(f" -> Application registered with ID: {app_id}")

    # 3. Test Connection
    print("\n[3] Testing Connection & Probing Health...")
    st, test_conn = post_json(f"/api/v1/applications/{app_id}/connection-test", None, token=token)
    assert st == 200 and test_conn["reachable"] is True, f"Connection test failed: {test_conn}"
    print(f" -> Target reachable: {test_conn['reachable']} (Latency: {test_conn['latencyMs']}ms)")

    # 4. Import OpenAPI Specification
    print("\n[4] Importing OpenAPI 3.0 Specification...")
    spec_json = {
        "openapi": "3.0.0",
        "info": { "title": "Users Service", "version": "1.0.0" },
        "paths": {
            "/api/v1/users": {
                "get": { "summary": "List all active users" },
                "post": { "summary": "Create user account" }
            },
            "/api/v1/users/{id}": {
                "get": { "summary": "Get user by ID" }
            },
            "/api/v1/orders": {
                "get": { "summary": "List orders" }
            }
        }
    }
    st, oas_res = post_json(f"/api/v1/applications/{app_id}/openapi/import", {
        "specContent": json.dumps(spec_json)
    }, token=token)
    assert st == 200, f"OpenAPI import failed: {st} {oas_res}"
    print(f" -> Imported {oas_res['endpointsImported']} endpoints from OpenAPI spec.")

    # 5. Verify Catalog
    st, apis = get_json(f"/api/v1/applications/{app_id}/apis", token=token)
    print(f" -> Current API Catalog Count: {len(apis)}")
    for ep in apis:
        print(f"    - {ep['method']} {ep['normalizedPath']} [{ep['documentationStatus']}] - {ep['summary']}")
        assert ep["documentationStatus"] in ["DOCUMENTED", "DOCUMENTED_AND_DISCOVERED"]

    # 6. Create Scoped API Key
    print("\n[5] Creating Scoped API Key...")
    st, key_data = post_json(f"/api/v1/applications/{app_id}/keys", {
        "name": "Production Client Key",
        "rateLimitPerMinute": 100
    }, token=token)
    assert st == 201, f"API key creation failed: {st} {key_data}"
    api_key = key_data["apiKey"]
    key_id = key_data["id"]
    print(f" -> API Key created: ID={key_id}, Key={api_key[:12]}...")

    # 7. Execute Developer Test Console
    print("\n[6] Executing request via Developer API Test Console...")
    st, console_res = post_json(f"/api/v1/applications/{app_id}/apis/test-console", {
        "apiKeyId": key_id,
        "method": "GET",
        "path": "/api/v1/users",
        "queryParams": { "limit": "10" },
        "headers": { "Accept": "application/json" }
    }, token=token)
    assert st == 200, f"Test console failed: {st} {console_res}"
    print(f" -> Console Result: Status={console_res['statusCode']}, Latency={console_res['latencyMs']}ms, RequestId={console_res['requestId']}")
    print(f"    Body={console_res['responseBody']}")

    # 8. Send Gateway Traffic & Verify Status Transition
    print("\n[7] Sending live traffic through Sentinel Gateway...")
    for i in range(3):
        st, body = get_json("/api/v1/gateway/api/v1/users", headers={"X-Sentinel-API-Key": api_key})
        print(f" -> Gateway Request {i+1}: Status={st}, Data={body}")
        assert st == 200

    # Verify Catalog Status Transition to DOCUMENTED_AND_DISCOVERED
    st, apis = get_json(f"/api/v1/applications/{app_id}/apis", token=token)
    user_ep = next(e for e in apis if e["method"] == "GET" and e["normalizedPath"] == "/api/v1/users")
    print(f" -> GET /api/v1/users status is now: {user_ep['documentationStatus']} (Total Reqs: {user_ep['totalRequests']})")
    assert user_ep["documentationStatus"] == "DOCUMENTED_AND_DISCOVERED"

    # 9. Consumer Analytics
    print("\n[8] Querying Consumer Analytics for Key...")
    st, key_analytics = get_json(f"/api/v1/applications/{app_id}/keys/{key_id}/analytics", token=token)
    assert st == 200, f"Key analytics failed: {st} {key_analytics}"
    print(f" -> Key Analytics: Total={key_analytics['totalRequests']}, Success={key_analytics['successRequests']}, AvgLatency={key_analytics['avgLatencyMs']}ms, P95={key_analytics['p95LatencyMs']}ms")
    print(f"    Top Endpoints: {key_analytics['topEndpoints']}")

    # 10. System Health
    print("\n[9] Checking Platform System Health...")
    st, health = get_json("/api/v1/analytics/system/health", token=token)
    assert st == 200, f"System health failed: {st} {health}"
    print(f" -> Control Plane: {health['controlPlaneStatus']}")
    print(f" -> MySQL: {health['mysql']['status']} ({health['mysql']['latencyMs']}ms)")
    print(f" -> Redis: {health['redis']['status']} ({health['redis']['latencyMs']}ms)")
    print(f" -> Gateway Summary: Total={health['gateway']['totalRequests']}, ErrorRate={health['gateway']['errorRate']}%, AvgLatency={health['gateway']['avgLatencyMs']}ms")

    # 11. Circuit Breaker Configuration & Fast Fail
    print("\n[10] Testing Circuit Breaker Configuration & Fast Fail...")
    st, pol = put_json(f"/api/v1/applications/{app_id}/policy", {
        "enabled": True,
        "rateLimit": 100,
        "rateLimitWindowSeconds": 60,
        "circuitBreakerEnabled": True,
        "circuitFailureThreshold": 3,
        "circuitRecoveryTimeoutSeconds": 10
    }, token=token)
    assert st == 200, f"Policy update failed: {st} {pol}"

    print(" -> Triggering 3 consecutive downstream failures on /api/v1/fail...")
    for i in range(3):
        st, err = get_json("/api/v1/gateway/api/v1/fail", headers={"X-Sentinel-API-Key": api_key})
        print(f"    Failure {i+1}: Status={st}")

    print(" -> Verifying Circuit Breaker trips to OPEN and fast-fails subsequent requests...")
    st, cb_res = get_json("/api/v1/gateway/api/v1/users", headers={"X-Sentinel-API-Key": api_key})
    print(f"    Gateway Response during OPEN: Status={st}, Error={cb_res.get('error')}")
    assert st == 503 and cb_res.get("error") == "CIRCUIT_BREAKER_OPEN"

    st, cb_status = get_json(f"/api/v1/applications/{app_id}/circuit-breaker", token=token)
    print(f" -> Circuit Breaker Status: State={cb_status['state']}, Failures={cb_status['consecutiveFailures']}, TimeUntilRecovery={cb_status['timeUntilRecoverySeconds']}s")
    assert cb_status["state"] == "OPEN"

    print("\n==================================================")
    print("ALL PHASE 3 END-TO-END VERIFICATIONS PASSED SUCCESSFULLY!")
    print("==================================================")

if __name__ == "__main__":
    run_e2e()
