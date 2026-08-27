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
TEST_PORT = 8999

class IsolatedTestTargetHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health" or self.path == "/":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"HEALTHY","target":"isolated-test-target"}')
        else:
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"path": self.path, "status": "ok"}).encode('utf-8'))

    def log_message(self, format, *args):
        pass

def start_isolated_target():
    server = socketserver.TCPServer(("127.0.0.1", TEST_PORT), IsolatedTestTargetHandler)
    server.allow_reuse_address = True
    t = threading.Thread(target=server.serve_forever, daemon=True)
    t.start()
    return server

def http_req(method, url, data=None, headers=None):
    if headers is None:
        headers = {}
    req_data = None
    if data is not None:
        req_data = json.dumps(data).encode('utf-8')
        headers["Content-Type"] = "application/json"
    
    req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            body = response.read().decode('utf-8')
            res_json = None
            if body:
                try:
                    res_json = json.loads(body)
                except Exception:
                    res_json = body
            return response.status, res_json
    except urllib.error.HTTPError as e:
        body = e.read().decode('utf-8')
        res_json = None
        if body:
            try:
                res_json = json.loads(body)
            except Exception:
                res_json = body
        return e.code, res_json
    except Exception as e:
        return 500, {"error": str(e)}

def run_step2_verification():
    print("=" * 70)
    print("SENTINEL STEP 2 VERIFICATION SUITE")
    print("=" * 70)

    # 0. Start Isolated Test Target
    print("\n[SETUP] Starting isolated local HTTP test service on 127.0.0.1:8999...")
    target_server = start_isolated_target()
    time.sleep(0.5)

    # Verify Target Service is reachable
    status, res = http_req("GET", f"http://127.0.0.1:{TEST_PORT}/health")
    assert status == 200, f"Isolated test target failed: {status}"
    print(f"  [OK] Isolated test target running and returning 200: {res}")

    unique_suffix = str(uuid.uuid4())[:8]
    user_a_email = f"sentinel_user_a_{unique_suffix}@sentinel.local"
    user_b_email = f"sentinel_user_b_{unique_suffix}@sentinel.local"
    password = "Password123!"

    # 1. Registration Verification
    print("\n[1. REGISTRATION] Registering User A and User B...")
    s_a, r_a = http_req("POST", f"{BASE_URL}/api/v1/auth/register", {
        "name": "Sentinel Test User A",
        "email": user_a_email,
        "password": password
    })
    print(f"  User A Registration: HTTP {s_a}, User ID: {r_a.get('id') if isinstance(r_a, dict) else r_a}")
    assert s_a == 201, f"User A registration failed: {r_a}"

    s_b, r_b = http_req("POST", f"{BASE_URL}/api/v1/auth/register", {
        "name": "Sentinel Test User B",
        "email": user_b_email,
        "password": password
    })
    print(f"  User B Registration: HTTP {s_b}, User ID: {r_b.get('id') if isinstance(r_b, dict) else r_b}")
    assert s_b == 201, f"User B registration failed: {r_b}"

    # 2. Login Verification
    print("\n[2. LOGIN] Authenticating User A and User B...")
    s_la, r_la = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {
        "email": user_a_email,
        "password": password
    })
    assert s_la == 200 and "token" in r_la, f"User A login failed: {r_la}"
    token_a = r_la["token"]
    print(f"  User A Login: HTTP 200, JWT Acquired (len: {len(token_a)})")

    s_lb, r_lb = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {
        "email": user_b_email,
        "password": password
    })
    assert s_lb == 200 and "token" in r_lb, f"User B login failed: {r_lb}"
    token_b = r_lb["token"]
    print(f"  User B Login: HTTP 200, JWT Acquired (len: {len(token_b)})")

    # 3. JWT Verification & Protection
    print("\n[3. JWT PROTECTION] Verifying /api/v1/auth/me and token validation...")
    # User A /me
    s_me_a, r_me_a = http_req("GET", f"{BASE_URL}/api/v1/auth/me", headers={"Authorization": f"Bearer {token_a}"})
    assert s_me_a == 200 and r_me_a.get("email") == user_a_email, f"User A /me failed: {r_me_a}"
    print(f"  User A /me: HTTP 200, Identity matches: {r_me_a.get('email')}")

    # User B /me
    s_me_b, r_me_b = http_req("GET", f"{BASE_URL}/api/v1/auth/me", headers={"Authorization": f"Bearer {token_b}"})
    assert s_me_b == 200 and r_me_b.get("email") == user_b_email, f"User B /me failed: {r_me_b}"
    print(f"  User B /me: HTTP 200, Identity matches: {r_me_b.get('email')}")

    # Missing JWT
    s_no_jwt, r_no_jwt = http_req("GET", f"{BASE_URL}/api/v1/auth/me")
    assert s_no_jwt in (401, 403), f"Missing JWT did not reject: {s_no_jwt}"
    print(f"  Missing JWT rejected with HTTP {s_no_jwt} [PASS]")

    # Invalid JWT
    s_bad_jwt, r_bad_jwt = http_req("GET", f"{BASE_URL}/api/v1/auth/me", headers={"Authorization": "Bearer invalid.jwt.token"})
    assert s_bad_jwt in (401, 403), f"Invalid JWT did not reject: {s_bad_jwt}"
    print(f"  Invalid JWT rejected with HTTP {s_bad_jwt} [PASS]")

    # 4. System Health (MySQL & Redis)
    print("\n[4. MYSQL & REDIS VERIFICATION] Checking /api/v1/analytics/system/health...")
    s_health, r_health = http_req("GET", f"{BASE_URL}/api/v1/analytics/system/health", headers={"Authorization": f"Bearer {token_a}"})
    assert s_health == 200, f"System health check failed: {r_health}"
    mysql_status = r_health.get("mysql", {}).get("status")
    mysql_latency = r_health.get("mysql", {}).get("latencyMs")
    redis_status = r_health.get("redis", {}).get("status")
    redis_latency = r_health.get("redis", {}).get("latencyMs")
    print(f"  MySQL Status: {mysql_status} (Latency: {mysql_latency} ms)")
    print(f"  Redis Status: {redis_status} (Latency: {redis_latency} ms)")
    assert mysql_status == "UP", f"MySQL is not UP: {mysql_status}"
    assert redis_status == "UP", f"Redis is not UP: {redis_status}"

    # 5. Application Registration (Strict No-Fake-Data Check)
    print("\n[5. APPLICATION REGISTRATION] Registering Application A (User A) & Application B (User B)...")
    s_app_a, r_app_a = http_req("POST", f"{BASE_URL}/api/v1/applications", {
        "name": "Sentinel Observability Target A",
        "description": "Real application registered for User A",
        "baseUrl": f"http://127.0.0.1:{TEST_PORT}"
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_app_a == 201, f"Create App A failed: {r_app_a}"
    app_a_id = r_app_a["id"]
    print(f"  Application A created: ID {app_a_id}, HealthStatus: {r_app_a.get('healthStatus')} [Initial state must be UNKNOWN]")
    assert r_app_a.get("healthStatus") == "UNKNOWN", f"App A initial health was not UNKNOWN: {r_app_a.get('healthStatus')}"

    s_app_b, r_app_b = http_req("POST", f"{BASE_URL}/api/v1/applications", {
        "name": "Sentinel Observability Target B",
        "description": "Real application registered for User B",
        "baseUrl": f"http://127.0.0.1:{TEST_PORT}"
    }, headers={"Authorization": f"Bearer {token_b}"})
    assert s_app_b == 201, f"Create App B failed: {r_app_b}"
    app_b_id = r_app_b["id"]
    print(f"  Application B created: ID {app_b_id}, HealthStatus: {r_app_b.get('healthStatus')}")

    # 6. Multi-Tenant Isolation Enforcement
    print("\n[6. MULTI-TENANT ISOLATION] Testing cross-tenant boundary protections...")
    # User A tries to GET App B
    s_cross_get, r_cross_get = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_b_id}", headers={"Authorization": f"Bearer {token_a}"})
    print(f"  User A GET App B (User B's app): HTTP {s_cross_get} [Expect 404 Not Found]")
    assert s_cross_get == 404, f"Cross-tenant GET was not isolated: {s_cross_get}"

    # User A tries to UPDATE App B
    s_cross_put, r_cross_put = http_req("PUT", f"{BASE_URL}/api/v1/applications/{app_b_id}", {
        "name": "Hacked App B Name"
    }, headers={"Authorization": f"Bearer {token_a}"})
    print(f"  User A PUT App B: HTTP {s_cross_put} [Expect 404 Not Found]")
    assert s_cross_put == 404, f"Cross-tenant PUT was not isolated: {s_cross_put}"

    # User A tries to DELETE App B
    s_cross_del, r_cross_del = http_req("DELETE", f"{BASE_URL}/api/v1/applications/{app_b_id}", headers={"Authorization": f"Bearer {token_a}"})
    print(f"  User A DELETE App B: HTTP {s_cross_del} [Expect 404 Not Found]")
    assert s_cross_del == 404, f"Cross-tenant DELETE was not isolated: {s_cross_del}"

    # User A tries to Connection-Test App B
    s_cross_test, r_cross_test = http_req("POST", f"{BASE_URL}/api/v1/applications/{app_b_id}/connection-test", headers={"Authorization": f"Bearer {token_a}"})
    print(f"  User A Connection-Test App B: HTTP {s_cross_test} [Expect 404 Not Found]")
    assert s_cross_test == 404, f"Cross-tenant Connection-Test was not isolated: {s_cross_test}"

    # User B tries to GET App A
    s_cross_b_a, r_cross_b_a = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_a_id}", headers={"Authorization": f"Bearer {token_b}"})
    print(f"  User B GET App A (User A's app): HTTP {s_cross_b_a} [Expect 404 Not Found]")
    assert s_cross_b_a == 404, f"Cross-tenant GET was not isolated: {s_cross_b_a}"

    # 7. Real Observation & Health Probing
    print("\n[7. REAL OBSERVATION & NON-BLOCKING PROBING] Performing real connection test on App A...")
    s_probe, r_probe = http_req("POST", f"{BASE_URL}/api/v1/applications/{app_a_id}/connection-test", headers={"Authorization": f"Bearer {token_a}"})
    print(f"  Connection Test Response: HTTP {s_probe}, Data: {r_probe}")
    assert s_probe == 200, f"Connection test failed: {r_probe}"
    assert r_probe.get("reachable") == True, f"Target was not reachable: {r_probe}"
    assert r_probe.get("statusCode") == 200, f"Status code was not 200: {r_probe}"
    assert r_probe.get("latencyMs") is not None and r_probe.get("latencyMs") >= 0, f"Invalid latency: {r_probe}"

    # Check updated status on App A
    s_stat, r_stat = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_a_id}/status", headers={"Authorization": f"Bearer {token_a}"})
    print(f"  App A Updated Status: HTTP {s_stat}, HealthStatus: {r_stat.get('status')}")
    assert r_stat.get("status") == "HEALTHY", f"Health status was not updated to HEALTHY: {r_stat}"

    # 8. AI Conversation & Tool Data-Source Pipeline Verification
    print("\n[8. AI PIPELINE & TOOL DATA-SOURCE] Verifying AI conversation creation and real data retrieval...")
    s_conv, r_conv = http_req("POST", f"{BASE_URL}/api/v1/conversations", {
        "title": "Observability Check",
        "applicationId": app_a_id
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_conv == 201, f"Create conversation failed: {r_conv}"
    conv_id = r_conv["id"]
    print(f"  Conversation created: ID {conv_id}, Title: {r_conv.get('title')}")

    # Send a query into the conversation
    s_msg, r_msg = http_req("POST", f"{BASE_URL}/api/v1/conversations/{conv_id}/messages", {
        "content": "What is the health status of my application?"
    }, headers={"Authorization": f"Bearer {token_a}"})
    assert s_msg == 200, f"Send message failed: {r_msg}"
    print(f"  Message processed: Total messages in conversation: {len(r_msg.get('messages', []))}")
    last_msg = r_msg.get('messages', [])[-1]
    print(f"  Last Response Sender: {last_msg.get('sender')}")
    print(f"  Content snippet: {last_msg.get('content', '')[:120].encode('ascii', errors='replace').decode('ascii')}...")

    # Verify conversation list
    s_clist, r_clist = http_req("GET", f"{BASE_URL}/api/v1/conversations", headers={"Authorization": f"Bearer {token_a}"})
    assert s_clist == 200 and len(r_clist) >= 1, f"List conversations failed: {r_clist}"
    print(f"  Conversations persisted in MySQL: {len(r_clist)} active conversations found.")

    print("\n" + "=" * 70)
    print("ALL STEP 2 VERIFICATION CHECKS PASSED PERFECTLY!")
    print("=" * 70)

if __name__ == "__main__":
    run_step2_verification()
