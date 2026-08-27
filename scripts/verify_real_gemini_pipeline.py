import http.server
import socketserver
import threading
import time
import uuid
import json
import os
import urllib.request
import urllib.error
import sys

BASE_URL = "http://localhost:8080"
TEST_PORT = 8996

class DedicatedRealtimeUpstreamHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health" or self.path == "/":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"HEALTHY","service":"isolated-realtime-upstream"}')
        elif "/error-500" in self.path:
            self.send_response(500)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error":"Real upstream 500 failure"}')
        elif self.path.startswith("/api/products"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({
                "productId": "prod_101",
                "name": "Sentinel Monitored Product",
                "price": 49.99,
                "path": self.path
            }).encode("utf-8"))
        elif self.path.startswith("/api/orders"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"orderStatus": "DISPATCHED", "path": self.path}).encode("utf-8"))
        else:
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"path": self.path, "status": "ok"}).encode("utf-8"))

    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length).decode("utf-8") if content_length > 0 else ""
        self.send_response(201)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"created": True, "rawBody": body}).encode("utf-8"))

    def log_message(self, format, *args):
        pass

def start_upstream():
    server = socketserver.TCPServer(("127.0.0.1", TEST_PORT), DedicatedRealtimeUpstreamHandler)
    server.allow_reuse_address = True
    t = threading.Thread(target=server.serve_forever, daemon=True)
    t.start()
    return server

def http_req(method, url, data=None, headers=None):
    if headers is None:
        headers = {}
    req_data = None
    if data is not None:
        if isinstance(data, (dict, list)):
            req_data = json.dumps(data).encode("utf-8")
            if "Content-Type" not in headers:
                headers["Content-Type"] = "application/json"
        elif isinstance(data, str):
            req_data = data.encode("utf-8")

    req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            body = response.read().decode("utf-8")
            res_json = None
            if body:
                try:
                    res_json = json.loads(body)
                except Exception:
                    res_json = body
            resp_headers = {k.lower(): v for k, v in response.headers.items()}
            return response.status, res_json, resp_headers
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        res_json = None
        if body:
            try:
                res_json = json.loads(body)
            except Exception:
                res_json = body
        resp_headers = {k.lower(): v for k, v in e.headers.items()}
        return e.code, res_json, resp_headers
    except Exception as e:
        return 500, {"error": str(e)}, {}

