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

def test_api_key_security_lifecycle():
    print("[Security Test: API Key Lifecycle & Secret Protection]")
    unique = int(time.time())
    email = f"keysec-{unique}@sentinel.io"
    
    # 1. Setup User & App
    st, _ = post_json("/api/v1/auth/register", {"name": "Key Tester", "email": email, "password": "Password123!"})
    assert st == 201
    _, login = post_json("/api/v1/auth/login", {"email": email, "password": "Password123!"})
    token = login["token"]

    _, app = post_json("/api/v1/applications", {"name": f"KeyApp-{unique}", "baseUrl": "http://127.0.0.1:9090"}, token=token)
    app_id = app["id"]

    # 2. Key Generation & Secret Isolation
    st, key_created = post_json(f"/api/v1/applications/{app_id}/keys", {"name": "Primary Key", "rateLimitPerMinute": 100}, token=token)
    assert st == 201
    raw_secret = key_created["apiKey"]
    key_id = key_created["id"]
    assert raw_secret.startswith("sk_sentinel_"), "Key must have standard prefix"
    print("  [OK] Raw API key returned on creation")

    # 3. GET keys must NEVER return plaintext apiKey
    st, keys_list = get_json(f"/api/v1/applications/{app_id}/keys", token=token)
    assert st == 200
    listed_key = next(k for k in keys_list if k["id"] == key_id)
    assert listed_key.get("apiKey") is None, "Plaintext API key MUST NOT be returned in key listings"
    assert "••••" in listed_key.get("maskedKey", "") or "sk_" in listed_key.get("maskedKey", ""), "Masked key representation must be present"
    print("  [OK] Plaintext secret never returned in GET listings (maskedKey enforced)")

    # 4. Ingress with Valid Key
    st, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": raw_secret})
    assert st == 200, f"Valid key request failed: {st}"
    print("  [OK] Valid API key allows gateway ingress")

    # 5. Missing / Inactive / Wrong Keys
    st_missing, _ = get_json("/api/v1/gateway/users")
    assert st_missing == 401, f"Missing key must fail with 401, got {st_missing}"

    st_wrong, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": "sk_sentinel_invalid_garbage_key"})
    assert st_wrong == 401, f"Invalid key must fail with 401, got {st_wrong}"
    print("  [OK] Missing and invalid API keys rejected with 401 UNAUTHORIZED")

    # 6. Key Revocation
    st, _ = post_json(f"/api/v1/applications/{app_id}/keys/{key_id}/revoke", None, token=token)
    assert st == 200
    st_revoked, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": raw_secret})
    assert st_revoked == 401, f"Revoked key must fail with 401, got {st_revoked}"
    print("  [OK] Revoked API key immediately returns 401 UNAUTHORIZED")

    # 7. Key Regeneration
    st, regen = post_json(f"/api/v1/applications/{app_id}/keys/{key_id}/regenerate", None, token=token)
    assert st == 200
    new_raw_secret = regen["apiKey"]
    assert new_raw_secret != raw_secret, "Regenerated key secret must differ from old secret"

    st_old_after_regen, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": raw_secret})
    assert st_old_after_regen == 401, "Old key after regeneration must fail with 401"

    st_new_after_regen, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": new_raw_secret})
    assert st_new_after_regen == 200, "New key after regeneration must succeed with 200"
    print("  [OK] Key regeneration immediately invalidates old secret and enables new secret")

    # 8. Key Deletion
    st_del, _ = delete_req(f"/api/v1/applications/{app_id}/keys/{key_id}", token=token)
    assert st_del in [200, 204]

    st_after_del, _ = get_json("/api/v1/gateway/users", headers={"X-Sentinel-API-Key": new_raw_secret})
    assert st_after_del == 401, "Deleted key must fail with 401"
    print("  [OK] Deleted API key immediately rejected with 401 UNAUTHORIZED")

    print("[PASS] API Key Security & Lifecycle Test Suite completed successfully.\n")

if __name__ == "__main__":
    test_api_key_security_lifecycle()
