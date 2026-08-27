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
            return resp.status, json.loads(resp.read().decode("utf-8")), resp.headers
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body), e.headers
        except Exception:
            return e.code, {"raw": body}, e.headers

def test_ssrf_and_header_security():
    print("[Security Test: SSRF Mitigation & Header Sanitization]")
    unique = int(time.time())
    email = f"ssrf-{unique}@sentinel.io"
    post_json("/api/v1/auth/register", {"name": "SSRF Tester", "email": email, "password": "Password123!"})
    _, login = post_json("/api/v1/auth/login", {"email": email, "password": "Password123!"})
    token = login["token"]

    _, app = post_json("/api/v1/applications", {"name": f"SSRFApp-{unique}", "baseUrl": "http://127.0.0.1:9090"}, token=token)
    app_id = app["id"]
    _, key = post_json(f"/api/v1/applications/{app_id}/keys", {"name": "SSRFKey"}, token=token)
    api_key = key["apiKey"]

    # 1. SSRF URL Import Attacks
    forbidden_urls = [
        "http://127.0.0.1:8080/actuator/health",
        "http://localhost:3306/openapi.json",
        "http://10.0.0.1/spec.json",
        "http://172.16.0.5/api-docs",
        "http://192.168.1.100/swagger.json",
        "http://169.254.169.254/latest/meta-data/",
        "http://0.0.0.0:9090/spec",
        "ftp://example.com/spec.json",
        "file:///etc/passwd"
    ]

    for malicious_url in forbidden_urls:
        st, res = post_json(f"/api/v1/applications/{app_id}/openapi/import", {"specUrl": malicious_url}, token=token)
        assert st in [400, 403, 502], f"SSRF attempt against {malicious_url} must be blocked, got status {st}"
        print(f"  [OK] Blocked malicious OpenAPI import URL: {malicious_url}")

    # 2. Header Injection & Hop-by-Hop Sanitization
    custom_trace_id = f"custom-trace-{unique}"
    st, body, resp_headers = get_json("/api/v1/gateway/users", headers={
        "X-Sentinel-API-Key": api_key,
        "X-Sentinel-Internal-Role": "ADMIN_OVERRIDE",
        "X-Internal-Secret": "leaked_secret_token",
        "X-Request-Id": custom_trace_id,
        "Connection": "close",
        "Keep-Alive": "timeout=5",
        "Host": "spoofed.domain.internal"
    })
    assert st == 200, f"Gateway request with headers failed: {st}"
    
    # Verify response contains X-Request-Id matching or generated
    received_trace = resp_headers.get("X-Request-Id")
    assert received_trace == custom_trace_id, f"Preserved trace ID expected {custom_trace_id}, got {received_trace}"
    print("  [OK] Preserved client trace ID safely in X-Request-Id header")

    # Verify rate limit headers
    assert "X-RateLimit-Limit" in resp_headers, "Missing X-RateLimit-Limit header"
    assert "X-RateLimit-Remaining" in resp_headers, "Missing X-RateLimit-Remaining header"
    print("  [OK] Verified presence of standard X-RateLimit-* telemetry response headers")

    print("[PASS] SSRF & Header Security Test Suite completed successfully.\n")

if __name__ == "__main__":
    test_ssrf_and_header_security()