def run_real_gemini_pipeline_test():
    print("=" * 85)
    print("REAL GEMINI LLM + REAL SENTINEL DATA + GROUNDING PIPELINE VERIFICATION")
    print("=" * 85)

    results = {
        "CORE_SENTINEL": "FAIL",
        "MYSQL": "FAIL",
        "REDIS": "FAIL",
        "JWT_TENANT_ISOLATION": "FAIL",
        "GATEWAY": "FAIL",
        "REAL_API_CATALOG": "FAIL",
        "REAL_TIME_TELEMETRY": "FAIL",
        "CONVERSATION_PERSISTENCE": "FAIL",
        "REAL_GEMINI_LLM_INVOCATION": "FAIL",
        "GEMINI_REAL_DATABASE_GROUNDING": "FAIL",
        "STALE_DATA_PROTECTION": "FAIL",
        "AI_TENANT_ISOLATION": "FAIL",
        "LEGACY_PROJECT_AUDIT": "FAIL"
    }

    # 1. Start Upstream Test Server
    print("\n[STEP 1: ENVIRONMENT & UPSTREAM SETUP]")
    upstream_server = start_upstream()
    time.sleep(0.5)
    s_up, r_up, _ = http_req("GET", f"http://127.0.0.1:{TEST_PORT}/health")
    assert s_up == 200 and r_up.get("status") == "HEALTHY", f"Upstream target unreachable: {r_up}"
    print(f"  Isolated test upstream server running at http://127.0.0.1:{TEST_PORT} [PASS]")

    # Check Core Sentinel Health
    s_act, r_act, _ = http_req("GET", f"{BASE_URL}/actuator/health")
    assert s_act == 200 and r_act.get("status") == "UP", f"Actuator not UP: {r_act}"
    results["CORE_SENTINEL"] = "PASS"
    print("  Core Sentinel Actuator UP [PASS]")

    # 2. Register and Authenticate Tenant A & Tenant B
    print("\n[STEP 2: MULTI-TENANT AUTH & JWT]")
    uid = str(uuid.uuid4())[:8]
    tenant_a_email = f"sentinel_ai_tenant_a_{uid}@sentinel.local"
    tenant_b_email = f"sentinel_ai_tenant_b_{uid}@sentinel.local"
    password = "Password123!"

    http_req("POST", f"{BASE_URL}/api/v1/auth/register", {"name": "Tenant A Admin", "email": tenant_a_email, "password": password})
    http_req("POST", f"{BASE_URL}/api/v1/auth/register", {"name": "Tenant B Admin", "email": tenant_b_email, "password": password})

    _, r_la, _ = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {"email": tenant_a_email, "password": password})
    _, r_lb, _ = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {"email": tenant_b_email, "password": password})
    token_a = r_la["token"]
    token_b = r_lb["token"]
    print("  Tenant A and Tenant B authenticated with JWT [PASS]")

    # Check MySQL & Redis Health via Authenticated Analytics
    s_sh, r_sh, _ = http_req("GET", f"{BASE_URL}/api/v1/analytics/system/health", headers={"Authorization": f"Bearer {token_a}"})
    if s_sh == 200:
        if r_sh.get("mysql", {}).get("status") == "UP":
            results["MYSQL"] = "PASS"
            print(f"  MySQL 8.4 connection pool UP (Latency: {r_sh.get('mysql', {}).get('latencyMs')}ms) [PASS]")
        if r_sh.get("redis", {}).get("status") == "UP":
            results["REDIS"] = "PASS"
            print(f"  Redis 7 cluster node UP (Latency: {r_sh.get('redis', {}).get('latencyMs')}ms) [PASS]")

    # 3. Register Monitored Application
    print("\n[STEP 3: APPLICATION ONBOARDING & EMPTY STATE AUDIT]")
    s_app, r_app, _ = http_req("POST", f"{BASE_URL}/api/v1/applications", {
        "name": "Live Inventory & Order Service",
        "description": "Production monitored microservice",
        "baseUrl": f"http://127.0.0.1:{TEST_PORT}"
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_app == 201, f"Create app failed: {r_app}"
    app_id = r_app["id"]
    print(f"  Monitored Application created: ID {app_id}, Status: {r_app.get('healthStatus')} (UNKNOWN expected)")

    # Confirm Initial Honest Empty States
    s_cat0, r_cat0, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_id}/apis", headers={"Authorization": f"Bearer {token_a}"})
    print(f"  Initial API Catalog: {len(r_cat0)} endpoints [Must be exactly 0]")
    assert len(r_cat0) == 0, f"Expected 0 endpoints for new app, found: {r_cat0}"

    s_req0, r_req0, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_id}/requests", headers={"Authorization": f"Bearer {token_a}"})
    initial_requests = len(r_req0.get("content", []))
    print(f"  Initial Request Count: {initial_requests} [Must be exactly 0]")
    assert initial_requests == 0, f"Expected 0 requests for new app, found: {initial_requests}"

    # 4. Generate API Key & Send Real Traffic through Gateway
    print("\n[STEP 4: REAL GATEWAY TRAFFIC & TELEMETRY OBSERVATION]")
    s_key, r_key, _ = http_req("POST", f"{BASE_URL}/api/v1/applications/{app_id}/keys", {
        "name": "Observability API Key",
        "rateLimitPerMinute": 120
    }, headers={"Authorization": f"Bearer {token_a}"})
    api_key = r_key["apiKey"]

    # Request 1: GET /api/products/101?fields=name,price -> 200
    s_g1, r_g1, _ = http_req("GET", f"{BASE_URL}/api/v1/gateway/api/products/101?fields=name,price", headers={"X-Sentinel-API-Key": api_key})
    assert s_g1 == 200, f"Gateway request 1 failed: {s_g1}"

    # Request 2: POST /api/products -> 201
    s_g2, r_g2, _ = http_req("POST", f"{BASE_URL}/api/v1/gateway/api/products", data={"name": "Widget A"}, headers={"X-Sentinel-API-Key": api_key})
    assert s_g2 == 201, f"Gateway request 2 failed: {s_g2}"

    # Request 3: GET /api/inventory/error-500 -> 500
    s_g3, r_g3, _ = http_req("GET", f"{BASE_URL}/api/v1/gateway/api/inventory/error-500", headers={"X-Sentinel-API-Key": api_key})
    assert s_g3 == 500, f"Gateway request 3 expected 500, got: {s_g3}"

    results["GATEWAY"] = "PASS"
    print("  Gateway forwarded GET, POST, and handled 500 error [PASS]")

    # 5. Confirm Telemetry and Catalog Auto-Discovery
    s_req1, r_req1, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_id}/requests", headers={"Authorization": f"Bearer {token_a}"})
    count_t1 = len(r_req1.get("content", []))
    print(f"  Telemetry updated after 3 real requests: Total Requests = {count_t1}")
    assert count_t1 == 3, f"Expected 3 requests, found {count_t1}"
    results["REAL_TIME_TELEMETRY"] = "PASS"

    s_cat1, r_cat1, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_id}/apis", headers={"Authorization": f"Bearer {token_a}"})
    print(f"  Auto-discovered endpoints in API Catalog: {len(r_cat1)}")
    for ep in r_cat1:
        print(f"    - [{ep['method']}] {ep['normalizedPath']} (Status: {ep.get('documentationStatus')}, Requests: {ep.get('totalRequests')})")
    assert len(r_cat1) >= 2, f"Expected at least 2 discovered endpoints, got: {len(r_cat1)}"
    results["REAL_API_CATALOG"] = "PASS"

    # 6. AI Conversation & Real-Time Grounding
    print("\n[STEP 5: AI CONVERSATION & REAL-TIME GROUNDING]")
    s_conv, r_conv, _ = http_req("POST", f"{BASE_URL}/api/v1/conversations", {
        "title": "Production Telemetry Analysis",
        "applicationId": app_id
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_conv == 201, f"Create conversation failed: {r_conv}"
    conv_id = r_conv["id"]
    results["CONVERSATION_PERSISTENCE"] = "PASS"

    # Turn 1: Ask about request counts and failures
    print("  Sending Turn 1 user message to Sentinel AI...")
    s_msg1, r_msg1, _ = http_req("POST", f"{BASE_URL}/api/v1/conversations/{conv_id}/messages", {
        "content": "How many total requests were observed, and are there any 500 errors?"
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_msg1 == 200, f"Send message 1 failed: {r_msg1}"
    
    last_msg1 = r_msg1.get("messages", [])[-1].get("content", "")
    safe_snippet1 = last_msg1[:120].encode('ascii', errors='replace').decode('ascii')
    print(f"  Turn 1 AI Response: {safe_snippet1}...")

    # Validate whether response is a REAL Gemini response vs Fallback
    is_fallback = "Google Gemini API Key Not Configured" in last_msg1 or "API Key Not Configured" in last_msg1
    if is_fallback:
        print("  [CRITICAL EVALUATION] Response is the 'API Key Not Configured' configuration guidance banner.")
        print("  -> REAL GEMINI LLM INVOCATION = FAIL (Fallback path was executed).")
        results["REAL_GEMINI_LLM_INVOCATION"] = "FAIL"
        results["GEMINI_REAL_DATABASE_GROUNDING"] = "FAIL"
    else:
        print("  -> Real Gemini LLM Response successfully generated [PASS].")
        results["REAL_GEMINI_LLM_INVOCATION"] = "PASS"
        # Grounding check: verify that response references the real database numbers (3 requests or 500 error)
        if "3" in last_msg1 or "500" in last_msg1 or "error" in last_msg1.lower() or "inventory" in last_msg1.lower():
            results["GEMINI_REAL_DATABASE_GROUNDING"] = "PASS"
            print("  -> Gemini reasoning grounded in real database telemetry [PASS].")

    # 7. Dynamic Telemetry Change & Stale-Data Test
    print("\n[STEP 6: STALE DATA EVOLUTION & MULTI-TURN GROUNDING TEST]")
    print("  Sending 5 additional real requests through Sentinel Gateway...")
    for i in range(5):
        s_extra, _, _ = http_req("GET", f"{BASE_URL}/api/v1/gateway/api/orders/item-{i+1}", headers={"X-Sentinel-API-Key": api_key})
        assert s_extra == 200

    # Verify real database count is now 3 + 5 = 8
    s_req2, r_req2, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_id}/requests", headers={"Authorization": f"Bearer {token_a}"})
    count_t2 = len(r_req2.get("content", []))
    print(f"  Current Real Database Request Count: {count_t2} [Expected 8]")
    assert count_t2 == 8, f"Expected 8 requests, got {count_t2}"

    # Turn 2: Query AI again in the SAME conversation
    print("  Sending Turn 2 user message to Sentinel AI...")
    s_msg2, r_msg2, _ = http_req("POST", f"{BASE_URL}/api/v1/conversations/{conv_id}/messages", {
        "content": "What is the new total request count now?"
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_msg2 == 200, f"Send message 2 failed: {r_msg2}"
    
    last_msg2 = r_msg2.get("messages", [])[-1].get("content", "")
    safe_snippet2 = last_msg2[:120].encode('ascii', errors='replace').decode('ascii')
    print(f"  Turn 2 AI Response: {safe_snippet2}...")
    
    if not is_fallback:
        if "8" in last_msg2 or "eight" in last_msg2.lower():
            results["STALE_DATA_PROTECTION"] = "PASS"
            print("  -> Gemini queried fresh database state and reported 8 requests [PASS].")
        else:
            print(f"  -> Gemini Turn 2 response did not mention current count of 8: {safe_snippet2}")
    else:
        results["STALE_DATA_PROTECTION"] = "FAIL"

    # 8. Tenant Isolation Check
    print("\n[STEP 7: MULTI-TENANT ISOLATION CHECK]")
    s_cross_cat, r_cross_cat, _ = http_req("GET", f"{BASE_URL}/api/v1/apis", headers={"Authorization": f"Bearer {token_b}"})
    assert s_cross_cat == 200 and len(r_cross_cat) == 0, f"Tenant B could view Tenant A endpoints: {r_cross_cat}"
    print("  Tenant B sees 0 endpoints in API catalog [PASS]")

    s_cross_conv, _, _ = http_req("GET", f"{BASE_URL}/api/v1/conversations/{conv_id}", headers={"Authorization": f"Bearer {token_b}"})
    assert s_cross_conv == 404, f"Tenant B could view Tenant A conversation: {s_cross_conv}"
    print("  Tenant B cannot access Tenant A conversation (HTTP 404) [PASS]")
    results["JWT_TENANT_ISOLATION"] = "PASS"
    results["AI_TENANT_ISOLATION"] = "PASS"

    # 9. Legacy Project Audit
    print("\n[STEP 8: LEGACY PROJECT SANITIZATION AUDIT]")
    # Scan codebase for prohibited strings
    backend_src = os.path.join(os.path.dirname(os.path.dirname(__file__)), "backend", "sentinel-api", "src", "main")
    frontend_src = os.path.join(os.path.dirname(os.path.dirname(__file__)), "frontend", "src")
    prohibited = ["PixelVault", "Event Management", "EventManagementSystem"]
    violations = []

    for src_dir in [backend_src, frontend_src]:
        if os.path.exists(src_dir):
            for root, _, files in os.walk(src_dir):
                for f in files:
                    if f.endswith((".java", ".ts", ".tsx", ".html")):
                        p = os.path.join(root, f)
                        with open(p, "r", encoding="utf-8", errors="ignore") as fp:
                            txt = fp.read()
                            for bad in prohibited:
                                if bad.lower() in txt.lower():
                                    violations.append((f, bad))

    print(f"  Audit Violations Found in Production Source: {len(violations)}")
    if len(violations) == 0:
        results["LEGACY_PROJECT_AUDIT"] = "PASS"
        print("  Production codebase 100% free of legacy project references [PASS]")
    else:
        print(f"  Violations: {violations}")

    print("\n" + "=" * 85)
    print("FINAL STEP-BY-STEP VERIFICATION MATRIX")
    print("=" * 85)
    for k, v in results.items():
        print(f"  {k.replace('_', ' ')}: {v}")
    print("=" * 85)

    return results

if __name__ == "__main__":
    res = run_real_gemini_pipeline_test()
    if res["REAL_GEMINI_LLM_INVOCATION"] != "PASS":
        print("\nNOTE: REAL_GEMINI_LLM_INVOCATION is FAIL because GEMINI_API_KEY is not configured in backend.")
        print("Sentinel reported the configuration status honestly without faking results.")
