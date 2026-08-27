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
        return e.code, json.loads(e.read().decode("utf-8"))

def test_circuit_breaker_full_cycle():
    print("[Failure & Recovery Test: Circuit Breaker State Transitions]")
    unique = int(time.time())
    email = f"cbtest-{unique}@sentinel.io"
    post_json("/api/v1/auth/register", {"name": "CB Tester", "email": email, "password": "Password123!"})
    _, login = post_json("/api/v1/auth/login", {"email": email, "password": "Password123!"})
    token = login["token"]

    _, app = post_json("/api/v1/applications", {"name": f"CBApp-{unique}", "baseUrl": "http://127.0.0.1:9090"}, token=token)
    app_id = app["id"]

    # Configure circuit breaker: threshold=3, recoveryTimeout=3s
    put_json(f"/api/v1/applications/{app_id}/policy", {
        "enabled": True,
        "rateLimit": 500,
        "circuitBreakerEnabled": True,
        "circuitFailureThreshold": 3,
        "circuitRecoveryTimeoutSeconds": 3
    }, token=token)

    _, key = post_json(f"/api/v1/applications/{app_id}/keys", {"name": "CBKey"}, token=token)
    api_key = key["apiKey"]

    # 1. State: CLOSED -> normal traffic succeeds
    st, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
    assert st == 200
    st_cb, cb_info = get_json(f"/api/v1/applications/{app_id}/circuit-breaker", token=token)
    assert cb_info["state"] == "CLOSED"
    print("  [OK] State: CLOSED (normal traffic succeeds)")

    # 2. Inject 3 consecutive 500 failures -> Trips to OPEN
    for i in range(3):
        st_err, _ = get_json("/api/v1/gateway/fail/500", headers={"X-Sentinel-API-Key": api_key})
        assert st_err == 500

    st_cb, cb_info = get_json(f"/api/v1/applications/{app_id}/circuit-breaker", token=token)
    assert cb_info["state"] == "OPEN", f"Circuit should be OPEN, was {cb_info['state']}"
    print("  [OK] State: OPEN (tripped after 3 consecutive failures)")

    # 3. Next request must fast-fail with 503 immediately without calling target
    t0 = time.perf_counter()
    st_fast, fast_body = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
    dur_ms = (time.perf_counter() - t0) * 1000.0
    assert st_fast == 503
    assert fast_body.get("error") == "CIRCUIT_BREAKER_OPEN"
    print(f"  [OK] 503 Fast-Fail executed in {dur_ms:.2f}ms with code CIRCUIT_BREAKER_OPEN")

    # 4. Wait for 3.2s recovery timeout -> transitions to HALF_OPEN on next request
    print("  Waiting 3.2s for recovery timeout...")
    time.sleep(3.2)

    # 5. Successful trial request restores CLOSED state
    st_probe, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": api_key})
    assert st_probe == 200
    st_cb, cb_info = get_json(f"/api/v1/applications/{app_id}/circuit-breaker", token=token)
    assert cb_info["state"] == "CLOSED", f"Circuit should be CLOSED after successful probe, was {cb_info['state']}"
    print("  [OK] State recovered to CLOSED following successful probe request")

    print("[PASS] Circuit Breaker Full Cycle Test completed successfully.\n")

if __name__ == "__main__":
    test_circuit_breaker_full_cycle()
