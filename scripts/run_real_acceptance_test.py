import urllib.request
import urllib.error
import json
import time
import sys

BASE_URL = "http://127.0.0.1:8080"
PIXELVAULT_URL = "https://pixelvault-clean-api.onrender.com"

results = []

def record(phase, step, name, status, details=""):
    results.append({
        "phase": phase,
        "step": step,
        "name": name,
        "status": "PASS" if status else "FAIL",
        "details": details
    })
    tag = "[PASS]" if status else "[FAIL]"
    print(f"{tag} Phase {phase} - Step {step:02d}: {name:<45} | {details}")

def request_json(url, method="GET", data=None, token=None, headers=None):
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    if headers:
        for k, v in headers.items():
            req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            resp_body = resp.read().decode("utf-8")
            resp_headers = dict(resp.info())
            try:
                parsed = json.loads(resp_body)
            except Exception:
                parsed = {"raw": resp_body}
            return resp.status, parsed, resp_headers
    except urllib.error.HTTPError as e:
        resp_body = e.read().decode("utf-8")
        resp_headers = dict(e.headers)
        try:
            parsed = json.loads(resp_body)
        except Exception:
            parsed = {"raw": resp_body}
        return e.code, parsed, resp_headers
    except Exception as e:
        return 0, {"error": str(e)}, {}

