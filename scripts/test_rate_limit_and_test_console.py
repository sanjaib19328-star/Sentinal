import urllib.request
import urllib.error
import json
import time
import sys

BASE_URL = "http://127.0.0.1:8080"
PIXELVAULT_URL = "https://pixelvault-clean-api.onrender.com"

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
    print("\n>>> Testing Rate Limiting, Circuit Breaker, and API Test Console")

    # 1. Login
    unique_id = int(time.time())
    email = f"cust-rl-{unique_id}@test.io"
    password = "TestPassword123!"
    request_json(f"{BASE_URL}/api/v1/auth/register", method="POST", data={"name": "RL Tester", "email": email, "password": password})
    _, login_data, _ = request_json(f"{BASE_URL}/api/v1/auth/login", method="POST", data={"email": email, "password": password})
    token = login_data.get("token")

    # 2. Create Application
    _, app_data, _ = request_json(
        f"{BASE_URL}/api/v1/applications",
        method="POST",
        data={"name": "RateLimit Application", "baseUrl": PIXELVAULT_URL, "description": "Rate limiting validation", "authType": "NONE"},
        token=token
    )
    app_id = app_data.get("id")

    # 3. Create Key with low rate limit (5 req/min)
    _, key_data, _ = request_json(
        f"{BASE_URL}/api/v1/applications/{app_id}/keys",
        method="POST",
        data={"name": "Throttled Key", "scopes": ["READ"], "rateLimitPerMinute": 5},
        token=token
    )
    raw_key = key_data.get("rawKey") or key_data.get("apiKey")
    key_id = key_data.get("id")

    # 4. Send requests until 429
    got_429 = False
    retry_after = None
    for i in range(8):
        status, body, hdrs = request_json(f"{BASE_URL}/api/v1/gateway/", headers={"X-Sentinel-API-Key": raw_key})
        print(f"Request {i+1}: HTTP {status}")
        if status == 429:
            got_429 = True
            retry_after = hdrs.get("Retry-After") or hdrs.get("retry-after")
            break
        time.sleep(0.1)

    print(f"Rate Limit 429 Triggered: {got_429} (Retry-After: {retry_after})")

    # 5. Test API Test Console
    tc_payload = {
        "apiKeyId": key_id,
        "method": "GET",
        "path": "/",
        "headers": {},
        "queryParams": {}
    }
    tc_status, tc_res, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/apis/test-console", method="POST", data=tc_payload, token=token)
    print(f"API Test Console Result: HTTP {tc_status}, UpstreamStatus={tc_res.get('statusCode')}, Latency={tc_res.get('latencyMs')}ms, RateLimitLimit={tc_res.get('rateLimitLimit')}, Remaining={tc_res.get('rateLimitRemaining')}")

    # 6. Circuit Breaker Status
    cb_status, cb_res, _ = request_json(f"{BASE_URL}/api/v1/applications/{app_id}/circuit-breaker", token=token)
    print(f"Circuit Breaker Status: HTTP {cb_status}, State={cb_res.get('state')}, Failures={cb_res.get('consecutiveFailures')}")

    success = got_429 and tc_status == 200 and cb_status == 200
    print(f"\nExtended Tests Passed: {success}")
    return success

if __name__ == "__main__":
    sys.exit(0 if main() else 1)
