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

def get_json(path, token=None):
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(url, method="GET")
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

def test_tenant_isolation():
    print("[Security Test: Multi-Tenant Boundary Isolation]")
    unique = int(time.time())
    
    # Register Tenant A (Alice)
    email_a = f"alice-{unique}@sentinel.io"
    post_json("/api/v1/auth/register", {"name": "Alice Tenant", "email": email_a, "password": "Password123!"})
    _, log_a = post_json("/api/v1/auth/login", {"email": email_a, "password": "Password123!"})
    token_a = log_a["token"]

    # Register Tenant B (Bob)
    email_b = f"bob-{unique}@sentinel.io"
    post_json("/api/v1/auth/register", {"name": "Bob Tenant", "email": email_b, "password": "Password123!"})
    _, log_b = post_json("/api/v1/auth/login", {"email": email_b, "password": "Password123!"})
    token_b = log_b["token"]

    # Tenant A creates Application A & Key A
    _, app_a = post_json("/api/v1/applications", {"name": f"App-A-{unique}", "baseUrl": "http://127.0.0.1:9090"}, token=token_a)
    app_id_a = app_a["id"]
    _, key_a = post_json(f"/api/v1/applications/{app_id_a}/keys", {"name": "Key A"}, token=token_a)
    key_id_a = key_a["id"]

    # Tenant B creates Application B
    _, app_b = post_json("/api/v1/applications", {"name": f"App-B-{unique}", "baseUrl": "http://127.0.0.1:9090"}, token=token_b)
    app_id_b = app_b["id"]

    # 1. Tenant B attempts to fetch Application A
    st_get_app, _ = get_json(f"/api/v1/applications/{app_id_a}", token=token_b)
    assert st_get_app in [403, 404], f"Cross-tenant app fetch must fail, got {st_get_app}"
    print("  [OK] User B cannot read User A's Application")

    # 2. Tenant B attempts to list Application A's Keys
    st_get_keys, _ = get_json(f"/api/v1/applications/{app_id_a}/keys", token=token_b)
    assert st_get_keys in [403, 404], f"Cross-tenant keys fetch must fail, got {st_get_keys}"
    print("  [OK] User B cannot list User A's API Keys")

    # 3. Tenant B attempts to mutate Application A's Policy
    st_put_policy, _ = put_json(f"/api/v1/applications/{app_id_a}/policy", {"enabled": True, "rateLimit": 1}, token=token_b)
    assert st_put_policy in [403, 404], f"Cross-tenant policy mutation must fail, got {st_put_policy}"
    print("  [OK] User B cannot mutate User A's Policies")

    # 4. Tenant B attempts to delete Application A's API Key
    st_del_key, _ = delete_req(f"/api/v1/applications/{app_id_a}/keys/{key_id_a}", token=token_b)
    assert st_del_key in [403, 404], f"Cross-tenant key deletion must fail, got {st_del_key}"
    print("  [OK] User B cannot delete User A's API Keys")

    # 5. Tenant B attempts to delete Application A
    st_del_app, _ = delete_req(f"/api/v1/applications/{app_id_a}", token=token_b)
    assert st_del_app in [403, 404], f"Cross-tenant application deletion must fail, got {st_del_app}"
    print("  [OK] User B cannot delete User A's Application")

    # 6. Global API Catalog & Request Explorer Isolation
    st_global_apis_b, apis_b = get_json("/api/v1/apis", token=token_b)
    assert st_global_apis_b == 200
    app_ids_in_b = [a["applicationId"] for a in apis_b]
    assert app_id_a not in app_ids_in_b, "Global API directory for User B must not leak User A's endpoints"
    print("  [OK] Global API Catalog isolates endpoints by authenticated owner")

    st_global_reqs_b, reqs_b = get_json("/api/v1/requests", token=token_b)
    assert st_global_reqs_b == 200
    app_ids_in_reqs = [r["applicationId"] for r in reqs_b["content"]]
    assert app_id_a not in app_ids_in_reqs, "Global Request Explorer for User B must not leak User A's traffic logs"
    print("  [OK] Global Request Explorer strictly filters logs to user-owned applications")

    print("[PASS] Multi-Tenant Boundary Isolation Test Suite completed successfully.\n")

if __name__ == "__main__":
    test_tenant_isolation()