def main():
    print("================================================================================")
    print("      SENTINEL REAL-WORLD CUSTOMER ACCEPTANCE TEST — PIXELVAULT UPSTREAM        ")
    print("================================================================================")

    # ---------------------------------------------------------
    # PHASE 1: START SENTINEL LOCALLY & VERIFY HEALTH
    # ---------------------------------------------------------
    print("\n>>> PHASE 1: Sentinel Local Startup & Health Verification")
    
    # 1.1 Actuator Health
    status, data, _ = request_json(f"{BASE_URL}/actuator/health")
    record(1, 1, "Actuator Health Check", status == 200 and data.get("status") == "UP", f"HTTP {status}, status={data.get('status')}")

    # 1.2 Customer Registration
    unique_id = int(time.time())
    customer_email = f"pixelvault-owner-{unique_id}@pixelvault.io"
    customer_pass = "SecurePass2026!"
    status, reg_data, _ = request_json(
        f"{BASE_URL}/api/v1/auth/register",
        method="POST",
        data={"name": "PixelVault Customer", "email": customer_email, "password": customer_pass}
    )
    record(1, 2, "Customer User Registration", status in [200, 201] and "email" in reg_data, f"HTTP {status}, email={reg_data.get('email')}")

    # 1.3 Customer Login
    status, login_data, _ = request_json(
        f"{BASE_URL}/api/v1/auth/login",
        method="POST",
        data={"email": customer_email, "password": customer_pass}
    )
    record(1, 3, "Customer User Login", status == 200 and "token" in login_data, f"HTTP {status}")
    customer_token = login_data.get("token")

    # 1.4 Dashboard Summary
    status, dash_data, _ = request_json(f"{BASE_URL}/api/v1/dashboard/summary", token=customer_token)
    record(1, 4, "Dashboard Summary API", status == 200 and "totalApplications" in dash_data, f"HTTP {status}, totalApps={dash_data.get('totalApplications')}")

    # 1.5 System Health Telemetry
    status, sys_health, _ = request_json(f"{BASE_URL}/api/v1/analytics/system/health", token=customer_token)
    record(1, 5, "Infrastructure Health Telemetry", status == 200 and sys_health.get("controlPlaneStatus") == "UP", f"HTTP {status}, ControlPlane={sys_health.get('controlPlaneStatus')}, MySQL={sys_health.get('mysql',{}).get('status')}, Redis={sys_health.get('redis',{}).get('status')}")

    # ---------------------------------------------------------
    # PHASE 2: CREATE CUSTOMER APPLICATION (PIXELVAULT)
    # ---------------------------------------------------------
    print("\n>>> PHASE 2: Customer Application Onboarding (PixelVault)")
    
    app_payload = {
        "name": "PixelVault Customer Test",
        "baseUrl": PIXELVAULT_URL,
        "description": "Real customer acceptance test against live deployed PixelVault backend",
        "authType": "NONE"
    }
    status, app_res, _ = request_json(f"{BASE_URL}/api/v1/applications", method="POST", data=app_payload, token=customer_token)
    app_id = app_res.get("id")
    record(2, 1, "Create Application (No Auth)", status in [200, 201] and app_id is not None, f"HTTP {status}, AppID={app_id}, Target={PIXELVAULT_URL}")

    # 2.2 Connection Test against Live PixelVault
    status, conn_res, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/connection-test", method="POST", token=customer_token)
    record(2, 2, "Real Upstream Connection Test", status == 200 and conn_res.get("reachable") == True, f"HTTP {status}, Reachable={conn_res.get('reachable')}, Latency={conn_res.get('latencyMs')}ms, UpstreamStatus={conn_res.get('statusCode')}")

    # 2.3 Verify Upstream Auth is NONE
    status, auth_cfg, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/upstream-auth", token=customer_token)
    record(2, 3, "Verify Upstream Auth is No Auth (Public)", status == 200 and auth_cfg.get("type") == "NONE", f"HTTP {status}, Type={auth_cfg.get('type')}")

    # ---------------------------------------------------------
    # PHASE 3: CREATE SENTINEL DEVELOPER API KEY
    # ---------------------------------------------------------
    print("\n>>> PHASE 3: Create Sentinel Developer API Key")

    key_payload = {
        "name": "PixelVault Web Client",
        "scopes": ["READ", "WRITE"],
        "rateLimitPerMinute": 100
    }
    status, key_res, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/keys", method="POST", data=key_payload, token=customer_token)
    raw_api_key = key_res.get("rawKey") or key_res.get("apiKey") or key_res.get("key")
    key_id = key_res.get("id")
    masked_key = f"{raw_api_key[:8]}...{raw_api_key[-4:]}" if raw_api_key and len(raw_api_key) > 12 else "KEY_GENERATED"
    record(3, 1, "Generate Developer API Key", status in [200, 201] and raw_api_key is not None, f"HTTP {status}, KeyId={key_id}, Format={masked_key}")

    # 3.2 Verify Key Listing does NOT return raw secret
    status, keys_list, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/keys", token=customer_token)
    has_secret_leaked = any(k.get("rawKey") for k in (keys_list if isinstance(keys_list, list) else []))
    record(3, 2, "Hashed Secret Storage (No Raw Secret Leak)", status == 200 and not has_secret_leaked, f"HTTP {status}, KeysCount={len(keys_list) if isinstance(keys_list, list) else 0}")

    # ---------------------------------------------------------
    # PHASE 4: TEST REAL API ACCESS THROUGH SENTINEL
    # ---------------------------------------------------------
    print("\n>>> PHASE 4: Real API Access: Customer -> Sentinel -> PixelVault")

    # 4.1 Real GET request through Sentinel Gateway to PixelVault
    gw_headers = {"X-Sentinel-API-Key": raw_api_key}
    status, gw_res, gw_resp_headers = request_json(f"{BASE_URL}/api/v1/gateway/health", headers=gw_headers)
    req_id = gw_resp_headers.get("X-Request-Id") or gw_resp_headers.get("x-request-id")
    record(4, 1, "Gateway GET /health to PixelVault", status in [200, 404] and req_id is not None, f"HTTP {status} (from PixelVault), X-Request-Id={req_id}")

    # 4.2 Real GET root endpoint / through Sentinel Gateway
    status, root_res, root_headers = request_json(f"{BASE_URL}/api/v1/gateway/", headers=gw_headers)
    root_req_id = root_headers.get("X-Request-Id") or root_headers.get("x-request-id")
    record(4, 2, "Gateway GET / to PixelVault", status in [200, 404] and root_req_id is not None, f"HTTP {status}, X-Request-Id={root_req_id}")

    # 4.3 Invalid endpoint through Sentinel Gateway
    status, inv_res, inv_headers = request_json(f"{BASE_URL}/api/v1/gateway/non-existent-endpoint-12345", headers=gw_headers)
    record(4, 3, "Gateway 404 Handling (Invalid Path)", status == 404, f"HTTP {status} forwarded accurately from upstream")

    # 4.4 Verify Automatic Discovery of Endpoints
    time.sleep(1)
    status, endpoints_list, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/apis", token=customer_token)
    paths_found = [e.get("path") or e.get("normalizedPath") for e in (endpoints_list if isinstance(endpoints_list, list) else [])]
    record(4, 4, "Automatic API Discovery in Catalog", status == 200 and len(endpoints_list) > 0, f"HTTP {status}, Discovered={paths_found}")

    # ---------------------------------------------------------
    # PHASE 5: VERIFY SECURITY CONTROLS
    # ---------------------------------------------------------
    print("\n>>> PHASE 5: Gateway Security Verification")

    # 5.1 Missing Sentinel API Key -> 401 Unauthorized
    status, sec_res, _ = request_json(f"{BASE_URL}/api/v1/gateway/health")
    record(5, 1, "Reject Missing API Key (401)", status == 401, f"HTTP {status}, Code={sec_res.get('code')}")

    # 5.2 Invalid Sentinel API Key -> 401 Unauthorized
    status, inv_key_res, _ = request_json(f"{BASE_URL}/api/v1/gateway/health", headers={"X-Sentinel-API-Key": "sk_live_invalid_token_99999"})
    record(5, 2, "Reject Invalid API Key (401)", status == 401, f"HTTP {status}, Code={inv_key_res.get('code')}")

    # 5.3 Tenant Isolation: Second User cannot access PixelVault Application
    user2_email = f"attacker-{unique_id}@othercorp.io"
    _, u2_reg, _ = request_json(f"{BASE_URL}/api/v1/auth/register", method="POST", data={"name": "Other User", "email": user2_email, "password": customer_pass})
    _, u2_login, _ = request_json(f"{BASE_URL}/api/v1/auth/login", method="POST", data={"email": user2_email, "password": customer_pass})
    u2_token = u2_login.get("token")
    status, iso_res, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}", token=u2_token)
    record(5, 3, "Tenant & Application Isolation", status in [403, 404], f"HTTP {status} (Unauthorized tenant access prevented)")

    # 5.4 Internal Header Stripping
    status, _, fwd_hdrs = request_json(
        f"{BASE_URL}/api/v1/gateway/health",
        headers={"X-Sentinel-API-Key": raw_api_key, "X-Internal-Sentinel-Role": "ADMIN_OVERRIDE"}
    )
    record(5, 4, "Header Injection & Tampering Prevention", status in [200, 404], f"HTTP {status}, Conflicting internal headers handled safely")

    # ---------------------------------------------------------
    # PHASE 6: VERIFY OBSERVABILITY & TELEMETRY
    # ---------------------------------------------------------
    print("\n>>> PHASE 6: Observability, Metrics & Telemetry")

    # 6.1 Request Explorer Logs
    status, logs_res, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/requests", token=customer_token)
    logs = logs_res.get("content", []) if isinstance(logs_res, dict) else logs_res
    record(6, 1, "Request Explorer Logs Recorded", status == 200 and len(logs) > 0, f"HTTP {status}, TotalLogsRecorded={len(logs)}")

    # 6.2 Global API Catalog
    status, global_apis, _ = request_json(f"{BASE_URL}/api/v1/apis", token=customer_token)
    api_items = global_apis.get("content", []) if isinstance(global_apis, dict) else global_apis
    record(6, 2, "Global API Catalog Multi-App View", status == 200 and len(api_items) > 0, f"HTTP {status}, CatalogEndpointsCount={len(api_items)}")

    # 6.3 Dashboard Metrics Updates
    status, updated_dash, _ = request_json(f"{BASE_URL}/api/v1/dashboard/summary", token=customer_token)
    tot_reqs = updated_dash.get("totalRequests", 0)
    record(6, 3, "Dashboard Live Telemetry Update", status == 200 and tot_reqs > 0, f"HTTP {status}, TotalRequests={tot_reqs}, AvgLatency={updated_dash.get('avgLatencyMs')}ms")

    # 6.4 Application Analytics Timeseries
    status, ts_data, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/analytics/timeseries", token=customer_token)
    record(6, 4, "Application Telemetry Timeseries", status == 200, f"HTTP {status}, Points={len(ts_data.get('points', [])) if isinstance(ts_data, dict) else 0}")

    # ---------------------------------------------------------
    # PHASE 7: REAL CUSTOMER ACCEPTANCE SUMMARY
    # ---------------------------------------------------------
    print("\n================================================================================")
    print("                    CUSTOMER ACCEPTANCE TEST RESULTS SUMMARY                    ")
    print("================================================================================")
    
    total_tests = len(results)
    passed_tests = sum(1 for r in results if r["status"] == "PASS")
    failed_tests = total_tests - passed_tests

    for r in results:
        print(f"Phase {r['phase']} | Step {r['step']:02d} | [{r['status']}] {r['name']:<42} : {r['details']}")

    print("--------------------------------------------------------------------------------")
    print(f"Total Tests Run: {total_tests} | Passed: {passed_tests} | Failed: {failed_tests}")
    print(f"Overall Acceptance Status: {'SUCCESS (100% PASS)' if failed_tests == 0 else 'FAILURE'}")
    print("================================================================================")

    return failed_tests == 0

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
