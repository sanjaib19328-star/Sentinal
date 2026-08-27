import json
import time
import urllib.request
import urllib.error
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler

BASE_URL = "http://localhost:8080"

# Mock FastAPI application serving /openapi.json with 8 endpoints and /health
SAMPLE_OPENAPI_SPEC = {
    "openapi": "3.1.0",
    "info": {
        "title": "PixelVault-Clean API",
        "version": "1.0.0",
        "description": "Image processing and analysis service"
    },
    "paths": {
        "/health": {
            "get": {"summary": "Service Health Check", "responses": {"200": {"description": "OK"}}}
        },
        "/api/v1/images/upload": {
            "post": {"summary": "Upload Raw Image", "responses": {"201": {"description": "Uploaded"}}}
        },
        "/api/v1/images/analyze": {
            "post": {"summary": "AI Image Analysis", "responses": {"200": {"description": "Analysis result"}}}
        },
        "/api/v1/images/clean": {
            "post": {"summary": "Noise Reduction and Clean", "responses": {"200": {"description": "Cleaned image"}}}
        },
        "/api/v1/images/{id}": {
            "get": {"summary": "Retrieve Image by ID", "responses": {"200": {"description": "Image metadata"}}},
            "delete": {"summary": "Delete Image", "responses": {"204": {"description": "Deleted"}}}
        },
        "/api/v1/presets": {
            "get": {"summary": "List Filter Presets", "responses": {"200": {"description": "Presets list"}}}
        },
        "/api/v1/metrics": {
            "get": {"summary": "System Performance Metrics", "responses": {"200": {"description": "Metrics"}}}
        }
    }
}

class MockFastApiHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"HEALTHY","service":"pixelvault-clean"}')
        elif self.path == "/openapi.json":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(SAMPLE_OPENAPI_SPEC).encode("utf-8"))
        elif self.path.startswith("/api/v1/images"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"imageId":"img_101","status":"processed"}')
        else:
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"message":"ok"}')

    def do_POST(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b'{"status":"success"}')

    def log_message(self, format, *args):
        pass # Suppress noisy server logs

def start_mock_server(port=5899):
    server = HTTPServer(("127.0.0.1", port), MockFastApiHandler)
    t = threading.Thread(target=server.serve_forever, daemon=True)
    t.start()
    return server

def http_req(method, url, data=None, headers=None):
    if headers is None:
        headers = {}
    if data is not None:
        if isinstance(data, dict):
            payload = json.dumps(data).encode("utf-8")
            headers["Content-Type"] = "application/json"
        else:
            payload = data.encode("utf-8")
    else:
        payload = None

    req = urllib.request.Request(url, data=payload, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = resp.read().decode("utf-8")
            try:
                return resp.status, json.loads(body), resp.headers
            except:
                return resp.status, body, resp.headers
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body), e.headers
        except:
            return e.code, body, e.headers
    except Exception as e:
        return 500, {"error": str(e)}, {}

