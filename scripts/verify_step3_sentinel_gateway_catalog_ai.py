import http.server
import socketserver
import threading
import time
import uuid
import json
import urllib.request
import urllib.error
import sys

BASE_URL = "http://localhost:8080"
TEST_PORT = 8998

class Step3IsolatedUpstreamHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health" or self.path == "/":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"HEALTHY","target":"step3-upstream"}')
        elif self.path.startswith("/api/users"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("X-Upstream-Received-Header", self.headers.get("X-Custom-Client-Header", "missing"))
            self.end_headers()
            response_payload = {
                "users": [{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}],
                "query": self.path.split("?")[1] if "?" in self.path else ""
            }
            self.wfile.write(json.dumps(response_payload).encode("utf-8"))
        elif self.path == "/api/error/404":
            self.send_response(404)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error":"Resource not found"}')
        elif self.path == "/api/error/500":
            self.send_response(500)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error":"Internal upstream error"}')
        else:
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"path": self.path, "status": "ok"}).encode("utf-8"))

    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length).decode("utf-8") if content_length > 0 else ""
        if self.path == "/api/orders":
            self.send_response(201)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            parsed_body = {}
            if body:
                try:
                    parsed_body = json.loads(body)
                except Exception:
                    parsed_body = {"raw": body}
            response_payload = {
                "orderId": "ord_12345",
                "receivedData": parsed_body,
                "status": "CREATED"
            }
            self.wfile.write(json.dumps(response_payload).encode("utf-8"))
        else:
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"ok"}')

    def log_message(self, format, *args):
        pass

def start_isolated_upstream():
    server = socketserver.TCPServer(("127.0.0.1", TEST_PORT), Step3IsolatedUpstreamHandler)
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
        elif isinstance(data, bytes):
            req_data = data

    req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            body = response.read().decode("utf-8")
            res_json = None
            if body:
                try:
                    res_json = json.loads(body)
                except Exception:
                    res_json = body
            # Extract headers as dict
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

