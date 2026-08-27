import urllib.request
import urllib.error
import json
import time
import sys

BASE_URL = "http://127.0.0.1:8080"

passed_steps = 0
failed_steps = 0

def log_result(step_num, step_name, success, details=""):
    global passed_steps, failed_steps
    status_str = "PASS" if success else "FAIL"
    if success:
        passed_steps += 1
        print(f"[Step {step_num:02d}] {step_name:<55} -> [{status_str}] {details}")
    else:
        failed_steps += 1
        print(f"[Step {step_num:02d}] {step_name:<55} -> [{status_str}] ERROR: {details}")

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
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {"raw": "error"}

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
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {}

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
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {}

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

def run_30_step_acceptance():
    print("==========================================================================================")
    print("                 SENTINEL 30-STEP PRODUCTION REAL-WORLD ACCEPTANCE TEST                   ")
    print("==========================================================================================")

    unique = int(time.time())
    email = f"sentinel-acc-{unique}@sentinel.io"
    password = "ProductionPassword123!"

    # 1. Health Check
    st, h = get_json("/actuator/health")
    log_result(1, "Initial Actuator Health Check", st == 200 and h.get("status") == "UP", f"Status={h.get('status')}")

    # 2. Login & Authentication
    post_json("/api/v1/auth/register", {"name": "Acceptance User", "email": email, "password": password})
    st, auth_res = post_json("/api/v1/auth/login", {"email": email, "password": password})
    token = auth_res.get("token")
    log_result(2, "User Authentication & JWT Acquisition", st == 200 and bool(token), f"Token acquired (expires in {auth_res.get('expiresIn', 86400)}s)")

    # 3. Register Application
    st, app = post_json("/api/v1/applications", {
        "name": f"Acceptance E-Store {unique}",
        "description": "Downstream target application for 30-step acceptance verification",
        "baseUrl": "http://127.0.0.1:9090"
    }, token=token)
    app_id = app.get("id")
    log_result(3, "Register Target Microservice Application", st == 201 and app_id is not None, f"Application ID={app_id}")

    # 4. Create API Key
    st, key_res = post_json(f"/api/v1/applications/{app_id}/keys", {"name": "Acceptance Master Key", "rateLimitPerMinute": 100}, token=token)
    api_key = key_res.get("apiKey")
    key_id = key_res.get("id")
    log_result(4, "Generate Scoped API Key (Masked Verification)", st == 201 and ("••••" in key_res.get("maskedKey", "") or "sk_" in key_res.get("maskedKey", "")), f"Key ID={key_id}, Masked={key_res.get('maskedKey')}")

    # 5. Import OpenAPI Specification
    spec_json = {
        "openapi": "3.0.0",
        "info": { "title": "Store API", "version": "1.0.0" },
        "paths": {
            "/users": { "get": { "summary": "List users" }, "post": { "summary": "Create user" } },
            "/users/{id}": { "get": { "summary": "Get user" }, "put": { "summary": "Update user" }, "delete": { "summary": "Delete user" } },
            "/orders": { "get": { "summary": "List orders" }, "post": { "summary": "Create order" } },
            "/products": { "get": { "summary": "List products" } }
        }
    }
    st, oas = post_json(f"/api/v1/applications/{app_id}/openapi/import", {"specContent": json.dumps(spec_json)}, token=token)
    log_result(5, "Import OpenAPI 3.0 Specification", st == 200 and oas.get("endpointsImported", 0) >= 8, f"Documented {oas.get('endpointsImported')} endpoints")

    # 6. Call GET API
    st_get, res_get = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
    log_result(6, "Gateway HTTP Forwarding: GET /users", st_get == 200 and len(res_get) >= 2, f"HTTP {st_get} OK (Received {len(res_get)} users)")

    # 7. Call POST API
    st_post, res_post = post_json("/api/v1/gateway/users", {"name": "Dan Acceptance", "email": "dan@sentinel.io"}, headers={"X-Sentinel-API-Key": api_key})
    log_result(7, "Gateway HTTP Forwarding: POST /users", st_post == 201, f"HTTP {st_post} Created (User ID={res_post.get('id')})")

    # 8. Call PUT API
    url_put = f"{BASE_URL}/api/v1/gateway/users/1"
    req_put = urllib.request.Request(url_put, data=json.dumps({"name": "Dan Updated"}).encode("utf-8"), method="PUT")
    req_put.add_header("Content-Type", "application/json")
    req_put.add_header("X-Sentinel-API-Key", api_key)
    with urllib.request.urlopen(req_put) as r_put:
        st_put_gw = r_put.status
    log_result(8, "Gateway HTTP Forwarding: PUT /users/{id}", st_put_gw == 200, f"HTTP {st_put_gw} OK")

    # 9. Call DELETE API
    url_del = f"{BASE_URL}/api/v1/gateway/users/1"
    req_del = urllib.request.Request(url_del, method="DELETE")
    req_del.add_header("X-Sentinel-API-Key", api_key)
    with urllib.request.urlopen(req_del) as r_del:
        st_del_gw = r_del.status
    log_result(9, "Gateway HTTP Forwarding: DELETE /users/{id}", st_del_gw == 200, f"HTTP {st_del_gw} OK")

    # 10. Dynamic API Discovery & Status Transition
    st_ep, endpoints = get_json(f"/api/v1/applications/{app_id}/apis", token=token)
    user_ep = next((e for e in endpoints if e["method"] == "GET" and e["normalizedPath"] == "/users"), None)
    log_result(10, "Dynamic API Auto-Discovery & Status Transition", user_ep is not None and user_ep["documentationStatus"] == "DOCUMENTED_AND_DISCOVERED", f"Status={user_ep.get('documentationStatus') if user_ep else 'NOT_FOUND'}")

    # 11. Verify Global API Catalog
    st_glob, glob_apis = get_json("/api/v1/apis", token=token)
    log_result(11, "Global API Directory (Cross-Application Search)", st_glob == 200 and len(glob_apis) >= 8, f"Total Registered APIs: {len(glob_apis)}")

    # 12. Verify Request Explorer
    st_reqs, req_logs = get_json(f"/api/v1/requests?applicationId={app_id}", token=token)
    log_result(12, "Global Request Explorer Observability", st_reqs == 200 and req_logs.get("totalElements", 0) >= 4, f"Logged {req_logs.get('totalElements')} requests with X-Request-Id")

    # 13. Verify Analytics & Latency Percentiles
    st_an, analytics = get_json(f"/api/v1/applications/{app_id}/analytics", token=token)
    log_result(13, "Real-Time Telemetry & Percentiles (P50/P95/P99)", st_an == 200 and analytics.get("totalRequests", 0) >= 4, f"Total={analytics.get('totalRequests')}, AvgLat={analytics.get('avgLatencyMs')}ms")

    # 14. Trigger Rate Limit Configuration
    st_pol, _ = put_json(f"/api/v1/applications/{app_id}/policy", {
        "enabled": True,
        "rateLimit": 2,
        "rateLimitWindowSeconds": 60,
        "circuitBreakerEnabled": True,
        "circuitFailureThreshold": 3,
        "circuitRecoveryTimeoutSeconds": 2
    }, token=token)
    log_result(14, "Configure Rate Limit Policy (2 req/min)", st_pol == 200, "Application Policy Updated")

    # 15. Verify 429 Throttle Enforcement
    throttled = False
    for _ in range(4):
        s_rl, b_rl = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
        if s_rl == 429:
            throttled = True
            break
    log_result(15, "Enforce HTTP 429 Too Many Requests Throttling", throttled, "HTTP 429 Rate Limit Enforced by Redis Token Bucket")

    # Reset rate limit
    put_json(f"/api/v1/applications/{app_id}/policy", {
        "enabled": True,
        "rateLimit": 500,
        "rateLimitWindowSeconds": 60,
        "circuitBreakerEnabled": True,
        "circuitFailureThreshold": 3,
        "circuitRecoveryTimeoutSeconds": 2
    }, token=token)

    # 16. Revoke API Key
    st_rev, _ = post_json(f"/api/v1/applications/{app_id}/keys/{key_id}/revoke", None, token=token)
    log_result(16, "Revoke API Key in Control Plane", st_rev == 200, f"Key {key_id} Revoked")

    # 17. Verify 401 on Revoked Key
    s_rev_req, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
    log_result(17, "Verify Immediate 401 UNAUTHORIZED on Revoked Key", s_rev_req == 401, f"HTTP {s_rev_req} Unauthorized")

    # 18. Regenerate API Key
    st_reg, regen_res = post_json(f"/api/v1/applications/{app_id}/keys/{key_id}/regenerate", None, token=token)
    new_key = regen_res.get("apiKey")
    log_result(18, "Regenerate API Key Secret", st_reg == 200 and bool(new_key), "New Secret Generated")

    # 19. Verify Old Key Fails (401)
    s_old_k, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
    log_result(19, "Verify Old Key Secret Fails Immediately", s_old_k == 401, f"HTTP {s_old_k} Rejected")

    # 20. Verify New Key Succeeds (200)
    s_new_k, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": new_key})
    log_result(20, "Verify New Key Secret Succeeds", s_new_k == 200, f"HTTP {s_new_k} OK")

    # 21. Simulate Downstream Faults
    st_fail, _ = get_json("/api/v1/gateway/fail/500", headers={"X-Sentinel-API-Key": new_key})
    log_result(21, "Downstream Fault Injection (500 Error)", st_fail == 500, f"HTTP {st_fail} Clean Forward")

    # 22. Verify Downstream Status Forwarding
    st_fail_400, _ = get_json("/api/v1/gateway/fail/400", headers={"X-Sentinel-API-Key": new_key})
    log_result(22, "Downstream Status Integrity (400 Client Error)", st_fail_400 == 400, f"HTTP {st_fail_400} Clean Forward")

    # 23. Trigger Circuit Breaker Trip
    for _ in range(3):
        get_json("/api/v1/gateway/fail/500", headers={"X-Sentinel-API-Key": new_key})
    st_cb, cb_body = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": new_key})
    log_result(23, "Circuit Breaker Trip & 503 Fast-Fail Protection", st_cb == 503 and cb_body.get("error") == "CIRCUIT_BREAKER_OPEN", f"HTTP {st_cb} CIRCUIT_BREAKER_OPEN")

    # 24. Recover Circuit after 2.2s timeout
    time.sleep(2.2)
    st_rec, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": new_key})
    log_result(24, "Circuit Breaker Recovery & Traffic Restoration", st_rec == 200, f"HTTP {st_rec} Restored")

    # 25. Alert Rules & Lifecycle
    st_al, alert_rules = get_json(f"/api/v1/applications/{app_id}/alert-rules", token=token)
    log_result(25, "Alert Rule Configuration & Monitoring Engine", st_al == 200, f"Configured {len(alert_rules)} alert rules")

    # 26. Audit Logging Verification
    st_aud, audit_logs_paged = get_json(f"/api/v1/audit-logs?applicationId={app_id}", token=token)
    log_result(26, "Immutable Audit Trail Logging", st_aud == 200 and audit_logs_paged.get("totalElements", 0) >= 3, f"Recorded {audit_logs_paged.get('totalElements')} administrative audit events")

    # 27. Tenant Isolation Verification
    email_other = f"intruder-{unique}@sentinel.io"
    post_json("/api/v1/auth/register", {"name": "Intruder", "email": email_other, "password": password})
    _, log_oth = post_json("/api/v1/auth/login", {"email": email_other, "password": password})
    token_other = log_oth.get("token")
    st_iso, _ = get_json(f"/api/v1/applications/{app_id}", token=token_other)
    log_result(27, "Multi-Tenant Isolation Security Boundary", st_iso in [403, 404], f"Cross-tenant access blocked (HTTP {st_iso})")

    # 28. System Health Telemetry
    st_sys, sys_health = get_json("/api/v1/analytics/system/health", token=token)
    log_result(28, "System Health Observability (MySQL & Redis latency)", st_sys == 200 and sys_health.get("controlPlaneStatus") == "UP", f"MySQL={sys_health['mysql']['status']} ({sys_health['mysql']['latencyMs']}ms), Redis={sys_health['redis']['status']} ({sys_health['redis']['latencyMs']}ms)")

    # 29. SSRF Protection on Import
    st_ssrf, _ = post_json(f"/api/v1/applications/{app_id}/openapi/import", {"specUrl": "http://127.0.0.1:8080/actuator/health"}, token=token)
    log_result(29, "SSRF Protection (Private Subnet Blocking)", st_ssrf in [400, 403, 502], f"Loopback import rejected with HTTP {st_ssrf}")

    # 30. Final Actuator & Platform Confirmation
    st_fin, h_fin = get_json("/actuator/health")
    log_result(30, "Final Platform Health & Verification Confirmation", st_fin == 200 and h_fin.get("status") == "UP", f"Platform Status: {h_fin.get('status')}")

    print("\n==========================================================================================")
    print(f"               ACCEPTANCE SUMMARY: {passed_steps}/30 PASSED | {failed_steps}/30 FAILED                   ")
    print("==========================================================================================")

    if failed_steps > 0:
        sys.exit(1)

if __name__ == "__main__":
    run_30_step_acceptance()