def main():
    print("=" * 70)
    print("SENTINEL PHASE 1 VERIFICATION: SIMPLE CONNECTION & AUTO-DISCOVERY")
    print("=" * 70)

    # 1. Start Mock Upstream App (FastAPI emulation with 8 endpoints)
    mock_port = 5899
    start_mock_server(mock_port)
    upstream_url = f"http://127.0.0.1:{mock_port}"
    print(f"\n[STEP 1: UPSTREAM APPLICATION READY] at {upstream_url}")

    # 2. Register & Login Tenants
    ts = int(time.time())
    email_a = f"tenant_a_{ts}@sentinel.io"
    email_b = f"tenant_b_{ts}@sentinel.io"
    password = "Password123!"

    s_reg_a, r_reg_a, _ = http_req("POST", f"{BASE_URL}/api/v1/auth/register", {"email": email_a, "password": password, "name": "Tenant A"})
    assert s_reg_a == 201, f"Reg A failed: {r_reg_a}"

    s_log_a, r_log_a, _ = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {"email": email_a, "password": password})
    assert s_log_a == 200, f"Login A failed: {r_log_a}"
    token_a = r_log_a["token"]

    s_reg_b, r_reg_b, _ = http_req("POST", f"{BASE_URL}/api/v1/auth/register", {"email": email_b, "password": password, "name": "Tenant B"})
    assert s_reg_b == 201, f"Reg B failed: {r_reg_b}"

    s_log_b, r_log_b, _ = http_req("POST", f"{BASE_URL}/api/v1/auth/login", {"email": email_b, "password": password})
    assert s_log_b == 200, f"Login B failed: {r_log_b}"
    token_b = r_log_b["token"]
    print("  Tenant A and Tenant B registered and authenticated with JWT [PASS]")

    # 3. Test Simplified Connect & Auto-Discover
    print("\n[STEP 2: CONNECT APPLICATION & AUTO-DISCOVER APIS]")
    connect_payload = {
        "applicationName": "PixelVault-Clean",
        "sentinelUrl": upstream_url,
        "apiKey": "" # Blank: Sentinel auto-generates ONE primary key
    }
    s_conn, r_conn, _ = http_req("POST", f"{BASE_URL}/api/v1/applications/connect-and-discover", connect_payload, headers={"Authorization": f"Bearer {token_a}"})
    assert s_conn == 200, f"Connect & Discover failed: {r_conn}"

    app_id = r_conn["applicationId"]
    app_name = r_conn["applicationName"]
    gateway_url = r_conn["sentinelGatewayUrl"]
    api_key = r_conn["apiKey"]
    health_status = r_conn["healthStatus"]
    apis_count = r_conn["apisDiscoveredCount"]
    discovered_apis = r_conn.get("discoveredApis", [])

    print(f"  Connected Application: {app_name} (ID: {app_id})")
    print(f"  Backend Health:        {health_status} (Healthy: {r_conn['backendHealthy']})")
    print(f"  Sentinel Gateway URL:  {gateway_url}")
    print(f"  Sentinel API Key:      {api_key[:16]}... (length {len(api_key)})")
    print(f"  APIs Discovered:       {apis_count} endpoints")

    assert app_name == "PixelVault-Clean", "App name mismatch"
    assert api_key.startswith("sk_sentinel_"), "Invalid Sentinel API Key generated"
    assert apis_count >= 8, f"Expected at least 8 discovered APIs from OpenAPI spec, got {apis_count}"
    print("  Connection and Auto-Discovery: PASS")

    # 4. Verify API Catalog Population
    print("\n[STEP 3: VERIFY API CATALOG PERSISTENCE]")
    s_cat, r_cat, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_id}/apis", headers={"Authorization": f"Bearer {token_a}"})
    assert s_cat == 200, f"Get catalog failed: {r_cat}"
    assert len(r_cat) == apis_count, f"Catalog length {len(r_cat)} does not match discovery count {apis_count}"

    endpoints_summary = [f"{ep['method']} {ep['normalizedPath']}" for ep in r_cat]
    print("  Catalog Endpoints in Sentinel Database:")
    for ep in endpoints_summary[:6]:
        print(f"    - {ep}")
    if len(endpoints_summary) > 6:
        print(f"    - ... and {len(endpoints_summary)-6} more")

    assert any("/api/v1/images/upload" in ep for ep in endpoints_summary), "Missing /api/v1/images/upload in catalog"
    assert any("/api/v1/images/analyze" in ep for ep in endpoints_summary), "Missing /api/v1/images/analyze in catalog"
    print("  API Catalog Population: PASS")

    # 5. Route Gateway Traffic using the ONE Connection API Key
    print("\n[STEP 4: ROUTE GATEWAY TRAFFIC WITH SENTINEL API KEY]")
    s_gw, r_gw, _ = http_req("GET", f"{BASE_URL}/api/v1/gateway/api/v1/images/item-99", headers={"X-Sentinel-API-Key": api_key})
    assert s_gw == 200, f"Gateway forwarding failed: {s_gw} {r_gw}"
    print(f"  Gateway forwarded GET /api/v1/images/item-99 -> HTTP 200 [PASS]")

    # 6. Multi-Tenant Isolation Verification
    print("\n[STEP 5: MULTI-TENANT ISOLATION]")
    s_cross_app, r_cross_app, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_id}", headers={"Authorization": f"Bearer {token_b}"})
    assert s_cross_app == 404, f"Tenant B could access Tenant A's application: {s_cross_app}"
    print("  Tenant B cannot access Tenant A application (HTTP 404) [PASS]")

    s_cross_cat, r_cross_cat, _ = http_req("GET", f"{BASE_URL}/api/v1/applications/{app_id}/apis", headers={"Authorization": f"Bearer {token_b}"})
    assert s_cross_cat == 404, f"Tenant B could access Tenant A catalog: {s_cross_cat}"
    print("  Tenant B cannot access Tenant A API catalog (HTTP 404) [PASS]")

    # 7. Local vs Hosted Uniformity Verification
    print("\n[STEP 6: LOCAL & HOSTED WORKFLOW UNIFORMITY]")
    local_payload = {
        "applicationName": "My Local API",
        "sentinelUrl": "http://127.0.0.1:5899",
        "apiKey": ""
    }
    s_loc, r_loc, _ = http_req("POST", f"{BASE_URL}/api/v1/applications/connect-and-discover", local_payload, headers={"Authorization": f"Bearer {token_a}"})
    assert s_loc == 200, f"Local connection failed: {r_loc}"
    print(f"  Local Application '{r_loc['applicationName']}' connected with {r_loc['apisDiscoveredCount']} APIs [PASS]")

    print("\n" + "=" * 70)
    print("ALL PHASE 1 SUCCESS CRITERIA VERIFIED (100% PASS)")
    print("=" * 70)

if __name__ == "__main__":
    main()