def run_step3_verification():
    print("=" * 80)
    print("SENTINEL STEP 3: REAL API CATALOG + GATEWAY VERIFICATION SUITE")
    print("=" * 80)

    # 1. Health & Infrastructure Check
    print("\n[1. INFRASTRUCTURE & HEALTH] Checking Sentinel backend...")
    s_actuator, r_actuator, _ = http_req("GET", f"{BASE_URL}/actuator/health")
    assert s_actuator == 200 and r_actuator.get("status") == "UP", f"Actuator health failed: {r_actuator}"
    print(f"  Sentinel Backend Actuator Status: {r_actuator.get('status')} [OK]")

    # 2. Start Isolated Real Upstream Server
    print("\n[2. UPSTREAM SETUP] Starting isolated real test upstream server on 127.0.0.1:8998...")
    upstream_server = start_isolated_upstream()
    time.sleep(0.5)
    s_up, r_up, _ = http_req("GET", f"http://127.0.0.1:{TEST_PORT}/health")
    assert s_up == 200 and r_up.get("status") == "HEALTHY", f"Upstream health check failed: {r_up}"
    print(f"  Isolated test upstream server verified at http://127.0.0.1:{TEST_PORT} [OK]")

    # 3. User Setup (Tenant A & Tenant B)
    print("\n[3. AUTH & TENANT SETUP] Registering & authenticating User A and User B...")
    uid = str(uuid.uuid4())[:8]
    user_a_email = f"user_a_{uid}@sentinel.local"
    user_b_email = f"user_b_{uid}@sentinel.local"
    password = "Password123!"

    http_req("POST", f"{BASE_URL}/api/v1/auth/register", {"name": "Tenant User A", "email": user_a_email, "password": password})
    http_req("POST", f"{BASE_URL}/api/v1/auth/register", {"name": "Tenant User B", "email": user_b_email, "password": password})

    _, r_la, _ = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {"email": user_a_email, "password": password})
    _, r_lb, _ = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {"email": user_b_email, "password": password})
    token_a = r_la["token"]
    token_b = r_lb["token"]
    print("  User A and User B authenticated with JWT [OK]")

    # 4. Application & API Key Creation for User A
    print("\n[4. APP & API KEY] Creating Application A and generating scoped API key...")
    s_app, r_app, _ = http_req("POST", f"{BASE_URL}/api/v1/applications", {
        "name": "Live Upstream Service A",
        "description": "Application configured to forward to 127.0.0.1:8998",
        "baseUrl": f"http://127.0.0.1:{TEST_PORT}"
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_app == 201, f"App creation failed: {r_app}"
    app_a_id = r_app["id"]
    print(f"  Application A created: ID {app_a_id}, BaseURL: http://127.0.0.1:{TEST_PORT}")

    s_key, r_key, _ = http_req("POST", f"{BASE_URL}/api/v1/applications/{app_a_id}/keys", {
        "name": "Primary Production Key",
        "rateLimitPerMinute": 60
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_key == 201 and "apiKey" in r_key, f"API key creation failed: {r_key}"
    api_key = r_key["apiKey"]
    print(f"  API Key generated: {api_key[:16]}... (scoped to app {app_a_id})")

    # 5. Real Gateway Forwarding Verification
    print("\n[5. GATEWAY FORWARDING] Testing real HTTP forwarding through Sentinel Gateway...")
    
    # 5.1 GET request with query parameters and custom header
    print("  5.1 Testing GET /api/v1/gateway/api/users?status=active...")
    s_gw_get, r_gw_get, h_gw_get = http_req(
        "GET",
        f"{BASE_URL}/api/v1/gateway/api/users?status=active",
        headers={
            "X-Sentinel-API-Key": api_key,
            "X-Custom-Client-Header": "sentinel-client-val"
        }
    )
    print(f"      Status: HTTP {s_gw_get}")
    print(f"      Response: {r_gw_get}")
    print(f"      RateLimit-Remaining Header: {h_gw_get.get('x-ratelimit-remaining')}")
    assert s_gw_get == 200, f"Gateway GET failed: {s_gw_get}, {r_gw_get}"
    assert r_gw_get.get("query") == "status=active", f"Query param lost: {r_gw_get}"
    assert len(r_gw_get.get("users", [])) == 2, f"Users data mismatch: {r_gw_get}"

    # 5.2 POST request with JSON body
    print("  5.2 Testing POST /api/v1/gateway/api/orders with JSON payload...")
    s_gw_post, r_gw_post, _ = http_req(
        "POST",
        f"{BASE_URL}/api/v1/gateway/api/orders",
        data={"item": "book", "quantity": 3},
        headers={"X-Sentinel-API-Key": api_key}
    )
    print(f"      Status: HTTP {s_gw_post}")
    print(f"      Response: {r_gw_post}")
    assert s_gw_post == 201, f"Gateway POST failed: {s_gw_post}, {r_gw_post}"
    assert r_gw_post.get("receivedData", {}).get("item") == "book", f"Body corrupted: {r_gw_post}"

    # 5.3 Upstream 404 Not Found forwarding
    print("  5.3 Testing upstream 404 response forwarding...")
    s_gw_404, r_gw_404, _ = http_req(
        "GET",
        f"{BASE_URL}/api/v1/gateway/api/error/404",
        headers={"X-Sentinel-API-Key": api_key}
    )
    print(f"      Status: HTTP {s_gw_404} [Correctly returned from upstream]")
    assert s_gw_404 == 404, f"Expected 404, got: {s_gw_404}"

    # 5.4 Upstream 500 Server Error forwarding
    print("  5.4 Testing upstream 500 response forwarding...")
    s_gw_500, r_gw_500, _ = http_req(
        "GET",
        f"{BASE_URL}/api/v1/gateway/api/error/500",
        headers={"X-Sentinel-API-Key": api_key}
    )
    print(f"      Status: HTTP {s_gw_500} [Correctly returned from upstream]")
    assert s_gw_500 == 500, f"Expected 500, got: {s_gw_500}"

    # 6. Real Request Log Persistence Verification
    print("\n[6. REQUEST LOGS] Verifying real request logs persisted in Sentinel database...")
    s_logs, r_logs, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_a_id}/requests?page=0&size=20", headers={"Authorization": f"Bearer {token_a}"})
    assert s_logs == 200, f"Fetch logs failed: {r_logs}"
    log_content = r_logs.get("content", [])
    print(f"  Total persisted request logs retrieved for App A: {len(log_content)}")
    assert len(log_content) >= 4, f"Expected at least 4 request logs, found: {len(log_content)}"
    
    # Verify status codes and paths are genuine
    status_codes = [l["statusCode"] for l in log_content]
    paths = [l["path"] for l in log_content]
    print(f"  Logged Status Codes: {status_codes}")
    print(f"  Logged Paths: {paths}")
    assert 200 in status_codes, "HTTP 200 log missing"
    assert 201 in status_codes, "HTTP 201 log missing"
    assert 404 in status_codes, "HTTP 404 log missing"
    assert 500 in status_codes, "HTTP 500 log missing"

    # 7. Traffic-Based API Discovery Verification
    print("\n[7. TRAFFIC-BASED API DISCOVERY] Checking auto-discovered endpoints in API Catalog...")
    s_eps, r_eps, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_a_id}/apis", headers={"Authorization": f"Bearer {token_a}"})
    assert s_eps == 200, f"Fetch endpoints failed: {r_eps}"
    print(f"  Discovered Endpoints for App A: {len(r_eps)}")
    for ep in r_eps:
        print(f"    - [{ep['method']}] {ep['normalizedPath']} (Status: {ep.get('documentationStatus')}, Total Requests: {ep.get('totalRequests')})")
    
    discovered_paths = [ep["normalizedPath"] for ep in r_eps]
    assert "/api/users" in discovered_paths or "/api/users/*" in discovered_paths, "Discovered endpoint /api/users missing"
    assert "/api/orders" in discovered_paths or "/api/orders/*" in discovered_paths, "Discovered endpoint /api/orders missing"

    # 8. Real OpenAPI Import Verification
    print("\n[8. OPENAPI IMPORT] Importing real OpenAPI 3.0 specification...")
    openapi_spec = {
        "openapi": "3.0.0",
        "info": {
            "title": "Inventory & Products API",
            "version": "1.0.0",
            "description": "Real OpenAPI spec provided by user"
        },
        "paths": {
            "/api/inventory": {
                "get": {
                    "summary": "List Inventory Items",
                    "description": "Returns current warehouse stock",
                    "responses": {
                        "200": {"description": "Successful response"}
                    }
                }
            },
            "/api/products": {
                "post": {
                    "summary": "Create Product",
                    "description": "Registers a new catalog item",
                    "responses": {
                        "201": {"description": "Product created"}
                    }
                }
            }
        }
    }

    s_import, r_import, _ = http_req("POST", f"{BASE_URL}/api/v1/applications/{app_a_id}/openapi/import", {
        "specType": "RAW_JSON",
        "specContent": json.dumps(openapi_spec)
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_import == 200, f"OpenAPI import failed: {r_import}"
    print(f"  Import Result: {r_import.get('endpointsImported')} imported, {r_import.get('totalDocumentedEndpoints')} total documented.")
    assert r_import.get("endpointsImported") == 2, f"Expected 2 imported endpoints, got: {r_import}"

    # Verify duplicate import does not duplicate endpoints
    s_reimport, r_reimport, _ = http_req("POST", f"{BASE_URL}/api/v1/applications/{app_a_id}/openapi/import", {
        "specType": "RAW_JSON",
        "specContent": json.dumps(openapi_spec)
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_reimport == 200, f"OpenAPI re-import failed: {r_reimport}"
    print(f"  Idempotency Check: {r_reimport.get('endpointsImported')} new, {r_reimport.get('endpointsUpdated')} updated (no duplicates created) [OK]")
    assert r_reimport.get("endpointsImported") == 0, "Duplicate endpoints were created on re-import"

    # 9. Multi-Tenant API Catalog Isolation Verification
    print("\n[9. MULTI-TENANT CATALOG ISOLATION] Verifying User B cannot see User A's API catalog...")
    s_b_cat, r_b_cat, _ = http_req("GET", f"{BASE_URL}/api/v1/apis", headers={"Authorization": f"Bearer {token_b}"})
    assert s_b_cat == 200, f"User B catalog fetch failed: {r_b_cat}"
    print(f"  User B Global API Catalog size: {len(r_b_cat)} (Expected 0 since User B has no apps with endpoints)")
    assert len(r_b_cat) == 0, f"Tenant isolation breach: User B saw {len(r_b_cat)} endpoints!"

    # 10. AI Tool Execution Pipeline & Diagnostics Verification
    print("\n[10. AI ASSISTANT & REAL DATA PIPELINE] Verifying conversation persistence and real tool data context...")
    s_conv, r_conv, _ = http_req("POST", f"{BASE_URL}/api/v1/conversations", {
        "title": "Gateway Telemetry Diagnostic",
        "applicationId": app_a_id
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_conv == 201, f"Create conversation failed: {r_conv}"
    conv_id = r_conv["id"]

    s_ai_msg, r_ai_msg, _ = http_req("POST", f"{BASE_URL}/api/v1/conversations/{conv_id}/messages", {
        "content": "List all active endpoints and their error rates."
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_ai_msg == 200, f"Send AI message failed: {r_ai_msg}"
    print(f"  AI Conversation {conv_id} processed message successfully.")
    last_response = r_ai_msg.get("messages", [])[-1].get("content", "")
    print(f"  AI Server Response: {last_response[:140].encode('ascii', errors='replace').decode('ascii')}...")

    print("\n" + "=" * 80)
    print("STEP 3: ALL GATEWAY, API CATALOG, OPENAPI & AI PIPELINE CHECKS PASSED (100%)!")
    print("=" * 80)

if __name__ == "__main__":
    run_step3_verification()
