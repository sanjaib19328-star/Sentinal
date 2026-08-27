import urllib.request
import urllib.error
import json
import time

BASE_URL = "http://127.0.0.1:8080"

def post_json(path, data, token=None):
    url = f"{BASE_URL}{path}"
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))

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

def test_target_fault_tolerance():
    print("[Failure Test: Target Fault Tolerance & Gateway Stability]")
    unique = int(time.time())
    email = f"fault-{unique}@sentinel.io"
    post_json("/api/v1/auth/register", {"name": "Fault Tester", "email": email, "password": "Password123!"})
    _, login = post_json("/api/v1/auth/login", {"email": email, "password": "Password123!"})
    token = login["token"]

    _, app = post_json("/api/v1/applications", {"name": f"FaultApp-{unique}", "baseUrl": "http://127.0.0.1:9090"}, token=token)
    app_id = app["id"]
    _, key = post_json(f"/api/v1/applications/{app_id}/keys", {"name": "FaultKey"}, token=token)
    api_key = key["apiKey"]

    # 1. Downstream 500
    st_500, body_500 = get_json("/api/v1/gateway/fail/500", headers={"X-Sentinel-API-Key": api_key})
    assert st_500 == 500
    print("  [OK] Gateway cleanly forwards downstream 500 status")

    # 2. Downstream 503
    st_503, body_503 = get_json("/api/v1/gateway/fail/503", headers={"X-Sentinel-API-Key": api_key})
    assert st_503 == 503
    print("  [OK] Gateway cleanly forwards downstream 503 status")

    # 3. Downstream 400 Bad Request
    st_400, body_400 = get_json("/api/v1/gateway/fail/400", headers={"X-Sentinel-API-Key": api_key})
    assert st_400 == 400
    print("  [OK] Gateway cleanly forwards downstream 400 client error")

    # 4. Latency / Delay simulation (200ms delay)
    t0 = time.perf_counter()
    st_delay, _ = get_json("/api/v1/gateway/users?delay=200", headers={"X-Sentinel-API-Key": api_key})
    dur_ms = (time.perf_counter() - t0) * 1000.0
    assert st_delay == 200
    assert dur_ms >= 180, f"Expected delayed response, took {dur_ms}ms"
    print(f"  [OK] Gateway safely proxies delayed downstream response in {dur_ms:.1f}ms")

    # 5. Verify Sentinel Control Plane Remains UP
    st_health, h = get_json("/actuator/health")
    assert st_health == 200 and h.get("status") == "UP"
    print("  [OK] Sentinel control plane remains fully UP throughout downstream faults")

    print("[PASS] Target Fault Tolerance Test Suite completed successfully.\n")

if __name__ == "__main__":
    test_target_fault_tolerance()
